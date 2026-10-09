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
    private final String institutionCode;
    private final String institutionName;

    public UserCreatedEvent(String userEmail, String userName, String userPassword,
            String firstname, String surname) {
        this(userEmail, userName, userPassword, firstname, surname, null, null);
    }

    public UserCreatedEvent(String userEmail, String userName, String userPassword,
            String firstname, String surname, String institutionCode, String institutionName) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.userPassword = userPassword;
        this.firstname = firstname;
        this.surname = surname;
        this.institutionCode = institutionCode;
        this.institutionName = institutionName;
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

    public String getInstitutionCode() {
        return institutionCode;
    }

    public String getInstitutionName() {
        return institutionName;
    }
}
