package com.njplastic.njplastic_api.production.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Injection machine and its detection parameters. Maps to the "machine" table
 * created by V1__init.sql. {@code standardCycleMs} and {@code toleranceFactor}
 * define the pause threshold (RN06); {@code consecutivePausesToStop} governs
 * the
 * escalation to AUTO_STOPPED (RN09, RF17); {@code offlineWindowMs} drives the
 * watchdog (OFFLINE). The {@code code} column is the short identifier
 * provisioned
 * on the Arduino (e.g. MAQ-01) used to resolve the pulse to a machine_id
 * (RF01).
 */
@Entity
@Table(name = "machine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Machine {

  @Schema(description = "Machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Short code provisioned on the Arduino", example = "MAQ-01", nullable = false)
  @Column(name = "code", nullable = false, unique = true, length = 32)
  private String code;

  @Schema(description = "Human-readable machine description", example = "Injetora 80t linha A", nullable = true)
  @Column(name = "description", length = 255)
  private String description;

  @Schema(description = "Sector the machine belongs to", example = "INJECAO", nullable = true)
  @Column(name = "sector", length = 64)
  private String sector;

  @Schema(description = "Standard cycle time in milliseconds (RN06)", example = "2000", nullable = false)
  @Column(name = "standard_cycle_ms", nullable = false)
  private Integer standardCycleMs;

  @Schema(description = "Tolerance factor applied over the standard cycle for pause detection (RN06)", example = "1.50", nullable = false)
  @Column(name = "tolerance_factor", nullable = false, precision = 5, scale = 2)
  private BigDecimal toleranceFactor;

  @Schema(description = "Consecutive pauses required to escalate to AUTO_STOPPED (RN09, RF17)", example = "3", nullable = false)
  @Column(name = "consecutive_pauses_to_stop", nullable = false)
  private Integer consecutivePausesToStop;

  @Schema(description = "Window in milliseconds without pulses before the watchdog marks the machine OFFLINE", example = "60000", nullable = false)
  @Column(name = "offline_window_ms", nullable = false)
  private Integer offlineWindowMs;

  @Schema(description = "Whether the machine is active", example = "true", nullable = false)
  @Column(name = "active", nullable = false)
  private boolean active;

  @Schema(description = "Creation timestamp", example = "2026-05-28T08:30:00Z", nullable = false)
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2026-05-28T08:30:00Z", nullable = false)
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
    sb.append("Machine{id=").append(id)
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