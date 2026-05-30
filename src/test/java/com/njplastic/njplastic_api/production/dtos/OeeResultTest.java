package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OeeResultTest {

  private OeeResult sample() {
    return OeeResult.builder()
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .availability(0.92)
        .performance(0.88)
        .quality(0.95)
        .oee(0.77)
        .partial(false)
        .build();
  }

  @Test
  void builder_populatesAllFields() {
    OeeResult result = sample();
    assertThat(result.getMachineId()).isEqualTo(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"));
    assertThat(result.getAvailability()).isEqualTo(0.92);
    assertThat(result.getPerformance()).isEqualTo(0.88);
    assertThat(result.getQuality()).isEqualTo(0.95);
    assertThat(result.getOee()).isEqualTo(0.77);
    assertThat(result.isPartial()).isFalse();
  }

  @Test
  void toString_includesAllFields() {
    String text = sample().toString();
    assertThat(text)
        .contains("OeeResult{")
        .contains("availability=0.92")
        .contains("performance=0.88")
        .contains("quality=0.95")
        .contains("oee=0.77")
        .contains("partial=false");
  }

  @Test
  void partialResult_keepsQualityAndOeeNull() {
    OeeResult partial = OeeResult.builder()
        .machineId(UUID.randomUUID())
        .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .availability(0.90)
        .performance(0.80)
        .partial(true)
        .build();
    assertThat(partial.getQuality()).isNull();
    assertThat(partial.getOee()).isNull();
    assertThat(partial.isPartial()).isTrue();
  }

  @Test
  void noArgsConstructor_yieldsDefaults() {
    OeeResult empty = new OeeResult();
    assertThat(empty.getMachineId()).isNull();
    assertThat(empty.getQuality()).isNull();
    assertThat(empty.isPartial()).isFalse();
  }
}
