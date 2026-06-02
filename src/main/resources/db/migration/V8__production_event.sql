-- =============================================================================
-- V8__production_event.sql  --  Manual production event (EP-BE-05 reopened)
-- =============================================================================
-- Carries non-cycle, non-pause events relevant to OEE narratives: training,
-- cleaning, meetings, etc. Mockup Dashboard_Part1_V1. machine_id and user_id
-- stay un-FK'd per RFC §5.2.1; integrity is enforced at the service layer.
-- =============================================================================

CREATE TYPE event_type AS ENUM ('TRAINING', 'CLEANING', 'MEETING', 'OTHER');

CREATE TABLE production_event (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id  UUID         NOT NULL,
    user_id     UUID,
    type        event_type   NOT NULL,
    description TEXT,
    started_at  TIMESTAMPTZ  NOT NULL,
    ended_at    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_production_event_machine_started ON production_event (machine_id, started_at);
CREATE INDEX idx_production_event_type            ON production_event (type);
