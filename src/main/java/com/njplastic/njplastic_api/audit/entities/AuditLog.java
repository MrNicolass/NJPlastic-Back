package com.njplastic.njplastic_api.audit.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable audit trail entry for every HTTP request reaching the API. Maps to
 * the append-only "audit_log" table created by V1__init.sql (RF20, RN12,
 * RNF08). The table never receives UPDATE or DELETE: there is no
 * {@code @PreUpdate}
 * and the repository exposes no mutating operation. Message edits on
 * machine_status are recorded here as a particular case (RN12). Request and
 * response payloads are stored as sanitized JSON (passwords, tokens and secrets
 * replaced by "[REDACTED]"); user_id is null for anonymous calls.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

  @Schema(description = "Audit entry UUID", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Moment the request was captured", example = "2026-05-28T08:30:00Z", nullable = false)
  @Column(name = "timestamp", nullable = false, updatable = false)
  private OffsetDateTime timestamp;

  @Schema(description = "Author UUID from the security context; null for anonymous calls", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = true)
  @Column(name = "user_id", updatable = false)
  private UUID userId;

  @Schema(description = "HTTP method", example = "POST", nullable = false)
  @Column(name = "http_method", nullable = false, updatable = false, length = 8)
  private String httpMethod;

  @Schema(description = "Requested endpoint path (with query string when present)", example = "/auth/login", nullable = false)
  @Column(name = "endpoint", nullable = false, updatable = false, length = 512)
  private String endpoint;

  @Schema(description = "HTTP status code returned to the client", example = "200", nullable = false)
  @Column(name = "http_status", nullable = false, updatable = false)
  private int httpStatus;

  @Schema(description = "Sanitized request body as JSON; passwords, tokens and secrets replaced by [REDACTED]", nullable = true)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "request_payload", updatable = false)
  private String requestPayload;

  @Schema(description = "Sanitized response body as JSON; passwords, tokens and secrets replaced by [REDACTED]", nullable = true)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "response_payload", updatable = false)
  private String responsePayload;

  @Schema(description = "Origin IP address", example = "192.168.0.10", nullable = true)
  @JdbcTypeCode(SqlTypes.INET)
  @Column(name = "source_ip", updatable = false)
  private String sourceIp;

  @Schema(description = "Total request processing time in milliseconds", example = "42", nullable = true)
  @Column(name = "duration_ms", updatable = false)
  private Integer durationMs;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (timestamp == null) {
      timestamp = OffsetDateTime.now();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("AuditLog{id=").append(id)
        .append(", timestamp=").append(timestamp)
        .append(", userId=").append(userId)
        .append(", httpMethod=").append(httpMethod)
        .append(", endpoint=").append(endpoint)
        .append(", httpStatus=").append(httpStatus)
        .append(", sourceIp=").append(sourceIp)
        .append(", durationMs=").append(durationMs)
        .append('}');
    return sb.toString();
  }
}