package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.njplastic.njplastic_api.production.enums.MachineState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response of {@code GET /machines/{id}/status}: current operational
 * state of the machine plus the timeline of status records that overlap
 * the window the caller requested. {@code current} is null until the
 * machine has any recorded transition (cold boot).
 */
@Schema(description = "Current machine state plus optional status timeline window")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineStatusResponseDTO {

  @Schema(description = "Owning machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID machineId;

  @Schema(description = "Current operational state derived from the open machine_status record", example = "RUNNING", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private MachineState currentState;

  @Schema(description = "Open record describing the current state; null when the machine has no transition yet", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private MachineStatusEntryDTO current;

  @Schema(description = "Window start used to query the timeline", example = "2026-05-28T06:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime from;

  @Schema(description = "Window end used to query the timeline", example = "2026-05-28T14:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime to;

  @Schema(description = "Status records overlapping the window ordered by start time", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private List<MachineStatusEntryDTO> timeline;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MachineStatusResponseDTO{machineId=").append(machineId)
        .append(", currentState=").append(currentState)
        .append(", current=").append(current)
        .append(", from=").append(from)
        .append(", to=").append(to)
        .append(", timelineSize=").append(timeline == null ? 0 : timeline.size())
        .append('}');
    return sb.toString();
  }
}
