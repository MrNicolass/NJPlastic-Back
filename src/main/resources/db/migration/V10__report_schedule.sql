-- =============================================================================
-- V10__report_schedule.sql  --  Report scheduling (EP-BE-08 sub-task 5)
-- =============================================================================
-- Persists the Manager-defined report schedules consumed by ReportSchedulerJob.
-- params is a free-form JSON document carrying the inputs for the chosen type
-- (e.g. SHIFT report -> {"sector":"INJECAO","shift":"TURNO_A"}). cron follows
-- Spring's 6-field syntax (second minute hour day-of-month month day-of-week).
-- created_by has no FK to users (RFC §5.2.1).
-- =============================================================================

CREATE TABLE report_schedule (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    type           VARCHAR(32)  NOT NULL,
    params         JSONB,
    cron           VARCHAR(64)  NOT NULL,
    delivery_email VARCHAR(255) NOT NULL,
    format         VARCHAR(8)   NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by     UUID,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_report_schedule_active ON report_schedule (active);
CREATE INDEX idx_report_schedule_type   ON report_schedule (type);
