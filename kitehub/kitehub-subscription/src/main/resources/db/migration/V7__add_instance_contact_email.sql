-- V7: Add contact_email to instances table
-- Reference: PR 4.19 - Email Service Integration
-- Enables sending email notifications to instance owners

-- Add contact_email column for email notifications
ALTER TABLE instances ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);

-- Comment for documentation
COMMENT ON COLUMN instances.contact_email IS 'Contact email for instance owner (used for notifications)';
