-- GAP-192 Phase 4b-i — Idempotency-key persistence for trial-to-paid migration.
-- Stores UpgradeRequest.idempotencyKey for 10 minutes so duplicate client retries
-- return the original 202 response instead of starting a second migration.
-- See: documents/01-business/kitehub/trial-to-paid-migration/api-contract.md
-- (POST /instances/{id}/upgrade — "Idempotency: idempotencyKey persisted; duplicate request within 10 minutes returns original 202 response.")

CREATE TABLE migration_idempotency_key (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL,
    instance_id UUID NOT NULL,
    response_phase VARCHAR(32) NOT NULL,
    response_started_at TIMESTAMP NOT NULL,
    response_poll_url VARCHAR(255) NOT NULL,
    response_estimated_completion_seconds INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_idempotency_key_instance
        FOREIGN KEY (instance_id) REFERENCES instances (id),
    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key, instance_id)
);

CREATE INDEX idx_idempotency_key_expires
    ON migration_idempotency_key (expires_at);

CREATE INDEX idx_idempotency_key_lookup
    ON migration_idempotency_key (idempotency_key, instance_id);

COMMENT ON TABLE migration_idempotency_key IS
    'GAP-192 Phase 4b-i — 10-minute idempotency store for upgrade-initiation requests. Purged by MigrationScheduler.purgeExpiredIdempotencyKeys().';
