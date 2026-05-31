package com.njplastic.njplastic_api.production.controllers;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO;
import com.njplastic.njplastic_api.production.services.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Consolidated reports of the production aggregate (RF15). Returns JSON
 * only; CSV/PDF export is part of the frontend epic EP-FE-07 (RF16).
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Reports", description = "Consolidated production reports (EP-BE-05 / RF15)")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  @GetMapping("/shift")
  @PreAuthorize("hasAnyRole('LEADER','MANAGER')")
  @Operation(summary = "Consolidated shift report for a window", description = "Aggregates per-machine totals, OEE and the list of manual pauses and auto stops over the window. The sector filter narrows the report below the principal scope; the shift parameter is informational.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Shift report", content = @Content(schema = @Schema(implementation = ShiftReportResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed to request reports", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public ShiftReportResponseDTO getShiftReport(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @RequestParam(required = false) String sector,
      @RequestParam(required = false) String shift,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return reportService.buildShiftReport(from, to, sector, shift, principal);
  }
}