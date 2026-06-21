package com.njplastic.njplastic_api.production.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.production.dtos.MachineStatusEntryDTO;
import com.njplastic.njplastic_api.production.dtos.OeeResultDTO;
import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO;
import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO.MachineReportSection;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.enums.MachineState;

import lombok.RequiredArgsConstructor;

/**
 * Aggregates the per-machine sections of the consolidated shift report
 *. Owns no repository - reads every aggregate through its service
 * and assembles the response. The optional {@code sector} parameter
 * narrows the report below the principal scope; the {@code shift} value
 * is carried in the response for the dashboard label but not used as a
 * filter today, since shift is a user-level attribute and machines are
 * shared across shifts.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

  private final MachineService machineService;
  private final MachineStatusService machineStatusService;
  private final ProductionService productionService;
  private final OeeService oeeService;
  private final ProductionDtoMapper mapper;

 /**
 * Build the consolidated shift report for the window.
 *
 * @param from window start
 * @param to window end
 * @param sector optional sector filter applied on top of the principal scope
 * @param shift shift label echoed in the response (informational)
 * @param principal the authenticated user
 * @return the report
 */
  public ShiftReportResponseDTO buildShiftReport(OffsetDateTime from, OffsetDateTime to,
      String sector, String shift, AuthenticatedUser principal) {
    List<Machine> visible = machineService.findAccessible(principal);
    List<MachineReportSection> sections = visible.stream()
        .filter(machine -> matchesSector(machine, sector))
        .map(machine -> buildSection(machine, from, to))
        .toList();
    return ShiftReportResponseDTO.builder()
        .periodStart(from)
        .periodEnd(to)
        .sector(sector)
        .shift(shift)
        .machines(sections)
        .build();
  }

  private boolean matchesSector(Machine machine, String sector) {
    if (sector == null || sector.isBlank()) {
      return true;
    }
    return machine.getSector() != null && machine.getSector().equalsIgnoreCase(sector);
  }

  private MachineReportSection buildSection(Machine machine, OffsetDateTime from, OffsetDateTime to) {
    UUID machineId = machine.getId();
    long confirmedCycles = productionService.countConfirmedCycles(machineId, from, to);
    OeeResultDTO oee = oeeService.calculate(machineId, from, to);
    List<MachineStatusEntryDTO> manualPauses = mapper.toStatusEntries(
        machineStatusService.findWindowByStates(machineId, List.of(MachineState.PAUSED), from, to));
    List<MachineStatusEntryDTO> autoStops = mapper.toStatusEntries(
        machineStatusService.findWindowByStates(machineId, List.of(MachineState.AUTO_STOPPED), from, to));
    MachineState currentState = machineStatusService.currentState(machineId).orElse(null);
    return MachineReportSection.builder()
        .machine(mapper.toMachineSummary(machine, currentState))
        .confirmedCycles(confirmedCycles)
        .oee(oee)
        .manualPauses(manualPauses)
        .autoStops(autoStops)
        .build();
  }
}