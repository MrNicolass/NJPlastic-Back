package com.njplastic.njplastic_api.erp.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KPI snapshot used by the four cards on top of the production-order
 * screen. Each counter reflects the current state of
 * {@code production_order_cache} after the last ERP sync window.
 */
@Schema(description = "Production order KPI counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionOrderSummaryDTO {

  @Schema(description = "Orders currently bound to an active machine", example = "8", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private long inProd;

  @Schema(description = "Orders open on the ERP that have not been bound to a machine yet", example = "12", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private long queued;

  @Schema(description = "Orders past their due date (when the ERP carries one)", example = "3", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private long overdue;

  @Schema(description = "Orders the ERP marked as completed in the latest window", example = "27", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private long completed;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ProductionOrderSummaryDTO{inProd=").append(inProd)
        .append(", queued=").append(queued)
        .append(", overdue=").append(overdue)
        .append(", completed=").append(completed)
        .append('}');
    return sb.toString();
  }
}
