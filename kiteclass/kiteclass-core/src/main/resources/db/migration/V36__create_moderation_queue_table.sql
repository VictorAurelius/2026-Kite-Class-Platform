-- =========================================================================
-- V36: moderation_queue (Wave 4 Sub-PR 4.1, GAP-018, ADR-010)
-- =========================================================================
-- Context: Content moderation pipeline (Stage 1 auto + Stage X human review).
-- Purpose: Persist every non-approved moderation outcome so admin UI can
--          adjudicate NEEDS_HUMAN_REVIEW rows and audit auto-REJECTED rows.
-- Reservations: V35=audit_log (4.0), V37=4.3 legal-ip, V38=4.4 retention,
--               V39=4.5 quality-gate.
-- =========================================================================

CREATE TABLE moderation_queue (
    id                     BIGSERIAL PRIMARY KEY,
    instance_id            UUID         NOT NULL,

    target_type            VARCHAR(100) NOT NULL,
    target_id              VARCHAR(100) NOT NULL,
    status                 VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    score                  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    flagged_keywords       JSONB,
    reason                 VARCHAR(500),
    assigned_reviewer_id   BIGINT,
    decided_at             TIMESTAMP,

    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    version                BIGINT       NOT NULL DEFAULT 0,
    deleted                BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_moderation_status
        CHECK (status IN ('PENDING','APPROVED','REJECTED','NEEDS_HUMAN_REVIEW'))
);

CREATE INDEX idx_moderation_status ON moderation_queue(status);
CREATE INDEX idx_moderation_target ON moderation_queue(target_type, target_id);
CREATE INDEX idx_moderation_deleted ON moderation_queue(deleted);
CREATE INDEX idx_moderation_instance_id ON moderation_queue(instance_id);
