-- V6: Add pending tier fields to subscriptions table
-- Reference: PR 4.18 - Payment Service Integration
-- Enables storing pending tier changes for downgrades (applied at end of cycle)
-- Enables tracking pending payments for tier upgrades (prorated charges)

-- Add pending_tier column (for downgrades scheduled to apply at end of billing cycle)
ALTER TABLE subscriptions ADD COLUMN pending_tier VARCHAR(20);

-- Add pending_payment_id column (FK to payments table for prorated upgrade payments)
ALTER TABLE subscriptions ADD COLUMN pending_payment_id UUID;

-- Foreign key constraint for pending_payment_id
ALTER TABLE subscriptions ADD CONSTRAINT fk_subscription_pending_payment
    FOREIGN KEY (pending_payment_id) REFERENCES payments(id) ON DELETE SET NULL;

-- Check constraint for valid pending tier values
ALTER TABLE subscriptions ADD CONSTRAINT chk_subscription_pending_tier
    CHECK (pending_tier IN ('FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'));

-- Comments for documentation
COMMENT ON COLUMN subscriptions.pending_tier IS 'Pending tier for downgrade (applied at end of billing cycle)';
COMMENT ON COLUMN subscriptions.pending_payment_id IS 'Payment ID for pending tier upgrade (prorated payment)';
