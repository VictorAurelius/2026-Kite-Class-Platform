-- V89: KC-native auth credentials for tenant-scoped roles (PARENT/TEACHER/STUDENT).
-- Wave auth-1 (Option B) — pull-forward parent/student/teacher login per GAP-725.
--
-- Why standalone (not tenant-RLS-scoped): login lookup happens PRE-auth — no
-- TenantContext / app.current_tenant_id GUC is set yet. The credential row IS the
-- source of tenant binding (instance_id) + reference id (entity_id). So this table
-- is intentionally a global-email lookup, NOT row-level-security scoped.

CREATE TABLE IF NOT EXISTS auth_credentials (
    id            BIGSERIAL PRIMARY KEY,
    user_uuid     UUID         NOT NULL,                 -- sub / X-User-Id (audit)
    entity_type   VARCHAR(16)  NOT NULL,                 -- PARENT | TEACHER | STUDENT
    entity_id     BIGINT       NOT NULL,                 -- referenceId: parents.id / teachers.id / students.id
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(72)  NOT NULL,                 -- BCrypt
    instance_id   UUID         NOT NULL,                 -- tenantId
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT uk_auth_credentials_email UNIQUE (email),
    CONSTRAINT uk_auth_credentials_user_uuid UNIQUE (user_uuid),
    CONSTRAINT ck_auth_credentials_entity_type CHECK (entity_type IN ('PARENT', 'TEACHER', 'STUDENT'))
);

CREATE INDEX IF NOT EXISTS ix_auth_credentials_entity ON auth_credentials (entity_type, entity_id);

-- Dev seed: test parent (parent1@test.com / Test@1234) → parents.id=1, instance aaaabbbb-…0001.
-- Mirrors KC-8 G1 walk fixture (parent 1 → child 1). BCrypt cost 10.
INSERT INTO auth_credentials (user_uuid, entity_type, entity_id, email, password_hash, instance_id)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'PARENT',
    1,
    'parent1@test.com',
    '$2b$10$ODyD.log0U.SKxS0elDbs.WRSYWFX1tw5/XkvBvfunfAAecTMn14i',
    'aaaabbbb-0000-0000-0000-000000000001'
)
ON CONFLICT (email) DO NOTHING;
