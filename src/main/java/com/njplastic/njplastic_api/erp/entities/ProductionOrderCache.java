package com.njplastic.njplastic_api.erp.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * Local read buffer of open production orders fetched from the customer ERP
 *. Maps to the "production_order_cache" table created by
 * V1__init.sql. Acts as a volatile cache - the ErpSyncScheduler overwrites it
 * on every window so consumers can avoid round-trips against the ERP
 *. The UNIQUE constraint on {@code erpOrderId} makes
 * the upsert idempotent; {@code machineId} may stay null while the order is
 * not yet bound to a local machine.
 */
@Entity
@Table(name = "production_order_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderCache {

  @Schema(description = "Local cache UUID", example = "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "External order identifier (PK in the ERP)", example = "OS-2026-00123", nullable = false)
  @Column(name = "erp_order_id", nullable = false, length = 64, unique = true)
  private String erpOrderId;

  @Schema(description = "Machine UUID the order is bound to; null until the binding is resolved", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", nullable = true)
  @Column(name = "machine_id")
  private UUID machineId;

  @Schema(description = "Product SKU produced by the order", example = "PVC-100ML-BR", nullable = true)
  @Column(name = "product_code", length = 64)
  private String productCode;

  @Schema(description = "Planned production quantity", example = "5000", nullable = true)
  @Column(name = "target_quantity")
  private Integer targetQuantity;

  @Schema(description = "ERP-side status of the order", example = "OPEN", nullable = true)
  @Column(name = "status", length = 32)
  private String status;

  @Schema(description = "Free-form JSON payload mirroring the ERP record", example = "{\"cliente\":\"Acme\",\"molde\":\"MLD-12-A\"}", nullable = true)
  @Column(name = "payload", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String payload;

  @Schema(description = "Instant the cache row was last refreshed by the sync", example = "2026-05-28T14:00:00Z", nullable = false)
  @Column(name = "last_sync_at", nullable = false)
  private OffsetDateTime lastSyncAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (lastSyncAt == null) {
      lastSyncAt = OffsetDateTime.now();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ProductionOrderCache{id=").append(id)
        .append(", erpOrderId=").append(erpOrderId)
        .append(", machineId=").append(machineId)
        .append(", productCode=").append(productCode)
        .append(", targetQuantity=").append(targetQuantity)
        .append(", status=").append(status)
        .append(", lastSyncAt=").append(lastSyncAt)
        .append('}');
    return sb.toString();
  }
}