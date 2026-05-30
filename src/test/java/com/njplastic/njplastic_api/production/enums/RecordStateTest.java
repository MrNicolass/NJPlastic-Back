package com.njplastic.njplastic_api.production.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class RecordStateTest {

  @Test
  void getDescription_equalsName() {
    for (RecordState state : RecordState.values()) {
      assertThat(state.getDescription()).isEqualTo(state.name());
    }
  }

  @Test
  void findByDescription_matchesIgnoringCase() {
    assertThat(RecordState.findByDescription("pending")).contains(RecordState.PENDING);
    assertThat(RecordState.findByDescription("CONFIRMED")).contains(RecordState.CONFIRMED);
    assertThat(RecordState.findByDescription("Synced")).contains(RecordState.SYNCED);
    assertThat(RecordState.findByDescription("DISCARDED")).contains(RecordState.DISCARDED);
  }

  @Test
  void findByDescription_returnsEmptyForUnknown() {
    assertThat(RecordState.findByDescription("UNKNOWN")).isEmpty();
  }

  @Test
  void findByDescription_returnsEmptyForNull() {
    assertThat(RecordState.findByDescription(null)).isEqualTo(Optional.empty());
  }
}
