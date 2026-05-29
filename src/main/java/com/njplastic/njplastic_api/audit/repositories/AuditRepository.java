package com.njplastic.njplastic_api.audit.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.audit.entities.AuditLog;

/**
 * Append-only access to the "audit_log" table (RF20, RN12, RNF08). Only the
 * inherited insert path is used; no update or delete operation is declared,
 * preserving the immutability of the audit trail.
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditLog, UUID> {
}