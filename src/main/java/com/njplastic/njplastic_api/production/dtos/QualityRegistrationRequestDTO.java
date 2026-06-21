package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload used by a user to register quality counts at the end of a
 * production order, completing the OEE Quality factor. The REST endpoint
 * that consumes it is delivered in; this DTO and the persistence are
 * provided here so OEE can already use the data.
 */
@Schema(description = "Quality counts registered at the end of a production order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityRegistrationRequestDTO {

  @Schema(description = "Owning machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private UUID machineId;

  @Schema(description = "ERP production order identifier this record refers to", example = "OS-2026-00123", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  private String erpOrderId;

  @Schema(description = "Start of the period the counts cover", example = "2026-05-28T06:00:00Z", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private OffsetDateTime periodStart;

  @Schema(description = "End of the period the counts cover", example = "2026-05-28T14:00:00Z", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private OffsetDateTime periodEnd;

  @Schema(description = "Number of good (non-defective) parts produced", example = "950", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @PositiveOrZero
  private Integer goodCount;

  @Schema(description = "Total number of parts produced", example = "1000", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @PositiveOrZero
  private Integer totalCount;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("QualityRegistrationRequestDTO{machineId=").append(machineId)
        .append(", erpOrderId=").append(erpOrderId)
        .append(", periodStart=").append(periodStart)
        .append(", periodEnd=").append(periodEnd)
        .append(", goodCount=").append(goodCount)
        .append(", totalCount=").append(totalCount)
        .append('}');
    return sb.toString();
  }
}
