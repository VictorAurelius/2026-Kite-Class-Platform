-- Migration: Create students table
-- Version: V2
-- Description: Creates students table with indexes for Core Service
-- Author: KiteClass Team
-- Date: 2026-01-27

-- Create students table
CREATE TABLE students (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Personal information
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),

    -- Contact information
    address TEXT,

    -- Profile
    avatar_url VARCHAR(500),

    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    note TEXT,

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

-- Create indexes for better query performance
CREATE INDEX idx_students_email ON students(email) WHERE deleted = FALSE;
CREATE INDEX idx_students_phone ON students(phone) WHERE deleted = FALSE;
CREATE INDEX idx_students_status ON students(status) WHERE deleted = FALSE;
CREATE INDEX idx_students_deleted ON students(deleted);
CREATE INDEX idx_students_name ON students(name) WHERE deleted = FALSE;

-- Add comments for documentation
COMMENT ON TABLE students IS 'Stores student information for KiteClass system';
COMMENT ON COLUMN students.id IS 'Unique identifier for student';
COMMENT ON COLUMN students.name IS 'Student full name (required)';
COMMENT ON COLUMN students.email IS 'Student email address (unique)';
COMMENT ON COLUMN students.phone IS 'Student phone number (Vietnamese format: 10 digits starting with 0)';
COMMENT ON COLUMN students.date_of_birth IS 'Student date of birth for age calculation';
COMMENT ON COLUMN students.gender IS 'Student gender: MALE, FEMALE, OTHER';
COMMENT ON COLUMN students.address IS 'Student full address';
COMMENT ON COLUMN students.avatar_url IS 'URL to student profile picture';
COMMENT ON COLUMN students.status IS 'Student status: PENDING, ACTIVE, INACTIVE, GRADUATED, DROPPED';
COMMENT ON COLUMN students.note IS 'Additional notes about the student';
COMMENT ON COLUMN students.deleted IS 'Soft delete flag (TRUE = deleted)';
COMMENT ON COLUMN students.version IS 'Version for optimistic locking';
