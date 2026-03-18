-- Wrapped with IF NOT EXISTS checks
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='courses' AND column_name='level') THEN
    ALTER TABLE courses ADD COLUMN level VARCHAR(50);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='courses' AND column_name='category') THEN
    ALTER TABLE courses ADD COLUMN category VARCHAR(100);
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_courses_level ON courses(level);
CREATE INDEX IF NOT EXISTS idx_courses_category ON courses(category);
