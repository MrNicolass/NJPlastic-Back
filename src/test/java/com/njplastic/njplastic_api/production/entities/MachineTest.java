package com.njplastic.njplastic_api.production.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MachineTest {

  private Machine sample() {
    return Machine.builder()
        .id(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .code("MAQ-01")
        .description("Injetora 80t linha A")
        .sector("INJECAO")
        .standardCycleMs(2000)
        .toleranceFactor(new BigDecimal("1.50"))
        .consecutivePausesToStop(3)
        .offlineWindowMs(60000)
        .active(true)
        .build();
  }

  @Test
  void toString_includesIdentifyingFields() {
    String text = sample().toString();
    assertThat(text)
        .contains("Machine{")
        .contains("code=MAQ-01")
        .contains("sector=INJECAO")
        .contains("standardCycleMs=2000")
        .contains("active=true");
  }

  @Test
  void onCreate_generatesIdAndTimestampsWhenMissing() {
    Machine machine = new Machine();
    machine.onCreate();
    assertThat(machine.getId()).isNotNull();
    assertThat(machine.getCreatedAt()).isNotNull();
    assertThat(machine.getUpdatedAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndCreatedAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.now().minusDays(3);
    Machine machine = Machine.builder().id(id).createdAt(created).build();
    machine.onCreate();
    assertThat(machine.getId()).isEqualTo(id);
    assertThat(machine.getCreatedAt()).isEqualTo(created);
    assertThat(machine.getUpdatedAt()).isNotNull();
  }

  @Test
  void onUpdate_refreshesUpdatedAt() {
    Machine machine = new Machine();
    machine.onUpdate();
    assertThat(machine.getUpdatedAt()).isNotNull();
  }
}
