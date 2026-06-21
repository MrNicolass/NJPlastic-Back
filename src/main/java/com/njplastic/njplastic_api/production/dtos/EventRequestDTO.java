package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.production.enums.EventType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for {@code POST /events} (reopened). The author
 * UUID is taken from the JWT, never from the payload.
 */
@Schema(description = "Manual production event request payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestDTO {

  @Schema(description = "Target machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private UUID machineId;

  @Schema(description = "Event category", example = "TRAINING", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private EventType type;

  @Schema(description = "Free-text description supplied by the author", example = "Treinamento de novo operador", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 2000)
  private String description;

  @Schema(description = "Event start timestamp", example = "2026-06-01T10:00:00Z", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private OffsetDateTime startedAt;

  @Schema(description = "Event end timestamp; null while ongoing", example = "2026-06-01T11:30:00Z", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  private OffsetDateTime endedAt;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("EventRequestDTO{machineId=").append(machineId)
        .append(", type=").append(type)
        .append(", description=").append(description)
        .append(", startedAt=").append(startedAt)
        .append(", endedAt=").append(endedAt)
        .append('}');
    return sb.toString();
  }
}
