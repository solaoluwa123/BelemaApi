package com.transgate.api.events;

import com.transgate.api.models.FinancialInstitutionModel;
import java.time.Instant;

/**
 * Slim FI-created payload published to the external RabbitMQ broker.
 */
public class FinancialInstitutionCreatedMessage {

    public static final String EVENT_TYPE = "FINANCIAL_INSTITUTION_CREATED";

    private String eventType;
    private String createdAt;
    private String name;
    private String code;
    private String email;
    private String password;
    private String hashKey;

    public FinancialInstitutionCreatedMessage() {
    }

    public static FinancialInstitutionCreatedMessage from(FinancialInstitutionModel institution) {
        FinancialInstitutionCreatedMessage msg = new FinancialInstitutionCreatedMessage();
        msg.setEventType(EVENT_TYPE);
        msg.setCreatedAt(Instant.now().toString());
        if (institution == null) {
            return msg;
        }
        msg.setName(institution.getName());
        msg.setCode(institution.getCode());
        msg.setEmail(institution.getEmail());
        msg.setPassword(institution.getPassword());
        msg.setHashKey(institution.getHashKey());
        return msg;
    }

    public static FinancialInstitutionCreatedMessage fromOutbox(
            String name, String code, String email, String password, String hashKey, String createdAt) {
        FinancialInstitutionCreatedMessage msg = new FinancialInstitutionCreatedMessage();
        msg.setEventType(EVENT_TYPE);
        msg.setCreatedAt(createdAt != null && !createdAt.isEmpty() ? createdAt : Instant.now().toString());
        msg.setName(name);
        msg.setCode(code);
        msg.setEmail(email);
        msg.setPassword(password);
        msg.setHashKey(hashKey);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }
}
