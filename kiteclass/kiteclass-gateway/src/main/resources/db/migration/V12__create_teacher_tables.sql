-- Migration: Create teacher tables
-- Version: V3
-- Description: Creates teachers, teacher_courses, and teacher_classes tables for Core Service
-- Author: KiteClass Team
-- Date: 2026-01-28

-- ============================================================================
-- Table: teachers
-- Description: Stores teacher information and professional details
-- ============================================================================
CREATE TABLE teachers (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Personal information
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20),

    -- Professional information
    specialization VARCHAR(100),
    bio TEXT,
    qualification VARCHAR(200),
    experience_years INT CHECK (experience_years >= 0),

    -- Profile
    avatar_url VARCHAR(500),

    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Optimistic locking
    version BIGINT
);

-- Create indexes for teachers table
CREATE INDEX idx_teachers_email ON teachers(email) WHERE deleted = FALSE;
CREATE INDEX idx_teachers_status ON teachers(status) WHERE deleted = FALSE;
CREATE INDEX idx_teachers_specialization ON teachers(specialization) WHERE deleted = FALSE;
CREATE INDEX idx_teachers_deleted ON teachers(deleted);
CREATE INDEX idx_teachers_name ON teachers(name) WHERE deleted = FALSE;

-- Add comments for teachers table
COMMENT ON TABLE teachers IS 'Stores teacher information for KiteClass system';
COMMENT ON COLUMN teachers.id IS 'Unique identifier for teacher';
COMMENT ON COLUMN teachers.name IS 'Teacher full name (required)';
COMMENT ON COLUMN teachers.email IS 'Teacher email address (unique, required)';
COMMENT ON COLUMN teachers.phone_number IS 'Teacher phone number (Vietnamese format: 10 digits starting with 0)';
COMMENT ON COLUMN teachers.specialization IS 'Teacher specialization/subject area (e.g., English, Math)';
COMMENT ON COLUMN teachers.bio IS 'Teacher biography/introduction (max 2000 chars)';
COMMENT ON COLUMN teachers.qualification IS 'Teacher qualification/education (e.g., Bachelor, Master)';
COMMENT ON COLUMN teachers.experience_years IS 'Years of teaching experience (>= 0)';
COMMENT ON COLUMN teachers.avatar_url IS 'URL to teacher profile picture';
COMMENT ON COLUMN teachers.status IS 'Teacher status: ACTIVE, INACTIVE, ON_LEAVE (default: ACTIVE)';
COMMENT ON COLUMN teachers.deleted IS 'Soft delete flag (TRUE = deleted)';
COMMENT ON COLUMN teachers.version IS 'Version for optimistic locking';

-- ============================================================================
-- Table: teacher_courses
-- Description: Course-level assignment between teacher and course
-- Controls course-level permissions (CREATOR, INSTRUCTOR, ASSISTANT)
-- ============================================================================
CREATE TABLE teacher_courses (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    teacher_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,

    -- Role and assignment info
    role VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,

    -- Constraints
    CONSTRAINT fk_teacher_courses_teacher FOREIGN KEY (teacher_id)
        REFERENCES teachers(id) ON DELETE CASCADE,
    -- Note: course_id FK will be added when Course module is implemented
    CONSTRAINT uk_teacher_courses_teacher_course UNIQUE (teacher_id, course_id)
);

-- Create indexes for teacher_courses table
CREATE INDEX idx_teacher_courses_teacher_id ON teacher_courses(teacher_id);
CREATE INDEX idx_teacher_courses_course_id ON teacher_courses(course_id);
CREATE INDEX idx_teacher_courses_role ON teacher_courses(role);

-- Add comments for teacher_courses table
COMMENT ON TABLE teacher_courses IS 'Course-level assignment between teacher and course';
COMMENT ON COLUMN teacher_courses.teacher_id IS 'Foreign key to teachers.id';
COMMENT ON COLUMN teacher_courses.course_id IS 'Foreign key to courses.id';
COMMENT ON COLUMN teacher_courses.role IS 'Teacher role in course: CREATOR, INSTRUCTOR, ASSISTANT';
COMMENT ON COLUMN teacher_courses.assigned_at IS 'Timestamp when teacher was assigned to course';
COMMENT ON COLUMN teacher_courses.assigned_by IS 'User ID who assigned teacher (NULL if self-created)';

-- ============================================================================
-- Table: teacher_classes
-- Description: Class-level assignment between teacher and class
-- Controls class-level permissions (MAIN_TEACHER, ASSISTANT)
-- ============================================================================
CREATE TABLE teacher_classes (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign keys
    teacher_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,

    -- Role and assignment info
    role VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,

    -- Constraints
    CONSTRAINT fk_teacher_classes_teacher FOREIGN KEY (teacher_id)
        REFERENCES teachers(id) ON DELETE CASCADE,
    -- Note: class_id FK will be added when Class module is implemented
    CONSTRAINT uk_teacher_classes_teacher_class UNIQUE (teacher_id, class_id)
);

-- Create indexes for teacher_classes table
CREATE INDEX idx_teacher_classes_teacher_id ON teacher_classes(teacher_id);
CREATE INDEX idx_teacher_classes_class_id ON teacher_classes(class_id);

-- Add comments for teacher_classes table
COMMENT ON TABLE teacher_classes IS 'Class-level assignment between teacher and class';
COMMENT ON COLUMN teacher_classes.teacher_id IS 'Foreign key to teachers.id';
COMMENT ON COLUMN teacher_classes.class_id IS 'Foreign key to classes.id';
COMMENT ON COLUMN teacher_classes.role IS 'Teacher role in class: MAIN_TEACHER, ASSISTANT';
COMMENT ON COLUMN teacher_classes.assigned_at IS 'Timestamp when teacher was assigned to class';
COMMENT ON COLUMN teacher_classes.assigned_by IS 'User ID who assigned teacher to class';
