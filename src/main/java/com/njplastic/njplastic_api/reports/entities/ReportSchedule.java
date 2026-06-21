package com.njplastic.njplastic_api.reports.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * Manager-defined schedule executed by {@code ReportSchedulerJob}. Maps to
 * {@code report_schedule}. {@code params} is a free-form JSON document
 * carrying inputs the renderer needs (e.g. SHIFT report -> sector and shift
 * labels).
 */
@Entity
@Table(name = "report_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSchedule {

  @Schema(description = "Schedule UUID", example = "7b6a5c4d-3e2f-1a0b-9c8d-7e6f5a4b3c2d", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Report category", example = "SHIFT", nullable = false)
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 32)
  private ReportType type;

  @Schema(description = "Free-form JSON parameters consumed by the renderer", example = "{\"sector\":\"INJECAO\",\"shift\":\"TURNO_A\"}", nullable = true)
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "params", columnDefinition = "jsonb")
  private String params;

  @Schema(description = "Spring 6-field cron expression (second minute hour day-of-month month day-of-week)", example = "0 0 7 * * MON-FRI", nullable = false)
  @Column(name = "cron", nullable = false, length = 64)
  private String cron;

  @Schema(description = "Destination email", example = "manager@njplastic.com", nullable = false)
  @Column(name = "delivery_email", nullable = false, length = 255)
  private String deliveryEmail;

  @Schema(description = "Output format", example = "CSV", nullable = false)
  @Enumerated(EnumType.STRING)
  @Column(name = "format", nullable = false, length = 8)
  private ReportFormat format;

  @Schema(description = "Whether the schedule is active", example = "true", nullable = false)
  @Column(name = "active", nullable = false)
  private boolean active;

  @Schema(description = "Author of the schedule", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = true)
  @Column(name = "created_by")
  private UUID createdBy;

  @Schema(description = "Creation timestamp", example = "2026-06-01T08:30:00Z", nullable = false)
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
    sb.append("ReportSchedule{id=").append(id)
        .append(", type=").append(type)
        .append(", cron=").append(cron)
        .append(", deliveryEmail=").append(deliveryEmail)
        .append(", format=").append(format)
        .append(", active=").append(active)
        .append(", createdBy=").append(createdBy)
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
