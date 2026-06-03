package com.njplastic.njplastic_api.reports.schedulers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import com.njplastic.njplastic_api.auth.services.EmailService;
import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.entities.ReportSchedule;
import com.njplastic.njplastic_api.reports.services.ReportGenerationService;
import com.njplastic.njplastic_api.reports.services.ReportGenerationService.GeneratedArtifact;
import com.njplastic.njplastic_api.reports.services.ReportScheduleService;

import lombok.RequiredArgsConstructor;

/**
 * Runs every minute and triggers any {@link ReportSchedule} whose cron
 * matched the previous minute window. {@code @EnableScheduling} is already
 * wired by {@link com.njplastic.njplastic_api.production.mqtt.MqttConfig}.
 * Failures during a single schedule are logged and the loop keeps going - a
 * bad cron expression must not stall every other schedule.
 */
@Component
@RequiredArgsConstructor
public class ReportSchedulerJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportSchedulerJob.class);

  private final ReportScheduleService scheduleService;
  private final ReportGenerationService generationService;
  private final EmailService emailService;

  @Scheduled(cron = "0 * * * * *")
  void tick() {
    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
    LocalDateTime lastMinute = now.minusMinutes(1);
    for (ReportSchedule schedule : scheduleService.findAllActive()) {
      if (!matchesCron(schedule.getCron(), lastMinute, now)) {
        continue;
      }
      runOnce(schedule);
    }
  }

  private boolean matchesCron(String cron, LocalDateTime from, LocalDateTime to) {
    try {
      CronExpression expr = CronExpression.parse(cron);
      LocalDateTime next = expr.next(from);
      return next != null && (next.isEqual(to) || (next.isAfter(from) && next.isBefore(to)));
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Skipping schedule with invalid cron [{}]: {}", cron, ex.getMessage());
      return false;
    }
  }

  private void runOnce(ReportSchedule schedule) {
    try {
      GeneratedArtifact artifact = generationService.generate(schedule);
      scheduleService.recordGeneration(ReportHistory.builder()
          .scheduleId(schedule.getId())
          .type(schedule.getType())
          .format(schedule.getFormat())
          .path(artifact.path().toString())
          .sizeBytes(artifact.bytes().length)
          .build());
      emailService.sendReportDelivery(
          schedule.getDeliveryEmail(),
          "NJPlastic - " + schedule.getType().name() + " report",
          "Attached you'll find the " + schedule.getType().name() + " report generated at "
              + Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()) + ".\n\n--\nNJPlastic",
          artifact.bytes(),
          artifact.filename(),
          artifact.mimeType());
      LOGGER.info("Generated and delivered report for schedule {}", schedule.getId());
    } catch (RuntimeException ex) {
      LOGGER.warn("Report generation failed for schedule [{}]: {}", schedule.getId(), ex.getMessage());
    }
  }
}
