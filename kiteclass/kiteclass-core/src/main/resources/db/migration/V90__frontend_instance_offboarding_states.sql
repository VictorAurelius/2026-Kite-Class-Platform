-- V90: frontend_instances off-boarding states (GAP-954 — PDPL Art 23 tenant DELETE cascade)
--
-- Adds SUSPENDED + DELETED to the status CHECK constraint defined in V31, and two timestamp
-- columns tracking the off-boarding lifecycle:
--   - suspended_at: when a DEPLOYED tenant was SUSPENDED (subscription expired / payment failed)
--   - deleted_at:   when a tenant was soft-DELETED — starts the 30-day retention grace before the
--                   cross-service hard purge runs in kitehub-subscription InstancePurgeService.
--
-- FSM (FrontendInstanceStatus): DEPLOYED → SUSPENDED ⇄ DEPLOYED; SUSPENDED → DELETED (terminal).
-- Existing rows keep their current status; the relaxed CHECK is backward-compatible.

ALTER TABLE frontend_instances
    DROP CONSTRAINT IF EXISTS chk_frontend_instance_status;

ALTER TABLE frontend_instances
    ADD CONSTRAINT chk_frontend_instance_status
        CHECK (status IN ('NOT_STARTED','INITIALIZING','GENERATING',
                          'DEPLOYED','REGENERATING','FAILED',
                          'SUSPENDED','DELETED'));

ALTER TABLE frontend_instances
    ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_at   TIMESTAMP;
