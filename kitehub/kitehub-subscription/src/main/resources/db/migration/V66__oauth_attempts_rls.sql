-- Wave p0-local-1 Bucket B — GAP-885 oauth_attempts RLS residual.
--
-- V34/V50 enabled RLS with a static table list; V58 (V58__rls_sweep_kh.sql) swept
-- the remaining KH tenant-scoped tables. `oauth_attempts` (created V51, AFTER V34/V50)
-- was the ONE table still missing an RLS policy:
--   * V58 tenant_id_tables loop SKIPS oauth_attempts because its guard requires
--     `data_type = 'uuid'` on the tenant_id column. oauth_attempts.tenant_id is BIGINT.
--
-- This migration mirrors the V58/V50 KH posture (non-forced + admin-bypass +
-- NULL force-fail) for oauth_attempts, closing the GAP-885 residual.
--
-- ====================================================================================
-- KNOWN SCHEMA ANOMALY (documented, NOT fixed here — out of scope for an RLS residual):
--   oauth_attempts.tenant_id is BIGINT NULL, whereas every other KH tenant-scoped
--   table keys on instance_id UUID (or tenant_id UUID for consent_record). The RLS GUC
--   `app.current_tenant_id` carries a UUID string. A direct `::uuid` cast (V58 pattern)
--   would fail at policy-eval time against a BIGINT column, so this policy compares as
--   TEXT instead. Practical effect: non-admin connections without a matching tenant
--   context are denied (null force-fail); admins bypass. Since oauth_attempts has NO
--   application caller yet (defensive scaffolding for the P2 Owner OAuth signup flow per
--   GAP-582) AND the app workload runs as the table owner under non-forced RLS, this is
--   safe. The BIGINT-vs-UUID drift is tracked as a sibling discovery (see GAP-877 theme:
--   actor/tenant id BIGINT/UUID cross-cluster drift). Re-keying oauth_attempts to
--   instance_id UUID is a separate migration once the OAuth flow is implemented.
-- ====================================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'oauth_attempts'
    ) THEN
        RAISE NOTICE 'Skipping oauth_attempts RLS (table does not exist)';
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'oauth_attempts' AND column_name = 'tenant_id'
    ) THEN
        RAISE NOTICE 'Skipping oauth_attempts RLS (no tenant_id column)';
        RETURN;
    END IF;

    -- Enable RLS (non-forced — consistent with V34/V50/V58 KH posture: Spring Boot
    -- HikariCP user = table owner bypasses per-row policy for the app workload; the
    -- policy applies to FORCE-RLS contexts / non-owner roles / cross-service connections).
    EXECUTE 'ALTER TABLE oauth_attempts ENABLE ROW LEVEL SECURITY';

    EXECUTE 'DROP POLICY IF EXISTS tenant_isolation ON oauth_attempts';
    EXECUTE
        'CREATE POLICY tenant_isolation ON oauth_attempts '
        'USING ('
        '    COALESCE(current_setting(''app.is_platform_admin'', true)::boolean, false) '
        '    OR tenant_id::text = NULLIF(current_setting(''app.current_tenant_id'', true), '''')'
        ') '
        'WITH CHECK ('
        '    COALESCE(current_setting(''app.is_platform_admin'', true)::boolean, false) '
        '    OR tenant_id::text = NULLIF(current_setting(''app.current_tenant_id'', true), '''')'
        ')';

    RAISE NOTICE 'RLS policy tenant_isolation created on oauth_attempts (tenant_id BIGINT as text, admin-bypass + NULL force-fail, non-forced)';
END $$;

COMMENT ON COLUMN oauth_attempts.tenant_id IS
    'BIGINT tenant ref (legacy shape — KH standard is instance_id UUID; see V66 RLS anomaly note). Nullable: set only after instance creation in the OAuth signup flow.';
