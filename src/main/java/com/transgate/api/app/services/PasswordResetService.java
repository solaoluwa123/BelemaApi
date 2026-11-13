package com.transgate.api.app.services;

import com.transgate.api.events.PasswordResetRequestedMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Forgot-password tokens ({@code tbl_password_reset}). The user's real password is not touched until a reset
 * is completed; only a SHA-256 hash of the token is stored, and each token expires and works once.
 */
@Service
public class PasswordResetService {

    public enum ResetResult {
        RESET,
        INVALID,
        EXPIRED
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String REFERENCE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REFERENCE_LENGTH = 16;
    private static final int TOKEN_BYTES = 32;

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxEventService outboxEventService;

    private final AppEnvironmentConfig appConfig;
    private final TransactionTemplate transactions;
    private final long tokenTtlMinutes;

    public PasswordResetService(AppEnvironmentConfig appConfig, PlatformTransactionManager transactionManager,
            @Value("${app.password-reset.token-ttl-minutes:30}") long tokenTtlMinutes) {
        this.appConfig = appConfig;
        this.transactions = new TransactionTemplate(transactionManager);
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    /**
     * Issues a new reset token for {@code username} (replacing any earlier one) and queues the reset message,
     * in one transaction.
     */
    public void requestReset(String username) {
        String reference = newReference();
        String token = newToken();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(tokenTtlMinutes));
        PasswordResetRequestedMessage message = PasswordResetRequestedMessage.of(
                username, reference, token, resetUrl(username, reference, token), expiresAt);
        transactions.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    "INSERT INTO tbl_password_reset (username, reference, token_hash, expires_at) "
                            + "VALUES (?, ?, ?, DATE_ADD(NOW(6), INTERVAL ? MINUTE)) AS new_row "
                            + "ON DUPLICATE KEY UPDATE reference = new_row.reference, token_hash = new_row.token_hash, "
                            + "expires_at = new_row.expires_at, date_created = NOW(6)",
                    username, reference, hashToken(token), tokenTtlMinutes);
            outboxEventService.recordPasswordResetRequested(message);
        });
    }

    /**
     * Sets {@code newPassword} (already checked against the password policy) if the reference and token match
     * an unexpired reset. A matching reset is deleted once used or once found to be expired.
     */
    public ResetResult resetPassword(String reference, String token, String newPassword) {
        return transactions.execute(status -> {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT username, token_hash, expires_at > NOW(6) AS active "
                            + "FROM tbl_password_reset WHERE reference = ? FOR UPDATE",
                    reference == null ? "" : reference.trim());
            if (rows.isEmpty()) {
                return ResetResult.INVALID;
            }
            Map<String, Object> row = rows.get(0);
            String username = (String) row.get("username");
            if (!tokenMatches(token, (String) row.get("token_hash"))) {
                return ResetResult.INVALID;
            }
            if (!isTrue(row.get("active"))) {
                jdbcTemplate.update("DELETE FROM tbl_password_reset WHERE username = ?", username);
                return ResetResult.EXPIRED;
            }
            int updated = jdbcTemplate.update(
                    "UPDATE sparkpayweb_db.tbl_users SET password = ?, must_change_password = 0 "
                            + "WHERE username = ? AND enabled = 1",
                    BCrypt.hashpw(newPassword, BCrypt.gensalt()), username);
            jdbcTemplate.update("DELETE FROM tbl_password_reset WHERE username = ?", username);
            return updated > 0 ? ResetResult.RESET : ResetResult.INVALID;
        });
    }

    private String resetUrl(String username, String reference, String token) {
        return appConfig.getFrontendBaseUrl() + "/password-recovery/verify?"
                + "ref=" + URLEncoder.encode(reference, StandardCharsets.UTF_8)
                + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    static String newReference() {
        StringBuilder sb = new StringBuilder(REFERENCE_LENGTH);
        for (int i = 0; i < REFERENCE_LENGTH; i++) {
            sb.append(REFERENCE_CHARS.charAt(RANDOM.nextInt(REFERENCE_CHARS.length())));
        }
        return sb.toString();
    }

    static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    static boolean tokenMatches(String token, String storedHash) {
        if (token == null || token.isEmpty() || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hashToken(token.trim()).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value instanceof Number && ((Number) value).intValue() != 0;
    }
}
