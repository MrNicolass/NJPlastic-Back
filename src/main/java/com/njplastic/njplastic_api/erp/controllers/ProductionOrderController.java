package com.njplastic.njplastic_api.erp.controllers;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.erp.dtos.ProductionOrderResponseDTO;
import com.njplastic.njplastic_api.erp.dtos.ProductionOrderSummaryDTO;
import com.njplastic.njplastic_api.erp.services.ProductionOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Production order endpoint. Reads the local {@code production_order_cache}
 * without round-tripping the ERP. Scope is enforced per role: MANAGER sees
 * everything; OPERATOR/LEADER only see orders bound to machines in their
 * sector.
 */
@RestController
@RequestMapping("/production-orders")
@Tag(name = "Production Orders", description = "Production order cache reads")
@RequiredArgsConstructor
public class ProductionOrderController {

  private final ProductionOrderService service;

  @GetMapping
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
  @Operation(summary = "Paginated production order list", description = "Filters apply with AND semantics. Sector scope is applied automatically before any filter.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of orders", content = @Content(schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public Page<ProductionOrderResponseDTO> listOrders(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID machineId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @PageableDefault(size = 50, sort = "lastSyncAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return service.findPaged(status, machineId, from, to, principal, pageable)
        .map(ProductionOrderResponseDTO::from);
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
  @Operation(summary = "Order KPI counters", description = "Returns inProd / queued / overdue / completed counters from the cached orders accessible to the caller.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "KPI snapshot", content = @Content(schema = @Schema(implementation = ProductionOrderSummaryDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public ProductionOrderSummaryDTO summary(@AuthenticationPrincipal AuthenticatedUser principal) {
    return service.summarize(principal);
  }
}
