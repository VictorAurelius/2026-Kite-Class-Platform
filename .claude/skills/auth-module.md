# KiteClass Gateway - Authentication Module

## Overview

**Status:** ✅ IMPLEMENTED (PR 1.4 - 2026-01-26)
**Branch:** feature/gateway
**Location:** `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/module/auth/`

The Authentication Module provides JWT-based authentication, token management, and security features for the KiteClass Gateway.

---

## Architecture

```
gateway/
├── security/
│   ├── jwt/
│   │   ├── JwtTokenProvider      # Generate & validate tokens
│   │   ├── JwtProperties         # JWT configuration
│   │   └── TokenType             # ACCESS, REFRESH
│   ├── UserPrincipal             # Spring Security principal
│   └── SecurityContextRepository # Load auth from JWT
├── filter/
│   └── AuthenticationFilter      # Gateway filter for downstream
└── module/auth/
    ├── entity/
    │   ├── RefreshToken          # Stored refresh tokens
    │   └── RolePermission        # Role-permission mapping
    ├── repository/
    │   ├── RefreshTokenRepository
    │   └── RolePermissionRepository
    ├── dto/
    │   ├── LoginRequest/Response
    │   ├── RefreshTokenRequest
    │   └── ForgotPasswordRequest/ResetPasswordRequest
    ├── service/
    │   └── AuthServiceImpl       # Login, logout, refresh logic
    └── controller/
        └── AuthController        # 5 auth endpoints
```

---

## JWT Token Structure

### Access Token
- **Type:** ACCESS
- **Expiry:** 1 hour (3600000ms)
- **Claims:**
  - `sub`: User ID (Long)
  - `email`: User email (String)
  - `roles`: Role codes array (List<String>)
  - `type`: "ACCESS"
  - `iat`: Issued at timestamp
  - `exp`: Expiration timestamp
- **Algorithm:** HS512 with secret key (min 512 bits)

### Refresh Token
- **Type:** REFRESH
- **Expiry:** 7 days (604800000ms)
- **Claims:**
  - `sub`: User ID (Long)
  - `type`: "REFRESH"
  - `iat`: Issued at timestamp
  - `exp`: Expiration timestamp
- **Stored:** In database (refresh_tokens table)

---

## Authentication Flow

### 1. Login Flow

```
Client                  Gateway                  Database
  |                        |                         |
  |--POST /auth/login---->|                         |
  |  {email, password}    |                         |
  |                       |--findByEmail----------->|
  |                       |<-User-------------------|
  |                       |--validatePassword------>|
  |                       |--checkAccountStatus---->|
  |                       |--checkLocked----------->|
  |                       |                         |
  |                       |--generateTokens-------->|
  |                       |--saveRefreshToken------>|
  |                       |--updateLastLogin------->|
  |                       |--resetFailedAttempts--->|
  |                       |                         |
  |<-200 + tokens---------|                         |
  |  {accessToken,        |                         |
  |   refreshToken,       |                         |
  |   user: {id, email}}  |                         |
```

### 2. Refresh Token Flow

```
Client                  Gateway                  Database
  |                        |                         |
  |--POST /auth/refresh-->|                         |
  |  {refreshToken}       |                         |
  |                       |--findByToken----------->|
  |                       |<-RefreshToken-----------|
  |                       |--checkExpiry----------->|
  |                       |--findUser-------------->|
  |                       |--checkUserStatus------->|
  |                       |                         |
  |                       |--deleteOldToken-------->|
  |                       |--generateNewTokens----->|
  |                       |--saveNewRefreshToken--->|
  |                       |                         |
  |<-200 + new tokens-----|                         |
```

### 3. Protected Endpoint Access

```
Client                  Gateway                  Downstream
  |                        |                         |
  |--GET /api/v1/users--->|                         |
  |  Authorization:        |                         |
  |  Bearer <token>        |                         |
  |                       |--validateJWT----------->|
  |                       |--extractClaims--------->|
  |                       |--checkExpiry----------->|
  |                       |                         |
  |                       |--addHeaders------------>|
  |                       |  X-User-Id: 123         |
  |                       |  X-User-Roles: OWNER    |
  |                       |                         |
  |                       |--forward--------------->|
  |                       |                    (Core Service)
  |<-200 + data-----------|<-response---------------|
```

---

## Security Features

### 1. Account Locking
- **Trigger:** 5 consecutive failed login attempts
- **Duration:** 30 minutes
- **Implementation:**
  ```java
  MAX_FAILED_ATTEMPTS = 5
  LOCK_DURATION_MINUTES = 30

  On failed login:
    failedLoginAttempts++
    if (failedLoginAttempts >= 5) {
      lockedUntil = now + 30 minutes
    }

  On successful login:
    failedLoginAttempts = 0
    lockedUntil = null
  ```

### 2. Token Rotation
- On refresh, old refresh token is deleted
- New access + refresh tokens generated
- Prevents token reuse attacks

### 3. Token Validation
- Signature verification (HMAC-SHA512)
- Expiration check
- Token type check (ACCESS vs REFRESH)
- Claims integrity validation

### 4. Role-Based Access Control (RBAC)

```java
// SecurityConfig.java
.pathMatchers(HttpMethod.GET, "/api/v1/users/**")
    .hasAnyRole("ADMIN", "OWNER", "STAFF")

.pathMatchers(HttpMethod.POST, "/api/v1/users/**")
    .hasAnyRole("ADMIN", "OWNER")

.pathMatchers(HttpMethod.DELETE, "/api/v1/users/**")
    .hasRole("OWNER")
```

---

## API Endpoints

### POST /api/v1/auth/login
**Authentication:** None (public)

**Request:**
```json
{
  "email": "owner@kiteclass.local",
  "password": "Admin@123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "owner@kiteclass.local",
      "name": "System Owner",
      "roles": ["OWNER"]
    }
  }
}
```

**Errors:**
- 401: Invalid credentials
- 403: Account locked or inactive

### POST /api/v1/auth/refresh
**Authentication:** None (public)

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": { ... }
  }
}
```

**Errors:**
- 401: Invalid or expired refresh token

### POST /api/v1/auth/logout
**Authentication:** None (public)

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response:** 204 No Content

### POST /api/v1/auth/forgot-password
**Status:** ⚠️ Basic implementation (email sending not implemented)

### POST /api/v1/auth/reset-password
**Status:** ⚠️ Basic implementation (token validation not implemented)

---

## Configuration

### application.yml

```yaml
jwt:
  secret: ${JWT_SECRET:your-super-secret-key-min-512-bits-long-for-hs512-algorithm-security}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:3600000}      # 1 hour
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 days

spring:
  security:
    user:
      name: admin  # Ignored (JWT-based auth)
      password: admin
```

### Environment Variables (Production)

```bash
# REQUIRED - Override default secret
export JWT_SECRET="production-secret-key-minimum-512-bits-long"

# OPTIONAL - Override token expiration
export JWT_ACCESS_EXPIRATION=7200000    # 2 hours
export JWT_REFRESH_EXPIRATION=1209600000 # 14 days
```

**⚠️ Security Warning:**
- Default JWT secret is for development only
- MUST set `JWT_SECRET` in production
- Secret must be ≥ 512 bits (64 characters)

---

## Database Schema

### refresh_tokens table

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### role_permissions table

```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);
```

---

## Testing

### Unit Tests (30+ tests)

**JwtTokenProviderTest:**
- Token generation (access, refresh)
- Token validation
- Claims extraction
- Expiration handling

**AuthServiceTest:**
- Login success/failure
- Account locking
- Refresh token flow
- Logout

**AuthControllerTest:**
- All endpoints
- Validation errors
- HTTP status codes

### Manual Testing

```bash
# Start application
./mvnw spring-boot:run

# Run automated test script
./test-auth-flow.sh
```

---

## Default Credentials

**Owner Account:**
- Email: `owner@kiteclass.local`
- Password: `Admin@123`
- Roles: OWNER (full permissions)
- Created by: V4 database migration

---

## Gateway Filter for Downstream Services

The `AuthenticationFilter` validates JWT and adds headers:

```java
// Applied in application.yml routes:
spring:
  cloud:
    gateway:
      routes:
        - id: core-students
          uri: ${CORE_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/api/v1/students/**
          filters:
            - AuthenticationFilter  # <-- This filter
```

**Headers Added:**
- `X-User-Id`: User ID from JWT (Long)
- `X-User-Roles`: Comma-separated role codes (String)

**Downstream services can use these headers:**
```java
@GetMapping("/students")
public Mono<List<Student>> getStudents(
    @RequestHeader("X-User-Id") Long userId,
    @RequestHeader("X-User-Roles") String roles
) {
    // Use userId and roles for authorization
}
```

---

## Common Issues & Solutions

### 1. "JAVA_HOME not defined"
```bash
./setup-java.sh
source ~/.bashrc
```

### 2. "JWT secret too short"
```bash
export JWT_SECRET="your-secret-key-must-be-at-least-512-bits-64-chars-long-here"
```

### 3. "Account locked"
Wait 30 minutes or manually reset in database:
```sql
UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE email = 'user@example.com';
```

### 4. "Refresh token invalid"
- Token expired (7 days)
- Token already used (deleted after refresh)
- Token not found in database (logout called)

---

## Future Enhancements (Not in PR 1.4)

- [ ] Email service integration for password reset
- [ ] Email verification for new users
- [ ] Token blacklist (logout before expiry)
- [ ] Rate limiting per user
- [ ] Permission-based access control (beyond roles)
- [ ] OAuth2 integration (Google, Facebook)
- [ ] Two-factor authentication (2FA)
- [ ] Session management (multiple devices)

---

## References

**Documentation:**
- PR-1.4-SUMMARY.md - Complete implementation guide
- TESTING.md - Test execution guide
- test-auth-flow.sh - Automated test script

**Code:**
- `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/module/auth/`
- `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/security/`

**Related:**
- User Module (PR 1.3)
- Gateway Configuration
- RBAC System

---

**Last Updated:** 2026-01-26 (PR 1.4)
**Author:** VictorAurelius + Claude Sonnet 4.5
