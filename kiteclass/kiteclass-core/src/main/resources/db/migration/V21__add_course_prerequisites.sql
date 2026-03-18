-- Add course prerequisites join table (IF NOT EXISTS)
CREATE TABLE IF NOT EXISTS course_prerequisites (
    course_id BIGINT NOT NULL,
    prerequisite_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, prerequisite_id),
    CONSTRAINT fk_course_prerequisites_course
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_prerequisites_prerequisite
        FOREIGN KEY (prerequisite_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_prerequisite CHECK (course_id != prerequisite_id)
);
CREATE INDEX IF NOT EXISTS idx_course_prerequisites_course ON course_prerequisites(course_id);
CREATE INDEX IF NOT EXISTS idx_course_prerequisites_prerequisite ON course_prerequisites(prerequisite_id);
