-- Migration: Add multi-tenant unique constraints
-- Version: V6
-- Description: Adds composite unique constraints scoped to instance_id for tenant isolation
-- Author: KiteClass Team
-- Date: 2026-02-12

-- ============================================================================
-- Students table: Add composite unique constraint (email, instance_id, deleted)
-- Requirement: Each tenant can have unique emails within their instance
-- ============================================================================

-- Add composite unique constraint for students email
-- Constraint: (email, instance_id) must be unique for non-deleted records
-- Allows: Same email in different tenants, same email after soft delete
CREATE UNIQUE INDEX uk_students_email_instance_id
ON students(email, instance_id)
WHERE deleted = FALSE AND email IS NOT NULL;

COMMENT ON INDEX uk_students_email_instance_id IS 'Ensures email uniqueness within tenant instance (excluding deleted records and NULL emails)';

-- ============================================================================
-- Teachers table: Replace global email uniqueness with tenant-scoped uniqueness
-- Requirement: Each tenant can have unique emails within their instance
-- ============================================================================

-- Drop existing global unique constraint on teachers.email
ALTER TABLE teachers DROP CONSTRAINT IF EXISTS teachers_email_key;

-- Add composite unique constraint for teachers email
-- Constraint: (email, instance_id) must be unique for non-deleted records
-- Allows: Same email in different tenants, same email after soft delete
CREATE UNIQUE INDEX uk_teachers_email_instance_id
ON teachers(email, instance_id)
WHERE deleted = FALSE;

COMMENT ON INDEX uk_teachers_email_instance_id IS 'Ensures email uniqueness within tenant instance (excluding deleted records)';

-- ============================================================================
-- Courses table: Replace global code uniqueness with tenant-scoped uniqueness
-- Requirement: Each tenant can have unique course codes within their instance
-- ============================================================================

-- Drop existing global unique constraint on courses.code
ALTER TABLE courses DROP CONSTRAINT IF EXISTS courses_code_key;

-- Drop existing unique index (created in V4)
DROP INDEX IF EXISTS idx_courses_code;

-- Add composite unique constraint for courses code
-- Constraint: (code, instance_id) must be unique for non-deleted records
-- Allows: Same code in different tenants, same code after soft delete
CREATE UNIQUE INDEX uk_courses_code_instance_id
ON courses(code, instance_id)
WHERE deleted = FALSE;

COMMENT ON INDEX uk_courses_code_instance_id IS 'Ensures course code uniqueness within tenant instance (excluding deleted records)';

-- ============================================================================
-- Important notes:
-- 1. Partial unique indexes (WHERE deleted = FALSE) allow reuse after soft delete
-- 2. NULL emails in students table are allowed (multiple students can have NULL email)
-- 3. Teachers table requires email (NOT NULL), so no NULL handling needed
-- 4. Same email/code can exist in different tenants (multi-tenant isolation)
-- 5. Application code must check uniqueness using: findByEmailAndInstanceIdAndDeletedFalse()
-- ============================================================================
