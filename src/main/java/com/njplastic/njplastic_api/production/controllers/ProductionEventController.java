package com.njplastic.njplastic_api.production.controllers;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.production.dtos.EventRequestDTO;
import com.njplastic.njplastic_api.production.dtos.EventResponseDTO;
import com.njplastic.njplastic_api.production.entities.ProductionEvent;
import com.njplastic.njplastic_api.production.services.MachineService;
import com.njplastic.njplastic_api.production.services.ProductionEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Manual production event endpoint (EP-BE-05 reopened, RFC §7.3.1). Backs the
 * "Registrar evento manual" action on the Leader/Manager dashboard
 * (mockup Dashboard_Part1_V1) and feeds the "Eventos recentes" panel on the
 * consolidated dashboard. Reads are scoped by sector via
 * {@link MachineService#requireAccessible} (RN02-RN04).
 */
@RestController
@Tag(name = "Production Events", description = "Manual production events - training, cleaning, meetings (EP-BE-05 reopened / RFC §7.3.1)")
@RequiredArgsConstructor
public class ProductionEventController {

  private final ProductionEventService eventService;
  private final MachineService machineService;

  @PostMapping("/events")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('LEADER','MANAGER')")
  @Operation(summary = "Register a manual production event", description = "Author is taken from the JWT. The target machine must be visible to the principal (RN02-RN04).")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Event persisted", content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public EventResponseDTO register(
      @Valid @RequestBody EventRequestDTO request,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    machineService.requireAccessible(request.getMachineId(), principal);
    ProductionEvent persisted = eventService.register(
        request.getMachineId(),
        principal.id(),
        request.getType(),
        request.getDescription(),
        request.getStartedAt(),
        request.getEndedAt());
    return EventResponseDTO.from(persisted);
  }

  @GetMapping("/machines/{machineId}/events")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
  @Operation(summary = "Paginated production events for a machine", description = "When both from and to are supplied, restricts the result to events with startedAt in the window. Scope-checked via MachineService.requireAccessible.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of events", content = @Content(schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public Page<EventResponseDTO> listForMachine(
      @PathVariable UUID machineId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @PageableDefault(size = 50, sort = "startedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    machineService.requireAccessible(machineId, principal);
    return eventService.findPaged(machineId, from, to, pageable).map(EventResponseDTO::from);
  }
}
