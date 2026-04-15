-- =========================================================================
-- V34: Rebrand approvals (GAP-070 — concurrent rebrand race + approval gate)
-- =========================================================================
-- Context: GAP-070, ADR-004 state machine extension, Wave 3 Sub-PR 3.5
-- Purpose: Track per-rebrand approval workflow (enterprise tier) + audit
-- =========================================================================

CREATE TABLE rebrand_approvals (
    id                   BIGSERIAL PRIMARY KEY,
    instance_id          UUID         NOT NULL,

    target_instance_id   BIGINT       NOT NULL,
    status               VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    initiator_user_id    BIGINT       NOT NULL,
    approver_user_id     BIGINT,
    reason               VARCHAR(500),
    rejection_reason     VARCHAR(500),

    requested_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at          TIMESTAMP,
    rejected_at          TIMESTAMP,
    expires_at           TIMESTAMP    NOT NULL,

    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_rebrand_approval_status
        CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED'))
);

CREATE INDEX idx_rebrand_approval_target
    ON rebrand_approvals(target_instance_id);
CREATE INDEX idx_rebrand_approval_status
    ON rebrand_approvals(status);
CREATE INDEX idx_rebrand_approval_expires
    ON rebrand_approvals(expires_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_rebrand_approval_deleted
    ON rebrand_approvals(deleted);
