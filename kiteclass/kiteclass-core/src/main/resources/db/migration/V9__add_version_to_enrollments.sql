-- V9: Add version column to enrollments table
-- Fixes missing version field from BaseEntity for optimistic locking

ALTER TABLE enrollments ADD COLUMN version BIGINT;

COMMENT ON COLUMN enrollments.version IS 'Version for optimistic locking';
