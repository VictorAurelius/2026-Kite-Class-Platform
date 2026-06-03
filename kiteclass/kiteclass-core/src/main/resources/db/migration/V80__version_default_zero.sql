-- =========================================================================
-- V80: version DEFAULT 0 backfill — 4 tables missed by V62/V63 (GAP-884)
-- =========================================================================
-- Context: GAP-884 (Wave 13 cluster docs writing — KC finance + KC branding).
-- V62/V63 set `version BIGINT DEFAULT 0` on 19 tables but missed 4:
--   - invoices.version        (V26 added column, no default)
--   - payments.version        (V26 added column, no default)
--   - payment_records.version (V69 created column, no default)
--   - landing_pages.version   (V75 created column, no default)
--
-- Raw INSERT (seed / test fixture / migration script) that doesn't bind
-- `version` → NULL → JPA @Version NPE at flush. Service path via JPA binds the
-- entity initializer default (OK); risk is for non-JPA write paths. This
-- migration brings the 4 stragglers in line with the 19 already-normalized
-- tables for consistency + defense-in-depth.
--
-- State-check (2026-06-03): all 4 tables confirmed to have a `version` column:
--   invoices (V26:50), payments (V26:58), payment_records (V69:33),
--   landing_pages (V75:40). No table skipped.
--
-- audit_log.version already has DEFAULT 0 (V35:31) — NOT in scope.
-- invoice_items already fixed by V62:43 — NOT in scope.
--
-- Breaking change: NO. Same pattern as V62/V63: SET DEFAULT 0 + UPDATE NULL→0.
-- Idempotent on re-run.
-- =========================================================================

ALTER TABLE invoices        ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE payments        ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE payment_records ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE landing_pages   ALTER COLUMN version SET DEFAULT 0;

UPDATE invoices        SET version = 0 WHERE version IS NULL;
UPDATE payments        SET version = 0 WHERE version IS NULL;
UPDATE payment_records SET version = 0 WHERE version IS NULL;
UPDATE landing_pages   SET version = 0 WHERE version IS NULL;
