-- =========================================================================
-- V64: Align grades table with current Grade entity schema (Wave 105 self-test deep)
-- =========================================================================
-- Context: Wave 105 KC local self-test deep re-verification 2026-05-23 found
-- Grade entity (kiteclass-core/src/main/java/.../grade/entity/Grade.java) was
-- refactored to a new column set but no migration applied. SELECT queries via
-- Hibernate fail with `ERROR: column g1_0.comments does not exist` because
-- entity expects columns that V23 (legacy grade module) never created.
--
-- Entity expects (per Grade.java line 81-157):
--   final_score, letter_grade, gpa, status, pass_threshold, comments,
--   calculated_at, finalized_at, finalized_by
-- Plus inherits from BaseEntity:
--   id, instance_id, created_at, updated_at, created_by, updated_by, deleted,
--   version
-- Plus declared:
--   student_id, class_id
--
-- DB currently has (V23 legacy + V62 calculated_at + V26 audit):
--   id, instance_id, class_id, student_id, grade_type, title, score, max_score,
--   weight, feedback, graded_date, created_at, updated_at, graded_by, created_by,
--   updated_by, version, calculated_at
--
-- Strategy: ADD missing columns (nullable initially or with DEFAULT). KEEP
-- legacy V23 columns as nullable for safety (grade_type/title/score/max_score/
-- weight/feedback/graded_date/graded_by) — entity ignores them, no harm; drop
-- in a future cleanup migration once usage confirmed zero.
--
-- Breaking change: NO. Additive ADD COLUMN IF NOT EXISTS. Existing grades data
-- preserved (currently 0 rows; safe for table with data too via defaults).
-- =========================================================================

-- ---------------------------------------------------------------------
-- ADD missing entity columns (Grade entity + BaseEntity.deleted)
-- ---------------------------------------------------------------------
ALTER TABLE grades ADD COLUMN IF NOT EXISTS deleted        BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE grades ADD COLUMN IF NOT EXISTS final_score    NUMERIC(5,2);
ALTER TABLE grades ADD COLUMN IF NOT EXISTS letter_grade   VARCHAR(5);
ALTER TABLE grades ADD COLUMN IF NOT EXISTS gpa            NUMERIC(3,2);
ALTER TABLE grades ADD COLUMN IF NOT EXISTS status         VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS';
ALTER TABLE grades ADD COLUMN IF NOT EXISTS pass_threshold NUMERIC(5,2) NOT NULL DEFAULT 50.0;
ALTER TABLE grades ADD COLUMN IF NOT EXISTS comments       TEXT;
ALTER TABLE grades ADD COLUMN IF NOT EXISTS finalized_at   TIMESTAMP WITH TIME ZONE;
ALTER TABLE grades ADD COLUMN IF NOT EXISTS finalized_by   BIGINT;

COMMENT ON COLUMN grades.final_score    IS 'Final calculated score 0-100 (NULL if not yet calculated)';
COMMENT ON COLUMN grades.letter_grade   IS 'Letter grade A+/A/B+/B/C+/C/D+/D/F mapped from final_score';
COMMENT ON COLUMN grades.gpa            IS 'GPA 0.0-4.0 mapped from letter_grade via grading_scale';
COMMENT ON COLUMN grades.status         IS 'GradeStatus enum: IN_PROGRESS | FINALIZED | PASSED | FAILED';
COMMENT ON COLUMN grades.pass_threshold IS 'Pass score threshold (default 50.0)';
COMMENT ON COLUMN grades.comments       IS 'Teacher comments and feedback (max 2000 chars)';
COMMENT ON COLUMN grades.finalized_at   IS 'When grade was finalized (locked from edits)';
COMMENT ON COLUMN grades.finalized_by   IS 'Teacher ID who finalized the grade';

-- ---------------------------------------------------------------------
-- Add UK on (student_id, class_id) — entity Grade.java line 67-69 declares
-- @UniqueConstraint(name = "uk_grades_student_class", columnNames = {"student_id", "class_id"})
-- ---------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uk_grades_student_class
    ON grades (student_id, class_id)
    WHERE deleted = false;

-- ---------------------------------------------------------------------
-- Legacy V23 NOT NULL columns — drop NOT NULL so entity (which doesn't
-- reference them) can INSERT. Keep columns themselves for backward compat.
-- Drop in future cleanup migration once code-wide usage check confirms zero refs.
-- ---------------------------------------------------------------------
ALTER TABLE grades ALTER COLUMN grade_type  DROP NOT NULL;
ALTER TABLE grades ALTER COLUMN title       DROP NOT NULL;
ALTER TABLE grades ALTER COLUMN score       DROP NOT NULL;
ALTER TABLE grades ALTER COLUMN graded_date DROP NOT NULL;
