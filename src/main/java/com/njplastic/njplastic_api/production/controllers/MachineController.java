package com.njplastic.njplastic_api.production.controllers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.production.dtos.EditStopMessageRequestDTO;
import com.njplastic.njplastic_api.production.dtos.MachineDetailResponseDTO;
import com.njplastic.njplastic_api.production.dtos.MachineRequestDTO;
import com.njplastic.njplastic_api.production.dtos.MachineStatusEntryDTO;
import com.njplastic.njplastic_api.production.dtos.MachineStatusResponseDTO;
import com.njplastic.njplastic_api.production.dtos.MachineSummaryDTO;
import com.njplastic.njplastic_api.production.dtos.MachineUpdateRequestDTO;
import com.njplastic.njplastic_api.production.dtos.OeeResultDTO;
import com.njplastic.njplastic_api.production.dtos.ProductionCycleResponseDTO;
import com.njplastic.njplastic_api.production.dtos.QualityRegistrationRequestDTO;
import com.njplastic.njplastic_api.production.dtos.RegisterPauseRequestDTO;
import com.njplastic.njplastic_api.production.dtos.StopEditDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.QualityRecord;
import com.njplastic.njplastic_api.production.exceptions.UnknownMachineException;
import com.njplastic.njplastic_api.production.services.MachineService;
import com.njplastic.njplastic_api.production.services.MachineStatusService;
import com.njplastic.njplastic_api.production.services.OeeService;
import com.njplastic.njplastic_api.production.services.ProductionDtoMapper;
import com.njplastic.njplastic_api.production.services.ProductionService;
import com.njplastic.njplastic_api.production.services.QualityService;

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
 * REST entry point of the production aggregate. Exposes the
 * machine catalogue, the status timeline and edition, the cycle
 * history, the OEE result and the quality registration endpoint that
 * closes the OEE loop. Authorization combines
 * {@code @PreAuthorize} for role coverage with
 * {@link MachineService#requireAccessible} for sector scoping in every
 * machine-bound operation.
 */
@RestController
@RequestMapping("/machines")
@Tag(name = "Machines", description = "Production REST API - machines, status, cycles, pauses, stops, OEE and quality ")
@RequiredArgsConstructor
public class MachineController {

  private final MachineService machineService;
  private final MachineStatusService machineStatusService;
  private final ProductionService productionService;
  private final OeeService oeeService;
  private final QualityService qualityService;
  private final ProductionDtoMapper mapper;

  @GetMapping
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
 @Operation(summary = "List the active machines visible to the caller", description = "OPERATOR/LEADER see machines of their own sector; MANAGER sees every active machine ().")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Visible machines", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MachineSummaryDTO.class)))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public List<MachineSummaryDTO> listMachines(@AuthenticationPrincipal AuthenticatedUser principal) {
    return machineService.findAccessible(principal).stream()
        .map(machine -> mapper.toMachineSummary(
            machine,
            machineStatusService.currentState(machine.getId()).orElse(null)))
        .toList();
  }

  @GetMapping("/{machineId}/status")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
 @Operation(summary = "Current state and timeline of a machine", description = "Returns the open machine_status record and every transition overlapping the requested window. Scope-checked via MachineService.requireAccessible.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Machine status snapshot", content = @Content(schema = @Schema(implementation = MachineStatusResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public MachineStatusResponseDTO getStatus(
      @PathVariable UUID machineId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    machineService.requireAccessible(machineId, principal);
    MachineStatus open = machineStatusService.findCurrentOpen(machineId).orElse(null);
    List<MachineStatusEntryDTO> timeline = mapper.toStatusEntries(
        machineStatusService.findWindow(machineId, from, to));
    long cyclesInWindow = productionService.countConfirmedCycles(machineId, from, to);
    return MachineStatusResponseDTO.builder()
        .machineId(machineId)
        .currentState(open == null ? null : open.getState())
        .current(open == null ? null : mapper.toStatusEntry(open))
        .from(from)
        .to(to)
        .timeline(timeline)
        .cyclesInWindow(cyclesInWindow)
        .build();
  }

  @GetMapping("/{machineId}/cycles")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
  @Operation(summary = "Paginated cycle history of a machine", description = "Default sort is pulseTimestamp,desc. Page size is bounded by Spring Data defaults (max 2000).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of production cycles", content = @Content(schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public Page<ProductionCycleResponseDTO> getCycles(
      @PathVariable UUID machineId,
      @PageableDefault(size = 50, sort = "pulseTimestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    machineService.requireAccessible(machineId, principal);
    return productionService.findCycles(machineId, pageable).map(mapper::toCycleResponse);
  }

  @PostMapping("/{machineId}/pauses")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
 @Operation(summary = "Classify the latest open isolated pause ", description = "Resolves the most recent PAUSED record with reason=null on the machine and attaches the reason and author. Returns 409 when no pending pause is available.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Classified pause", content = @Content(schema = @Schema(implementation = MachineStatusEntryDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "409", description = "No pending pause to classify", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public MachineStatusEntryDTO registerPause(
      @PathVariable UUID machineId,
      @Valid @RequestBody RegisterPauseRequestDTO request,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    machineService.requireAccessible(machineId, principal);
    MachineStatus updated = machineStatusService.classifyLastIsolatedPause(
        machineId, request.getReason(), principal.id());
    return mapper.toStatusEntry(updated);
  }

  @PutMapping("/{machineId}/stops/{stopId}/message")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
 @Operation(summary = "Edit the message of an AUTO_STOPPED record ()", description = "Replaces the message text and stores the author. The full audit trail is captured by AuditFilter. Returns 422 when the record is not AUTO_STOPPED.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Updated stop record", content = @Content(schema = @Schema(implementation = MachineStatusEntryDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine or stop id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "422", description = "Record is not in AUTO_STOPPED state", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public MachineStatusEntryDTO editStopMessage(
      @PathVariable UUID machineId,
      @PathVariable UUID stopId,
      @Valid @RequestBody EditStopMessageRequestDTO request,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    machineService.requireAccessible(machineId, principal);
    MachineStatus updated = machineStatusService.editAutoStopMessage(
        machineId, stopId, request.getMessage(), principal.id());
    return mapper.toStatusEntry(updated);
  }

  @GetMapping("/{machineId}/stops/{stopId}/edits")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
 @Operation(summary = "Edition history of an AUTO_STOPPED message ", description = "Backs the Histórico de edições block of the Modal_Change_Stop mockups (Líder/Gestor). Reconstructed from audit_log; no dedicated persistence. Sort is forced to timestamp DESC.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of edition entries (newest first)", content = @Content(schema = @Schema(implementation = StopEditDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine or stop id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public Page<StopEditDTO> getStopEditHistory(
      @PathVariable UUID machineId,
      @PathVariable UUID stopId,
      @PageableDefault(size = 20) Pageable pageable,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    machineService.requireAccessible(machineId, principal);
    return machineStatusService.findEditHistory(machineId, stopId, pageable);
  }

  @GetMapping("/{machineId}/oee")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
 @Operation(summary = "OEE result for a machine over a window ", description = "Availability x Performance x Quality. When no quality_record covers the window the response is partial (Quality and OEE null).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OEE result", content = @Content(schema = @Schema(implementation = OeeResultDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public OeeResultDTO getOee(
      @PathVariable UUID machineId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    machineService.requireAccessible(machineId, principal);
    return oeeService.calculate(machineId, from, to);
  }

  @PostMapping("/{machineId}/quality")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
 @Operation(summary = "Register good/total quality counts (quality factor)", description = "Persists quality counts produced during a production order so OEE can complete the calculation for periods covered by the record.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Stored quality record id", content = @Content(schema = @Schema(implementation = QualityRecord.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public QualityRecord registerQuality(
      @PathVariable UUID machineId,
      @Valid @RequestBody QualityRegistrationRequestDTO request,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    Machine machine = machineService.requireAccessible(machineId, principal);
    request.setMachineId(machine.getId());
    return qualityService.registerQuality(request, principal.id());
  }

  @GetMapping("/{machineId}/detail")
  @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')")
 @Operation(summary = "Machine detail with detection parameters ", description = "Returns the full Machine projection with standardCycleMs, toleranceFactor, consecutivePausesToStop and offlineWindowMs. Scope-checked via MachineService.requireAccessible.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Machine detail", content = @Content(schema = @Schema(implementation = MachineDetailResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Machine is outside the caller scope", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public MachineDetailResponseDTO getMachineDetail(
      @PathVariable UUID machineId,
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return MachineDetailResponseDTO.from(machineService.requireAccessible(machineId, principal));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('MANAGER')")
 @Operation(summary = "Register a machine ", description = "Validates uniqueness of the short code. The machine is created active.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Machine created", content = @Content(schema = @Schema(implementation = MachineDetailResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "409", description = "Machine code already in use", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public MachineDetailResponseDTO createMachine(@Valid @RequestBody MachineRequestDTO request) {
    Machine entity = Machine.builder()
        .code(request.getCode())
        .description(request.getDescription())
        .sector(request.getSector())
        .standardCycleMs(request.getStandardCycleMs())
        .toleranceFactor(request.getToleranceFactor())
        .consecutivePausesToStop(request.getConsecutivePausesToStop())
        .offlineWindowMs(request.getOfflineWindowMs())
        .active(true)
        .build();
    return MachineDetailResponseDTO.from(machineService.create(entity));
  }

  @PutMapping("/{machineId}")
  @PreAuthorize("hasRole('MANAGER')")
 @Operation(summary = "Update machine parameters ", description = "Code is immutable - it identifies the machine on the MQTT payload and on historical cycles.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Machine updated", content = @Content(schema = @Schema(implementation = MachineDetailResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public MachineDetailResponseDTO updateMachine(
      @PathVariable UUID machineId,
      @Valid @RequestBody MachineUpdateRequestDTO request) {
    if (machineService.findById(machineId).isEmpty()) {
      throw new UnknownMachineException("Machine not found: " + machineId);
    }
    Machine patch = Machine.builder()
        .description(request.getDescription())
        .sector(request.getSector())
        .standardCycleMs(request.getStandardCycleMs())
        .toleranceFactor(request.getToleranceFactor())
        .consecutivePausesToStop(request.getConsecutivePausesToStop())
        .offlineWindowMs(request.getOfflineWindowMs())
        .active(request.isActive())
        .build();
    return MachineDetailResponseDTO.from(machineService.update(machineId, patch));
  }

  @DeleteMapping("/{machineId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('MANAGER')")
 @Operation(summary = "Soft-delete a machine ", description = "Flips active to false. Preserves cycle history and audit traceability.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Machine soft-deleted"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown machine id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public void deleteMachine(@PathVariable UUID machineId) {
    machineService.softDelete(machineId);
  }
}
