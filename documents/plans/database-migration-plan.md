# DATABASE MIGRATION PLAN

**Version:** 1.0
**Created:** 2026-01-30
**Database:** PostgreSQL 15
**Migration Tool:** Flyway

**Tham chiếu:**
- `backend-implementation-plan-v2.md`
- `kitehub-implementation-plan.md`
- `system-architecture-v3-final.md` (PHẦN 6B-6F)

---

## MỤC LỤC

1. [Migration Strategy](#migration-strategy)
2. [V1: Instance Configuration (Feature Detection)](#v1-instance-configuration)
3. [V2: Payment Orders (VietQR)](#v2-payment-orders)
4. [V3: Instance Bank Accounts](#v3-instance-bank-accounts)
5. [V4: Guest Analytics](#v4-guest-analytics)
6. [V5: AI Branding Jobs](#v5-ai-branding-jobs)
7. [V6: Subscriptions](#v6-subscriptions)
8. [V7: Storage Tracking](#v7-storage-tracking)
9. [Rollback Strategy](#rollback-strategy)

---

# MIGRATION STRATEGY

## Flyway Configuration

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    validate-on-migrate: true
    out-of-order: false
```

## File Naming Convention

```
src/main/resources/db/migration/
├── V1__add_instance_configs.sql
├── V2__add_payment_orders.sql
├── V3__add_instance_bank_accounts.sql
├── V4__add_guest_analytics.sql
├── V5__add_branding_jobs.sql
├── V6__add_subscriptions.sql
├── V7__add_storage_tracking.sql
└── V8__add_indexes.sql
```

## Multi-Database Strategy

```
KiteHub Database (Platform):
- instances table
- subscriptions table
- payment_orders table
- branding_jobs table

Instance Database (Per-tenant):
- instance_configs table
- users table
- courses table
- students table
- attendance_records table
- grades table
- lessons table
- ... (all instance-specific data)
```

---

# V1: INSTANCE CONFIGURATION

**File:** `V1__add_instance_configs.sql`
**Purpose:** Feature detection & tier management

```sql
-- ============================================================================
-- V1: Instance Configuration Tables
-- ============================================================================

-- Instance Config table
CREATE TABLE instance_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL UNIQUE,
    tier VARCHAR(20) NOT NULL CHECK (tier IN ('BASIC', 'STANDARD', 'PREMIUM')),
    features JSONB NOT NULL DEFAULT '{}'::jsonb,
    limitations JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_instance_configs_instance ON instance_configs(instance_id);
CREATE INDEX idx_instance_configs_tier ON instance_configs(tier);
CREATE INDEX idx_instance_configs_created ON instance_configs(created_at DESC);

-- Comments
COMMENT ON TABLE instance_configs IS 'Instance configuration for feature detection';
COMMENT ON COLUMN instance_configs.tier IS 'Pricing tier: BASIC, STANDARD, PREMIUM';
COMMENT ON COLUMN instance_configs.features IS 'Feature flags: {engagement: true, media: false, premium: false}';
COMMENT ON COLUMN instance_configs.limitations IS 'Limitations: {maxStudents: 50, maxCourses: 10}';

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_instance_configs_updated_at
    BEFORE UPDATE ON instance_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default config for existing instances (if any)
-- INSERT INTO instance_configs (instance_id, tier, features, limitations)
-- SELECT id, 'BASIC',
--        '{"engagement": false, "media": false, "premium": false}'::jsonb,
--        '{"maxStudents": 50, "maxCourses": 10}'::jsonb
-- FROM instances
-- ON CONFLICT (instance_id) DO NOTHING;
```

---

# V2: PAYMENT ORDERS

**File:** `V2__add_payment_orders.sql`
**Purpose:** VietQR payment tracking

```sql
-- ============================================================================
-- V2: Payment Orders (VietQR)
-- ============================================================================

-- Payment type enum
CREATE TYPE payment_type AS ENUM ('SUBSCRIPTION', 'ENROLLMENT');

-- Payment status enum
CREATE TYPE payment_status AS ENUM ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED');

-- Payment Orders table
CREATE TABLE payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id VARCHAR(50) NOT NULL UNIQUE,
    type payment_type NOT NULL,
    user_id UUID NOT NULL,
    instance_id UUID,
    amount BIGINT NOT NULL CHECK (amount > 0),
    tier VARCHAR(20) CHECK (tier IN ('BASIC', 'STANDARD', 'PREMIUM')),
    status payment_status NOT NULL DEFAULT 'PENDING',
    qr_image_url VARCHAR(500),
    payment_content VARCHAR(200) NOT NULL,
    transaction_reference VARCHAR(100),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_payment_orders_order_id ON payment_orders(order_id);
CREATE INDEX idx_payment_orders_user ON payment_orders(user_id);
CREATE INDEX idx_payment_orders_instance ON payment_orders(instance_id);
CREATE INDEX idx_payment_orders_status ON payment_orders(status);
CREATE INDEX idx_payment_orders_created ON payment_orders(created_at DESC);
CREATE INDEX idx_payment_orders_expires ON payment_orders(expires_at) WHERE status = 'PENDING';

-- Comments
COMMENT ON TABLE payment_orders IS 'VietQR payment orders';
COMMENT ON COLUMN payment_orders.type IS 'SUBSCRIPTION (KiteHub) or ENROLLMENT (Instance)';
COMMENT ON COLUMN payment_orders.amount IS 'Amount in VND';
COMMENT ON COLUMN payment_orders.payment_content IS 'VietQR content: KITEHUB {orderId} {email}';
COMMENT ON COLUMN payment_orders.expires_at IS '24-hour expiry for payment';

-- Trigger
CREATE TRIGGER update_payment_orders_updated_at
    BEFORE UPDATE ON payment_orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Auto-expire old pending orders (cron job will use this)
-- SELECT * FROM payment_orders
-- WHERE status = 'PENDING' AND expires_at < NOW();
```

---

# V3: INSTANCE BANK ACCOUNTS

**File:** `V3__add_instance_bank_accounts.sql`
**Purpose:** Owner-configurable bank accounts

```sql
-- ============================================================================
-- V3: Instance Bank Accounts (Owner Configurable)
-- ============================================================================

-- Add bank account columns to instances table
-- Assuming instances table exists in KiteHub database

-- If instances table doesn't exist yet, create it
CREATE TABLE IF NOT EXISTS instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subdomain VARCHAR(50) NOT NULL UNIQUE,
    custom_domain VARCHAR(255),
    organization_name VARCHAR(200) NOT NULL,
    owner_id UUID NOT NULL,
    tier VARCHAR(20) NOT NULL DEFAULT 'BASIC',
    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    database_url VARCHAR(500) NOT NULL,
    database_username VARCHAR(100) NOT NULL,
    database_password VARCHAR(255) NOT NULL, -- Encrypted
    trial_started_at TIMESTAMP,
    trial_expires_at TIMESTAMP,
    subscription_id UUID,
    subscription_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Add bank account columns
ALTER TABLE instances ADD COLUMN IF NOT EXISTS bank_code VARCHAR(10);
ALTER TABLE instances ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100);
ALTER TABLE instances ADD COLUMN IF NOT EXISTS account_number VARCHAR(50);
ALTER TABLE instances ADD COLUMN IF NOT EXISTS account_name VARCHAR(200);
ALTER TABLE instances ADD COLUMN IF NOT EXISTS qr_template VARCHAR(500) DEFAULT 'HOCPHI {courseId} {studentName}';

-- Indexes
CREATE INDEX IF NOT EXISTS idx_instances_subdomain ON instances(subdomain);
CREATE INDEX IF NOT EXISTS idx_instances_custom_domain ON instances(custom_domain) WHERE custom_domain IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_instances_owner ON instances(owner_id);
CREATE INDEX IF NOT EXISTS idx_instances_status ON instances(status);
CREATE INDEX IF NOT EXISTS idx_instances_trial_expires ON instances(trial_expires_at) WHERE status = 'TRIAL';

-- Comments
COMMENT ON COLUMN instances.bank_code IS 'Vietnamese bank BIN code (e.g., 970415 for Vietcombank)';
COMMENT ON COLUMN instances.account_number IS 'Bank account number for receiving course enrollment payments';
COMMENT ON COLUMN instances.qr_template IS 'VietQR content template with variables: {courseId}, {studentName}, {timestamp}';

-- Trigger
CREATE TRIGGER IF NOT EXISTS update_instances_updated_at
    BEFORE UPDATE ON instances
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

# V4: GUEST ANALYTICS

**File:** `V4__add_guest_analytics.sql`
**Purpose:** Track guest behavior for owner insights

```sql
-- ============================================================================
-- V4: Guest Analytics Tables
-- ============================================================================

-- Guest Sessions table
CREATE TABLE guest_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(50) NOT NULL UNIQUE,
    instance_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    source VARCHAR(50), -- 'landing_page', 'social_media', 'organic', 'referral'
    device_type VARCHAR(20), -- 'mobile', 'desktop', 'tablet'
    user_agent TEXT,
    ip_address VARCHAR(45)
);

-- Guest Events table
CREATE TABLE guest_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(50) NOT NULL,
    instance_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- 'page_view', 'contact_click', 'course_view', etc.
    course_id UUID,
    course_name VARCHAR(200),
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

-- Indexes
CREATE INDEX idx_guest_sessions_session ON guest_sessions(session_id);
CREATE INDEX idx_guest_sessions_instance ON guest_sessions(instance_id);
CREATE INDEX idx_guest_sessions_created ON guest_sessions(created_at DESC);
CREATE INDEX idx_guest_sessions_expires ON guest_sessions(expires_at);

CREATE INDEX idx_guest_events_session ON guest_events(session_id);
CREATE INDEX idx_guest_events_instance ON guest_events(instance_id);
CREATE INDEX idx_guest_events_type ON guest_events(event_type);
CREATE INDEX idx_guest_events_course ON guest_events(course_id) WHERE course_id IS NOT NULL;
CREATE INDEX idx_guest_events_timestamp ON guest_events(timestamp DESC);

-- Comments
COMMENT ON TABLE guest_sessions IS 'Anonymous guest user sessions';
COMMENT ON TABLE guest_events IS 'Guest user behavior events for analytics';
COMMENT ON COLUMN guest_events.metadata IS 'Additional event data: {contactMethod: "facebook", duration: 120}';

-- Cleanup old sessions (retention: 90 days)
-- DELETE FROM guest_sessions WHERE created_at < NOW() - INTERVAL '90 days';
-- DELETE FROM guest_events WHERE timestamp < NOW() - INTERVAL '90 days';
```

---

# V5: AI BRANDING JOBS

**File:** `V5__add_branding_jobs.sql`
**Purpose:** Track AI branding generation jobs

```sql
-- ============================================================================
-- V5: AI Branding Jobs
-- ============================================================================

-- Branding status enum
CREATE TYPE branding_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');

-- Branding Jobs table
CREATE TABLE branding_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id VARCHAR(50) NOT NULL UNIQUE,
    instance_id UUID NOT NULL,
    logo_url VARCHAR(500),
    organization_name VARCHAR(200) NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'vi',
    status branding_status NOT NULL DEFAULT 'PENDING',
    generated_assets JSONB, -- {profileImages: {...}, heroImages: [...], marketingCopy: {...}}
    error_message VARCHAR(1000),
    progress_percentage INTEGER NOT NULL DEFAULT 0 CHECK (progress_percentage >= 0 AND progress_percentage <= 100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_branding_jobs_job_id ON branding_jobs(job_id);
CREATE INDEX idx_branding_jobs_instance ON branding_jobs(instance_id);
CREATE INDEX idx_branding_jobs_status ON branding_jobs(status);
CREATE INDEX idx_branding_jobs_created ON branding_jobs(created_at DESC);

-- Comments
COMMENT ON TABLE branding_jobs IS 'AI branding generation jobs (GPT-4 Vision + DALL-E 3)';
COMMENT ON COLUMN branding_jobs.generated_assets IS 'Generated assets: profile images, hero images, logos, banners, marketing copy';
COMMENT ON COLUMN branding_jobs.progress_percentage IS '0-100%: Logo analysis (20%), Hero generation (60%), Marketing copy (100%)';

-- Trigger
CREATE TRIGGER update_branding_jobs_updated_at
    BEFORE UPDATE ON branding_jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

# V6: SUBSCRIPTIONS

**File:** `V6__add_subscriptions.sql`
**Purpose:** Subscription lifecycle tracking

```sql
-- ============================================================================
-- V6: Subscriptions
-- ============================================================================

-- Subscription status enum
CREATE TYPE subscription_status AS ENUM ('TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'EXPIRED');

-- Subscriptions table
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL UNIQUE,
    tier VARCHAR(20) NOT NULL CHECK (tier IN ('BASIC', 'STANDARD', 'PREMIUM')),
    monthly_price BIGINT NOT NULL CHECK (monthly_price > 0),
    status subscription_status NOT NULL DEFAULT 'TRIAL',
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    trial_end TIMESTAMP,
    canceled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Subscription History (audit trail)
CREATE TABLE subscription_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    tier VARCHAR(20) NOT NULL,
    status subscription_status NOT NULL,
    monthly_price BIGINT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by VARCHAR(100),
    change_reason VARCHAR(500)
);

-- Indexes
CREATE INDEX idx_subscriptions_instance ON subscriptions(instance_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_period_end ON subscriptions(current_period_end);
CREATE INDEX idx_subscription_history_subscription ON subscription_history(subscription_id);
CREATE INDEX idx_subscription_history_changed ON subscription_history(changed_at DESC);

-- Comments
COMMENT ON TABLE subscriptions IS 'Subscription lifecycle management';
COMMENT ON TABLE subscription_history IS 'Audit trail for subscription changes';
COMMENT ON COLUMN subscriptions.trial_end IS 'Trial end date (14 days from trial_started_at)';
COMMENT ON COLUMN subscriptions.current_period_end IS 'When current billing period ends (renewal date)';

-- Triggers
CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger to log subscription changes
CREATE OR REPLACE FUNCTION log_subscription_change()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'UPDATE') THEN
        INSERT INTO subscription_history (subscription_id, tier, status, monthly_price, changed_at, changed_by)
        VALUES (NEW.id, NEW.tier, NEW.status, NEW.monthly_price, NOW(), NEW.updated_by);
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER log_subscription_changes
    AFTER UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION log_subscription_change();
```

---

# V7: STORAGE TRACKING

**File:** `V7__add_storage_tracking.sql`
**Purpose:** Track media storage usage per instance

```sql
-- ============================================================================
-- V7: Storage Tracking (MEDIA package)
-- ============================================================================

-- Storage Usage table
CREATE TABLE storage_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL UNIQUE,
    used_bytes BIGINT NOT NULL DEFAULT 0 CHECK (used_bytes >= 0),
    limit_bytes BIGINT NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Storage Events (audit trail for uploads/deletes)
CREATE TABLE storage_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    file_type VARCHAR(20) NOT NULL, -- 'IMAGE', 'VIDEO', 'ATTACHMENT'
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('UPLOAD', 'DELETE')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100)
);

-- Indexes
CREATE INDEX idx_storage_usage_instance ON storage_usage(instance_id);
CREATE INDEX idx_storage_events_instance ON storage_events(instance_id);
CREATE INDEX idx_storage_events_created ON storage_events(created_at DESC);
CREATE INDEX idx_storage_events_operation ON storage_events(operation);

-- Comments
COMMENT ON TABLE storage_usage IS 'Current storage usage per instance';
COMMENT ON TABLE storage_events IS 'Audit trail for all file uploads/deletes';
COMMENT ON COLUMN storage_usage.limit_bytes IS 'Storage limit based on tier: BASIC=0, STANDARD=5GB, PREMIUM=20GB';

-- Trigger to update storage usage on events
CREATE OR REPLACE FUNCTION update_storage_usage()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.operation = 'UPLOAD') THEN
        UPDATE storage_usage
        SET used_bytes = used_bytes + NEW.file_size_bytes,
            last_updated = NOW()
        WHERE instance_id = NEW.instance_id;

        -- Insert if not exists
        INSERT INTO storage_usage (instance_id, used_bytes, limit_bytes)
        VALUES (NEW.instance_id, NEW.file_size_bytes, 0)
        ON CONFLICT (instance_id) DO NOTHING;

    ELSIF (NEW.operation = 'DELETE') THEN
        UPDATE storage_usage
        SET used_bytes = GREATEST(used_bytes - NEW.file_size_bytes, 0),
            last_updated = NOW()
        WHERE instance_id = NEW.instance_id;
    END IF;

    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER track_storage_changes
    AFTER INSERT ON storage_events
    FOR EACH ROW
    EXECUTE FUNCTION update_storage_usage();

-- Initialize storage limits for existing instances
-- INSERT INTO storage_usage (instance_id, used_bytes, limit_bytes)
-- SELECT ic.instance_id, 0,
--   CASE ic.tier
--     WHEN 'BASIC' THEN 0
--     WHEN 'STANDARD' THEN 5368709120  -- 5GB
--     WHEN 'PREMIUM' THEN 21474836480  -- 20GB
--   END
-- FROM instance_configs ic
-- ON CONFLICT (instance_id) DO NOTHING;
```

---

# V8: PERFORMANCE INDEXES

**File:** `V8__add_performance_indexes.sql`
**Purpose:** Additional indexes for performance

```sql
-- ============================================================================
-- V8: Performance Indexes
-- ============================================================================

-- Composite indexes for common queries

-- Instance configs: frequently joined with other tables
CREATE INDEX IF NOT EXISTS idx_instance_configs_tier_deleted
ON instance_configs(tier, deleted) WHERE deleted = FALSE;

-- Payment orders: admin panel queries
CREATE INDEX IF NOT EXISTS idx_payment_orders_status_created
ON payment_orders(status, created_at DESC) WHERE status = 'PENDING';

-- Guest events: analytics queries
CREATE INDEX IF NOT EXISTS idx_guest_events_instance_type_timestamp
ON guest_events(instance_id, event_type, timestamp DESC);

-- Branding jobs: poll queries
CREATE INDEX IF NOT EXISTS idx_branding_jobs_status_updated
ON branding_jobs(status, updated_at DESC) WHERE status IN ('PENDING', 'PROCESSING');

-- Subscriptions: expiration checks
CREATE INDEX IF NOT EXISTS idx_subscriptions_status_period_end
ON subscriptions(status, current_period_end) WHERE status = 'ACTIVE';

-- Partial indexes for soft delete
CREATE INDEX IF NOT EXISTS idx_instances_active
ON instances(id) WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_payment_orders_active
ON payment_orders(id) WHERE deleted = FALSE;

-- ANALYZE tables for better query planning
ANALYZE instance_configs;
ANALYZE payment_orders;
ANALYZE guest_sessions;
ANALYZE guest_events;
ANALYZE branding_jobs;
ANALYZE subscriptions;
ANALYZE storage_usage;
```

---

# ROLLBACK STRATEGY

## Rollback Scripts

```sql
-- Rollback V8 (indexes can be dropped safely)
DROP INDEX IF EXISTS idx_instance_configs_tier_deleted;
DROP INDEX IF EXISTS idx_payment_orders_status_created;
DROP INDEX IF EXISTS idx_guest_events_instance_type_timestamp;
DROP INDEX IF EXISTS idx_branding_jobs_status_updated;
DROP INDEX IF EXISTS idx_subscriptions_status_period_end;
DROP INDEX IF EXISTS idx_instances_active;
DROP INDEX IF EXISTS idx_payment_orders_active;

-- Rollback V7 (storage tracking)
DROP TRIGGER IF EXISTS track_storage_changes ON storage_events;
DROP FUNCTION IF EXISTS update_storage_usage();
DROP TABLE IF EXISTS storage_events;
DROP TABLE IF EXISTS storage_usage;

-- Rollback V6 (subscriptions)
DROP TRIGGER IF EXISTS log_subscription_changes ON subscriptions;
DROP FUNCTION IF EXISTS log_subscription_change();
DROP TRIGGER IF EXISTS update_subscriptions_updated_at ON subscriptions;
DROP TABLE IF EXISTS subscription_history;
DROP TABLE IF EXISTS subscriptions;
DROP TYPE IF EXISTS subscription_status;

-- Rollback V5 (branding jobs)
DROP TRIGGER IF EXISTS update_branding_jobs_updated_at ON branding_jobs;
DROP TABLE IF EXISTS branding_jobs;
DROP TYPE IF EXISTS branding_status;

-- Rollback V4 (guest analytics)
DROP TABLE IF EXISTS guest_events;
DROP TABLE IF EXISTS guest_sessions;

-- Rollback V3 (bank accounts)
ALTER TABLE instances DROP COLUMN IF EXISTS bank_code;
ALTER TABLE instances DROP COLUMN IF EXISTS bank_name;
ALTER TABLE instances DROP COLUMN IF EXISTS account_number;
ALTER TABLE instances DROP COLUMN IF EXISTS account_name;
ALTER TABLE instances DROP COLUMN IF EXISTS qr_template;

-- Rollback V2 (payment orders)
DROP TRIGGER IF EXISTS update_payment_orders_updated_at ON payment_orders;
DROP TABLE IF EXISTS payment_orders;
DROP TYPE IF EXISTS payment_status;
DROP TYPE IF EXISTS payment_type;

-- Rollback V1 (instance configs)
DROP TRIGGER IF EXISTS update_instance_configs_updated_at ON instance_configs;
DROP FUNCTION IF EXISTS update_updated_at_column();
DROP TABLE IF EXISTS instance_configs;
```

## Testing Migrations

```bash
# Test migration on dev environment
flyway migrate -url=jdbc:postgresql://localhost:5432/kiteclass_dev

# Verify migration
flyway info

# Test rollback (clean + re-migrate)
flyway clean
flyway migrate

# Baseline for existing database
flyway baseline -baselineVersion=0

# Repair if needed
flyway repair
```

---

# SUMMARY

**Migrations:**
1. ✅ V1: Instance Configuration (Feature Detection)
2. ✅ V2: Payment Orders (VietQR)
3. ✅ V3: Instance Bank Accounts (Owner configurable)
4. ✅ V4: Guest Analytics (Sessions & Events)
5. ✅ V5: AI Branding Jobs
6. ✅ V6: Subscriptions (Lifecycle tracking)
7. ✅ V7: Storage Tracking (MEDIA package)
8. ✅ V8: Performance Indexes

**Total:** 8 migrations
**Estimated time:** 2-3 hours (with testing)

**Ready for:**
- Development environment
- Staging environment
- Production deployment

**Next Steps:**
1. Run migrations on dev database
2. Verify all tables created
3. Test with sample data
4. Deploy to staging
5. Production migration (with backup!)
