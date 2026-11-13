package com.transgate.api.app.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transgate.api.events.FinancialInstitutionCreatedMessage;
import com.transgate.api.events.OutboxEntry;
import com.transgate.api.events.OutboxRecordedEvent;
import com.transgate.api.events.OutboxStore;
import com.transgate.api.events.PasswordResetRequestedMessage;
import com.transgate.api.events.UserCreatedEvent;
import com.transgate.api.events.WelcomeEmailMessage;
import com.transgate.api.models.FinancialInstitutionModel;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Transactional outbox for every RabbitMQ message the API sends ({@code tbl_outbox_event}). The payload is the
 * message JSON, AES-encrypted because it carries passwords.
 */
@Service
public class OutboxEventService implements OutboxStore {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";

    private static final int LAST_ERROR_MAX = 512;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, Class<?>> MESSAGE_TYPES = Map.of(
            FinancialInstitutionCreatedMessage.EVENT_TYPE, FinancialInstitutionCreatedMessage.class,
            WelcomeEmailMessage.EVENT_TYPE, WelcomeEmailMessage.class,
            PasswordResetRequestedMessage.EVENT_TYPE, PasswordResetRequestedMessage.class);

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${app.rabbitmq.fi.exchange}")
    private String fiExchange;

    @Value("${app.rabbitmq.fi.routing-key}")
    private String fiRoutingKey;

    @Value("${app.mail.welcome.exchange}")
    private String welcomeExchange;

    @Value("${app.mail.welcome.routing-key:${app.mail.welcome.queue}}")
    private String welcomeRoutingKey;

    @Value("${app.mail.password-reset.exchange:belema.password.reset.exchange}")
    private String passwordResetExchange;

    @Value("${app.mail.password-reset.routing-key:${app.mail.password-reset.queue:belema.password.reset}}")
    private String passwordResetRoutingKey;

    private final AppEnvironmentConfig appConfig;

    public OutboxEventService(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }

    /** Call inside the transaction that creates the FI; errors propagate so the create rolls back. */
    public void recordFiCreated(FinancialInstitutionModel institution) {
        record(FinancialInstitutionCreatedMessage.EVENT_TYPE, institution.getCode(), fiExchange, fiRoutingKey,
                FinancialInstitutionCreatedMessage.from(institution));
    }

    /** Call inside the transaction that creates the user; errors propagate so the create rolls back. */
    public void recordWelcomeEmail(UserCreatedEvent user) {
        record(WelcomeEmailMessage.EVENT_TYPE, user.getUserEmail(), welcomeExchange, welcomeRoutingKey,
                WelcomeEmailMessage.from(user));
    }

    /** Call inside the transaction that stores the reset token; errors propagate so it rolls back. */
    public void recordPasswordResetRequested(PasswordResetRequestedMessage message) {
        record(PasswordResetRequestedMessage.EVENT_TYPE, message.getUserEmail(), passwordResetExchange,
                passwordResetRoutingKey, message);
    }

    private void record(String eventType, String eventKey, String exchange, String routingKey, Object message) {
        jdbcTemplate.update(
                "INSERT INTO tbl_outbox_event (event_type, event_key, exchange_name, routing_key, payload, status) "
                        + "VALUES (?, ?, ?, ?, TO_BASE64(AES_ENCRYPT(?, ?)), ?)",
                eventType, eventKey, exchange, routingKey, toJson(message), appConfig.getSqlEncodeString(),
                STATUS_PENDING);
        eventPublisher.publishEvent(new OutboxRecordedEvent());
    }

    @Override
    public OutboxEntry lockNextPending() {
        List<OutboxEntry> rows = jdbcTemplate.query(
                "SELECT id, event_type, event_key, exchange_name, routing_key, "
                        + "CAST(AES_DECRYPT(FROM_BASE64(payload), ?) AS CHAR) AS payload "
                        + "FROM tbl_outbox_event WHERE status = ? ORDER BY id ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
                (rs, rowNum) -> {
                    long id = rs.getLong("id");
                    String eventType = rs.getString("event_type");
                    return new OutboxEntry(
                            id,
                            "outbox-" + id,
                            eventType + " " + rs.getString("event_key"),
                            rs.getString("exchange_name"),
                            rs.getString("routing_key"),
                            fromJson(eventType, rs.getString("payload")));
                },
                appConfig.getSqlEncodeString(), STATUS_PENDING);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM tbl_outbox_event WHERE id = ?", id);
    }

    @Override
    public void markPublishFailed(long id, String error, int maxAttempts) {
        jdbcTemplate.update(
                "UPDATE tbl_outbox_event "
                        + "SET status = CASE WHEN attempt_count + 1 >= ? THEN ? ELSE status END, "
                        + "attempt_count = attempt_count + 1, last_error = ?, date_updated = NOW(6) "
                        + "WHERE id = ? AND status = ?",
                maxAttempts, STATUS_FAILED, truncate(error, LAST_ERROR_MAX), id, STATUS_PENDING);
    }

    static String toJson(Object message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise outbox message " + message.getClass().getName(), e);
        }
    }

    /** Rebuilds the original message object so it is published exactly as it would have been when recorded. */
    static Object fromJson(String eventType, String json) {
        Class<?> type = MESSAGE_TYPES.get(eventType);
        if (type == null) {
            throw new IllegalStateException("Unknown outbox event type " + eventType);
        }
        if (json == null) {
            throw new IllegalStateException("Outbox payload for " + eventType + " could not be decrypted");
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read outbox payload for " + eventType, e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
