-- V70 (GAP-1115): add org_type column to branding_jobs
--
-- Carries the wizard "user-type" axis (SOLO_TEACHER / SMALL_CENTER / LARGE_CENTER)
-- orthogonal to the audience/theme axis. Drives portrait-count strategy (GAP-1116)
-- + tier hints. Nullable for backward-compat: pre-GAP-1115 jobs carry NULL.
--
-- Schema owner note: the `branding_jobs` table is created + migrated in
-- kitehub-subscription (V4/V8/V31/V60) but the entity lives in kitehub-branding
-- (ddl-auto=validate in prod). This column add keeps the entity ↔ schema in sync
-- per design-patterns.md §3.12 entity-migration triad.

ALTER TABLE branding_jobs ADD COLUMN IF NOT EXISTS org_type VARCHAR(20);

COMMENT ON COLUMN branding_jobs.org_type IS
    'Wizard user-type axis (GAP-1115): SOLO_TEACHER | SMALL_CENTER | LARGE_CENTER. Nullable.';
