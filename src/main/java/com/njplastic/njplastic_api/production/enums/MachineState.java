package com.njplastic.njplastic_api.production.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Operational state of an injection machine, persisted in machine_status.state
 * (PostgreSQL TYPE machine_state). RUNNING and PAUSED describe normal operation
 * with isolated pauses (RN06); AUTO_STOPPED is reached after N consecutive
 * pauses
 * (RN09, RF17); OFFLINE is produced by the watchdog when no pulse arrives
 * within
 * the configured window. Values must match the CREATE TYPE in V1__init.sql,
 * since
 * Hibernate serializes by name().
 */
public enum MachineState {

  RUNNING("RUNNING"),
  PAUSED("PAUSED"),
  AUTO_STOPPED("AUTO_STOPPED"),
  OFFLINE("OFFLINE");

  private final String description;

  MachineState(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public static Optional<MachineState> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(state -> state.description.equalsIgnoreCase(description))
        .findFirst();
  }
}