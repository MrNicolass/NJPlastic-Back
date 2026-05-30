package com.njplastic.njplastic_api.production.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class QualityRecordTest {

  private QualityRecord sample() {
    return QualityRecord.builder()
        .id(UUID.fromString("5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b"))
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .erpOrderId("OS-2026-00123")
        .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .goodCount(950)
        .totalCount(1000)
        .build();
  }

  @Test
  void toString_includesKeyFields() {
    String text = sample().toString();
    assertThat(text)
        .contains("QualityRecord{")
        .contains("erpOrderId=OS-2026-00123")
        .contains("goodCount=950")
        .contains("totalCount=1000");
  }

  @Test
  void onCreate_generatesIdAndRegisteredAtWhenMissing() {
    QualityRecord record = new QualityRecord();
    record.onCreate();
    assertThat(record.getId()).isNotNull();
    assertThat(record.getRegisteredAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndRegisteredAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime registered = OffsetDateTime.now().minusDays(1);
    QualityRecord record = QualityRecord.builder().id(id).registeredAt(registered).build();
    record.onCreate();
    assertThat(record.getId()).isEqualTo(id);
    assertThat(record.getRegisteredAt()).isEqualTo(registered);
  }
}
