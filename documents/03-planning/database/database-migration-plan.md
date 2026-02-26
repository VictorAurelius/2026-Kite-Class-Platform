# DATABASE MIGRATION PLAN

**Version:** 4.1 (Bundled Model) ⭐
**Created:** 2026-01-30
**Last Updated:** 2026-02-26 ⭐
**Database:** PostgreSQL 15
**Migration Tool:** Flyway

**Tham chiếu:**
- `database-design.md` (V4.1)
- `core-service-implementation.md` (V4.1)
- `system-architecture-v4.md` (V4.1)

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
9. [V8: Indexes](#v8-indexes)
10. [V9: LMS Tables (V4.1)](#v9-lms-tables-v41) ⭐ NEW
11. [V10: Marketing Tables (V4.1)](#v10-marketing-tables-v41) ⭐ NEW
12. [V11: Demo LMS Content (Optional)](#v11-demo-lms-content-optional) ⭐ NEW
13. [Rollback Strategy](#rollback-strategy)

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

# V9: LMS TABLES (V4.1) ⭐ NEW

**File:** `V9__create_lms_tables.sql`
**Purpose:** Create LMS module tables for structured learning paths
**Target:** Core Database (per-tenant)

```sql
-- ============================================================================
-- V9: Create LMS Module Tables (V4.1 Bundled Model)
-- Purpose: Course modules, lessons, resources, and progress tracking
-- ============================================================================

-- 1. Course Modules Table
CREATE TABLE course_modules (
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,

    -- Content
    title VARCHAR(200) NOT NULL,
    description TEXT,
    order_number INTEGER NOT NULL DEFAULT 0,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0 NOT NULL,

    -- Constraints
    CONSTRAINT uk_course_modules_course_order
        UNIQUE (course_id, order_number, instance_id, deleted)
);

-- Indexes
CREATE INDEX idx_course_modules_course_id ON course_modules(course_id) WHERE deleted = FALSE;
CREATE INDEX idx_course_modules_instance_id ON course_modules(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_course_modules_order ON course_modules(course_id, order_number) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE course_modules IS 'Learning modules within courses (V4.1)';
COMMENT ON COLUMN course_modules.order_number IS 'Display order within course (unique per course)';

-- 2. Lessons Table
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    module_id BIGINT NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,

    -- Content
    title VARCHAR(200) NOT NULL,
    content TEXT,
    video_url VARCHAR(500),

    -- Access Control ⭐ KEY
    is_trial BOOLEAN DEFAULT FALSE NOT NULL,

    -- Metadata
    order_number INTEGER NOT NULL DEFAULT 0,
    estimated_duration INTEGER, -- minutes

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0 NOT NULL,

    -- Constraints
    CONSTRAINT uk_lessons_module_order
        UNIQUE (module_id, order_number, instance_id, deleted),
    CONSTRAINT chk_lessons_duration
        CHECK (estimated_duration IS NULL OR estimated_duration > 0)
);

-- Indexes
CREATE INDEX idx_lessons_module_id ON lessons(module_id) WHERE deleted = FALSE;
CREATE INDEX idx_lessons_is_trial ON lessons(is_trial) WHERE deleted = FALSE;
CREATE INDEX idx_lessons_instance_id ON lessons(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_lessons_order ON lessons(module_id, order_number) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE lessons IS 'Individual lessons within modules (V4.1)';
COMMENT ON COLUMN lessons.is_trial IS 'Guest access flag: TRUE = public, FALSE = requires enrollment';
COMMENT ON COLUMN lessons.estimated_duration IS 'Estimated completion time in minutes';

-- 3. Learning Resources Table
CREATE TABLE learning_resources (
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,

    -- Resource Info
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    url VARCHAR(500),
    file_size BIGINT, -- bytes

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,

    -- Constraints
    CONSTRAINT chk_learning_resources_type
        CHECK (type IN ('PDF', 'VIDEO', 'SLIDES', 'QUIZ', 'OTHER')),
    CONSTRAINT chk_learning_resources_size
        CHECK (file_size IS NULL OR file_size > 0)
);

-- Indexes
CREATE INDEX idx_learning_resources_lesson_id ON learning_resources(lesson_id) WHERE deleted = FALSE;
CREATE INDEX idx_learning_resources_type ON learning_resources(type) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE learning_resources IS 'Additional learning materials attached to lessons (V4.1)';
COMMENT ON COLUMN learning_resources.type IS 'Resource type: PDF, VIDEO, SLIDES, QUIZ, OTHER';

-- 4. Lesson Progress Table
CREATE TABLE lesson_progress (
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    user_id BIGINT NOT NULL, -- From Gateway users table
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,

    -- Progress Tracking
    completed BOOLEAN DEFAULT FALSE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    progress_percent INTEGER DEFAULT 0 NOT NULL,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT uk_lesson_progress_user_lesson
        UNIQUE (user_id, lesson_id, instance_id),
    CONSTRAINT chk_lesson_progress_percent
        CHECK (progress_percent >= 0 AND progress_percent <= 100),
    CONSTRAINT chk_lesson_progress_completed
        CHECK (
            (completed = FALSE AND completed_at IS NULL) OR
            (completed = TRUE AND completed_at IS NOT NULL)
        )
);

-- Indexes
CREATE INDEX idx_lesson_progress_user_id ON lesson_progress(user_id);
CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress(lesson_id);
CREATE INDEX idx_lesson_progress_completed ON lesson_progress(completed);
CREATE INDEX idx_lesson_progress_instance_id ON lesson_progress(instance_id);

-- Comments
COMMENT ON TABLE lesson_progress IS 'Student learning progress per lesson (V4.1)';
COMMENT ON COLUMN lesson_progress.progress_percent IS 'Progress percentage (0-100)';

-- 5. Trigger for updated_at
CREATE OR REPLACE FUNCTION update_lms_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_course_modules_updated_at
    BEFORE UPDATE ON course_modules
    FOR EACH ROW
    EXECUTE FUNCTION update_lms_updated_at();

CREATE TRIGGER update_lessons_updated_at
    BEFORE UPDATE ON lessons
    FOR EACH ROW
    EXECUTE FUNCTION update_lms_updated_at();

CREATE TRIGGER update_learning_resources_updated_at
    BEFORE UPDATE ON learning_resources
    FOR EACH ROW
    EXECUTE FUNCTION update_lms_updated_at();

CREATE TRIGGER update_lesson_progress_updated_at
    BEFORE UPDATE ON lesson_progress
    FOR EACH ROW
    EXECUTE FUNCTION update_lms_updated_at();
```

**Rollback V9:**
```sql
DROP TRIGGER IF EXISTS update_lesson_progress_updated_at ON lesson_progress;
DROP TRIGGER IF EXISTS update_learning_resources_updated_at ON learning_resources;
DROP TRIGGER IF EXISTS update_lessons_updated_at ON lessons;
DROP TRIGGER IF EXISTS update_course_modules_updated_at ON course_modules;
DROP FUNCTION IF EXISTS update_lms_updated_at();
DROP TABLE IF EXISTS lesson_progress CASCADE;
DROP TABLE IF EXISTS learning_resources CASCADE;
DROP TABLE IF EXISTS lessons CASCADE;
DROP TABLE IF EXISTS course_modules CASCADE;
```

---

# V10: MARKETING TABLES (V4.1) ⭐ NEW

**File:** `V10__create_marketing_tables.sql`
**Purpose:** Create Marketing module tables for landing pages and lead capture
**Target:** Core Database (per-tenant)

```sql
-- ============================================================================
-- V10: Create Marketing Module Tables (V4.1 Bundled Model)
-- Purpose: Landing pages, lead management, and contact forms
-- ============================================================================

-- 1. Landing Pages Table
CREATE TABLE landing_pages (
    id BIGSERIAL PRIMARY KEY,

    -- Tenant Relationship (1:1)
    instance_id UUID NOT NULL UNIQUE,

    -- Hero Section
    hero_title VARCHAR(200),
    hero_subtitle VARCHAR(500),
    hero_image_url VARCHAR(500),

    -- About Section
    teacher_bio TEXT,
    logo_url VARCHAR(500),
    tagline VARCHAR(200),

    -- Branding
    primary_color VARCHAR(7), -- Hex #RRGGBB
    secondary_color VARCHAR(7),

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,

    -- Constraints
    CONSTRAINT chk_landing_pages_primary_color
        CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_landing_pages_secondary_color
        CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Indexes
CREATE INDEX idx_landing_pages_instance_id ON landing_pages(instance_id);

-- Comments
COMMENT ON TABLE landing_pages IS 'Tenant-specific landing page content (1:1 per tenant) - V4.1';
COMMENT ON COLUMN landing_pages.instance_id IS '1:1 with tenant (unique constraint)';
COMMENT ON COLUMN landing_pages.primary_color IS 'Primary brand color in hex format (#RRGGBB)';

-- 2. Leads Table
CREATE TABLE leads (
    id BIGSERIAL PRIMARY KEY,

    -- Tenant
    instance_id UUID NOT NULL,

    -- Lead Info
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(20),

    -- Source & Status
    source VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',

    -- Interest
    course_interest_id BIGINT REFERENCES courses(id),
    message TEXT,

    -- Workflow Tracking
    last_contacted_at TIMESTAMP WITH TIME ZONE,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT chk_leads_source
        CHECK (source IN ('LANDING_PAGE', 'CONTACT_FORM', 'TRIAL', 'REFERRAL')),
    CONSTRAINT chk_leads_status
        CHECK (status IN ('NEW', 'CONTACTED', 'CONVERTED', 'LOST'))
);

-- Indexes
CREATE INDEX idx_leads_instance_id ON leads(instance_id);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_created_at ON leads(created_at DESC);
CREATE INDEX idx_leads_source ON leads(source);

-- Comments
COMMENT ON TABLE leads IS 'Guest leads for conversion tracking (V4.1)';
COMMENT ON COLUMN leads.source IS 'Lead origin: LANDING_PAGE, CONTACT_FORM, TRIAL, REFERRAL';
COMMENT ON COLUMN leads.status IS 'Workflow: NEW → CONTACTED → CONVERTED/LOST';

-- 3. Contact Messages Table
CREATE TABLE contact_messages (
    id BIGSERIAL PRIMARY KEY,

    -- Tenant
    instance_id UUID NOT NULL,

    -- Message Info
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    message TEXT NOT NULL,

    -- Read Tracking
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT chk_contact_messages_read
        CHECK (
            (is_read = FALSE AND read_at IS NULL) OR
            (is_read = TRUE AND read_at IS NOT NULL)
        )
);

-- Indexes
CREATE INDEX idx_contact_messages_instance_id ON contact_messages(instance_id);
CREATE INDEX idx_contact_messages_is_read ON contact_messages(is_read) WHERE is_read = FALSE;
CREATE INDEX idx_contact_messages_created_at ON contact_messages(created_at DESC);

-- Comments
COMMENT ON TABLE contact_messages IS 'Guest contact form submissions (V4.1)';
COMMENT ON COLUMN contact_messages.is_read IS 'Marked as read by admin';

-- 4. Triggers for updated_at
CREATE OR REPLACE FUNCTION update_marketing_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_landing_pages_updated_at
    BEFORE UPDATE ON landing_pages
    FOR EACH ROW
    EXECUTE FUNCTION update_marketing_updated_at();

CREATE TRIGGER update_leads_updated_at
    BEFORE UPDATE ON leads
    FOR EACH ROW
    EXECUTE FUNCTION update_marketing_updated_at();
```

**Rollback V10:**
```sql
DROP TRIGGER IF EXISTS update_leads_updated_at ON leads;
DROP TRIGGER IF EXISTS update_landing_pages_updated_at ON landing_pages;
DROP FUNCTION IF EXISTS update_marketing_updated_at();
DROP TABLE IF EXISTS contact_messages CASCADE;
DROP TABLE IF EXISTS leads CASCADE;
DROP TABLE IF EXISTS landing_pages CASCADE;
```

---

# V11: DEMO LMS CONTENT (OPTIONAL) ⭐ NEW

**File:** `V11__seed_demo_lms_content.sql`
**Purpose:** Insert demo course structure for testing
**Target:** Core Database (per-tenant)
**WARNING:** FOR DEMO/TESTING ONLY - DELETE IN PRODUCTION

```sql
-- ============================================================================
-- V11: Seed Demo LMS Content (V4.1 Bundled Model)
-- Purpose: Insert demo course structure for testing
-- WARNING: FOR DEMO/TESTING ONLY - DELETE IN PRODUCTION
-- ============================================================================

-- Note: Assumes demo course exists with code 'DEMO-JAVA-2026'
-- If not, create one first or adjust the WHERE clause

-- Insert demo modules
DO $$
DECLARE
    v_course_id BIGINT;
    v_instance_id UUID;
    v_module_1_id BIGINT;
    v_module_2_id BIGINT;
BEGIN
    -- Get course and instance_id
    SELECT id, instance_id INTO v_course_id, v_instance_id
    FROM courses
    WHERE code = 'DEMO-JAVA-2026' AND deleted = FALSE
    LIMIT 1;

    IF v_course_id IS NULL THEN
        RAISE NOTICE 'Demo course not found. Skipping LMS content seeding.';
        RETURN;
    END IF;

    -- Insert Module 1
    INSERT INTO course_modules (course_id, title, description, order_number, instance_id, created_at, updated_at, deleted)
    VALUES (
        v_course_id,
        'Module 1: Introduction to Java',
        'Learn the basics of Java programming language',
        1,
        v_instance_id,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        FALSE
    )
    RETURNING id INTO v_module_1_id;

    -- Insert Module 2
    INSERT INTO course_modules (course_id, title, description, order_number, instance_id, created_at, updated_at, deleted)
    VALUES (
        v_course_id,
        'Module 2: Object-Oriented Programming',
        'Master OOP concepts in Java',
        2,
        v_instance_id,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        FALSE
    )
    RETURNING id INTO v_module_2_id;

    -- Insert Lessons for Module 1 (1 trial, 2 paid)
    INSERT INTO lessons (module_id, title, content, video_url, is_trial, order_number, estimated_duration, instance_id, created_at, updated_at, deleted)
    VALUES
        (v_module_1_id, 'Lesson 1.1: What is Java? (FREE)', 'Introduction to Java and its ecosystem. This is a free trial lesson.', 'https://example.com/video/java-intro.mp4', TRUE, 1, 30, v_instance_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
        (v_module_1_id, 'Lesson 1.2: Installing Java JDK', 'Step-by-step guide to install Java Development Kit.', 'https://example.com/video/java-install.mp4', FALSE, 2, 45, v_instance_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
        (v_module_1_id, 'Lesson 1.3: Your First Java Program', 'Write and run your first Hello World program.', 'https://example.com/video/hello-world.mp4', FALSE, 3, 60, v_instance_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

    -- Insert Lessons for Module 2 (1 trial, 2 paid)
    INSERT INTO lessons (module_id, title, content, video_url, is_trial, order_number, estimated_duration, instance_id, created_at, updated_at, deleted)
    VALUES
        (v_module_2_id, 'Lesson 2.1: Classes and Objects (FREE)', 'Understanding classes and objects in Java. This is a free trial lesson.', 'https://example.com/video/classes-objects.mp4', TRUE, 1, 50, v_instance_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
        (v_module_2_id, 'Lesson 2.2: Inheritance and Polymorphism', 'Learn about inheritance and polymorphism concepts.', 'https://example.com/video/inheritance.mp4', FALSE, 2, 70, v_instance_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
        (v_module_2_id, 'Lesson 2.3: Interfaces and Abstract Classes', 'Deep dive into interfaces and abstract classes.', 'https://example.com/video/interfaces.mp4', FALSE, 3, 65, v_instance_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

    RAISE NOTICE 'Demo LMS content seeded successfully.';
END $$;
```

**Rollback V11:**
```sql
-- Delete all demo content (lessons → modules)
DELETE FROM lessons WHERE module_id IN (
    SELECT id FROM course_modules WHERE course_id IN (
        SELECT id FROM courses WHERE code = 'DEMO-JAVA-2026'
    )
);

DELETE FROM course_modules WHERE course_id IN (
    SELECT id FROM courses WHERE code = 'DEMO-JAVA-2026'
);
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
9. ⭐ V9: LMS Tables (V4.1 - course_modules, lessons, learning_resources, lesson_progress)
10. ⭐ V10: Marketing Tables (V4.1 - landing_pages, leads, contact_messages)
11. ⭐ V11: Demo LMS Content (Optional - testing only)

**Total:** 11 migrations (8 existing + 3 new V4.1)
**Estimated time:** 3-4 hours (with testing + V4.1 additions)

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
