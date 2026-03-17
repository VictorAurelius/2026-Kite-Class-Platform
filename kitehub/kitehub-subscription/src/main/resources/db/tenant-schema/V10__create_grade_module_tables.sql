-- =====================================================
-- V10: Create Grade Module Tables
-- =====================================================
-- Description: Redesign grading system with final grades,
--              grade components, grading scales, and transcripts
-- Author: KiteClass Team
-- Date: 2026-03-03
-- Dependencies: V1 (students, classes tables)
-- =====================================================

-- 1. Rename old grades table to preserve data
ALTER TABLE IF EXISTS grades RENAME TO individual_grades;

-- 2. Create new grades table (final grades per student per class)
CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Foreign keys
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,

    -- Final score calculation
    final_score DECIMAL(5,2),           -- 0-100
    letter_grade VARCHAR(5),            -- A+, A, B+, B, C+, C, D+, D, F
    gpa DECIMAL(3,2),                   -- 0.0-4.0

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    -- IN_PROGRESS, FINALIZED, PASSED, FAILED

    -- Configuration
    pass_threshold DECIMAL(5,2) NOT NULL DEFAULT 50.0,

    -- Teacher feedback
    comments TEXT,

    -- Timestamps
    calculated_at TIMESTAMP WITH TIME ZONE,
    finalized_at TIMESTAMP WITH TIME ZONE,
    finalized_by BIGINT,               -- Teacher ID who finalized

    -- Audit fields
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,

    -- Constraints
    CONSTRAINT fk_grades_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_grades_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT uk_grades_student_class UNIQUE (student_id, class_id),
    CONSTRAINT chk_grades_final_score CHECK (final_score IS NULL OR (final_score >= 0 AND final_score <= 100)),
    CONSTRAINT chk_grades_gpa CHECK (gpa IS NULL OR (gpa >= 0 AND gpa <= 4.0)),
    CONSTRAINT chk_grades_pass_threshold CHECK (pass_threshold >= 0 AND pass_threshold <= 100),
    CONSTRAINT chk_grades_status CHECK (status IN ('IN_PROGRESS', 'FINALIZED', 'PASSED', 'FAILED'))
);

-- 3. Create grade_components table (individual component scores)
CREATE TABLE grade_components (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Foreign key
    grade_id BIGINT NOT NULL,

    -- Component info
    component_type VARCHAR(50) NOT NULL,
    -- ATTENDANCE, ASSIGNMENT, MIDTERM, FINAL, QUIZ, PROJECT, PARTICIPATION
    component_name VARCHAR(255) NOT NULL,
    component_ref_id BIGINT,           -- Reference to attendance_id, assignment_id, etc.

    -- Scores
    score DECIMAL(5,2) NOT NULL,       -- Actual score achieved
    max_score DECIMAL(5,2) NOT NULL,   -- Maximum possible score
    weight_percent DECIMAL(5,2) NOT NULL, -- Weight in final grade (0-100)
    weighted_score DECIMAL(5,2),       -- Calculated: (score/max_score * 100) * (weight_percent/100)

    -- Audit fields
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,

    -- Constraints
    CONSTRAINT fk_grade_components_grade FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE CASCADE,
    CONSTRAINT uk_grade_components_ref UNIQUE (grade_id, component_type, component_ref_id),
    CONSTRAINT chk_grade_components_score CHECK (score >= 0),
    CONSTRAINT chk_grade_components_max_score CHECK (max_score > 0),
    CONSTRAINT chk_grade_components_weight CHECK (weight_percent >= 0 AND weight_percent <= 100),
    CONSTRAINT chk_grade_components_type CHECK (component_type IN ('ATTENDANCE', 'ASSIGNMENT', 'MIDTERM', 'FINAL', 'QUIZ', 'PROJECT', 'PARTICIPATION'))
);

-- 4. Create grading_scales table (configurable grading scale)
CREATE TABLE grading_scales (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Scale info
    scale_name VARCHAR(100) NOT NULL,  -- e.g., "Standard", "Strict", "Custom"
    letter_grade VARCHAR(5) NOT NULL,  -- A+, A, B+, etc.
    min_score DECIMAL(5,2) NOT NULL,   -- Minimum score for this grade (inclusive)
    max_score DECIMAL(5,2) NOT NULL,   -- Maximum score for this grade (inclusive)
    gpa_value DECIMAL(3,2) NOT NULL,   -- GPA value (0.0-4.0)

    -- Configuration
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_passing BOOLEAN NOT NULL DEFAULT TRUE, -- Whether this grade is passing

    -- Audit fields
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,

    -- Constraints
    CONSTRAINT chk_grading_scales_min_score CHECK (min_score >= 0 AND min_score <= 100),
    CONSTRAINT chk_grading_scales_max_score CHECK (max_score >= 0 AND max_score <= 100),
    CONSTRAINT chk_grading_scales_range CHECK (min_score <= max_score),
    CONSTRAINT chk_grading_scales_gpa CHECK (gpa_value >= 0 AND gpa_value <= 4.0)
);

-- 5. Create transcripts table (student academic records)
CREATE TABLE transcripts (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Foreign key
    student_id BIGINT NOT NULL,

    -- Academic period
    semester VARCHAR(50),              -- e.g., "Spring 2026", "Fall 2025"
    academic_year INTEGER,             -- e.g., 2026

    -- Credits
    total_credits DECIMAL(5,2) NOT NULL DEFAULT 0,

    -- GPA
    semester_gpa DECIMAL(3,2),         -- GPA for this semester
    cumulative_gpa DECIMAL(3,2),       -- Overall GPA

    -- Course counts
    total_courses INTEGER NOT NULL DEFAULT 0,
    passed_courses INTEGER NOT NULL DEFAULT 0,
    failed_courses INTEGER NOT NULL DEFAULT 0,

    -- Audit fields
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,

    -- Constraints
    CONSTRAINT fk_transcripts_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT uk_transcripts_student_semester UNIQUE (student_id, semester, academic_year),
    CONSTRAINT chk_transcripts_credits CHECK (total_credits >= 0),
    CONSTRAINT chk_transcripts_gpa CHECK (semester_gpa IS NULL OR (semester_gpa >= 0 AND semester_gpa <= 4.0)),
    CONSTRAINT chk_transcripts_cumulative_gpa CHECK (cumulative_gpa IS NULL OR (cumulative_gpa >= 0 AND cumulative_gpa <= 4.0)),
    CONSTRAINT chk_transcripts_courses CHECK (total_courses >= 0 AND passed_courses >= 0 AND failed_courses >= 0)
);

-- =====================================================
-- Indexes for Performance
-- =====================================================

-- Grades indexes
CREATE INDEX idx_grades_instance_id ON grades(instance_id);
CREATE INDEX idx_grades_student_id ON grades(student_id);
CREATE INDEX idx_grades_class_id ON grades(class_id);
CREATE INDEX idx_grades_status ON grades(status);
CREATE INDEX idx_grades_deleted ON grades(deleted);

-- Grade components indexes
CREATE INDEX idx_grade_components_instance_id ON grade_components(instance_id);
CREATE INDEX idx_grade_components_grade_id ON grade_components(grade_id);
CREATE INDEX idx_grade_components_type ON grade_components(component_type);
CREATE INDEX idx_grade_components_ref_id ON grade_components(component_ref_id);
CREATE INDEX idx_grade_components_deleted ON grade_components(deleted);

-- Grading scales indexes
CREATE INDEX idx_grading_scales_instance_id ON grading_scales(instance_id);
CREATE INDEX idx_grading_scales_scale_name ON grading_scales(scale_name);
CREATE INDEX idx_grading_scales_is_default ON grading_scales(is_default);
CREATE INDEX idx_grading_scales_deleted ON grading_scales(deleted);

-- Transcripts indexes
CREATE INDEX idx_transcripts_instance_id ON transcripts(instance_id);
CREATE INDEX idx_transcripts_student_id ON transcripts(student_id);
CREATE INDEX idx_transcripts_academic_year ON transcripts(academic_year);
CREATE INDEX idx_transcripts_semester ON transcripts(semester);
CREATE INDEX idx_transcripts_deleted ON transcripts(deleted);

-- =====================================================
-- Seed Data: Default Grading Scale
-- =====================================================

-- Insert standard grading scale (A+ to F with GPA mapping)
-- This will be used as default if no custom scale is configured
-- Note: instance_id will be set to a placeholder UUID, actual instances
--       will create their own scales during provisioning

INSERT INTO grading_scales (instance_id, scale_name, letter_grade, min_score, max_score, gpa_value, is_default, is_passing) VALUES
-- A grades
('00000000-0000-0000-0000-000000000000', 'Standard', 'A+', 95.00, 100.00, 4.00, TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'Standard', 'A',  90.00, 94.99,  4.00, TRUE, TRUE),
-- B grades
('00000000-0000-0000-0000-000000000000', 'Standard', 'B+', 85.00, 89.99,  3.30, TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'Standard', 'B',  80.00, 84.99,  3.00, TRUE, TRUE),
-- C grades
('00000000-0000-0000-0000-000000000000', 'Standard', 'C+', 75.00, 79.99,  2.30, TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'Standard', 'C',  70.00, 74.99,  2.00, TRUE, TRUE),
-- D grades
('00000000-0000-0000-0000-000000000000', 'Standard', 'D+', 65.00, 69.99,  1.30, TRUE, TRUE),
('00000000-0000-0000-0000-000000000000', 'Standard', 'D',  60.00, 64.99,  1.00, TRUE, TRUE),
-- F grade (failing)
('00000000-0000-0000-0000-000000000000', 'Standard', 'F',  0.00,  59.99,  0.00, TRUE, FALSE);

-- =====================================================
-- Comments
-- =====================================================

COMMENT ON TABLE grades IS 'Final grades for students in classes';
COMMENT ON TABLE grade_components IS 'Individual grade components (attendance, assignments, exams)';
COMMENT ON TABLE grading_scales IS 'Configurable grading scale (letter grades and GPA mapping)';
COMMENT ON TABLE transcripts IS 'Student academic transcripts per semester';

COMMENT ON COLUMN grades.final_score IS 'Calculated final score (0-100) from weighted components';
COMMENT ON COLUMN grades.letter_grade IS 'Letter grade (A+, A, B+, etc.) mapped from final_score';
COMMENT ON COLUMN grades.gpa IS 'Grade Point Average (0.0-4.0) mapped from letter_grade';
COMMENT ON COLUMN grades.status IS 'Grade status: IN_PROGRESS (calculating), FINALIZED (locked), PASSED, FAILED';

COMMENT ON COLUMN grade_components.component_type IS 'Type of component: ATTENDANCE, ASSIGNMENT, MIDTERM, FINAL, QUIZ, PROJECT, PARTICIPATION';
COMMENT ON COLUMN grade_components.component_ref_id IS 'Reference ID to source record (attendance_id, assignment_id, etc.)';
COMMENT ON COLUMN grade_components.weight_percent IS 'Weight of this component in final grade (0-100%)';
COMMENT ON COLUMN grade_components.weighted_score IS 'Contribution to final score: (score/max_score * 100) * (weight/100)';

COMMENT ON COLUMN grading_scales.is_default IS 'Whether this scale is the default for new instances';
COMMENT ON COLUMN grading_scales.is_passing IS 'Whether this grade is considered passing';
