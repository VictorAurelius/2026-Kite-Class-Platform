-- Migration: Create class tables
-- Version: V7
-- Description: Creates classes and class_sessions tables for Core Service
-- Author: KiteClass Team
-- Date: 2026-02-21

-- ============================================================================
-- Table: classes
-- Description: Lớp học cụ thể (instance của course)
-- ============================================================================
CREATE TABLE classes (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Relationship to course
    course_id BIGINT NOT NULL,

    -- Basic information
    name VARCHAR(200) NOT NULL,
    description TEXT,

    -- Schedule
    schedule VARCHAR(200),

    -- Location
    location_type VARCHAR(20) NOT NULL DEFAULT 'IN_PERSON',
    location_detail VARCHAR(200),

    -- Dates
    start_date DATE,
    end_date DATE,

    -- Capacity
    max_students INT NOT NULL DEFAULT 30 CHECK (max_students >= 1),
    current_enrolled INT NOT NULL DEFAULT 0 CHECK (current_enrolled >= 0),

    -- Class code for self-enrollment
    class_code VARCHAR(20),
    code_expires_at TIMESTAMP,

    -- Status lifecycle
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',

    -- Audit timestamps for lifecycle events
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,

    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Multi-tenant
    instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',

    -- Soft delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Optimistic locking
    version BIGINT,

    -- Foreign key to courses
    CONSTRAINT fk_classes_course FOREIGN KEY (course_id)
        REFERENCES courses(id) ON DELETE RESTRICT
);

-- Indexes for classes table
CREATE INDEX idx_classes_course_id ON classes(course_id) WHERE deleted = FALSE;
CREATE INDEX idx_classes_status ON classes(status) WHERE deleted = FALSE;
CREATE INDEX idx_classes_start_date ON classes(start_date) WHERE deleted = FALSE;
CREATE INDEX idx_classes_instance_id ON classes(instance_id);
CREATE UNIQUE INDEX idx_classes_class_code_unique
    ON classes(class_code)
    WHERE class_code IS NOT NULL AND deleted = FALSE;

-- Composite unique: class name per course per tenant
CREATE UNIQUE INDEX uk_classes_name_course_instance
    ON classes(name, course_id, instance_id)
    WHERE deleted = FALSE;

-- Status constraint
ALTER TABLE classes ADD CONSTRAINT chk_classes_status
    CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

-- Location type constraint
ALTER TABLE classes ADD CONSTRAINT chk_classes_location_type
    CHECK (location_type IN ('IN_PERSON', 'ONLINE'));

-- Date validation: end_date > start_date
ALTER TABLE classes ADD CONSTRAINT chk_classes_dates
    CHECK (end_date IS NULL OR start_date IS NULL OR end_date > start_date);

-- Capacity check
ALTER TABLE classes ADD CONSTRAINT chk_classes_capacity
    CHECK (current_enrolled <= max_students);

-- Comments
COMMENT ON TABLE classes IS 'Class instances of courses in KiteClass system';
COMMENT ON COLUMN classes.id IS 'Unique identifier for class';
COMMENT ON COLUMN classes.course_id IS 'FK to courses.id - class belongs to course';
COMMENT ON COLUMN classes.name IS 'Class name (required, 5-200 chars)';
COMMENT ON COLUMN classes.description IS 'Class description (optional, max 2000 chars)';
COMMENT ON COLUMN classes.schedule IS 'Schedule text e.g. Mon-Wed-Fri 18:00-20:00 (max 200 chars)';
COMMENT ON COLUMN classes.location_type IS 'IN_PERSON or ONLINE';
COMMENT ON COLUMN classes.location_detail IS 'Room number or online link (max 200 chars)';
COMMENT ON COLUMN classes.start_date IS 'Class start date';
COMMENT ON COLUMN classes.end_date IS 'Class end date (must be after start_date)';
COMMENT ON COLUMN classes.max_students IS 'Maximum students allowed (>= 1, default 30)';
COMMENT ON COLUMN classes.current_enrolled IS 'Current active enrollments count (auto-managed)';
COMMENT ON COLUMN classes.class_code IS 'Unique code for student self-enrollment (6-20 chars, uppercase)';
COMMENT ON COLUMN classes.code_expires_at IS 'Expiry timestamp for class code';
COMMENT ON COLUMN classes.status IS 'Lifecycle: SCHEDULED (default), IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN classes.started_at IS 'When class transitioned to IN_PROGRESS';
COMMENT ON COLUMN classes.completed_at IS 'When class transitioned to COMPLETED';
COMMENT ON COLUMN classes.cancelled_at IS 'When class transitioned to CANCELLED';
COMMENT ON COLUMN classes.instance_id IS 'Tenant instance ID for multi-tenant isolation';

-- ============================================================================
-- Table: class_sessions
-- Description: Individual sessions/buổi học của class
-- ============================================================================
CREATE TABLE class_sessions (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    class_id BIGINT NOT NULL,

    -- Session details
    session_number INT NOT NULL CHECK (session_number >= 1),
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    -- Optional overrides
    location VARCHAR(200),
    topic VARCHAR(200),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    attendance_taken BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Multi-tenant (inherited from parent class)
    instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',

    -- Soft delete (follows parent class)
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Optimistic locking
    version BIGINT,

    -- Foreign key
    CONSTRAINT fk_class_sessions_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE CASCADE,

    -- Time validation: end_time > start_time
    CONSTRAINT chk_session_times CHECK (end_time > start_time)
);

-- Indexes for class_sessions
CREATE INDEX idx_class_sessions_class_id ON class_sessions(class_id) WHERE deleted = FALSE;
CREATE INDEX idx_class_sessions_date ON class_sessions(session_date) WHERE deleted = FALSE;
CREATE INDEX idx_class_sessions_status ON class_sessions(status) WHERE deleted = FALSE;
CREATE INDEX idx_class_sessions_instance_id ON class_sessions(instance_id);

-- Unique session number per class
CREATE UNIQUE INDEX uk_class_sessions_number
    ON class_sessions(class_id, session_number)
    WHERE deleted = FALSE;

-- Status constraint
ALTER TABLE class_sessions ADD CONSTRAINT chk_session_status
    CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'MAKEUP'));

-- Comments
COMMENT ON TABLE class_sessions IS 'Individual sessions (buổi học) within a class';
COMMENT ON COLUMN class_sessions.id IS 'Unique identifier for session';
COMMENT ON COLUMN class_sessions.class_id IS 'FK to classes.id';
COMMENT ON COLUMN class_sessions.session_number IS 'Sequential number within class (1, 2, 3, ...)';
COMMENT ON COLUMN class_sessions.session_date IS 'Date of the session';
COMMENT ON COLUMN class_sessions.start_time IS 'Start time of session';
COMMENT ON COLUMN class_sessions.end_time IS 'End time (must be after start_time)';
COMMENT ON COLUMN class_sessions.location IS 'Optional location override for this session';
COMMENT ON COLUMN class_sessions.topic IS 'Session topic/content (max 200 chars)';
COMMENT ON COLUMN class_sessions.status IS 'SCHEDULED, COMPLETED, CANCELLED, MAKEUP';
COMMENT ON COLUMN class_sessions.attendance_taken IS 'Whether attendance has been recorded';
COMMENT ON COLUMN class_sessions.instance_id IS 'Tenant instance ID for multi-tenant isolation';
