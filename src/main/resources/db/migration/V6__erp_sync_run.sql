-- =============================================================================
-- V6__erp_sync_run.sql  --  ERP sync history (EP-BE-06 part 2, UC08)
-- =============================================================================
-- Persists one row per ErpSyncScheduler execution so the GET /erp/sync/status
-- endpoint (Manager only, RN04) can expose the KPIs the Figura 26a/b mockup
-- requires: last run, next window, success rate over the last 24h, average
-- latency, counters of orders read and cycles/pauses written, and the last
-- error message when the run failed. Counters and finished_at are NULL while
-- the run is in progress; error_message is NULL on success. Idempotency of
-- the writes themselves stays in the ERP repository (chave por id local, RN08);
-- this table is observability-only and never the source of truth for record
-- state transitions, which remain owned by ProductionService /
-- MachineStatusService.
-- =============================================================================

CREATE TABLE erp_sync_run (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    started_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    status          VARCHAR(16)  NOT NULL,
    orders_read     INTEGER,
    cycles_written  INTEGER,
    pauses_written  INTEGER,
    duration_ms     INTEGER,
    error_message   TEXT,
    CONSTRAINT erp_sync_run_status_check
        CHECK (status IN ('RUNNING', 'SUCCESS', 'ERROR', 'PARTIAL'))
);

CREATE INDEX idx_erp_sync_run_started ON erp_sync_run (started_at DESC);
