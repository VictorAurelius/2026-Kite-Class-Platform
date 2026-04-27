-- V11: Create email_sent_log table for email idempotency
-- Prevents duplicate emails being sent to the same recipient on the same day
--
-- NOTE: Postgres does NOT support expressions inside CONSTRAINT UNIQUE; only column
-- names. The original V11 used `UNIQUE (..., (sent_at::date))` which fails with
-- SQL state 42601. Function-based unique enforcement is provided via a separate
-- CREATE UNIQUE INDEX below — same effective constraint, valid syntax.
-- GAP-242 (2026-04-27).

CREATE TABLE IF NOT EXISTS email_sent_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID,
    email_type VARCHAR(100) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Functional unique index — enforces "at most one email of a given type per
-- (instance, recipient, calendar-day)" without needing a generated column.
CREATE UNIQUE INDEX IF NOT EXISTS uq_email_per_day
    ON email_sent_log (instance_id, email_type, recipient, ((sent_at)::date));

CREATE INDEX IF NOT EXISTS idx_email_sent_log_instance ON email_sent_log(instance_id);
CREATE INDEX IF NOT EXISTS idx_email_sent_log_type ON email_sent_log(email_type, sent_at);
