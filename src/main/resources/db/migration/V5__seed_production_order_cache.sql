-- =============================================================================
-- V5__seed_production_order_cache.sql  --  ERP mock seed (EP-BE-06 part 1, UC11)
-- =============================================================================
-- Sprint 5 deliverable: populate production_order_cache with open orders so
-- UC11 (operator chooses a production order) has data to read without the real
-- JDBC integration to the Customer ERP. The real ErpDatabaseRepository and
-- ErpSyncScheduler arrive in EP-BE-06 part 2 (Sprint 6) and will overwrite
-- this seed at each sync window - production_order_cache is a volatile buffer
-- by design (RFC 5.2.1 and 6.1.5). machine_id is left NULL because the MVP
-- ships no machine seed yet; binding will happen when the ERP sync runs or a
-- machine seed lands. erp_order_id values stay outside any real ERP namespace
-- so the Sprint 6 sync cannot collide on the UNIQUE constraint.
-- =============================================================================

INSERT INTO production_order_cache
    (erp_order_id, machine_id, product_code, target_quantity, status, payload, last_sync_at)
VALUES
    ('MOCK-OP-2026-0001', NULL, 'PVC-100ML-BR', 5000, 'OPEN',
     '{"cliente":"Acme Embalagens","molde":"MLD-12-A","prazo":"2026-06-15","cavidades":4}'::jsonb,
     now()),
    ('MOCK-OP-2026-0002', NULL, 'PET-500ML-AZ', 8000, 'OPEN',
     '{"cliente":"Bravo Bebidas","molde":"MLD-07-C","prazo":"2026-06-10","cavidades":8}'::jsonb,
     now()),
    ('MOCK-OP-2026-0003', NULL, 'PP-TAMPA-AM',  12000, 'OPEN',
     '{"cliente":"Cinco Cosmeticos","molde":"MLD-22-B","prazo":"2026-06-20","cavidades":16}'::jsonb,
     now()),
    ('MOCK-OP-2026-0004', NULL, 'PEAD-GALAO-VD', 1500, 'OPEN',
     '{"cliente":"Delta Quimica","molde":"MLD-03-A","prazo":"2026-06-08","cavidades":2}'::jsonb,
     now()),
    ('MOCK-OP-2026-0005', NULL, 'ABS-CAIXA-PR',  700, 'OPEN',
     '{"cliente":"Echo Industria","molde":"MLD-19-D","prazo":"2026-06-25","cavidades":1}'::jsonb,
     now()),
    ('MOCK-OP-2026-0006', NULL, 'PS-COPO-TR',   9500, 'OPEN',
     '{"cliente":"Foxtrot Food","molde":"MLD-14-A","prazo":"2026-06-12","cavidades":12}'::jsonb,
     now());
