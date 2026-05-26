-- =============================================================================
-- V1__init.sql  --  NJPlastic initial schema (RFC §5.2)
-- =============================================================================
-- Sem REFERENCES declaradas: machine_id, user_id e reason_author_id sao UUIDs
-- simples (RFC §5.2.1). Integridade referencial e responsabilidade da camada de
-- servico (ProductionService).
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================================
-- ENUM types
-- =============================================================================

CREATE TYPE user_role     AS ENUM ('OPERATOR', 'LEADER', 'MANAGER');
CREATE TYPE machine_state AS ENUM ('RUNNING', 'PAUSED', 'AUTO_STOPPED', 'OFFLINE');
CREATE TYPE record_state  AS ENUM ('PENDING', 'CONFIRMED', 'SYNCED', 'DISCARDED');

-- =============================================================================
-- users  (RF03, RF04, RN01-RN04)
-- =============================================================================

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    login         VARCHAR(64)  NOT NULL UNIQUE,
    name          VARCHAR(128) NOT NULL,
    email         VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          user_role    NOT NULL,
    sector        VARCHAR(64),
    shift         VARCHAR(32),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- =============================================================================
-- machine  (RF06, RN06, RN09)
-- =============================================================================

CREATE TABLE machine (
    id                            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code                          VARCHAR(32)  NOT NULL UNIQUE,
    description                   VARCHAR(255),
    sector                        VARCHAR(64),
    standard_cycle_ms             INTEGER      NOT NULL,
    tolerance_factor              NUMERIC(5,2) NOT NULL,
    consecutive_pauses_to_stop    INTEGER      NOT NULL,
    offline_window_ms             INTEGER      NOT NULL DEFAULT 60000,
    active                        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- =============================================================================
-- production_cycle  (RF01, RF07, RN05, RN07)
-- =============================================================================

CREATE TABLE production_cycle (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id        UUID         NOT NULL,
    pulse_timestamp   TIMESTAMPTZ  NOT NULL,
    received_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sequence          BIGINT       NOT NULL,
    interval_ms       INTEGER,
    state             record_state NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_production_cycle_machine_ts ON production_cycle (machine_id, pulse_timestamp);
CREATE INDEX idx_production_cycle_state      ON production_cycle (state);

-- =============================================================================
-- machine_status  (RF08, RF09, RF17, RF18, RN06-RN11)
-- =============================================================================

CREATE TABLE machine_status (
    id                            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id                    UUID          NOT NULL,
    state                         machine_state NOT NULL,
    reason                        VARCHAR(255),
    message                       TEXT,
    start_time                    TIMESTAMPTZ   NOT NULL,
    end_time                      TIMESTAMPTZ,
    reason_author_id              UUID,
    consecutive_count_at_creation INTEGER,
    record_state                  record_state  NOT NULL DEFAULT 'PENDING',
    created_at                    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_machine_status_machine_start ON machine_status (machine_id, start_time);
CREATE INDEX idx_machine_status_state         ON machine_status (state);

-- =============================================================================
-- audit_log  (RF20, RN12, RNF08)  --  append-only
-- =============================================================================

CREATE TABLE audit_log (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_id          UUID,
    http_method      VARCHAR(8)   NOT NULL,
    endpoint         VARCHAR(512) NOT NULL,
    http_status      INTEGER      NOT NULL,
    request_payload  JSONB,
    response_payload JSONB,
    source_ip        INET,
    duration_ms      INTEGER
);

CREATE INDEX idx_audit_log_user_ts     ON audit_log (user_id, timestamp);
CREATE INDEX idx_audit_log_endpoint_ts ON audit_log (endpoint, timestamp);

-- =============================================================================
-- production_order_cache  (RF13, RF14)  --  buffer de leitura do ERP
-- =============================================================================

CREATE TABLE production_order_cache (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    erp_order_id    VARCHAR(64)  NOT NULL,
    machine_id      UUID,
    product_code    VARCHAR(64),
    target_quantity INTEGER,
    status          VARCHAR(32),
    payload         JSONB,
    last_sync_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (erp_order_id)
);
