-- =========================================================================
-- V37: DMCA takedown requests (Wave 4 Sub-PR 4.3, GAP-042)
-- =========================================================================
-- Context: ADR-012 Track 2 (reactive DMCA workflow)
-- Purpose: Persist public DMCA intake + review workflow
--          (PENDING → REVIEWING → VALID / INVALID → EXECUTED / CONTESTED).
--          Paired with audit_log (V35) rows for the §512 safe-harbor trail.
-- =========================================================================

CREATE TABLE dmca_takedown_requests (
    id                              BIGSERIAL PRIMARY KEY,
    instance_id                     UUID          NOT NULL,

    reporter_email                  VARCHAR(255)  NOT NULL,
    reporter_name                   VARCHAR(255)  NOT NULL,
    alleged_infringing_url          VARCHAR(2000) NOT NULL,
    copyrighted_work_description    VARCHAR(4000) NOT NULL,

    status                          VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    counter_notice_email            VARCHAR(255),
    reviewer_user_id                BIGINT,
    reviewed_at                     TIMESTAMP,
    executed_at                     TIMESTAMP,
    contested_at                    TIMESTAMP,
    rejection_reason                VARCHAR(500),

    created_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(100),
    updated_by                      VARCHAR(100),
    version                         BIGINT        NOT NULL DEFAULT 0,
    deleted                         BOOLEAN       NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_dmca_takedown_status
        CHECK (status IN ('PENDING','REVIEWING','VALID','INVALID','EXECUTED','CONTESTED'))
);

CREATE INDEX idx_dmca_takedown_status
    ON dmca_takedown_requests(status);
CREATE INDEX idx_dmca_takedown_deleted
    ON dmca_takedown_requests(deleted);
CREATE INDEX idx_dmca_takedown_reporter_email
    ON dmca_takedown_requests(reporter_email);
