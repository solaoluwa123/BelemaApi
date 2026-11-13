package com.transgate.api.events;

import java.time.Instant;

/**
 * Password-reset payload published to the sw RabbitMQ broker; the consumer emails {@code resetUrl} to the user.
 */
public class PasswordResetRequestedMessage {

    public static final String EVENT_TYPE = "PASSWORD_RESET_REQUESTED";

    private String eventType;
    private String createdAt;
    private String userEmail;
    private String reference;
    private String token;
    private String resetUrl;
    private String expiresAt;

    public PasswordResetRequestedMessage() {
    }

    public static PasswordResetRequestedMessage of(String userEmail, String reference, String token,
            String resetUrl, Instant expiresAt) {
        PasswordResetRequestedMessage msg = new PasswordResetRequestedMessage();
        msg.setEventType(EVENT_TYPE);
        msg.setCreatedAt(Instant.now().toString());
        msg.setUserEmail(userEmail);
        msg.setReference(reference);
        msg.setToken(token);
        msg.setResetUrl(resetUrl);
        msg.setExpiresAt(expiresAt == null ? null : expiresAt.toString());
        return msg;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getResetUrl() {
        return resetUrl;
    }

    public void setResetUrl(String resetUrl) {
        this.resetUrl = resetUrl;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}
