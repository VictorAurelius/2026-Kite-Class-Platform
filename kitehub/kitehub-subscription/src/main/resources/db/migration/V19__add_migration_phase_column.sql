-- GAP-192 Phase 4a — Trial → Paid migration state machine.
-- Adds migration_phase sub-state + observability timestamps to instance table.
-- See: documents/01-business/kitehub/trial-to-paid-migration/rules.md §3

ALTER TABLE instances
    ADD COLUMN migration_phase VARCHAR(32) NOT NULL DEFAULT 'NONE',
    ADD COLUMN migration_started_at TIMESTAMP,
    ADD COLUMN migration_completed_at TIMESTAMP,
    ADD COLUMN migration_failure_reason VARCHAR(500);

-- Index on phase for queue-style queries (e.g. find all PAYMENT_CAPTURED instances to process).
CREATE INDEX idx_instances_migration_phase
    ON instances (migration_phase)
    WHERE migration_phase <> 'NONE';

-- Constraint: migration_phase values must be within the known enum.
-- Enforced at app layer via MigrationPhase enum; check constraint acts as a defense in depth.
ALTER TABLE instances
    ADD CONSTRAINT chk_instances_migration_phase
    CHECK (migration_phase IN (
        'NONE', 'INITIATED', 'PAYMENT_PENDING', 'PAYMENT_CAPTURED',
        'MIGRATING', 'COMPLETED', 'REVERSED', 'MIGRATION_FAILED'
    ));

-- Outbox table for migration-domain events. Minimal schema — dispatcher belongs to a future PR.
-- See rules.md §5 for the 7 event types published via this table.
CREATE TABLE migration_outbox (
    id UUID PRIMARY KEY,
    instance_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    topic VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at TIMESTAMP,
    CONSTRAINT fk_migration_outbox_instance
        FOREIGN KEY (instance_id) REFERENCES instances (id)
);

CREATE INDEX idx_migration_outbox_undispatched
    ON migration_outbox (created_at)
    WHERE dispatched_at IS NULL;

CREATE INDEX idx_migration_outbox_instance
    ON migration_outbox (instance_id);

COMMENT ON TABLE migration_outbox IS
    'GAP-192 outbox for trial-to-paid events. Written in same txn as Instance mutation. Dispatcher deferred to Phase 4b.';
COMMENT ON COLUMN instances.migration_phase IS
    'GAP-192 trial-to-paid migration sub-state. NONE when no migration in flight.';
