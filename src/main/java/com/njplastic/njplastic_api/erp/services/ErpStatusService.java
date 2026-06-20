package com.njplastic.njplastic_api.erp.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.erp.dtos.ErpSyncRunDTO;
import com.njplastic.njplastic_api.erp.dtos.ErpSyncStatusResponseDTO;
import com.njplastic.njplastic_api.erp.entities.ErpSyncRun;
import com.njplastic.njplastic_api.erp.enums.ErpConnectionStatus;
import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;
import com.njplastic.njplastic_api.erp.repositories.ErpSyncRunRepository;

/**
 * Aggregates erp_sync_run into the KPI payload consumed by 
 * (GET /erp/sync/status). Stays available even when
 * {@code app.datasource.erp.enabled=false} so the Gestor screen can confirm the
 * disabled state - in that case the response carries
 * {@link ErpConnectionStatus#DISABLED} and the historical KPIs accumulated up
 * to the moment the flag was switched off.
 */
@Service
public class ErpStatusService {

  private final ErpSyncRunRepository syncRunRepository;
  private final boolean erpEnabled;
  private final long fixedDelayMs;
  private final int historyPageSize;
  private final int kpiWindowHours;

  public ErpStatusService(
      ErpSyncRunRepository syncRunRepository,
      @Value("${app.datasource.erp.enabled:false}") boolean erpEnabled,
      @Value("${app.erp.sync.fixed-delay-ms:60000}") long fixedDelayMs,
      @Value("${app.erp.sync.history-page-size:20}") int historyPageSize,
      @Value("${app.erp.sync.kpi-window-hours:24}") int kpiWindowHours) {
    this.syncRunRepository = syncRunRepository;
    this.erpEnabled = erpEnabled;
    this.fixedDelayMs = fixedDelayMs;
    this.historyPageSize = historyPageSize;
    this.kpiWindowHours = kpiWindowHours;
  }

 /**
 * Build the aggregated status payload for the ERP screen. Returns
 * {@link ErpConnectionStatus#DISABLED} top-level status when the ERP
 * datasource flag is off; otherwise derives the connection state from the
 * most recent run.
 *
 * @return the aggregated KPI payload
 */
  public ErpSyncStatusResponseDTO buildStatus() {
    Optional<ErpSyncRun> latest = syncRunRepository.findTopByOrderByStartedAtDesc();
    List<ErpSyncRun> recent = syncRunRepository
        .findAllByOrderByStartedAtDesc(PageRequest.of(0, historyPageSize));
    OffsetDateTime windowStart = OffsetDateTime.now().minus(kpiWindowHours, ChronoUnit.HOURS);
    long total = syncRunRepository.countByStartedAtAfter(windowStart);
    long success = syncRunRepository.countByStartedAtAfterAndStatus(windowStart, ErpSyncStatus.SUCCESS);
    Double avgDurationMs = syncRunRepository.averageDurationMsAfter(windowStart);

    return ErpSyncStatusResponseDTO.builder()
        .status(resolveTopLevelStatus(latest))
        .lastSyncAt(latest.map(ErpSyncRun::getStartedAt).orElse(null))
        .nextWindowAt(estimateNextWindow(latest))
        .successRate24h(successRate(success, total))
        .avgLatencyMs(avgDurationMs == null ? null : avgDurationMs.intValue())
        .ordersReadLastRun(latest.map(ErpSyncRun::getOrdersRead).orElse(null))
        .cyclesWrittenLastRun(latest.map(ErpSyncRun::getCyclesWritten).orElse(null))
        .pausesWrittenLastRun(latest.map(ErpSyncRun::getPausesWritten).orElse(null))
        .lastErrorMessage(latest.map(ErpSyncRun::getErrorMessage).orElse(null))
        .recentRuns(recent.isEmpty() ? Collections.emptyList() : recent.stream().map(this::toDto).toList())
        .build();
  }

  private ErpConnectionStatus resolveTopLevelStatus(Optional<ErpSyncRun> latest) {
    if (!erpEnabled) {
      return ErpConnectionStatus.DISABLED;
    }
    if (latest.isEmpty()) {
      return ErpConnectionStatus.OPERATIONAL;
    }
    return switch (latest.get().getStatus()) {
      case RUNNING -> ErpConnectionStatus.RUNNING;
      case SUCCESS -> ErpConnectionStatus.OPERATIONAL;
      case ERROR, PARTIAL -> ErpConnectionStatus.ERROR;
    };
  }

  private OffsetDateTime estimateNextWindow(Optional<ErpSyncRun> latest) {
    if (!erpEnabled) {
      return null;
    }
    return latest
        .map(ErpSyncRun::getStartedAt)
        .map(start -> start.plus(fixedDelayMs, ChronoUnit.MILLIS))
        .orElse(null);
  }

  private BigDecimal successRate(long success, long total) {
    if (total == 0) {
      return null;
    }
    return BigDecimal.valueOf(success)
        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
  }

  private ErpSyncRunDTO toDto(ErpSyncRun run) {
    boolean wasSuccess = run.getStatus() == ErpSyncStatus.SUCCESS;
    return ErpSyncRunDTO.builder()
        .id(run.getId())
        .startedAt(run.getStartedAt())
        .finishedAt(run.getFinishedAt())
        .status(run.getStatus())
        .ordersRead(run.getOrdersRead())
        .cyclesWritten(run.getCyclesWritten())
        .pausesWritten(run.getPausesWritten())
        .durationMs(run.getDurationMs())
        .errorMessage(wasSuccess ? null : run.getErrorMessage())
        .build();
  }
}
