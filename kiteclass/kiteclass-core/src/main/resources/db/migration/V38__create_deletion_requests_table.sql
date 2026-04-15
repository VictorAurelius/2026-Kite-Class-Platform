-- =========================================================================
-- V38: deletion_requests (Wave 4 Sub-PR 4.4 — GDPR Art. 17 erasure workflow)
-- =========================================================================
-- Context: ADR-013 Data Retention Classification + GAP-073 GDPR Deletion.
-- Purpose: Track tenant deletion requests through a 7-day grace window and
--          on into PROCESSING / COMPLETED (purge + pseudonymize pipeline).
--          Cancellation is allowed while still in PENDING or GRACE_PERIOD.
-- Owned by: wave/04-security-compliance/gdpr-deletion (Sub-PR 4.4).
-- =========================================================================

CREATE TABLE deletion_requests (
    id                     BIGSERIAL PRIMARY KEY,
    instance_id            UUID         NOT NULL,

    user_id                BIGINT       NOT NULL,
    tenant_id              UUID         NOT NULL,

    status                 VARCHAR(16)  NOT NULL,
    requested_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    grace_starts_at        TIMESTAMP,
    grace_ends_at          TIMESTAMP,
    processing_started_at  TIMESTAMP,
    completed_at           TIMESTAMP,
    cancelled_at           TIMESTAMP,
    cancellation_reason    VARCHAR(500),
    data_export_url        VARCHAR(1024),

    -- BaseEntity audit columns
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             BIGINT,
    updated_by             BIGINT,
    version                BIGINT       NOT NULL DEFAULT 0,
    deleted                BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_deletion_request_status CHECK (
        status IN ('PENDING', 'GRACE_PERIOD', 'PROCESSING', 'COMPLETED', 'CANCELLED')
    )
);

CREATE INDEX idx_deletion_request_user       ON deletion_requests(user_id);
CREATE INDEX idx_deletion_request_tenant     ON deletion_requests(tenant_id);
CREATE INDEX idx_deletion_request_status     ON deletion_requests(status);
CREATE INDEX idx_deletion_request_grace_ends ON deletion_requests(grace_ends_at);
CREATE INDEX idx_deletion_request_deleted    ON deletion_requests(deleted);
