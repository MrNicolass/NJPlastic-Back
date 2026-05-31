package com.njplastic.njplastic_api.erp.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ErpSyncPhaseTest {

  @Test
  void getDescription_isHyphenatedLabel() {
    assertThat(ErpSyncPhase.REFRESH_ORDERS.getDescription()).isEqualTo("refresh-orders");
    assertThat(ErpSyncPhase.PUSH_CYCLES.getDescription()).isEqualTo("push-cycles");
    assertThat(ErpSyncPhase.PUSH_PAUSES.getDescription()).isEqualTo("push-pauses");
  }

  @Test
  void findByDescription_matchesIgnoringCase() {
    assertThat(ErpSyncPhase.findByDescription("REFRESH-ORDERS")).contains(ErpSyncPhase.REFRESH_ORDERS);
    assertThat(ErpSyncPhase.findByDescription("push-cycles")).contains(ErpSyncPhase.PUSH_CYCLES);
    assertThat(ErpSyncPhase.findByDescription("Push-Pauses")).contains(ErpSyncPhase.PUSH_PAUSES);
  }

  @Test
  void findByDescription_returnsEmptyForUnknown() {
    assertThat(ErpSyncPhase.findByDescription("PUSH_NOTHING")).isEmpty();
  }

  @Test
  void findByDescription_returnsEmptyForNull() {
    assertThat(ErpSyncPhase.findByDescription(null)).isEqualTo(Optional.empty());
  }
}
