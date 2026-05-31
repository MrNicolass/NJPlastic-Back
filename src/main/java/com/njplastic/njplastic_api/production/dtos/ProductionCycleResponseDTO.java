package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.production.enums.RecordState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Production cycle item, used as the element type of the paginated
 * response of {@code GET /machines/{id}/cycles}. Mirrors
 * {@link com.njplastic.njplastic_api.production.entities.ProductionCycle}
 * minus persistence-only fields.
 */
@Schema(description = "Production cycle item in the cycle history page")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionCycleResponseDTO {

  @Schema(description = "Production cycle UUID", example = "1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Owning machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID machineId;

  @Schema(description = "Pulse timestamp reconstructed by the backend (RN05)", example = "2026-05-28T14:23:55Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime pulseTimestamp;

  @Schema(description = "Instant the pulse was received by the backend", example = "2026-05-28T14:23:55Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime receivedAt;

  @Schema(description = "Monotonic sequence per machine for gap detection", example = "42", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private Long sequence;

  @Schema(description = "Interval in milliseconds since the previous confirmed cycle (RF07)", example = "2010", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer intervalMs;

  @Schema(description = "Record lifecycle state (RN07)", example = "CONFIRMED", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private RecordState state;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ProductionCycleResponseDTO{id=").append(id)
        .append(", machineId=").append(machineId)
        .append(", pulseTimestamp=").append(pulseTimestamp)
        .append(", receivedAt=").append(receivedAt)
        .append(", sequence=").append(sequence)
        .append(", intervalMs=").append(intervalMs)
        .append(", state=").append(state)
        .append('}');
    return sb.toString();
  }
}
