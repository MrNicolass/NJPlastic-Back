package com.njplastic.njplastic_api.audit.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.audit.entities.AuditLog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read-side projection of an {@link AuditLog} entry. Payloads are surfaced
 * verbatim from the database - they were already sanitized at write time by
 * {@code PayloadSanitizer}, so the read path does not redact a second time.
 */
@Schema(description = "Audit trail projection (EP-BE-08 / RF20)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponseDTO {

  @Schema(description = "Audit entry UUID", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Moment the request was captured", example = "2026-05-28T08:30:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime timestamp;

  @Schema(description = "Author UUID from the security context; null for anonymous calls", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID userId;

  @Schema(description = "HTTP method", example = "POST", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String httpMethod;

  @Schema(description = "Requested endpoint path (with query string when present)", example = "/auth/login", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String endpoint;

  @Schema(description = "HTTP status code returned to the client", example = "200", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private int httpStatus;

  @Schema(description = "Sanitized request body as JSON; passwords, tokens and secrets replaced by [REDACTED]", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String requestPayload;

  @Schema(description = "Sanitized response body as JSON; passwords, tokens and secrets replaced by [REDACTED]", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String responsePayload;

  @Schema(description = "Origin IP address", example = "192.168.0.10", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String sourceIp;

  @Schema(description = "Total request processing time in milliseconds", example = "42", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer durationMs;

  /**
   * @param entity source audit entry
   * @return populated DTO
   */
  public static AuditLogResponseDTO from(AuditLog entity) {
    return AuditLogResponseDTO.builder()
        .id(entity.getId())
        .timestamp(entity.getTimestamp())
        .userId(entity.getUserId())
        .httpMethod(entity.getHttpMethod())
        .endpoint(entity.getEndpoint())
        .httpStatus(entity.getHttpStatus())
        .requestPayload(entity.getRequestPayload())
        .responsePayload(entity.getResponsePayload())
        .sourceIp(entity.getSourceIp())
        .durationMs(entity.getDurationMs())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("AuditLogResponseDTO{id=").append(id)
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
