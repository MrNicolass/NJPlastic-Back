package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.production.entities.ProductionEvent;
import com.njplastic.njplastic_api.production.enums.EventType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response projection of a {@link ProductionEvent}. Carries the persisted id
 * and timestamps so the frontend can place the event on the dashboard timeline.
 */
@Schema(description = "Manual production event response payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDTO {

  @Schema(description = "Event UUID", example = "8a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Target machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID machineId;

  @Schema(description = "Author user UUID; null when the event was created by an automated job", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID userId;

  @Schema(description = "Event category", example = "TRAINING", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private EventType type;

  @Schema(description = "Free-text description supplied by the author", example = "Treinamento de novo operador", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String description;

  @Schema(description = "Event start timestamp", example = "2026-06-01T10:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime startedAt;

  @Schema(description = "Event end timestamp; null while ongoing", example = "2026-06-01T11:30:00Z", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private OffsetDateTime endedAt;

  @Schema(description = "Creation timestamp", example = "2026-06-01T10:00:05Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime createdAt;

  /**
   * Build a response DTO from a persisted entity.
   *
   * @param event source entity
   * @return populated DTO
   */
  public static EventResponseDTO from(ProductionEvent event) {
    return EventResponseDTO.builder()
        .id(event.getId())
        .machineId(event.getMachineId())
        .userId(event.getUserId())
        .type(event.getType())
        .description(event.getDescription())
        .startedAt(event.getStartedAt())
        .endedAt(event.getEndedAt())
        .createdAt(event.getCreatedAt())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("EventResponseDTO{id=").append(id)
        .append(", machineId=").append(machineId)
        .append(", userId=").append(userId)
        .append(", type=").append(type)
        .append(", description=").append(description)
        .append(", startedAt=").append(startedAt)
        .append(", endedAt=").append(endedAt)
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
