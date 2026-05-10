-- Phase 1 of GAP-466 (kh-subscription slice): Enable Postgres RLS on tenant-scoped tables
--
-- See kiteclass-core V58__enable_rls_tenant_scoped_tables.sql for full rationale.
--
-- kh-subscription has 12 tenant-scoped tables (11 using `instance_id`, 1 using `tenant_id`).
-- Tables `subscriptions`, `migration_outbox`, `email_logs` etc. — see Wave 56 plan §4 State-Check Evidence.
--
-- IMPORTANT difference vs kc-core (V58):
--   kh-subscription is a control-plane service that does not currently propagate a per-request
--   `TenantContext`. Forcing RLS on its tables would default-deny every query and break ALL
--   existing repository methods + tests.
--
--   This migration therefore ENABLES RLS but does NOT issue `FORCE ROW LEVEL SECURITY`. As a
--   result the table-owner role used by Flyway + Spring's HikariCP user bypasses the policy,
--   while any OTHER role (e.g. a future per-tenant analytical role or a cross-service connection
--   that does not own the table) is filtered. This is the recommended posture per
--   AWS Well-Architected SaaS Lens — "policies present and reviewed; force tightened in a
--   follow-up wave once the service gains tenant-aware request context".
--
--   Follow-up: GAP filed by Wave 56 closure PR if/when kh-subscription gains a per-request
--   `TenantContext` (e.g. when KiteHub admin UI gates per-instance read flows).

DO $$
DECLARE
    t text;
    tenant_col text;
    -- Tables keyed by their tenant column (instance_id vs tenant_id) so policy targets correct column
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
    -- instance_id-keyed tables
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

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        -- NB: NO `FORCE ROW LEVEL SECURITY` — see header comment.
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', t);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            'USING (instance_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid) '
            'WITH CHECK (instance_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid)',
            t
        );
        RAISE NOTICE 'RLS enabled on table % (instance_id, non-forced)', t;
    END LOOP;

    -- tenant_id-keyed tables (consent_record uses tenant_id per Wave 56 plan state-check)
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

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        -- NB: NO `FORCE ROW LEVEL SECURITY` — see header comment.
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', t);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            'USING (tenant_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid) '
            'WITH CHECK (tenant_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid)',
            t
        );
        RAISE NOTICE 'RLS enabled on table % (tenant_id, non-forced)', t;
    END LOOP;
END $$;
