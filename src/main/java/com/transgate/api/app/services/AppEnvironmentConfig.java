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

    public String getAPIBASEURL() {
        return env.getProperty("app.apibaseurl");
    }

    public String getAutoacceptdisputes() {
        return env.getProperty("app.autoacceptdisputes");
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

    /**
     * Table holding archived transactions. Deployments without an archival feed
     * keep every transaction in the live table, so this points there by default.
     */
    public String getTransactionsArchiveTable() {
        String table = env.getProperty("app.transactions.archive-table");
        if (table == null || table.trim().isEmpty()) {
            return "ajiswitch_db.tbl_creditfundtransfers";
        }
        return table.trim();
    }

    /**
     * Maximum number of live Administrator accounts (role id 1), including seats reserved
     * by pending admin creates. Override with env {@code APP_MAX_ADMINS}.
     */
    public int getMaxAdmins() {
        Integer max = env.getProperty("app.max-admins", Integer.class);
        if (max == null || max < 1) {
            return 2;
        }
        return max;
    }

    /**
     * When true, newly created users get must_change_password=1 and the interceptor
     * blocks other APIs until they update their password. Override with
     * {@code APP_REQUIRE_PASSWORD_CHANGE}.
     */
    public boolean isRequirePasswordChange() {
        return env.getProperty("app.require-password-change", Boolean.class, Boolean.TRUE);
    }

    /**
     * When true, any user with two_fa_enabled=0 must set up 2FA before other APIs.
     * Override with {@code APP_REQUIRE_2FA}.
     */
    public boolean isRequire2fa() {
        return env.getProperty("app.require-2fa", Boolean.class, Boolean.TRUE);
    }

    public String getSupersoftMailHost() {
        return env.getProperty("app.mail.supersoft.host");
    }

    public String getSupersoftMailPort() {
        return env.getProperty("app.mail.supersoft.port");
    }

    public String getSupersoftMailUsername() {
        return env.getProperty("app.mail.supersoft.username");
    }

    public String getSupersoftMailPassword() {
        return env.getProperty("app.mail.supersoft.password");
    }

    public String getSupersoftMailFrom() {
        return env.getProperty("app.mail.supersoft.from");
    }
}
