-- V14: Create AI usage log table for rate limiting per tier
-- Tracks daily AI request counts per instance

CREATE TABLE IF NOT EXISTS ai_usage_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    request_count INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_ai_usage_per_day UNIQUE (instance_id, usage_date)
);

-- Index for efficient lookups by instance and date
CREATE INDEX IF NOT EXISTS idx_ai_usage_instance_date ON ai_usage_log (instance_id, usage_date);
