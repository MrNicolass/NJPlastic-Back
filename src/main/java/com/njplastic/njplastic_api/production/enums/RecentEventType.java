package com.njplastic.njplastic_api.production.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Category of an entry in the Leader's "Eventos recentes" panel (EP-FE-05,
 * RFC §7.3.2 EP-FE-05 item 6). Used only as a discriminator in the response
 * payload of {@code GET /events/recent} - there is no corresponding column
 * in the database because each entry is sourced from a different table and
 * the type is computed at projection time. Follows the project-wide enum
 * convention: each constant exposes a description equal to its
 * {@link #name()} and a case-insensitive {@link #findByDescription(String)}
 * lookup.
 */
public enum RecentEventType {

  MANUAL_EVENT("MANUAL_EVENT"),
  MANUAL_PAUSE("MANUAL_PAUSE"),
  AUTO_STOP("AUTO_STOP"),
  STOP_MESSAGE_EDIT("STOP_MESSAGE_EDIT");

  private final String description;

  RecentEventType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  /**
   * @param description candidate description from an external payload
   * @return matching RecentEventType, case-insensitive, or empty
   */
  public static Optional<RecentEventType> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(t -> t.description.equalsIgnoreCase(description))
        .findFirst();
  }
}
