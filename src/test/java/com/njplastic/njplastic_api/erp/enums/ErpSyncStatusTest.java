package com.njplastic.njplastic_api.erp.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ErpSyncStatusTest {

  @Test
  void getDescription_equalsName() {
    for (ErpSyncStatus status : ErpSyncStatus.values()) {
      assertThat(status.getDescription()).isEqualTo(status.name());
    }
  }

  @Test
  void findByDescription_matchesIgnoringCase() {
    assertThat(ErpSyncStatus.findByDescription("running")).contains(ErpSyncStatus.RUNNING);
    assertThat(ErpSyncStatus.findByDescription("SUCCESS")).contains(ErpSyncStatus.SUCCESS);
    assertThat(ErpSyncStatus.findByDescription("Error")).contains(ErpSyncStatus.ERROR);
    assertThat(ErpSyncStatus.findByDescription("partial")).contains(ErpSyncStatus.PARTIAL);
  }

  @Test
  void findByDescription_returnsEmptyForUnknown() {
    assertThat(ErpSyncStatus.findByDescription("CANCELLED")).isEmpty();
  }

  @Test
  void findByDescription_returnsEmptyForNull() {
    assertThat(ErpSyncStatus.findByDescription(null)).isEqualTo(Optional.empty());
  }
}
