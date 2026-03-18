-- Migration: Add specialization field to teachers table
-- Version: V10
-- Date: 2026-03-13
-- Description: Add specialization field to track teacher's subject expertise

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='teachers' AND column_name='specialization') THEN
    ALTER TABLE teachers ADD COLUMN specialization VARCHAR(50);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_teachers_specialization ON teachers(specialization);
