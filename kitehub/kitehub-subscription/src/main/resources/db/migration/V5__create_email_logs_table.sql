-- V5: Create email_logs table for email notification tracking
-- Reference: PR 4.12 - Email Service (AWS SES)

CREATE TABLE email_logs (
    id UUID PRIMARY KEY,
    instance_id UUID,  -- Nullable: platform emails (welcome, trial reminders) have no instance
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(200),
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100) NOT NULL,  -- welcome, trial-ending, payment-confirmation, etc.
    template_variables TEXT,  -- JSON: variables used in template

    -- AWS SES Tracking
    message_id VARCHAR(255),  -- AWS SES Message ID
    status VARCHAR(20) NOT NULL,  -- QUEUED, SENT, DELIVERED, BOUNCED, COMPLAINED, FAILED

    -- Timing
    queued_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,  -- When sent to SES
    delivered_at TIMESTAMP,  -- When delivered to recipient (from SES webhook)
    bounced_at TIMESTAMP,  -- If email bounced
    complained_at TIMESTAMP,  -- If recipient marked as spam

    -- Error Handling
    error_message TEXT,  -- Error details if send failed
    bounce_type VARCHAR(50),  -- TRANSIENT, PERMANENT
    bounce_reason TEXT,  -- Detailed bounce reason from SES
    retry_count INTEGER DEFAULT 0 NOT NULL,  -- Number of retry attempts

    -- Audit
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN DEFAULT FALSE NOT NULL,

    CONSTRAINT fk_email_log_instance FOREIGN KEY (instance_id)
        REFERENCES instances(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_email_logs_instance ON email_logs(instance_id);
CREATE INDEX idx_email_logs_recipient ON email_logs(recipient_email);
CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_template ON email_logs(template_name);
CREATE INDEX idx_email_logs_message_id ON email_logs(message_id);
CREATE INDEX idx_email_logs_queued ON email_logs(queued_at DESC);
CREATE INDEX idx_email_logs_deleted ON email_logs(deleted) WHERE deleted = false;

-- Index for finding pending emails (for queue processing)
CREATE INDEX idx_email_logs_pending ON email_logs(queued_at)
    WHERE status = 'QUEUED';

-- Check constraint for valid status
ALTER TABLE email_logs ADD CONSTRAINT chk_email_log_status
    CHECK (status IN ('QUEUED', 'SENT', 'DELIVERED', 'BOUNCED', 'COMPLAINED', 'FAILED'));

-- Check constraint for valid bounce type
ALTER TABLE email_logs ADD CONSTRAINT chk_email_log_bounce_type
    CHECK (bounce_type IS NULL OR bounce_type IN ('TRANSIENT', 'PERMANENT', 'UNDETERMINED'));

-- Check constraint for valid retry count
ALTER TABLE email_logs ADD CONSTRAINT chk_email_log_retry
    CHECK (retry_count >= 0 AND retry_count <= 5);

-- Check constraint for valid email format
ALTER TABLE email_logs ADD CONSTRAINT chk_email_log_recipient_format
    CHECK (recipient_email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Comments for documentation
COMMENT ON TABLE email_logs IS 'Email notification tracking with AWS SES integration';
COMMENT ON COLUMN email_logs.instance_id IS 'NULL for platform-wide emails (welcome, trial reminders)';
COMMENT ON COLUMN email_logs.message_id IS 'AWS SES Message ID for tracking delivery status';
COMMENT ON COLUMN email_logs.template_variables IS 'JSON: variables passed to email template (userName, trialExpiresAt, etc.)';
COMMENT ON COLUMN email_logs.bounce_type IS 'TRANSIENT: temporary failure (retry), PERMANENT: invalid email';
COMMENT ON COLUMN email_logs.complained_at IS 'Timestamp when recipient marked email as spam';
