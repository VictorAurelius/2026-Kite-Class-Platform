-- =========================================================================
-- V82: frontend_instances rename tenant_id → tenant_slug (GAP-891)
-- =========================================================================
-- Context: GAP-891 (Wave 13 cluster docs writing — KC branding/marketing).
-- V31 created frontend_instances with TWO tenant-prefixed identifiers of
-- different types/purposes:
--   - instance_id UUID NOT NULL — primary tenant identifier (RLS filter column)
--   - tenant_id  VARCHAR(100) NOT NULL — string slug / human-readable ID used for
--     FE deploy lookup + cross-service ref to KiteHub instances.slug
-- The two "tenant"-prefixed columns are confusing. Rename the VARCHAR one to
-- tenant_slug to make purpose explicit + add a clarifying COMMENT.
--
-- State-check (2026-06-03):
--   - V31:12 `tenant_id VARCHAR(100) NOT NULL` — the column to rename.
--   - V31:45 `idx_frontend_instance_tenant ON frontend_instances(tenant_id)` —
--     index column reference auto-follows RENAME COLUMN in Postgres (index def
--     updates automatically); no separate index rename needed.
--   - RLS (V58/V59) filters on instance_id (UUID), NOT tenant_id (VARCHAR) →
--     rename does NOT affect RLS policy. check-rls-coverage only inspects uuid
--     instance_id/tenant_id columns; this VARCHAR column is out of its scope.
--
-- Entity lockstep (same commit): FrontendInstance.java field tenantId →
-- tenantSlug + @Column(name="tenant_slug"); read call-sites updated.
--
-- Breaking change: column rename. Idempotent guard via DO block (RENAME COLUMN
-- has no IF EXISTS in older PG) — skip if already renamed.
-- =========================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'frontend_instances'
          AND column_name = 'tenant_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'frontend_instances'
          AND column_name = 'tenant_slug'
    ) THEN
        ALTER TABLE frontend_instances RENAME COLUMN tenant_id TO tenant_slug;
    END IF;
END $$;

COMMENT ON COLUMN frontend_instances.tenant_slug IS
    'GAP-891: human-readable tenant slug (VARCHAR) for FE deploy lookup + '
    'cross-service ref to KiteHub instances.slug. Distinct from instance_id '
    '(UUID, the RLS tenant-isolation filter column). Renamed from tenant_id '
    'to remove naming confusion between the two tenant-prefixed identifiers.';
