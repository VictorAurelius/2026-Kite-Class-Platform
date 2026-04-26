-- GAP-222c — Generalize migration_outbox → subscription_outbox.
--
-- Per ADR-021 the per-MODULE domain outbox is one table per module. V19 introduced
-- migration_outbox specifically for trial-to-paid events; this migration broadens it
-- so InstancePurgeService + EmailServiceClient (Exception A migrations) share the same
-- outbox row contract.
--
-- Changes:
--   1. Rename table migration_outbox → subscription_outbox
--   2. Drop FK fk_migration_outbox_instance — email events have null instanceId,
--      and purge events should survive instance deletion (outbox rows are forensic)
--   3. Drop NOT NULL on instance_id (email events publish without instance binding)
--   4. Rename indexes to match new table name
--
-- See design-patterns.md §3.5.1 Exception A for the outbox+fast-path contract.

ALTER TABLE migration_outbox
    DROP CONSTRAINT fk_migration_outbox_instance;

ALTER TABLE migration_outbox
    ALTER COLUMN instance_id DROP NOT NULL;

ALTER TABLE migration_outbox
    RENAME TO subscription_outbox;

ALTER INDEX idx_migration_outbox_undispatched
    RENAME TO idx_subscription_outbox_undispatched;

ALTER INDEX idx_migration_outbox_instance
    RENAME TO idx_subscription_outbox_instance;

COMMENT ON TABLE subscription_outbox IS
    'GAP-222c per-module outbox for kitehub-subscription cross-service events. Originally migration-only (V19); extended to cover purge + email events. Exception A pattern (outbox + best-effort fast-path) per design-patterns.md §3.5.1.';
