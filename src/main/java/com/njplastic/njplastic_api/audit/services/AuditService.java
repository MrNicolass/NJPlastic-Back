package com.njplastic.njplastic_api.audit.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.audit.entities.AuditLog;
import com.njplastic.njplastic_api.audit.repositories.AuditRepository;

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
}