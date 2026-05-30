package com.njplastic.njplastic_api.production.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class MachineStateTest {

  @Test
  void getDescription_equalsName() {
    for (MachineState state : MachineState.values()) {
      assertThat(state.getDescription()).isEqualTo(state.name());
    }
  }

  @Test
  void findByDescription_matchesIgnoringCase() {
    assertThat(MachineState.findByDescription("running")).contains(MachineState.RUNNING);
    assertThat(MachineState.findByDescription("PAUSED")).contains(MachineState.PAUSED);
    assertThat(MachineState.findByDescription("Auto_Stopped")).contains(MachineState.AUTO_STOPPED);
    assertThat(MachineState.findByDescription("OFFLINE")).contains(MachineState.OFFLINE);
  }

  @Test
  void findByDescription_returnsEmptyForUnknown() {
    assertThat(MachineState.findByDescription("SUPER_RUNNING")).isEmpty();
  }

  @Test
  void findByDescription_returnsEmptyForNull() {
    assertThat(MachineState.findByDescription(null)).isEqualTo(Optional.empty());
  }
}
