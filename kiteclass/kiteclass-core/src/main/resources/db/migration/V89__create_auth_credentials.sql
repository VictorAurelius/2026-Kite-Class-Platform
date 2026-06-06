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

-- Schema only — NO seed. Credentials are provisioned at runtime by
-- AuthCredentialProvisioningService (parent-invitation redeem, etc.). Test
-- fixtures belong in kiteclass/scripts/seed-data.sh, never a production migration
-- (a known-password row must not ship to prod).
