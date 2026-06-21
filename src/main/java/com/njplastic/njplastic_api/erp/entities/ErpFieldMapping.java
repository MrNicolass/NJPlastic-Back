package com.njplastic.njplastic_api.erp.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Field-level mapping between an NJPlastic entity and an ERP table column.
 * Maps to {@code erp_field_mapping}. The combination (entity_type,
 * nj_field) is unique - one local field resolves to at most one ERP column
 * per entity.
 */
@Entity
@Table(name = "erp_field_mapping", uniqueConstraints = @UniqueConstraint(columnNames = { "entity_type", "nj_field" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpFieldMapping {

  @Schema(description = "Mapping UUID", example = "6b5a4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Logical group of mappings (e.g. PRODUCTION_ORDER)", example = "PRODUCTION_ORDER", nullable = false)
  @Column(name = "entity_type", nullable = false, length = 64)
  private String entityType;

  @Schema(description = "NJPlastic-side property name", example = "targetQuantity", nullable = false)
  @Column(name = "nj_field", nullable = false, length = 128)
  private String njField;

  @Schema(description = "ERP-side column name", example = "QTD_PROGRAMADA", nullable = false)
  @Column(name = "erp_field", nullable = false, length = 128)
  private String erpField;

  @Schema(description = "Data type used to bind the JDBC parameter (e.g. INTEGER, VARCHAR, TIMESTAMP)", example = "INTEGER", nullable = false)
  @Column(name = "data_type", nullable = false, length = 32)
  private String dataType;

  @Schema(description = "Whether the mapping is mandatory on the ERP side", example = "true", nullable = false)
  @Column(name = "required", nullable = false)
  private boolean required;

  @Schema(description = "Author of the last update", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = true)
  @Column(name = "updated_by")
  private UUID updatedBy;

  @Schema(description = "Last update timestamp", example = "2026-06-01T08:30:00Z", nullable = false)
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (updatedAt == null) {
      updatedAt = OffsetDateTime.now();
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ErpFieldMapping{id=").append(id)
        .append(", entityType=").append(entityType)
        .append(", njField=").append(njField)
        .append(", erpField=").append(erpField)
        .append(", dataType=").append(dataType)
        .append(", required=").append(required)
        .append(", updatedBy=").append(updatedBy)
        .append(", updatedAt=").append(updatedAt)
        .append('}');
    return sb.toString();
  }
}
