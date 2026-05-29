package com.njplastic.njplastic_api.audit.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class PayloadSanitizerTest {

  private static final String JSON = "application/json";

  private PayloadSanitizer sanitizer;

  @BeforeEach
  void setUp() {
    sanitizer = new PayloadSanitizer(new ObjectMapper(), 10240);
  }

  private byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void sanitize_returnsNullForNullBody() {
    assertThat(sanitizer.sanitize(null, JSON)).isNull();
  }

  @Test
  void sanitize_returnsNullForEmptyBody() {
    assertThat(sanitizer.sanitize(new byte[0], JSON)).isNull();
  }

  @Test
  void sanitize_returnsPlaceholderForNullContentType() {
    assertThat(sanitizer.sanitize(bytes("{\"a\":1}"), null))
        .isEqualTo("{\"_nonJson\":true}");
  }

  @Test
  void sanitize_returnsPlaceholderForNonJsonContentType() {
    assertThat(sanitizer.sanitize(bytes("plain text"), "text/plain"))
        .isEqualTo("{\"_nonJson\":true}");
  }

  @Test
  void sanitize_returnsPlaceholderForMalformedJson() {
    assertThat(sanitizer.sanitize(bytes("{not json"), JSON))
        .isEqualTo("{\"_nonJson\":true}");
  }

  @Test
  void sanitize_acceptsContentTypeWithCharsetSuffix() {
    String result = sanitizer.sanitize(bytes("{\"login\":\"manager\"}"),
        "application/json;charset=UTF-8");

    assertThat(result).contains("\"login\":\"manager\"");
  }

  @Test
  void sanitize_redactsSensitiveTopLevelKeys() {
    String result = sanitizer.sanitize(
        bytes("{\"password\":\"secret\",\"token\":\"abc\",\"login\":\"manager\"}"), JSON);

    assertThat(result).contains("\"password\":\"[REDACTED]\"");
    assertThat(result).contains("\"token\":\"[REDACTED]\"");
    assertThat(result).contains("\"login\":\"manager\"");
    assertThat(result).doesNotContain("secret");
    assertThat(result).doesNotContain("abc");
  }

  @Test
  void sanitize_redactsSensitiveKeysCaseInsensitively() {
    String result = sanitizer.sanitize(
        bytes("{\"Password\":\"secret\",\"AUTHORIZATION\":\"Bearer x\"}"), JSON);

    assertThat(result).contains("\"Password\":\"[REDACTED]\"");
    assertThat(result).contains("\"AUTHORIZATION\":\"[REDACTED]\"");
    assertThat(result).doesNotContain("secret");
    assertThat(result).doesNotContain("Bearer x");
  }

  @Test
  void sanitize_redactsNestedSensitiveKeys() {
    String result = sanitizer.sanitize(
        bytes("{\"user\":{\"secret\":\"hidden\",\"name\":\"joe\"}}"), JSON);

    assertThat(result).contains("\"secret\":\"[REDACTED]\"");
    assertThat(result).contains("\"name\":\"joe\"");
    assertThat(result).doesNotContain("hidden");
  }

  @Test
  void sanitize_redactsSensitiveKeysInsideArrays() {
    String result = sanitizer.sanitize(
        bytes("{\"items\":[{\"token\":\"t1\"},{\"token\":\"t2\"}]}"), JSON);

    assertThat(result).contains("[REDACTED]");
    assertThat(result).doesNotContain("t1");
    assertThat(result).doesNotContain("t2");
  }

  @Test
  void sanitize_returnsTruncatedPlaceholderForOversizedPayload() {
    PayloadSanitizer smallSanitizer = new PayloadSanitizer(new ObjectMapper(), 16);
    String result = smallSanitizer.sanitize(
        bytes("{\"data\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"), JSON);

    assertThat(result).startsWith("{\"_truncated\":true,\"size\":");
  }
}
