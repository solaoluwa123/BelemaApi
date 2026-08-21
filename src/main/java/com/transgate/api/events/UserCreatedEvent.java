package com.transgate.api.events;

/**
 * Published when a live Belema user account is created (admin create or approved create).
 */
public class UserCreatedEvent {

    private final String userEmail;
    private final String userName;
    private final String userPassword;
    private final String firstname;
    private final String surname;

    public UserCreatedEvent(String userEmail, String userName, String userPassword,
            String firstname, String surname) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.userPassword = userPassword;
        this.firstname = firstname;
        this.surname = surname;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getSurname() {
        return surname;
    }
}
