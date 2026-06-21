package com.njplastic.njplastic_api.erp.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.erp.dtos.ErpFieldMappingDTO;
import com.njplastic.njplastic_api.erp.dtos.ErpFieldMappingUpdateRequestDTO;
import com.njplastic.njplastic_api.erp.services.ErpFieldMappingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ERP field mapping endpoint. Backs the Manager-only drawer that edits the
 * NJPlastic <-> ERP field correspondence. The PUT replaces the full set
 * per entity_type inside one transaction; the request/response diff is
 * captured by {@code AuditFilter}.
 */
@RestController
@RequestMapping("/erp/field-mapping")
@Tag(name = "ERP Field Mapping", description = "ERP field mapping CRUD")
@RequiredArgsConstructor
public class ErpFieldMappingController {

  private final ErpFieldMappingService service;

  @GetMapping
  @PreAuthorize("hasAnyRole('LEADER','MANAGER')")
  @Operation(summary = "List ERP field mappings for an entity_type", description = "Read-only projection; LEADER and MANAGER can inspect the panel.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Mapping list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ErpFieldMappingDTO.class)))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed to read ERP mapping", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public List<ErpFieldMappingDTO> getMapping(@RequestParam String entityType) {
    return service.getMapping(entityType).stream()
        .map(ErpFieldMappingDTO::from)
        .toList();
  }

  @PutMapping
  @PreAuthorize("hasRole('MANAGER')")
 @Operation(summary = "Replace every mapping under one entity_type", description = "Atomic replace-all: prior rows for the entity_type are dropped and the new list is inserted. The audit trail is captured by AuditFilter.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Resulting mapping list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ErpFieldMappingDTO.class)))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed to edit ERP mapping", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public List<ErpFieldMappingDTO> replaceMapping(
      @Valid @RequestBody ErpFieldMappingUpdateRequestDTO request,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return service.replaceMapping(request, principal.id()).stream()
        .map(ErpFieldMappingDTO::from)
        .toList();
  }
}
