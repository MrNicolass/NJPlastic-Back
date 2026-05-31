package com.njplastic.njplastic_api.erp.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ErpConnectionStatusTest {

  @Test
  void getDescription_equalsName() {
    for (ErpConnectionStatus status : ErpConnectionStatus.values()) {
      assertThat(status.getDescription()).isEqualTo(status.name());
    }
  }

  @Test
  void findByDescription_matchesIgnoringCase() {
    assertThat(ErpConnectionStatus.findByDescription("disabled")).contains(ErpConnectionStatus.DISABLED);
    assertThat(ErpConnectionStatus.findByDescription("OPERATIONAL")).contains(ErpConnectionStatus.OPERATIONAL);
    assertThat(ErpConnectionStatus.findByDescription("Running")).contains(ErpConnectionStatus.RUNNING);
    assertThat(ErpConnectionStatus.findByDescription("error")).contains(ErpConnectionStatus.ERROR);
  }

  @Test
  void findByDescription_returnsEmptyForUnknown() {
    assertThat(ErpConnectionStatus.findByDescription("UNKNOWN")).isEmpty();
  }

  @Test
  void findByDescription_returnsEmptyForNull() {
    assertThat(ErpConnectionStatus.findByDescription(null)).isEqualTo(Optional.empty());
  }
}
