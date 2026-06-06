-- =========================================================================
-- V90: tenant_settings — per-tenant (trường học) settings, 1:1 with instance (GAP-947)
-- =========================================================================
-- Context: Wave provisioning-1 Bucket F (GAP-947). Settings were previously
-- scattered (Instance.organizationName/contactEmail) or hard-coded global
-- (system_config.locale). This table holds per-tenant timezone, locale, Năm học
-- (academic year), fiscal year, school type, address, phone, logo URL, theme.
--
-- 1:1 enforcement: UNIQUE on instance_id (one settings row per tenant).
-- academic_year is auto-filled at provision by the app
-- (AcademicYearCalculator, VN K-12 Sep→May convention).
--
-- Breaking change: NO. New table, no data migration. Idempotent
-- (IF NOT EXISTS on table/index; DROP POLICY IF EXISTS before CREATE).
-- RLS: ENABLE + FORCE + tenant_isolation policy (V59/V81 hardened pattern:
-- admin-bypass via app.is_platform_admin + NULL force-fail). Testcontainers
-- runs migrations as superuser → RLS bypassed for test fixtures.
-- =========================================================================

CREATE TABLE IF NOT EXISTS tenant_settings (
    id            BIGSERIAL    PRIMARY KEY,
    instance_id   UUID         NOT NULL,                              -- tenant scope + 1:1 key
    timezone      VARCHAR(50)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    locale        VARCHAR(10)  NOT NULL DEFAULT 'vi',
    academic_year VARCHAR(20)  NOT NULL,                             -- Năm học, e.g. '2026-2027'
    fiscal_year   VARCHAR(20),
    school_type   VARCHAR(20)  NOT NULL DEFAULT 'CENTER',
    address       VARCHAR(500),
    phone         VARCHAR(30),
    logo_url      VARCHAR(1000),
    theme_config  JSONB,
    -- BaseEntity audit columns
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,
    created_by    UUID,
    updated_by    UUID,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    version       BIGINT,
    CONSTRAINT ck_tenant_settings_school_type
        CHECK (school_type IN ('CENTER', 'K12', 'UNIVERSITY', 'OTHER'))
);

-- 1:1 per tenant — one settings row per instance
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_settings_instance_id
    ON tenant_settings (instance_id);

CREATE INDEX IF NOT EXISTS idx_tenant_settings_deleted
    ON tenant_settings (deleted);

COMMENT ON TABLE tenant_settings IS
    'Per-tenant settings (timezone/locale/Năm học/...), 1:1 with instance (GAP-947). '
    'Tenant isolation enforced by RLS tenant_isolation policy — defense beyond Hibernate @Filter.';
COMMENT ON COLUMN tenant_settings.academic_year IS
    'Năm học (VN K-12 Sep→May), auto-filled at provision via AcademicYearCalculator, e.g. ''2026-2027''.';

-- RLS: enable + force + hardened tenant_isolation policy (mirrors V59/V81 admin-bypass + NULL force-fail)
ALTER TABLE tenant_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_settings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON tenant_settings;
CREATE POLICY tenant_isolation ON tenant_settings
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );
