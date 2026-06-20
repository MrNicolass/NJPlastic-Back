package com.njplastic.njplastic_api.erp.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.njplastic.njplastic_api.erp.enums.ErpConnectionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response of {@code GET /erp/sync/status} (Figura 26a/b). Aggregates
 * the KPIs the Gestor needs to validate the ERP integration: overall
 * connection state, last/next sync window, success rate over the last 24h,
 * average latency, counters of the last run, the last error message, and the
 * list of recent executions for the audit log.
 */
@Schema(description = "Aggregated ERP sync KPIs and recent run log for ")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpSyncStatusResponseDTO {

  @Schema(description = "Top-level connection state aggregated by ErpStatusService", example = "OPERATIONAL", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private ErpConnectionStatus status;

  @Schema(description = "Instant of the last sync execution; null when no run has been recorded", example = "2026-05-28T14:00:00Z", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private OffsetDateTime lastSyncAt;

  @Schema(description = "Estimated instant of the next scheduled run; null when disabled or unknown", example = "2026-05-28T14:01:00Z", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private OffsetDateTime nextWindowAt;

  @Schema(description = "Ratio of SUCCESS runs over the last 24 hours (0-1); null when no run in the window", example = "0.9583", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private BigDecimal successRate24h;

  @Schema(description = "Average duration of runs over the last 24 hours in milliseconds; null when no run in the window", example = "1840", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer avgLatencyMs;

  @Schema(description = "Number of open orders read from the ERP in the last run", example = "12", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer ordersReadLastRun;

  @Schema(description = "Number of production cycles written to the ERP in the last run", example = "240", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer cyclesWrittenLastRun;

  @Schema(description = "Number of pause/auto-stop records written to the ERP in the last run", example = "5", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer pausesWrittenLastRun;

  @Schema(description = "Error message from the last failed run; null when last run was SUCCESS or no run exists", example = "ERP connection timeout", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String lastErrorMessage;

  @Schema(description = "Most recent sync executions, newest first; size capped by app.erp.sync.history-page-size", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private List<ErpSyncRunDTO> recentRuns;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ErpSyncStatusResponseDTO{status=").append(status)
        .append(", lastSyncAt=").append(lastSyncAt)
        .append(", nextWindowAt=").append(nextWindowAt)
        .append(", successRate24h=").append(successRate24h)
        .append(", avgLatencyMs=").append(avgLatencyMs)
        .append(", ordersReadLastRun=").append(ordersReadLastRun)
        .append(", cyclesWrittenLastRun=").append(cyclesWrittenLastRun)
        .append(", pausesWrittenLastRun=").append(pausesWrittenLastRun)
        .append(", recentRunsSize=").append(recentRuns == null ? 0 : recentRuns.size())
        .append('}');
    return sb.toString();
  }
}