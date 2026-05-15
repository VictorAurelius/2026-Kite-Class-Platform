-- V49: Wave 80 Bucket B (GAP-561b) — Staff Invitation audit log.
--
-- Audit trail for every state transition of a staff_invitations row per
-- OWASP A09 (Security Logging & Monitoring Failures) admin audit log
-- requirement (`.claude/rules/pre-launch-auth-hardening-checklist.md` §2.7).
--
-- Event types: CREATED / SENT / RESENT / ACCEPTED / REVOKED / EXPIRED.
-- Enum whitelisted at app layer (StaffInvitationAuditEntry.EventType) and DB
-- check constraint below.
--
-- Migration ordering: V49 (this) MUST run AFTER V45__create_staff_invitations.sql
-- (parent table reference) and AFTER V46__create_rbac_roles.sql (role enum
-- compatibility window). Idempotent — uses IF NOT EXISTS guards.

CREATE TABLE IF NOT EXISTS staff_invitation_audit_log (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id   UUID         NOT NULL,
    tenant_id       UUID         NOT NULL,
    email           VARCHAR(255) NOT NULL,
    event_type      VARCHAR(32)  NOT NULL,
    actor_user_id   UUID,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    details         VARCHAR(512),
    CONSTRAINT ck_staff_invitation_audit_event_type
        CHECK (event_type IN ('CREATED', 'SENT', 'RESENT', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_staff_invitation_audit_invitation
    ON staff_invitation_audit_log (invitation_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_staff_invitation_audit_tenant
    ON staff_invitation_audit_log (tenant_id, occurred_at DESC);

COMMENT ON TABLE staff_invitation_audit_log IS
    'Wave 80 GAP-561b — append-only audit trail for staff_invitations lifecycle. One row per state transition.';

COMMENT ON COLUMN staff_invitation_audit_log.event_type IS
    'Lifecycle event: CREATED (owner issues) | SENT (email dispatched) | RESENT (re-dispatch) | ACCEPTED | REVOKED | EXPIRED.';

COMMENT ON COLUMN staff_invitation_audit_log.actor_user_id IS
    'Owner who triggered the action. NULL for system events (ACCEPTED by recipient, EXPIRED by reaper).';
