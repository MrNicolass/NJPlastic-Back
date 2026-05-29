-- =============================================================================
-- V3__audit_log_retention_policy.sql  --  audit_log retention policy (RF20, RN12, RNF08)
-- =============================================================================
-- Documental migration. The audit_log table is append-only: the API exposes no
-- UPDATE or DELETE and the AuditRepository declares no mutating operation
-- (RFC 6.5). Minimum retention is 5 years, justified by LGPD Art. 7 II and VI
-- (RFC 6.1.5). No purge mechanism is provided on purpose - records are never
-- deleted before the legal window, and the 5-year minimum is satisfied by the
-- absence of deletion. These COMMENT statements register the policy in the
-- database catalog for operators and auditors.
-- =============================================================================

COMMENT ON TABLE audit_log IS
  'Append-only request audit trail (RF20, RN12, RNF08). Never receives UPDATE or DELETE. Minimum retention 5 years (LGPD Art. 7 II and VI).';

COMMENT ON COLUMN audit_log.user_id IS
  'Author UUID resolved from the security context; NULL for anonymous calls (e.g. failed POST /auth/login).';

COMMENT ON COLUMN audit_log.request_payload IS
  'Sanitized request body as JSON; passwords, tokens and secrets replaced by [REDACTED]. Oversized or non-JSON bodies collapse to a small JSON placeholder.';

COMMENT ON COLUMN audit_log.response_payload IS
  'Sanitized response body as JSON; passwords, tokens and secrets replaced by [REDACTED]. Oversized or non-JSON bodies collapse to a small JSON placeholder.';
