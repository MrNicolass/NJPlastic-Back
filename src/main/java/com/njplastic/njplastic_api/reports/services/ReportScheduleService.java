package com.njplastic.njplastic_api.reports.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;
import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.entities.ReportSchedule;
import com.njplastic.njplastic_api.reports.enums.ReportType;
import com.njplastic.njplastic_api.reports.repositories.ReportHistoryRepository;
import com.njplastic.njplastic_api.reports.repositories.ReportScheduleRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link ReportScheduleRepository} and
 * {@link ReportHistoryRepository}. Exposes the CRUD consumed by the Manager
 * UI and the paginated read of {@code report_history} for the 90-day
 * library tab.
 */
@Service
@RequiredArgsConstructor
public class ReportScheduleService {

  private final ReportScheduleRepository scheduleRepository;
  private final ReportHistoryRepository historyRepository;

 /**
 * Persist a new schedule. The schedule is created active.
 *
 * @param schedule fully populated entity (created_by must be set by the caller)
 * @return the persisted entity
 */
  @Transactional
  public ReportSchedule create(ReportSchedule schedule) {
    schedule.setActive(true);
    return scheduleRepository.save(schedule);
  }

 /**
 * Delete a schedule by id. Past {@code report_history} rows are kept.
 *
 * @param id schedule UUID
 * @throws BaseApiNotFoundException if no schedule matches
 */
  @Transactional
  public void delete(UUID id) {
    if (!scheduleRepository.existsById(id)) {
      throw new BaseApiNotFoundException("Report schedule not found: " + id);
    }
    scheduleRepository.deleteById(id);
  }

 /**
 * @return every active schedule, used by the scheduler tick.
 */
  public List<ReportSchedule> findAllActive() {
    return scheduleRepository.findAllByActiveTrue();
  }

 /**
 * Persist a generation record.
 *
 * @param history fully populated entity
 * @return the persisted entity
 */
  @Transactional
  public ReportHistory recordGeneration(ReportHistory history) {
    return historyRepository.save(history);
  }

 /**
 * Paginated read of {@code report_history} consumed by the library tab.
 *
 * @param type optional report category filter
 * @param from optional inclusive lower bound on generatedAt
 * @param to optional inclusive upper bound on generatedAt
 * @param pageable paging/sort
 * @return page of history rows
 */
  public Page<ReportHistory> findHistory(ReportType type, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
    Specification<ReportHistory> spec = (root, query, cb) -> {
      Predicate predicate = cb.conjunction();
      if (type != null) {
        predicate = cb.and(predicate, cb.equal(root.get("type"), type));
      }
      if (from != null) {
        predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("generatedAt"), from));
      }
      if (to != null) {
        predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("generatedAt"), to));
      }
      return predicate;
    };
    return historyRepository.findAll(spec, pageable);
  }

 /**
 * @param cutoff timestamp before which rows are eligible for retention deletion
 * @return rows to delete (both the JPA entity and the file at {@code path})
 */
  public List<ReportHistory> findExpiredHistory(OffsetDateTime cutoff) {
    return historyRepository.findAllByGeneratedAtBefore(cutoff);
  }

 /**
 * Delete a generation record after the file on disk has been removed.
 *
 * @param id history UUID
 */
  @Transactional
  public void deleteHistory(UUID id) {
    historyRepository.deleteById(id);
  }
}
