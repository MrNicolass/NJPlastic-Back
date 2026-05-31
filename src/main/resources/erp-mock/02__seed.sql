-- =============================================================================
-- 02__seed.sql -- Mock ERP seed (EP-BE-06 part 2, MVP)
-- =============================================================================
-- Populates the mock ERP with six open production orders so the first sync
-- window of ErpSyncScheduler has data to read and to write into
-- production_order_cache. ID format follows the convention "OP-YYYY-NNNN" so it
-- does not collide with the local MOCK-OP-* seed in V5__seed_production_order_cache.sql:
-- the first scheduled sync window will overwrite the local seed with these rows.
-- =============================================================================

INSERT INTO meplas_ordens_producao
    (id_op, codigo_maquina, codigo_produto, quantidade_alvo, status, payload)
VALUES
    ('OP-2026-0001', 'MAQ-01', 'PVC-100ML-BR', 5000, 'ABERTA',
     '{"cliente":"Acme Embalagens","molde":"MLD-12-A","prazo":"2026-06-15","cavidades":4}'),
    ('OP-2026-0002', 'MAQ-02', 'PET-500ML-AZ', 8000, 'ABERTA',
     '{"cliente":"Bravo Bebidas","molde":"MLD-07-C","prazo":"2026-06-10","cavidades":8}'),
    ('OP-2026-0003', 'MAQ-03', 'PP-TAMPA-AM',  12000, 'ABERTA',
     '{"cliente":"Cinco Cosmeticos","molde":"MLD-22-B","prazo":"2026-06-20","cavidades":16}'),
    ('OP-2026-0004', 'MAQ-04', 'PEAD-GALAO-VD', 1500, 'ABERTA',
     '{"cliente":"Delta Quimica","molde":"MLD-03-A","prazo":"2026-06-08","cavidades":2}'),
    ('OP-2026-0005', 'MAQ-05', 'ABS-CAIXA-PR',  700, 'ABERTA',
     '{"cliente":"Echo Industria","molde":"MLD-19-D","prazo":"2026-06-25","cavidades":1}'),
    ('OP-2026-0006', 'MAQ-06', 'PS-COPO-TR',   9500, 'ABERTA',
     '{"cliente":"Foxtrot Food","molde":"MLD-14-A","prazo":"2026-06-12","cavidades":12}');
