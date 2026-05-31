package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.production.dtos.MachineStatusEntryDTO;
import com.njplastic.njplastic_api.production.dtos.MachineSummaryDTO;
import com.njplastic.njplastic_api.production.dtos.OeeResultDTO;
import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.enums.MachineState;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

  @Mock
  private MachineService machineService;

  @Mock
  private MachineStatusService machineStatusService;

  @Mock
  private ProductionService productionService;

  @Mock
  private OeeService oeeService;

  @Mock
  private ProductionDtoMapper mapper;

  @InjectMocks
  private ReportService reportService;

  private static final UUID MACHINE_ID = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");
  private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-05-28T06:00:00Z");
  private static final OffsetDateTime TO = OffsetDateTime.parse("2026-05-28T14:00:00Z");

  private AuthenticatedUser principal() {
    return new AuthenticatedUser(UUID.randomUUID(), "leader", UserRole.LEADER, "INJECAO", "TURNO_A");
  }

  private Machine machine(String sector) {
    return Machine.builder().id(MACHINE_ID).code("MAQ-01").sector(sector).build();
  }

  private OeeResultDTO oeeResult() {
    return OeeResultDTO.builder()
        .machineId(MACHINE_ID)
        .periodStart(FROM)
        .periodEnd(TO)
        .availability(0.9)
        .performance(0.8)
        .partial(true)
        .build();
  }

  @Test
  void buildShiftReport_aggregatesPerMachineSections() {
    AuthenticatedUser principal = principal();
    Machine machine = machine("INJECAO");
    when(machineService.findAccessible(principal)).thenReturn(List.of(machine));
    when(productionService.countConfirmedCycles(MACHINE_ID, FROM, TO)).thenReturn(1280L);
    when(oeeService.calculate(MACHINE_ID, FROM, TO)).thenReturn(oeeResult());
    when(machineStatusService.findWindowByStates(eq(MACHINE_ID), eq(List.of(MachineState.PAUSED)), eq(FROM), eq(TO)))
        .thenReturn(List.of());
    when(machineStatusService.findWindowByStates(eq(MACHINE_ID), eq(List.of(MachineState.AUTO_STOPPED)), eq(FROM), eq(TO)))
        .thenReturn(List.of());
    when(mapper.toStatusEntries(any())).thenReturn(List.<MachineStatusEntryDTO>of());
    when(machineStatusService.currentState(MACHINE_ID)).thenReturn(Optional.of(MachineState.RUNNING));
    MachineSummaryDTO summary = MachineSummaryDTO.builder().id(MACHINE_ID).code("MAQ-01").build();
    when(mapper.toMachineSummary(machine, MachineState.RUNNING)).thenReturn(summary);

    ShiftReportResponseDTO report = reportService.buildShiftReport(FROM, TO, null, "TURNO_A", principal);

    assertThat(report.getPeriodStart()).isEqualTo(FROM);
    assertThat(report.getPeriodEnd()).isEqualTo(TO);
    assertThat(report.getShift()).isEqualTo("TURNO_A");
    assertThat(report.getMachines()).hasSize(1);
    assertThat(report.getMachines().get(0).getConfirmedCycles()).isEqualTo(1280L);
    assertThat(report.getMachines().get(0).getMachine()).isSameAs(summary);
    assertThat(report.getMachines().get(0).getOee().isPartial()).isTrue();
  }

  @Test
  void buildShiftReport_filtersMachinesBySector() {
    AuthenticatedUser principal = principal();
    Machine inSector = machine("INJECAO");
    Machine outSector = Machine.builder().id(UUID.randomUUID()).code("MAQ-02").sector("MONTAGEM").build();
    when(machineService.findAccessible(principal)).thenReturn(List.of(inSector, outSector));
    when(productionService.countConfirmedCycles(MACHINE_ID, FROM, TO)).thenReturn(0L);
    when(oeeService.calculate(MACHINE_ID, FROM, TO)).thenReturn(oeeResult());
    when(mapper.toStatusEntries(any())).thenReturn(List.<MachineStatusEntryDTO>of());
    when(machineStatusService.currentState(MACHINE_ID)).thenReturn(Optional.empty());
    when(mapper.toMachineSummary(inSector, null))
        .thenReturn(MachineSummaryDTO.builder().id(MACHINE_ID).build());

    ShiftReportResponseDTO report = reportService.buildShiftReport(FROM, TO, "INJECAO", null, principal);

    assertThat(report.getMachines()).hasSize(1);
    assertThat(report.getSector()).isEqualTo("INJECAO");
    verify(oeeService, never()).calculate(eq(outSector.getId()), any(), any());
  }

  @Test
  void buildShiftReport_keepsAllWhenSectorIsBlank() {
    AuthenticatedUser principal = principal();
    Machine machine = machine("INJECAO");
    when(machineService.findAccessible(principal)).thenReturn(List.of(machine));
    when(productionService.countConfirmedCycles(MACHINE_ID, FROM, TO)).thenReturn(0L);
    when(oeeService.calculate(MACHINE_ID, FROM, TO)).thenReturn(oeeResult());
    when(mapper.toStatusEntries(any())).thenReturn(List.<MachineStatusEntryDTO>of());
    when(machineStatusService.currentState(MACHINE_ID)).thenReturn(Optional.empty());
    when(mapper.toMachineSummary(machine, null))
        .thenReturn(MachineSummaryDTO.builder().id(MACHINE_ID).build());

    ShiftReportResponseDTO report = reportService.buildShiftReport(FROM, TO, "  ", null, principal);

    assertThat(report.getMachines()).hasSize(1);
  }

  @Test
  void buildShiftReport_mapsManualPausesAndAutoStopsThroughMapper() {
    AuthenticatedUser principal = principal();
    Machine machine = machine("INJECAO");
    MachineStatus pause = MachineStatus.builder().id(UUID.randomUUID()).state(MachineState.PAUSED).build();
    MachineStatus stop = MachineStatus.builder().id(UUID.randomUUID()).state(MachineState.AUTO_STOPPED).build();
    MachineStatusEntryDTO pauseDto = MachineStatusEntryDTO.builder().id(pause.getId()).state(MachineState.PAUSED).build();
    MachineStatusEntryDTO stopDto = MachineStatusEntryDTO.builder().id(stop.getId()).state(MachineState.AUTO_STOPPED).build();
    when(machineService.findAccessible(principal)).thenReturn(List.of(machine));
    when(productionService.countConfirmedCycles(MACHINE_ID, FROM, TO)).thenReturn(0L);
    when(oeeService.calculate(MACHINE_ID, FROM, TO)).thenReturn(oeeResult());
    when(machineStatusService.findWindowByStates(eq(MACHINE_ID), eq(List.of(MachineState.PAUSED)), eq(FROM), eq(TO)))
        .thenReturn(List.of(pause));
    when(machineStatusService.findWindowByStates(eq(MACHINE_ID), eq(List.of(MachineState.AUTO_STOPPED)), eq(FROM), eq(TO)))
        .thenReturn(List.of(stop));
    when(mapper.toStatusEntries(List.of(pause))).thenReturn(List.of(pauseDto));
    when(mapper.toStatusEntries(List.of(stop))).thenReturn(List.of(stopDto));
    when(machineStatusService.currentState(MACHINE_ID)).thenReturn(Optional.of(MachineState.RUNNING));
    when(mapper.toMachineSummary(machine, MachineState.RUNNING))
        .thenReturn(MachineSummaryDTO.builder().id(MACHINE_ID).build());

    ShiftReportResponseDTO report = reportService.buildShiftReport(FROM, TO, null, null, principal);

    assertThat(report.getMachines().get(0).getManualPauses()).containsExactly(pauseDto);
    assertThat(report.getMachines().get(0).getAutoStops()).containsExactly(stopDto);
  }
}
