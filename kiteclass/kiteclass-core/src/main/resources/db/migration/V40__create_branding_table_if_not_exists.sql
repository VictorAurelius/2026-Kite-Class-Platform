-- =========================================================================
-- V40: Create branding table IF NOT EXISTS (GAP-065)
-- =========================================================================
-- Context: GAP-065 — V25 assumes branding table exists (from kitehub-branding),
--          but fresh-deploy from empty DB has no branding table yet.
-- Purpose: Ensure branding table exists in kiteclass-core so the full migration
--          chain V1..V39 succeeds on an empty Postgres without baseline hacks.
-- Breaking change: NO (IF NOT EXISTS — safe for existing environments)
-- =========================================================================

-- Create the branding table matching kiteclass-core Branding entity.
-- Uses IF NOT EXISTS so existing environments (where kitehub-branding already
-- created this table) are unaffected.
CREATE TABLE IF NOT EXISTS branding (
    id                  BIGSERIAL PRIMARY KEY,
    instance_id         UUID         NOT NULL,

    -- Visual branding
    logo_url            VARCHAR(500),
    favicon_url         VARCHAR(500),
    display_name        VARCHAR(200) NOT NULL,
    tagline             VARCHAR(500),

    -- Colors (hex format: #RRGGBB)
    primary_color       VARCHAR(7)   NOT NULL DEFAULT '#3B82F6',
    secondary_color     VARCHAR(7)   NOT NULL DEFAULT '#8B5CF6',
    accent_color        VARCHAR(7)   NOT NULL DEFAULT '#10B981',

    -- Theme configuration (AI-generated complete theme JSON)
    theme_config_json   TEXT,

    -- Contact information
    contact_email       VARCHAR(255),
    contact_phone       VARCHAR(20),
    address             TEXT,

    -- Social media links
    facebook_url        VARCHAR(500),
    zalo_url            VARCHAR(500),
    website_url         VARCHAR(500),

    -- Audit fields (BaseEntity)
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    version             BIGINT       NOT NULL DEFAULT 0
);

-- Indexes (also IF NOT EXISTS to stay idempotent)
CREATE INDEX IF NOT EXISTS idx_branding_instance_id ON branding(instance_id);
CREATE INDEX IF NOT EXISTS idx_branding_deleted ON branding(deleted);
