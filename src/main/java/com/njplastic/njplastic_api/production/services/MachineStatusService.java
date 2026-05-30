package com.njplastic.njplastic_api.production.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;
import com.njplastic.njplastic_api.production.exceptions.MachineStatusPersistenceException;
import com.njplastic.njplastic_api.production.repositories.MachineStatusRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link MachineStatusRepository} and the machine-state
 * timeline.
 * Keeps a single open record per machine (end_time null) representing the
 * current
 * state; every transition closes the previous record and opens a new one
 * (RN09-RN11,
 * RFC §5.2.2). The consecutive-pause counter itself is not stored here - it is
 * derived from production_cycle by {@link ProductionService}; this service only
 * persists its snapshot in {@code consecutiveCountAtCreation} for traceability.
 */
@Service
@RequiredArgsConstructor
public class MachineStatusService {

  private final MachineStatusRepository machineStatusRepository;

  /**
   * Current open status record for a machine, if any.
   *
   * @param machineId the machine UUID
   * @return the open record, or empty when no transition has been recorded yet
   */
  public Optional<MachineStatus> findCurrentOpen(UUID machineId) {
    return machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(machineId);
  }

  /**
   * Current operational state of a machine derived from its open record.
   *
   * @param machineId the machine UUID
   * @return the current state, or empty when none has been recorded yet
   */
  public Optional<MachineState> currentState(UUID machineId) {
    return findCurrentOpen(machineId).map(MachineStatus::getState);
  }

  /**
   * Ensure the machine is RUNNING after a normal-interval pulse. Closes any open
   * non-running record (AUTO_STOPPED/OFFLINE/PAUSED) at the pulse instant and
   * opens
   * a RUNNING record; does nothing when already running (RN10, RN11).
   *
   * @param machine        the owning machine
   * @param pulseTimestamp the reconstructed pulse instant
   */
  public void resumeRunning(Machine machine, OffsetDateTime pulseTimestamp) {
    try {
      Optional<MachineStatus> current = findCurrentOpen(machine.getId());
      if (current.isPresent()) {
        if (current.get().getState() == MachineState.RUNNING) {
          return;
        }
        close(current.get(), pulseTimestamp);
      }
      open(machine.getId(), MachineState.RUNNING, pulseTimestamp, null, null, null);
    } catch (DataAccessException ex) {
      throw new MachineStatusPersistenceException(
          "Failed to transition machine " + machine.getId() + " to RUNNING", ex);
    }
  }

  /**
   * Record an isolated pause (RN06) and keep the machine producing. Closes the
   * open
   * record at the gap start, persists a closed PAUSED segment for the gap, and
   * opens
   * a fresh RUNNING record at the pulse instant.
   *
   * @param machine        the owning machine
   * @param gapStart       timestamp of the previous confirmed pulse
   * @param pulseTimestamp the reconstructed pulse instant that ended the gap
   * @param count          consecutive-pause counter snapshot
   */
  public void recordIsolatedPause(Machine machine, OffsetDateTime gapStart, OffsetDateTime pulseTimestamp, int count) {
    try {
      findCurrentOpen(machine.getId()).ifPresent(open -> close(open, gapStart));
      savePause(machine.getId(), gapStart, pulseTimestamp, count);
      open(machine.getId(), MachineState.RUNNING, pulseTimestamp, null, null, null);
    } catch (DataAccessException ex) {
      throw new MachineStatusPersistenceException(
          "Failed to record isolated pause for machine " + machine.getId(), ex);
    }
  }

  /**
   * Escalate to AUTO_STOPPED when the consecutive-pause threshold is reached
   * (RN09,
   * RF17). Closes the open record at the gap start, persists the triggering gap
   * as a
   * closed PAUSED segment, and opens an AUTO_STOPPED record with the default
   * message
   * (RF18); the machine stays stopped until the next normal pulse (RN10).
   *
   * @param machine        the owning machine
   * @param gapStart       timestamp of the previous confirmed pulse
   * @param pulseTimestamp the reconstructed pulse instant that ended the gap
   * @param count          consecutive-pause counter snapshot
   * @param message        default message applied to the AUTO_STOPPED record
   */
  public void recordAutoStop(Machine machine, OffsetDateTime gapStart, OffsetDateTime pulseTimestamp,
      int count, String message) {
    try {
      findCurrentOpen(machine.getId()).ifPresent(open -> close(open, gapStart));
      savePause(machine.getId(), gapStart, pulseTimestamp, count);
      open(machine.getId(), MachineState.AUTO_STOPPED, pulseTimestamp, null, message, count);
    } catch (DataAccessException ex) {
      throw new MachineStatusPersistenceException(
          "Failed to record auto-stop for machine " + machine.getId(), ex);
    }
  }

  /**
   * Log a continuing slow cycle while the machine is already AUTO_STOPPED.
   * Persists a
   * closed PAUSED segment for traceability without touching the open stop record.
   *
   * @param machine        the owning machine
   * @param gapStart       timestamp of the previous confirmed pulse
   * @param pulseTimestamp the reconstructed pulse instant that ended the gap
   * @param count          consecutive-pause counter snapshot
   */
  public void recordPauseUnderStop(Machine machine, OffsetDateTime gapStart, OffsetDateTime pulseTimestamp, int count) {
    try {
      savePause(machine.getId(), gapStart, pulseTimestamp, count);
    } catch (DataAccessException ex) {
      throw new MachineStatusPersistenceException(
          "Failed to record pause under stop for machine " + machine.getId(), ex);
    }
  }

  /**
   * Mark a machine OFFLINE when the watchdog detects no pulses within the window.
   * Closes the open record and opens an OFFLINE record; no-op when already
   * offline.
   *
   * @param machine the owning machine
   * @param now     the watchdog scan instant
   */
  public void markOffline(Machine machine, OffsetDateTime now) {
    try {
      Optional<MachineStatus> current = findCurrentOpen(machine.getId());
      if (current.isPresent() && current.get().getState() == MachineState.OFFLINE) {
        return;
      }
      current.ifPresent(open -> close(open, now));
      open(machine.getId(), MachineState.OFFLINE, now, null, null, null);
    } catch (DataAccessException ex) {
      throw new MachineStatusPersistenceException(
          "Failed to mark machine " + machine.getId() + " as OFFLINE", ex);
    }
  }

  /**
   * Status records of downtime states overlapping the window, used by OEE (RF10).
   *
   * @param machineId the machine UUID
   * @param from      window start
   * @param to        window end
   * @return overlapping PAUSED/AUTO_STOPPED/OFFLINE records
   */
  public List<MachineStatus> findDowntimeOverlapping(UUID machineId, OffsetDateTime from, OffsetDateTime to) {
    return machineStatusRepository.findOverlapping(
        machineId,
        List.of(MachineState.PAUSED, MachineState.AUTO_STOPPED, MachineState.OFFLINE),
        from, to);
  }

  private void savePause(UUID machineId, OffsetDateTime start, OffsetDateTime end, int count) {
    MachineStatus pause = MachineStatus.builder()
        .machineId(machineId)
        .state(MachineState.PAUSED)
        .startTime(start)
        .endTime(end)
        .consecutiveCountAtCreation(count)
        .recordState(RecordState.CONFIRMED)
        .build();
    machineStatusRepository.save(pause);
  }

  private void open(UUID machineId, MachineState state, OffsetDateTime start, String reason,
      String message, Integer count) {
    MachineStatus status = MachineStatus.builder()
        .machineId(machineId)
        .state(state)
        .reason(reason)
        .message(message)
        .startTime(start)
        .consecutiveCountAtCreation(count)
        .recordState(RecordState.CONFIRMED)
        .build();
    machineStatusRepository.save(status);
  }

  private void close(MachineStatus status, OffsetDateTime endTime) {
    status.setEndTime(endTime);
    status.setRecordState(RecordState.CONFIRMED);
    machineStatusRepository.save(status);
  }
}