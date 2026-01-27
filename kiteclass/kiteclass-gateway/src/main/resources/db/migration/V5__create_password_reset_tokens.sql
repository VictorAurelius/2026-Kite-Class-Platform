-- =====================================================
-- KiteClass Gateway - Password Reset Tokens Schema
-- Version: V5
-- Description: Creates password_reset_tokens table for email service
-- =====================================================

-- Create password_reset_tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes on password_reset_tokens table
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_used_at ON password_reset_tokens(used_at);

-- Add comments for documentation
COMMENT ON TABLE password_reset_tokens IS 'Stores password reset tokens for email-based password recovery';
COMMENT ON COLUMN password_reset_tokens.token IS 'Unique UUID token sent to user via email';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Token expiration time (typically 1 hour from creation)';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'When token was used (NULL if not used yet, prevents reuse)';
