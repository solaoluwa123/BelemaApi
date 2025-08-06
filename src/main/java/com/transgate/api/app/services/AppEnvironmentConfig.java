package com.transgate.api.app.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Class to fetch environment variables configured in the application's
 * properties.
 *
 * The variables are injected from the environment using Spring's Environment
 * abstraction.
 *
 * @author Makintola
 */
@Component
public class AppEnvironmentConfig {

    @Autowired
    private Environment env;

    // Method to get 'app.sessionmasterkeycheckvalue'
    public String getSessionMasterKeyCheckValue() {
        return env.getProperty("app.sessionmasterkeycheckvalue");
    }

    // Method to get 'app.sessionmasterkey'
    public String getSessionMasterKey() {
        return env.getProperty("app.sessionmasterkey");
    }

    // Method to get 'app.sessionmasterkey2'
    public String getSessionMasterKey2() {
        return env.getProperty("app.sessionmasterkey2");
    }

    // Method to get 'app.bankincomeaccount'
    public String getBankIncomeAccount() {
        return env.getProperty("app.bankincomeaccount");
    }

    // Method to get 'app.habariincomeaccount'
    public String getHabariIncomeAccount() {
        return env.getProperty("app.habariincomeaccount");
    }

    // Method to get 'app.sqlencodestring'
    public String getSqlEncodeString() {
        return env.getProperty("app.sqlencodestring");
    }

    // Method to get 'app.systemficode'
    public String getSystemFICode() {
        return env.getProperty("app.systemficode");
    }

    // Method to get 'app.jwtissuer'
    public String getJWTIssuer() {
        return env.getProperty("app.jwtissuer");
    }

    // Method to get 'app.secretev'
    public String getSecreteV() {
        return env.getProperty("app.secretev");
    }

    // Method to get 'app.apiheader'
    public String getAPIHeader() {
        return env.getProperty("app.apiheader");
    }

    // Method to get 'app.apiheaderexternal'
    public String getAPIHeaderExternal() {
        return env.getProperty("app.apiheaderexternal");
    }

    // Method to get 'app.tippingpoint'
    public String getTippingPoint() {
        String tippingPoint = env.getProperty("app.tippingpoint");
        if (tippingPoint == null || tippingPoint.isEmpty()) {
            // Formatter for ISO 8601 up to seconds
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return LocalDateTime.now().format(fmt);
        }
        return tippingPoint;
    }
}
