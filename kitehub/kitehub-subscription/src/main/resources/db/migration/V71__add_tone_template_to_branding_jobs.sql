-- V71 (GAP-1146): add tone + template_id columns to branding_jobs
--
-- Carries the wizard "tone" axis (professional / friendly / energetic / luxury)
-- and the chosen template id onto the job so the deterministic preview palette
-- in BrandColoursDeriver reflects the style the owner selected — not just a hash
-- of the organisation name. Nullable for backward-compat: pre-GAP-1146 jobs carry
-- NULL and fall back to the org-name hash.
--
-- Schema owner note: the `branding_jobs` table is created + migrated in
-- kitehub-subscription (V4/V8/V31/V60/V70) but the entity lives in kitehub-branding
-- (ddl-auto=validate in prod). This column add keeps the entity ↔ schema in sync
-- per design-patterns.md §3.12 entity-migration triad.

ALTER TABLE branding_jobs ADD COLUMN IF NOT EXISTS tone VARCHAR(20);
ALTER TABLE branding_jobs ADD COLUMN IF NOT EXISTS template_id VARCHAR(50);

COMMENT ON COLUMN branding_jobs.tone IS
    'Wizard tone axis (GAP-1146): professional | friendly | energetic | luxury. Drives preview palette. Nullable.';
COMMENT ON COLUMN branding_jobs.template_id IS
    'Wizard template selection (GAP-1146): chosen templateId; palette variant seed. Nullable.';
