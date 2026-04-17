-- V8: Add language column to branding_jobs table
-- Required by BrandingJob entity for content generation language preference

ALTER TABLE branding_jobs ADD COLUMN IF NOT EXISTS language VARCHAR(10) DEFAULT 'vi' NOT NULL;

COMMENT ON COLUMN branding_jobs.language IS 'Content generation language code (vi, en)';
