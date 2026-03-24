-- V12: Add custom domain fields to instances table
-- Domain status: NONE | PENDING_VERIFY | VERIFIED | FAILED
ALTER TABLE instances ADD COLUMN IF NOT EXISTS domain_verify_token VARCHAR(255);
ALTER TABLE instances ADD COLUMN IF NOT EXISTS domain_verified_at TIMESTAMP;
ALTER TABLE instances ADD COLUMN IF NOT EXISTS domain_status VARCHAR(50) DEFAULT 'NONE';

-- Note: custom_domain column already exists from V1 migration
-- Add index for custom_domain lookups (for uniqueness checks)
CREATE INDEX IF NOT EXISTS idx_instances_custom_domain ON instances (custom_domain)
    WHERE custom_domain IS NOT NULL AND deleted = false;
