-- GAP-098: Add notification preferences to instances
-- Two boolean columns instead of JSONB for simplicity + direct query support.
-- Defaults TRUE to preserve current behavior (all existing users continue receiving notifications).

ALTER TABLE instances
    ADD COLUMN email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN trial_reminders BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN instances.email_notifications IS 'User preference: receive email notifications about instance activity';
COMMENT ON COLUMN instances.trial_reminders IS 'User preference: receive trial expiration reminder emails';
