-- =========================================================================
-- V31: Frontend Instance Provisioning Lifecycle
-- =========================================================================
-- Context: GAP-009, ADR-004
-- Purpose: Track provisioning state of per-tenant frontend instances
-- Breaking change: NO (additive, no existing instance table)
-- =========================================================================

CREATE TABLE frontend_instances (
    id                   BIGSERIAL PRIMARY KEY,
    instance_id          UUID         NOT NULL,
    tenant_id            VARCHAR(100) NOT NULL,
    slug                 VARCHAR(80)  NOT NULL,
    frontend_url         VARCHAR(300),

    status               VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED',

    initializing_at      TIMESTAMP,
    generating_at        TIMESTAMP,
    deployed_at          TIMESTAMP,
    last_regenerate_at   TIMESTAMP,
    failed_at            TIMESTAMP,

    retry_count          INT          NOT NULL DEFAULT 0,
    failure_reason       VARCHAR(1000),
    branding_version     INT          NOT NULL DEFAULT 0,

    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_frontend_instance_status
        CHECK (status IN ('NOT_STARTED','INITIALIZING','GENERATING',
                          'DEPLOYED','REGENERATING','FAILED')),
    CONSTRAINT chk_frontend_instance_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_frontend_instance_branding_version CHECK (branding_version >= 0)
);

CREATE UNIQUE INDEX idx_frontend_instance_slug
    ON frontend_instances(instance_id, slug) WHERE deleted = FALSE;
CREATE INDEX idx_frontend_instance_status ON frontend_instances(status);
CREATE INDEX idx_frontend_instance_tenant ON frontend_instances(tenant_id);
CREATE INDEX idx_frontend_instance_deleted ON frontend_instances(deleted);
