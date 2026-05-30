-- =============================================================================
-- V4__quality_record.sql  --  quality counts for OEE Quality factor (RF10)
-- =============================================================================
-- Captures good/total part counts registered by a user at the end of a
-- production order. Consumed by OeeService to complete the Quality factor; when
-- no record covers a period, OEE is returned as partial (Availability x
-- Performance only). machine_id and registered_by are plain UUIDs without
-- REFERENCES (RFC §5.2) - integrity is enforced in the service layer.
-- =============================================================================

CREATE TABLE quality_record (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id    UUID         NOT NULL,
    erp_order_id  VARCHAR(64),
    period_start  TIMESTAMPTZ  NOT NULL,
    period_end    TIMESTAMPTZ  NOT NULL,
    good_count    INTEGER      NOT NULL,
    total_count   INTEGER      NOT NULL,
    registered_by UUID,
    registered_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_quality_record_machine_period ON quality_record (machine_id, period_start);
