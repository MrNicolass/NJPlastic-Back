package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.ProductionCycle;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;
import com.njplastic.njplastic_api.production.mqtt.ProductionProperties;
import com.njplastic.njplastic_api.production.mqtt.PulsePayload;
import com.njplastic.njplastic_api.production.repositories.ProductionRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionServiceTest {

  @Mock
  private ProductionRepository productionRepository;

  @Mock
  private MachineService machineService;

  @Mock
  private MachineStatusService machineStatusService;

  private ProductionService service;

  private static final UUID MACHINE_ID = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");

  private final ProductionProperties properties = new ProductionProperties(
      "UTC",
      300_000L,
      30_000L,
      "AUTO_STOP_DEFAULT");

  @BeforeEach
  void setUp() {
    service = new ProductionService(productionRepository, machineService, machineStatusService, properties);
  }

  private Machine machine() {
    return Machine.builder()
        .id(MACHINE_ID)
        .code("MAQ-01")
        .standardCycleMs(2000)
        .toleranceFactor(new BigDecimal("1.50"))
        .consecutivePausesToStop(3)
        .offlineWindowMs(60_000)
        .build();
  }

  private PulsePayload payload(String generatedAt) {
    return PulsePayload.builder().machineCode("MAQ-01").generatedAt(generatedAt).build();
  }

  private OffsetDateTime utc(int hour, int minute, int second) {
    return OffsetDateTime.of(2026, 5, 28, hour, minute, second, 0, ZoneOffset.UTC);
  }

  @Test
  void processPulse_dropsWhenMachineUnknown() {
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.empty());

    service.processPulse(payload("14:23:55"), utc(14, 23, 55));

    verify(productionRepository, never()).save(any());
    verify(machineStatusService, never()).resumeRunning(any(), any());
  }

  @Test
  void processPulse_dropsWhenGeneratedAtUnparseable() {
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.of(machine()));

    service.processPulse(payload("not-a-time"), utc(14, 23, 55));

    verify(productionRepository, never()).save(any());
  }

  @Test
  void processPulse_persistsDiscardedWhenClockDriftExceedsTolerance() {
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.of(machine()));
    when(productionRepository.findTopByMachineIdOrderByReceivedAtDesc(MACHINE_ID))
        .thenReturn(Optional.empty());

    // generated 14:00:00 vs received 14:10:00 -> 600_000ms drift > 300_000ms tolerance
    service.processPulse(payload("14:00:00"), utc(14, 10, 0));

    ArgumentCaptor<ProductionCycle> captor = ArgumentCaptor.forClass(ProductionCycle.class);
    verify(productionRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getState()).isEqualTo(RecordState.DISCARDED);
    verify(machineStatusService, never()).resumeRunning(any(), any());
  }

  @Test
  void processPulse_confirmedNormalPulseResumesRunning() {
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.of(machine()));
    when(productionRepository.findTopByMachineIdOrderByReceivedAtDesc(MACHINE_ID))
        .thenReturn(Optional.empty());
    when(productionRepository.findTopByMachineIdAndStateOrderByPulseTimestampDesc(MACHINE_ID, RecordState.CONFIRMED))
        .thenReturn(Optional.empty());

    service.processPulse(payload("14:23:55"), utc(14, 23, 55));

    ArgumentCaptor<ProductionCycle> captor = ArgumentCaptor.forClass(ProductionCycle.class);
    verify(productionRepository).save(captor.capture());
    ProductionCycle saved = captor.getValue();
    assertThat(saved.getState()).isEqualTo(RecordState.CONFIRMED);
    assertThat(saved.getSequence()).isEqualTo(1L);
    assertThat(saved.getIntervalMs()).isNull();
    verify(machineStatusService).resumeRunning(any(Machine.class), any(OffsetDateTime.class));
  }

  @Test
  void processPulse_assignsIntervalAndIncrementsSequenceFromPrevious() {
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.of(machine()));
    ProductionCycle prev = ProductionCycle.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .sequence(41L)
        .pulseTimestamp(utc(14, 23, 53))
        .state(RecordState.CONFIRMED)
        .build();
    when(productionRepository.findTopByMachineIdOrderByReceivedAtDesc(MACHINE_ID))
        .thenReturn(Optional.of(prev));
    when(productionRepository.findTopByMachineIdAndStateOrderByPulseTimestampDesc(MACHINE_ID, RecordState.CONFIRMED))
        .thenReturn(Optional.of(prev));

    service.processPulse(payload("14:23:55"), utc(14, 23, 55));

    ArgumentCaptor<ProductionCycle> captor = ArgumentCaptor.forClass(ProductionCycle.class);
    verify(productionRepository).save(captor.capture());
    ProductionCycle saved = captor.getValue();
    assertThat(saved.getSequence()).isEqualTo(42L);
    assertThat(saved.getIntervalMs()).isEqualTo(2000);
    verify(machineStatusService).resumeRunning(any(), any());
  }

  @Test
  void processPulse_isolatedPauseWhenIntervalExceedsThresholdButUnderConsecutiveLimit() {
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.of(machine()));
    ProductionCycle prev = ProductionCycle.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .sequence(10L)
        .pulseTimestamp(utc(14, 23, 0))
        .state(RecordState.CONFIRMED)
        .build();
    when(productionRepository.findTopByMachineIdOrderByReceivedAtDesc(MACHINE_ID))
        .thenReturn(Optional.of(prev));
    when(productionRepository.findTopByMachineIdAndStateOrderByPulseTimestampDesc(MACHINE_ID, RecordState.CONFIRMED))
        .thenReturn(Optional.of(prev));
    when(productionRepository.findByMachineIdAndStateOrderByPulseTimestampDesc(
        eq(MACHINE_ID), eq(RecordState.CONFIRMED), any(Pageable.class)))
        .thenReturn(List.of(
            ProductionCycle.builder().intervalMs(5_000).build(),
            ProductionCycle.builder().intervalMs(1_500).build()));
    when(machineStatusService.currentState(MACHINE_ID)).thenReturn(Optional.of(MachineState.RUNNING));

    // pulse 30s after previous => intervalMs 30_000ms > threshold 3000ms (2000 * 1.5)
    service.processPulse(payload("14:23:30"), utc(14, 23, 30));

    verify(productionRepository, times(1)).save(any(ProductionCycle.class));
    verify(machineStatusService, times(1)).recordIsolatedPause(
        any(Machine.class),
        eq(utc(14, 23, 0)),
        eq(utc(14, 23, 30)),
        eq(1));
    verify(machineStatusService, never()).recordAutoStop(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
  }

  @Test
  void processPulse_autoStopsWhenConsecutivePausesReachThreshold() {
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.of(machine()));
    ProductionCycle prev = ProductionCycle.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .sequence(10L)
        .pulseTimestamp(utc(14, 23, 0))
        .state(RecordState.CONFIRMED)
        .build();
    when(productionRepository.findTopByMachineIdOrderByReceivedAtDesc(MACHINE_ID))
        .thenReturn(Optional.of(prev));
    when(productionRepository.findTopByMachineIdAndStateOrderByPulseTimestampDesc(MACHINE_ID, RecordState.CONFIRMED))
        .thenReturn(Optional.of(prev));
    when(productionRepository.findByMachineIdAndStateOrderByPulseTimestampDesc(
        eq(MACHINE_ID), eq(RecordState.CONFIRMED), any(Pageable.class)))
        .thenReturn(List.of(
            ProductionCycle.builder().intervalMs(10_000).build(),
            ProductionCycle.builder().intervalMs(10_000).build(),
            ProductionCycle.builder().intervalMs(10_000).build()));
    when(machineStatusService.currentState(MACHINE_ID)).thenReturn(Optional.of(MachineState.RUNNING));

    service.processPulse(payload("14:23:30"), utc(14, 23, 30));

    verify(machineStatusService).recordAutoStop(
        any(Machine.class),
        eq(utc(14, 23, 0)),
        eq(utc(14, 23, 30)),
        eq(3),
        eq("AUTO_STOP_DEFAULT"));
    verify(machineStatusService, never()).recordIsolatedPause(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void processPulse_underStopRecordsPauseUnderStop() {
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.of(machine()));
    ProductionCycle prev = ProductionCycle.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .sequence(10L)
        .pulseTimestamp(utc(14, 23, 0))
        .state(RecordState.CONFIRMED)
        .build();
    when(productionRepository.findTopByMachineIdOrderByReceivedAtDesc(MACHINE_ID))
        .thenReturn(Optional.of(prev));
    when(productionRepository.findTopByMachineIdAndStateOrderByPulseTimestampDesc(MACHINE_ID, RecordState.CONFIRMED))
        .thenReturn(Optional.of(prev));
    when(productionRepository.findByMachineIdAndStateOrderByPulseTimestampDesc(
        eq(MACHINE_ID), eq(RecordState.CONFIRMED), any(Pageable.class)))
        .thenReturn(List.of(
            ProductionCycle.builder().intervalMs(10_000).build(),
            ProductionCycle.builder().intervalMs(10_000).build()));
    when(machineStatusService.currentState(MACHINE_ID)).thenReturn(Optional.of(MachineState.AUTO_STOPPED));

    service.processPulse(payload("14:23:30"), utc(14, 23, 30));

    verify(machineStatusService).recordPauseUnderStop(
        any(Machine.class),
        eq(utc(14, 23, 0)),
        eq(utc(14, 23, 30)),
        eq(2));
    verify(machineStatusService, never()).recordAutoStop(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
    verify(machineStatusService, never()).recordIsolatedPause(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void lastReceivedAt_returnsReceivedAtFromRepository() {
    OffsetDateTime received = utc(14, 0, 0);
    ProductionCycle cycle = ProductionCycle.builder().receivedAt(received).build();
    when(productionRepository.findTopByMachineIdOrderByReceivedAtDesc(MACHINE_ID))
        .thenReturn(Optional.of(cycle));

    assertThat(service.lastReceivedAt(MACHINE_ID)).contains(received);
  }

  @Test
  void lastReceivedAt_emptyWhenNoCycles() {
    when(productionRepository.findTopByMachineIdOrderByReceivedAtDesc(MACHINE_ID))
        .thenReturn(Optional.empty());

    assertThat(service.lastReceivedAt(MACHINE_ID)).isEmpty();
  }

  @Test
  void countConfirmedCycles_delegatesToRepository() {
    OffsetDateTime from = utc(6, 0, 0);
    OffsetDateTime to = utc(14, 0, 0);
    when(productionRepository.countByMachineIdAndStateAndPulseTimestampBetween(
        MACHINE_ID, RecordState.CONFIRMED, from, to)).thenReturn(120L);

    assertThat(service.countConfirmedCycles(MACHINE_ID, from, to)).isEqualTo(120L);
  }

  @Test
  void findCycles_delegatesToRepository() {
    org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
    ProductionCycle cycle = ProductionCycle.builder().id(UUID.randomUUID()).build();
    org.springframework.data.domain.Page<ProductionCycle> page = new org.springframework.data.domain.PageImpl<>(List.of(cycle));
    when(productionRepository.findByMachineId(MACHINE_ID, pageable)).thenReturn(page);

    org.springframework.data.domain.Page<ProductionCycle> result = service.findCycles(MACHINE_ID, pageable);

    assertThat(result.getContent()).containsExactly(cycle);
  }

  @Test
  void findConfirmedCyclesWindow_delegatesToRepository() {
    OffsetDateTime from = utc(6, 0, 0);
    OffsetDateTime to = utc(14, 0, 0);
    ProductionCycle cycle = ProductionCycle.builder().id(UUID.randomUUID()).build();
    when(productionRepository
        .findByMachineIdAndStateAndPulseTimestampBetweenOrderByPulseTimestampAsc(
            MACHINE_ID, RecordState.CONFIRMED, from, to)).thenReturn(List.of(cycle));

    assertThat(service.findConfirmedCyclesWindow(MACHINE_ID, from, to)).containsExactly(cycle);
  }
}
