package com.transgate.api.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Redacts sensitive fields from request body JSON before audit persistence.
 */
public final class AuditRedactor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_DETAILS_CHARS = 2000;
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "passwd",
            "secret",
            "token",
            "session_token",
            "sessiontoken",
            "authorization",
            "auth-token",
            "authtoken",
            "two_fa_secret",
            "twofasecretkey",
            "apiheader",
            "reference"
    );

    private AuditRedactor() {
    }

    public static String redactAndTruncate(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        String trimmed = rawBody.trim();
        try {
            JsonNode root = MAPPER.readTree(trimmed);
            redactNode(root);
            String json = MAPPER.writeValueAsString(root);
            return truncate(json);
        } catch (Exception ex) {
            return truncate(trimmed);
        }
    }

    private static void redactNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return;
        }
        ObjectNode object = (ObjectNode) node;
        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String name = fields.next();
            JsonNode child = object.get(name);
            if (isSensitive(name)) {
                object.put(name, "[REDACTED]");
            } else if (child != null && child.isObject()) {
                redactNode(child);
            } else if (child != null && child.isArray()) {
                for (JsonNode element : child) {
                    redactNode(element);
                }
            }
        }
    }

    private static boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String key = fieldName.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        for (String sensitive : SENSITIVE_KEYS) {
            String normalized = sensitive.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            if (key.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= MAX_DETAILS_CHARS) {
            return value;
        }
        return value.substring(0, MAX_DETAILS_CHARS) + "...";
    }
}
