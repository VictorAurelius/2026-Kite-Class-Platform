-- =========================================================================
-- V30: Role Hierarchy + Granular Permissions
-- =========================================================================
-- Context: GAP-058, ADR-003
-- Purpose: Replace flat roles with hierarchical tree + permission bundles
-- Breaking change: NO (additive — existing role enums continue to work
--                   until full migration to this system)
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. permissions table
-- -------------------------------------------------------------------------
CREATE TABLE permissions (
    id             BIGSERIAL PRIMARY KEY,
    instance_id    UUID         NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(300),
    category       VARCHAR(50),
    is_system      BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX idx_permission_name
    ON permissions(instance_id, name) WHERE deleted = FALSE;
CREATE INDEX idx_permission_category ON permissions(category);
CREATE INDEX idx_permission_deleted ON permissions(deleted);

-- -------------------------------------------------------------------------
-- 2. roles table (hierarchical)
-- -------------------------------------------------------------------------
CREATE TABLE roles (
    id             BIGSERIAL PRIMARY KEY,
    instance_id    UUID         NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    description    VARCHAR(300),
    parent_id      BIGINT       REFERENCES roles(id),
    level          INT          NOT NULL DEFAULT 5,
    is_system      BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_role_level CHECK (level >= 1 AND level <= 10)
);

CREATE UNIQUE INDEX idx_role_name
    ON roles(instance_id, name) WHERE deleted = FALSE;
CREATE INDEX idx_role_parent ON roles(parent_id);
CREATE INDEX idx_role_level ON roles(level);
CREATE INDEX idx_role_deleted ON roles(deleted);

-- -------------------------------------------------------------------------
-- 3. role_permissions (many-to-many)
-- -------------------------------------------------------------------------
CREATE TABLE role_permissions (
    role_id        BIGINT NOT NULL REFERENCES roles(id),
    permission_id  BIGINT NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_perm_role ON role_permissions(role_id);
CREATE INDEX idx_role_perm_permission ON role_permissions(permission_id);

-- -------------------------------------------------------------------------
-- 4. user_roles (many-to-many user ↔ role)
-- -------------------------------------------------------------------------
CREATE TABLE user_roles (
    id             BIGSERIAL PRIMARY KEY,
    instance_id    UUID         NOT NULL,
    user_id        BIGINT       NOT NULL,
    role_id        BIGINT       NOT NULL REFERENCES roles(id),
    assigned_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by    VARCHAR(100),
    notes          VARCHAR(500),

    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX idx_ur_user_role
    ON user_roles(user_id, role_id) WHERE deleted = FALSE;
CREATE INDEX idx_ur_role ON user_roles(role_id);
CREATE INDEX idx_ur_instance_id ON user_roles(instance_id);
CREATE INDEX idx_ur_deleted ON user_roles(deleted);

-- -------------------------------------------------------------------------
-- Note: System permissions and role templates seeded at application startup
-- via RoleSeederService (runs on per-tenant basis after tenant creation).
-- -------------------------------------------------------------------------
