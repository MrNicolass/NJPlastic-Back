package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.production.enums.MachineState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Status timeline entry, returned inside {@link MachineStatusResponseDTO} and
 * {@link ShiftReportResponseDTO}. Mirrors the persisted
 * {@code machine_status} row with the fields the dashboard needs (no
 * audit timestamps).
 */
@Schema(description = "Machine status timeline entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineStatusEntryDTO {

  @Schema(description = "Machine status UUID", example = "7c8d9e0f-1a2b-3c4d-5e6f-7a8b9c0d1e2f", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Operational state for this transition", example = "AUTO_STOPPED", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private MachineState state;

  @Schema(description = "Classification reason for PAUSED/AUTO_STOPPED records", example = "TROCA_DE_MOLDE", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String reason;

 @Schema(description = "Editable message for AUTO_STOPPED records ", example = "Stop detected automatically after 3 consecutive pauses", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String message;

  @Schema(description = "Transition start timestamp", example = "2026-05-28T14:25:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime startTime;

  @Schema(description = "Transition end timestamp; null while the state is active", example = "2026-05-28T14:40:00Z", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private OffsetDateTime endTime;

 @Schema(description = "Author UUID of the last message edition ", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID reasonAuthorId;

 @Schema(description = "Consecutive-pause counter value when this record was created ", example = "3", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer consecutiveCountAtCreation;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MachineStatusEntryDTO{id=").append(id)
        .append(", state=").append(state)
        .append(", reason=").append(reason)
        .append(", message=").append(message)
        .append(", startTime=").append(startTime)
        .append(", endTime=").append(endTime)
        .append(", reasonAuthorId=").append(reasonAuthorId)
        .append(", consecutiveCountAtCreation=").append(consecutiveCountAtCreation)
        .append('}');
    return sb.toString();
  }
}
