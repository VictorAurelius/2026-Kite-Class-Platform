-- =====================================================================
-- Wave 2 — GAP-052a: Parent portal identity + invitation MVP
-- =====================================================================
-- Introduces three tables:
--   * parents               — parent/guardian profile
--   * parent_student_links  — many-to-many edges, per-edge link_type
--   * parent_invitations    — token-based onboarding (24h default TTL)
--
-- All tables extend the BaseEntity contract: instance_id (tenant), audit
-- timestamps, created_by / updated_by (BIGINT user ids), soft-delete, and
-- optimistic-lock version. JPA auditing fills these at persist/update
-- time, so the DDL only needs the columns + defaults.
--
-- Messaging, fee payment, attendance / grade widgets follow in Wave 5 —
-- this migration is deliberately minimal.
-- =====================================================================

-- ---- parents --------------------------------------------------------
CREATE TABLE parents (
    id              BIGSERIAL PRIMARY KEY,
    instance_id     UUID         NOT NULL,

    email           VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(20),
    full_name       VARCHAR(100) NOT NULL,
    relationship    VARCHAR(20)  NOT NULL DEFAULT 'GUARDIAN',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_parents_email_tenant UNIQUE (instance_id, email),
    CONSTRAINT chk_parents_relationship
        CHECK (relationship IN ('FATHER', 'MOTHER', 'GUARDIAN')),
    CONSTRAINT chk_parents_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_parents_email    ON parents (email);
CREATE INDEX idx_parents_instance ON parents (instance_id);
CREATE INDEX idx_parents_status   ON parents (status);

-- ---- parent_student_links ------------------------------------------
-- Unique on (parent_id, student_id) so we can never accidentally create
-- duplicate edges when a sibling redemption re-links the same pair.
CREATE TABLE parent_student_links (
    id              BIGSERIAL PRIMARY KEY,
    instance_id     UUID     NOT NULL,

    parent_id       BIGINT   NOT NULL REFERENCES parents (id),
    student_id      BIGINT   NOT NULL REFERENCES students (id),
    link_type       VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN   NOT NULL DEFAULT FALSE,
    version         BIGINT    NOT NULL DEFAULT 0,

    CONSTRAINT uk_parent_student       UNIQUE (parent_id, student_id),
    CONSTRAINT chk_psl_link_type       CHECK (link_type IN ('PRIMARY', 'SECONDARY'))
);

CREATE INDEX idx_psl_parent   ON parent_student_links (parent_id);
CREATE INDEX idx_psl_student  ON parent_student_links (student_id);
CREATE INDEX idx_psl_instance ON parent_student_links (instance_id);

-- ---- parent_invitations --------------------------------------------
-- Token is globally unique (128-bit UUID) — we rely on it as the public
-- redemption key. A partial index on pending rows accelerates the
-- hourly sweeper without bloating storage for already-resolved invites.
CREATE TABLE parent_invitations (
    id                  BIGSERIAL PRIMARY KEY,
    instance_id         UUID         NOT NULL,

    email               VARCHAR(255) NOT NULL,
    student_id          BIGINT       NOT NULL REFERENCES students (id),
    token               VARCHAR(64)  NOT NULL UNIQUE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    expires_at          TIMESTAMP    NOT NULL,
    invited_by_user_id  BIGINT,
    redeemed_at         TIMESTAMP,
    redeemed_parent_id  BIGINT       REFERENCES parents (id),

    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_parent_invitation_status
        CHECK (status IN ('PENDING', 'REDEEMED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_inv_email    ON parent_invitations (email);
CREATE INDEX idx_inv_status   ON parent_invitations (status);
CREATE INDEX idx_inv_instance ON parent_invitations (instance_id);
-- Partial index — only pending invitations need expiry scans.
CREATE INDEX idx_inv_expires_pending
    ON parent_invitations (expires_at)
    WHERE status = 'PENDING';

COMMENT ON TABLE parents               IS 'Parent / guardian profiles — identity lives in gateway users.';
COMMENT ON TABLE parent_student_links  IS 'Many-to-many edges between parents and students with link metadata.';
COMMENT ON TABLE parent_invitations    IS 'Token-based parent onboarding invitations (Wave 2, GAP-052a).';
