-- GAP-1020 (Part 1) — add platform-admin bypass to the branding tenant-scoped RLS policies.
--
-- V34 enabled RLS on the branding tables with a tenant-only policy:
--     instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
-- V58 later swept payments / branding_outbox / subscription_outbox + the tenant_id tables to ALSO
-- honour an `app.is_platform_admin` bypass, but the original V34 branding tables were never
-- updated. Now that kitehub-branding actually SETS the GUCs (TenantContextFilter +
-- TenantAwareDataSourceInterceptor), a platform-admin operating cross-instance under a future
-- non-superuser DB role would be filtered to their OWN tenant on these tables. This migration
-- re-creates the tenant_isolation policy WITH the same admin-bypass clause V58 uses, so admin
-- cross-instance branding operations work and non-admin tenant isolation is unchanged.
--
-- Posture preserved (per V34/V58 boundary calls): RLS stays ENABLED but NOT FORCED — the table
-- owner role (Flyway + Spring HikariCP `kitehub`) continues to bypass; only a non-owner /
-- non-superuser role is filtered. No data change; policy semantics only.

DO $$
DECLARE
    t text;
    branding_instance_id_tables text[] := ARRAY[
        'ai_usage_log',
        'branding_instance_state',
        'branding_jobs',
        'branding_lifecycle_events',
        'branding_regenerate_usage'
    ];
BEGIN
    FOREACH t IN ARRAY branding_instance_id_tables
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = t
        ) THEN
            RAISE NOTICE 'Skipping table % (does not exist)', t;
            CONTINUE;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = t AND column_name = 'instance_id'
        ) THEN
            RAISE NOTICE 'Skipping table % (no instance_id column)', t;
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        -- NB: NO `FORCE ROW LEVEL SECURITY` — owner role keeps bypassing (V34/V58 posture).
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', t);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            'USING ('
            '    COALESCE(current_setting(''app.is_platform_admin'', true)::boolean, false) '
            '    OR instance_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid'
            ') '
            'WITH CHECK ('
            '    COALESCE(current_setting(''app.is_platform_admin'', true)::boolean, false) '
            '    OR instance_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid'
            ')',
            t
        );
        RAISE NOTICE 'RLS admin-bypass policy applied on table % (instance_id, non-forced)', t;
    END LOOP;
END $$;
