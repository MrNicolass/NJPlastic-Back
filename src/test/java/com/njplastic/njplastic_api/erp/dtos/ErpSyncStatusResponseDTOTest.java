package com.njplastic.njplastic_api.erp.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.erp.enums.ErpConnectionStatus;
import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;

class ErpSyncStatusResponseDTOTest {

  private ErpSyncRunDTO run() {
    return ErpSyncRunDTO.builder().status(ErpSyncStatus.SUCCESS).build();
  }

  @Test
  void toString_includesRecentRunsSize() {
    ErpSyncStatusResponseDTO dto = ErpSyncStatusResponseDTO.builder()
        .status(ErpConnectionStatus.OPERATIONAL)
        .lastSyncAt(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .nextWindowAt(OffsetDateTime.parse("2026-05-28T14:01:00Z"))
        .successRate24h(new BigDecimal("0.9583"))
        .avgLatencyMs(1840)
        .ordersReadLastRun(12)
        .cyclesWrittenLastRun(240)
        .pausesWrittenLastRun(5)
        .lastErrorMessage(null)
        .recentRuns(List.of(run(), run()))
        .build();

    String text = dto.toString();
    assertThat(text)
        .contains("ErpSyncStatusResponseDTO{")
        .contains("status=OPERATIONAL")
        .contains("avgLatencyMs=1840")
        .contains("recentRunsSize=2");
  }

  @Test
  void toString_recentRunsSizeIsZeroWhenNull() {
    ErpSyncStatusResponseDTO dto = ErpSyncStatusResponseDTO.builder()
        .status(ErpConnectionStatus.DISABLED)
        .build();
    assertThat(dto.toString()).contains("recentRunsSize=0");
  }

  @Test
  void buildersAndAccessors_roundTripValues() {
    ErpSyncStatusResponseDTO dto = ErpSyncStatusResponseDTO.builder()
        .status(ErpConnectionStatus.ERROR)
        .recentRuns(Collections.emptyList())
        .lastErrorMessage("ERP connection timeout")
        .build();
    assertThat(dto.getStatus()).isEqualTo(ErpConnectionStatus.ERROR);
    assertThat(dto.getRecentRuns()).isEmpty();
    assertThat(dto.getLastErrorMessage()).isEqualTo("ERP connection timeout");
  }

  @Test
  void settersUpdateFields() {
    ErpSyncStatusResponseDTO dto = new ErpSyncStatusResponseDTO();
    dto.setStatus(ErpConnectionStatus.RUNNING);
    dto.setAvgLatencyMs(123);
    dto.setRecentRuns(List.of(run()));
    assertThat(dto.getStatus()).isEqualTo(ErpConnectionStatus.RUNNING);
    assertThat(dto.getAvgLatencyMs()).isEqualTo(123);
    assertThat(dto.getRecentRuns()).hasSize(1);
  }
}
