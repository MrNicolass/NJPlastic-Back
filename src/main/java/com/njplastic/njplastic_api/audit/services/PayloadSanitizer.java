package com.njplastic.njplastic_api.audit.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Turns a raw HTTP body into the JSON string persisted in audit_log, enforcing
 * the LGPD/ sanitization rules. Values of sensitive keys
 * (password, token, authorization, secret) are replaced by "[REDACTED]"; the
 * output is always valid JSON so it can be stored in the JSONB columns.
 * Non-JSON
 * and oversized payloads collapse to small JSON placeholders instead of raw
 * text, keeping the column valid and the table bounded.
 */
@Component
public class PayloadSanitizer {

  private static final String REDACTED = "[REDACTED]";
  private static final String NON_JSON_PLACEHOLDER = "{\"_nonJson\":true}";
  private static final Set<String> SENSITIVE_KEYS = Set.of("password", "token", "authorization", "secret");

  private final ObjectMapper objectMapper;
  private final int maxPayloadBytes;

  public PayloadSanitizer(ObjectMapper objectMapper,
      @Value("${app.audit.max-payload-bytes:10240}") int maxPayloadBytes) {
    this.objectMapper = objectMapper;
    this.maxPayloadBytes = maxPayloadBytes;
  }

 /**
 * Sanitize a request or response body for persistence.
 *
 * @param body the raw body bytes (may be null or empty)
 * @param contentType the body content type, used to detect JSON
 * @return sanitized JSON string, a JSON placeholder for non-JSON/oversized
 * bodies, or null when the body is empty
 */
  public String sanitize(byte[] body, String contentType) {
    if (body == null || body.length == 0) {
      return null;
    }
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("json")) {
      return NON_JSON_PLACEHOLDER;
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      redact(root);
      String result = objectMapper.writeValueAsString(root);
      if (result.length() > maxPayloadBytes) {
        return "{\"_truncated\":true,\"size\":" + body.length + "}";
      }
      return result;
    } catch (JacksonException e) {
      return NON_JSON_PLACEHOLDER;
    }
  }

  private void redact(JsonNode node) {
    if (node instanceof ObjectNode object) {
      List<Map.Entry<String, JsonNode>> entries = new ArrayList<>(object.properties());
      for (Map.Entry<String, JsonNode> entry : entries) {
        if (SENSITIVE_KEYS.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
          object.put(entry.getKey(), REDACTED);
        } else {
          redact(entry.getValue());
        }
      }
    } else if (node instanceof ArrayNode array) {
      for (int i = 0; i < array.size(); i++) {
        redact(array.get(i));
      }
    }
  }
}