package com.transgate.api.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WelcomeEmailMessageTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void queuePayloadIncludesEventTypeAndInstitution() throws Exception {
        UserCreatedEvent event = new UserCreatedEvent(
                "ops@examplebank.com",
                "ops.user",
                "temp-pass",
                "Ada",
                "Okeke",
                "089",
                "Example Bank");

        WelcomeEmailMessage message = WelcomeEmailMessage.from(event);
        JsonNode json = mapper.readTree(mapper.writeValueAsString(message));

        assertEquals(WelcomeEmailMessage.EVENT_TYPE, json.get("eventType").asText());
        assertEquals("USER_CREATED", json.get("eventType").asText());
        assertTrue(json.has("createdAt"));
        assertEquals("ops@examplebank.com", json.get("userEmail").asText());
        assertEquals("ops.user", json.get("userName").asText());
        assertEquals("temp-pass", json.get("userPassword").asText());
        assertEquals("Ada", json.get("firstname").asText());
        assertEquals("Okeke", json.get("surname").asText());
        assertEquals("089", json.get("institutionCode").asText());
        assertEquals("Example Bank", json.get("institutionName").asText());
        assertEquals(9, json.size());
    }
}
