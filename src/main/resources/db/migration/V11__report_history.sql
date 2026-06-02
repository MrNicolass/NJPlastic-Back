-- =============================================================================
-- V11__report_history.sql  --  Report generation history (EP-BE-08 sub-task 5)
-- =============================================================================
-- One row per generated artifact. schedule_id may be null when a Manager runs
-- a report on-demand outside any schedule. Retention is enforced by
-- ReportRetentionJob (delete after 90 days, both the row and the file at
-- {path}).
-- =============================================================================

CREATE TABLE report_history (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id   UUID,
    type          VARCHAR(32)  NOT NULL,
    generated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    format        VARCHAR(8)   NOT NULL,
    path          VARCHAR(512) NOT NULL,
    size_bytes    BIGINT       NOT NULL
);

CREATE INDEX idx_report_history_generated_at ON report_history (generated_at);
CREATE INDEX idx_report_history_schedule_id  ON report_history (schedule_id);
CREATE INDEX idx_report_history_type         ON report_history (type);
