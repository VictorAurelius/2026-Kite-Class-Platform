-- V17: Add purge tracking columns and indexes (GAP-094)

-- Add purged_at timestamp to instances for tracking when purge occurred
ALTER TABLE instances ADD COLUMN purged_at TIMESTAMP;

-- Partial index for finding purge-eligible instances efficiently
-- Only indexes rows where status = 'DELETED', keeping the index small
CREATE INDEX idx_instances_status_updated_at ON instances(status, updated_at) WHERE status = 'DELETED';
