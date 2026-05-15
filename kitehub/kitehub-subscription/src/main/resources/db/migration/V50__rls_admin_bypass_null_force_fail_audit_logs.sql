-- Wave 85 Bucket B — Sister migration cho kh-subscription
--   * Strengthen V34 RLS policies với admin-bypass clause (B-AC7) + NULL force-fail (B-AC8)
--   * Create immutable admin_audit_logs table (B-AC2 + B-AC7)
--
-- See kc-core V59__rls_admin_bypass_and_null_force_fail.sql + V60__create_admin_audit_logs.sql
-- for full rationale.
--
-- IMPORTANT difference vs kc-core: kh-subscription V34 enabled RLS WITHOUT `FORCE ROW LEVEL
-- SECURITY` (Spring Boot HikariCP user = table owner → bypasses policy for app workload).
-- This Wave 85 migration KHÔNG flip FORCE — that's a separate hardening task tracked
-- post-Wave 85 (requires per-request TenantContext propagation through kh-subscription's
-- control-plane endpoints, which currently are admin-scoped).
--
-- We DO strengthen the policy predicate so when RLS IS active (future tenants of the policy
-- e.g., per-tenant analytical roles, cross-service connections) the admin-bypass + NULL
-- force-fail apply uniformly across kc-core + kh-subscription.

DO $$
DECLARE
    t text;
    instance_id_tables text[] := ARRAY[
        'ai_usage_log',
        'backup_records',
        'branding_instance_state',
        'branding_jobs',
        'branding_lifecycle_events',
        'branding_regenerate_usage',
        'email_logs',
        'email_sent_log',
        'migration_idempotency_key',
        'migration_outbox',
        'subscriptions'
    ];
    tenant_id_tables text[] := ARRAY[
        'consent_record'
    ];
BEGIN
    -- instance_id-keyed tables: re-create policy with admin-bypass + NULL force-fail
    FOREACH t IN ARRAY instance_id_tables
    LOOP
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = t) THEN
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
        RAISE NOTICE 'RLS policy hardened on table % (instance_id, admin-bypass + NULL force-fail, non-forced)', t;
    END LOOP;

    -- tenant_id-keyed tables: same strengthening
    FOREACH t IN ARRAY tenant_id_tables
    LOOP
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = t) THEN
            RAISE NOTICE 'Skipping table % (does not exist)', t;
            CONTINUE;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = t AND column_name = 'tenant_id'
        ) THEN
            RAISE NOTICE 'Skipping table % (no tenant_id column)', t;
            CONTINUE;
        END IF;

        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', t);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            'USING ('
            '    COALESCE(current_setting(''app.is_platform_admin'', true)::boolean, false) '
            '    OR tenant_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid'
            ') '
            'WITH CHECK ('
            '    COALESCE(current_setting(''app.is_platform_admin'', true)::boolean, false) '
            '    OR tenant_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid'
            ')',
            t
        );
        RAISE NOTICE 'RLS policy hardened on table % (tenant_id, admin-bypass + NULL force-fail, non-forced)', t;
    END LOOP;
END $$;

-- ===== Immutable admin_audit_logs table (B-AC2 + B-AC7) =====
-- PDPL Art 11 compliance. Sister to kc-core V60. Same schema, same RLS immutability policies.
-- Both services emit to their respective DB instance's admin_audit_logs — federated read at
-- app layer via AdminAuditLogService.findAll() merges from both.

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_tenant_id UUID,
    target_resource VARCHAR(512),
    payload_jsonb JSONB,
    client_ip VARCHAR(64),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_admin ON admin_audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_target_tenant ON admin_audit_logs(target_tenant_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action ON admin_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at ON admin_audit_logs(created_at DESC);

COMMENT ON TABLE admin_audit_logs IS
    'PDPL Art 11 — immutable platform admin action audit log (Wave 85 Bucket B). Append-only.';

ALTER TABLE admin_audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_audit_logs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS admin_audit_select ON admin_audit_logs;
CREATE POLICY admin_audit_select ON admin_audit_logs FOR SELECT USING (true);

DROP POLICY IF EXISTS admin_audit_insert ON admin_audit_logs;
CREATE POLICY admin_audit_insert ON admin_audit_logs FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS admin_audit_no_update ON admin_audit_logs;
CREATE POLICY admin_audit_no_update ON admin_audit_logs FOR UPDATE USING (false) WITH CHECK (false);

DROP POLICY IF EXISTS admin_audit_no_delete ON admin_audit_logs;
CREATE POLICY admin_audit_no_delete ON admin_audit_logs FOR DELETE USING (false);
