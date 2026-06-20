package com.njplastic.njplastic_api.audit.controllers;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.audit.dtos.AuditLogResponseDTO;
import com.njplastic.njplastic_api.audit.services.AuditService;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Audit trail read endpoint (sub-task 4). Backs the Auditoria tab of
 * the Reports screen (mockup Reports_Part2_V1). MANAGER-only - the audit table
 * contains sanitized payloads but exposing them broadly would still leak
 * activity patterns.
 */
@RestController
@RequestMapping("/audit-logs")
@Tag(name = "Audit Logs", description = "Audit trail reads (mockup Reports_Part2_V1)")
@RequiredArgsConstructor
public class AuditLogController {

  private final AuditService auditService;

  @GetMapping
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "Paginated audit trail", description = "Filters apply with AND semantics. Payloads are returned verbatim from the database - they were already sanitized at write time by PayloadSanitizer.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of audit entries", content = @Content(schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed to read the audit trail", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public Page<AuditLogResponseDTO> list(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String endpoint,
      @RequestParam(required = false) String method,
      @RequestParam(required = false) Integer statusCode,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @PageableDefault(size = 50, sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
    return auditService.findPaged(userId, endpoint, method, statusCode, from, to, pageable)
        .map(AuditLogResponseDTO::from);
  }
}
