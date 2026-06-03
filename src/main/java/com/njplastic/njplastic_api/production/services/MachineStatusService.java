package com.njplastic.njplastic_api.production.services;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.audit.entities.AuditLog;
import com.njplastic.njplastic_api.audit.services.AuditService;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.services.UserService;
import com.njplastic.njplastic_api.production.dtos.StopEditDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;
import com.njplastic.njplastic_api.production.exceptions.MachineStatusPersistenceException;
import com.njplastic.njplastic_api.production.exceptions.PauseAlreadyClassifiedException;
import com.njplastic.njplastic_api.production.exceptions.StopMessageNotEditableException;
import com.njplastic.njplastic_api.production.exceptions.StopNotFoundException;
import com.njplastic.njplastic_api.production.repositories.MachineStatusRepository;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
  private final AuditService auditService;
  private final UserService userService;
  private final ProductionDtoMapper mapper;
  private final ObjectMapper objectMapper;

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

  /**
   * Every status record of a machine that overlaps {@code [from, to]},
   * ordered by start time. Used by the status timeline endpoint and the
   * shift report (RF15).
   *
   * @param machineId the machine UUID
   * @param from      window start
   * @param to        window end
   * @return overlapping records
   */
  public List<MachineStatus> findWindow(UUID machineId, OffsetDateTime from, OffsetDateTime to) {
    return machineStatusRepository.findWindow(machineId, from, to);
  }

  /**
   * Status records in the given states overlapping {@code [from, to]},
   * used by the shift report (RF15) to list manual pauses and auto stops
   * in separate sections.
   *
   * @param machineId the machine UUID
   * @param states    the states to include
   * @param from      window start
   * @param to        window end
   * @return overlapping records
   */
  public List<MachineStatus> findWindowByStates(UUID machineId,
      List<MachineState> states, OffsetDateTime from, OffsetDateTime to) {
    return machineStatusRepository.findWindowByStates(machineId, states, from, to);
  }

  /**
   * Classify the most recent isolated pause of a machine by attaching a
   * reason and the author (RF09, UC03). Targets the latest PAUSED record
   * whose reason is null; throws {@link PauseAlreadyClassifiedException}
   * (409) when no such record exists.
   *
   * @param machineId the machine UUID
   * @param reason    the reason text supplied by the user
   * @param authorId  the user UUID registering the reason
   * @return the updated record
   */
  @Transactional
  public MachineStatus classifyLastIsolatedPause(UUID machineId, String reason, UUID authorId) {
    MachineStatus target = machineStatusRepository
        .findTopByMachineIdAndStateAndReasonIsNullOrderByStartTimeDesc(machineId, MachineState.PAUSED)
        .orElseThrow(() -> new PauseAlreadyClassifiedException(
            "No pending pause to classify for machine " + machineId));
    target.setReason(reason);
    target.setReasonAuthorId(authorId);
    try {
      return machineStatusRepository.save(target);
    } catch (DataAccessException ex) {
      throw new MachineStatusPersistenceException(
          "Failed to classify isolated pause " + target.getId(), ex);
    }
  }

  /**
   * Edit the message of an AUTO_STOPPED record (RF18, RF19, UC12). The
   * audit trail is captured by the global {@code AuditFilter} (RF20).
   * Throws {@link StopNotFoundException} (404) when the id is unknown or
   * the record does not belong to the machine, and
   * {@link StopMessageNotEditableException} (422) when the state is not
   * AUTO_STOPPED.
   *
   * @param machineId the machine UUID from the path
   * @param stopId    the stop UUID from the path
   * @param message   the new message text
   * @param authorId  the user UUID editing the message
   * @return the updated record
   */
  @Transactional
  public MachineStatus editAutoStopMessage(UUID machineId, UUID stopId, String message, UUID authorId) {
    MachineStatus stop = machineStatusRepository.findById(stopId)
        .orElseThrow(() -> new StopNotFoundException("Stop not found: " + stopId));

    if (!stop.getMachineId().equals(machineId)) {
      throw new StopNotFoundException("Stop " + stopId + " does not belong to machine " + machineId);
    }
    if (stop.getState() != MachineState.AUTO_STOPPED) {
      throw new StopMessageNotEditableException(
          "Only AUTO_STOPPED records can have the message edited");
    }

    stop.setMessage(message);
    stop.setReasonAuthorId(authorId);
    try {
      return machineStatusRepository.save(stop);
    } catch (DataAccessException ex) {
      throw new MachineStatusPersistenceException(
          "Failed to edit auto-stop message " + stop.getId(), ex);
    }
  }

  /**
   * Edition history of an AUTO_STOPPED message (UC12, RF18, RF19, RN12).
   * The history is reconstructed from the append-only audit_log instead of
   * a dedicated table: every {@code PUT
   * /machines/{id}/stops/{stopId}/message} request was captured by the
   * global AuditFilter (RF20), so the audit row already carries
   * timestamp, author and the new message JSON. The {@code
   * previousMessage} is resolved by a sliding window over the page plus a
   * single lookup of the entry immediately preceding the page when needed.
   *
   * <p>The result is forcibly ordered by timestamp descending (most recent
   * first) to match the dashboard rendering; the caller's {@code Sort}
   * value is overridden.</p>
   *
   * @param machineId owning machine UUID
   * @param stopId    target stop UUID
   * @param pageable  paging info; sort is overridden to timestamp DESC
   * @return page of {@link StopEditDTO} entries, oldest of the page at the
   *         tail
   * @throws StopNotFoundException when the stop does not exist or does not
   *                               belong to the machine
   */
  public Page<StopEditDTO> findEditHistory(UUID machineId, UUID stopId, Pageable pageable) {
    MachineStatus stop = machineStatusRepository.findById(stopId)
        .orElseThrow(() -> new StopNotFoundException("Stop not found: " + stopId));
    if (!stop.getMachineId().equals(machineId)) {
      throw new StopNotFoundException("Stop " + stopId + " does not belong to machine " + machineId);
    }

    Pageable sorted = PageRequest.of(
        pageable.getPageNumber(), pageable.getPageSize(),
        Sort.by(Sort.Direction.DESC, "timestamp"));
    Page<AuditLog> rawPage = auditService.findStopMessageEdits(machineId, stopId, sorted);
    List<AuditLog> rows = rawPage.getContent();
    if (rows.isEmpty()) {
      return new PageImpl<>(List.of(), sorted, rawPage.getTotalElements());
    }

    String predecessorMessage = auditService
        .findPreviousStopMessageEdit(machineId, stopId, rows.getLast().getTimestamp())
        .map(this::extractMessageFromAudit)
        .orElse(stop.getMessage());

    List<StopEditDTO> entries = new ArrayList<>(rows.size());
    for (int i = 0; i < rows.size(); i++) {
      AuditLog log = rows.get(i);
      String newMessage = extractMessageFromAudit(log);
      String previousMessage = (i == rows.size() - 1)
          ? predecessorMessage
          : extractMessageFromAudit(rows.get(i + 1));
      entries.add(mapper.toStopEditDTO(log, resolveAuthorName(log.getUserId()), previousMessage, newMessage));
    }
    return new PageImpl<>(entries, sorted, rawPage.getTotalElements());
  }

  private String extractMessageFromAudit(AuditLog log) {
    String payload = log.getRequestPayload();
    if (payload == null || payload.isBlank()) {
      return null;
    }
    try {
      JsonNode root = objectMapper.readTree(payload);
      JsonNode message = root.get("message");
      return message == null || message.isNull() ? null : message.asString();
    } catch (JacksonException ex) {
      return null;
    }
  }

  private String resolveAuthorName(UUID authorId) {
    if (authorId == null) {
      return null;
    }
    return userService.findById(authorId).map(User::getName).orElse("(deleted user)");
  }

  /**
   * Confirmed PAUSED/AUTO_STOPPED records waiting to be written to the ERP,
   * ordered by start time ascending. Page size caps the batch the ERP sync
   * writes per window (RF14, RN07). Sole consumer is {@code ErpSyncService}.
   *
   * @param pageable the page request
   * @return confirmed downtime records waiting to be synced
   */
  public List<MachineStatus> findConfirmedDowntimeAwaitingSync(Pageable pageable) {
    return machineStatusRepository.findByRecordStateAndStateInOrderByStartTimeAsc(
        RecordState.CONFIRMED,
        List.of(MachineState.PAUSED, MachineState.AUTO_STOPPED),
        pageable);
  }

  /**
   * Transition the given records from CONFIRMED to SYNCED after the ERP write
   * acknowledged them (RN07). MachineStatusService is the sole owner of the
   * record_state transition, so callers must come through this method.
   *
   * @param records the records to mark
   */
  public void markStatusesAsSynced(Collection<MachineStatus> records) {
    if (records.isEmpty()) {
      return;
    }
    try {
      for (MachineStatus record : records) {
        record.setRecordState(RecordState.SYNCED);
      }
      machineStatusRepository.saveAll(records);
    } catch (DataAccessException ex) {
      throw new MachineStatusPersistenceException(
          "Failed to mark machine_status records as SYNCED", ex);
    }
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