-- =========================================================================
-- V81: student_badges instance_id + RLS tenant isolation (GAP-887)
-- =========================================================================
-- Context: GAP-887 (Wave 13 cluster docs writing — KC gamification).
-- 5/6 gamification tables have `instance_id` + RLS FORCED. student_badges
-- (V1:766) was created WITHOUT instance_id → V58/V59 sanity-check skipped it
-- (RLS loop CONTINUEs when no instance_id column) → no tenant_isolation policy.
-- Raw `SELECT * FROM student_badges` returns rows cross-tenant.
--
-- State-check (2026-06-03):
--   - student_badges has FK student_id → students(id) (V1:769).
--   - students has instance_id UUID NOT NULL → backfill source.
--   - No badge_id→instance_id needed; student_id join is authoritative + 1:1
--     tenant scope (a student belongs to exactly one instance).
--
-- Steps (per V64 entity-align + V58/V59 RLS precedents):
--   1. ADD COLUMN instance_id UUID (nullable first — backfill needs it set)
--   2. Backfill from students.instance_id via student_id FK join
--   3. SET NOT NULL after backfill (matches sibling gamification tables)
--   4. Index for tenant-scoped queries
--   5. ENABLE + FORCE RLS + tenant_isolation policy (V59 hardened pattern:
--      admin-bypass via app.is_platform_admin + NULL force-fail)
--
-- Breaking change: NO. Additive column + backfill + RLS. Idempotent
-- (IF NOT EXISTS on column/index; DROP POLICY IF EXISTS before CREATE).
-- Test profile: RLS bypass via DB superuser `SET LOCAL row_security = off`
-- (per V58 break-glass note) — Testcontainers runs migrations as superuser.
-- =========================================================================

-- 1. Add column (nullable for backfill)
ALTER TABLE student_badges ADD COLUMN IF NOT EXISTS instance_id UUID;

-- 2. Backfill from students.instance_id via student_id FK
UPDATE student_badges sb
SET instance_id = s.instance_id
FROM students s
WHERE sb.student_id = s.id
  AND sb.instance_id IS NULL;

-- 3. Enforce NOT NULL after backfill (defensive: only if no NULL rows remain).
--    If any orphan row has no matching student (shouldn't happen — FK enforced),
--    the SET NOT NULL would fail loudly — desirable (surfaces data integrity bug).
ALTER TABLE student_badges ALTER COLUMN instance_id SET NOT NULL;

-- 4. Index for tenant-scoped queries (sibling gamification table pattern)
CREATE INDEX IF NOT EXISTS idx_student_badges_instance_id ON student_badges(instance_id);

COMMENT ON COLUMN student_badges.instance_id IS
    'Tenant scope (GAP-887). Backfilled from students.instance_id via student_id FK. '
    'Enforced by RLS tenant_isolation policy (V81) — defense beyond Hibernate @Filter.';

-- 5. Enable RLS + hardened tenant_isolation policy (mirrors V59 admin-bypass + NULL force-fail)
ALTER TABLE student_badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_badges FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON student_badges;
CREATE POLICY tenant_isolation ON student_badges
    USING (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    )
    WITH CHECK (
        COALESCE(current_setting('app.is_platform_admin', true)::boolean, false)
        OR instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid
    );
