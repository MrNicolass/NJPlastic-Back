package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.njplastic.njplastic_api.production.dtos.OeeResultDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.QualityRecord;
import com.njplastic.njplastic_api.production.exceptions.UnknownMachineException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OeeServiceTest {

  @Mock
  private MachineService machineService;

  @Mock
  private ProductionService productionService;

  @Mock
  private MachineStatusService machineStatusService;

  @Mock
  private QualityService qualityService;

  @InjectMocks
  private OeeService oeeService;

  private static final UUID MACHINE_ID = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");
  private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-05-28T06:00:00Z");
  private static final OffsetDateTime TO = OffsetDateTime.parse("2026-05-28T14:00:00Z");

  private Machine machine() {
    return Machine.builder().id(MACHINE_ID).standardCycleMs(2000).build();
  }

  private MachineStatus downtime(OffsetDateTime start, OffsetDateTime end) {
    return MachineStatus.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .startTime(start)
        .endTime(end)
        .build();
  }

  private QualityRecord quality(int good, int total) {
    return QualityRecord.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .goodCount(good)
        .totalCount(total)
        .build();
  }

  @Test
  void calculate_throwsWhenMachineMissing() {
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> oeeService.calculate(MACHINE_ID, FROM, TO))
        .isInstanceOf(UnknownMachineException.class);
  }

  @Test
  void calculate_returnsPartialWhenNoQualityRecord() {
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.of(machine()));
    when(machineStatusService.findDowntimeOverlapping(MACHINE_ID, FROM, TO)).thenReturn(List.of());
    when(productionService.countConfirmedCycles(MACHINE_ID, FROM, TO)).thenReturn(10_000L);
    when(qualityService.findForPeriod(MACHINE_ID, FROM, TO)).thenReturn(List.of());

    OeeResultDTO result = oeeService.calculate(MACHINE_ID, FROM, TO);

    assertThat(result.isPartial()).isTrue();
    assertThat(result.getQuality()).isNull();
    assertThat(result.getAvailability()).isEqualTo(1.0);
    assertThat(result.getPerformance()).isGreaterThan(0.0);
    assertThat(result.getOee())
        .isEqualTo(result.getAvailability() * result.getPerformance());
    assertThat(result.getMachineId()).isEqualTo(MACHINE_ID);
    assertThat(result.getPeriodStart()).isEqualTo(FROM);
    assertThat(result.getPeriodEnd()).isEqualTo(TO);
  }

  @Test
  void calculate_combinesAvailabilityPerformanceAndQuality() {
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.of(machine()));
    // 1h downtime out of 8h window
    OffsetDateTime dStart = FROM.plusHours(1);
    OffsetDateTime dEnd = FROM.plusHours(2);
    when(machineStatusService.findDowntimeOverlapping(MACHINE_ID, FROM, TO))
        .thenReturn(List.of(downtime(dStart, dEnd)));
    // 7h run time = 25_200_000ms, standard 2000ms cycle => ideal max 12_600 cycles
    when(productionService.countConfirmedCycles(MACHINE_ID, FROM, TO)).thenReturn(12_000L);
    when(qualityService.findForPeriod(MACHINE_ID, FROM, TO))
        .thenReturn(List.of(quality(950, 1000)));

    OeeResultDTO result = oeeService.calculate(MACHINE_ID, FROM, TO);

    assertThat(result.isPartial()).isFalse();
    assertThat(result.getAvailability()).isEqualTo(7.0 / 8.0);
    assertThat(result.getPerformance()).isCloseTo(24_000_000.0 / 25_200_000.0, within(1e-9));
    assertThat(result.getQuality()).isEqualTo(0.95);
    assertThat(result.getOee())
        .isEqualTo(result.getAvailability() * result.getPerformance() * result.getQuality());
  }

  @Test
  void calculate_clipsDowntimeToWindowAndUsesEndOfWindowWhenOpen() {
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.of(machine()));
    // Downtime starts before window and is still open: should be clipped to [FROM, TO]
    when(machineStatusService.findDowntimeOverlapping(MACHINE_ID, FROM, TO))
        .thenReturn(List.of(downtime(FROM.minusHours(1), null)));
    when(productionService.countConfirmedCycles(MACHINE_ID, FROM, TO)).thenReturn(0L);
    when(qualityService.findForPeriod(MACHINE_ID, FROM, TO)).thenReturn(List.of());

    OeeResultDTO result = oeeService.calculate(MACHINE_ID, FROM, TO);

    // Full window is downtime, so availability is 0
    assertThat(result.getAvailability()).isEqualTo(0.0);
    assertThat(result.getPerformance()).isEqualTo(0.0);
    assertThat(result.isPartial()).isTrue();
  }

  @Test
  void calculate_returnsPartialWhenAllQualityTotalsZero() {
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.of(machine()));
    when(machineStatusService.findDowntimeOverlapping(MACHINE_ID, FROM, TO)).thenReturn(List.of());
    when(productionService.countConfirmedCycles(MACHINE_ID, FROM, TO)).thenReturn(0L);
    when(qualityService.findForPeriod(MACHINE_ID, FROM, TO)).thenReturn(List.of(quality(0, 0)));

    OeeResultDTO result = oeeService.calculate(MACHINE_ID, FROM, TO);

    assertThat(result.isPartial()).isTrue();
    assertThat(result.getQuality()).isNull();
    assertThat(result.getOee()).isEqualTo(0.0);
  }

  private static org.assertj.core.data.Offset<Double> within(double tolerance) {
    return org.assertj.core.data.Offset.offset(tolerance);
  }
}
