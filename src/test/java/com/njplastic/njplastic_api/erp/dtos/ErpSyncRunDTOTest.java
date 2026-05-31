package com.njplastic.njplastic_api.erp.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;

class ErpSyncRunDTOTest {

  private ErpSyncRunDTO sample() {
    return ErpSyncRunDTO.builder()
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
        .contains("ErpSyncRunDTO{")
        .contains("status=SUCCESS")
        .contains("ordersRead=12")
        .contains("cyclesWritten=240")
        .contains("pausesWritten=5")
        .contains("durationMs=1840");
  }

  @Test
  void buildersAndAccessors_roundTripValues() {
    ErpSyncRunDTO dto = sample();
    assertThat(dto.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    assertThat(dto.getOrdersRead()).isEqualTo(12);
    assertThat(dto.getCyclesWritten()).isEqualTo(240);
    assertThat(dto.getPausesWritten()).isEqualTo(5);
    assertThat(dto.getDurationMs()).isEqualTo(1840);
    assertThat(dto.getErrorMessage()).isNull();
  }

  @Test
  void settersUpdateFields() {
    ErpSyncRunDTO dto = new ErpSyncRunDTO();
    dto.setStatus(ErpSyncStatus.ERROR);
    dto.setErrorMessage("oops");
    assertThat(dto.getStatus()).isEqualTo(ErpSyncStatus.ERROR);
    assertThat(dto.getErrorMessage()).isEqualTo("oops");
  }
}
