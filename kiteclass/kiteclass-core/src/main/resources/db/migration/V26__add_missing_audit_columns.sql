-- Add missing audit columns (created_by, updated_by, version) to all tables
-- These columns are required by BaseEntity for JPA auditing and optimistic locking.
-- V1 created most tables without these columns; this migration adds them idempotently.

ALTER TABLE students        ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE students        ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE students        ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE teachers        ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE teachers        ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE teachers        ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE courses         ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE courses         ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE classes         ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE classes         ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE class_sessions  ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE class_sessions  ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE class_sessions  ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE enrollments     ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE enrollments     ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE attendance      ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE attendance      ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE attendance      ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE grades          ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE grades          ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE grades          ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE grading_scales  ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE grading_scales  ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE grading_scales  ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE assignments     ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE assignments     ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE submissions     ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE submissions     ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE submissions     ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE invoices        ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE invoices        ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE invoice_items   ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE invoice_items   ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE invoice_items   ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE payments        ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE payments        ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE payments        ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE point_rules     ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE point_rules     ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE point_rules     ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE student_points  ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE student_points  ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE student_points  ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE badges          ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE badges          ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE badges          ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE student_badges  ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE student_badges  ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE student_badges  ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE rewards         ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE rewards         ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE rewards         ADD COLUMN IF NOT EXISTS version    BIGINT;

ALTER TABLE reward_redemptions ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE reward_redemptions ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE reward_redemptions ADD COLUMN IF NOT EXISTS version    BIGINT;
