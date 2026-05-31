package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.enums.MachineState;

class MachineStatusEntryDTOTest {

  private MachineStatusEntryDTO sample() {
    return MachineStatusEntryDTO.builder()
        .id(UUID.fromString("7c8d9e0f-1a2b-3c4d-5e6f-7a8b9c0d1e2f"))
        .state(MachineState.AUTO_STOPPED)
        .reason("TROCA_DE_MOLDE")
        .message("Stopped after 3 pauses")
        .startTime(OffsetDateTime.parse("2026-05-28T14:25:00Z"))
        .endTime(OffsetDateTime.parse("2026-05-28T14:40:00Z"))
        .reasonAuthorId(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"))
        .consecutiveCountAtCreation(3)
        .build();
  }

  @Test
  void builder_populatesAllFields() {
    MachineStatusEntryDTO dto = sample();
    assertThat(dto.getState()).isEqualTo(MachineState.AUTO_STOPPED);
    assertThat(dto.getReason()).isEqualTo("TROCA_DE_MOLDE");
    assertThat(dto.getMessage()).isEqualTo("Stopped after 3 pauses");
    assertThat(dto.getConsecutiveCountAtCreation()).isEqualTo(3);
    assertThat(dto.getEndTime()).isNotNull();
  }

  @Test
  void toString_includesStateAndReason() {
    String text = sample().toString();
    assertThat(text)
        .contains("MachineStatusEntryDTO{")
        .contains("state=AUTO_STOPPED")
        .contains("reason=TROCA_DE_MOLDE")
        .contains("consecutiveCountAtCreation=3");
  }

  @Test
  void settersUpdateFields() {
    MachineStatusEntryDTO dto = new MachineStatusEntryDTO();
    dto.setState(MachineState.PAUSED);
    dto.setReason("test");
    assertThat(dto.getState()).isEqualTo(MachineState.PAUSED);
    assertThat(dto.getReason()).isEqualTo("test");
  }
}
