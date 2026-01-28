-- Migration: Create course tables
-- Version: V4
-- Description: Creates courses table for Core Service
-- Author: KiteClass Team
-- Date: 2026-01-28

-- ============================================================================
-- Table: courses
-- Description: Stores course information and educational content
-- ============================================================================
CREATE TABLE courses (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Basic information
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,

    -- Educational content
    syllabus TEXT,
    objectives TEXT,
    prerequisites TEXT,
    target_audience TEXT,

    -- Course details
    teacher_id BIGINT,
    duration_weeks INT CHECK (duration_weeks >= 1),
    total_sessions INT CHECK (total_sessions >= 1),

    -- Pricing and media
    price DECIMAL(15,2) CHECK (price >= 0),
    cover_image_url VARCHAR(500),

    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Optimistic locking
    version BIGINT,

    -- Foreign key constraints
    CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id)
        REFERENCES teachers(id) ON DELETE SET NULL
);

-- Create indexes for courses table
CREATE UNIQUE INDEX idx_courses_code ON courses(code) WHERE deleted = FALSE;
CREATE INDEX idx_courses_status ON courses(status) WHERE deleted = FALSE;
CREATE INDEX idx_courses_teacher_id ON courses(teacher_id) WHERE deleted = FALSE;
CREATE INDEX idx_courses_name ON courses(name) WHERE deleted = FALSE;
CREATE INDEX idx_courses_deleted ON courses(deleted);

-- Add check constraint for status enum
ALTER TABLE courses ADD CONSTRAINT chk_courses_status
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));

-- Add comments for courses table
COMMENT ON TABLE courses IS 'Stores course information for KiteClass system';
COMMENT ON COLUMN courses.id IS 'Unique identifier for course';
COMMENT ON COLUMN courses.name IS 'Course name (required, 5-200 chars)';
COMMENT ON COLUMN courses.code IS 'Course code (required, unique, 3-50 chars, uppercase with hyphens)';
COMMENT ON COLUMN courses.description IS 'Course description (max 5000 chars)';
COMMENT ON COLUMN courses.syllabus IS 'Course syllabus with week-by-week content (max 10000 chars)';
COMMENT ON COLUMN courses.objectives IS 'Learning objectives (max 5000 chars)';
COMMENT ON COLUMN courses.prerequisites IS 'Prerequisites for the course (max 2000 chars)';
COMMENT ON COLUMN courses.target_audience IS 'Target audience description (max 1000 chars)';
COMMENT ON COLUMN courses.teacher_id IS 'Foreign key to teachers.id (course creator, CREATOR role)';
COMMENT ON COLUMN courses.duration_weeks IS 'Course duration in weeks (>= 1)';
COMMENT ON COLUMN courses.total_sessions IS 'Total number of sessions (>= 1)';
COMMENT ON COLUMN courses.price IS 'Course price in VND (>= 0, NULL or 0 = free)';
COMMENT ON COLUMN courses.cover_image_url IS 'URL to course cover image (max 500 chars)';
COMMENT ON COLUMN courses.status IS 'Course lifecycle status: DRAFT, PUBLISHED, ARCHIVED (default: DRAFT)';
COMMENT ON COLUMN courses.deleted IS 'Soft delete flag (TRUE = deleted)';
COMMENT ON COLUMN courses.version IS 'Version for optimistic locking';

-- ============================================================================
-- Update teacher_courses table to add course_id foreign key
-- Description: Adds FK constraint now that courses table exists
-- ============================================================================
ALTER TABLE teacher_courses
    ADD CONSTRAINT fk_teacher_courses_course FOREIGN KEY (course_id)
        REFERENCES courses(id) ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_teacher_courses_course ON teacher_courses IS 'Foreign key to courses.id, cascade delete when course is deleted';
