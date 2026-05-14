-- GAP-515: Account lockout after 5 failed login attempts
-- Adds columns to track failed login attempts + exponential-backoff lockout
-- on the users table. See AuthService.login + .recordFailedLogin.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_failed_login_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS locked_until          TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lockout_count         INTEGER NOT NULL DEFAULT 0;

-- Partial index for fast lookup of currently-locked accounts (admin dashboard / audit)
CREATE INDEX IF NOT EXISTS idx_users_locked_until
    ON users (locked_until)
    WHERE locked_until IS NOT NULL;
