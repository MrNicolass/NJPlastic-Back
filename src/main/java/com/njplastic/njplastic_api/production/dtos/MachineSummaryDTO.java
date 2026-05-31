package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.UUID;

import com.njplastic.njplastic_api.production.enums.MachineState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compact view of a machine returned by {@code GET /machines} and embedded
 * in detail responses. Carries identification, sector, detection
 * parameters and the current operational state derived from
 * {@code machine_status}.
 */
@Schema(description = "Machine summary view scoped by the caller role (RN02-RN04)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineSummaryDTO {

  @Schema(description = "Machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Short code provisioned on the Arduino", example = "MAQ-01", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String code;

  @Schema(description = "Human-readable machine description", example = "Injection molder 80t line A", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String description;

  @Schema(description = "Sector the machine belongs to", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String sector;

  @Schema(description = "Standard cycle time in milliseconds (RN06)", example = "2000", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private Integer standardCycleMs;

  @Schema(description = "Consecutive pauses required to escalate to AUTO_STOPPED (RN09, RF17)", example = "3", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private Integer consecutivePausesToStop;

  @Schema(description = "Whether the machine is active", example = "true", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private boolean active;

  @Schema(description = "Current operational state derived from machine_status", example = "RUNNING", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private MachineState currentState;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MachineSummaryDTO{id=").append(id)
        .append(", code=").append(code)
        .append(", description=").append(description)
        .append(", sector=").append(sector)
        .append(", standardCycleMs=").append(standardCycleMs)
        .append(", consecutivePausesToStop=").append(consecutivePausesToStop)
        .append(", active=").append(active)
        .append(", currentState=").append(currentState)
        .append('}');
    return sb.toString();
  }
}
