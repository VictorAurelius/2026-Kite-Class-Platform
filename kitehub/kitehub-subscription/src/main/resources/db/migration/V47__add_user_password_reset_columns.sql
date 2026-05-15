-- V47: Add password-reset token columns to users table.
--
-- Closes GAP-548 (Wave 79 Bucket C): backend support for /api/auth/password-reset-request
-- and /api/auth/password-reset-confirm. Gateway-side rate-limit shipped Wave 78
-- (PR #1354) but BE controller was missing.
--
-- Scope:
--   * `password_reset_token`         — opaque random URL-safe token issued on request
--   * `password_reset_token_expires` — TTL boundary (default 1 hour from issue)
--
-- Both columns nullable: NULL means no reset in flight. Tokens are single-use:
-- on successful confirm both columns are cleared in the same transaction that
-- updates the password hash.
--
-- Existing `verification_token` / `token_expires_at` reserved for email-verification
-- flow (per V9 + V30 history) — intentionally NOT reused to keep flows orthogonal.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password_reset_token_expires TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_password_reset_token
    ON users (password_reset_token)
    WHERE password_reset_token IS NOT NULL;
