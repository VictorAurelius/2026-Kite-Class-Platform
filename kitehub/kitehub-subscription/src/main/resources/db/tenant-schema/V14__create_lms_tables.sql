-- V14: Create LMS (Learning Management System) Tables
-- Purpose: 3-tier course structure (Course → Module → Lesson), trial lesson access, progress tracking
-- Dependencies: V4 (courses table), V10 (enrollments table)

-- Table 1: course_modules (2nd tier - modules within a course)
CREATE TABLE course_modules (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    order_number INT NOT NULL CHECK (order_number >= 1),

    -- Multi-tenant & audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,

    CONSTRAINT fk_course_modules_course FOREIGN KEY (course_id)
        REFERENCES courses(id) ON DELETE CASCADE
);

-- Unique constraint: order_number must be unique within a course (per tenant)
CREATE UNIQUE INDEX uk_course_modules_course_order
    ON course_modules(course_id, order_number, instance_id)
    WHERE deleted = FALSE;

-- Indexes for performance
CREATE INDEX idx_course_modules_course_id ON course_modules(course_id) WHERE deleted = FALSE;
CREATE INDEX idx_course_modules_instance_id ON course_modules(instance_id) WHERE deleted = FALSE;

COMMENT ON TABLE course_modules IS 'Course modules - 2nd tier in Course → Module → Lesson hierarchy';
COMMENT ON COLUMN course_modules.order_number IS 'Display order within course (must be unique per course)';

-- Table 2: lessons (3rd tier - lessons within a module)
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    module_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    video_url VARCHAR(500),
    is_trial BOOLEAN NOT NULL DEFAULT FALSE,  -- Guest access control flag
    order_number INT NOT NULL CHECK (order_number >= 1),
    estimated_duration INT CHECK (estimated_duration IS NULL OR estimated_duration > 0),  -- Duration in minutes

    -- Multi-tenant & audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,

    CONSTRAINT fk_lessons_module FOREIGN KEY (module_id)
        REFERENCES course_modules(id) ON DELETE CASCADE
);

-- Unique constraint: order_number must be unique within a module (per tenant)
CREATE UNIQUE INDEX uk_lessons_module_order
    ON lessons(module_id, order_number, instance_id)
    WHERE deleted = FALSE;

-- Indexes for performance
CREATE INDEX idx_lessons_module_id ON lessons(module_id) WHERE deleted = FALSE;
CREATE INDEX idx_lessons_is_trial ON lessons(is_trial) WHERE deleted = FALSE AND is_trial = TRUE;
CREATE INDEX idx_lessons_instance_id ON lessons(instance_id) WHERE deleted = FALSE;

COMMENT ON TABLE lessons IS 'Lessons - 3rd tier in Course → Module → Lesson hierarchy';
COMMENT ON COLUMN lessons.is_trial IS 'If true, guest users can access this lesson without enrollment';
COMMENT ON COLUMN lessons.order_number IS 'Display order within module (must be unique per module)';
COMMENT ON COLUMN lessons.estimated_duration IS 'Estimated lesson duration in minutes';

-- Table 3: learning_resources (supplemental materials for lessons)
CREATE TABLE learning_resources (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('VIDEO', 'PDF', 'SLIDE', 'AUDIO', 'LINK', 'CODE', 'OTHER')),
    url VARCHAR(500) NOT NULL,
    title VARCHAR(200) NOT NULL,
    file_size BIGINT CHECK (file_size IS NULL OR file_size > 0),  -- File size in bytes

    -- Multi-tenant & audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,

    CONSTRAINT fk_learning_resources_lesson FOREIGN KEY (lesson_id)
        REFERENCES lessons(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_learning_resources_lesson_id ON learning_resources(lesson_id) WHERE deleted = FALSE;
CREATE INDEX idx_learning_resources_type ON learning_resources(type) WHERE deleted = FALSE;
CREATE INDEX idx_learning_resources_instance_id ON learning_resources(instance_id) WHERE deleted = FALSE;

COMMENT ON TABLE learning_resources IS 'Supplemental learning materials attached to lessons (videos, PDFs, slides, etc.)';
COMMENT ON COLUMN learning_resources.type IS 'Resource type: VIDEO, PDF, SLIDE, AUDIO, LINK, CODE, OTHER';
COMMENT ON COLUMN learning_resources.file_size IS 'File size in bytes (optional)';

-- Table 4: lesson_progress (student progress tracking)
CREATE TABLE lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,  -- User ID (supports future TRIAL_USER tracking)
    lesson_id BIGINT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    progress_percent INT NOT NULL DEFAULT 0 CHECK (progress_percent >= 0 AND progress_percent <= 100),

    -- Multi-tenant & audit
    instance_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,

    CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id)
        REFERENCES lessons(id) ON DELETE CASCADE
);

-- Unique constraint: one progress record per user per lesson (per tenant)
CREATE UNIQUE INDEX uk_lesson_progress_user_lesson
    ON lesson_progress(user_id, lesson_id, instance_id)
    WHERE deleted = FALSE;

-- Indexes for performance
CREATE INDEX idx_lesson_progress_user_id ON lesson_progress(user_id) WHERE deleted = FALSE;
CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress(lesson_id) WHERE deleted = FALSE;
CREATE INDEX idx_lesson_progress_completed ON lesson_progress(completed) WHERE deleted = FALSE AND completed = TRUE;
CREATE INDEX idx_lesson_progress_instance_id ON lesson_progress(instance_id) WHERE deleted = FALSE;

COMMENT ON TABLE lesson_progress IS 'Student progress tracking for lessons';
COMMENT ON COLUMN lesson_progress.user_id IS 'User ID (NOT enrollmentId - supports future trial user tracking)';
COMMENT ON COLUMN lesson_progress.completed IS 'Whether the lesson has been completed';
COMMENT ON COLUMN lesson_progress.completed_at IS 'Timestamp when the lesson was completed';
COMMENT ON COLUMN lesson_progress.progress_percent IS 'Progress percentage (0-100)';
