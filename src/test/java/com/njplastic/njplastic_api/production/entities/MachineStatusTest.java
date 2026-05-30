package com.njplastic.njplastic_api.production.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;

class MachineStatusTest {

  private MachineStatus sample() {
    return MachineStatus.builder()
        .id(UUID.fromString("7c8d9e0f-1a2b-3c4d-5e6f-7a8b9c0d1e2f"))
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .state(MachineState.AUTO_STOPPED)
        .reason("TROCA_DE_MOLDE")
        .message("Parada detectada")
        .startTime(OffsetDateTime.parse("2026-05-28T14:25:00Z"))
        .recordState(RecordState.CONFIRMED)
        .consecutiveCountAtCreation(3)
        .build();
  }

  @Test
  void toString_includesStateAndRecordState() {
    String text = sample().toString();
    assertThat(text)
        .contains("MachineStatus{")
        .contains("state=AUTO_STOPPED")
        .contains("recordState=CONFIRMED")
        .contains("reason=TROCA_DE_MOLDE");
  }

  @Test
  void onCreate_generatesIdAndTimestampsWhenMissing() {
    MachineStatus status = new MachineStatus();
    status.onCreate();
    assertThat(status.getId()).isNotNull();
    assertThat(status.getCreatedAt()).isNotNull();
    assertThat(status.getUpdatedAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndCreatedAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.now().minusHours(2);
    MachineStatus status = MachineStatus.builder().id(id).createdAt(created).build();
    status.onCreate();
    assertThat(status.getId()).isEqualTo(id);
    assertThat(status.getCreatedAt()).isEqualTo(created);
    assertThat(status.getUpdatedAt()).isNotNull();
  }

  @Test
  void onUpdate_refreshesUpdatedAt() {
    MachineStatus status = new MachineStatus();
    status.onUpdate();
    assertThat(status.getUpdatedAt()).isNotNull();
  }
}
