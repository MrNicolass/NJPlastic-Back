-- =============================================================================
-- 01__schema.sql -- Mock ERP schema (EP-BE-06 part 2, MVP)
-- =============================================================================
-- This script imitates a generic ERP database that the customer (Meplas) will
-- expose in production. It is mounted into /docker-entrypoint-initdb.d/ of the
-- mock Postgres container so it runs once on the first boot of the data volume.
-- The DDL deliberately stays outside Flyway: the real Meplas database will not
-- be managed by Flyway either, and the NJPlastic backend talks to it via raw
-- JDBC (RNF13). Table and column names are in Portuguese to mirror typical
-- Brazilian ERP naming conventions and stress-test the SQL-via-properties
-- approach that lets the same binary serve different ERP schemas.
-- =============================================================================

CREATE TABLE IF NOT EXISTS meplas_ordens_producao (
    id_op            VARCHAR(40)  PRIMARY KEY,
    codigo_maquina   VARCHAR(40),
    codigo_produto   VARCHAR(40),
    quantidade_alvo  INTEGER,
    status           VARCHAR(16)  NOT NULL,
    payload          TEXT,
    atualizado_em    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS meplas_apontamentos (
    id_apontamento   UUID         PRIMARY KEY,
    codigo_maquina   VARCHAR(40)  NOT NULL,
    pulso_em         TIMESTAMPTZ  NOT NULL,
    sequencia        BIGINT       NOT NULL,
    intervalo_ms     BIGINT,
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_meplas_apontamentos_maquina_pulso
    ON meplas_apontamentos (codigo_maquina, pulso_em DESC);

CREATE TABLE IF NOT EXISTS meplas_paradas (
    id_parada              UUID         PRIMARY KEY,
    codigo_maquina         VARCHAR(40)  NOT NULL,
    estado                 VARCHAR(16)  NOT NULL,
    motivo                 VARCHAR(64),
    mensagem               TEXT,
    inicio_em              TIMESTAMPTZ  NOT NULL,
    fim_em                 TIMESTAMPTZ,
    contagem_consecutiva   INTEGER,
    criado_em              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_meplas_paradas_maquina_inicio
    ON meplas_paradas (codigo_maquina, inicio_em DESC);
