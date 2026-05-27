-- =====================================================================
-- Wave meta-6 Bucket A — GAP-772: Staff invitation MVP
-- =====================================================================
-- Mirror of V42__create_parent_portal_schema.sql `parent_invitations`
-- block, adapted for staff scope:
--   * No student_id FK (staff is tenant-scoped, not child-scoped)
--   * role column (STAFF/TEACHER/MANAGER) instead of GUARDIAN relationship
--   * accepted_user_id replaces redeemed_parent_id (gateway User row)
--
-- Token-based onboarding (7-day default TTL per
-- kiteclass.staff-invite.invitation-ttl-hours). Public claim endpoint:
-- POST /api/v1/staff-invitations/{token}/accept.
-- =====================================================================

CREATE TABLE staff_invitations (
    id                  BIGSERIAL    PRIMARY KEY,
    instance_id         UUID         NOT NULL,

    email               VARCHAR(255) NOT NULL,
    role                VARCHAR(32)  NOT NULL DEFAULT 'STAFF',
    token               VARCHAR(64)  NOT NULL UNIQUE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    expires_at          TIMESTAMP    NOT NULL,
    invited_by_user_id  BIGINT,
    accepted_at         TIMESTAMP,
    accepted_user_id    BIGINT,

    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_staff_invitation_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT chk_staff_invitation_role
        CHECK (role IN ('STAFF', 'TEACHER', 'MANAGER'))
);

CREATE INDEX idx_staff_inv_email    ON staff_invitations (email);
CREATE INDEX idx_staff_inv_status   ON staff_invitations (status);
CREATE INDEX idx_staff_inv_instance ON staff_invitations (instance_id);
-- Partial index — only pending invitations need expiry scans.
CREATE INDEX idx_staff_inv_expires_pending
    ON staff_invitations (expires_at)
    WHERE status = 'PENDING';

COMMENT ON TABLE staff_invitations IS
    'Token-based staff onboarding invitations — Owner provisions STAFF/TEACHER/MANAGER role at tenant (Wave meta-6, GAP-772).';
