package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.enums.MachineState;

class MachineStatusResponseDTOTest {

  @Test
  void builder_populatesAllFields() {
    MachineStatusEntryDTO entry = MachineStatusEntryDTO.builder()
        .id(UUID.randomUUID())
        .state(MachineState.RUNNING)
        .startTime(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .build();

    MachineStatusResponseDTO dto = MachineStatusResponseDTO.builder()
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .currentState(MachineState.RUNNING)
        .current(entry)
        .from(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .to(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .timeline(List.of(entry))
        .build();

    assertThat(dto.getMachineId()).isEqualTo(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"));
    assertThat(dto.getCurrentState()).isEqualTo(MachineState.RUNNING);
    assertThat(dto.getCurrent()).isSameAs(entry);
    assertThat(dto.getTimeline()).containsExactly(entry);
  }

  @Test
  void toString_usesTimelineSize() {
    MachineStatusResponseDTO empty = MachineStatusResponseDTO.builder()
        .machineId(UUID.randomUUID())
        .from(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .to(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .timeline(List.of())
        .build();
    assertThat(empty.toString()).contains("timelineSize=0");
  }

  @Test
  void toString_handlesNullTimeline() {
    MachineStatusResponseDTO dto = new MachineStatusResponseDTO();
    assertThat(dto.toString()).contains("timelineSize=0");
  }
}
