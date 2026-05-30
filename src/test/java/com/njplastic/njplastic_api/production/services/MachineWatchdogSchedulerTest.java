package com.njplastic.njplastic_api.production.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.njplastic.njplastic_api.production.entities.Machine;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MachineWatchdogSchedulerTest {

  @Mock
  private MachineService machineService;

  @Mock
  private ProductionService productionService;

  @Mock
  private MachineStatusService machineStatusService;

  @InjectMocks
  private MachineWatchdogScheduler scheduler;

  private Machine machine(UUID id, int offlineWindowMs) {
    return Machine.builder().id(id).code("MAQ-01").offlineWindowMs(offlineWindowMs).build();
  }

  @Test
  void scan_marksMachineOfflineWhenIdleExceedsWindow() {
    UUID id = UUID.randomUUID();
    Machine machine = machine(id, 60_000);
    when(machineService.findAllActive()).thenReturn(List.of(machine));
    when(productionService.lastReceivedAt(id))
        .thenReturn(Optional.of(OffsetDateTime.now().minusMinutes(10)));

    scheduler.scanForOfflineMachines();

    verify(machineStatusService, times(1)).markOffline(eq(machine), any(OffsetDateTime.class));
  }

  @Test
  void scan_skipsMachinesWithoutPulses() {
    UUID id = UUID.randomUUID();
    Machine machine = machine(id, 60_000);
    when(machineService.findAllActive()).thenReturn(List.of(machine));
    when(productionService.lastReceivedAt(id)).thenReturn(Optional.empty());

    scheduler.scanForOfflineMachines();

    verify(machineStatusService, never()).markOffline(any(), any());
  }

  @Test
  void scan_skipsMachinesWithRecentPulse() {
    UUID id = UUID.randomUUID();
    Machine machine = machine(id, 60_000);
    when(machineService.findAllActive()).thenReturn(List.of(machine));
    when(productionService.lastReceivedAt(id))
        .thenReturn(Optional.of(OffsetDateTime.now()));

    scheduler.scanForOfflineMachines();

    verify(machineStatusService, never()).markOffline(any(), any());
  }
}
