package com.transgate.api.util;

/**
 * Platform role identifiers for Transgate web users (tbl_role / tbl_user_details.role).
 */
public final class PlatformRole {

    public static final int ADMIN = 1;
    public static final int OPERATOR = 2;
    public static final int APPROVER = 3;
    public static final int OTHER_MIN = 4;
    /** Institution contact / Third Party Vendor (same as OTHER_MIN). */
    public static final int THIRD_PARTY_VENDOR = 4;
    public static final int OTHER_MAX = 8;
    public static final int READ_ONLY = 9;

    private PlatformRole() {
    }

    public static boolean isThirdPartyVendor(int role) {
        return role == THIRD_PARTY_VENDOR;
    }

    public static boolean isReadOnly(int role) {
        return role == READ_ONLY;
    }

    public static boolean isPlatformRole(int role) {
        return (role >= ADMIN && role <= APPROVER) || role == READ_ONLY;
    }

    public static boolean isSystemUserRole(int role) {
        return role >= ADMIN && role <= APPROVER;
    }

    public static boolean isOtherUserRole(int role) {
        return role >= OTHER_MIN && role <= OTHER_MAX;
    }

    public static boolean canMutate(int role) {
        return !isReadOnly(role);
    }

    /** Transgate main menu (shared role_id=0 rows plus role-specific parents). */
    public static boolean hasTransgateMenu(int role) {
        return role < 5 || role == READ_ONLY;
    }

    /** Sparkpay secondary menu (admin/operator/approver and read-only). */
    public static boolean hasSparkpayMenu(int role) {
        return role < 4 || role == READ_ONLY;
    }
}
