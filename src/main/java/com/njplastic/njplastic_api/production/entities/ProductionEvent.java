package com.njplastic.njplastic_api.production.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.njplastic.njplastic_api.production.enums.EventType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Manual production event (EP-BE-05 reopened, RFC §7.3.1). Persists context that
 * is not a cycle nor a pause - training, cleaning, meetings - so the operator
 * dashboard can present an event timeline and OEE reports can subtract these
 * windows when relevant. Maps to {@code production_event} from V8.
 */
@Entity
@Table(name = "production_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionEvent {

  @Schema(description = "Event UUID", example = "8a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Target machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", nullable = false)
  @Column(name = "machine_id", nullable = false)
  private UUID machineId;

  @Schema(description = "Author user UUID; null when the event was created by an automated job", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = true)
  @Column(name = "user_id")
  private UUID userId;

  @Schema(description = "Event category", example = "TRAINING", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", nullable = false, columnDefinition = "event_type")
  private EventType type;

  @Schema(description = "Free-text description supplied by the author", example = "Treinamento de novo operador", nullable = true)
  @Column(name = "description")
  private String description;

  @Schema(description = "Event start timestamp", example = "2026-06-01T10:00:00Z", nullable = false)
  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Schema(description = "Event end timestamp; null while ongoing", example = "2026-06-01T11:30:00Z", nullable = true)
  @Column(name = "ended_at")
  private OffsetDateTime endedAt;

  @Schema(description = "Creation timestamp", example = "2026-06-01T10:00:05Z", nullable = false)
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ProductionEvent{id=").append(id)
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
