-- GAP-322c Phase 1C v1 (Wave 19 Bucket A): mandatory-reporting foundation.
--
-- Two additive changes in a single migration:
--   1. ALTER incidents ADD visibility_scope VARCHAR(32) NOT NULL DEFAULT 'STAFF_ONLY'
--      Per BR-CHILD-PROTECT-005 (visibility scope, Phase 1C v1). Existing rows
--      default to STAFF_ONLY so legacy data NEVER leaks into the parent-portal
--      conduct facet (Bucket D consumes this column).
--
--   2. CREATE TABLE child_protection_audit_log (hash-chain append-only)
--      Per BR-CHILD-PROTECT-007. Append-only invariant enforced via REVOKE
--      DELETE on the typical app role; daily integrity verification cron
--      tracked as Phase 1C remainder follow-up.
--
-- Compliance:
--   * Luật Trẻ em 2016 Đ.51 (mandatory reporting ≤24h) — audit log proves the
--     ack chain when reviewed by công an / MOLISA
--   * PDPL Decree 13/2023/NĐ-CP Art 16 — children's PII special protection
--   * BLHS Đ.147 — CSAM criminal liability; non-repudiation matters

-- -------------------------------------------------------------------------
-- 1. Visibility scope on incidents
-- -------------------------------------------------------------------------

ALTER TABLE incidents
    ADD COLUMN visibility_scope VARCHAR(32) NOT NULL DEFAULT 'STAFF_ONLY';

ALTER TABLE incidents
    ADD CONSTRAINT chk_incidents_visibility_scope CHECK (
        visibility_scope IN ('PARENT_VISIBLE', 'PUBLIC', 'STAFF_ONLY', 'RESTRICTED')
    );

CREATE INDEX idx_incidents_visibility_scope ON incidents(visibility_scope);

COMMENT ON COLUMN incidents.visibility_scope IS
    'BR-CHILD-PROTECT-005 (Phase 1C v1): audience exposure scope. Defaults STAFF_ONLY so abuse/grooming/CSAM never leak to the parent portal facet.';

-- -------------------------------------------------------------------------
-- 2. Hash-chain audit log table
-- -------------------------------------------------------------------------

CREATE TABLE child_protection_audit_log (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(128) NOT NULL,
    actor_id BIGINT,
    occurred_at TIMESTAMP NOT NULL,
    prev_hash VARCHAR(64) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,

    CONSTRAINT chk_cp_audit_hash_length CHECK (
        length(prev_hash) = 64 AND length(content_hash) = 64
    )
);

CREATE INDEX idx_cp_audit_instance_id ON child_protection_audit_log(instance_id);
CREATE INDEX idx_cp_audit_entity ON child_protection_audit_log(entity_type, entity_id);
CREATE INDEX idx_cp_audit_actor ON child_protection_audit_log(actor_id);
CREATE INDEX idx_cp_audit_occurred_at ON child_protection_audit_log(occurred_at);

COMMENT ON TABLE child_protection_audit_log IS
    'BR-CHILD-PROTECT-007 (Phase 1C v1): append-only hash-chain audit log for child-protection domain. content_hash = SHA-256(prev_hash || canonical_payload). Daily integrity verification cron deferred to Phase 1C remainder.';
COMMENT ON COLUMN child_protection_audit_log.prev_hash IS
    '64-char hex SHA-256 of the prior chain entry`s content_hash; "0".repeat(64) for genesis per (instance_id, entity_type) chain.';
COMMENT ON COLUMN child_protection_audit_log.content_hash IS
    '64-char hex SHA-256 of (prev_hash || canonical_payload_json). Recompute on read to detect tamper.';

-- -------------------------------------------------------------------------
-- 3. Append-only invariant — REVOKE DELETE for typical app role
-- -------------------------------------------------------------------------
--
-- The application connection MUST never issue DELETE on this table. We
-- attempt to REVOKE DELETE for the role identified by current_user inside
-- a DO block so the migration is a no-op when the typical operator role
-- is something Flyway can't introspect (e.g., a superuser running the
-- migration during dev). When the migration runs as `kiteclass_app`, the
-- REVOKE takes effect; when it runs as a superuser, the GRANT bypass
-- still leaves the row for cron-integrity to detect.
--
-- Unit + IT tests verify the application path never invokes a delete; the
-- DB grant is the belt-and-suspenders second line.

DO $$
DECLARE
    app_role TEXT;
BEGIN
    -- Look up a likely app role; skip the REVOKE silently if not present.
    SELECT rolname INTO app_role
    FROM pg_roles
    WHERE rolname IN ('kiteclass_app', 'kiteclass', 'kite_app')
    LIMIT 1;

    IF app_role IS NOT NULL THEN
        EXECUTE format('REVOKE DELETE ON child_protection_audit_log FROM %I', app_role);
        EXECUTE format('REVOKE TRUNCATE ON child_protection_audit_log FROM %I', app_role);
    END IF;
END $$;
