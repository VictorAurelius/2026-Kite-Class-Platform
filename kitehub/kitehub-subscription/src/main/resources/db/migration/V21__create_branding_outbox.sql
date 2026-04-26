-- GAP-222a Phase 2 — Per-module domain outbox for kitehub-branding events.
-- Mirrors V19 migration_outbox shape. Dispatcher is deferred (Exception A pattern
-- per design-patterns.md §3.5.1: outbox-row + best-effort fast-path publish).
-- See ADR-021 for the per-module-vs-shared-lib decision.
--
-- Note: kitehub-branding does not run Flyway itself; this DDL lives here because
-- kitehub-subscription owns the kitehub-schema migration timeline.

CREATE TABLE branding_outbox (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    topic VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at TIMESTAMP
);

CREATE INDEX idx_branding_outbox_undispatched
    ON branding_outbox (created_at)
    WHERE dispatched_at IS NULL;

CREATE INDEX idx_branding_outbox_aggregate
    ON branding_outbox (aggregate_id);

COMMENT ON TABLE branding_outbox IS
    'GAP-222a per-module outbox for branding events. Written in same txn as BrandingJob mutation. Exception A pattern (outbox + fast-path) per design-patterns.md §3.5.1.';
