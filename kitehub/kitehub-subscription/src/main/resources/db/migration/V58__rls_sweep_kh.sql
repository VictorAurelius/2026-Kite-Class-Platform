-- Wave local-doable-14 Bucket A — KH RLS sweep and tenant-column backfill.
--
-- Boundary calls:
--   * KH subscription/control-plane migrations V34/V50 intentionally avoid
--     FORCE ROW LEVEL SECURITY because the service still does not propagate a
--     per-request TenantContext for all admin flows. This migration preserves
--     that non-forced posture for tenant-scoped KH tables.
--   * `payments` backfills `instance_id` from `subscriptions`.
--   * `branding_outbox` backfills `instance_id` from `branding_jobs`, with an
--     instance-id aggregate fallback for historical event rows.

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS instance_id UUID;

UPDATE payments p
SET instance_id = s.instance_id
FROM subscriptions s
WHERE p.subscription_id = s.id
  AND p.instance_id IS NULL;

ALTER TABLE payments
    ALTER COLUMN instance_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_instance_id ON payments(instance_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'payments'
          AND constraint_name = 'fk_payments_instance'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT fk_payments_instance
            FOREIGN KEY (instance_id) REFERENCES instances(id);
    END IF;
END $$;

ALTER TABLE branding_outbox
    ADD COLUMN IF NOT EXISTS instance_id UUID;

UPDATE branding_outbox bo
SET instance_id = bj.instance_id
FROM branding_jobs bj
WHERE bo.aggregate_id = bj.id
  AND bo.instance_id IS NULL;

UPDATE branding_outbox bo
SET instance_id = i.id
FROM instances i
WHERE bo.aggregate_id = i.id
  AND bo.instance_id IS NULL;

DO $$
DECLARE
    missing_count bigint;
BEGIN
    SELECT COUNT(*) INTO missing_count
    FROM branding_outbox
    WHERE instance_id IS NULL;

    IF missing_count > 0 THEN
        RAISE EXCEPTION 'branding_outbox has % rows that cannot be backfilled to instance_id', missing_count;
    END IF;
END $$;

ALTER TABLE branding_outbox
    ALTER COLUMN instance_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_branding_outbox_instance_id ON branding_outbox(instance_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'branding_outbox'
          AND constraint_name = 'fk_branding_outbox_instance'
    ) THEN
        ALTER TABLE branding_outbox
            ADD CONSTRAINT fk_branding_outbox_instance
            FOREIGN KEY (instance_id) REFERENCES instances(id);
    END IF;
END $$;

DO $$
DECLARE
    t text;
    instance_id_tables text[] := ARRAY[
        'payments',
        'branding_outbox',
        'subscription_outbox'
    ];
    tenant_id_tables text[] := ARRAY[
        'consent_record',
        'impersonation_audit_log',
        'onboarding_progress',
        'staff_invitation_audit_log',
        'staff_invitations',
        'users'
    ];
BEGIN
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

        RAISE NOTICE 'RLS policy swept on table % (instance_id, non-forced)', t;
    END LOOP;

    FOREACH t IN ARRAY tenant_id_tables
    LOOP
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = t) THEN
            RAISE NOTICE 'Skipping table % (does not exist)', t;
            CONTINUE;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = t
              AND column_name = 'tenant_id'
              AND data_type = 'uuid'
        ) THEN
            RAISE NOTICE 'Skipping table % (no UUID tenant_id column)', t;
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
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

        RAISE NOTICE 'RLS policy swept on table % (tenant_id, non-forced)', t;
    END LOOP;
END $$;

ALTER TABLE branding_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE branding_templates FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS branding_templates_public_read ON branding_templates;
CREATE POLICY branding_templates_public_read
    ON branding_templates
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS branding_templates_admin_insert ON branding_templates;
CREATE POLICY branding_templates_admin_insert
    ON branding_templates
    FOR INSERT
    WITH CHECK (COALESCE(current_setting('app.is_platform_admin', true)::boolean, false));

DROP POLICY IF EXISTS branding_templates_admin_update ON branding_templates;
CREATE POLICY branding_templates_admin_update
    ON branding_templates
    FOR UPDATE
    USING (COALESCE(current_setting('app.is_platform_admin', true)::boolean, false))
    WITH CHECK (COALESCE(current_setting('app.is_platform_admin', true)::boolean, false));

DROP POLICY IF EXISTS branding_templates_admin_delete ON branding_templates;
CREATE POLICY branding_templates_admin_delete
    ON branding_templates
    FOR DELETE
    USING (COALESCE(current_setting('app.is_platform_admin', true)::boolean, false));
