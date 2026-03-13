-- Add course prerequisites join table
-- Supports many-to-many prerequisite relationships between courses
-- Includes circular dependency prevention via application logic (DFS algorithm)

CREATE TABLE course_prerequisites (
    course_id BIGINT NOT NULL,
    prerequisite_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, prerequisite_id),
    CONSTRAINT fk_course_prerequisites_course
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_prerequisites_prerequisite
        FOREIGN KEY (prerequisite_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_prerequisite CHECK (course_id != prerequisite_id)
);

-- Indexes for performance
CREATE INDEX idx_course_prerequisites_course ON course_prerequisites(course_id);
CREATE INDEX idx_course_prerequisites_prerequisite ON course_prerequisites(prerequisite_id);

-- Comments
COMMENT ON TABLE course_prerequisites IS 'Many-to-many relationship for course prerequisites';
COMMENT ON COLUMN course_prerequisites.course_id IS 'Course that has prerequisites';
COMMENT ON COLUMN course_prerequisites.prerequisite_id IS 'Course that is required as prerequisite';
COMMENT ON CONSTRAINT chk_no_self_prerequisite ON course_prerequisites IS 'Prevents course from being its own prerequisite';
