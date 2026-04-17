-- Users table for KiteHub platform authentication
-- Replaces in-memory ConcurrentHashMap with persistent storage

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'OWNER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Seed admin user (password: Admin@KiteHub123, BCrypt hash)
-- Generated with: new BCryptPasswordEncoder().encode("Admin@KiteHub123")
INSERT INTO users (id, email, name, password_hash, role)
VALUES (
    '00000000-0000-0000-0000-000000000099',
    'admin@kitehub.com',
    'KiteHub Admin',
    '$2b$12$igFWhWc2I9BDP68N95NnLOuhzM.EiPAbMgmElhK1pbRJT2fzN2M/q',
    'ADMIN'
);
