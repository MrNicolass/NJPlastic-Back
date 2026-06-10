-- =============================================================================
-- 03__seed_tcc_demo.sql -- Mock ERP seed for the TCC demo (ClickUp 868jzeq7v)
-- =============================================================================
-- Expands 02__seed.sql so the /ordens screen shows the full status mix
-- (ABERTA already there, plus EM_PRODUCAO and CONCLUIDA), and the apontamentos
-- / paradas tables ship with historical data the demo can show before the
-- first ErpSyncScheduler window writes anything.
--
-- Order matters: /docker-entrypoint-initdb.d/ runs files alphabetically, so
-- 03 is loaded AFTER 01 (schema) and 02 (ABERTA orders).
--
-- UUIDs intentionally do NOT match production_cycle / machine_status UUIDs in
-- the application DB: the ErpSyncScheduler insert queries are idempotent via
-- ON CONFLICT (id_apontamento) / (id_parada), so when the sync runs against
-- the live data it appends new rows alongside these historical ones without
-- collision.
-- =============================================================================

-- =============================================================================
-- 1. Extra production orders (EM_PRODUCAO + CONCLUIDA)
--    The 6 ABERTA orders from 02__seed.sql remain so the find-open-orders
--    query keeps returning them; these extras populate /ordens with mixed
--    status badges and historical entries.
-- =============================================================================

INSERT INTO meplas_ordens_producao
    (id_op, codigo_maquina, codigo_produto, quantidade_alvo, status, payload, atualizado_em)
VALUES
    ('OP-2026-0007', 'MAQ-01', 'PVC-100ML-BR',  4500, 'EM_PRODUCAO',
        '{"cliente":"Acme Embalagens","molde":"MLD-12-A","prazo":"2026-06-18","cavidades":4}',
        now() - INTERVAL '2 hour'),
    ('OP-2026-0008', 'MAQ-02', 'PET-500ML-AZ',  7500, 'EM_PRODUCAO',
        '{"cliente":"Bravo Bebidas","molde":"MLD-07-C","prazo":"2026-06-14","cavidades":8}',
        now() - INTERVAL '90 minute'),
    ('OP-2026-0009', 'MAQ-03', 'PP-TAMPA-AM',  12000, 'CONCLUIDA',
        '{"cliente":"Cinco Cosmeticos","molde":"MLD-22-B","prazo":"2026-06-05","cavidades":16}',
        now() - INTERVAL '40 hour'),
    ('OP-2026-0010', 'MAQ-04', 'PEAD-GALAO-VD', 1200, 'EM_PRODUCAO',
        '{"cliente":"Delta Quimica","molde":"MLD-03-A","prazo":"2026-06-12","cavidades":2}',
        now() - INTERVAL '30 minute'),
    ('OP-2026-0011', 'MAQ-06', 'PS-COPO-TR',    9500, 'CONCLUIDA',
        '{"cliente":"Foxtrot Food","molde":"MLD-14-A","prazo":"2026-06-04","cavidades":12}',
        now() - INTERVAL '36 hour'),
    ('OP-2026-0012', 'MAQ-05', 'ABS-CAIXA-PR',   850, 'CONCLUIDA',
        '{"cliente":"Echo Industria","molde":"MLD-19-D","prazo":"2026-06-02","cavidades":1}',
        now() - INTERVAL '52 hour'),
    ('OP-2026-0013', 'MAQ-01', 'PVC-100ML-BR',  3200, 'CONCLUIDA',
        '{"cliente":"Acme Embalagens","molde":"MLD-12-A","prazo":"2026-05-30","cavidades":4}',
        now() - INTERVAL '80 hour'),
    ('OP-2026-0014', 'MAQ-02', 'PET-500ML-AZ',  6800, 'CONCLUIDA',
        '{"cliente":"Bravo Bebidas","molde":"MLD-07-C","prazo":"2026-05-28","cavidades":8}',
        now() - INTERVAL '100 hour');

-- =============================================================================
-- 2. Apontamentos (cycles) - historical data, ~60 records spread across
--    MAQ-01..MAQ-04 + MAQ-06. MAQ-05 (OFFLINE) gets a thin slice.
--    Uses generate_series with a deterministic uuid_in() derivation per row
--    so re-running the script (e.g. dropping the volume) yields the same IDs.
-- =============================================================================

WITH machine_codes AS (
    SELECT * FROM (VALUES
        ('MAQ-01', 2000, 'a1'),
        ('MAQ-02', 2500, 'a2'),
        ('MAQ-03', 1800, 'a3'),
        ('MAQ-04', 3000, 'a4'),
        ('MAQ-06', 3500, 'a6')
    ) AS t(codigo, standard_cycle_ms, prefix)
),
grid AS (
    SELECT
        m.codigo,
        m.standard_cycle_ms,
        m.prefix,
        gs AS pulso_em,
        ROW_NUMBER() OVER (PARTITION BY m.codigo ORDER BY gs) AS sequencia
    FROM machine_codes m
    CROSS JOIN generate_series(
        now() - INTERVAL '24 hour',
        now() - INTERVAL '13 hour',
        INTERVAL '60 minute'
    ) AS gs
)
INSERT INTO meplas_apontamentos (id_apontamento, codigo_maquina, pulso_em, sequencia, intervalo_ms)
SELECT
    gen_random_uuid(),
    codigo,
    pulso_em,
    sequencia,
    standard_cycle_ms + ((random() - 0.5) * 200)::int
FROM grid;

-- =============================================================================
-- 3. Paradas (stops) - ~10 records aligned with realistic operations:
--    PAUSED (TROCA_DE_MOLDE / AGUARDANDO_INSUMO / MANUTENCAO_PREVENTIVA) and
--    AUTO_STOPPED (PARADA_AUTOMATICA).
-- =============================================================================

INSERT INTO meplas_paradas
    (id_parada, codigo_maquina, estado, motivo, mensagem, inicio_em, fim_em, contagem_consecutiva)
VALUES
    (gen_random_uuid(), 'MAQ-01', 'PAUSED', 'TROCA_DE_MOLDE', 'Troca para MLD-12-A',
        now() - INTERVAL '20 hour', now() - INTERVAL '19 hour 30 minute', 1),
    (gen_random_uuid(), 'MAQ-01', 'PAUSED', 'MANUTENCAO_PREVENTIVA', 'Lubrificacao guias',
        now() - INTERVAL '8 hour', now() - INTERVAL '7 hour 40 minute', 2),
    (gen_random_uuid(), 'MAQ-02', 'PAUSED', 'TROCA_DE_TURNO', 'Passagem TURNO_A -> TURNO_B',
        now() - INTERVAL '14 hour', now() - INTERVAL '13 hour 50 minute', 1),
    (gen_random_uuid(), 'MAQ-03', 'PAUSED', 'TROCA_DE_MOLDE', 'Troca para PVC-100ML-BR',
        now() - INTERVAL '8 hour', now() - INTERVAL '7 hour 30 minute', 1),
    (gen_random_uuid(), 'MAQ-03', 'PAUSED', 'TROCA_DE_MOLDE', 'Troca para PP-TAMPA-AM',
        now() - INTERVAL '20 minute', NULL, 2),
    (gen_random_uuid(), 'MAQ-04', 'PAUSED', 'AGUARDANDO_INSUMO', 'Falta de polipropileno',
        now() - INTERVAL '4 hour', now() - INTERVAL '3 hour 30 minute', 1),
    (gen_random_uuid(), 'MAQ-04', 'AUTO_STOPPED', 'PARADA_AUTOMATICA',
        'Parada detectada automaticamente apos 3 pausas consecutivas',
        now() - INTERVAL '2 hour', NULL, 3),
    (gen_random_uuid(), 'MAQ-05', 'OFFLINE', NULL, NULL,
        now() - INTERVAL '5 hour 10 minute', NULL, NULL),
    (gen_random_uuid(), 'MAQ-06', 'PAUSED', 'AJUSTE_DE_PARAMETROS', 'Ajuste fino de temperatura',
        now() - INTERVAL '11 hour', now() - INTERVAL '10 hour 35 minute', 1),
    (gen_random_uuid(), 'MAQ-06', 'PAUSED', 'LIMPEZA_DE_ROSCA', 'Limpeza programada',
        now() - INTERVAL '36 hour', now() - INTERVAL '35 hour 30 minute', 1);
