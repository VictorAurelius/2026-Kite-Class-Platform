-- =========================================================================
-- V29: K-12 Multi-Subject Data Model
-- =========================================================================
-- Context: GAP-054, ADR-001
-- Purpose: HomeroomClass + SubjectSection + Curriculum + SubjectGrade
--          Enable K-12 schools (P5 persona) to have students study 12+ subjects
-- Breaking change: NO (additive; existing Class/Enrollment unchanged)
-- Strangler Fig: coexist with center model via feature flag ENABLE_K12_MODEL
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. homeroom_classes table (Lớp chính)
-- -------------------------------------------------------------------------
CREATE TABLE homeroom_classes (
    id                    BIGSERIAL PRIMARY KEY,
    instance_id           UUID         NOT NULL,
    academic_year_id      BIGINT       NOT NULL REFERENCES academic_years(id),
    grade                 VARCHAR(10)  NOT NULL,
    section               VARCHAR(20)  NOT NULL,
    homeroom_teacher_id   BIGINT,
    capacity              INT          NOT NULL DEFAULT 40,
    current_enrolled      INT          NOT NULL DEFAULT 0,
    description           VARCHAR(500),

    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    version               BIGINT       NOT NULL DEFAULT 0,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_hrc_capacity CHECK (capacity > 0 AND current_enrolled >= 0 AND current_enrolled <= capacity)
);

CREATE UNIQUE INDEX idx_hrc_year_grade_section
    ON homeroom_classes(academic_year_id, grade, section) WHERE deleted = FALSE;
CREATE INDEX idx_hrc_homeroom_teacher ON homeroom_classes(homeroom_teacher_id);
CREATE INDEX idx_hrc_instance_id ON homeroom_classes(instance_id);
CREATE INDEX idx_hrc_deleted ON homeroom_classes(deleted);

-- -------------------------------------------------------------------------
-- 2. subject_sections table (Lớp bộ môn)
-- -------------------------------------------------------------------------
CREATE TABLE subject_sections (
    id                    BIGSERIAL PRIMARY KEY,
    instance_id           UUID         NOT NULL,
    homeroom_class_id     BIGINT       NOT NULL REFERENCES homeroom_classes(id),
    course_id             BIGINT       NOT NULL,
    teacher_id            BIGINT,
    schedule              VARCHAR(200),
    weekly_hours          INT,

    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    version               BIGINT       NOT NULL DEFAULT 0,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_ssec_weekly_hours CHECK (weekly_hours IS NULL OR weekly_hours > 0)
);

CREATE UNIQUE INDEX idx_ssec_homeroom_course
    ON subject_sections(homeroom_class_id, course_id) WHERE deleted = FALSE;
CREATE INDEX idx_ssec_teacher ON subject_sections(teacher_id);
CREATE INDEX idx_ssec_instance_id ON subject_sections(instance_id);
CREATE INDEX idx_ssec_deleted ON subject_sections(deleted);

-- -------------------------------------------------------------------------
-- 3. curricula table (Chương trình học)
-- -------------------------------------------------------------------------
CREATE TABLE curricula (
    id             BIGSERIAL PRIMARY KEY,
    instance_id    UUID         NOT NULL,
    grade          VARCHAR(10)  NOT NULL,
    name           VARCHAR(100),
    subjects       JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX idx_curriculum_grade
    ON curricula(instance_id, grade) WHERE deleted = FALSE;
CREATE INDEX idx_curriculum_deleted ON curricula(deleted);

-- -------------------------------------------------------------------------
-- 4. subject_grades table (Điểm môn học)
-- -------------------------------------------------------------------------
CREATE TABLE subject_grades (
    id                    BIGSERIAL PRIMARY KEY,
    instance_id           UUID         NOT NULL,
    student_id            BIGINT       NOT NULL,
    subject_section_id    BIGINT       NOT NULL REFERENCES subject_sections(id),
    semester_id           BIGINT       NOT NULL REFERENCES semesters(id),
    regular_score         DECIMAL(4,2),
    midterm_score         DECIMAL(4,2),
    final_score           DECIMAL(4,2),
    average               DECIMAL(4,2),
    letter_grade          VARCHAR(20),
    notes                 VARCHAR(500),

    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    version               BIGINT       NOT NULL DEFAULT 0,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_sg_scores CHECK (
        (regular_score IS NULL OR (regular_score >= 0 AND regular_score <= 10)) AND
        (midterm_score IS NULL OR (midterm_score >= 0 AND midterm_score <= 10)) AND
        (final_score IS NULL OR (final_score >= 0 AND final_score <= 10)) AND
        (average IS NULL OR (average >= 0 AND average <= 10))
    )
);

CREATE UNIQUE INDEX idx_sg_student_section_semester
    ON subject_grades(student_id, subject_section_id, semester_id) WHERE deleted = FALSE;
CREATE INDEX idx_sg_instance_id ON subject_grades(instance_id);
CREATE INDEX idx_sg_deleted ON subject_grades(deleted);
