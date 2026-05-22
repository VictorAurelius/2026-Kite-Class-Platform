-- =========================================================================
-- V62: Backfill missing schema bits surfaced by Wave 105 self-test 2026-05-23
-- =========================================================================
-- Context: Wave 105 local self-test (audit
--   documents/04-quality/audits/local-stack/2026-05-23-wave-105-kc-self-test.md)
--   identified 2 pre-existing schema gaps that block PR validation:
--
--   1. grades.calculated_at column referenced by Grade entity but never
--      created by any migration. GradeService.calculateFinalScore() updates
--      this field; SELECT queries fail with "column g1_0.calculated_at does
--      not exist" → 500 on every grade read after enrollment touch.
--
--   2. 11 tables have `version BIGINT` column (added by V26 audit-columns
--      backfill) without DEFAULT 0. Hibernate @Version annotation expects
--      non-null value; inserts via JPA work because entity initializer sets
--      it, but raw INSERTs (test fixtures, seed scripts) and rows inserted
--      via legacy paths fail with NullPointerException at flush time.
--
-- Both gaps are PRE-EXISTING (not introduced by Wave 105 PRs); surfaced
-- only when test walk seeded data via direct SQL + traversed real endpoints.
--
-- Breaking change: NO. Additive ADD COLUMN IF NOT EXISTS + SET DEFAULT +
-- UPDATE rows with NULL version → 0 (idempotent on re-run).
-- =========================================================================

-- ---------------------------------------------------------------------
-- Bug 1: grades.calculated_at — referenced by Grade.java line 143-144
-- (@Column(name = "calculated_at") LocalDateTime calculatedAt)
-- ---------------------------------------------------------------------
ALTER TABLE grades ADD COLUMN IF NOT EXISTS calculated_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN grades.calculated_at IS 'Timestamp when final score was computed by GradeService.calculateFinalScore(). NULL = not yet calculated (raw component scores only).';

-- ---------------------------------------------------------------------
-- Bug 2: version DEFAULT 0 on 11 tables (Hibernate @Version NPE class).
-- Pattern: SET DEFAULT then UPDATE existing NULL rows.
-- Tables: grades, grading_scales, invoice_items, point_rules,
--   reward_redemptions, rewards, student_badges, student_points,
--   students, submissions, teachers
-- ---------------------------------------------------------------------
ALTER TABLE grades             ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE grading_scales     ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE invoice_items      ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE point_rules        ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE reward_redemptions ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE rewards            ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE student_badges     ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE student_points     ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE students           ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE submissions        ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE teachers           ALTER COLUMN version SET DEFAULT 0;

-- Backfill NULL rows to 0 (rows inserted before DEFAULT was set).
UPDATE grades             SET version = 0 WHERE version IS NULL;
UPDATE grading_scales     SET version = 0 WHERE version IS NULL;
UPDATE invoice_items      SET version = 0 WHERE version IS NULL;
UPDATE point_rules        SET version = 0 WHERE version IS NULL;
UPDATE reward_redemptions SET version = 0 WHERE version IS NULL;
UPDATE rewards            SET version = 0 WHERE version IS NULL;
UPDATE student_badges     SET version = 0 WHERE version IS NULL;
UPDATE student_points     SET version = 0 WHERE version IS NULL;
UPDATE students           SET version = 0 WHERE version IS NULL;
UPDATE submissions        SET version = 0 WHERE version IS NULL;
UPDATE teachers           SET version = 0 WHERE version IS NULL;
