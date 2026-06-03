-- Wave local-doable-14 Bucket A — RLS sweep for tenant-scoped tables created
-- after V58/V59, plus re-verify tables flagged by Wave 13 audit.
--
-- Boundary call: the wave plan AC says `app.current_tenant`, but the shipped
-- interceptor and every existing RLS migration use `app.current_tenant_id`.
-- This migration keeps `app.current_tenant_id` to avoid a second, inactive GUC.

DO $$
DECLARE
    t text;
    instance_id_tables text[] := ARRAY[
        'attendance_period',
        'landing_pages',
        'payment_idempotency_keys',
        'payment_records',
        'staff_invitations',
        'vettings',
        'zalo_oa_notification_outbox'
    ];
    tenant_id_tables text[] := ARRAY[
        'idempotency_keys'
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
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
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

        RAISE NOTICE 'RLS policy swept on table % (instance_id, forced)', t;
    END LOOP;

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
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
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

        RAISE NOTICE 'RLS policy swept on table % (tenant_id, forced)', t;
    END LOOP;
END $$;

-- `teacher_courses` is tenant-scoped indirectly through `teachers` and `courses`.
-- It has no direct tenant column, so use a join-backed policy instead of skipping it.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'teacher_courses') THEN
        ALTER TABLE teacher_courses ENABLE ROW LEVEL SECURITY;
        ALTER TABLE teacher_courses FORCE ROW LEVEL SECURITY;

        DROP POLICY IF EXISTS tenant_isolation ON teacher_courses;
        CREATE POLICY tenant_isolation ON teacher_courses
            USING (
                COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
                OR EXISTS (
                    SELECT 1
                    FROM teachers
                    WHERE teachers.id = teacher_courses.teacher_id
                      AND teachers.instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
                )
            )
            WITH CHECK (
                COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
                OR EXISTS (
                    SELECT 1
                    FROM teachers
                    WHERE teachers.id = teacher_courses.teacher_id
                      AND teachers.instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
                )
            );

        RAISE NOTICE 'RLS policy swept on table teacher_courses (teacher join, forced)';
    END IF;
END $$;
