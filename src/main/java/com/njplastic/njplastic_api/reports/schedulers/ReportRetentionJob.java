package com.njplastic.njplastic_api.reports.schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.services.ReportProperties;
import com.njplastic.njplastic_api.reports.services.ReportScheduleService;

import lombok.RequiredArgsConstructor;

/**
 * Runs daily at 03:00 and deletes {@code report_history} rows past their
 * retention window, plus the artifact file on disk. Missing files are
 * tolerated so a manual cleanup elsewhere does not break the job.
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ReportProperties.class)
public class ReportRetentionJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportRetentionJob.class);

  private final ReportScheduleService scheduleService;
  private final ReportProperties properties;

  @Scheduled(cron = "0 0 3 * * *")
  void purge() {
    OffsetDateTime cutoff = OffsetDateTime.now().minusDays(properties.retentionDays());
    List<ReportHistory> expired = scheduleService.findExpiredHistory(cutoff);
    int deletedFiles = 0;
    for (ReportHistory row : expired) {
      try {
        Files.deleteIfExists(Path.of(row.getPath()));
        deletedFiles++;
      } catch (NoSuchFileException ignored) {
 // already gone - tolerate
      } catch (IOException ex) {
        LOGGER.warn("Failed to delete report file [{}]: {}", row.getPath(), ex.getMessage());
      }
      scheduleService.deleteHistory(row.getId());
    }
    if (!expired.isEmpty()) {
      LOGGER.info("Report retention purge: removed {} rows and {} files older than {}",
          expired.size(), deletedFiles, cutoff);
    }
  }
}
