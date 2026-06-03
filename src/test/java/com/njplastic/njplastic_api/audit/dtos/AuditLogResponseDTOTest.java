package com.njplastic.njplastic_api.audit.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.audit.entities.AuditLog;

class AuditLogResponseDTOTest {

  private AuditLog sampleEntity() {
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
  void from_copiesEveryField() {
    AuditLogResponseDTO dto = AuditLogResponseDTO.from(sampleEntity());

    assertThat(dto.getId()).isEqualTo(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"));
    assertThat(dto.getTimestamp()).isEqualTo(OffsetDateTime.parse("2026-05-28T08:30:00Z"));
    assertThat(dto.getUserId()).isEqualTo(UUID.fromString("11111111-2222-3333-4444-555555555555"));
    assertThat(dto.getHttpMethod()).isEqualTo("POST");
    assertThat(dto.getEndpoint()).isEqualTo("/auth/login");
    assertThat(dto.getHttpStatus()).isEqualTo(200);
    assertThat(dto.getRequestPayload()).isEqualTo("{\"login\":\"manager\"}");
    assertThat(dto.getResponsePayload()).isEqualTo("{\"token\":\"[REDACTED]\"}");
    assertThat(dto.getSourceIp()).isEqualTo("192.168.0.10");
    assertThat(dto.getDurationMs()).isEqualTo(42);
  }

  @Test
  void toString_omitsPayloads() {
    String text = AuditLogResponseDTO.from(sampleEntity()).toString();

    assertThat(text).contains("httpMethod=POST");
    assertThat(text).contains("endpoint=/auth/login");
    assertThat(text).contains("httpStatus=200");
    assertThat(text).doesNotContain("requestPayload");
    assertThat(text).doesNotContain("responsePayload");
  }
}
