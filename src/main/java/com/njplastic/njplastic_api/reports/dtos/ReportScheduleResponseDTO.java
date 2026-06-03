package com.njplastic.njplastic_api.reports.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.reports.entities.ReportSchedule;
import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read-side projection of a {@link ReportSchedule}.
 */
@Schema(description = "Report schedule projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportScheduleResponseDTO {

  @Schema(description = "Schedule UUID", example = "7b6a5c4d-3e2f-1a0b-9c8d-7e6f5a4b3c2d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Report category", example = "SHIFT", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private ReportType type;

  @Schema(description = "Free-form JSON parameters consumed by the renderer", example = "{\"sector\":\"INJECAO\",\"shift\":\"TURNO_A\"}", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String params;

  @Schema(description = "Spring 6-field cron expression", example = "0 0 7 * * MON-FRI", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String cron;

  @Schema(description = "Destination email", example = "manager@njplastic.com", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String deliveryEmail;

  @Schema(description = "Output format", example = "CSV", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private ReportFormat format;

  @Schema(description = "Whether the schedule is active", example = "true", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private boolean active;

  @Schema(description = "Author of the schedule", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID createdBy;

  @Schema(description = "Creation timestamp", example = "2026-06-01T08:30:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime createdAt;

  /**
   * @param entity source entity
   * @return populated DTO
   */
  public static ReportScheduleResponseDTO from(ReportSchedule entity) {
    return ReportScheduleResponseDTO.builder()
        .id(entity.getId())
        .type(entity.getType())
        .params(entity.getParams())
        .cron(entity.getCron())
        .deliveryEmail(entity.getDeliveryEmail())
        .format(entity.getFormat())
        .active(entity.isActive())
        .createdBy(entity.getCreatedBy())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ReportScheduleResponseDTO{id=").append(id)
        .append(", type=").append(type)
        .append(", cron=").append(cron)
        .append(", deliveryEmail=").append(deliveryEmail)
        .append(", format=").append(format)
        .append(", active=").append(active)
        .append(", createdBy=").append(createdBy)
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
