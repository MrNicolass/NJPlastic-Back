package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.enums.MachineState;

class MachineSummaryDTOTest {

  private MachineSummaryDTO sample() {
    return MachineSummaryDTO.builder()
        .id(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .code("MAQ-01")
        .description("Injection molder 80t line A")
        .sector("INJECAO")
        .standardCycleMs(2000)
        .consecutivePausesToStop(3)
        .active(true)
        .currentState(MachineState.RUNNING)
        .build();
  }

  @Test
  void builder_populatesAllFields() {
    MachineSummaryDTO dto = sample();
    assertThat(dto.getCode()).isEqualTo("MAQ-01");
    assertThat(dto.getSector()).isEqualTo("INJECAO");
    assertThat(dto.getStandardCycleMs()).isEqualTo(2000);
    assertThat(dto.getConsecutivePausesToStop()).isEqualTo(3);
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getCurrentState()).isEqualTo(MachineState.RUNNING);
  }

  @Test
  void toString_includesAllFields() {
    String text = sample().toString();
    assertThat(text)
        .contains("MachineSummaryDTO{")
        .contains("code=MAQ-01")
        .contains("sector=INJECAO")
        .contains("active=true")
        .contains("currentState=RUNNING");
  }
}
