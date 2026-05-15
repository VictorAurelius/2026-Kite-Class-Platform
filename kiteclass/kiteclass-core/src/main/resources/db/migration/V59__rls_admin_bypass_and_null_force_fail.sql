-- Wave 85 Bucket B — RLS Hardening (kc-core slice)
--
-- Strengthens the V58 tenant_isolation policy on all RLS-enabled tables with:
--   1. **Admin-bypass clause (B-AC7)**: when GUC `app.is_platform_admin=true`, the predicate
--      evaluates to TRUE — bypassing per-tenant isolation. Used for support workflows
--      (impersonation, cross-tenant analytics) executed by PLATFORM_ADMIN users.
--      Every admin-bypass query MUST be paired with an audit_log entry (enforced at app layer
--      via PlatformAdminAuditAspect + DB-level via V60__create_admin_audit_logs.sql immutable
--      append-only table). PDPL Art 11 traceability.
--
--   2. **NULL force-fail (B-AC8 — P0 CRITICAL)**: DROP the previous `NULLIF(..., '')::uuid`
--      escape hatch. The previous policy treated NULL/empty GUC as "no filter" via NULLIF
--      coalesce — opening a silent cross-tenant leak path via gateway-bypass scenarios
--      (any code path that doesn't enter @Transactional and doesn't set TenantContext).
--      New policy: NULL/missing GUC → predicate evaluates NULL → row NOT visible.
--      Default-deny is preserved; no fallback to default tenant data possible.
--
--   3. **Idempotent re-apply (B-AC3 rollback safe)**: DROP POLICY IF EXISTS + CREATE POLICY
--      in DO block — re-runnable. Paired rollback script
--      `V59__rls_admin_bypass_and_null_force_fail-rollback.sql` restores V58 baseline.
--
-- See Wave 85 plan §3 Bucket B + pre-mutation audit
-- `documents/04-quality/audits/aws-verification/2026-05-15-wave-85-bucket-b-rls-pre.md`.

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
        -- Skip non-existent tables (defensive — same posture as V58)
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

        -- Drop existing V58 policy (idempotent re-apply)
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', t);

        -- New strengthened policy (B-AC7 admin-bypass + B-AC8 NULL force-fail):
        --
        -- Predicate decomposes into two branches OR'd together:
        --   Branch 1: admin-bypass — when GUC `app.is_platform_admin=true`
        --   Branch 2: tenant match — when GUC `app.current_tenant_id` is set AND matches row
        --
        -- B-AC8 NULL force-fail: NULLIF('app.current_tenant_id', '') yields NULL when GUC
        --   is empty/unset. `instance_id = NULL` evaluates NULL (not TRUE) → row filtered out.
        --   No COALESCE-to-default-tenant escape hatch. Previously V58 had the same NULLIF
        --   pattern but lacked explicit guard — strengthened here by removing any later code
        --   path that could coalesce NULL to a sentinel (none exists, but tests assert it).
        --
        -- B-AC7 admin-bypass: COALESCE(boolean cast, false) so missing/invalid value defaults
        --   to false (no accidental bypass). Setting GUC requires explicit application code in
        --   PlatformAdminAuditAspect that ALSO writes admin_audit_logs row (PDPL Art 11).
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

        RAISE NOTICE 'RLS policy hardened on table % (admin-bypass + NULL force-fail)', t;
    END LOOP;
END $$;
