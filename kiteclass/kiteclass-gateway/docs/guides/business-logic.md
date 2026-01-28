# KiteClass Gateway - Business Logic Documentation

## Overview

Gateway Service is responsible for:
- **API Gateway:** Routing, rate limiting, logging
- **User Management:** CRUD operations for users, roles, permissions
- **Authentication:** Login/logout, JWT tokens, refresh tokens
- **Authorization:** Role-based access control (RBAC)
- **Cross-Service Integration:** Fetching profiles from Core Service

**Technology Stack:**
- Spring Boot 3.5.10
- Spring Cloud Gateway (Reactive)
- Spring Security
- Spring Data R2DBC (PostgreSQL)
- Spring Data Redis (Reactive)
- JWT (io.jsonwebtoken)
- Spring Cloud OpenFeign

---

## Core Modules

### 1. User Module

**Location:** `src/main/java/com/kiteclass/gateway/module/user/`

**Entities:**
- `User`: User accounts with credentials
- `Role`: Predefined roles (OWNER, ADMIN, TEACHER, PARENT, STUDENT)
- `Permission`: Fine-grained permissions
- `UserRole`: User-Role many-to-many relationship
- `RolePermission`: Role-Permission many-to-many relationship

**Business Rules:**

#### User Registration
```java
// UserServiceImpl.createUser()
1. Validate email uniqueness
2. Encode password
3. Set default status = PENDING
4. Assign roles from request
5. Create user in database
6. Return user response with roles
```

**Validation Rules:**
- Email must be unique (not soft-deleted)
- Email format: RFC 5322 standard
- Password: min 8 characters, must contain uppercase, lowercase, digit, special char
- Name: required, max 100 characters
- Phone: optional, 10-15 digits

#### User Update
```java
// UserServiceImpl.updateUser()
1. Fetch user by ID
2. Update allowed fields: name, phone, address, avatarUrl
3. Update roles if provided
4. Save updated user
5. Return updated response
```

**Immutable Fields:**
- `id`, `email`, `passwordHash` (use separate endpoint for password change)
- `status`, `emailVerified` (admin-only operations)
- Audit fields: `createdAt`, `updatedAt`, `deleted`, `deletedAt`

#### User Deletion
```java
// UserServiceImpl.deleteUser()
1. Fetch user by ID
2. Soft delete: set deleted = true, deletedAt = now()
3. Optionally delete all refresh tokens
4. Save user
```

**Cascade Rules:**
- Refresh tokens: deleted on user deletion
- User roles: retained for audit (marked as deleted via user.deleted flag)

### 2. Authentication Module

**Location:** `src/main/java/com/kiteclass/gateway/module/auth/`

**Entities:**
- `RefreshToken`: JWT refresh tokens with expiration
- `PasswordResetToken`: One-time tokens for password reset

**Business Rules:**

#### Login Flow
```java
// AuthServiceImpl.login()
1. Validate credentials
   a. Check email exists
   b. Check account not deleted
   c. Check account not locked
   d. Check account status = ACTIVE
   e. Verify password matches

2. Handle failed attempts
   a. Increment failedLoginAttempts
   b. Lock account if attempts >= 5
   c. Set lockedUntil = now + 30 minutes

3. Generate tokens
   a. Create JWT access token (expires: 1 hour)
   b. Create JWT refresh token (expires: 7 days)
   c. Save refresh token to database

4. Fetch user profile from Core (since PR 1.8)
   a. If ADMIN/STAFF → profile = null
   b. If STUDENT/TEACHER/PARENT → fetch from Core via Feign
   c. Handle Core service errors gracefully (return null)

5. Return LoginResponse
   - accessToken, refreshToken, tokenType, expiresIn
   - UserInfo: id, email, name, roles, profile
```

**Login Security:**
- Max failed attempts: 5
- Lock duration: 30 minutes
- Account statuses: PENDING, ACTIVE, INACTIVE, SUSPENDED
- Only ACTIVE accounts can login

#### Refresh Token Flow
```java
// AuthServiceImpl.refreshToken()
1. Validate refresh token
   a. Find token in database
   b. Check not expired
   c. Check user still active

2. Revoke old token
   - Delete old refresh token from database

3. Generate new tokens
   - New access token
   - New refresh token
   - Save new refresh token

4. Fetch user profile from Core
5. Return new LoginResponse
```

**Token Security:**
- Refresh tokens stored in database (revocable)
- Access tokens stateless (not stored, validated via signature)
- Expired refresh tokens automatically deleted
- One refresh token per login session

#### Logout Flow
```java
// AuthServiceImpl.logout()
1. Find refresh token in database
2. Delete refresh token (revokes session)
3. Client discards access token
```

**Note:** Access tokens cannot be revoked (stateless). They expire after 1 hour.

#### Password Reset Flow
```java
// AuthServiceImpl.forgotPassword()
1. Find user by email
2. Check account is ACTIVE
3. Generate unique reset token (UUID)
4. Create PasswordResetToken entity
   - token, userId, expiresAt (1 hour)
5. Delete any existing reset tokens for user
6. Save reset token
7. Send password reset email (async)

// AuthServiceImpl.resetPassword()
1. Validate reset token
   a. Find token in database
   b. Check not expired
   c. Check not already used
2. Update user password
   - Encode new password
   - Reset failedLoginAttempts to 0
   - Clear lockedUntil
3. Mark token as used
4. Revoke all refresh tokens (security measure)
5. User must login again
```

**Password Reset Security:**
- Tokens expire after 1 hour
- One-time use only
- All sessions terminated after password change
- Email validation required

### 3. Cross-Service Integration (PR 1.8)

**Location:** `src/main/java/com/kiteclass/gateway/service/`

**Components:**
- `CoreServiceClient`: Feign client for Core service internal APIs
- `ProfileFetcher`: Service to fetch user profiles based on UserType

**Business Rules:**

#### Profile Fetching
```java
// ProfileFetcher.fetchProfile(UserType, Long referenceId)

1. Internal Staff (ADMIN, STAFF)
   - No Core entity
   - Return null
   - No API call

2. External Users (STUDENT, TEACHER, PARENT)
   - Validate referenceId not null
   - Call Core service internal API
   - Return profile DTO or null on error

3. Error Handling
   - 404 NotFound → return null (profile doesn't exist)
   - 503 ServiceUnavailable → return null (Core down)
   - 500 InternalServerError → return null (Core error)
   - Log all errors for monitoring
```

**Profile Integration Status:**
| UserType | Core Module | Status | Profile DTO |
|----------|-------------|--------|-------------|
| ADMIN | N/A | ✅ Complete | null |
| STAFF | N/A | ✅ Complete | null |
| STUDENT | Student | ✅ Complete | StudentProfileResponse |
| TEACHER | Teacher | ⏳ Pending | TeacherProfileResponse (placeholder) |
| PARENT | Parent | ⏳ Pending | ParentProfileResponse (placeholder) |

**Note:** Teacher and Parent modules not yet implemented in Core Service. Gateway has placeholder DTOs and will fetch profiles once Core modules are ready.

#### Cross-Service Security
```java
// CoreServiceClient uses X-Internal-Request header
@GetMapping("/internal/students/{id}")
ApiResponse<StudentProfileResponse> getStudent(
    @PathVariable Long id,
    @RequestHeader("X-Internal-Request") String internalHeader
);
```

**Security Rules:**
- All internal APIs require `X-Internal-Request: true` header
- Core service validates via `InternalRequestFilter`
- Requests without header rejected with 403 Forbidden
- Header hardcoded in Gateway (not exposed to clients)

---

## Authorization Rules

### Role Hierarchy

```
OWNER (Superuser)
  └── Full system access
  └── Cannot be deleted
  └── Can manage all users and roles

ADMIN (Administrator)
  └── Full access except owner-level operations
  └── Can manage users, roles, permissions
  └── Can view all data

TEACHER
  └── Access to own classes and students
  └── Can mark attendance, create assignments
  └── Can view own schedule

PARENT
  └── Access to own children's data
  └── Can view student progress, attendance
  └── Can view invoices and payments

STUDENT
  └── Access to own data only
  └── Can view own classes, attendance, grades
  └── Cannot modify data
```

### Permission System

**Format:** `ENTITY:ACTION`

**Examples:**
- `USER:READ` - View users
- `USER:WRITE` - Create/Update users
- `USER:DELETE` - Delete users
- `ROLE:MANAGE` - Manage roles and permissions

**Permission Checking:**
```java
// AuthorizationManager
@PreAuthorize("hasPermission('USER', 'WRITE')")
public Mono<UserResponse> createUser(CreateUserRequest request)
```

### API Endpoint Access Control

| Endpoint | Public | Roles Required | Notes |
|----------|--------|----------------|-------|
| POST /api/v1/auth/login | ✅ | None | Anyone can login |
| POST /api/v1/auth/refresh | ✅ | None | Valid refresh token |
| POST /api/v1/auth/logout | 🔒 | Authenticated | Any logged-in user |
| POST /api/v1/auth/forgot-password | ✅ | None | Email-based |
| POST /api/v1/auth/reset-password | ✅ | None | Token-based |
| GET /api/v1/users | 🔒 | USER:READ | Admin/Owner |
| POST /api/v1/users | 🔒 | USER:WRITE | Admin/Owner |
| GET /api/v1/users/{id} | 🔒 | USER:READ or SELF | Users can view self |
| PUT /api/v1/users/{id} | 🔒 | USER:WRITE or SELF | Users can update self |
| DELETE /api/v1/users/{id} | 🔒 | USER:DELETE | Admin/Owner only |

---

## Data Flow Examples

### Example 1: Student Login with Profile

```
Client                 Gateway                      Core
  |                       |                          |
  |-- POST /auth/login -->|                          |
  |    {email, password}  |                          |
  |                       |                          |
  |                  [Validate credentials]          |
  |                       |                          |
  |                  [Check UserType = STUDENT]      |
  |                       |                          |
  |                       |-- GET /internal/students/{id} -->
  |                       |    X-Internal-Request: true
  |                       |                          |
  |                       |<-- StudentProfileResponse ---|
  |                       |                          |
  |                  [Generate JWT tokens]           |
  |                       |                          |
  |<-- LoginResponse -----|                          |
  |    {tokens, user,     |                          |
  |     profile}          |                          |
```

### Example 2: Admin Login (No Profile)

```
Client                 Gateway
  |                       |
  |-- POST /auth/login -->|
  |    {email, password}  |
  |                       |
  |                  [Validate credentials]
  |                       |
  |                  [Check UserType = ADMIN]
  |                       |
  |                  [Skip profile fetch]
  |                       |
  |                  [Generate JWT tokens]
  |                       |
  |<-- LoginResponse -----|
  |    {tokens, user,     |
  |     profile: null}    |
```

### Example 3: Core Service Down (Graceful Degradation)

```
Client                 Gateway                      Core
  |                       |                          |
  |-- POST /auth/login -->|                          |
  |                       |                          |
  |                  [Validate credentials OK]       |
  |                       |                          |
  |                       |-- GET /internal/students/{id} -->
  |                       |                          |
  |                       |<-- 503 Service Unavailable ---|
  |                       |                          |
  |                  [Log warning]                   |
  |                  [Set profile = null]            |
  |                       |                          |
  |<-- LoginResponse -----|                          |
  |    {tokens, user,     |                          |
  |     profile: null}    |   ← Still allows login! |
```

**Graceful Degradation Strategy:**
- User can still login even if Core service is down
- Profile fetch failures logged but not thrown
- Client receives `profile: null` and handles gracefully
- Authentication never depends on Core availability

---

## Database Schema Overview

### User Tables

```sql
-- users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    email_verified BOOLEAN DEFAULT false,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    user_type VARCHAR(20) DEFAULT 'ADMIN',  -- PR 1.8
    reference_id BIGINT,                     -- PR 1.8
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMP
);

-- roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- permissions table
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- user_roles (junction table)
CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- role_permissions (junction table)
CREATE TABLE role_permissions (
    role_id BIGINT REFERENCES roles(id),
    permission_id BIGINT REFERENCES permissions(id),
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);
```

### Authentication Tables

```sql
-- refresh_tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- password_reset_tokens table
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Data Relationships

```
User 1 ──────────< user_roles >──────────┐
                                          │
                                    Role 1
                                          │
                            role_permissions >────── Permission *

User 1 ────────── refresh_tokens *
User 1 ────────── password_reset_tokens *
```

---

## Configuration

### JWT Configuration

```yaml
jwt:
  secret: ${JWT_SECRET:your-super-secret-key-min-512-bits}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:3600000}      # 1 hour
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 days
```

### Email Configuration

```yaml
email:
  from: ${EMAIL_FROM:KiteClass <noreply@kiteclass.com>}
  base-url: ${APP_BASE_URL:http://localhost:3000}
  reset-token-expiration: ${EMAIL_RESET_TOKEN_EXPIRATION:3600000}  # 1 hour
```

### Rate Limiting

```yaml
rate-limit:
  enabled: true
  unauthenticated-requests-per-minute: 100    # Per IP
  authenticated-requests-per-minute: 1000     # Per user
  time-window-seconds: 60
```

### Core Service Configuration (PR 1.8)

```yaml
core:
  service:
    url: ${CORE_SERVICE_URL:http://localhost:8081}
```

---

## Future Enhancements

### Pending Core Module Integration

**When Teacher Module is implemented in Core:**
1. Update `ProfileFetcher.fetchTeacherProfile()` - uncomment Feign call
2. Test teacher login with profile fetching
3. Update integration tests

**When Parent Module is implemented in Core:**
1. Update `ProfileFetcher.fetchParentProfile()` - uncomment Feign call
2. Test parent login with profile fetching
3. Update integration tests

### Suggested Improvements

**Authentication:**
- [ ] 2FA (Two-Factor Authentication) support
- [ ] OAuth2 social login (Google, Facebook)
- [ ] Remember me functionality
- [ ] Session management (view all active sessions)

**User Management:**
- [ ] Bulk user import/export
- [ ] User activity logs
- [ ] Advanced search and filtering
- [ ] User profile pictures upload

**Authorization:**
- [ ] Dynamic permission management UI
- [ ] Permission inheritance
- [ ] Time-based role assignments
- [ ] Resource-level permissions (e.g., class-specific)

**Cross-Service:**
- [ ] Profile caching in Redis
- [ ] Circuit breaker for Core service calls
- [ ] Async profile fetching with CompletableFuture
- [ ] Profile change event synchronization

---

## Testing Strategy

### Unit Tests

**UserService:**
- ✅ Create user with valid data
- ✅ Create user with duplicate email → DuplicateResourceException
- ✅ Get user by ID
- ✅ Update user successfully
- ✅ Delete user (soft delete)
- ✅ Pagination and search

**AuthService:**
- ✅ Login with valid credentials
- ✅ Login with invalid password → failed attempts increment
- ✅ Login with locked account → AUTH_ACCOUNT_LOCKED
- ✅ Login with inactive account → AUTH_ACCOUNT_INACTIVE
- ✅ Refresh token successfully
- ✅ Refresh with expired token → AUTH_REFRESH_TOKEN_EXPIRED
- ✅ Logout successfully
- ✅ Login with STUDENT userType → includes profile
- ✅ Login with ADMIN userType → profile = null

**ProfileFetcher:**
- ✅ Fetch profile for ADMIN → returns null
- ✅ Fetch profile for STAFF → returns null
- ✅ Fetch profile for STUDENT → returns StudentProfileResponse
- ✅ Fetch profile for TEACHER → returns null (not implemented)
- ✅ Fetch profile for PARENT → returns null (not implemented)
- ✅ Fetch profile with null referenceId → IllegalArgumentException
- ✅ Fetch profile when Core returns 404 → returns null
- ✅ Fetch profile when Core returns 503 → returns null

### Integration Tests

**AuthController:**
- ✅ Login with default owner account
- ✅ Login with invalid email → 401
- ✅ Login with invalid password → 401
- ✅ Refresh token flow
- ✅ Logout flow

**UserController:**
- ✅ Create user → 201
- ✅ Get users list → 200
- ✅ Get user by ID → 200
- ✅ Update user → 200
- ✅ Delete user → 204

---

## Monitoring and Logging

### Key Metrics

**Authentication:**
- Login success/failure rate
- Failed login attempts per user
- Account lockouts per hour
- Token generation time
- Token validation time

**Profile Fetching:**
- Core service response time
- Core service availability
- Profile fetch success/failure rate
- Cache hit/miss ratio (future)

**User Management:**
- Active users count
- New user registrations per day
- User role distribution

### Logging Levels

```java
// INFO - Business events
log.info("Login successful for user: {}", email);
log.info("User created: {}", user.getId());

// WARN - Recoverable issues
log.warn("Account locked for user: {}", email);
log.warn("Core service unavailable, returning null profile");

// ERROR - Unexpected errors
log.error("Failed to send email to: {}", email, exception);
log.error("Database error during user creation", exception);
```

---

**Last Updated:** 2026-01-28
**Version:** 1.8.0
**Author:** KiteClass Team + Claude Sonnet 4.5
