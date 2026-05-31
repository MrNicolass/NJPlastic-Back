package com.njplastic.njplastic_api.erp.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Top-level connection state returned by GET /erp/sync/status (UC08). Aggregates
 * the runtime state of the ERP integration into one of four labels consumed by
 * the Gestor screen (Figura 26a/b). DISABLED is set when
 * {@code app.datasource.erp.enabled=false}; OPERATIONAL is the green-state
 * label used when there is no run history yet or the most recent run succeeded;
 * RUNNING reflects an in-flight execution; ERROR covers both ERROR and PARTIAL
 * outcomes of the most recent run. Distinct from {@code ErpSyncStatus} which
 * describes one isolated execution, while this enum describes the aggregated
 * integration state.
 */
public enum ErpConnectionStatus {

  DISABLED("DISABLED"),
  OPERATIONAL("OPERATIONAL"),
  RUNNING("RUNNING"),
  ERROR("ERROR");

  private final String description;

  ErpConnectionStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public static Optional<ErpConnectionStatus> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(status -> status.description.equalsIgnoreCase(description))
        .findFirst();
  }
}
