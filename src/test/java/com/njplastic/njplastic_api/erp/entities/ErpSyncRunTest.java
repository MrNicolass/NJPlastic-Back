package com.njplastic.njplastic_api.erp.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;

class ErpSyncRunTest {

  private ErpSyncRun sample() {
    return ErpSyncRun.builder()
        .id(UUID.fromString("1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e"))
        .startedAt(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .finishedAt(OffsetDateTime.parse("2026-05-28T14:00:03Z"))
        .status(ErpSyncStatus.SUCCESS)
        .ordersRead(12)
        .cyclesWritten(240)
        .pausesWritten(5)
        .durationMs(1840)
        .errorMessage(null)
        .build();
  }

  @Test
  void toString_includesCountersAndStatus() {
    String text = sample().toString();
    assertThat(text)
        .contains("ErpSyncRun{")
        .contains("status=SUCCESS")
        .contains("ordersRead=12")
        .contains("cyclesWritten=240")
        .contains("pausesWritten=5")
        .contains("durationMs=1840");
  }

  @Test
  void onCreate_generatesIdAndStartedAtWhenMissing() {
    ErpSyncRun run = new ErpSyncRun();
    run.onCreate();
    assertThat(run.getId()).isNotNull();
    assertThat(run.getStartedAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndStartedAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-01T10:00:00Z");
    ErpSyncRun run = ErpSyncRun.builder().id(id).startedAt(startedAt).build();
    run.onCreate();
    assertThat(run.getId()).isEqualTo(id);
    assertThat(run.getStartedAt()).isEqualTo(startedAt);
  }

  @Test
  void buildersAndAccessors_roundTripValues() {
    ErpSyncRun run = sample();
    assertThat(run.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    run.setStatus(ErpSyncStatus.ERROR);
    run.setErrorMessage("ERP connection timeout");
    assertThat(run.getStatus()).isEqualTo(ErpSyncStatus.ERROR);
    assertThat(run.getErrorMessage()).isEqualTo("ERP connection timeout");
  }
}
