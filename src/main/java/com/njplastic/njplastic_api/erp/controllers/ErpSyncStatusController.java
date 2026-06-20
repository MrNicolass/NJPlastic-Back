package com.njplastic.njplastic_api.erp.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.erp.dtos.ErpSyncStatusResponseDTO;
import com.njplastic.njplastic_api.erp.services.ErpStatusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Exposes the ERP sync observability endpoint consumed by the Gestor screen
 * (Figura 26a/b). Available regardless of the
 * {@code app.datasource.erp.enabled} flag so the Manager can confirm the
 * disabled state. Restricted to MANAGER. Audit trail and CORS are
 * handled by the standard filters.
 */
@RestController
@RequestMapping("/erp/sync")
@Tag(name = "ERP", description = "ERP integration observability (part 2)")
@RequiredArgsConstructor
public class ErpSyncStatusController {

  private final ErpStatusService erpStatusService;

  @GetMapping("/status")
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "Aggregated ERP sync status and recent runs", description = "Returns the top-level connection state, last/next sync window, success rate over 24h, average latency, counters of the last run, the last error message and the most recent runs (capped by app.erp.sync.history-page-size). Status is DISABLED when the ERP datasource flag is off.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "ERP sync KPIs", content = @Content(schema = @Schema(implementation = ErpSyncStatusResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed to read ERP status", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public ErpSyncStatusResponseDTO getStatus() {
    return erpStatusService.buildStatus();
  }
}