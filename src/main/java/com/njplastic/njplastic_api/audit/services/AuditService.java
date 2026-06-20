package com.njplastic.njplastic_api.audit.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.audit.entities.AuditLog;
import com.njplastic.njplastic_api.audit.repositories.AuditRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link AuditRepository}. Other layers (the AuditFilter, future
 * message-edit auditing per) must persist audit entries through this
 * service rather than touching the repository directly, keeping a single
 * append-only entry point.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

  private final AuditRepository auditRepository;

 /**
 * Persist an audit entry. A persistence failure is logged and swallowed on
 * purpose: the audit trail must never break the request it is observing, so
 * this is a legitimate recovery case rather than a re-throwing wrapper.
 *
 * @param auditLog the entry to store
 */
  public void saveAudit(AuditLog auditLog) {
    try {
      auditRepository.save(auditLog);
    } catch (RuntimeException e) {
      LOGGER.error("Failed to persist audit log for {} {}: {}",
          auditLog.getHttpMethod(), auditLog.getEndpoint(), e.getMessage());
    }
  }

 /**
 * Paginated read of the audit trail with optional filters (* sub-task 4). Used by {@code GET /audit-logs}. The audit table stays
 * append-only - this method is strictly read-side.
 *
 * @param userId optional author UUID filter
 * @param endpoint optional endpoint substring filter (case-insensitive)
 * @param httpMethod optional HTTP method filter (e.g. POST, PUT)
 * @param httpStatus optional HTTP status code filter
 * @param from optional inclusive lower bound on timestamp
 * @param to optional inclusive upper bound on timestamp
 * @param pageable paging/sort
 * @return page of audit entries matching every supplied filter
 */
  public Page<AuditLog> findPaged(UUID userId, String endpoint, String httpMethod, Integer httpStatus,
      OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
    Specification<AuditLog> spec = (root, query, cb) -> {
      Predicate predicate = cb.conjunction();
      if (userId != null) {
        predicate = cb.and(predicate, cb.equal(root.get("userId"), userId));
      }
      if (endpoint != null && !endpoint.isBlank()) {
        predicate = cb.and(predicate, cb.like(cb.upper(root.get("endpoint")),
            "%" + endpoint.toUpperCase(Locale.ROOT) + "%"));
      }
      if (httpMethod != null && !httpMethod.isBlank()) {
        predicate = cb.and(predicate, cb.equal(cb.upper(root.get("httpMethod")),
            httpMethod.toUpperCase(Locale.ROOT)));
      }
      if (httpStatus != null) {
        predicate = cb.and(predicate, cb.equal(root.get("httpStatus"), httpStatus));
      }
      if (from != null) {
        predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("timestamp"), from));
      }
      if (to != null) {
        predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("timestamp"), to));
      }
      return predicate;
    };
    return auditRepository.findAll(spec, pageable);
  }

 /**
 * Edition history of an AUTO_STOPPED message reconstructed from the
 * append-only audit trail. Filters on the exact
 * endpoint prefix produced by {@code PUT
 * /machines/{machineId}/stops/{stopId}/message} requests, keeping only the
 * successful ones (HTTP 200), so failed attempts do not leak into the
 * history shown to the user.
 *
 * @param machineId owning machine UUID, validated by the caller
 * @param stopId target stop UUID, validated by the caller
 * @param pageable paging/sort - the service forces a deterministic
 * timestamp-descending order
 * @return page of audit entries that materialized a stored edition
 */
  public Page<AuditLog> findStopMessageEdits(UUID machineId, UUID stopId, Pageable pageable) {
    String prefix = buildStopMessageEndpointPrefix(machineId, stopId);
    return auditRepository
        .findByHttpMethodAndEndpointStartingWithAndHttpStatusOrderByTimestampDesc(
            "PUT", prefix, 200, pageable);
  }

 /**
 * Latest successful edition strictly before {@code before}. Used to
 * resolve {@code previousMessage} for the oldest entry of a page, since
 * that entry's predecessor lives outside the page.
 *
 * @param machineId owning machine UUID
 * @param stopId target stop UUID
 * @param before exclusive upper bound on the captured timestamp
 * @return the predecessor entry, or empty when none exists
 */
  public Optional<AuditLog> findPreviousStopMessageEdit(UUID machineId, UUID stopId, OffsetDateTime before) {
    String prefix = buildStopMessageEndpointPrefix(machineId, stopId);
    return auditRepository
        .findTopByHttpMethodAndEndpointStartingWithAndHttpStatusAndTimestampBeforeOrderByTimestampDesc(
            "PUT", prefix, 200, before);
  }

  private String buildStopMessageEndpointPrefix(UUID machineId, UUID stopId) {
    return "/machines/" + machineId + "/stops/" + stopId + "/message";
  }

 /**
 * Successful stop-message edits captured in the given window across every
 * machine, ordered by timestamp descending. Backs the Leader "Eventos
 * recentes" feed (item 6).
 *
 * @param from inclusive lower bound on timestamp
 * @param to exclusive upper bound on timestamp
 * @return matching audit entries
 */
  public List<AuditLog> findStopMessageEditsInWindow(OffsetDateTime from, OffsetDateTime to) {
    return auditRepository.findStopMessageEditsInWindow(from, to);
  }
}