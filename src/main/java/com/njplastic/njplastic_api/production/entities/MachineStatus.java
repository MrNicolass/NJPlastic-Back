package com.njplastic.njplastic_api.production.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Operational state transition of a machine. Maps to the "machine_status" table
 * created by V1__init.sql. Each transition opens a new record with
 * {@code startTime}
 * and a null {@code endTime} while active; the previous open record is closed
 * by the
 * service layer (RN09-RN11). {@code reason}/{@code message} apply to PAUSED and
 * AUTO_STOPPED only (RF08, RF09, RF17, RF18);
 * {@code consecutiveCountAtCreation}
 * preserves the consecutive-pause counter at creation for traceability and to
 * make
 * the counter durable across restarts. {@code machineId}/{@code reasonAuthorId}
 * are
 * plain UUIDs without REFERENCES; integrity is enforced in the service layer.
 */
@Entity
@Table(name = "machine_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineStatus {

  @Schema(description = "Machine status UUID", example = "7c8d9e0f-1a2b-3c4d-5e6f-7a8b9c0d1e2f", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Owning machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", nullable = false)
  @Column(name = "machine_id", nullable = false)
  private UUID machineId;

  @Schema(description = "Operational state for this transition", example = "AUTO_STOPPED", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "state", nullable = false, columnDefinition = "machine_state")
  private MachineState state;

  @Schema(description = "Classification reason for PAUSED/AUTO_STOPPED records", example = "TROCA_DE_MOLDE", nullable = true)
  @Column(name = "reason", length = 255)
  private String reason;

  @Schema(description = "Editable message for AUTO_STOPPED records (RF18, RF19)", example = "Stop detected automatically after 3 consecutive pauses", nullable = true)
  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Schema(description = "Transition start timestamp", example = "2026-05-28T14:25:00Z", nullable = false)
  @Column(name = "start_time", nullable = false)
  private OffsetDateTime startTime;

  @Schema(description = "Transition end timestamp; null while the state is active", example = "2026-05-28T14:40:00Z", nullable = true)
  @Column(name = "end_time")
  private OffsetDateTime endTime;

  @Schema(description = "Author UUID of the last message edition (RN12)", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = true)
  @Column(name = "reason_author_id")
  private UUID reasonAuthorId;

  @Schema(description = "Consecutive-pause counter value when this record was created (RN09-RN11)", example = "3", nullable = true)
  @Column(name = "consecutive_count_at_creation")
  private Integer consecutiveCountAtCreation;

  @Schema(description = "Record lifecycle state (RN07)", example = "CONFIRMED", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "record_state", nullable = false, columnDefinition = "record_state")
  private RecordState recordState;

  @Schema(description = "Creation timestamp", example = "2026-05-28T14:25:00Z", nullable = false)
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2026-05-28T14:40:00Z", nullable = false)
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MachineStatus{id=").append(id)
        .append(", machineId=").append(machineId)
        .append(", state=").append(state)
        .append(", reason=").append(reason)
        .append(", message=").append(message)
        .append(", startTime=").append(startTime)
        .append(", endTime=").append(endTime)
        .append(", reasonAuthorId=").append(reasonAuthorId)
        .append(", consecutiveCountAtCreation=").append(consecutiveCountAtCreation)
        .append(", recordState=").append(recordState)
        .append(", createdAt=").append(createdAt)
        .append(", updatedAt=").append(updatedAt)
        .append('}');
    return sb.toString();
  }
}