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
13. [V12: Trial Learning Support (V4.1 Phase 2)](#v12-trial-learning-support-v41---phase-2) ⭐ NEW
14. [V13: File Storage Tables (V4.1 Phase 2)](#v13-file-storage-tables-v41---phase-2) ⭐ NEW
15. [Rollback Strategy](#rollback-strategy)

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

# V12: TRIAL LEARNING SUPPORT (V4.1 - Phase 2) ⭐ NEW

**Date**: 2026-Q2 (planned)
**Dependencies**: V11 (Demo LMS Content), V10 (Marketing Tables - leads table exists)
**Services affected**: Gateway (users enum), Core (leads extension, trial_quotas, courses, lessons)

## Purpose

Add trial user support for "try before buy" learning experience. Extends Marketing Module's lead capture with actual learning functionality.

**Key Features**:
- Trial users authenticated via magic links (passwordless)
- Daily quota limit (3 lessons/day)
- Self-paced learning (no class enrollment required in Phase 1)
- Progress preserved after conversion to paid student

## Changes

### Gateway Service

**File**: `kiteclass-gateway/src/main/resources/db/migration/V12__add_trial_user_role.sql`

```sql
-- V12: Add TRIAL_USER role to user_role enum
-- Purpose: Support trial user authentication and authorization

-- Step 1: Add new enum value
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'TRIAL_USER';

-- Step 2: Verify enum values
COMMENT ON TYPE user_role IS 'User roles: SUPER_ADMIN, ADMIN, TEACHER, STUDENT, TRIAL_USER (as of V12)';

-- Step 3: Add index for trial users (performance optimization)
CREATE INDEX IF NOT EXISTS idx_users_role_trial
ON users(role)
WHERE role = 'TRIAL_USER' AND deleted = FALSE;

COMMENT ON INDEX idx_users_role_trial IS 'Fast lookup for trial users in rate limiting and access control';
```

**Verification**:
```sql
-- Check enum values
SELECT enumlabel, enumsortorder
FROM pg_enum
WHERE enumtypid = 'user_role'::regtype
ORDER BY enumsortorder;

-- Expected output includes: SUPER_ADMIN, ADMIN, TEACHER, STUDENT, TRIAL_USER
```

**Rollback**:
```sql
-- WARNING: Cannot remove enum value in PostgreSQL without recreating enum
-- Alternative: Mark as deprecated in comments
COMMENT ON TYPE user_role IS 'User roles: SUPER_ADMIN, ADMIN, TEACHER, STUDENT, TRIAL_USER (deprecated V13+ - do not use)';

-- Manual cleanup required: Update all TRIAL_USER rows to STUDENT before removing
UPDATE users SET role = 'STUDENT' WHERE role = 'TRIAL_USER';

-- Drop index
DROP INDEX IF EXISTS idx_users_role_trial;
```

### Core Service

**File**: `kiteclass-core/src/main/resources/db/migration/V12__create_trial_learning_tables.sql`

```sql
-- V12: Create Trial Learning Tables
-- Purpose: Support trial users with daily lesson quotas
-- Dependencies: V10 (leads table must exist)

-- =============================================================================
-- SECTION 1: Extend leads table with user_id column
-- =============================================================================

-- Add user_id column to existing leads table (from V10)
ALTER TABLE leads ADD COLUMN IF NOT EXISTS user_id UUID;

-- Add index for user_id lookups
CREATE INDEX IF NOT EXISTS idx_leads_user_id ON leads(user_id);

-- Add comment
COMMENT ON COLUMN leads.user_id IS 'FK to Gateway users.id (soft reference for cross-service). NULL for non-trial leads.';

-- Update constraint to allow NULL user_id (non-trial leads don't have user accounts)
-- Existing constraint uq_leads_email_instance remains unchanged

-- =============================================================================
-- SECTION 2: Create trial_quotas table
-- =============================================================================

CREATE TABLE trial_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    user_id UUID NOT NULL, -- FK to Gateway users(id)
    quota_date DATE NOT NULL,
    lessons_accessed INT NOT NULL DEFAULT 0,
    quota_limit INT NOT NULL DEFAULT 3, -- Default 3 lessons per day
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_trial_quotas_user_date UNIQUE (user_id, quota_date, instance_id),
    CONSTRAINT chk_quota_limit_positive CHECK (quota_limit > 0),
    CONSTRAINT chk_lessons_accessed_non_negative CHECK (lessons_accessed >= 0),
    CONSTRAINT chk_lessons_not_exceed_limit CHECK (lessons_accessed <= quota_limit)
);

-- Indexes
CREATE INDEX idx_trial_quotas_user_id ON trial_quotas(user_id);
CREATE INDEX idx_trial_quotas_date ON trial_quotas(quota_date);
CREATE INDEX idx_trial_quotas_user_date ON trial_quotas(user_id, quota_date);

-- Comments
COMMENT ON TABLE trial_quotas IS 'Daily lesson access limits for trial users (default 3 lessons/day). Resets daily.';
COMMENT ON COLUMN trial_quotas.quota_date IS 'Date of quota (resets daily at midnight UTC)';
COMMENT ON COLUMN trial_quotas.lessons_accessed IS 'Number of lessons accessed on quota_date (incremented on lesson view)';
COMMENT ON COLUMN trial_quotas.quota_limit IS 'Maximum lessons allowed per day (default 3, configurable per tenant)';

-- =============================================================================
-- SECTION 3: Extend courses table with is_trial flag
-- =============================================================================

ALTER TABLE courses ADD COLUMN IF NOT EXISTS is_trial BOOLEAN NOT NULL DEFAULT FALSE;

-- Index for filtering trial courses
CREATE INDEX IF NOT EXISTS idx_courses_trial ON courses(is_trial) WHERE is_trial = TRUE AND deleted = FALSE;

COMMENT ON COLUMN courses.is_trial IS 'Mark course as trial-accessible (single course approach, not separate trial course). Typically one trial course per tenant.';

-- =============================================================================
-- SECTION 4: Extend lessons table with is_trial_accessible flag
-- =============================================================================

ALTER TABLE lessons ADD COLUMN IF NOT EXISTS is_trial_accessible BOOLEAN NOT NULL DEFAULT FALSE;

-- Index for filtering trial lessons
CREATE INDEX IF NOT EXISTS idx_lessons_trial ON lessons(is_trial_accessible) WHERE is_trial_accessible = TRUE AND deleted = FALSE;

-- Composite index for trial lesson queries (course + trial flag)
CREATE INDEX IF NOT EXISTS idx_lessons_course_trial ON lessons(course_id, is_trial_accessible) WHERE deleted = FALSE;

COMMENT ON COLUMN lessons.is_trial_accessible IS 'Mark lesson accessible to trial users (typically first 1-3 lessons per course). Must be part of a course where is_trial = TRUE.';

-- =============================================================================
-- SECTION 5: Add trigger for trial_quotas updated_at
-- =============================================================================

CREATE OR REPLACE FUNCTION update_trial_quotas_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_trial_quotas_updated_at
BEFORE UPDATE ON trial_quotas
FOR EACH ROW
EXECUTE FUNCTION update_trial_quotas_updated_at();

COMMENT ON TRIGGER update_trial_quotas_updated_at ON trial_quotas IS 'Auto-update updated_at on quota changes';
```

**Verification**:
```sql
-- Check tables created
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('leads', 'trial_quotas')
ORDER BY table_name;

-- Check columns added
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'courses' AND column_name = 'is_trial';

SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'lessons' AND column_name = 'is_trial_accessible';

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'leads' AND column_name = 'user_id';

-- Check constraints
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name IN ('leads', 'trial_quotas')
ORDER BY table_name, constraint_name;

-- Check indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('trial_quotas', 'courses', 'lessons')
  AND indexname LIKE '%trial%'
ORDER BY tablename, indexname;
```

**Rollback**:
```sql
-- Rollback V12: Remove trial learning tables and columns

-- Step 1: Drop triggers
DROP TRIGGER IF EXISTS update_trial_quotas_updated_at ON trial_quotas;
DROP FUNCTION IF EXISTS update_trial_quotas_updated_at();

-- Step 2: Drop indexes
DROP INDEX IF EXISTS idx_lessons_course_trial;
DROP INDEX IF EXISTS idx_lessons_trial;
DROP INDEX IF EXISTS idx_courses_trial;
DROP INDEX IF EXISTS idx_trial_quotas_user_date;
DROP INDEX IF EXISTS idx_trial_quotas_date;
DROP INDEX IF EXISTS idx_trial_quotas_user_id;
DROP INDEX IF EXISTS idx_leads_user_id;

-- Step 3: Drop columns
ALTER TABLE lessons DROP COLUMN IF EXISTS is_trial_accessible;
ALTER TABLE courses DROP COLUMN IF EXISTS is_trial;
ALTER TABLE leads DROP COLUMN IF EXISTS user_id;

-- Step 4: Drop tables
DROP TABLE IF EXISTS trial_quotas CASCADE;

-- Note: leads table is NOT dropped (created in V10, only user_id column removed)
```

## Testing

### Pre-Migration Checks

```bash
# Backup database before migration
pg_dump -U postgres -d kiteclass_dev > kiteclass_dev_pre_v12_backup.sql

# Check current migration status
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```

### Post-Migration Verification

```sql
-- 1. Verify Gateway enum
\dT+ user_role

-- 2. Verify Core tables
\dt+ trial_quotas

-- 3. Test trial quota insertion
INSERT INTO trial_quotas (instance_id, user_id, quota_date, lessons_accessed, quota_limit)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000', -- test tenant
    gen_random_uuid(), -- test user
    CURRENT_DATE,
    0,
    3
);

-- 4. Test quota constraints
-- Should FAIL (negative lessons_accessed)
INSERT INTO trial_quotas (instance_id, user_id, quota_date, lessons_accessed, quota_limit)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    gen_random_uuid(),
    CURRENT_DATE,
    -1, -- Invalid
    3
);
-- Expected: ERROR: new row for relation "trial_quotas" violates check constraint "chk_lessons_accessed_non_negative"

-- 5. Test updated_at trigger
UPDATE trial_quotas SET lessons_accessed = 1 WHERE id = (SELECT id FROM trial_quotas LIMIT 1);
SELECT id, lessons_accessed, created_at, updated_at FROM trial_quotas LIMIT 1;
-- Expected: updated_at > created_at

-- 6. Cleanup test data
DELETE FROM trial_quotas WHERE instance_id = '550e8400-e29b-41d4-a716-446655440000';
```

## Data Migration (if needed)

If converting existing "guest" users to trial users (ONLY if applicable):

```sql
-- WARNING: Run ONLY if your system has existing guest users to convert
-- DO NOT run in production without DBA review

-- Step 1: Identify guest users (example criteria: email contains 'guest' or 'trial')
SELECT id, email, role, created_at
FROM users
WHERE role = 'STUDENT'
  AND (email LIKE '%guest%' OR email LIKE '%trial%')
LIMIT 10;

-- Step 2: Update user role to TRIAL_USER (test with LIMIT first)
-- BEGIN TRANSACTION;

UPDATE users
SET role = 'TRIAL_USER', updated_at = NOW()
WHERE role = 'STUDENT'
  AND email LIKE '%trial%'
  AND created_at > '2026-01-01'; -- Only recent signups

-- Step 3: Create lead records for trial users
INSERT INTO leads (instance_id, user_id, name, email, source, status, registration_date, created_at, updated_at)
SELECT
    u.instance_id,
    u.id,
    COALESCE(u.full_name, u.email),
    u.email,
    'TRIAL_SIGNUP',
    'NEW',
    u.created_at,
    NOW(),
    NOW()
FROM users u
WHERE u.role = 'TRIAL_USER'
  AND NOT EXISTS (
    SELECT 1 FROM leads l WHERE l.user_id = u.id
  );

-- Step 4: Verify changes
SELECT u.id, u.email, u.role, l.status, l.source
FROM users u
LEFT JOIN leads l ON l.user_id = u.id
WHERE u.role = 'TRIAL_USER'
LIMIT 10;

-- COMMIT; -- Only commit if verification looks correct
-- ROLLBACK; -- Use this if something looks wrong
```

## Deployment Notes

### Deployment Order (CRITICAL)

1. **Gateway first**: Run Gateway V12 migration to add TRIAL_USER enum
2. **Core second**: Run Core V12 migration to create tables/columns
3. **Verify**: Query enum values, table existence, constraints
4. **Application deployment**: Deploy Gateway service, then Core service

**Rationale**: Core migration references TRIAL_USER enum in comments/documentation. While not a hard FK dependency, deploying Gateway first maintains logical consistency.

### Downtime Requirements

- **No downtime required** (additive changes only)
- Migrations add columns with DEFAULT values → no NULL conflicts
- New tables created → no disruption to existing queries
- Enum values added → backward compatible (existing roles still work)

### Performance Impact

- **Negligible**: New indexes created with `IF NOT EXISTS`
- Estimated migration time: < 30 seconds per database
- No table locks on existing data

### Monitoring After Deployment

```sql
-- 1. Check trial user count
SELECT role, COUNT(*)
FROM users
WHERE deleted = FALSE
GROUP BY role;

-- 2. Check trial quota usage
SELECT
    quota_date,
    COUNT(*) as active_users,
    AVG(lessons_accessed) as avg_lessons,
    SUM(CASE WHEN lessons_accessed >= quota_limit THEN 1 ELSE 0 END) as quota_exceeded_count
FROM trial_quotas
WHERE quota_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY quota_date
ORDER BY quota_date DESC;

-- 3. Check trial course configuration
SELECT id, code, name, is_trial, deleted
FROM courses
WHERE is_trial = TRUE;

-- 4. Check trial lesson count per course
SELECT
    c.id as course_id,
    c.code,
    c.name,
    COUNT(l.id) as total_lessons,
    COUNT(CASE WHEN l.is_trial_accessible THEN 1 END) as trial_lessons
FROM courses c
LEFT JOIN lessons l ON l.course_id = c.id AND l.deleted = FALSE
WHERE c.is_trial = TRUE AND c.deleted = FALSE
GROUP BY c.id, c.code, c.name;
```

## Related PRs

- **Gateway PR 1.13**: Trial User Authentication Support (magic link, JWT)
- **Core PR 2.13**: Trial Registration & Quota Management (LeadService, TrialQuotaService)
- **Core PR 2.14**: Lead to Student Conversion (payment verification, role update)
- **Frontend PR 3.13**: Trial Learning UI (dashboard, lesson viewer, quota display)
- **Frontend PR 3.14**: Lead Conversion Flow (payment form, success page)

## Migration Timeline

- **Week 1**: Gateway V12 deployment (enum addition)
- **Week 2**: Core V12 deployment (tables + columns)
- **Week 3**: Application code deployment (PRs 1.13, 2.13, 2.14)
- **Week 4**: Frontend deployment (PRs 3.13, 3.14) + End-to-end testing

**Total estimated time**: 4 weeks (includes testing and staged rollout)

---

# V13: FILE STORAGE TABLES (V4.1 - Phase 2) ⭐ NEW

**Date**: 2026-Q2 (planned)
**Dependencies**: V12 (Trial Learning System)
**Services affected**: Core (uploaded_files, storage_quotas tables)

## Purpose

Add comprehensive file storage support with S3-compatible storage (MinIO dev, AWS S3 prod), presigned URLs for secure upload/download, storage quota tracking, and multi-tenant isolation.

**Key Features**:
- Direct client-to-S3 uploads via presigned URLs (10min upload, 24h download)
- Storage quota enforcement (Trial: 500MB, Basic: 5GB, Pro: 50GB)
- Multi-tenant isolation (bucket prefixes + instance_id)
- File lifecycle tracking (UPLOADING → PROCESSING → READY → FAILED)
- Access control (PRIVATE, COURSE, PUBLIC)
- Video metadata support (duration, resolution, codec)
- Soft delete with 30-day grace period

**Related Documentation**: See [Storage Service Design](../implementation/storage-service-design.md) for complete architecture, flows, and implementation details.

## Changes

### Core Service

**File**: `kiteclass-core/src/main/resources/db/migration/V13__create_file_storage_tables.sql`

```sql
-- V13: Create File Storage Tables
-- Purpose: Support file uploads (avatars, documents, videos, certificates, assignments)
-- Dependencies: None (standalone tables)

-- =============================================================================
-- SECTION 1: uploaded_files table
-- =============================================================================

CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    uploaded_by UUID NOT NULL, -- FK to Gateway users(id) - soft reference
    file_type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL UNIQUE,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADING',

    -- Video metadata (nullable for non-video files)
    duration_seconds INT,
    resolution VARCHAR(20),
    video_codec VARCHAR(50),

    -- Access control
    access_level VARCHAR(50) NOT NULL DEFAULT 'PRIVATE',
    related_entity_type VARCHAR(50),
    related_entity_id VARCHAR(50),

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT chk_file_type CHECK (file_type IN ('AVATAR', 'DOCUMENT', 'VIDEO', 'CERTIFICATE', 'ASSIGNMENT')),
    CONSTRAINT chk_file_status CHECK (status IN ('UPLOADING', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_access_level CHECK (access_level IN ('PRIVATE', 'COURSE', 'PUBLIC')),
    CONSTRAINT chk_file_size_positive CHECK (file_size_bytes > 0)
);

-- Indexes
CREATE INDEX idx_uploaded_files_instance_id ON uploaded_files(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_uploaded_by ON uploaded_files(uploaded_by) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_type ON uploaded_files(file_type) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_entity ON uploaded_files(related_entity_type, related_entity_id) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_status ON uploaded_files(status) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_created_at ON uploaded_files(created_at);

-- Comments
COMMENT ON TABLE uploaded_files IS 'File metadata storage - actual files in S3 (V4.1)';
COMMENT ON COLUMN uploaded_files.uploaded_by IS 'FK to Gateway users.id (soft reference for cross-service)';
COMMENT ON COLUMN uploaded_files.storage_path IS 'S3 object key: {tenant-id}/{file-type}/{uuid}.{ext}';
COMMENT ON COLUMN uploaded_files.status IS 'Upload lifecycle: UPLOADING → PROCESSING → READY → FAILED';
COMMENT ON COLUMN uploaded_files.access_level IS 'Access control: PRIVATE (uploader only), COURSE (teacher+students), PUBLIC (all authenticated)';

-- =============================================================================
-- SECTION 2: storage_quotas table
-- =============================================================================

CREATE TABLE storage_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL UNIQUE, -- One quota per tenant
    quota_bytes BIGINT NOT NULL DEFAULT 1073741824, -- Default 1GB
    used_bytes BIGINT NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT chk_quota_bytes_positive CHECK (quota_bytes > 0),
    CONSTRAINT chk_used_bytes_non_negative CHECK (used_bytes >= 0)
);

-- Indexes
CREATE INDEX idx_storage_quotas_instance_id ON storage_quotas(instance_id);

-- Comments
COMMENT ON TABLE storage_quotas IS 'Per-tenant storage quota tracking (V4.1)';
COMMENT ON COLUMN storage_quotas.quota_bytes IS 'Maximum storage allowed (Trial: 500MB, Basic: 5GB, Pro: 50GB, Enterprise: custom)';
COMMENT ON COLUMN storage_quotas.used_bytes IS 'Current storage usage (calculated from uploaded_files)';
COMMENT ON COLUMN storage_quotas.last_calculated_at IS 'Last time quota was recalculated (scheduled job)';

-- =============================================================================
-- SECTION 3: Triggers for updated_at
-- =============================================================================

CREATE OR REPLACE FUNCTION update_storage_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_uploaded_files_updated_at
BEFORE UPDATE ON uploaded_files
FOR EACH ROW
EXECUTE FUNCTION update_storage_updated_at();

CREATE TRIGGER update_storage_quotas_updated_at
BEFORE UPDATE ON storage_quotas
FOR EACH ROW
EXECUTE FUNCTION update_storage_updated_at();

COMMENT ON TRIGGER update_uploaded_files_updated_at ON uploaded_files IS 'Auto-update updated_at on file metadata changes';
COMMENT ON TRIGGER update_storage_quotas_updated_at ON storage_quotas IS 'Auto-update updated_at on quota changes';
```

**Verification**:
```sql
-- Check tables created
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('uploaded_files', 'storage_quotas')
ORDER BY table_name;

-- Check columns
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'uploaded_files'
ORDER BY ordinal_position;

SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'storage_quotas'
ORDER BY ordinal_position;

-- Check constraints
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name IN ('uploaded_files', 'storage_quotas')
ORDER BY table_name, constraint_name;

-- Check indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('uploaded_files', 'storage_quotas')
ORDER BY tablename, indexname;
```

**Rollback**:
```sql
-- Rollback V13: Remove file storage tables

-- Step 1: Drop triggers
DROP TRIGGER IF EXISTS update_storage_quotas_updated_at ON storage_quotas;
DROP TRIGGER IF EXISTS update_uploaded_files_updated_at ON uploaded_files;
DROP FUNCTION IF EXISTS update_storage_updated_at();

-- Step 2: Drop tables (CASCADE in case of FKs)
DROP TABLE IF EXISTS storage_quotas CASCADE;
DROP TABLE IF EXISTS uploaded_files CASCADE;
```

## Testing

### Pre-Migration Checks

```bash
# Backup database before migration
pg_dump -U postgres -d kiteclass_dev > kiteclass_dev_pre_v13_backup.sql

# Check current migration status
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```

### Post-Migration Verification

```sql
-- 1. Verify tables created
\dt+ uploaded_files storage_quotas

-- 2. Test file record insertion
INSERT INTO uploaded_files (
    instance_id, uploaded_by, file_type, original_filename,
    storage_path, file_size_bytes, mime_type, status, access_level
)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000', -- test tenant
    gen_random_uuid(), -- test user
    'AVATAR',
    'profile.png',
    '550e8400-e29b-41d4-a716-446655440000/avatars/abc123.png',
    102400, -- 100KB
    'image/png',
    'READY',
    'PRIVATE'
);

-- 3. Test storage quota insertion
INSERT INTO storage_quotas (instance_id, quota_bytes, used_bytes)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    1073741824, -- 1GB
    0
);

-- 4. Test quota constraints
-- Should FAIL (negative used_bytes)
INSERT INTO storage_quotas (instance_id, quota_bytes, used_bytes)
VALUES (
    gen_random_uuid(),
    1073741824,
    -100 -- Invalid
);
-- Expected: ERROR: new row for relation "storage_quotas" violates check constraint "chk_used_bytes_non_negative"

-- 5. Test updated_at trigger
UPDATE uploaded_files SET status = 'PROCESSING' WHERE id = (SELECT id FROM uploaded_files LIMIT 1);
SELECT id, status, created_at, updated_at FROM uploaded_files LIMIT 1;
-- Expected: updated_at > created_at

-- 6. Test quota calculation query
UPDATE storage_quotas sq
SET used_bytes = (
    SELECT COALESCE(SUM(file_size_bytes), 0)
    FROM uploaded_files uf
    WHERE uf.instance_id = sq.instance_id
      AND uf.status = 'READY'
      AND uf.deleted = FALSE
),
last_calculated_at = CURRENT_TIMESTAMP;

SELECT * FROM storage_quotas LIMIT 1;

-- 7. Cleanup test data
DELETE FROM uploaded_files WHERE instance_id = '550e8400-e29b-41d4-a716-446655440000';
DELETE FROM storage_quotas WHERE instance_id = '550e8400-e29b-41d4-a716-446655440000';
```

## Data Seeding (Optional)

```sql
-- Create default quota for existing tenants (if needed)
INSERT INTO storage_quotas (instance_id, quota_bytes, used_bytes)
SELECT DISTINCT instance_id, 1073741824, 0 -- 1GB default
FROM courses
WHERE deleted = FALSE
ON CONFLICT (instance_id) DO NOTHING;
```

## Related PRs

- **Core PR 2.10.1**: Storage & File Management Service (FileService, StorageQuotaService, FileRetentionService)
- **Frontend PR 3.10**: Profile picture upload (Settings page)
- **Frontend PR 3.12**: Guest Pages (teacher photos, hero images upload)
- **Frontend PR 3.13**: AI Branding (logo upload via FileService)

## File Type Limits

```
AVATAR: max 10MB (image/png, image/jpeg, image/webp)
DOCUMENT: max 50MB (application/pdf, .docx, .xlsx)
VIDEO: max 2GB (video/mp4, video/webm)
CERTIFICATE: max 5MB (application/pdf)
ASSIGNMENT: max 50MB (application/pdf, .docx)
```

## Storage Quota Tiers

```
Trial: 500 MB (524,288,000 bytes)
Basic: 5 GB (5,368,709,120 bytes)
Pro: 50 GB (53,687,091,200 bytes)
Enterprise: Custom (unlimited)
```

## Migration Timeline

- **Week 1**: Core V13 deployment (tables creation)
- **Week 2**: MinIO Docker setup + S3 configuration
- **Week 3**: Application code deployment (PR 2.10.1)
- **Week 4**: Frontend deployment (PRs 3.10, 3.12) + End-to-end testing

**Total estimated time**: 4 weeks (includes testing and staged rollout)

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
12. ⭐ V12: Trial Learning Support (V4.1 Phase 2 - TRIAL_USER role, trial_quotas, lesson access control)
13. ⭐ V13: File Storage Tables (V4.1 Phase 2 - uploaded_files, storage_quotas) ⭐ NEW

**Total:** 13 migrations (8 existing + 5 new V4.1)
**Estimated time:** 5-6 hours (with testing + V4.1 additions)

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
