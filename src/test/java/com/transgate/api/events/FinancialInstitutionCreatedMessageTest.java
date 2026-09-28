package com.transgate.api.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transgate.api.models.FinancialInstitutionModel;
import org.junit.jupiter.api.Test;

class FinancialInstitutionCreatedMessageTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void queuePayloadKeepsOnlyIdentityCredentialsAndEmail() throws Exception {
        FinancialInstitutionModel institution = new FinancialInstitutionModel();
        institution.setName("Example Bank");
        institution.setCode("000014");
        institution.setEmail("ops@examplebank.com");
        institution.setPassword("plain-password");
        institution.setHashKey("abcdefghijklmnopqrstuvwxyz012345");
        institution.setBusiness_address("12 Marina");
        institution.setPort_number(9001);
        institution.setServerIP("10.0.0.5");
        institution.setUrl("https://example/ne");

        FinancialInstitutionCreatedMessage message = FinancialInstitutionCreatedMessage.from(institution);
        JsonNode json = mapper.readTree(mapper.writeValueAsString(message));

        assertEquals(FinancialInstitutionCreatedMessage.EVENT_TYPE, json.get("eventType").asText());
        assertEquals("Example Bank", json.get("name").asText());
        assertEquals("000014", json.get("code").asText());
        assertEquals("ops@examplebank.com", json.get("email").asText());
        assertEquals("plain-password", json.get("password").asText());
        assertEquals("abcdefghijklmnopqrstuvwxyz012345", json.get("hashKey").asText());
        assertTrue(json.has("createdAt"));
        assertFalse(json.has("businessAddress"));
        assertFalse(json.has("portNumber"));
        assertFalse(json.has("serverIP"));
        assertFalse(json.has("url"));
    }

    @Test
    void outboxRetryRebuildsTheSamePayload() throws Exception {
        FinancialInstitutionCreatedMessage message = FinancialInstitutionCreatedMessage.fromOutbox(
                "Example Bank",
                "000014",
                "ops@examplebank.com",
                "plain-password",
                "abcdefghijklmnopqrstuvwxyz012345",
                "2026-09-28T10:00:00Z");
        JsonNode json = mapper.readTree(mapper.writeValueAsString(message));

        assertEquals("FINANCIAL_INSTITUTION_CREATED", json.get("eventType").asText());
        assertEquals("2026-09-28T10:00:00Z", json.get("createdAt").asText());
        assertEquals("Example Bank", json.get("name").asText());
        assertEquals("000014", json.get("code").asText());
        assertEquals("ops@examplebank.com", json.get("email").asText());
        assertEquals("plain-password", json.get("password").asText());
        assertEquals("abcdefghijklmnopqrstuvwxyz012345", json.get("hashKey").asText());
        assertEquals(7, json.size());
    }
}
