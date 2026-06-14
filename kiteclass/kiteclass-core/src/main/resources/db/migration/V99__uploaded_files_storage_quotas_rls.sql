-- ============================================================================
-- V99: RLS backstop for storage tables (uploaded_files + storage_quotas)
-- ============================================================================
-- GAP-1311 (security full audit 2026-06-14, F-004 / EVIDENCE-2026-06-14-INFRA-006).
--
-- Both tables are tenant-scoped (instance_id UUID NOT NULL, created in V79) and protected
-- by the Hibernate @Filter("tenantFilter") at the ORM layer. Unlike every other tenant-scoped
-- table they were NEVER added to an enable-RLS sweep (absent from V58/V59/V78/V81/V83/V84),
-- leaving them single-layer: if any code path skips the Hibernate filter (native @Query, a
-- filter-exempt path, or a forgotten enableFilter) there is NO DB backstop and a cross-tenant
-- read/write could occur.
--
-- This migration adds the missing DB-level RLS layer using the V59-hardened tenant_isolation
-- policy shape:
--   * admin-bypass  — COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
--   * NULL force-fail — instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
--     (unset/empty GUC → predicate NULL → row invisible → default-deny preserved)
--
-- Forward-only, idempotent (DROP POLICY IF EXISTS + CREATE POLICY in a DO block; defensive
-- table/column existence guard mirrors V58/V84). Precedent: V58 (enable+force), V59 (hardened
-- policy), V79 (per-table RLS for leads/contact_messages), V84 (single-table denormalize+RLS).
-- ============================================================================

DO $$
DECLARE
    t text;
    storage_tables text[] := ARRAY[
        'uploaded_files',
        'storage_quotas'
    ];
BEGIN
    FOREACH t IN ARRAY storage_tables
    LOOP
        -- Defensive: skip if the table does not exist (same posture as V58/V84).
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = t
        ) THEN
            RAISE NOTICE 'Skipping table % (does not exist)', t;
            CONTINUE;
        END IF;

        -- Sanity check: table must carry instance_id for the policy predicate.
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = t AND column_name = 'instance_id'
        ) THEN
            RAISE NOTICE 'Skipping table % (no instance_id column)', t;
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);

        -- Idempotent re-apply.
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

        RAISE NOTICE 'RLS enabled on storage table % (admin-bypass + NULL force-fail)', t;
    END LOOP;
END $$;
