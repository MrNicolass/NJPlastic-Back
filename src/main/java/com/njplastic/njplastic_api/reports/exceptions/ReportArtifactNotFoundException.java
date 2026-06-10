package com.njplastic.njplastic_api.reports.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;

/**
 * Raised when a {@code report_history} row exists but the underlying file on
 * disk has gone missing (e.g. retention reaper ran between the listing call
 * and the download call). Returns HTTP 404 so the caller can refresh the
 * library tab and discover the row is gone.
 */
public class ReportArtifactNotFoundException extends BaseApiNotFoundException {

  public ReportArtifactNotFoundException(String message) {
    super(message);
  }
}
