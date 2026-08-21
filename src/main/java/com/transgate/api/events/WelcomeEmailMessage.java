package com.transgate.api.events;

/**
 * Durable welcome-email payload published to RabbitMQ.
 */
public class WelcomeEmailMessage {

    private String userEmail;
    private String userName;
    private String userPassword;
    private String firstname;
    private String surname;

    public WelcomeEmailMessage() {
    }

    public WelcomeEmailMessage(String userEmail, String userName, String userPassword,
            String firstname, String surname) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.userPassword = userPassword;
        this.firstname = firstname;
        this.surname = surname;
    }

    public static WelcomeEmailMessage from(UserCreatedEvent event) {
        return new WelcomeEmailMessage(
                event.getUserEmail(),
                event.getUserName(),
                event.getUserPassword(),
                event.getFirstname(),
                event.getSurname());
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
}
