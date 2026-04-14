-- =========================================================================
-- V32: Branding Resources (classification pipeline)
-- =========================================================================
-- Context: GAP-007, ADR-005
-- Purpose: Track per-tenant branding artifacts by category (STATIC/TEMPLATE/FULL_AI)
-- Breaking change: NO (new table)
-- =========================================================================

CREATE TABLE branding_resources (
    id                BIGSERIAL PRIMARY KEY,
    instance_id       UUID         NOT NULL,

    type              VARCHAR(30)  NOT NULL,
    category          VARCHAR(20)  NOT NULL,
    storage_url       VARCHAR(500),
    template_id       BIGINT,
    ai_job_id         UUID,
    metadata          JSONB,

    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       NOT NULL DEFAULT 0,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_branding_resource_type
        CHECK (type IN ('LOGO','FAVICON','BANNER','HERO',
                        'COURSE_THUMBNAIL','SOCIAL_COVER','EMAIL_HEADER')),
    CONSTRAINT chk_branding_resource_category
        CHECK (category IN ('STATIC','TEMPLATE','FULL_AI')),
    CONSTRAINT chk_branding_resource_template_fk
        CHECK (category <> 'TEMPLATE' OR template_id IS NOT NULL),
    CONSTRAINT chk_branding_resource_ai_fk
        CHECK (category <> 'FULL_AI' OR ai_job_id IS NOT NULL),
    CONSTRAINT chk_branding_resource_static_no_fk
        CHECK (category <> 'STATIC' OR (template_id IS NULL AND ai_job_id IS NULL))
);

CREATE INDEX idx_branding_resource_type ON branding_resources(instance_id, type);
CREATE INDEX idx_branding_resource_category ON branding_resources(category);
CREATE INDEX idx_branding_resource_deleted ON branding_resources(deleted);
