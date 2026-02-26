-- V8: Create Enrollment Tables
-- Creates enrollments table to manage student enrollment in classes

-- Enrollment status enum
-- ACTIVE: Student is currently enrolled
-- PENDING_PAYMENT: Enrollment pending payment confirmation
-- COMPLETED: Enrollment completed (class finished)
-- WITHDRAWN: Student withdrew from class
-- CANCELLED: Enrollment cancelled by admin/system

CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Keys
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,

    -- Enrollment Details
    enrollment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Financial Details
    tuition_amount DECIMAL(10, 2) NOT NULL,
    discount_percent DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    final_amount DECIMAL(10, 2) NOT NULL,

    -- Notes
    notes TEXT,

    -- Multi-tenant Support
    instance_id UUID NOT NULL,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Soft Delete
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE RESTRICT,
    CONSTRAINT fk_enrollments_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE RESTRICT,
    CONSTRAINT chk_enrollment_status CHECK (status IN (
        'ACTIVE', 'PENDING_PAYMENT', 'COMPLETED', 'WITHDRAWN', 'CANCELLED'
    )),
    CONSTRAINT chk_enrollment_tuition_positive CHECK (tuition_amount >= 0),
    CONSTRAINT chk_enrollment_discount_range CHECK (
        discount_percent >= 0 AND discount_percent <= 100
    ),
    CONSTRAINT chk_enrollment_final_positive CHECK (final_amount >= 0),

    -- Unique constraint: One student cannot enroll in same class twice (per tenant)
    CONSTRAINT uk_enrollments_student_class_instance
        UNIQUE (student_id, class_id, instance_id, deleted)
);

-- Indexes for performance
CREATE INDEX idx_enrollments_student_id ON enrollments(student_id);
CREATE INDEX idx_enrollments_class_id ON enrollments(class_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
CREATE INDEX idx_enrollments_instance_id ON enrollments(instance_id);
CREATE INDEX idx_enrollments_deleted ON enrollments(deleted);
CREATE INDEX idx_enrollments_enrollment_date ON enrollments(enrollment_date);

-- Composite index for common queries
CREATE INDEX idx_enrollments_class_status ON enrollments(class_id, status)
    WHERE deleted = FALSE;
CREATE INDEX idx_enrollments_student_status ON enrollments(student_id, status)
    WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE enrollments IS 'Student enrollments in classes';
COMMENT ON COLUMN enrollments.student_id IS 'Foreign key to students table';
COMMENT ON COLUMN enrollments.class_id IS 'Foreign key to classes table';
COMMENT ON COLUMN enrollments.enrollment_date IS 'Date when student enrolled';
COMMENT ON COLUMN enrollments.status IS 'Enrollment status: ACTIVE, PENDING_PAYMENT, COMPLETED, WITHDRAWN, CANCELLED';
COMMENT ON COLUMN enrollments.tuition_amount IS 'Original tuition amount for the class';
COMMENT ON COLUMN enrollments.discount_percent IS 'Discount percentage (0-100)';
COMMENT ON COLUMN enrollments.final_amount IS 'Final amount after discount (tuition_amount * (1 - discount_percent/100))';
COMMENT ON COLUMN enrollments.notes IS 'Additional notes about the enrollment';
