-- GAP-805 walk fix: grades unique constraint must include grade_type.
--
-- ROOT CAUSE
-- ----------
-- V64 created `uk_grades_student_class` UNIQUE (student_id, class_id) WHERE deleted=false,
-- mirroring Grade.java @UniqueConstraint(columnNames = {"student_id", "class_id"}).
-- This is wrong business-wise: a student in a class earns MULTIPLE grade types
-- (midterm + assignment + final). The 2-column index allows only ONE grade row per
-- (student, class) — any teacher entering a 2nd grade type hits a unique violation.
-- Surfaced 2026-05-29 during demo-trio RST walk: seed-sky-demo-enrich (3 grade types
-- per student-class) failed with "duplicate key value violates uk_grades_student_class".
--
-- FIX
-- ---
-- Drop the 2-column index, recreate including grade_type. Soft-delete predicate kept.
-- Paired with Grade.java @UniqueConstraint rename → uk_grades_student_class_type.

DROP INDEX IF EXISTS uk_grades_student_class;

CREATE UNIQUE INDEX IF NOT EXISTS uk_grades_student_class_type
    ON grades (student_id, class_id, grade_type)
    WHERE deleted = false;
