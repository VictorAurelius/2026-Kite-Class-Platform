-- GAP-385 (Wave 35 Bucket B): PDPL 2023 explicit-consent columns on beta_access_request.
--
-- PDPL 2023 Art 11 mandates explicit, informed, freely-given consent prior to
-- personal-data collection. The beta-signup form collects email + name +
-- orgName (all PII per Art 2(3)), so the submit endpoint MUST capture and
-- persist a consent token + timestamp as the durable evidence trail required
-- by Art 16 (data-subject rights enforcement).
--
-- Note V31 is reserved for Wave 35 Bucket E indexes per
-- documents/03-planning/waves/wave-2026-05-08-35-audit-p0-blockers-sprint.md
-- §3 footer; this migration uses V32.
--
-- Schema change:
--   1. consent_given BOOLEAN NOT NULL DEFAULT FALSE — backfill default for
--      existing PENDING rows; new inserts post-Wave 35 carry TRUE.
--   2. consent_at TIMESTAMP WITH TIME ZONE — backfilled to created_at for
--      pre-existing rows so the NOT NULL constraint can hold; new inserts
--      receive now() at submit time.
--
-- The DEFAULT FALSE on consent_given is intentional for backfill safety only
-- — application-level @AssertTrue + service insert paths require TRUE for new
-- submissions. Pre-existing PENDING rows MAY be re-validated by coordinator
-- review before approval (out of scope for this migration; coordinator policy).

ALTER TABLE beta_access_request
    ADD COLUMN consent_given BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE beta_access_request
    ADD COLUMN consent_at TIMESTAMP WITH TIME ZONE NULL;

-- Backfill consent_at = created_at for existing rows so NOT NULL holds.
UPDATE beta_access_request
   SET consent_at = created_at
 WHERE consent_at IS NULL;

ALTER TABLE beta_access_request
    ALTER COLUMN consent_at SET NOT NULL;

COMMENT ON COLUMN beta_access_request.consent_given IS
    'PDPL 2023 Art 11 explicit-consent indicator (GAP-385 Wave 35). New submissions enforce TRUE; pre-existing rows backfilled FALSE — coordinator policy gates approval.';

COMMENT ON COLUMN beta_access_request.consent_at IS
    'PDPL 2023 Art 16 consent-evidence timestamp (GAP-385 Wave 35). New inserts: now() at submit; pre-existing rows: backfilled to created_at.';
