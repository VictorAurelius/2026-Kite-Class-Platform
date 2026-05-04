-- GAP-322b Phase 1B foundation (Wave 18b2 Bucket B): staff vetting workflow.
--
-- Sister of V49 (incidents). One row per teacher per vetting cycle. Sensitive
-- fields (lltp_number, police_check_details) stored as BYTEA — encrypted at
-- rest by AesGcmAttributeConverter (same converter used by Incident).
--
-- Compliance:
--   * Decree 56/2017/NĐ-CP §Đ.25 — vetting nhân sự mandate for adults working
--     with minors.
--   * Luật Trẻ em 2016 Đ.25 — quyền được bảo vệ; staff who interact with
--     children must be vetted.
--   * PDPL Decree 13/2023/NĐ-CP Art 16 — encryption-at-rest for special-
--     protection child-related personal data; same standard applied to staff
--     LLTP since the certificate references criminal-record check on someone
--     who works with minors.
--
-- Phase 1B foundation scope: schema + entity + service-level state machine +
-- MinIO storage hook (stub) + RBAC gate. Concrete file-upload UI, verify-
-- queue UI, MinIO SDK wiring, 7-year retention enforcement, anti-delete on
-- REJECTED, Tổng đài 111 webhook deferred to Phase 1B follow-up + Phase 1C
-- (GAP-322c).

CREATE TABLE vettings (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- FK to users.id (the teacher being vetted). Plaintext for query
    -- efficiency + tenant scoping. Soft-delete preserves the row.
    teacher_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- Encrypted at rest by AesGcmAttributeConverter. DO NOT query directly
    -- via raw SQL. Layout: [IV(12) | ciphertext | auth_tag(16)].
    lltp_number BYTEA,
    police_check_details BYTEA,

    submitted_at TIMESTAMP,
    interviewed_at TIMESTAMP,
    decided_at TIMESTAMP,
    expires_at TIMESTAMP,
    decided_by_user_id BIGINT,

    -- BaseEntity audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_vettings_status CHECK (
        status IN ('PENDING', 'SUBMITTED', 'INTERVIEW_DONE',
                   'APPROVED', 'REJECTED', 'EXPIRED')
    )
);

CREATE INDEX idx_vettings_instance_id ON vettings(instance_id);
CREATE INDEX idx_vettings_teacher_id ON vettings(teacher_id);
CREATE INDEX idx_vettings_status ON vettings(status);
CREATE INDEX idx_vettings_deleted ON vettings(deleted);
CREATE INDEX idx_vettings_expires_at ON vettings(expires_at);

COMMENT ON TABLE vettings IS
    'GAP-322b Phase 1B foundation: staff vetting record per Decree 56/2017 + Luật Trẻ em 2016 Đ.25. Sensitive fields encrypted via AesGcmAttributeConverter. State machine enforced at service layer (VettingServiceImpl); BR-VETTING-001 transitions: PENDING→SUBMITTED→INTERVIEW_DONE→APPROVED|REJECTED; APPROVED→EXPIRED.';
COMMENT ON COLUMN vettings.lltp_number IS
    'AES-256-GCM encrypted LLTP số 2 document identifier. Layout: [IV(12) | ciphertext | auth_tag(16)]. Decrypted only via AesGcmAttributeConverter on entity read; raw BYTEA query returns ciphertext.';
COMMENT ON COLUMN vettings.police_check_details IS
    'AES-256-GCM encrypted narrative outcome of police check / interview. Restricted decryption to SAFEGUARDING_OFFICER (BR-VETTING-003) at controller layer.';
COMMENT ON COLUMN vettings.status IS
    'VettingStatus enum: PENDING/SUBMITTED/INTERVIEW_DONE/APPROVED/REJECTED/EXPIRED. State machine in VettingServiceImpl rejects illegal transitions with VETTING_INVALID_TRANSITION.';
COMMENT ON COLUMN vettings.expires_at IS
    'Expiry date for an APPROVED vetting (BR-VETTING-001). Cron + reminder ships Phase 1B follow-up. Per Decree 56/2017 + LLTP ≤2 years cadence.';
