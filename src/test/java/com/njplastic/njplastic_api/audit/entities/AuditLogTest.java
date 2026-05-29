package com.njplastic.njplastic_api.audit.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AuditLogTest {

  private AuditLog sampleLog() {
    return AuditLog.builder()
        .id(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"))
        .timestamp(OffsetDateTime.parse("2026-05-28T08:30:00Z"))
        .userId(UUID.fromString("11111111-2222-3333-4444-555555555555"))
        .httpMethod("POST")
        .endpoint("/auth/login")
        .httpStatus(200)
        .requestPayload("{\"login\":\"manager\"}")
        .responsePayload("{\"token\":\"[REDACTED]\"}")
        .sourceIp("192.168.0.10")
        .durationMs(42)
        .build();
  }

  @Test
  void onCreate_generatesIdAndTimestampWhenMissing() {
    AuditLog log = new AuditLog();

    log.onCreate();

    assertThat(log.getId()).isNotNull();
    assertThat(log.getTimestamp()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndTimestamp() {
    UUID id = UUID.randomUUID();
    OffsetDateTime timestamp = OffsetDateTime.parse("2026-05-28T08:30:00Z");
    AuditLog log = AuditLog.builder().id(id).timestamp(timestamp).build();

    log.onCreate();

    assertThat(log.getId()).isEqualTo(id);
    assertThat(log.getTimestamp()).isEqualTo(timestamp);
  }

  @Test
  void toString_keepsNonSensitiveFields() {
    String text = sampleLog().toString();

    assertThat(text).contains("httpMethod=POST");
    assertThat(text).contains("endpoint=/auth/login");
    assertThat(text).contains("httpStatus=200");
    assertThat(text).contains("sourceIp=192.168.0.10");
    assertThat(text).contains("durationMs=42");
  }

  @Test
  void toString_omitsPayloads() {
    String text = sampleLog().toString();

    assertThat(text).doesNotContain("requestPayload");
    assertThat(text).doesNotContain("responsePayload");
  }
}
