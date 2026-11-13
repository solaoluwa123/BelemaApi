package com.transgate.api.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordResetServiceTest {

    @Test
    void tokensAreLongUrlSafeAndUnique() {
        String first = PasswordResetService.newToken();
        String second = PasswordResetService.newToken();

        assertEquals(43, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
        assertNotEquals(first, second);
    }

    @Test
    void referencesUseUnambiguousCharacters() {
        String reference = PasswordResetService.newReference();

        assertEquals(16, reference.length());
        assertTrue(reference.matches("[A-HJ-NP-Z2-9]+"));
    }

    @Test
    void onlyTheSha256HashIsStoredAndMatched() {
        String token = PasswordResetService.newToken();
        String hash = PasswordResetService.hashToken(token);

        assertEquals(64, hash.length());
        assertNotEquals(token, hash);
        assertTrue(PasswordResetService.tokenMatches(token, hash));
        assertTrue(PasswordResetService.tokenMatches(" " + token + " ", hash));
        assertFalse(PasswordResetService.tokenMatches(PasswordResetService.newToken(), hash));
        assertFalse(PasswordResetService.tokenMatches(null, hash));
        assertFalse(PasswordResetService.tokenMatches("", hash));
        assertFalse(PasswordResetService.tokenMatches(token, null));
    }
}
