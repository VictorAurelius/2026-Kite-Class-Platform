-- Add missing columns to bring DB schema in sync with entity classes.
-- V1 created tables with an older/simpler schema; entities have evolved since.
-- All changes are idempotent (ADD COLUMN IF NOT EXISTS / CREATE TABLE IF NOT EXISTS).

-- Teachers: entity expects phone_number, qualification, experience_years
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS phone_number    VARCHAR(20);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS qualification   VARCHAR(200);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS experience_years INTEGER;

-- Courses: entity expects many columns missing from the V1 schema
ALTER TABLE courses ADD COLUMN IF NOT EXISTS teacher_id      BIGINT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS syllabus        TEXT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS objectives      TEXT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS prerequisites   TEXT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS target_audience TEXT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS duration_weeks  INTEGER;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS total_sessions  INTEGER;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS price           DECIMAL(15, 2);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(500);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS level           VARCHAR(50);

-- Classes: entity expects location/schedule/lifecycle columns missing from V1
ALTER TABLE classes ADD COLUMN IF NOT EXISTS description     TEXT;
ALTER TABLE classes ADD COLUMN IF NOT EXISTS schedule        VARCHAR(200);
ALTER TABLE classes ADD COLUMN IF NOT EXISTS location_type   VARCHAR(20);
ALTER TABLE classes ADD COLUMN IF NOT EXISTS location_detail VARCHAR(200);
ALTER TABLE classes ADD COLUMN IF NOT EXISTS current_enrolled INTEGER NOT NULL DEFAULT 0;
ALTER TABLE classes ADD COLUMN IF NOT EXISTS class_code      VARCHAR(20);
ALTER TABLE classes ADD COLUMN IF NOT EXISTS code_expires_at TIMESTAMPTZ;
ALTER TABLE classes ADD COLUMN IF NOT EXISTS started_at      TIMESTAMPTZ;
ALTER TABLE classes ADD COLUMN IF NOT EXISTS completed_at    TIMESTAMPTZ;

-- Classes: missing lifecycle timestamp + update status constraint to match entity enum
ALTER TABLE classes ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE classes ALTER COLUMN code DROP NOT NULL;
ALTER TABLE classes ALTER COLUMN tuition_amount DROP NOT NULL;
ALTER TABLE classes DROP CONSTRAINT IF EXISTS chk_classes_status;
ALTER TABLE classes ADD CONSTRAINT chk_classes_status
    CHECK (status IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

-- Enrollments: entity extends BaseEntity (has deleted) + financial columns
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS deleted           BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS enrollment_date   TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS tuition_amount    DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS discount_percent  DECIMAL(5,2) NOT NULL DEFAULT 0;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS final_amount      DECIMAL(10,2) NOT NULL DEFAULT 0;

-- course_prerequisites: required by @ManyToMany in Course entity
CREATE TABLE IF NOT EXISTS course_prerequisites (
    course_id       BIGINT NOT NULL REFERENCES courses(id),
    prerequisite_id BIGINT NOT NULL REFERENCES courses(id),
    PRIMARY KEY (course_id, prerequisite_id)
);

-- teacher_courses: required by TeacherCourse entity (course-level teacher assignments)
CREATE TABLE IF NOT EXISTS teacher_courses (
    id          BIGSERIAL PRIMARY KEY,
    teacher_id  BIGINT NOT NULL REFERENCES teachers(id),
    course_id   BIGINT NOT NULL REFERENCES courses(id),
    role        VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by BIGINT,
    CONSTRAINT uk_teacher_courses_teacher_course UNIQUE (teacher_id, course_id)
);
CREATE INDEX IF NOT EXISTS idx_teacher_courses_teacher_id ON teacher_courses(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_courses_course_id  ON teacher_courses(course_id);
CREATE INDEX IF NOT EXISTS idx_teacher_courses_role       ON teacher_courses(role);
