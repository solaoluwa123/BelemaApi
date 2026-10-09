package com.transgate.api.events;

import java.time.Instant;

/**
 * Welcome-email payload published to the sw RabbitMQ broker.
 */
public class WelcomeEmailMessage {

    public static final String EVENT_TYPE = "USER_CREATED";

    private String eventType;
    private String createdAt;
    private String userEmail;
    private String userName;
    private String userPassword;
    private String firstname;
    private String surname;
    private String institutionCode;
    private String institutionName;

    public WelcomeEmailMessage() {
    }

    public static WelcomeEmailMessage from(UserCreatedEvent event) {
        WelcomeEmailMessage msg = new WelcomeEmailMessage();
        msg.setEventType(EVENT_TYPE);
        msg.setCreatedAt(Instant.now().toString());
        if (event == null) {
            return msg;
        }
        msg.setUserEmail(event.getUserEmail());
        msg.setUserName(event.getUserName());
        msg.setUserPassword(event.getUserPassword());
        msg.setFirstname(event.getFirstname());
        msg.setSurname(event.getSurname());
        msg.setInstitutionCode(event.getInstitutionCode());
        msg.setInstitutionName(event.getInstitutionName());
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }
}
