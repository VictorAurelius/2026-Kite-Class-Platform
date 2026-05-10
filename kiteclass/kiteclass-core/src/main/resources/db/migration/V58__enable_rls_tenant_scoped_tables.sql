-- Phase 1 of GAP-466: Enable Postgres Row-Level Security (RLS) on all tenant-scoped tables
--
-- Per Wave 56 plan:
--   - Code-level enforcement (Hibernate @Filter on BaseEntity) is insufficient because
--     custom JPQL / native SQL / projection DTOs can bypass it.
--   - RLS adds DB-level defense: even raw `SELECT * FROM students` returns only
--     rows where instance_id matches `app.current_tenant_id` session setting.
--
-- Pattern:
--   ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;
--   ALTER TABLE <table> FORCE ROW LEVEL SECURITY;   -- applies to table owner too
--   CREATE POLICY tenant_isolation ON <table>
--     USING (instance_id = current_setting('app.current_tenant_id', true)::uuid);
--
-- `current_setting('app.current_tenant_id', true)` returns NULL if unset (second arg = true
-- means "missing_ok"). When NULL, the policy predicate evaluates to NULL and the row is
-- not visible — default-deny is preserved (preventing data leak via unset context).
--
-- Spring side: TenantAwareDataSourceInterceptor issues `SET LOCAL app.current_tenant_id = ?`
-- per @Transactional boundary, so the value is transaction-scoped and auto-clears on commit/rollback.
--
-- Break-glass: DB superuser can `SET LOCAL row_security = off` for admin/migration ops.
-- See documents/05-guides/operations/runbooks/rls-policy-violation.md.

-- Helper: idempotent application of RLS policy via DO block (CREATE POLICY does not support IF NOT EXISTS in PG < 16)
DO $$
DECLARE
    t text;
    tenant_tables text[] := ARRAY[
        'academic_years',
        'assignments',
        'attendance',
        'attendance_period',
        'audit_log',
        'badges',
        'branding',
        'branding_resources',
        'branding_versions',
        'child_protection_audit_log',
        'class_schedule_slots',
        'classes',
        'courses',
        'curricula',
        'deletion_requests',
        'dmca_takedown_requests',
        'enrollments',
        'frontend_instances',
        'grades',
        'grading_scales',
        'holidays',
        'homeroom_classes',
        'incidents',
        'invoices',
        'moderation_queue',
        'outbox_events',
        'parent_complaint_queue',
        'parent_invitations',
        'parent_read_audit_log',
        'parent_student_links',
        'parents',
        'payments',
        'payroll_configs',
        'payroll_periods',
        'permissions',
        'point_rules',
        'quality_reports',
        'rebrand_approvals',
        'reward_redemptions',
        'rewards',
        'roles',
        'semesters',
        'student_bulk_import_jobs',
        'student_points',
        'students',
        'subject_grades',
        'subject_sections',
        'submissions',
        'teachers',
        'user_roles',
        'vettings'
    ];
BEGIN
    FOREACH t IN ARRAY tenant_tables
    LOOP
        -- Skip tables that do not exist (defensive — some optional features may not have shipped)
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = t) THEN
            RAISE NOTICE 'Skipping table % (does not exist)', t;
            CONTINUE;
        END IF;

        -- Sanity check: table must have instance_id column
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = t AND column_name = 'instance_id'
        ) THEN
            RAISE NOTICE 'Skipping table % (no instance_id column)', t;
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);

        -- Drop existing policy if any (idempotent re-apply)
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', t);

        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            'USING (instance_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid) '
            'WITH CHECK (instance_id = NULLIF(current_setting(''app.current_tenant_id'', true), '''')::uuid)',
            t
        );

        RAISE NOTICE 'RLS enabled on table %', t;
    END LOOP;
END $$;
