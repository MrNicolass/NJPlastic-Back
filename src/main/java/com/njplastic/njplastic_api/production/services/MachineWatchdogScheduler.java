package com.njplastic.njplastic_api.production.services;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.production.entities.Machine;

import lombok.RequiredArgsConstructor;

/**
 * Periodically marks machines OFFLINE when no pulse has arrived within their
 * configured window ({@code machine.offline_window_ms}). Complements the
 * pulse-driven
 * detection in {@link ProductionService}, which cannot notice a machine that
 * stopped
 * emitting entirely. Machines with no cycles yet are skipped.
 */
@Component
@RequiredArgsConstructor
public class MachineWatchdogScheduler {

  private final MachineService machineService;
  private final ProductionService productionService;
  private final MachineStatusService machineStatusService;

  @Scheduled(fixedDelayString = "${app.production.watchdog-interval-ms}")
  @Transactional
  public void scanForOfflineMachines() {
    OffsetDateTime now = OffsetDateTime.now();
    for (Machine machine : machineService.findAllActive()) {
      Optional<OffsetDateTime> lastReceived = productionService.lastReceivedAt(machine.getId());
      if (lastReceived.isEmpty()) {
        continue;
      }
      long idleMs = Duration.between(lastReceived.get(), now).toMillis();
      if (idleMs > machine.getOfflineWindowMs()) {
        machineStatusService.markOffline(machine, now);
      }
    }
  }
}