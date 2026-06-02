package com.njplastic.njplastic_api.reports.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.config.exceptions.BaseApiBadRequestException;
import com.njplastic.njplastic_api.config.exceptions.BaseApiInternalServerErrorException;
import com.njplastic.njplastic_api.production.dtos.MachineStatusEntryDTO;
import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO;
import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO.MachineReportSection;
import com.njplastic.njplastic_api.production.services.ReportService;
import com.njplastic.njplastic_api.reports.entities.ReportSchedule;
import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Generates report artifacts on disk and returns the file path for downstream
 * delivery. CSV is fully supported in the MVP; PDF and XLSX are rejected with
 * a clear 400 until a richer renderer is wired in (tracked separately).
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ReportProperties.class)
public class ReportGenerationService {

  private static final DateTimeFormatter PATH_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
  private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("HH-mm-ss");

  private final ReportProperties properties;
  private final ReportService reportService;
  private final ObjectMapper objectMapper;

  /**
   * Snapshot returned by a generation pass - both metadata and the bytes
   * needed for email delivery without re-reading the disk.
   *
   * @param path     filesystem path the artifact was written to
   * @param bytes    artifact contents
   * @param mimeType IANA mime type aligned with the format
   * @param filename suggested filename for the email attachment
   */
  public record GeneratedArtifact(Path path, byte[] bytes, String mimeType, String filename) {
  }

  /**
   * Build and persist an artifact for the given schedule.
   *
   * @param schedule the schedule driving the generation; carries type, format and params
   * @return the generated artifact metadata + bytes
   */
  public GeneratedArtifact generate(ReportSchedule schedule) {
    String content = renderContent(schedule.getType(), schedule.getFormat(), schedule.getParams());
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    OffsetDateTime now = OffsetDateTime.now();
    String filename = schedule.getType().name().toLowerCase()
        + "_" + FILE_FMT.format(now) + "." + schedule.getFormat().getExtension();
    Path target = Path.of(properties.storagePath(), PATH_FMT.format(now), filename);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, bytes);
    } catch (IOException ex) {
      throw new BaseApiInternalServerErrorException("Failed to persist report artifact: " + ex.getMessage());
    }
    return new GeneratedArtifact(target, bytes, schedule.getFormat().getMimeType(), filename);
  }

  private String renderContent(ReportType type, ReportFormat format, String paramsJson) {
    if (format != ReportFormat.CSV) {
      throw new BaseApiBadRequestException("Report format not yet supported: " + format);
    }
    return switch (type) {
      case SHIFT -> renderShiftCsv(paramsJson);
      case DAILY -> renderShiftCsv(paramsJson);
      case WEEKLY -> renderShiftCsv(paramsJson);
    };
  }

  private String renderShiftCsv(String paramsJson) {
    ScheduleParams parsed = parseParams(paramsJson);
    OffsetDateTime to = OffsetDateTime.now();
    OffsetDateTime from = to.minusHours(parsed.windowHours);
    AuthenticatedUser systemPrincipal = new AuthenticatedUser(
        new UUID(0L, 0L), "scheduler", UserRole.MANAGER, parsed.sector, parsed.shift);
    ShiftReportResponseDTO report = reportService.buildShiftReport(from, to, parsed.sector, parsed.shift, systemPrincipal);
    StringBuilder csv = new StringBuilder();
    csv.append("machine_code,sector,confirmed_cycles,availability,performance,quality,oee,")
        .append("manual_pauses_count,auto_stops_count\n");
    for (MachineReportSection section : report.getMachines()) {
      csv.append(safe(section.getMachine().getCode())).append(',')
          .append(safe(section.getMachine().getSector())).append(',')
          .append(section.getConfirmedCycles()).append(',')
          .append(formatOptional(section.getOee() == null ? null : section.getOee().getAvailability())).append(',')
          .append(formatOptional(section.getOee() == null ? null : section.getOee().getPerformance())).append(',')
          .append(formatOptional(section.getOee() == null ? null : section.getOee().getQuality())).append(',')
          .append(formatOptional(section.getOee() == null ? null : section.getOee().getOee())).append(',')
          .append(countSafe(section.getManualPauses())).append(',')
          .append(countSafe(section.getAutoStops())).append('\n');
    }
    return csv.toString();
  }

  private ScheduleParams parseParams(String json) {
    if (json == null || json.isBlank()) {
      return new ScheduleParams(null, null, 8);
    }
    try {
      JsonNode node = objectMapper.readTree(json);
      return new ScheduleParams(
          textOrNull(node, "sector"),
          textOrNull(node, "shift"),
          node.has("windowHours") ? node.get("windowHours").asInt(8) : 8);
    } catch (RuntimeException ex) {
      throw new BaseApiBadRequestException("Invalid schedule params JSON: " + ex.getMessage());
    }
  }

  private String textOrNull(JsonNode node, String field) {
    return node.has(field) && !node.get(field).isNull() ? node.get(field).asString() : null;
  }

  private String safe(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return '"' + value.replace("\"", "\"\"") + '"';
    }
    return value;
  }

  private String formatOptional(Object value) {
    return value == null ? "" : value.toString();
  }

  private int countSafe(List<MachineStatusEntryDTO> list) {
    return list == null ? 0 : list.size();
  }

  private record ScheduleParams(String sector, String shift, int windowHours) {
  }
}
