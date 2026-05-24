-- =====================================================================
-- Wave beta-readiness-2 Bucket A — Shared idempotency for POST mutations (GAP-730)
-- =====================================================================
-- Creates table `idempotency_keys` used by `IdempotencyService` to dedupe
-- POST requests carrying an `Idempotency-Key` header. Covers SIGNUP,
-- ENROLLMENT, BETA_REQUEST, PAYMENT scopes — same row layout regardless
-- of scope so a single service + repository serves all callers.
--
-- Pattern reuses Wave 105 Bucket D `payment_idempotency_keys` precedent
-- (per `kiteclass-core/.../parent/payment/PaymentIdempotencyService.java`)
-- but generalized to cross-domain scope. Per `pre-handoff-self-test-
-- completeness.md` §2.6 (d) "Same key replayed → no double-charge; row in
-- payment_attempts table with idempotency state" — same guarantee extended
-- to enrollment + signup + beta-request to prevent duplicate creation.
--
-- Per `audit-to-gap-pipeline.md` Step 2 state-check:
--   * V65 is current head (Wave beta-readiness-1 Bucket B enrollment capacity);
--     V66 = next sequential per Flyway convention.
--
-- Composite primary key (tenant_id, idempotency_key, scope) lets the same
-- caller-supplied UUID be reused safely across different scopes (signup vs
-- enrollment) without collision; same scope replay returns the cached
-- response. Cross-tenant collision prevented by tenant_id.
-- =====================================================================

CREATE TABLE idempotency_keys (
    tenant_id        UUID         NOT NULL,
    idempotency_key  VARCHAR(255) NOT NULL,
    scope            VARCHAR(32)  NOT NULL,

    -- Caller identity (nullable for anonymous flows like SIGNUP).
    user_id          UUID,

    -- SHA-256 hash of normalized request body so future request with same key
    -- but DIFFERENT body can be detected as a client bug (replay must mean
    -- "same request again", not "different request, reused key").
    request_hash     VARCHAR(64)  NOT NULL,

    -- Cached HTTP status the first-write returned (201 / 200 / 4xx).
    response_status  INT          NOT NULL,

    -- Cached response body (JSON string) replayed on hit. TEXT covers
    -- enrollment/signup payload sizes; large payment payloads truncate
    -- to the response shape callers actually need.
    response_body    TEXT,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    PRIMARY KEY (tenant_id, idempotency_key, scope)
);

CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys (created_at);

COMMENT ON TABLE idempotency_keys IS
    'Shared idempotency for POST mutations per GAP-730 (Wave beta-readiness-2 Bucket A). '
    'Scopes: SIGNUP | ENROLLMENT | BETA_REQUEST | PAYMENT. '
    'See pre-handoff-self-test-completeness.md §2.6 (d).';

COMMENT ON COLUMN idempotency_keys.scope IS
    'SIGNUP | ENROLLMENT | BETA_REQUEST | PAYMENT — keeps same client-supplied '
    'UUID disjoint across domains; same scope replay returns cached response.';

COMMENT ON COLUMN idempotency_keys.request_hash IS
    'SHA-256 of normalized request body. Future request with same key but '
    'different hash = client bug (reused key for different request).';
