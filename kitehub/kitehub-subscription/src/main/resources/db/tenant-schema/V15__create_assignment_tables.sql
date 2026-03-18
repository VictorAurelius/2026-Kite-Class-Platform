-- V15: Create Assignment & Submission Tables
-- Purpose: Assignment lifecycle, late penalties, grading workflow
-- Dependencies: V5 (classes), V3 (students), V4 (teachers)

-- Table 1: assignments (teacher creates assignments for classes)
CREATE TABLE IF NOT EXISTS assignments (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    instructions TEXT,
    due_date TIMESTAMP NOT NULL,
    max_score DECIMAL(5,2) NOT NULL CHECK (max_score > 0),
    weight_percent DECIMAL(5,2) NOT NULL CHECK (weight_percent >= 0 AND weight_percent <= 100),
    allow_late_submission BOOLEAN NOT NULL DEFAULT FALSE,
    late_penalty_percent DECIMAL(5,2) DEFAULT 10.0 CHECK (late_penalty_percent >= 0 AND late_penalty_percent <= 100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
    created_by BIGINT NOT NULL,  -- teacher_id (FK to users.id in Gateway)

    -- Multi-tenant & audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,

    CONSTRAINT fk_assignments_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_assignments_class_id ON assignments(class_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_assignments_status ON assignments(status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_assignments_due_date ON assignments(due_date) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_assignments_instance_id ON assignments(instance_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_assignments_created_by ON assignments(created_by) WHERE deleted = FALSE;

-- Composite index for findPendingGrading query optimization
CREATE INDEX IF NOT EXISTS idx_assignments_status_class_due ON assignments(status, class_id, due_date) WHERE deleted = FALSE;

COMMENT ON TABLE assignments IS 'Teacher-created assignments for classes';
COMMENT ON COLUMN assignments.weight_percent IS 'Assignment weight in final grade (0-100%)';
COMMENT ON COLUMN assignments.late_penalty_percent IS 'Penalty per day for late submissions (default 10%)';
COMMENT ON COLUMN assignments.status IS 'DRAFT (not visible), PUBLISHED (students can submit), CLOSED (no submissions)';

-- Table 2: submissions (student submissions for assignments)
CREATE TABLE IF NOT EXISTS submissions (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    submission_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    content_url VARCHAR(500),  -- URL to uploaded file (S3/Storage Service)
    notes TEXT,  -- Student notes/comments
    score DECIMAL(5,2) CHECK (score IS NULL OR score >= 0),  -- Original score before penalty
    adjusted_score DECIMAL(5,2) CHECK (adjusted_score IS NULL OR adjusted_score >= 0),  -- Final score after late penalty
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'GRADED', 'RETURNED')),
    graded_by BIGINT,  -- teacher_id who graded this submission
    graded_at TIMESTAMP,
    feedback TEXT,  -- Teacher feedback

    -- Multi-tenant & audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,

    CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id)
        REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_submissions_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE
);

-- Unique constraint: one submission per student per assignment (per tenant)
CREATE UNIQUE INDEX IF NOT EXISTS uk_submissions_assignment_student
    ON submissions(assignment_id, student_id, instance_id)
    WHERE deleted = FALSE;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_submissions_assignment_id ON submissions(assignment_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_submissions_student_id ON submissions(student_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_submissions_status ON submissions(status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_submissions_instance_id ON submissions(instance_id) WHERE deleted = FALSE;

-- Composite index for findPendingGrading query
CREATE INDEX IF NOT EXISTS idx_submissions_status_assignment ON submissions(status, assignment_id) WHERE deleted = FALSE AND status = 'PENDING';

COMMENT ON TABLE submissions IS 'Student submissions for assignments';
COMMENT ON COLUMN submissions.score IS 'Original score before late penalty (0 to assignment.max_score)';
COMMENT ON COLUMN submissions.adjusted_score IS 'Final score after late penalty applied';
COMMENT ON COLUMN submissions.status IS 'PENDING (not graded), GRADED (scored), RETURNED (feedback sent to student)';
