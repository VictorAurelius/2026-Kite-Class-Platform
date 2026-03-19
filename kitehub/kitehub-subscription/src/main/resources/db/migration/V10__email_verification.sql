-- Email verification support
-- Users must verify email before instance DB is provisioned

ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN token_expires_at TIMESTAMP;

CREATE INDEX idx_users_verification_token ON users(verification_token) WHERE verification_token IS NOT NULL;

-- Mark existing users as verified (seeded admin + demo accounts)
UPDATE users SET email_verified = TRUE WHERE email_verified = FALSE;
