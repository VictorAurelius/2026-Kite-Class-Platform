-- V11: Create Attendance Table
-- Creates attendance table to manage student attendance tracking for class sessions

-- Attendance status enum
-- PRESENT: Student attended the session
-- ABSENT: Student was absent without excuse
-- LATE: Student arrived late
-- EXCUSED: Student was absent with valid excuse
-- MAKEUP: Makeup session attendance

CREATE TABLE attendance (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    enrollment_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,

    -- Attendance Data
    status VARCHAR(20) NOT NULL,
    marked_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    marked_by BIGINT, -- Teacher ID who marked attendance
    notes TEXT,
    points_awarded INTEGER DEFAULT 0, -- Calculated points for this record

    -- Multi-tenant Support
    instance_id UUID NOT NULL,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Soft Delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Optimistic Locking
    version BIGINT NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES enrollments(id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_session FOREIGN KEY (session_id)
        REFERENCES class_sessions(id) ON DELETE RESTRICT,
    CONSTRAINT chk_attendance_status CHECK (status IN (
        'PRESENT', 'ABSENT', 'LATE', 'EXCUSED', 'MAKEUP'
    )),

    -- Unique constraint: Cannot mark attendance twice for same enrollment+session
    CONSTRAINT uk_attendance_enrollment_session
        UNIQUE (enrollment_id, session_id, instance_id, deleted)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_attendance_enrollment_id ON attendance(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_attendance_session_id ON attendance(session_id);
CREATE INDEX IF NOT EXISTS idx_attendance_status ON attendance(status);
CREATE INDEX IF NOT EXISTS idx_attendance_instance_id ON attendance(instance_id);
CREATE INDEX IF NOT EXISTS idx_attendance_deleted ON attendance(deleted);
CREATE INDEX IF NOT EXISTS idx_attendance_marked_date ON attendance(marked_date);
CREATE INDEX IF NOT EXISTS idx_attendance_marked_by ON attendance(marked_by);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_attendance_enrollment_status ON attendance(enrollment_id, status)
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_attendance_session_status ON attendance(session_id, status)
    WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE attendance IS 'Student attendance records for class sessions';
COMMENT ON COLUMN attendance.enrollment_id IS 'Foreign key to enrollments table';
COMMENT ON COLUMN attendance.session_id IS 'Foreign key to class_sessions table';
COMMENT ON COLUMN attendance.status IS 'Attendance status: PRESENT, ABSENT, LATE, EXCUSED, MAKEUP';
COMMENT ON COLUMN attendance.marked_date IS 'Timestamp when attendance was marked';
COMMENT ON COLUMN attendance.marked_by IS 'Teacher ID who marked the attendance';
COMMENT ON COLUMN attendance.notes IS 'Additional notes about the attendance record';
COMMENT ON COLUMN attendance.points_awarded IS 'Points awarded/deducted for this attendance record';
