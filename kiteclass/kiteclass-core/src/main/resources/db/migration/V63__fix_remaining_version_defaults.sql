-- =========================================================================
-- V63: Backfill remaining version DEFAULT 0 — 8 tables missed by V62
-- =========================================================================
-- Context: V62 (Wave 105 self-test 2026-05-23) fixed 11 tables but the
-- initial DB query truncated output (tail -50) and missed 8 more tables
-- created by V1 core schema with `version BIGINT` (no default).
--
-- Tables: assignments, attendance, badges, class_schedules, class_sessions,
-- classes, courses, enrollments.
--
-- Same fix pattern as V62: SET DEFAULT 0 + UPDATE NULL → 0.
-- Breaking change: NO. Additive + idempotent.
-- =========================================================================

ALTER TABLE assignments     ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE attendance      ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE badges          ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE class_schedules ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE class_sessions  ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE classes         ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE courses         ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE enrollments     ALTER COLUMN version SET DEFAULT 0;

UPDATE assignments     SET version = 0 WHERE version IS NULL;
UPDATE attendance      SET version = 0 WHERE version IS NULL;
UPDATE badges          SET version = 0 WHERE version IS NULL;
UPDATE class_schedules SET version = 0 WHERE version IS NULL;
UPDATE class_sessions  SET version = 0 WHERE version IS NULL;
UPDATE classes         SET version = 0 WHERE version IS NULL;
UPDATE courses         SET version = 0 WHERE version IS NULL;
UPDATE enrollments     SET version = 0 WHERE version IS NULL;
