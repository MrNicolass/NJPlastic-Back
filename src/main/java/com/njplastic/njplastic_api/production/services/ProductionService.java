package com.njplastic.njplastic_api.production.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.ProductionCycle;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;
import com.njplastic.njplastic_api.production.mqtt.ProductionProperties;
import com.njplastic.njplastic_api.production.mqtt.PulsePayload;
import com.njplastic.njplastic_api.production.repositories.ProductionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Sole owner of the production business rules (RN05-RN12). Owns
 * {@link ProductionRepository} (production_cycle) and orchestrates machine
 * lookup,
 * timestamp reconstruction, clock-drift rejection, cycle-time computation,
 * pause
 * detection and the escalation to AUTO_STOPPED through
 * {@link MachineStatusService}.
 * The consecutive-pause counter is derived from production_cycle, the source of
 * truth, so it survives restarts and is independent of the status timeline.
 */
@Service
@RequiredArgsConstructor
public class ProductionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductionService.class);
  private static final int PAUSE_SCAN_LIMIT = 200;

  private final ProductionRepository productionRepository;
  private final MachineService machineService;
  private final MachineStatusService machineStatusService;
  private final ProductionProperties properties;

  /**
   * Process one MQTT pulse: validate the clock, persist the cycle and update the
   * machine state (RN05-RN12, RF08, RF17). Runs on the MQTT client thread,
   * outside
   * the web request scope.
   *
   * @param payload    the deserialized pulse payload
   * @param receivedAt the instant the pulse reached the backend
   */
  @Transactional
  public void processPulse(PulsePayload payload, OffsetDateTime receivedAt) {
    Optional<Machine> machineLookup = machineService.findActiveByCode(payload.getMachineCode());
    if (machineLookup.isEmpty()) {
      LOGGER.warn("Discarding pulse for unknown or inactive machine code [{}]", payload.getMachineCode());
      return;
    }
    Machine machine = machineLookup.get();

    OffsetDateTime pulseTimestamp;
    try {
      pulseTimestamp = reconstructTimestamp(payload.getGeneratedAt(), receivedAt);
    } catch (DateTimeParseException ex) {
      LOGGER.warn("Discarding pulse with unparseable generated_at [{}] for machine [{}]",
          payload.getGeneratedAt(), machine.getCode());
      return;
    }

    long sequence = nextSequence(machine);

    long driftMs = Math.abs(Duration.between(pulseTimestamp, receivedAt).toMillis());
    if (driftMs > properties.clockToleranceMs()) {
      ProductionCycle discarded = buildCycle(machine, pulseTimestamp, receivedAt, sequence, null,
          RecordState.DISCARDED);
      productionRepository.save(discarded);
      LOGGER.warn("Discarded pulse for machine [{}]: clock drift {} ms exceeds tolerance {} ms (RN05)",
          machine.getCode(), driftMs, properties.clockToleranceMs());
      return;
    }

    Optional<ProductionCycle> lastConfirmed = productionRepository
        .findTopByMachineIdAndStateOrderByPulseTimestampDesc(machine.getId(), RecordState.CONFIRMED);
    Integer intervalMs = lastConfirmed
        .map(prev -> clampToInt(Duration.between(prev.getPulseTimestamp(), pulseTimestamp).toMillis()))
        .orElse(null);

    ProductionCycle cycle = buildCycle(machine, pulseTimestamp, receivedAt, sequence, intervalMs,
        RecordState.CONFIRMED);
    productionRepository.save(cycle);

    long thresholdMs = pauseThresholdMs(machine);
    boolean isPause = intervalMs != null && intervalMs > thresholdMs;
    if (!isPause) {
      machineStatusService.resumeRunning(machine, pulseTimestamp);
      return;
    }

    OffsetDateTime gapStart = lastConfirmed.get().getPulseTimestamp();
    int newCount = consecutivePauseCount(machine, thresholdMs);
    boolean alreadyStopped = machineStatusService.currentState(machine.getId())
        .filter(state -> state == MachineState.AUTO_STOPPED)
        .isPresent();

    if (alreadyStopped) {
      machineStatusService.recordPauseUnderStop(machine, gapStart, pulseTimestamp, newCount);
    } else if (newCount >= machine.getConsecutivePausesToStop()) {
      machineStatusService.recordAutoStop(machine, gapStart, pulseTimestamp, newCount, properties.autoStopMessage());
      LOGGER.info("Machine [{}] auto-stopped after {} consecutive pauses (RN09, RF17)",
          machine.getCode(), newCount);
    } else {
      machineStatusService.recordIsolatedPause(machine, gapStart, pulseTimestamp, newCount);
    }
  }

  /**
   * Instant the most recent pulse was received for a machine, used by the
   * watchdog.
   *
   * @param machineId the machine UUID
   * @return the last received instant, or empty when the machine has no cycles
   *         yet
   */
  public Optional<OffsetDateTime> lastReceivedAt(UUID machineId) {
    return productionRepository.findTopByMachineIdOrderByReceivedAtDesc(machineId)
        .map(ProductionCycle::getReceivedAt);
  }

  /**
   * Count confirmed cycles for a machine within a window, used by OEE (RF10).
   *
   * @param machineId the machine UUID
   * @param from      window start
   * @param to        window end
   * @return the number of confirmed cycles in the window
   */
  public long countConfirmedCycles(UUID machineId, OffsetDateTime from, OffsetDateTime to) {
    return productionRepository.countByMachineIdAndStateAndPulseTimestampBetween(
        machineId, RecordState.CONFIRMED, from, to);
  }

  /**
   * Reconstruct the full TIMESTAMPTZ from the Arduino "HH:MM:SS" wall-clock time
   * and
   * the local date, handling the day-rollover edge near midnight (RN05, RFC
   * §5.3).
   *
   * @param generatedAt wall-clock time in HH:MM:SS
   * @param receivedAt  the instant the pulse reached the backend
   * @return the reconstructed pulse timestamp
   */
  private OffsetDateTime reconstructTimestamp(String generatedAt, OffsetDateTime receivedAt) {
    ZoneId zone = ZoneId.of(properties.timezone());
    LocalTime time = LocalTime.parse(generatedAt);
    OffsetDateTime nowLocal = receivedAt.atZoneSameInstant(zone).toOffsetDateTime();
    LocalDate date = nowLocal.toLocalDate();
    if (time.getHour() >= 23 && nowLocal.getHour() < 1) {
      date = date.minusDays(1);
    }
    return date.atTime(time).atZone(zone).toOffsetDateTime();
  }

  /**
   * Length of the current streak of most-recent confirmed cycles whose interval
   * exceeds the pause threshold (RN06, RN09, RN11). Derived from production_cycle
   * so
   * it is durable and independent of the status timeline.
   *
   * @param machine     the owning machine
   * @param thresholdMs the pause threshold in milliseconds
   * @return the consecutive-pause count including the current cycle
   */
  private int consecutivePauseCount(Machine machine, long thresholdMs) {
    List<ProductionCycle> recent = productionRepository.findByMachineIdAndStateOrderByPulseTimestampDesc(
        machine.getId(), RecordState.CONFIRMED, PageRequest.of(0, PAUSE_SCAN_LIMIT));
    int count = 0;
    for (ProductionCycle cycle : recent) {
      if (cycle.getIntervalMs() != null && cycle.getIntervalMs() > thresholdMs) {
        count++;
      } else {
        break;
      }
    }
    return count;
  }

  private long nextSequence(Machine machine) {
    return productionRepository.findTopByMachineIdOrderByReceivedAtDesc(machine.getId())
        .map(prev -> prev.getSequence() + 1)
        .orElse(1L);
  }

  private long pauseThresholdMs(Machine machine) {
    return BigDecimal.valueOf(machine.getStandardCycleMs())
        .multiply(machine.getToleranceFactor())
        .longValue();
  }

  private ProductionCycle buildCycle(Machine machine, OffsetDateTime pulseTimestamp, OffsetDateTime receivedAt,
      long sequence, Integer intervalMs, RecordState state) {
    return ProductionCycle.builder()
        .machineId(machine.getId())
        .pulseTimestamp(pulseTimestamp)
        .receivedAt(receivedAt)
        .sequence(sequence)
        .intervalMs(intervalMs)
        .state(state)
        .build();
  }

  private int clampToInt(long value) {
    return (int) Math.min(value, Integer.MAX_VALUE);
  }
}