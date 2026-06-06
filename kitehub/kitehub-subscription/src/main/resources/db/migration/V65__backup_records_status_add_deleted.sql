-- =========================================================================
-- V65: backup_records.status CHECK constraint — add 'DELETED' (GAP-954)
-- =========================================================================
-- Context: Wave provisioning-1 KC-1 closure walk surfaced a triad drift
-- (design-patterns.md §3.12): the BackupStatus enum
-- (kitehub-subscription/domain/BackupStatus.java) declares
-- {IN_PROGRESS, COMPLETED, FAILED, DELETED}, but V59 created
-- chk_backup_records_status as {IN_PROGRESS, COMPLETED, FAILED, RESTORED}
-- — it included the unused 'RESTORED' value and OMITTED 'DELETED'.
--
-- Impact: InstancePurgeService.executePurge() marks every backup record
-- DELETED after deleting its S3 object (step 4). Because purge REQUIRES at
-- least one COMPLETED backup (safety gate), every real purge hit the missing
-- 'DELETED' value → CHECK violation → @Transactional rollback → HTTP 409
-- RESOURCE_CONFLICT. Purge could therefore NEVER succeed in production for a
-- tenant that has a backup (i.e. every purgeable tenant). Mockito unit tests
-- mocked the repository so the DB constraint never ran — the bug was invisible
-- until a live walk on a real Postgres stack.
--
-- Fix: recreate the constraint as the UNION of the enum values + the legacy
-- 'RESTORED' (kept for forward-compat / any historical rows). Idempotent
-- (DROP IF EXISTS before ADD).
--
-- Breaking change: NO. Widens the allowed set; existing rows unaffected.
-- =========================================================================

ALTER TABLE backup_records
    DROP CONSTRAINT IF EXISTS chk_backup_records_status;

ALTER TABLE backup_records
    ADD CONSTRAINT chk_backup_records_status
    CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'RESTORED', 'DELETED'));
