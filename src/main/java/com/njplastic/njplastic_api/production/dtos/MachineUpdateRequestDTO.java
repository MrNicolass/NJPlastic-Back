package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Update payload for {@code PUT /machines/{id}}. Deliberately omits
 * {@code code} - the short code identifies the machine on the MQTT payload
 * (RFC §5.3) and on historical cycles, so it must be immutable.
 */
@Schema(description = "Machine update payload (code is immutable)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineUpdateRequestDTO {

  @Schema(description = "Human-readable machine description", example = "Injetora 80t linha A", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 255)
  private String description;

  @Schema(description = "Sector the machine belongs to", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 64)
  private String sector;

  @Schema(description = "Standard cycle time in milliseconds (RN06)", example = "2000", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @Min(1)
  private Integer standardCycleMs;

  @Schema(description = "Tolerance factor applied over the standard cycle for pause detection (RN06)", example = "1.50", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @DecimalMin(value = "1.01", inclusive = true)
  private BigDecimal toleranceFactor;

  @Schema(description = "Consecutive pauses required to escalate to AUTO_STOPPED (RN09, RF17)", example = "3", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @Min(1)
  private Integer consecutivePausesToStop;

  @Schema(description = "Window in milliseconds without pulses before the watchdog marks the machine OFFLINE", example = "60000", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @Min(1000)
  private Integer offlineWindowMs;

  @Schema(description = "Whether the machine is active", example = "true", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  private boolean active;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MachineUpdateRequestDTO{description=").append(description)
        .append(", sector=").append(sector)
        .append(", standardCycleMs=").append(standardCycleMs)
        .append(", toleranceFactor=").append(toleranceFactor)
        .append(", consecutivePausesToStop=").append(consecutivePausesToStop)
        .append(", offlineWindowMs=").append(offlineWindowMs)
        .append(", active=").append(active)
        .append('}');
    return sb.toString();
  }
}
