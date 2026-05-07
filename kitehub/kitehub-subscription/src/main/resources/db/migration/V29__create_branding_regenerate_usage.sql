-- Wave 34 Bucket A — branding regenerate usage (sub-GAP-272d)
--
-- Tracks regenerate quota consumption per user per daily window. Reset window
-- = UTC day boundary; row created lazily on first regenerate of the day.
--
-- Tier caps live in code (kitehub.regenerate.* config keys with FREE=3, PRO=10,
-- PREMIUM=30, ENTERPRISE=-1). This table only stores the counter + window so
-- the service can compute "remaining" client-side.
--
-- Idempotency-Key column is for POST /regenerate idempotency (10-min window
-- per api-contract.md). One row per (user_id, idempotency_key) pair guarantees
-- replays return the same job_id.
CREATE TABLE branding_regenerate_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    instance_id UUID,
    job_id UUID,
    idempotency_key VARCHAR(100),
    tier VARCHAR(20) NOT NULL,
    used_count INTEGER NOT NULL DEFAULT 0,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    last_regenerate_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_brand_regen_user_window
    ON branding_regenerate_usage (user_id, window_start);

CREATE INDEX idx_brand_regen_idempotency
    ON branding_regenerate_usage (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_brand_regen_window_end
    ON branding_regenerate_usage (window_end);

COMMENT ON TABLE branding_regenerate_usage IS
    'Wave 34 — regenerate-quota counter per user per daily window. Sub-GAP-272d.';
COMMENT ON COLUMN branding_regenerate_usage.idempotency_key IS
    'Idempotency-Key header from POST /regenerate; replays within window return same job_id.';
COMMENT ON COLUMN branding_regenerate_usage.window_start IS
    'UTC midnight of the day this counter applies to.';
