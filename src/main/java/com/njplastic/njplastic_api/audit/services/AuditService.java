package com.njplastic.njplastic_api.audit.services;

import java.time.OffsetDateTime;
import java.util.Locale;
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
 * message-edit auditing per RN12) must persist audit entries through this
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
   * Paginated read of the audit trail with optional filters (EP-BE-08
   * sub-task 4). Used by {@code GET /audit-logs}. The audit table stays
   * append-only - this method is strictly read-side.
   *
   * @param userId     optional author UUID filter
   * @param endpoint   optional endpoint substring filter (case-insensitive)
   * @param httpMethod optional HTTP method filter (e.g. POST, PUT)
   * @param httpStatus optional HTTP status code filter
   * @param from       optional inclusive lower bound on timestamp
   * @param to         optional inclusive upper bound on timestamp
   * @param pageable   paging/sort
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
}