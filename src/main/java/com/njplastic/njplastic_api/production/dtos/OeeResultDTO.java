package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OEE computation result for a machine over a period (RF10). Availability and
 * Performance are always present; Quality and the final OEE are null when no
 * quality counts cover the period, in which case {@code partial} is true. All
 * ratios are fractions in the range [0, 1].
 */
@Schema(description = "OEE result; Quality and OEE are null when the result is partial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OeeResultDTO {

  @Schema(description = "Machine the OEE refers to", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID machineId;

  @Schema(description = "Start of the evaluated window", example = "2026-05-28T06:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime periodStart;

  @Schema(description = "End of the evaluated window", example = "2026-05-28T14:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime periodEnd;

  @Schema(description = "Availability factor: run time over planned time", example = "0.92", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private double availability;

  @Schema(description = "Performance factor: ideal time over run time", example = "0.88", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private double performance;

  @Schema(description = "Quality factor: good parts over total parts; null when unknown", example = "0.95", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Double quality;

  @Schema(description = "Final OEE = Availability x Performance x Quality; null when partial", example = "0.77", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Double oee;

  @Schema(description = "True when Quality is unknown and OEE could not be completed", example = "false", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private boolean partial;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("OeeResultDTO{machineId=").append(machineId)
        .append(", periodStart=").append(periodStart)
        .append(", periodEnd=").append(periodEnd)
        .append(", availability=").append(availability)
        .append(", performance=").append(performance)
        .append(", quality=").append(quality)
        .append(", oee=").append(oee)
        .append(", partial=").append(partial)
        .append('}');
    return sb.toString();
  }
}
