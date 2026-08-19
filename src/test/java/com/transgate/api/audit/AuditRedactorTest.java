package com.transgate.api.audit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class AuditRedactorTest {

    @Test
    void redactsPasswordFields() {
        String redacted = AuditRedactor.redactAndTruncate("{\"username\":\"a\",\"password\":\"secret\"}");
        assertTrue(redacted.contains("[REDACTED]"));
        assertFalse(redacted.contains("secret"));
        assertTrue(redacted.contains("username"));
    }

    @Test
    void resolveActionDetectsApprove() {
        assertTrue(AuditService.resolveAction("PUT", "/sparkpayapi/wallets/approval").equals("APPROVE"));
        assertTrue(AuditService.resolveAction("DELETE", "/sparkpayapi/users/1/x").equals("DELETE"));
    }
}
