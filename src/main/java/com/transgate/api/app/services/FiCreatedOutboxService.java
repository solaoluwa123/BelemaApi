package com.transgate.api.app.services;

import com.transgate.api.events.FinancialInstitutionCreatedMessage;
import com.transgate.api.models.FinancialInstitutionModel;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

/**
 * Persists FI-created queue payloads so a failed RabbitMQ publish can be retried later.
 */
@Service
public class FiCreatedOutboxService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    private static final Logger logger = Logger.getLogger(FiCreatedOutboxService.class.getName());
    private static final int LAST_ERROR_MAX = 512;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private final AppEnvironmentConfig appConfig;

    public FiCreatedOutboxService(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }

    public static class Record {
        private final Long id;
        private final String status;

        public Record(Long id, String status) {
            this.id = id;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }

        public boolean isSent() {
            return STATUS_SENT.equalsIgnoreCase(status);
        }

        public boolean isFailed() {
            return STATUS_FAILED.equalsIgnoreCase(status);
        }

        public boolean alreadyHandled() {
            return isSent() || isFailed();
        }
    }

    public static class PendingRow {
        private final Long id;
        private final String code;
        private final String name;
        private final String email;
        private final String password;
        private final String hashKey;
        private final String eventCreatedAt;
        private final int attemptCount;

        public PendingRow(Long id, String code, String name, String email, String password,
                String hashKey, String eventCreatedAt, int attemptCount) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.email = email;
            this.password = password;
            this.hashKey = hashKey;
            this.eventCreatedAt = eventCreatedAt;
            this.attemptCount = attemptCount;
        }

        public Long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public FinancialInstitutionCreatedMessage toMessage() {
            return FinancialInstitutionCreatedMessage.fromOutbox(
                    name, code, email, password, hashKey, eventCreatedAt);
        }
    }

    /**
     * Insert a PENDING outbox row (password stored AES-encrypted). If one already exists for the code, return it.
     */
    public Record recordPending(FinancialInstitutionModel institution) {
        if (institution == null || nz(institution.getCode()).isEmpty()) {
            return null;
        }
        String code = institution.getCode().trim();
        try {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO tbl_financial_institution_created_outbox "
                                + "(institution_code, institution_name, email, password, hash_key, event_type, event_created_at, status) "
                                + "VALUES (?, ?, ?, TO_BASE64(AES_ENCRYPT(?, ?)), ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, code);
                ps.setString(2, institution.getName());
                ps.setString(3, institution.getEmail());
                ps.setString(4, institution.getPassword());
                ps.setString(5, appConfig.getSqlEncodeString());
                ps.setString(6, institution.getHashKey());
                ps.setString(7, FinancialInstitutionCreatedMessage.EVENT_TYPE);
                ps.setString(8, Instant.now().toString());
                ps.setString(9, STATUS_PENDING);
                return ps;
            }, keys);
            Number key = keys.getKey();
            if (key != null) {
                return new Record(key.longValue(), STATUS_PENDING);
            }
            return findByCode(code);
        } catch (DuplicateKeyException ex) {
            return findByCode(code);
        } catch (DataAccessException ex) {
            logger.log(Level.WARNING,
                    "FI created outbox insert failed for code=" + code + ": " + ex.getMessage());
            return null;
        }
    }

    public void deletePublished(Long id) {
        if (id == null) {
            return;
        }
        try {
            jdbcTemplate.update("DELETE FROM tbl_financial_institution_created_outbox WHERE id = ?", id);
        } catch (DataAccessException ex) {
            logger.log(Level.WARNING, "Could not delete published FI created outbox id=" + id + ": " + ex.getMessage());
        }
    }

    public int deleteSentRows() {
        try {
            return jdbcTemplate.update(
                    "DELETE FROM tbl_financial_institution_created_outbox WHERE status = ?",
                    STATUS_SENT);
        } catch (DataAccessException ex) {
            logger.log(Level.WARNING, "Could not delete SENT FI created outbox rows: " + ex.getMessage());
            return 0;
        }
    }

    public List<PendingRow> listPending(int limit) {
        int batch = limit < 1 ? 1 : limit;
        try {
            return jdbcTemplate.query(
                    "SELECT id, institution_code, institution_name, email, "
                            + "CAST(AES_DECRYPT(FROM_BASE64(password), ?) AS CHAR) AS password, "
                            + "hash_key, event_created_at, attempt_count "
                            + "FROM tbl_financial_institution_created_outbox "
                            + "WHERE status = ? ORDER BY id ASC LIMIT ?",
                    new Object[]{appConfig.getSqlEncodeString(), STATUS_PENDING, batch},
                    (rs, rowNum) -> new PendingRow(
                            rs.getLong("id"),
                            rs.getString("institution_code"),
                            rs.getString("institution_name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("hash_key"),
                            rs.getString("event_created_at"),
                            rs.getInt("attempt_count")));
        } catch (DataAccessException ex) {
            logger.log(Level.WARNING, "Could not list PENDING FI created outbox rows: " + ex.getMessage());
            return List.of();
        }
    }

    public void markPublishFailed(Long id, String error) {
        markPublishFailed(id, error, 0);
    }

    public void markPublishFailed(Long id, String error, int maxAttempts) {
        if (id == null) {
            return;
        }
        try {
            if (maxAttempts > 0) {
                jdbcTemplate.update(
                        "UPDATE tbl_financial_institution_created_outbox "
                                + "SET last_error = ?, attempt_count = attempt_count + 1, date_updated = NOW(6), "
                                + "status = CASE WHEN attempt_count + 1 >= ? THEN ? ELSE status END "
                                + "WHERE id = ? AND status = ?",
                        truncate(error, LAST_ERROR_MAX), maxAttempts, STATUS_FAILED, id, STATUS_PENDING);
            } else {
                jdbcTemplate.update(
                        "UPDATE tbl_financial_institution_created_outbox "
                                + "SET last_error = ?, attempt_count = attempt_count + 1, date_updated = NOW(6) "
                                + "WHERE id = ? AND status = ?",
                        truncate(error, LAST_ERROR_MAX), id, STATUS_PENDING);
            }
        } catch (DataAccessException ex) {
            logger.log(Level.WARNING, "Could not record FI created outbox failure for id=" + id + ": " + ex.getMessage());
        }
    }

    private Record findByCode(String code) {
        try {
            List<Record> rows = jdbcTemplate.query(
                    "SELECT id, status FROM tbl_financial_institution_created_outbox WHERE institution_code = ? LIMIT 1",
                    new Object[]{code},
                    (rs, rowNum) -> new Record(rs.getLong("id"), rs.getString("status")));
            return rows.isEmpty() ? null : rows.get(0);
        } catch (DataAccessException ex) {
            logger.log(Level.WARNING, "Could not load FI created outbox for code=" + code + ": " + ex.getMessage());
            return null;
        }
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
