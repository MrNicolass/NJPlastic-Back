package com.njplastic.njplastic_api.production.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.enums.RecordState;

class ProductionCycleTest {

  private ProductionCycle sample() {
    return ProductionCycle.builder()
        .id(UUID.fromString("1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e"))
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .pulseTimestamp(OffsetDateTime.parse("2026-05-28T14:23:55Z"))
        .receivedAt(OffsetDateTime.parse("2026-05-28T14:23:55Z"))
        .sequence(42L)
        .intervalMs(2010)
        .state(RecordState.CONFIRMED)
        .build();
  }

  @Test
  void toString_includesKeyFields() {
    String text = sample().toString();
    assertThat(text)
        .contains("ProductionCycle{")
        .contains("sequence=42")
        .contains("intervalMs=2010")
        .contains("state=CONFIRMED");
  }

  @Test
  void onCreate_generatesIdAndCreatedAtWhenMissing() {
    ProductionCycle cycle = new ProductionCycle();
    cycle.onCreate();
    assertThat(cycle.getId()).isNotNull();
    assertThat(cycle.getCreatedAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndCreatedAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.now().minusMinutes(5);
    ProductionCycle cycle = ProductionCycle.builder().id(id).createdAt(created).build();
    cycle.onCreate();
    assertThat(cycle.getId()).isEqualTo(id);
    assertThat(cycle.getCreatedAt()).isEqualTo(created);
  }
}
