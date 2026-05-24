-- V65: Enrollment capacity model — add DB-level constraints to classes table.
-- Context: Wave beta-readiness-1 Bucket B — failure-mode matrix A4/B5 capacity-race guard.
--
-- Class entity already has max_students + current_enrolled columns (V27).
-- OPTIMISTIC_FORCE_INCREMENT lock already wired in ClassRepository (Wave 105 Bucket E0).
-- This migration:
--   1. Adds CHECK constraints so DB enforces capacity invariant independently of JPA layer.
--   2. Adds CHECK for max_students >= 1 (entity default 30, but guard DB too).
--   3. Adds non-null constraints with safe defaults (columns exist from V27; guard future schema).
--
-- All idempotent (DROP CONSTRAINT IF EXISTS before ADD).

-- Ensure columns are NOT NULL (V27 added them, but earlier rows might lack NOT NULL).
ALTER TABLE classes ALTER COLUMN max_students SET NOT NULL;
ALTER TABLE classes ALTER COLUMN max_students SET DEFAULT 30;
ALTER TABLE classes ALTER COLUMN current_enrolled SET NOT NULL;
ALTER TABLE classes ALTER COLUMN current_enrolled SET DEFAULT 0;

-- Capacity integrity CHECK: enrolled count must never exceed max seats.
ALTER TABLE classes DROP CONSTRAINT IF EXISTS chk_classes_capacity;
ALTER TABLE classes ADD CONSTRAINT chk_classes_capacity
    CHECK (current_enrolled >= 0 AND current_enrolled <= max_students);

-- Positive capacity CHECK: max_students must be at least 1.
ALTER TABLE classes DROP CONSTRAINT IF EXISTS chk_classes_max_students_positive;
ALTER TABLE classes ADD CONSTRAINT chk_classes_max_students_positive
    CHECK (max_students >= 1);
