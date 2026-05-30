package com.njplastic.njplastic_api.production.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.njplastic.njplastic_api.production.enums.RecordState;

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
 * One production cycle generated from a valid MQTT pulse. Maps to the
 * "production_cycle" table created by V1__init.sql. {@code pulseTimestamp} is
 * the
 * TIMESTAMPTZ reconstructed by the service from the Arduino generated_at plus
 * the
 * local date (RN05); {@code intervalMs} is the gap since the previous confirmed
 * cycle (RF07); {@code state} follows the RN07 lifecycle. The {@code machineId}
 * column is a plain UUID without REFERENCES; integrity is enforced in the
 * service
 * layer (RFC §5.2).
 */
@Entity
@Table(name = "production_cycle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionCycle {

  @Schema(description = "Production cycle UUID", example = "1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Owning machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", nullable = false)
  @Column(name = "machine_id", nullable = false)
  private UUID machineId;

  @Schema(description = "Pulse timestamp reconstructed by the backend (RN05)", example = "2026-05-28T14:23:55Z", nullable = false)
  @Column(name = "pulse_timestamp", nullable = false)
  private OffsetDateTime pulseTimestamp;

  @Schema(description = "Instant the pulse was received by the backend", example = "2026-05-28T14:23:55Z", nullable = false)
  @Column(name = "received_at", nullable = false)
  private OffsetDateTime receivedAt;

  @Schema(description = "Monotonic sequence per machine for gap detection", example = "42", nullable = false)
  @Column(name = "sequence", nullable = false)
  private Long sequence;

  @Schema(description = "Interval in milliseconds since the previous confirmed cycle (RF07)", example = "2010", nullable = true)
  @Column(name = "interval_ms")
  private Integer intervalMs;

  @Schema(description = "Record lifecycle state (RN07)", example = "CONFIRMED", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "state", nullable = false, columnDefinition = "record_state")
  private RecordState state;

  @Schema(description = "Creation timestamp", example = "2026-05-28T14:23:55Z", nullable = false)
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
    sb.append("ProductionCycle{id=").append(id)
        .append(", machineId=").append(machineId)
        .append(", pulseTimestamp=").append(pulseTimestamp)
        .append(", receivedAt=").append(receivedAt)
        .append(", sequence=").append(sequence)
        .append(", intervalMs=").append(intervalMs)
        .append(", state=").append(state)
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}