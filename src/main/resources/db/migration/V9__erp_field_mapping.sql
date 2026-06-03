-- =============================================================================
-- V9__erp_field_mapping.sql  --  ERP field mapping (EP-BE-06 reopened)
-- =============================================================================
-- Persists the NJPlastic <-> ERP field correspondence shown on the Gestor
-- screen (mockup ERP_Part2_V1). entity_type groups mappings per ERP table
-- (e.g. PRODUCTION_ORDER); nj_field is the local property name; erp_field is
-- the ERP column name; data_type narrows the JDBC binding; required toggles
-- the NOT NULL semantic on the ERP side. updated_by/updated_at trace the last
-- edit for the audit panel (audit_log captures the full diff via AuditFilter).
-- =============================================================================

CREATE TABLE erp_field_mapping (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(64)  NOT NULL,
    nj_field    VARCHAR(128) NOT NULL,
    erp_field   VARCHAR(128) NOT NULL,
    data_type   VARCHAR(32)  NOT NULL,
    required    BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by  UUID,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (entity_type, nj_field)
);

CREATE INDEX idx_erp_field_mapping_entity_type ON erp_field_mapping (entity_type);
