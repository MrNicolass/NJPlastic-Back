package com.njplastic.njplastic_api.reports.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read-side projection of a {@link ReportHistory} row.
 */
@Schema(description = "Generated report record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHistoryResponseDTO {

  @Schema(description = "History UUID", example = "8c7b6a5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Originating schedule UUID; null when generated on demand", example = "7b6a5c4d-3e2f-1a0b-9c8d-7e6f5a4b3c2d", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID scheduleId;

  @Schema(description = "Report category", example = "SHIFT", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private ReportType type;

  @Schema(description = "Generation timestamp", example = "2026-06-01T07:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime generatedAt;

  @Schema(description = "Output format", example = "CSV", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private ReportFormat format;

  @Schema(description = "Filesystem path to the persisted artifact", example = "./reports/2026/06/01/shift_07-00.csv", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String path;

  @Schema(description = "Size of the artifact in bytes", example = "4096", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private long sizeBytes;

 /**
 * @param entity source row
 * @return populated DTO
 */
  public static ReportHistoryResponseDTO from(ReportHistory entity) {
    return ReportHistoryResponseDTO.builder()
        .id(entity.getId())
        .scheduleId(entity.getScheduleId())
        .type(entity.getType())
        .generatedAt(entity.getGeneratedAt())
        .format(entity.getFormat())
        .path(entity.getPath())
        .sizeBytes(entity.getSizeBytes())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ReportHistoryResponseDTO{id=").append(id)
        .append(", scheduleId=").append(scheduleId)
        .append(", type=").append(type)
        .append(", generatedAt=").append(generatedAt)
        .append(", format=").append(format)
        .append(", path=").append(path)
        .append(", sizeBytes=").append(sizeBytes)
        .append('}');
    return sb.toString();
  }
}
