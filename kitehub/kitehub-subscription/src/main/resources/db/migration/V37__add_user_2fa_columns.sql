-- GAP-516: TOTP 2FA enrollment + recovery codes for PLATFORM_ADMIN (Wave 72b Bucket A)
-- Adds 2FA columns to users + creates recovery_codes table.
-- See documents/01-business/kitehub/auth/{rules.md,api-contract.md} (Wave 72b Bucket 0 Foundation).

-- 2FA columns on users
-- totp_secret_encrypted: AES-encrypted base32 TOTP secret (32-byte encryption key
--   sourced from config `kitehub.auth.totp.encryption-key` — Phase 1.5+ moves to KMS).
-- totp_enrolled_at: timestamp the user completed enrollment (enroll-confirm success).
-- totp_required: true when this user MUST enroll before issuing access tokens
--   (PLATFORM_ADMIN seeded TRUE; OWNER FALSE for Phase 1 BETA).
-- recovery_codes_hashes: deprecated holdover slot — actual recovery codes are now
--   stored in the new recovery_codes table below for richer audit columns; this
--   column is reserved to avoid breaking any future migration that wants to inline
--   a small array of hashes. Kept nullable; not consumed by current code.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS totp_secret_encrypted  VARCHAR(256),
    ADD COLUMN IF NOT EXISTS totp_enrolled_at       TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS totp_required          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS recovery_codes_hashes  TEXT;

-- Partial index for fast lookup of users still pending enrollment (admin dashboard).
CREATE INDEX IF NOT EXISTS idx_users_totp_pending
    ON users (totp_required)
    WHERE totp_required = TRUE AND totp_enrolled_at IS NULL;

-- Recovery codes (one row per single-use code, bcrypt-hashed).
-- We store ten rows per user at enrollment; consuming a code sets used_at.
-- On regenerate, all rows for the user get used_at=now() and ten new rows
-- inserted in the same transaction.
CREATE TABLE IF NOT EXISTS recovery_codes (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash   VARCHAR(72)  NOT NULL,   -- bcrypt hash (60 chars + bcrypt $2a/$2b prefix)
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_recovery_codes_user
    ON recovery_codes (user_id, used_at);

-- Seed: existing PLATFORM_ADMIN users get totp_required=TRUE so first login
-- after this migration forces enrollment. (Idempotent — safe to re-run.)
UPDATE users SET totp_required = TRUE WHERE role = 'PLATFORM_ADMIN' AND totp_required = FALSE;
