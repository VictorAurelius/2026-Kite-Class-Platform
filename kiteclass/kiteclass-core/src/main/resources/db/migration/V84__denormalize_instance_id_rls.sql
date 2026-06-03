-- ============================================================================
-- V84: Denormalize instance_id + RLS for academic-structure + M2M join tables
-- ============================================================================
-- Wave 14 Bucket B2 (GAP-908 + GAP-910).
--
-- Scope:
--   * class_schedules (GAP-908): V1 child of `classes`, never had a direct
--     instance_id. Indirect tenant scope via FK class_id → classes.instance_id
--     leaves it open to raw-query cross-tenant leak. Denormalize instance_id
--     (backfill from parent class) + NOT NULL + forced RLS policy.
--   * class_sessions (GAP-908): ALREADY denormalized + RLS-forced in V79
--     (lines 532-572). No action needed here; documented for traceability.
--   * teacher_courses (GAP-910): pure M2M (teacher_id + course_id) created in
--     V27. No direct tenant column by design — scope via JOIN to teachers.
--     Join-based RLS was first applied in V78. Re-asserted here idempotently so
--     this migration owns the GAP-910 closure and matches the join-policy shape
--     `scripts/check-rls-coverage.sh` special-cases (qual LIKE '%teachers%'
--     AND '%app.current_tenant_id%', forced).
--
-- Precedent: V58 (RLS enable+force), V59 tenant_isolation policy (NULL force-fail
-- via NULLIF), V79 leads/contact_messages (direct) + teacher_classes (join).
-- Forward-only, idempotent.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- class_schedules (GAP-908): denormalize instance_id from parent class
-- ---------------------------------------------------------------------------
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS instance_id UUID;

UPDATE class_schedules cs
SET instance_id = c.instance_id
FROM classes c
WHERE cs.class_id = c.id
  AND cs.instance_id IS NULL;

DO $$
DECLARE
    missing_count bigint;
BEGIN
    SELECT COUNT(*) INTO missing_count
    FROM class_schedules
    WHERE instance_id IS NULL;

    IF missing_count > 0 THEN
        RAISE EXCEPTION 'class_schedules has % rows that cannot be backfilled to instance_id', missing_count;
    END IF;
END $$;

ALTER TABLE class_schedules ALTER COLUMN instance_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_class_schedules_instance_id ON class_schedules(instance_id);

ALTER TABLE class_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_schedules FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON class_schedules;
CREATE POLICY tenant_isolation ON class_schedules
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );

COMMENT ON COLUMN class_schedules.instance_id IS
    'Tenant scope denormalized from parent class (GAP-908 Wave 14 B2). Backfilled from classes.instance_id.';

-- ---------------------------------------------------------------------------
-- teacher_courses (GAP-910): join-based RLS re-assert (idempotent)
-- Matches check-rls-coverage.sh special-case: join to teachers + forced.
-- First applied V78; re-asserted here so V84 owns GAP-910 closure.
-- ---------------------------------------------------------------------------
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
    END IF;
END $$;
