package com.njplastic.njplastic_api.erp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.njplastic.njplastic_api.erp.dtos.ErpSyncStatusResponseDTO;
import com.njplastic.njplastic_api.erp.entities.ErpSyncRun;
import com.njplastic.njplastic_api.erp.enums.ErpConnectionStatus;
import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;
import com.njplastic.njplastic_api.erp.repositories.ErpSyncRunRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErpStatusServiceTest {

  @Mock
  private ErpSyncRunRepository syncRunRepository;

  private static final long FIXED_DELAY_MS = 60_000L;
  private static final int HISTORY_PAGE_SIZE = 20;
  private static final int KPI_WINDOW_HOURS = 24;

  private ErpStatusService service(boolean enabled) {
    return new ErpStatusService(syncRunRepository, enabled,
        FIXED_DELAY_MS, HISTORY_PAGE_SIZE, KPI_WINDOW_HOURS);
  }

  private ErpSyncRun run(ErpSyncStatus status, int ordersRead, int cyclesWritten, int pausesWritten,
      int durationMs, String errorMessage) {
    return ErpSyncRun.builder()
        .id(UUID.randomUUID())
        .startedAt(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .finishedAt(OffsetDateTime.parse("2026-05-28T14:00:03Z"))
        .status(status)
        .ordersRead(ordersRead)
        .cyclesWritten(cyclesWritten)
        .pausesWritten(pausesWritten)
        .durationMs(durationMs)
        .errorMessage(errorMessage)
        .build();
  }

  @Test
  void buildStatus_returnsDisabledWithoutNextWindowWhenFlagOff() {
    when(syncRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.empty());
    when(syncRunRepository.findAllByOrderByStartedAtDesc(any(Pageable.class))).thenReturn(List.of());
    when(syncRunRepository.countByStartedAtAfter(any())).thenReturn(0L);
    when(syncRunRepository.countByStartedAtAfterAndStatus(any(), eq(ErpSyncStatus.SUCCESS))).thenReturn(0L);
    when(syncRunRepository.averageDurationMsAfter(any())).thenReturn(null);

    ErpSyncStatusResponseDTO dto = service(false).buildStatus();

    assertThat(dto.getStatus()).isEqualTo(ErpConnectionStatus.DISABLED);
    assertThat(dto.getNextWindowAt()).isNull();
    assertThat(dto.getRecentRuns()).isEmpty();
    assertThat(dto.getSuccessRate24h()).isNull();
    assertThat(dto.getAvgLatencyMs()).isNull();
  }

  @Test
  void buildStatus_returnsOperationalWhenEnabledAndNoHistory() {
    when(syncRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.empty());
    when(syncRunRepository.findAllByOrderByStartedAtDesc(any(Pageable.class))).thenReturn(List.of());
    when(syncRunRepository.countByStartedAtAfter(any())).thenReturn(0L);
    when(syncRunRepository.countByStartedAtAfterAndStatus(any(), eq(ErpSyncStatus.SUCCESS))).thenReturn(0L);
    when(syncRunRepository.averageDurationMsAfter(any())).thenReturn(null);

    ErpSyncStatusResponseDTO dto = service(true).buildStatus();

    assertThat(dto.getStatus()).isEqualTo(ErpConnectionStatus.OPERATIONAL);
    assertThat(dto.getLastSyncAt()).isNull();
    assertThat(dto.getNextWindowAt()).isNull();
  }

  @Test
  void buildStatus_returnsOperationalWhenLastSuccess() {
    ErpSyncRun latest = run(ErpSyncStatus.SUCCESS, 12, 240, 5, 1840, null);
    when(syncRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(latest));
    when(syncRunRepository.findAllByOrderByStartedAtDesc(any(Pageable.class))).thenReturn(List.of(latest));
    when(syncRunRepository.countByStartedAtAfter(any())).thenReturn(2L);
    when(syncRunRepository.countByStartedAtAfterAndStatus(any(), eq(ErpSyncStatus.SUCCESS))).thenReturn(2L);
    when(syncRunRepository.averageDurationMsAfter(any())).thenReturn(1840.0);

    ErpSyncStatusResponseDTO dto = service(true).buildStatus();

    assertThat(dto.getStatus()).isEqualTo(ErpConnectionStatus.OPERATIONAL);
    assertThat(dto.getLastSyncAt()).isEqualTo(latest.getStartedAt());
    assertThat(dto.getNextWindowAt())
        .isEqualTo(latest.getStartedAt().plus(FIXED_DELAY_MS, ChronoUnit.MILLIS));
    assertThat(dto.getOrdersReadLastRun()).isEqualTo(12);
    assertThat(dto.getCyclesWrittenLastRun()).isEqualTo(240);
    assertThat(dto.getPausesWrittenLastRun()).isEqualTo(5);
    assertThat(dto.getAvgLatencyMs()).isEqualTo(1840);
    assertThat(dto.getSuccessRate24h()).isEqualByComparingTo(new BigDecimal("1.0000"));
    assertThat(dto.getLastErrorMessage()).isNull();
    assertThat(dto.getRecentRuns()).hasSize(1);
    assertThat(dto.getRecentRuns().get(0).getErrorMessage()).isNull();
  }

  @Test
  void buildStatus_returnsRunningWhenLastIsRunning() {
    ErpSyncRun latest = run(ErpSyncStatus.RUNNING, 0, 0, 0, 0, null);
    when(syncRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(latest));
    when(syncRunRepository.findAllByOrderByStartedAtDesc(any(Pageable.class))).thenReturn(List.of(latest));
    when(syncRunRepository.countByStartedAtAfter(any())).thenReturn(0L);
    when(syncRunRepository.countByStartedAtAfterAndStatus(any(), eq(ErpSyncStatus.SUCCESS))).thenReturn(0L);
    when(syncRunRepository.averageDurationMsAfter(any())).thenReturn(null);

    ErpSyncStatusResponseDTO dto = service(true).buildStatus();

    assertThat(dto.getStatus()).isEqualTo(ErpConnectionStatus.RUNNING);
  }

  @Test
  void buildStatus_returnsErrorWhenLastFailed() {
    ErpSyncRun latest = run(ErpSyncStatus.ERROR, 0, 0, 0, 1200, "connection refused");
    when(syncRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(latest));
    when(syncRunRepository.findAllByOrderByStartedAtDesc(any(Pageable.class))).thenReturn(List.of(latest));
    when(syncRunRepository.countByStartedAtAfter(any())).thenReturn(4L);
    when(syncRunRepository.countByStartedAtAfterAndStatus(any(), eq(ErpSyncStatus.SUCCESS))).thenReturn(3L);
    when(syncRunRepository.averageDurationMsAfter(any())).thenReturn(1500.5);

    ErpSyncStatusResponseDTO dto = service(true).buildStatus();

    assertThat(dto.getStatus()).isEqualTo(ErpConnectionStatus.ERROR);
    assertThat(dto.getLastErrorMessage()).isEqualTo("connection refused");
    assertThat(dto.getAvgLatencyMs()).isEqualTo(1500);
    assertThat(dto.getSuccessRate24h())
        .isCloseTo(new BigDecimal("0.7500").setScale(4, RoundingMode.HALF_UP),
            within(new BigDecimal("0.0001")));
    assertThat(dto.getRecentRuns()).hasSize(1);
    assertThat(dto.getRecentRuns().get(0).getErrorMessage()).isEqualTo("connection refused");
  }

  @Test
  void buildStatus_returnsErrorWhenLastIsPartial() {
    ErpSyncRun latest = run(ErpSyncStatus.PARTIAL, 12, 240, 0, 1840, "push-pauses: db down");
    when(syncRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(latest));
    when(syncRunRepository.findAllByOrderByStartedAtDesc(any(Pageable.class))).thenReturn(List.of(latest));
    when(syncRunRepository.countByStartedAtAfter(any())).thenReturn(1L);
    when(syncRunRepository.countByStartedAtAfterAndStatus(any(), eq(ErpSyncStatus.SUCCESS))).thenReturn(0L);
    when(syncRunRepository.averageDurationMsAfter(any())).thenReturn(1840.0);

    ErpSyncStatusResponseDTO dto = service(true).buildStatus();

    assertThat(dto.getStatus()).isEqualTo(ErpConnectionStatus.ERROR);
  }

  @Test
  void buildStatus_usesHistoryPageSize() {
    when(syncRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.empty());
    when(syncRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, HISTORY_PAGE_SIZE)))
        .thenReturn(List.of());
    when(syncRunRepository.countByStartedAtAfter(any())).thenReturn(0L);
    when(syncRunRepository.countByStartedAtAfterAndStatus(any(), eq(ErpSyncStatus.SUCCESS))).thenReturn(0L);
    when(syncRunRepository.averageDurationMsAfter(any())).thenReturn(null);

    service(true).buildStatus();
  }
}
