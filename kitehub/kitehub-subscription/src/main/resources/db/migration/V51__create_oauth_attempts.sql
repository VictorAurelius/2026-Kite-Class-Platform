-- Wave 86 GAP-582 — OAuth callback idempotency table với UNIQUE state_token
--
-- Defensive scaffolding cho upcoming P2 Owner OAuth signup flow (Google +
-- Microsoft SSO). 5 P2 owners concurrent accept invite via OAuth có thể replay
-- state_token nếu identity provider transient 503 → backend creates duplicate
-- user account → cross-tenant orphan record. UNIQUE constraint trên state_token
-- prevents this at the DB layer; controller catches DataIntegrityViolationException
-- và surfaces 409.
--
-- Reference:
-- - documents/04-quality/gaps/GAP-582-oauth-callback-idempotency-state-token-unique.md
-- - .claude/rules/pre-handoff-self-test-completeness.md §2.7 (multi-tenant tenant-switch)
-- - Wave 86 plan §3 Bucket G AC G-AC2

CREATE TABLE IF NOT EXISTS oauth_attempts (
    id              BIGSERIAL PRIMARY KEY,
    state_token     VARCHAR(255) NOT NULL,
    provider        VARCHAR(50)  NOT NULL,
    tenant_id       BIGINT       NULL,
    user_email      VARCHAR(255) NULL,
    initiated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP    NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_code      VARCHAR(50)  NULL,
    CONSTRAINT uk_oauth_attempts_state_token UNIQUE (state_token)
);

COMMENT ON TABLE oauth_attempts IS
    'OAuth signup/login state tracking — state_token UNIQUE enforces idempotency. Wave 86 GAP-582.';

COMMENT ON COLUMN oauth_attempts.state_token IS
    'OAuth 2.0 state parameter (CSRF + replay defense). UNIQUE constraint catches duplicate callbacks → 409 at controller.';

COMMENT ON COLUMN oauth_attempts.provider IS
    'Identity provider: google | microsoft | apple | github.';

COMMENT ON COLUMN oauth_attempts.status IS
    'Lifecycle: PENDING (initiated) | SUCCEEDED (completed_at set) | FAILED (error_code set).';

-- Index for cleanup job: stale PENDING rows older than 1h.
CREATE INDEX IF NOT EXISTS idx_oauth_attempts_status_initiated
    ON oauth_attempts (status, initiated_at);
