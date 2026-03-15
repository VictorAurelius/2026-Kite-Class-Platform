-- Add level and category columns to courses table
-- Supports course filtering and search functionality

-- Add level column (e.g., "Beginner", "Intermediate", "Advanced")
ALTER TABLE courses ADD COLUMN level VARCHAR(50);

-- Add category column (e.g., "Math", "Science", "Language", "Technology")
ALTER TABLE courses ADD COLUMN category VARCHAR(100);

-- Create indexes for search performance
CREATE INDEX idx_courses_level ON courses(level);
CREATE INDEX idx_courses_category ON courses(category);

-- Comments
COMMENT ON COLUMN courses.level IS 'Course difficulty level (e.g., Beginner, Intermediate, Advanced)';
COMMENT ON COLUMN courses.category IS 'Course category/subject area (e.g., Math, Science, Language)';
