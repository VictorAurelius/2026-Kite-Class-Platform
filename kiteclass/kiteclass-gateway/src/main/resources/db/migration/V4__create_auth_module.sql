-- =====================================================
-- KiteClass Gateway - Auth Module Schema
-- Version: V4
-- Description: Creates refresh_tokens and role_permissions tables
-- =====================================================

-- Create role_permissions join table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- Create indexes on role_permissions table
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes on refresh_tokens table
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- =====================================================
-- Seed Role Permissions
-- =====================================================

-- OWNER has all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'OWNER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ADMIN has most permissions (except USER:DELETE, ROLE:DELETE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code NOT IN ('USER:DELETE', 'ROLE:DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TEACHER has class and attendance permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'TEACHER'
  AND p.code IN (
    'STUDENT:READ', 'STUDENT:LIST',
    'CLASS:READ', 'CLASS:LIST',
    'ATTENDANCE:TAKE', 'ATTENDANCE:READ', 'ATTENDANCE:UPDATE'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- STAFF has limited read permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'STAFF'
  AND p.code IN (
    'STUDENT:READ', 'STUDENT:LIST',
    'CLASS:READ', 'CLASS:LIST',
    'ATTENDANCE:READ',
    'INVOICE:READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PARENT has very limited read permissions (only for their children)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'PARENT'
  AND p.code IN (
    'STUDENT:READ',
    'ATTENDANCE:READ',
    'INVOICE:READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- =====================================================
-- Create default owner user (if not exists)
-- Password: Admin@123
-- =====================================================
INSERT INTO users (email, password_hash, name, status, email_verified)
VALUES (
    'owner@kiteclass.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye.IhQRnO.xbK0l.lqZRvNT2TcMmwCJIG',
    'System Owner',
    'ACTIVE',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

-- Assign OWNER role to default user
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN roles r
WHERE u.email = 'owner@kiteclass.local'
  AND r.code = 'OWNER'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =====================================================
-- Comments for documentation
-- =====================================================
COMMENT ON TABLE role_permissions IS 'Many-to-many relationship between roles and permissions';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for authentication';

COMMENT ON COLUMN refresh_tokens.token IS 'JWT refresh token string';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp (default 7 days)';
