package com.njplastic.njplastic_api.reports.services;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for scheduled report generation. Bound from "app.reports.*".
 * {@code storagePath} is the directory where artifacts are persisted before
 * email delivery; {@code retentionDays} caps how long {@code report_history}
 * rows (and the underlying files) are kept on disk.
 */
@ConfigurationProperties(prefix = "app.reports")
public record ReportProperties(
    String storagePath,
    int retentionDays) {
}
