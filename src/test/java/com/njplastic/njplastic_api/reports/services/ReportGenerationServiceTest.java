package com.njplastic.njplastic_api.reports.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.njplastic.njplastic_api.config.exceptions.BaseApiBadRequestException;
import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO;
import com.njplastic.njplastic_api.production.services.ReportService;
import com.njplastic.njplastic_api.reports.entities.ReportSchedule;
import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

  @TempDir
  Path tempDir;

  @Mock
  private ReportProperties properties;

  @Mock
  private ReportService reportService;

  private ReportGenerationService service;

  @BeforeEach
  void setUp() {
    service = new ReportGenerationService(properties, reportService, new ObjectMapper());
    lenient().when(properties.storagePath()).thenReturn(tempDir.toString());
  }

  private ReportSchedule csvShiftSchedule(String params) {
    return ReportSchedule.builder()
        .id(UUID.randomUUID())
        .type(ReportType.SHIFT)
        .format(ReportFormat.CSV)
        .params(params)
        .cron("0 0 7 * * MON-FRI")
        .deliveryEmail("m@njplastic.com")
        .active(true)
        .build();
  }

  @Test
  void generate_csvShift_writesArtifact() {
    ShiftReportResponseDTO report = ShiftReportResponseDTO.builder()
        .periodStart(OffsetDateTime.now().minusHours(8))
        .periodEnd(OffsetDateTime.now())
        .machines(List.of())
        .build();
    when(reportService.buildShiftReport(any(), any(), any(), any(), any())).thenReturn(report);

    ReportGenerationService.GeneratedArtifact artifact = service.generate(csvShiftSchedule(null));

    assertThat(artifact.bytes()).isNotEmpty();
    assertThat(artifact.mimeType()).isEqualTo("text/csv");
    assertThat(artifact.filename()).endsWith(".csv");
    assertThat(Files.exists(artifact.path())).isTrue();
  }

  @Test
  void generate_unsupportedFormat_throws400() {
    ReportSchedule schedule = ReportSchedule.builder()
        .id(UUID.randomUUID())
        .type(ReportType.SHIFT)
        .format(ReportFormat.PDF)
        .cron("0 0 7 * * MON-FRI")
        .deliveryEmail("m@njplastic.com")
        .active(true)
        .build();

    assertThatThrownBy(() -> service.generate(schedule))
        .isInstanceOf(BaseApiBadRequestException.class)
        .hasMessageContaining("not yet supported");
  }

  @Test
  void generate_nullParams_usesDefaults() {
    ShiftReportResponseDTO report = ShiftReportResponseDTO.builder()
        .periodStart(OffsetDateTime.now().minusHours(8))
        .periodEnd(OffsetDateTime.now())
        .machines(List.of())
        .build();
    when(reportService.buildShiftReport(any(), any(), any(), any(), any())).thenReturn(report);

    ReportGenerationService.GeneratedArtifact artifact = service.generate(csvShiftSchedule(null));

    assertThat(artifact).isNotNull();
  }

  @Test
  void generate_invalidParamsJson_throws400() {
    assertThatThrownBy(() -> service.generate(csvShiftSchedule("{bad: json}")))
        .isInstanceOf(BaseApiBadRequestException.class)
        .hasMessageContaining("Invalid schedule params JSON");
  }
}
