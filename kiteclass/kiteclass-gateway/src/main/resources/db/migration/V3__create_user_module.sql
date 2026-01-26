-- =====================================================
-- KiteClass Gateway - User Module Schema
-- Version: V3
-- Description: Creates users, roles, permissions, and user_roles tables
-- =====================================================

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'LOCKED')),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Create indexes on users table
CREATE INDEX idx_users_email ON users(email) WHERE deleted = FALSE;
CREATE INDEX idx_users_status ON users(status) WHERE deleted = FALSE;
CREATE INDEX idx_users_deleted ON users(deleted);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_role_code CHECK (code ~ '^[A-Z_]+$')
);

-- Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_permission_code CHECK (code ~ '^[A-Z_:]+$')
);

-- Create user_roles join table
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- Create indexes on user_roles table
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- =====================================================
-- Seed Default Roles
-- =====================================================
INSERT INTO roles (code, name, description, is_system) VALUES
('OWNER', 'Chủ trung tâm', 'Full access to all features and settings', TRUE),
('ADMIN', 'Quản trị viên', 'Manage users, classes, billing, and reports', TRUE),
('TEACHER', 'Giáo viên', 'Manage assigned classes, attendance, and student progress', TRUE),
('STAFF', 'Nhân viên', 'Limited access based on assigned permissions', TRUE),
('PARENT', 'Phụ huynh', 'View children information and invoices', TRUE)
ON CONFLICT (code) DO NOTHING;

-- =====================================================
-- Seed Basic Permissions
-- =====================================================
-- User Management Permissions
INSERT INTO permissions (code, name, module, description) VALUES
('USER:CREATE', 'Tạo người dùng', 'USER', 'Create new users'),
('USER:READ', 'Xem người dùng', 'USER', 'View user details'),
('USER:UPDATE', 'Cập nhật người dùng', 'USER', 'Update user information'),
('USER:DELETE', 'Xóa người dùng', 'USER', 'Delete users'),
('USER:LIST', 'Danh sách người dùng', 'USER', 'List all users'),

-- Role Management Permissions
('ROLE:ASSIGN', 'Phân quyền', 'USER', 'Assign roles to users'),
('ROLE:CREATE', 'Tạo vai trò', 'USER', 'Create custom roles'),
('ROLE:UPDATE', 'Cập nhật vai trò', 'USER', 'Update role information'),
('ROLE:DELETE', 'Xóa vai trò', 'USER', 'Delete custom roles'),

-- Student Management Permissions
('STUDENT:CREATE', 'Tạo học sinh', 'STUDENT', 'Create new students'),
('STUDENT:READ', 'Xem học sinh', 'STUDENT', 'View student details'),
('STUDENT:UPDATE', 'Cập nhật học sinh', 'STUDENT', 'Update student information'),
('STUDENT:DELETE', 'Xóa học sinh', 'STUDENT', 'Delete students'),
('STUDENT:LIST', 'Danh sách học sinh', 'STUDENT', 'List all students'),

-- Class Management Permissions
('CLASS:CREATE', 'Tạo lớp học', 'CLASS', 'Create new classes'),
('CLASS:READ', 'Xem lớp học', 'CLASS', 'View class details'),
('CLASS:UPDATE', 'Cập nhật lớp học', 'CLASS', 'Update class information'),
('CLASS:DELETE', 'Xóa lớp học', 'CLASS', 'Delete classes'),
('CLASS:LIST', 'Danh sách lớp học', 'CLASS', 'List all classes'),

-- Attendance Permissions
('ATTENDANCE:TAKE', 'Điểm danh', 'ATTENDANCE', 'Take attendance for classes'),
('ATTENDANCE:READ', 'Xem điểm danh', 'ATTENDANCE', 'View attendance records'),
('ATTENDANCE:UPDATE', 'Cập nhật điểm danh', 'ATTENDANCE', 'Update attendance records'),

-- Billing Permissions
('INVOICE:CREATE', 'Tạo hóa đơn', 'BILLING', 'Create invoices'),
('INVOICE:READ', 'Xem hóa đơn', 'BILLING', 'View invoice details'),
('INVOICE:UPDATE', 'Cập nhật hóa đơn', 'BILLING', 'Update invoice information'),
('INVOICE:DELETE', 'Xóa hóa đơn', 'BILLING', 'Delete invoices'),
('PAYMENT:RECORD', 'Ghi nhận thanh toán', 'BILLING', 'Record payment transactions')

ON CONFLICT (code) DO NOTHING;

-- =====================================================
-- Update Trigger for users.updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Comments for documentation
-- =====================================================
COMMENT ON TABLE users IS 'User accounts with authentication and profile information';
COMMENT ON TABLE roles IS 'System and custom roles for access control';
COMMENT ON TABLE permissions IS 'Granular permissions for feature access';
COMMENT ON TABLE user_roles IS 'Many-to-many relationship between users and roles';

COMMENT ON COLUMN users.status IS 'Account status: ACTIVE, INACTIVE, PENDING, LOCKED';
COMMENT ON COLUMN users.email_verified IS 'Whether email has been verified';
COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN users.locked_until IS 'Timestamp when account will be automatically unlocked';
COMMENT ON COLUMN users.deleted IS 'Soft delete flag';
COMMENT ON COLUMN roles.is_system IS 'System roles cannot be deleted or modified';
