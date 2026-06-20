package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.production.enums.RecentEventType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single entry of the Leader's "Eventos recentes" panel (RFC
 * mockup Dashboard_Part2_V1). Aggregated server-side
 * from four sources - manual events, manual pauses, auto stops and stop
 * message edits - and projected into a uniform shape so the frontend can
 * render a single timeline without fan-out fetches per machine.
 */
@Schema(description = "Entry of the Leader 'Eventos recentes' panel; uniform projection of events, pauses, auto stops and stop-message edits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentEventDTO {

  @Schema(description = "Discriminator pointing to the underlying source aggregate", example = "AUTO_STOP", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private RecentEventType type;

  @Schema(description = "Owning machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID machineId;

  @Schema(description = "Short machine code provisioned on the microcontroller; resolved server-side so the frontend can render without a second lookup", example = "INJ-04", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String machineCode;

  @Schema(description = "When the underlying entry happened: startedAt for events, startTime for pauses/stops, timestamp for audit-log edits", example = "2026-06-01T08:42:11Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime timestamp;

  @Schema(description = "Human-readable summary of the entry, ready to render on the timeline", example = "Mold change - batch 4321", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String description;

  @Schema(description = "Author UUID of the underlying entry; null when no human author applies (e.g. AUTO_STOP detected by the scheduler)", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID userId;

  @Schema(description = "Human name of the author resolved from the users aggregate; placeholder when the account was soft-deleted; null when no author applies", example = "Maria Souza", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String userName;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("RecentEventDTO{type=").append(type)
        .append(", machineId=").append(machineId)
        .append(", machineCode=").append(machineCode)
        .append(", timestamp=").append(timestamp)
        .append(", description=").append(description)
        .append(", userId=").append(userId)
        .append(", userName=").append(userName)
        .append('}');
    return sb.toString();
  }
}
