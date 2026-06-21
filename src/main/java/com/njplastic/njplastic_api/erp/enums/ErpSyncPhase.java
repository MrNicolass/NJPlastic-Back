package com.njplastic.njplastic_api.erp.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Phases of one {@code ErpSyncService.runSync} execution. Used to label the
 * prefix of error messages persisted in {@code erp_sync_run.error_message} so
 * the Gestor screen can tell which phase aborted the run. REFRESH_ORDERS reads
 * open orders from the ERP and rewrites {@code production_order_cache};
 * PUSH_CYCLES writes confirmed production_cycle rows to the ERP; PUSH_PAUSES
 * writes confirmed PAUSED/AUTO_STOPPED machine_status rows to the ERP.
 */
public enum ErpSyncPhase {

  REFRESH_ORDERS("refresh-orders"),
  PUSH_CYCLES("push-cycles"),
  PUSH_PAUSES("push-pauses");

  private final String description;

  ErpSyncPhase(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public static Optional<ErpSyncPhase> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(phase -> phase.description.equalsIgnoreCase(description))
        .findFirst();
  }
}
