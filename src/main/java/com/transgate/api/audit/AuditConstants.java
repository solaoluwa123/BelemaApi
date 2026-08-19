package com.transgate.api.audit;

/**
 * Request attribute keys and action/outcome constants for the audit trail.
 */
public final class AuditConstants {

    public static final String ATTR_ACTOR_EMAIL = "audit.actorEmail";
    public static final String ATTR_ACTOR_USERNAME = "audit.actorUsername";
    public static final String ATTR_ACTOR_ROLE = "audit.actorRole";

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";

    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_LOGIN_2FA = "LOGIN_2FA";
    public static final String ACTION_LOGIN_2FA_FAILED = "LOGIN_2FA_FAILED";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_MUTATION = "MUTATION";

    private AuditConstants() {
    }
}
