package com.njplastic.njplastic_api.erp.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.erp.entities.ProductionOrderCache;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read-side projection of a {@link ProductionOrderCache} row. Carries the
 * fields the frontend needs to render the production-order list and detail
 * screens (mockup OS_Part1/2_V1) without exposing the raw JSON payload mirror.
 */
@Schema(description = "Production order cache projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderResponseDTO {

  @Schema(description = "Local cache UUID", example = "2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "External order identifier (PK in the ERP)", example = "OS-2026-00123", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String erpOrderId;

  @Schema(description = "Machine UUID the order is bound to; null until the binding is resolved", example = "9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID machineId;

  @Schema(description = "Product SKU produced by the order", example = "PVC-100ML-BR", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String productCode;

  @Schema(description = "Planned production quantity", example = "5000", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer targetQuantity;

  @Schema(description = "ERP-side status of the order", example = "OPEN", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String status;

  @Schema(description = "Instant the cache row was last refreshed by the sync", example = "2026-05-28T14:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime lastSyncAt;

 /**
 * @param entity source cache row
 * @return populated DTO
 */
  public static ProductionOrderResponseDTO from(ProductionOrderCache entity) {
    return ProductionOrderResponseDTO.builder()
        .id(entity.getId())
        .erpOrderId(entity.getErpOrderId())
        .machineId(entity.getMachineId())
        .productCode(entity.getProductCode())
        .targetQuantity(entity.getTargetQuantity())
        .status(entity.getStatus())
        .lastSyncAt(entity.getLastSyncAt())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ProductionOrderResponseDTO{id=").append(id)
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
