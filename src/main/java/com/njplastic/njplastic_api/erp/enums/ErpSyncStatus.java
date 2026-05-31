package com.njplastic.njplastic_api.erp.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Outcome of one ErpSyncScheduler execution persisted in
 * {@code erp_sync_run.status}. RUNNING marks an in-flight execution; SUCCESS
 * means every phase (order read, cycle write, pause write) finished without
 * raising; PARTIAL means at least one phase ran but a later phase failed;
 * ERROR means the execution aborted before completing any phase. Values must
 * match the CHECK constraint defined in V6__erp_sync_run.sql.
 */
public enum ErpSyncStatus {

  RUNNING("RUNNING"),
  SUCCESS("SUCCESS"),
  ERROR("ERROR"),
  PARTIAL("PARTIAL");

  private final String description;

  ErpSyncStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public static Optional<ErpSyncStatus> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(status -> status.description.equalsIgnoreCase(description))
        .findFirst();
  }
}