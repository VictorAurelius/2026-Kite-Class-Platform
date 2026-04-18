-- =========================================================================
-- V43: Branding version history + manual rollback (GAP-033p, Wave 4)
-- =========================================================================
-- Context: Wave 4 introduces branding version snapshots so admins can roll
--          back to a known-good state. Auto-rollback + A/B testing arrive
--          in a later wave; this migration covers storage + manual rollback.
--
-- Breaking change: NO (additive — new table, no changes to branding itself).
-- =========================================================================

CREATE TABLE IF NOT EXISTS branding_versions (
    id                 BIGSERIAL   PRIMARY KEY,

    -- Tenant isolation — tenants are UUIDs everywhere else in kiteclass-core.
    instance_id        UUID        NOT NULL,

    -- Monotonic per-instance; enforced by uk_version_per_instance below.
    version_number     INT         NOT NULL,

    -- Complete Branding snapshot as JSON (logo, colors, contact, social, theme).
    snapshot_json      JSONB       NOT NULL,

    -- FK to the version this record rolled back to, or NULL for forward edits.
    rollback_of        BIGINT      REFERENCES branding_versions(id),

    -- Marks the version currently applied to the branding table. Exactly one
    -- per instance (enforced by partial unique index below).
    active             BOOLEAN     NOT NULL DEFAULT FALSE,

    -- BaseEntity audit columns.
    created_at         TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by         BIGINT,
    updated_by         BIGINT,
    deleted            BOOLEAN     NOT NULL DEFAULT FALSE,
    version            BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uk_version_per_instance UNIQUE (instance_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_branding_versions_instance
    ON branding_versions(instance_id);

-- Partial unique index: at most one active version per instance.
CREATE UNIQUE INDEX IF NOT EXISTS idx_branding_versions_active
    ON branding_versions(instance_id)
    WHERE active = TRUE;
