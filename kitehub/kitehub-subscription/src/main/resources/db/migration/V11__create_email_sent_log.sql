-- V11: Create email_sent_log table for email idempotency
-- Prevents duplicate emails being sent to the same recipient on the same day

CREATE TABLE IF NOT EXISTS email_sent_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID,
    email_type VARCHAR(100) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_email_per_day UNIQUE (instance_id, email_type, recipient, (sent_at::date))
);

CREATE INDEX idx_email_sent_log_instance ON email_sent_log(instance_id);
CREATE INDEX idx_email_sent_log_type ON email_sent_log(email_type, sent_at);
