package com.transgate.api.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transgate.api.events.FinancialInstitutionCreatedMessage;
import com.transgate.api.events.PasswordResetRequestedMessage;
import com.transgate.api.events.UserCreatedEvent;
import com.transgate.api.events.WelcomeEmailMessage;
import com.transgate.api.models.FinancialInstitutionModel;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OutboxEventServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void fiCreatedPayloadRoundTripsUnchanged() throws Exception {
        FinancialInstitutionModel institution = new FinancialInstitutionModel();
        institution.setName("Example Bank");
        institution.setCode("000014");
        institution.setEmail("ops@examplebank.com");
        institution.setPassword("plain-password");
        institution.setHashKey("abcdefghijklmnopqrstuvwxyz012345");
        FinancialInstitutionCreatedMessage original = FinancialInstitutionCreatedMessage.from(institution);

        String json = OutboxEventService.toJson(original);
        Object restored = OutboxEventService.fromJson(FinancialInstitutionCreatedMessage.EVENT_TYPE, json);

        assertEquals(FinancialInstitutionCreatedMessage.class, restored.getClass());
        assertEquals(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(restored)));
        assertEquals(original.getCreatedAt(), ((FinancialInstitutionCreatedMessage) restored).getCreatedAt());
    }

    @Test
    void welcomeEmailPayloadRoundTripsUnchanged() throws Exception {
        WelcomeEmailMessage original = WelcomeEmailMessage.from(new UserCreatedEvent(
                "ops@examplebank.com", "ops.user", "temp-pass", "Ada", "Okeke", null, null));

        String json = OutboxEventService.toJson(original);
        Object restored = OutboxEventService.fromJson(WelcomeEmailMessage.EVENT_TYPE, json);

        assertEquals(WelcomeEmailMessage.class, restored.getClass());
        assertEquals(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(restored)));
        assertEquals("temp-pass", ((WelcomeEmailMessage) restored).getUserPassword());
    }

    @Test
    void passwordResetPayloadRoundTripsUnchanged() throws Exception {
        PasswordResetRequestedMessage original = PasswordResetRequestedMessage.of(
                "ops@examplebank.com",
                "ABCDEFGHJKLMNPQR",
                "token-value",
                "http://frontend/password-recovery/verify?ref=ABCDEFGHJKLMNPQR&token=token-value&email=ops%40examplebank.com",
                Instant.parse("2026-11-13T10:30:00Z"));

        String json = OutboxEventService.toJson(original);
        Object restored = OutboxEventService.fromJson(PasswordResetRequestedMessage.EVENT_TYPE, json);

        assertEquals(PasswordResetRequestedMessage.class, restored.getClass());
        assertEquals(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(restored)));
        JsonNode node = mapper.readTree(json);
        assertEquals("PASSWORD_RESET_REQUESTED", node.get("eventType").asText());
        assertEquals("ops@examplebank.com", node.get("userEmail").asText());
        assertEquals("ABCDEFGHJKLMNPQR", node.get("reference").asText());
        assertEquals("token-value", node.get("token").asText());
        assertEquals("2026-11-13T10:30:00Z", node.get("expiresAt").asText());
        assertTrue(node.get("resetUrl").asText().contains("/password-recovery/verify?ref=ABCDEFGHJKLMNPQR"));
        assertTrue(node.has("createdAt"));
        assertEquals(7, node.size());
    }

    @Test
    void rejectsUnknownEventTypesAndUndecryptablePayloads() {
        assertThrows(IllegalStateException.class, () -> OutboxEventService.fromJson("SOMETHING_ELSE", "{}"));
        assertThrows(IllegalStateException.class,
                () -> OutboxEventService.fromJson(WelcomeEmailMessage.EVENT_TYPE, null));
    }
}
