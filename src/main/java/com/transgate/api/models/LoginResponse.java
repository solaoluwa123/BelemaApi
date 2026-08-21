/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.models;

import java.util.List;

/**
 *
 * @author Makintola
 */
public class LoginResponse {
    
    int id, roleid, code, twofaenabled, mustChangePassword, require2faSetup;
    String status, role, message, username, firstname, surname, phone_number, email_address, date_created, date_updated, session_token, last_login, financial_institution_code, financial_institution_name, twofasecretkey, temporaryPassword;
    List<MenuModel> transgateMenu, sparkpayMenu;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoleid() {
        return roleid;
    }

    public void setRoleid(int roleid) {
        this.roleid = roleid;
    }
    
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getEmail_address() {
        return email_address;
    }

    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public String getDate_updated() {
        return date_updated;
    }

    public void setDate_updated(String date_updated) {
        this.date_updated = date_updated;
    }

    public String getFinancial_institution_code() {
        return financial_institution_code;
    }

    public void setFinancial_institution_code(String financial_institution_code) {
        this.financial_institution_code = financial_institution_code;
    }

    public String getFinancial_institution_name() {
        return financial_institution_name;
    }

    public void setFinancial_institution_name(String financial_institution_name) {
        this.financial_institution_name = financial_institution_name;
    }

    public String getSession_token() {
        return session_token;
    }

    public void setSession_token(String session_token) {
        this.session_token = session_token;
    }

    public String getLast_login() {
        return last_login;
    }

    public void setLast_login(String last_login) {
        this.last_login = last_login;
    }

    public List<MenuModel> getTransgateMenu() {
        return transgateMenu;
    }

    public void setTransgateMenu(List<MenuModel> transgateMenu) {
        this.transgateMenu = transgateMenu;
    }

    public List<MenuModel> getSparkpayMenu() {
        return sparkpayMenu;
    }

    public void setSparkpayMenu(List<MenuModel> sparkpayMenu) {
        this.sparkpayMenu = sparkpayMenu;
    }

    public int getTwofaenabled() {
        return twofaenabled;
    }

    public void setTwofaenabled(int twofaenabled) {
        this.twofaenabled = twofaenabled;
    }

    public String getTwofasecretkey() {
        return twofasecretkey;
    }

    public void setTwofasecretkey(String twofasecretkey) {
        this.twofasecretkey = twofasecretkey;
    }

    public int getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(int mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public int getRequire2faSetup() {
        return require2faSetup;
    }

    public void setRequire2faSetup(int require2faSetup) {
        this.require2faSetup = require2faSetup;
    }
    
}
