package com.njplastic.njplastic_api.production.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Quality counts registered by a user at the end of a production order, used to
 * complete the OEE Quality factor. Maps to the "quality_record" table
 * created by V4__quality_record.sql. {@code goodCount}/{@code totalCount} yield
 * the
 * Quality ratio for the covered period; without a record the OEE is returned as
 * partial. {@code machineId}/{@code registeredBy} are plain UUIDs without
 * REFERENCES; integrity is enforced in the service layer.
 */
@Entity
@Table(name = "quality_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityRecord {

  @Schema(description = "Quality record UUID", example = "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Owning machine UUID", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", nullable = false)
  @Column(name = "machine_id", nullable = false)
  private UUID machineId;

  @Schema(description = "ERP production order identifier this record refers to", example = "OS-2026-00123", nullable = true)
  @Column(name = "erp_order_id", length = 64)
  private String erpOrderId;

  @Schema(description = "Start of the period the counts cover", example = "2026-05-28T06:00:00Z", nullable = false)
  @Column(name = "period_start", nullable = false)
  private OffsetDateTime periodStart;

  @Schema(description = "End of the period the counts cover", example = "2026-05-28T14:00:00Z", nullable = false)
  @Column(name = "period_end", nullable = false)
  private OffsetDateTime periodEnd;

  @Schema(description = "Number of good (non-defective) parts produced", example = "950", nullable = false)
  @Column(name = "good_count", nullable = false)
  private Integer goodCount;

  @Schema(description = "Total number of parts produced", example = "1000", nullable = false)
  @Column(name = "total_count", nullable = false)
  private Integer totalCount;

  @Schema(description = "Author UUID who registered the counts", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = true)
  @Column(name = "registered_by")
  private UUID registeredBy;

  @Schema(description = "Registration timestamp", example = "2026-05-28T14:05:00Z", nullable = false)
  @Column(name = "registered_at", nullable = false)
  private OffsetDateTime registeredAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (registeredAt == null) {
      registeredAt = OffsetDateTime.now();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("QualityRecord{id=").append(id)
        .append(", machineId=").append(machineId)
        .append(", erpOrderId=").append(erpOrderId)
        .append(", periodStart=").append(periodStart)
        .append(", periodEnd=").append(periodEnd)
        .append(", goodCount=").append(goodCount)
        .append(", totalCount=").append(totalCount)
        .append(", registeredBy=").append(registeredBy)
        .append(", registeredAt=").append(registeredAt)
        .append('}');
    return sb.toString();
  }
}