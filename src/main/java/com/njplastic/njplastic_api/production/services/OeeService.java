package com.njplastic.njplastic_api.production.services;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.production.dtos.OeeResultDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.QualityRecord;
import com.njplastic.njplastic_api.production.exceptions.UnknownMachineException;

import lombok.RequiredArgsConstructor;

/**
 * Computes OEE (Availability x Performance x Quality) for a machine over
 * a window. Availability and Performance always come from production_cycle and
 * machine_status; the Quality factor is optional and read from quality_record.
 * When no quality record overlaps the window the result is flagged as partial
 * and OEE is computed assuming Q = 1.0, so the UI can display a provisional
 * percentage while keeping the partial flag as the single source of truth for
 * "no quality data yet". Owns no repository - it orchestrates the production
 * aggregates through their services.
 */
@Service
@RequiredArgsConstructor
public class OeeService {

  private final MachineService machineService;
  private final ProductionService productionService;
  private final MachineStatusService machineStatusService;
  private final QualityService qualityService;

 /**
 * Compute the OEE for a machine within [from, to]. When no quality record
 * overlaps the window the result is marked partial and OEE assumes Q = 1.0.
 *
 * @param machineId the machine UUID
 * @param from window start
 * @param to window end
 * @return the OEE result, with partial=true when no quality data covers the
 * window
 */
  public OeeResultDTO calculate(UUID machineId, OffsetDateTime from, OffsetDateTime to) {
    Machine machine = machineService.findById(machineId)
        .orElseThrow(() -> new UnknownMachineException("Machine not found: " + machineId));

    long plannedMs = Math.max(0L, Duration.between(from, to).toMillis());
    if (plannedMs == 0L) {
      return OeeResultDTO.builder()
          .machineId(machineId)
          .periodStart(from)
          .periodEnd(to)
          .availability(0.0)
          .performance(0.0)
          .quality(null)
          .oee(0.0)
          .partial(true)
          .build();
    }

    long downtimeMs = downtimeMs(machineId, from, to);
    long runMs = Math.max(0L, plannedMs - downtimeMs);
    double availability = (double) runMs / plannedMs;

    long confirmedCycles = productionService.countConfirmedCycles(machineId, from, to);
    long idealMs = confirmedCycles * machine.getStandardCycleMs();
    double performance = runMs > 0 ? Math.min(1.0, (double) idealMs / runMs) : 0.0;

    Double quality = quality(machineId, from, to);
    boolean partial = quality == null;
    double qualityFactor = partial ? 1.0 : quality;
    Double oee = availability * performance * qualityFactor;

    return OeeResultDTO.builder()
        .machineId(machineId)
        .periodStart(from)
        .periodEnd(to)
        .availability(availability)
        .performance(performance)
        .quality(quality)
        .oee(oee)
        .partial(partial)
        .build();
  }

  private long downtimeMs(UUID machineId, OffsetDateTime from, OffsetDateTime to) {
    List<MachineStatus> records = machineStatusService.findDowntimeOverlapping(machineId, from, to);
    long total = 0L;
    for (MachineStatus record : records) {
      OffsetDateTime start = record.getStartTime().isAfter(from) ? record.getStartTime() : from;
      OffsetDateTime end = record.getEndTime() == null || record.getEndTime().isAfter(to)
          ? to
          : record.getEndTime();
      if (end.isAfter(start)) {
        total += Duration.between(start, end).toMillis();
      }
    }
    return total;
  }

  private Double quality(UUID machineId, OffsetDateTime from, OffsetDateTime to) {
    List<QualityRecord> records = qualityService.findForPeriod(machineId, from, to);
    if (records.isEmpty()) {
      return null;
    }
    long good = 0L;
    long total = 0L;
    for (QualityRecord record : records) {
      good += record.getGoodCount();
      total += record.getTotalCount();
    }
    if (total <= 0L) {
      return null;
    }
    return (double) good / total;
  }
}
