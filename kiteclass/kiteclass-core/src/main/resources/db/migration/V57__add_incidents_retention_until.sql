-- GAP-359 Phase 1C v1.5 (Wave 24 Bucket A): 7-year retention column on incidents.
--
-- Per BR-CHILD-PROTECT-008 (Phase 1C remainder — added Wave 24): once an Incident
-- transitions to CLOSED, the row enters a 7-year mandatory retention window during
-- which soft-delete is BLOCKED at the service layer. After the window expires the
-- daily RetentionLifecycleService secure-deletes the row + appends an audit-log
-- entry recording the lifecycle action.
--
-- Compliance:
--   * PDPL Decree 13/2023/NĐ-CP Art 16 — children's PII special protection;
--     minimum retention overlaps with statute-of-limitations on child-abuse offences.
--   * Luật Trẻ em 2016 Đ.51 — mandatory-reporting follow-through; the audit log
--     for these incidents must outlive the operational lifecycle of the case.
--   * Bộ luật Hình sự Đ.147 — CSAM criminal liability (multi-year statute).
--
-- Design:
--   * Column is NULL for existing OPEN / non-CLOSED incidents and gets set when
--     IncidentService.updateStatus(..., CLOSED) runs (closed_at + 7 years).
--   * Backfill: existing rows (Wave 18b1+) seeded with COALESCE(updated_at,
--     created_at) + 7 years so the safest assumption (block delete) applies if
--     anyone migrated CLOSED rows pre-V57. updated_at acts as a stand-in for
--     closed_at since the schema does not yet track a dedicated closed_at column.
--   * Partial index on retention_until WHERE NOT NULL AND deleted = false keeps
--     the daily lifecycle scan cheap (only future-active rows hit the index).

-- -------------------------------------------------------------------------
-- 1. Add retention_until column
-- -------------------------------------------------------------------------

ALTER TABLE incidents
    ADD COLUMN retention_until TIMESTAMPTZ NULL;

COMMENT ON COLUMN incidents.retention_until IS
    'BR-CHILD-PROTECT-008 (Phase 1C v1.5): mandatory 7-year retention deadline. Set on CLOSED transition; soft-delete blocked while in window. NULL for active (non-CLOSED) incidents.';

-- -------------------------------------------------------------------------
-- 2. Backfill existing rows for safety
-- -------------------------------------------------------------------------

UPDATE incidents
SET retention_until = COALESCE(updated_at, created_at) + INTERVAL '7 years'
WHERE retention_until IS NULL;

-- -------------------------------------------------------------------------
-- 3. Partial index for cron scan + delete-block lookup
-- -------------------------------------------------------------------------

CREATE INDEX idx_incidents_retention_until
    ON incidents (retention_until)
    WHERE retention_until IS NOT NULL AND deleted = false;
