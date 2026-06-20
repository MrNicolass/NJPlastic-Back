package com.njplastic.njplastic_api.reports.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;

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
 * Generated artifact record (sub-task 5). Maps to {@code report_history}
 * from V11. Retention is enforced by {@code ReportRetentionJob} - the row and
 * the file at {@code path} are deleted after 90 days.
 */
@Entity
@Table(name = "report_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHistory {

  @Schema(description = "History UUID", example = "8c7b6a5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Originating schedule UUID; null when generated on demand", example = "7b6a5c4d-3e2f-1a0b-9c8d-7e6f5a4b3c2d", nullable = true)
  @Column(name = "schedule_id", updatable = false)
  private UUID scheduleId;

  @Schema(description = "Report category", example = "SHIFT", nullable = false)
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, updatable = false, length = 32)
  private ReportType type;

  @Schema(description = "Generation timestamp", example = "2026-06-01T07:00:00Z", nullable = false)
  @Column(name = "generated_at", nullable = false, updatable = false)
  private OffsetDateTime generatedAt;

  @Schema(description = "Output format", example = "CSV", nullable = false)
  @Enumerated(EnumType.STRING)
  @Column(name = "format", nullable = false, updatable = false, length = 8)
  private ReportFormat format;

  @Schema(description = "Filesystem path to the persisted artifact", example = "./reports/2026/06/01/shift_07-00.csv", nullable = false)
  @Column(name = "path", nullable = false, updatable = false, length = 512)
  private String path;

  @Schema(description = "Size of the artifact in bytes", example = "4096", nullable = false)
  @Column(name = "size_bytes", nullable = false, updatable = false)
  private long sizeBytes;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (generatedAt == null) {
      generatedAt = OffsetDateTime.now();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ReportHistory{id=").append(id)
        .append(", scheduleId=").append(scheduleId)
        .append(", type=").append(type)
        .append(", generatedAt=").append(generatedAt)
        .append(", format=").append(format)
        .append(", path=").append(path)
        .append(", sizeBytes=").append(sizeBytes)
        .append('}');
    return sb.toString();
  }
}
