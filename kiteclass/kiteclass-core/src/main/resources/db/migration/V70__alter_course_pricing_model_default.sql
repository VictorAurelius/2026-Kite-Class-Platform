-- V70: Set courses.pricing_model column default to PER_HOUR
-- ADR-035 mandate: VN TT Anh ngữ market bán theo giờ là chính.
-- V67 backfilled existing rows with COURSE_PACKAGE (correct for backward compat).
-- This migration sets the DB column default for NEW inserts going forward.
ALTER TABLE courses ALTER COLUMN pricing_model SET DEFAULT 'PER_HOUR';
