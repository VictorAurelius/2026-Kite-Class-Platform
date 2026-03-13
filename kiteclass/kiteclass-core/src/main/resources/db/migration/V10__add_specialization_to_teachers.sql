-- Migration: Add specialization field to teachers table
-- Version: V10
-- Date: 2026-03-13
-- Description: Add specialization field to track teacher's subject expertise

ALTER TABLE teachers
ADD COLUMN specialization VARCHAR(50);

-- Index for filtering/searching by specialization
CREATE INDEX idx_teachers_specialization ON teachers(specialization);

-- Note: Existing teachers will have NULL specialization, update manually or via API
