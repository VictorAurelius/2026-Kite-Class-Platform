-- GAP-536 (Wave 77 Bucket D): POST /tenants (instances) idempotency key support.
--
-- Wave 77 outside-in failure-mode matrix F3 (P0): P1 Solo teacher on 3G taps
-- "Create" twice before the spinner shows → 2 POST /instances requests race →
-- 2 instance rows created → 1 orphan (no admin, no billing). VN mobile-first
-- + slow 3G makes this common (vs developed-country broadband).
--
-- Standard remedy: Stripe-style Idempotency-Key header on mutation endpoints.
-- Server caches request_hash + response for 24h; duplicate key + same body
-- replays the cached response; duplicate key + different body → 422 conflict
-- (Stripe semantics).
--
-- Distinct from V20 migration_idempotency_key (scoped to trial-to-paid upgrade
-- 10-min TTL). This table is generic per-endpoint idempotency for any POST
-- mutation endpoint that opts in.

CREATE TABLE idempotency_keys (
    key VARCHAR(128) PRIMARY KEY,
    endpoint VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_keys_expires_at
    ON idempotency_keys(expires_at);

CREATE INDEX idx_idempotency_keys_endpoint_created
    ON idempotency_keys(endpoint, created_at);

COMMENT ON TABLE idempotency_keys IS
    'GAP-536 Wave 77 — generic per-endpoint idempotency cache. Key supplied by client via Idempotency-Key header. TTL 24h purged by IdempotencyCleanupJob. Stripe semantics: same key + same body → replay; same key + different body → 422.';

COMMENT ON COLUMN idempotency_keys.key IS
    'Client-supplied Idempotency-Key header value. Expected UUID v4 (36 chars) but VARCHAR(128) tolerates other formats.';

COMMENT ON COLUMN idempotency_keys.endpoint IS
    'Logical endpoint identifier (e.g., POST_instances). Allows partial cleanup + per-endpoint replay scoping.';

COMMENT ON COLUMN idempotency_keys.request_hash IS
    'SHA-256 hex of request body (64 chars). Mismatch with cached value → 422 idempotency conflict.';

COMMENT ON COLUMN idempotency_keys.response_status IS
    'HTTP status code from the original response (e.g., 201, 200).';

COMMENT ON COLUMN idempotency_keys.response_body IS
    'Serialized response body (JSON) returned by the original request; replayed verbatim on duplicate.';

COMMENT ON COLUMN idempotency_keys.expires_at IS
    'Row expiry (created_at + 24h). IdempotencyCleanupJob DELETEs rows where expires_at < NOW().';
