package com.njplastic.njplastic_api.erp.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;

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
 * Observability record for one ErpSyncScheduler execution (UC08, EP-BE-06 part
 * 2). Maps to the "erp_sync_run" table created by V6__erp_sync_run.sql.
 * Counters
 * and {@code finishedAt} are null while the row is RUNNING;
 * {@code errorMessage}
 * is null on SUCCESS. This table never owns the {@code record_state} transition
 * of {@code production_cycle}/{@code machine_status} - those stay with
 * ProductionService / MachineStatusService and are mirrored here only as
 * counters for the Manager dashboard.
 */
@Entity
@Table(name = "erp_sync_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpSyncRun {

  @Schema(description = "Sync run UUID", example = "1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Instant the sync started", example = "2026-05-28T14:00:00Z", nullable = false)
  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Schema(description = "Instant the sync finished; null while RUNNING", example = "2026-05-28T14:00:03Z", nullable = true)
  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;

  @Schema(description = "Outcome of the sync execution", example = "SUCCESS", nullable = false)
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private ErpSyncStatus status;

  @Schema(description = "Number of open orders read from the ERP", example = "12", nullable = true)
  @Column(name = "orders_read")
  private Integer ordersRead;

  @Schema(description = "Number of production cycles written to the ERP", example = "240", nullable = true)
  @Column(name = "cycles_written")
  private Integer cyclesWritten;

  @Schema(description = "Number of pause/auto-stop records written to the ERP", example = "5", nullable = true)
  @Column(name = "pauses_written")
  private Integer pausesWritten;

  @Schema(description = "Wall-clock duration of the sync in milliseconds", example = "1840", nullable = true)
  @Column(name = "duration_ms")
  private Integer durationMs;

  @Schema(description = "Error message captured when the sync failed; null on SUCCESS", example = "ERP connection timeout", nullable = true)
  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (startedAt == null) {
      startedAt = OffsetDateTime.now();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ErpSyncRun{id=").append(id)
        .append(", startedAt=").append(startedAt)
        .append(", finishedAt=").append(finishedAt)
        .append(", status=").append(status)
        .append(", ordersRead=").append(ordersRead)
        .append(", cyclesWritten=").append(cyclesWritten)
        .append(", pausesWritten=").append(pausesWritten)
        .append(", durationMs=").append(durationMs)
        .append('}');
    return sb.toString();
  }
}