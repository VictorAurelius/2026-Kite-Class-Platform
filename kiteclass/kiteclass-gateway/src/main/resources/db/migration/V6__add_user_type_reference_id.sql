-- V6: Add user_type and reference_id to users table for cross-service linking
-- Purpose: Link Gateway User accounts to Core service entities (Student/Teacher/Parent)
-- Reference: cross-service-data-strategy.md

-- Add user_type column (ADMIN, STAFF, TEACHER, PARENT, STUDENT)
ALTER TABLE users
    ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'ADMIN';

-- Add reference_id column (links to students.id, teachers.id, parents.id in Core)
ALTER TABLE users
    ADD COLUMN reference_id BIGINT NULL;

-- Add comment for documentation
COMMENT ON COLUMN users.user_type IS 'Type of user: ADMIN, STAFF (no referenceId), TEACHER, PARENT, STUDENT (has referenceId to Core entity)';
COMMENT ON COLUMN users.reference_id IS 'Foreign key to Core service entity (students.id, teachers.id, parents.id). NULL for ADMIN/STAFF.';

-- Create indexes for better query performance
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_reference_id ON users(reference_id);

-- Update existing owner account to ADMIN type (already has userType = ADMIN as default)
-- No action needed as DEFAULT 'ADMIN' handles this

-- Example data for reference:
-- ADMIN/STAFF: user_type = 'ADMIN' or 'STAFF', reference_id = NULL
-- STUDENT: user_type = 'STUDENT', reference_id = 123 (students.id in Core)
-- TEACHER: user_type = 'TEACHER', reference_id = 456 (teachers.id in Core)
-- PARENT: user_type = 'PARENT', reference_id = 789 (parents.id in Core)
