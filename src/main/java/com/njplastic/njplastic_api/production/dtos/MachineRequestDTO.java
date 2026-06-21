package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Create payload for {@code POST /machines}. Validates the business
 * invariants explicitly: {@code toleranceFactor > 1.0} (a factor below 1
 * would mean every cycle is a pause) and {@code consecutivePausesToStop >= 1}
 * (requires at least one pause to escalate).
 */
@Schema(description = "Machine creation payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineRequestDTO {

  @Schema(description = "Short code provisioned on the Arduino", example = "MAQ-01", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(max = 32)
  private String code;

  @Schema(description = "Human-readable machine description", example = "Injetora 80t linha A", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 255)
  private String description;

  @Schema(description = "Sector the machine belongs to", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 64)
  private String sector;

 @Schema(description = "Standard cycle time in milliseconds ", example = "2000", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @Min(1)
  private Integer standardCycleMs;

 @Schema(description = "Tolerance factor applied over the standard cycle for pause detection. Must be greater than 1.0.", example = "1.50", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @DecimalMin(value = "1.01", inclusive = true)
  private BigDecimal toleranceFactor;

 @Schema(description = "Consecutive pauses required to escalate to AUTO_STOPPED ", example = "3", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @Min(1)
  private Integer consecutivePausesToStop;

  @Schema(description = "Window in milliseconds without pulses before the watchdog marks the machine OFFLINE", example = "60000", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @Min(1000)
  private Integer offlineWindowMs;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MachineRequestDTO{code=").append(code)
        .append(", description=").append(description)
        .append(", sector=").append(sector)
        .append(", standardCycleMs=").append(standardCycleMs)
        .append(", toleranceFactor=").append(toleranceFactor)
        .append(", consecutivePausesToStop=").append(consecutivePausesToStop)
        .append(", offlineWindowMs=").append(offlineWindowMs)
        .append('}');
    return sb.toString();
  }
}
