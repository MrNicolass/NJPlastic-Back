package com.njplastic.njplastic_api.production.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Category of a manual production event (EP-BE-05 reopened, RFC §7.3.1). Maps
 * to the PostgreSQL ENUM {@code event_type} declared in V8__production_event.sql.
 * Follows the project-wide enum convention: each constant exposes a description
 * equal to its {@link #name()} and a case-insensitive
 * {@link #findByDescription(String)} lookup for inbound payloads.
 */
public enum EventType {

  TRAINING("TRAINING"),
  CLEANING("CLEANING"),
  MEETING("MEETING"),
  OTHER("OTHER");

  private final String description;

  EventType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  /**
   * @param description candidate description from an external payload
   * @return matching EventType, case-insensitive, or empty
   */
  public static Optional<EventType> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(t -> t.description.equalsIgnoreCase(description))
        .findFirst();
  }
}
