package com.njplastic.njplastic_api.reports.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Category of a scheduled report (sub-task 5). Each type drives a
 * distinct renderer in {@code ReportGenerationService}. Follows the
 * project-wide enum convention: description equals {@link #name},
 * {@link #findByDescription(String)} powers inbound payload parsing.
 */
public enum ReportType {

  SHIFT("SHIFT"),
  DAILY("DAILY"),
  WEEKLY("WEEKLY");

  private final String description;

  ReportType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

 /**
 * @param description candidate description from an external payload
 * @return matching ReportType, case-insensitive, or empty
 */
  public static Optional<ReportType> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(t -> t.description.equalsIgnoreCase(description))
        .findFirst();
  }
}
