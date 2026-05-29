-- GAP-809 walk fix: create landing_pages table (entity shipped without migration).
--
-- ROOT CAUSE
-- ----------
-- LandingPage entity (com.kiteclass.core.module.marketing.entity.LandingPage,
-- @Table "landing_pages") + LandingPageServiceImpl.getOrCreateDefault() shipped
-- WITHOUT a Flyway migration creating the table. The public tenant homepage
-- (kiteclass-frontend (public)/page.tsx → GET /api/v1/tenants/{id}/landing) thus
-- returned HTTP 500 (relation "public.landing_pages" does not exist) → fell back
-- to the generic KiteClass landing (blue #3B82F6) for ALL tenants. Surfaced
-- 2026-05-29 demo-trio walk when verifying the Sky Education public homepage.
--
-- Columns mirror LandingPage entity + BaseEntity (audit cols UUID post-V73).
-- Tenant isolation via Hibernate tenantFilter (per V69 payment_records pattern);
-- no DB RLS policy (public landing query is intentionally cross-tenant via explicit
-- tenantId param).

CREATE TABLE IF NOT EXISTS landing_pages (
    id              BIGSERIAL PRIMARY KEY,
    instance_id     UUID NOT NULL,
    hero_title      VARCHAR(200) NOT NULL DEFAULT 'Welcome to Our Learning Center',
    hero_subtitle   VARCHAR(500),
    hero_image_url  VARCHAR(500),
    teacher_bio     TEXT,
    logo_url        VARCHAR(500),
    tagline         VARCHAR(200),
    primary_color   VARCHAR(7) DEFAULT '#3B82F6',
    secondary_color VARCHAR(7) DEFAULT '#8B5CF6',
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(20),
    address         TEXT,
    facebook_url    VARCHAR(255),
    youtube_url     VARCHAR(255),
    instagram_url   VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      UUID,
    updated_by      UUID,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT
);

-- One landing page per tenant (getOrCreateDefault assumes single active row per instance).
CREATE UNIQUE INDEX IF NOT EXISTS uk_landing_pages_instance
    ON landing_pages (instance_id)
    WHERE deleted = false;
