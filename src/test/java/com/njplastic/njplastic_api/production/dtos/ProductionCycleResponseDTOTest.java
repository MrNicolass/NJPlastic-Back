package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.enums.RecordState;

class ProductionCycleResponseDTOTest {

  @Test
  void builder_populatesAllFields() {
    ProductionCycleResponseDTO dto = ProductionCycleResponseDTO.builder()
        .id(UUID.fromString("1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e"))
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .pulseTimestamp(OffsetDateTime.parse("2026-05-28T14:23:55Z"))
        .receivedAt(OffsetDateTime.parse("2026-05-28T14:23:55Z"))
        .sequence(42L)
        .intervalMs(2010)
        .state(RecordState.CONFIRMED)
        .build();

    assertThat(dto.getSequence()).isEqualTo(42L);
    assertThat(dto.getIntervalMs()).isEqualTo(2010);
    assertThat(dto.getState()).isEqualTo(RecordState.CONFIRMED);
  }

  @Test
  void toString_includesFields() {
    ProductionCycleResponseDTO dto = ProductionCycleResponseDTO.builder()
        .id(UUID.randomUUID())
        .sequence(7L)
        .state(RecordState.DISCARDED)
        .build();
    assertThat(dto.toString())
        .contains("ProductionCycleResponseDTO{")
        .contains("sequence=7")
        .contains("state=DISCARDED");
  }
}
