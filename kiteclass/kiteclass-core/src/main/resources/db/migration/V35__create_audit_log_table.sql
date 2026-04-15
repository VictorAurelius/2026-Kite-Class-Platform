-- =========================================================================
-- V35: audit_log (Wave 4 security foundation)
-- =========================================================================
-- Context: Wave 4 Sub-PR 4.0, ADR-010 + ADR-012 + ADR-013
-- Purpose: Append-only audit trail shared by moderation (4.1), DMCA workflow
--          (4.3), and deletion/retention (4.4). One row per security-relevant
--          action — writes only, no updates or deletes (soft-delete disabled
--          semantically for audit rows).
-- Reservations: V36=4.1 moderation, V37=4.3 legal-ip, V38=4.4 retention,
--               V39=4.5 quality-gate (do NOT consume these for other work).
-- =========================================================================

CREATE TABLE audit_log (
    id               BIGSERIAL PRIMARY KEY,
    instance_id      UUID         NOT NULL,

    action_type      VARCHAR(100) NOT NULL,
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     VARCHAR(100) NOT NULL,
    actor_user_id    BIGINT,
    actor_role       VARCHAR(50),
    payload          JSONB,
    reason           VARCHAR(500),

    -- Base entity audit columns kept so Hibernate filter recognizes the entity,
    -- but `deleted` is not flipped for audit rows (semantic append-only).
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    version          BIGINT       NOT NULL DEFAULT 0,
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_audit_log_action_type ON audit_log(action_type);
CREATE INDEX idx_audit_log_aggregate
    ON audit_log(aggregate_type, aggregate_id);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);
CREATE INDEX idx_audit_log_instance_id ON audit_log(instance_id);
