-- Create instances table for KiteClass instance management
CREATE TABLE instances (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(50) UNIQUE NOT NULL,
    custom_domain VARCHAR(255),
    organization_name VARCHAR(200) NOT NULL,
    owner_id UUID NOT NULL,
    tier VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    database_url VARCHAR(500) NOT NULL,
    database_username VARCHAR(100) NOT NULL,
    database_password VARCHAR(255) NOT NULL, -- Encrypted with AES-256
    trial_started_at TIMESTAMP,
    trial_expires_at TIMESTAMP,
    subscription_id UUID,
    subscription_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN DEFAULT FALSE NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_instances_subdomain ON instances(subdomain);
CREATE INDEX idx_instances_owner ON instances(owner_id);
CREATE INDEX idx_instances_status ON instances(status);
CREATE INDEX idx_instances_tier ON instances(tier);
CREATE INDEX idx_instances_deleted ON instances(deleted) WHERE deleted = false;

-- Comments for documentation
COMMENT ON TABLE instances IS 'KiteClass instance management table';
COMMENT ON COLUMN instances.subdomain IS 'Subdomain for instance (e.g., customer1 for customer1.kiteclass.com)';
COMMENT ON COLUMN instances.custom_domain IS 'Custom domain (PREMIUM/ENTERPRISE only)';
COMMENT ON COLUMN instances.owner_id IS 'Owner UUID (CENTER_OWNER user)';
COMMENT ON COLUMN instances.tier IS 'Pricing tier: FREE, BASIC, PREMIUM, ENTERPRISE';
COMMENT ON COLUMN instances.status IS 'Instance status: TRIAL, ACTIVE, SUSPENDED, DELETED';
COMMENT ON COLUMN instances.database_password IS 'Encrypted with AES-256-GCM';
COMMENT ON COLUMN instances.trial_started_at IS 'Trial start date';
COMMENT ON COLUMN instances.trial_expires_at IS 'Trial expiration date (14 days from start)';
COMMENT ON COLUMN instances.subscription_id IS 'Reference to subscription record (PR 4.4)';
