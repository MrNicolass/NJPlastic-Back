package com.njplastic.njplastic_api.reports.controllers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.reports.dtos.ReportHistoryResponseDTO;
import com.njplastic.njplastic_api.reports.dtos.ReportScheduleRequestDTO;
import com.njplastic.njplastic_api.reports.dtos.ReportScheduleResponseDTO;
import com.njplastic.njplastic_api.reports.entities.ReportSchedule;
import com.njplastic.njplastic_api.reports.enums.ReportType;
import com.njplastic.njplastic_api.reports.services.ReportDownloadService;
import com.njplastic.njplastic_api.reports.services.ReportScheduleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Report scheduling endpoint. Backs the Manager-only scheduling cards and
 * the LEADER/MANAGER library tab. Actual generation lives in
 * {@code ReportSchedulerJob}.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Reports Schedule", description = "Report library and scheduling")
@RequiredArgsConstructor
public class ReportsScheduleController {

  private final ReportScheduleService service;
  private final ReportDownloadService downloadService;

  @GetMapping("/history")
  @PreAuthorize("hasAnyRole('LEADER','MANAGER')")
  @Operation(summary = "Paginated report library", description = "Retention is 90 days. Filters apply with AND semantics.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of generated reports", content = @Content(schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public Page<ReportHistoryResponseDTO> history(
      @RequestParam(required = false) ReportType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @PageableDefault(size = 50, sort = "generatedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
    return service.findHistory(type, from, to, pageable).map(ReportHistoryResponseDTO::from);
  }

  @PostMapping("/schedule")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "Create a scheduled report", description = "Author UUID is taken from the JWT. The schedule runs as soon as its cron matches the next minute tick.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Schedule created", content = @Content(schema = @Schema(implementation = ReportScheduleResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public ReportScheduleResponseDTO createSchedule(
      @Valid @RequestBody ReportScheduleRequestDTO request,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    ReportSchedule entity = ReportSchedule.builder()
        .type(request.getType())
        .params(request.getParams())
        .cron(request.getCron())
        .deliveryEmail(request.getDeliveryEmail())
        .format(request.getFormat())
        .active(true)
        .createdBy(principal.id())
        .build();
    return ReportScheduleResponseDTO.from(service.create(entity));
  }

  @DeleteMapping("/schedule/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "Delete a scheduled report", description = "Past generations stay in report_history.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Schedule deleted"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown schedule id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public void deleteSchedule(@PathVariable UUID id) {
    service.delete(id);
  }

  @GetMapping("/schedule")
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "List active schedules", description = "Backs the Manager-only schedules grid.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Active schedules", content = @Content(schema = @Schema(implementation = ReportScheduleResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public List<ReportScheduleResponseDTO> listSchedules() {
    return service.findAllActive().stream().map(ReportScheduleResponseDTO::from).toList();
  }

  @GetMapping("/{id}/download")
  @PreAuthorize("hasAnyRole('LEADER','MANAGER')")
  @Operation(summary = "Download a stored report artifact", description = "Streams the binary persisted under app.reports.storagePath.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Report artifact stream", content = @Content(mediaType = "application/octet-stream")),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown report id or missing artifact", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public ResponseEntity<Resource> downloadReport(@PathVariable UUID id) {
    ReportDownloadService.ResolvedArtifact artifact = downloadService.resolve(id);
    String filename = artifact.resource().getFilename();
    if (filename == null || filename.isBlank()) {
      filename = "report-" + id + "." + artifact.history().getFormat().getExtension();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(artifact.history().getFormat().getMimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentLength(artifact.history().getSizeBytes())
        .body(artifact.resource());
  }
}
