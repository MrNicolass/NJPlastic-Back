package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.production.entities.Machine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Detailed projection of a {@link Machine}. Used by {@code GET /machines/{id}}
 * and by the response of {@code POST}/{@code PUT}, carrying every operational
 * parameter that the Manager screen shows (mockup Machine_Register_Modal_*).
 */
@Schema(description = "Full machine projection with detection parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineDetailResponseDTO {

  @Schema(description = "Machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Short code provisioned on the Arduino", example = "MAQ-01", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String code;

  @Schema(description = "Human-readable machine description", example = "Injetora 80t linha A", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String description;

  @Schema(description = "Sector the machine belongs to", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String sector;

 @Schema(description = "Standard cycle time in milliseconds ", example = "2000", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private Integer standardCycleMs;

 @Schema(description = "Tolerance factor applied over the standard cycle for pause detection ", example = "1.50", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private BigDecimal toleranceFactor;

 @Schema(description = "Consecutive pauses required to escalate to AUTO_STOPPED ", example = "3", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private Integer consecutivePausesToStop;

  @Schema(description = "Window in milliseconds without pulses before the watchdog marks the machine OFFLINE", example = "60000", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private Integer offlineWindowMs;

  @Schema(description = "Whether the machine is active", example = "true", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private boolean active;

  @Schema(description = "Creation timestamp", example = "2026-05-28T08:30:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2026-05-28T08:30:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime updatedAt;

 /**
 * @param machine source entity
 * @return populated DTO
 */
  public static MachineDetailResponseDTO from(Machine machine) {
    return MachineDetailResponseDTO.builder()
        .id(machine.getId())
        .code(machine.getCode())
        .description(machine.getDescription())
        .sector(machine.getSector())
        .standardCycleMs(machine.getStandardCycleMs())
        .toleranceFactor(machine.getToleranceFactor())
        .consecutivePausesToStop(machine.getConsecutivePausesToStop())
        .offlineWindowMs(machine.getOfflineWindowMs())
        .active(machine.isActive())
        .createdAt(machine.getCreatedAt())
        .updatedAt(machine.getUpdatedAt())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MachineDetailResponseDTO{id=").append(id)
        .append(", code=").append(code)
        .append(", description=").append(description)
        .append(", sector=").append(sector)
        .append(", standardCycleMs=").append(standardCycleMs)
        .append(", toleranceFactor=").append(toleranceFactor)
        .append(", consecutivePausesToStop=").append(consecutivePausesToStop)
        .append(", offlineWindowMs=").append(offlineWindowMs)
        .append(", active=").append(active)
        .append(", createdAt=").append(createdAt)
        .append(", updatedAt=").append(updatedAt)
        .append('}');
    return sb.toString();
  }
}
