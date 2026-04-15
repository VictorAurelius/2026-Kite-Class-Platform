-- =========================================================================
-- V33: Outbox events (Transactional Outbox Pattern)
-- =========================================================================
-- Context: GAP-009 deferred, ADR-007, Wave 3 Sub-PR 3.1
-- Purpose: Reliable at-least-once event delivery. Services write domain row
--          + outbox row in same tx; a separate poller publishes to broker.
-- Breaking change: NO (new table)
-- =========================================================================

CREATE TABLE outbox_events (
    id                BIGSERIAL PRIMARY KEY,
    instance_id       UUID         NOT NULL,

    aggregate_type    VARCHAR(100) NOT NULL,
    aggregate_id      VARCHAR(100) NOT NULL,
    event_type        VARCHAR(100) NOT NULL,
    payload           JSONB        NOT NULL,

    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    retry_count       INT          NOT NULL DEFAULT 0,
    last_error        TEXT,

    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at      TIMESTAMP,
    next_attempt_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT       NOT NULL DEFAULT 0,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_outbox_status
        CHECK (status IN ('PENDING','PUBLISHED','FAILED')),
    CONSTRAINT chk_outbox_retry_nonneg
        CHECK (retry_count >= 0)
);

-- Partial index: scheduler scans only rows that need work
CREATE INDEX idx_outbox_pending
    ON outbox_events(next_attempt_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_aggregate
    ON outbox_events(aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_event_type
    ON outbox_events(event_type);

CREATE INDEX idx_outbox_deleted
    ON outbox_events(deleted);
