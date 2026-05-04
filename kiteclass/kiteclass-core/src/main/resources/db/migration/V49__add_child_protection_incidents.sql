-- GAP-322 Phase 1A (Wave 18b1 Bucket E): Child-protection incident table.
--
-- Sensitive columns (description, evidence_paths) stored as BYTEA — encrypted
-- at rest via JPA AesGcmAttributeConverter (AES-256-GCM, per-field random IV,
-- 128-bit auth tag). Cipher layout: [IV(12) | ciphertext | auth_tag(16)].
--
-- Compliance:
--   * PDPL Decree 13/2023/NĐ-CP Art 16 — special protection of children's PII
--   * Luật Trẻ em 2016 Đ.6 + Đ.25 — quyền được bảo vệ + vetting nhân sự
--   * Luật Trẻ em 2016 Đ.51 — mandatory reporting ≤24h (Phase 1C banner)
--
-- Phase 1A ships entity + CRUD only. Phase 1B (GAP-322b) adds vetting
-- workflow + MinIO encrypted bucket + RBAC-gated decryption. Phase 1C
-- (GAP-322c) adds Đ.51 banner + hash-chained audit log + 7y retention
-- enforcement.

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- Plaintext title for indexing + admin triage. NON-sensitive.
    title VARCHAR(200) NOT NULL,

    -- Encrypted at rest by AesGcmAttributeConverter — DO NOT query directly
    -- via raw SQL. Layout: [IV(12) | ciphertext | auth_tag(16)].
    description BYTEA,
    evidence_paths BYTEA,

    severity VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REPORTED',

    reporter_user_id BIGINT NOT NULL,
    subject_student_id BIGINT,
    assigned_officer_user_id BIGINT,

    -- BaseEntity audit columns
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_incidents_severity CHECK (
        severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    ),
    CONSTRAINT chk_incidents_category CHECK (
        category IN ('BULLYING', 'ABUSE', 'GROOMING', 'CSAM', 'OTHER')
    ),
    CONSTRAINT chk_incidents_status CHECK (
        status IN ('REPORTED', 'INVESTIGATING', 'ESCALATED', 'RESOLVED', 'CLOSED')
    )
);

CREATE INDEX idx_incidents_instance_id ON incidents(instance_id);
CREATE INDEX idx_incidents_severity ON incidents(severity);
CREATE INDEX idx_incidents_category ON incidents(category);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_reporter ON incidents(reporter_user_id);
CREATE INDEX idx_incidents_subject_student ON incidents(subject_student_id);
CREATE INDEX idx_incidents_deleted ON incidents(deleted);

COMMENT ON TABLE incidents IS
    'GAP-322 Phase 1A: child-protection ticket with encrypted sensitive fields. Compliance: PDPL Decree 13/2023 Art 16 + Luật Trẻ em 2016 Đ.6/25/51. Phase 1B/1C add RBAC, mandatory reporting, hash-chained audit, 7y retention.';
COMMENT ON COLUMN incidents.title IS
    'Plaintext non-sensitive title (≤200 chars). Search-friendly. Sensitive narrative is in description (encrypted).';
COMMENT ON COLUMN incidents.description IS
    'AES-256-GCM encrypted narrative. Layout: [IV(12) | ciphertext | auth_tag(16)]. Decrypted only via AesGcmAttributeConverter on entity read; raw BYTEA query returns ciphertext.';
COMMENT ON COLUMN incidents.evidence_paths IS
    'AES-256-GCM encrypted newline-separated MinIO object keys. Phase 1B encrypts the MinIO bucket itself (GAP-322b).';
COMMENT ON COLUMN incidents.severity IS
    'IncidentSeverity enum: LOW/MEDIUM/HIGH/CRITICAL. CRITICAL+abuse-category triggers Đ.51 banner in Phase 1C.';
COMMENT ON COLUMN incidents.category IS
    'IncidentCategory enum: BULLYING/ABUSE/GROOMING/CSAM/OTHER. CSAM is strictest — Tổng đài 111 + công an mandatory ≤24h per Đ.51 + BLHS Đ.147.';
COMMENT ON COLUMN incidents.status IS
    'IncidentStatus lifecycle. Phase 1A allows arbitrary transitions; Phase 1B locks state machine.';

-- ---------------------------------------------------------------------------
-- Seed SAFEGUARDING_OFFICER as a system role + permission template per
-- Luật Trẻ em 2016 Đ.51 (mandatory reporting) + Đ.25 (vetting). Tenants
-- inherit this role at provisioning time via the existing role-seeder
-- mechanism (V30 schema, Wave-3 RoleSeederService).
--
-- Note: Phase 1A only registers the *system* permission constants — actual
-- per-tenant role rows are created by RoleSeederService at tenant-creation.
-- This migration documents the intent in a system-level instance_id
-- (NIL UUID) which all tenants ignore via the multi-tenant filter; it is
-- consumed by the seeder at startup.
-- ---------------------------------------------------------------------------
INSERT INTO permissions (instance_id, name, description, category, is_system,
                         created_at, updated_at, version, deleted)
VALUES
    ('00000000-0000-0000-0000-000000000000',
     'INCIDENT_READ_DECRYPTED',
     'Decrypt + read sensitive Incident fields. Restricted to safeguarding officer + Hiệu trưởng + designated counselor (Phase 1B RBAC gate).',
     'CHILD_PROTECTION', TRUE,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, FALSE),
    ('00000000-0000-0000-0000-000000000000',
     'INCIDENT_WRITE',
     'Create + update Incident lifecycle. Safeguarding officer scope.',
     'CHILD_PROTECTION', TRUE,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, FALSE),
    ('00000000-0000-0000-0000-000000000000',
     'INCIDENT_REPORT',
     'Submit a new Incident (PH/HS/GV channels — broad grant).',
     'CHILD_PROTECTION', TRUE,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, FALSE)
ON CONFLICT DO NOTHING;

-- System-template role at NIL UUID (instance_id=00000000-...). The
-- RoleSeederService picks this up when provisioning new tenants and clones
-- the row with the tenant's actual instance_id, attaching the 3 permissions
-- above + parent=PRINCIPAL (level 3).
INSERT INTO roles (instance_id, name, description, parent_id, level, is_system,
                   created_at, updated_at, version, deleted)
VALUES
    ('00000000-0000-0000-0000-000000000000',
     'SAFEGUARDING_OFFICER',
     'Cán bộ bảo vệ trẻ em — designated incident handler per Luật Trẻ em 2016 Đ.51 + Decree 56/2017. Decrypts sensitive Incident fields; coordinates mandatory reporting to Tổng đài 111 + công an địa phương.',
     NULL, 3, TRUE,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, FALSE)
ON CONFLICT DO NOTHING;
