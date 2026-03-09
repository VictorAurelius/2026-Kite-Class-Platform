-- V2: Create subscriptions table for managing paid subscriptions
-- Reference: PR 4.4 - Subscription Management Service

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    instance_id UUID NOT NULL,
    tier VARCHAR(20) NOT NULL,  -- FREE, BASIC, PREMIUM, ENTERPRISE
    billing_cycle VARCHAR(20) NOT NULL,  -- MONTHLY, ANNUALLY
    price_vnd BIGINT NOT NULL,  -- Price in Vietnamese Dong
    status VARCHAR(20) NOT NULL,  -- ACTIVE, SUSPENDED, CANCELLED, EXPIRED
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    auto_renew BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN DEFAULT FALSE NOT NULL,

    CONSTRAINT fk_subscription_instance FOREIGN KEY (instance_id)
        REFERENCES instances(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_subscriptions_instance ON subscriptions(instance_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_expires ON subscriptions(expires_at);
CREATE INDEX idx_subscriptions_deleted ON subscriptions(deleted) WHERE deleted = false;

-- Check constraint for valid tier values
ALTER TABLE subscriptions ADD CONSTRAINT chk_subscription_tier
    CHECK (tier IN ('FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'));

-- Check constraint for valid billing cycle
ALTER TABLE subscriptions ADD CONSTRAINT chk_subscription_billing_cycle
    CHECK (billing_cycle IN ('MONTHLY', 'ANNUALLY'));

-- Check constraint for valid status
ALTER TABLE subscriptions ADD CONSTRAINT chk_subscription_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'EXPIRED'));

-- Check constraint for positive price
ALTER TABLE subscriptions ADD CONSTRAINT chk_subscription_price
    CHECK (price_vnd >= 0);

-- Comments for documentation
COMMENT ON TABLE subscriptions IS 'Paid subscriptions for KiteClass instances';
COMMENT ON COLUMN subscriptions.price_vnd IS 'Subscription price in Vietnamese Dong (VND)';
COMMENT ON COLUMN subscriptions.billing_cycle IS 'Payment frequency: MONTHLY or ANNUALLY';
COMMENT ON COLUMN subscriptions.auto_renew IS 'Automatically renew subscription on expiration';
