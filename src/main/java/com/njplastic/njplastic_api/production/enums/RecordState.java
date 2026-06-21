package com.njplastic.njplastic_api.production.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Lifecycle of a production record, shared by production_cycle.state and
 * machine_status.record_state (PostgreSQL TYPE record_state). PENDING is the
 * initial state on creation; CONFIRMED is reached after clock validation 
 * and pause evaluation; SYNCED marks records written to the ERP;
 * DISCARDED marks cycles rejected by the clock-drift window. Values must
 * match the CREATE TYPE in V1__init.sql, since Hibernate serializes by name.
 */
public enum RecordState {

  PENDING("PENDING"),
  CONFIRMED("CONFIRMED"),
  SYNCED("SYNCED"),
  DISCARDED("DISCARDED");

  private final String description;

  RecordState(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public static Optional<RecordState> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(state -> state.description.equalsIgnoreCase(description))
        .findFirst();
  }
}