-- V17__create_settings_tables.sql
-- Settings & Preferences Module (PR 2.9)
-- Creates tables: branding, user_preferences

-- ========================================
-- Table: branding (1 row per tenant)
-- ========================================
CREATE TABLE IF NOT EXISTS branding (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- Visual branding
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    display_name VARCHAR(200) NOT NULL,
    tagline VARCHAR(500),

    -- Colors (hex format: #RRGGBB)
    primary_color VARCHAR(7) NOT NULL DEFAULT '#3B82F6',
    secondary_color VARCHAR(7) NOT NULL DEFAULT '#8B5CF6',
    accent_color VARCHAR(7) NOT NULL DEFAULT '#10B981',

    -- Contact information
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    address TEXT,

    -- Social media links
    facebook_url VARCHAR(500),
    zalo_url VARCHAR(500),
    website_url VARCHAR(500),

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT branding_instance_id_unique UNIQUE (instance_id, deleted)
);

-- Index for multi-tenant queries
CREATE INDEX IF NOT EXISTS idx_branding_instance_id ON branding(instance_id) WHERE deleted = FALSE;

-- ========================================
-- Table: user_preferences
-- ========================================
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- User reference (links to Gateway User via reference_id)
    user_id BIGINT NOT NULL,

    -- Locale settings
    language VARCHAR(5) NOT NULL DEFAULT 'vi',  -- 'en', 'vi'
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',

    -- UI preferences
    theme VARCHAR(10) NOT NULL DEFAULT 'light',  -- 'light', 'dark', 'auto'

    -- Notification preferences (JSON)
    notification_preferences JSONB NOT NULL DEFAULT '{"email": true, "push": true, "sms": false}'::jsonb,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT user_preferences_instance_user_unique UNIQUE (instance_id, user_id, deleted)
);

-- Index for multi-tenant queries
CREATE INDEX IF NOT EXISTS idx_user_preferences_instance_id ON user_preferences(instance_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON user_preferences(instance_id, user_id) WHERE deleted = FALSE;

-- ========================================
-- Comments
-- ========================================
COMMENT ON TABLE branding IS 'Tenant branding settings (logo, colors, contact info)';
COMMENT ON TABLE user_preferences IS 'User-specific preferences (language, theme, notifications)';

COMMENT ON COLUMN branding.instance_id IS 'Tenant identifier (multi-tenant isolation)';
COMMENT ON COLUMN branding.display_name IS 'Tenant display name (e.g., "ABC English Center")';
COMMENT ON COLUMN branding.primary_color IS 'Primary brand color (hex format)';
COMMENT ON COLUMN branding.notification_preferences IS 'JSON object with notification channel preferences';

COMMENT ON COLUMN user_preferences.user_id IS 'Reference to Gateway User.id (cross-service link)';
COMMENT ON COLUMN user_preferences.language IS 'UI language code (ISO 639-1)';
COMMENT ON COLUMN user_preferences.timezone IS 'IANA timezone identifier';
COMMENT ON COLUMN user_preferences.theme IS 'UI theme preference';
