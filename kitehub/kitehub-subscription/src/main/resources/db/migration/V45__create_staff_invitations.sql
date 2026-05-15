-- V45: Wave 79 Bucket B (GAP-561) — Staff Invitations table.
--
-- Persona P3 Manager enablement: Owner invites Staff via email; recipient
-- accepts via tokenized link → sets password → first login → dashboard scoped
-- by STAFF role (see V46 + RBAC enforcement).
--
-- Schema source-of-truth: documents/01-business/roles/api-contract.md
-- (Wave 79 Bucket 0 Foundation contract — committed in PR #1364).
--
-- Migration ordering: V45 (this) MUST run BEFORE V46__create_rbac_roles.sql
-- per .claude/rules/concurrent-production-mutation-ops.md (serialize schema
-- changes touching shared role enumeration).

CREATE TABLE IF NOT EXISTS staff_invitations (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL,
    email               VARCHAR(255) NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    invited_by          UUID         NOT NULL,
    token_hash          VARCHAR(255) NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    accepted_at         TIMESTAMPTZ,
    accepted_user_id    UUID,
    revoked_at          TIMESTAMPTZ,
    revoked_by          UUID,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_staff_invitations_token UNIQUE (token_hash),
    CONSTRAINT ck_staff_invitations_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_staff_invitations_tenant_status
    ON staff_invitations (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_staff_invitations_email_pending
    ON staff_invitations (tenant_id, email)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_staff_invitations_expires_at
    ON staff_invitations (expires_at)
    WHERE status = 'PENDING';

COMMENT ON TABLE staff_invitations IS
    'Wave 79 GAP-561 — Owner→Staff invitation flow. token_hash is SHA-256 of opaque token sent in email; raw token never persisted.';

COMMENT ON COLUMN staff_invitations.status IS
    'Lifecycle: PENDING → ACCEPTED (recipient set password) | EXPIRED (TTL passed) | REVOKED (Owner cancelled). Whitelisted by StaffInvitationStatus enum.';

COMMENT ON COLUMN staff_invitations.token_hash IS
    'SHA-256 hex of single-use invitation token. Raw token sent in email link; comparing requires re-hashing the URL token at accept time.';

COMMENT ON COLUMN staff_invitations.expires_at IS
    'Default 7 days after created_at (BR-ROLE-INVITE-TTL). Recipient must accept before this timestamp.';
