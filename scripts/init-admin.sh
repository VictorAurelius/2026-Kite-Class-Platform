#!/bin/bash
#
# Initialize Admin User Script
# Creates default admin user in the database
#

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}🔑 Initialize Admin User${NC}"
echo -e "${BLUE}================================${NC}\n"

# Check if PostgreSQL container is running
if ! docker ps | grep -q kiteclass-postgres; then
    echo -e "${RED}❌ PostgreSQL container is not running!${NC}"
    echo -e "${YELLOW}Start it with: ./scripts/dev-docker.sh up${NC}"
    exit 1
fi

echo -e "${YELLOW}📊 Creating database schema...${NC}\n"

# Run migrations SQL directly
docker exec kiteclass-postgres psql -U kiteclass -d kiteclass_dev <<'EOSQL'
-- Create users table if not exists
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    user_type VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    reference_id BIGINT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- Insert default roles
INSERT INTO roles (code, name, description, is_system) VALUES
('OWNER', 'Chủ trung tâm', 'Full access to all features', TRUE),
('ADMIN', 'Quản trị viên', 'Manage users and system', TRUE),
('TEACHER', 'Giáo viên', 'Manage classes and students', TRUE),
('STAFF', 'Nhân viên', 'Limited access', TRUE)
ON CONFLICT (code) DO NOTHING;

-- Insert default admin user
-- Password: Admin@123 (BCrypt hash)
INSERT INTO users (email, password_hash, name, status, user_type, email_verified, created_at, updated_at)
VALUES (
    'admin@kiteclass.local',
    '$2a$10$qnMxZXjJVkC5wDjzVQ5yH.nXf7jCQPxXBdJ3nqYOXd9Q8DGXtLlNa',
    'KiteClass Admin',
    'ACTIVE',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    status = 'ACTIVE',
    email_verified = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- Assign OWNER role to admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@kiteclass.local' AND r.code = 'OWNER'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Show result
SELECT
    u.id,
    u.email,
    u.name,
    u.status,
    u.user_type,
    r.code as role
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'admin@kiteclass.local';
EOSQL

echo -e "\n${GREEN}✅ Admin user created successfully!${NC}\n"
echo -e "${BLUE}📋 Login Credentials:${NC}"
echo -e "  Email:    ${GREEN}admin@kiteclass.local${NC}"
echo -e "  Password: ${GREEN}Admin@123${NC}\n"
echo -e "${YELLOW}🌐 Login at: http://localhost:3000${NC}\n"
