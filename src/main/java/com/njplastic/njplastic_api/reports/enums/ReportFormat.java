package com.njplastic.njplastic_api.reports.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Output format for a scheduled report. The MVP renderer supports CSV
 * end-to-end; PDF and XLSX are accepted at the API surface but generation
 * is deferred until the artifact-rendering library is added.
 */
public enum ReportFormat {

  CSV("CSV", "text/csv", "csv"),
  PDF("PDF", "application/pdf", "pdf"),
  XLSX("XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

  private final String description;
  private final String mimeType;
  private final String extension;

  ReportFormat(String description, String mimeType, String extension) {
    this.description = description;
    this.mimeType = mimeType;
    this.extension = extension;
  }

  public String getDescription() {
    return description;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getExtension() {
    return extension;
  }

 /**
 * @param description candidate description from an external payload
 * @return matching ReportFormat, case-insensitive, or empty
 */
  public static Optional<ReportFormat> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(t -> t.description.equalsIgnoreCase(description))
        .findFirst();
  }
}
