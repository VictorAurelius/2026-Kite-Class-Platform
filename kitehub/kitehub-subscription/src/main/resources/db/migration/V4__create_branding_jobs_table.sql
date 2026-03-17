-- V4: Create branding_jobs table for AI logo and asset generation
-- Reference: PR 4.9 - AI Branding Job Queue

CREATE TABLE branding_jobs (
    id UUID PRIMARY KEY,
    instance_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,  -- QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED
    progress INTEGER DEFAULT 0 NOT NULL,  -- 0-100
    current_step VARCHAR(100),  -- Current processing step description
    logo_url VARCHAR(500),  -- Original logo URL uploaded by user
    organization_name VARCHAR(200) NOT NULL,  -- Organization name for branding
    brand_personality VARCHAR(50),  -- PROFESSIONAL, CREATIVE, PLAYFUL, MODERN
    color_scheme VARCHAR(50),  -- PRIMARY_BLUE, WARM_ORANGE, NATURE_GREEN, etc.

    -- Generated Assets (JSON array of URLs)
    assets_generated TEXT,  -- JSON: {"logos": [...], "banners": [...], "heroes": [...]}

    -- AI Analysis Results
    logo_analysis TEXT,  -- JSON: AI analysis of uploaded logo
    theme_extracted VARCHAR(50),  -- Extracted theme from logo analysis

    -- Error Handling
    error_message TEXT,  -- Error details if job failed
    retry_count INTEGER DEFAULT 0 NOT NULL,  -- Number of retry attempts

    -- Timing
    queued_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    -- Audit
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN DEFAULT FALSE NOT NULL,

    CONSTRAINT fk_branding_job_instance FOREIGN KEY (instance_id)
        REFERENCES instances(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_branding_jobs_instance ON branding_jobs(instance_id);
CREATE INDEX idx_branding_jobs_status ON branding_jobs(status);
CREATE INDEX idx_branding_jobs_queued ON branding_jobs(queued_at DESC);
CREATE INDEX idx_branding_jobs_deleted ON branding_jobs(deleted) WHERE deleted = false;

-- Index for finding stale processing jobs (for monitoring)
-- Use simple index on started_at; filter at query time for stale jobs
CREATE INDEX idx_branding_jobs_processing ON branding_jobs(started_at)
    WHERE status = 'PROCESSING';

-- Check constraint for valid status
ALTER TABLE branding_jobs ADD CONSTRAINT chk_branding_job_status
    CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'));

-- Check constraint for valid progress range
ALTER TABLE branding_jobs ADD CONSTRAINT chk_branding_job_progress
    CHECK (progress >= 0 AND progress <= 100);

-- Check constraint for valid retry count
ALTER TABLE branding_jobs ADD CONSTRAINT chk_branding_job_retry
    CHECK (retry_count >= 0 AND retry_count <= 5);

-- Check constraint for valid brand personality
ALTER TABLE branding_jobs ADD CONSTRAINT chk_branding_job_personality
    CHECK (brand_personality IN ('PROFESSIONAL', 'CREATIVE', 'PLAYFUL', 'MODERN', 'ELEGANT', 'BOLD'));

-- Comments for documentation
COMMENT ON TABLE branding_jobs IS 'AI-powered branding job queue for generating logos and assets';
COMMENT ON COLUMN branding_jobs.assets_generated IS 'JSON array of generated asset URLs (logos, banners, heroes, etc.)';
COMMENT ON COLUMN branding_jobs.logo_analysis IS 'JSON containing GPT-4 Vision analysis of uploaded logo';
COMMENT ON COLUMN branding_jobs.theme_extracted IS 'Dominant theme extracted from logo (warm, cool, energetic, calm, etc.)';
COMMENT ON COLUMN branding_jobs.retry_count IS 'Number of retry attempts (max 5 before moving to DLQ)';
COMMENT ON COLUMN branding_jobs.current_step IS 'Current step: analyzing_logo, generating_variants, creating_banners, etc.';
