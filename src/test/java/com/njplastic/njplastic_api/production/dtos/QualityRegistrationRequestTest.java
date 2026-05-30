package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class QualityRegistrationRequestTest {

  private QualityRegistrationRequest sample() {
    return QualityRegistrationRequest.builder()
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .erpOrderId("OS-2026-00123")
        .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .goodCount(950)
        .totalCount(1000)
        .build();
  }

  @Test
  void builder_populatesAllFields() {
    QualityRegistrationRequest request = sample();
    assertThat(request.getErpOrderId()).isEqualTo("OS-2026-00123");
    assertThat(request.getGoodCount()).isEqualTo(950);
    assertThat(request.getTotalCount()).isEqualTo(1000);
  }

  @Test
  void toString_includesNonSensitiveFields() {
    String text = sample().toString();
    assertThat(text)
        .contains("QualityRegistrationRequest{")
        .contains("erpOrderId=OS-2026-00123")
        .contains("goodCount=950")
        .contains("totalCount=1000");
  }

  @Test
  void settersUpdateFields() {
    QualityRegistrationRequest request = new QualityRegistrationRequest();
    UUID id = UUID.randomUUID();
    request.setMachineId(id);
    request.setGoodCount(10);
    request.setTotalCount(20);
    assertThat(request.getMachineId()).isEqualTo(id);
    assertThat(request.getGoodCount()).isEqualTo(10);
    assertThat(request.getTotalCount()).isEqualTo(20);
  }
}
