-- V18__create_marketing_tables.sql
-- Marketing Module: Landing page, Lead management, Contact messages
-- @since 2.10

-- =====================================================
-- Table: landing_pages
-- Purpose: Per-tenant landing page content
-- Business Rule: BR-MKT-001 - Each tenant has ONE landing page
-- =====================================================
CREATE TABLE IF NOT EXISTS landing_pages (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- Hero Section
    hero_title VARCHAR(200) NOT NULL DEFAULT 'Welcome to Our Learning Center',
    hero_subtitle VARCHAR(500),
    hero_image_url VARCHAR(500),

    -- Teacher/About Section
    teacher_bio TEXT,

    -- Branding
    logo_url VARCHAR(500),
    tagline VARCHAR(200),
    primary_color VARCHAR(7) DEFAULT '#3B82F6',
    secondary_color VARCHAR(7) DEFAULT '#8B5CF6',

    -- Contact Info
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    address TEXT,

    -- Social Media
    facebook_url VARCHAR(255),
    youtube_url VARCHAR(255),
    instagram_url VARCHAR(255),

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT uk_landing_page_instance UNIQUE (instance_id, deleted),
    CONSTRAINT chk_landing_page_colors CHECK (
        primary_color ~ '^#[0-9A-Fa-f]{6}$' AND
        secondary_color ~ '^#[0-9A-Fa-f]{6}$'
    )
);

-- Index for tenant lookup
CREATE INDEX IF NOT EXISTS idx_landing_pages_instance ON landing_pages(instance_id) WHERE deleted = false;

-- =====================================================
-- Table: leads
-- Purpose: Track potential students (trial registrations)
-- Business Rule: BR-MKT-002 - Lead email unique per tenant
-- =====================================================
CREATE TABLE IF NOT EXISTS leads (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- Lead Information
    email VARCHAR(255) NOT NULL,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(20),

    -- Source & Interest
    source VARCHAR(50) NOT NULL DEFAULT 'LANDING_PAGE',
    -- LANDING_PAGE, CONTACT_FORM, TRIAL_SIGNUP, REFERRAL, SOCIAL_MEDIA, OTHER

    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    -- NEW, CONTACTED, QUALIFIED, CONVERTED, LOST, INVALID

    course_interest_id BIGINT,
    message TEXT,

    -- Tracking
    registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_contacted_at TIMESTAMP,
    converted_at TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT uk_lead_email_instance UNIQUE (instance_id, email, deleted),
    CONSTRAINT fk_lead_course FOREIGN KEY (course_interest_id) REFERENCES courses(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_leads_instance ON leads(instance_id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_leads_status ON leads(instance_id, status) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_leads_email ON leads(email) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_leads_registration_date ON leads(registration_date DESC);

-- =====================================================
-- Table: contact_messages
-- Purpose: Contact form submissions from guests
-- Business Rule: BR-MKT-003 - Triggers email to teacher
-- =====================================================
CREATE TABLE IF NOT EXISTS contact_messages (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- Sender Information
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),

    -- Message Content
    subject VARCHAR(300),
    message TEXT NOT NULL,

    -- Status
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    read_by VARCHAR(100),

    -- Response tracking
    replied BOOLEAN NOT NULL DEFAULT FALSE,
    replied_at TIMESTAMP,
    reply_message TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_contact_messages_instance ON contact_messages(instance_id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_contact_messages_unread ON contact_messages(instance_id, is_read) WHERE deleted = false AND is_read = false;
CREATE INDEX IF NOT EXISTS idx_contact_messages_created ON contact_messages(created_at DESC);

-- =====================================================
-- Comments
-- =====================================================
COMMENT ON TABLE landing_pages IS 'Per-tenant landing page content for guest visitors';
COMMENT ON TABLE leads IS 'Potential students who registered for trial or showed interest';
COMMENT ON TABLE contact_messages IS 'Contact form submissions from website visitors';

COMMENT ON COLUMN leads.source IS 'How the lead found us: LANDING_PAGE, CONTACT_FORM, TRIAL_SIGNUP, REFERRAL, SOCIAL_MEDIA, OTHER';
COMMENT ON COLUMN leads.status IS 'Lead qualification status: NEW, CONTACTED, QUALIFIED, CONVERTED, LOST, INVALID';
COMMENT ON COLUMN contact_messages.is_read IS 'Whether teacher has read the message';
