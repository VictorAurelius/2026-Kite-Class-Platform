-- Migration: Add multi-tenant support to all tables
-- Version: V5
-- Description: Adds instance_id column and indexes for tenant isolation
-- Author: KiteClass Team
-- Date: 2026-02-04

-- Add instance_id to students table
ALTER TABLE students
ADD COLUMN instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

CREATE INDEX idx_students_instance_id ON students(instance_id);

COMMENT ON COLUMN students.instance_id IS 'Tenant instance ID for multi-tenant data isolation';

-- Add instance_id to teachers table
ALTER TABLE teachers
ADD COLUMN instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

CREATE INDEX idx_teachers_instance_id ON teachers(instance_id);

COMMENT ON COLUMN teachers.instance_id IS 'Tenant instance ID for multi-tenant data isolation';

-- Add instance_id to teacher_classes table
ALTER TABLE teacher_classes
ADD COLUMN instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

CREATE INDEX idx_teacher_classes_instance_id ON teacher_classes(instance_id);

COMMENT ON COLUMN teacher_classes.instance_id IS 'Tenant instance ID for multi-tenant data isolation';

-- Add instance_id to teacher_courses table
ALTER TABLE teacher_courses
ADD COLUMN instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

CREATE INDEX idx_teacher_courses_instance_id ON teacher_courses(instance_id);

COMMENT ON COLUMN teacher_courses.instance_id IS 'Tenant instance ID for multi-tenant data isolation';

-- Add instance_id to courses table (if exists)
-- Note: Course table structure depends on V4 migration
ALTER TABLE courses
ADD COLUMN instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

CREATE INDEX idx_courses_instance_id ON courses(instance_id);

COMMENT ON COLUMN courses.instance_id IS 'Tenant instance ID for multi-tenant data isolation';

-- Important notes:
-- 1. Default UUID '00000000-0000-0000-0000-000000000000' is used for existing data
-- 2. Application code must set proper instance_id when creating new entities
-- 3. Hibernate filter "tenantFilter" will automatically filter queries by instance_id
-- 4. TenantContext must be set before any database operations
