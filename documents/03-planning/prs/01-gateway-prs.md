# Gateway Service - PR Implementation List

**Service**: kiteclass-gateway
**Architecture Version**: V4.1 (Bundled Model)
**Effective Date**: 2026-02-26
**Tech Stack**: Spring Boot 3.5.10, Spring Cloud 2025.0.0, Spring Cloud Gateway
**Total PRs**: 10 (core PRs)
**Completed**: 10 (100%)
**Status**: 🎉 **COMPLETE** - All core features done

**Changes from V4.0**:
- LMS Module merged into Core Service (removed from separate service)
- Marketing Module merged into Core Service (removed from separate service)
- Simplified architecture: 3 services (Gateway, Core, Frontend) instead of 5

**Reference**:
- Technical plan: [`gateway-implementation-plan.md`](../implementation/gateway-implementation-plan.md)
- Master index: [`00-master-pr-index.md`](./00-master-pr-index.md)

---

## Overview

Gateway Service là entry point cho tất cả API requests. Chịu trách nhiệm:
- Authentication & Authorization (JWT)
- User management (CRUD)
- API routing & load balancing
- Rate limiting & throttling
- Request/Response logging
- Internal API security (HMAC-SHA256)
- Email service integration

---

## ✅ Completed PRs (9/10)

### PR 1.1: Project Setup ✅
**Status**: Complete
**Description**: Initialize Spring Cloud Gateway project

**Tasks**:
- Maven project with Spring Cloud Gateway
- Application properties configuration
- Health check endpoints
- Actuator setup

---

### PR 1.2: Common Components ✅
**Status**: Complete
**Description**: Shared utilities and DTOs

**Tasks**:
- BaseEntity with audit fields
- Common DTOs (ApiResponse, PageResponse, ErrorResponse)
- Exception handling
- Enums (UserRole, UserStatus)

---

### PR 1.3: User Module ✅
**Status**: Complete
**Description**: User CRUD operations

**Features**:
- User entity (email, password hash, role, status)
- CRUD endpoints
- Multi-tenant support
- Soft delete

**Endpoints**:
- POST /api/users - Create user
- GET /api/users - List users
- GET /api/users/{id} - Get user
- PUT /api/users/{id} - Update user
- DELETE /api/users/{id} - Soft delete

---

### PR 1.4: Auth Module ✅
**Status**: Complete
**Description**: JWT authentication

**Features**:
- Login with email/password
- JWT token generation (access + refresh)
- Token refresh mechanism
- Logout (invalidate refresh token)
- Password hashing (BCrypt)

**Endpoints**:
- POST /api/auth/login
- POST /api/auth/refresh
- POST /api/auth/logout
- POST /api/auth/forgot-password
- POST /api/auth/reset-password

**Testing**: 179 tests passing (149 unit + 30 integration)

---

### PR 1.4.1: Docker Setup & Integration Tests ✅
**Status**: Complete
**Description**: Docker Compose for local development

**Tasks**:
- docker-compose.yml (PostgreSQL, Redis, Gateway)
- Testcontainers for integration tests
- Database initialization scripts

---

### PR 1.5: Email Service ✅
**Status**: Complete
**Description**: Email sending with templates

**Features**:
- SMTP configuration
- Thymeleaf email templates
- Async email sending
- Email queue with retry logic
- Templates: Welcome, Password Reset, OTP

---

### PR 1.6: Gateway Configuration ✅
**Status**: Complete
**Description**: Rate limiting & logging

**Features**:
- **Rate Limiting** (Bucket4j):
  - 100 requests/min per IP
  - 1000 requests/min per authenticated user
  - Redis-backed token bucket
- **Request Logging**:
  - Request ID (X-Request-Id header)
  - Request/Response body logging
  - Execution time tracking
- **CORS Configuration**:
  - Allow frontend origins
  - Credentials support

---

### PR 1.7: Internal API Security ✅
**Status**: Complete (PR-REVIEW-2.4)
**Description**: HMAC-SHA256 for service-to-service calls

**Features**:
- Signature generation/verification
- InternalRequestFilter (validates signature)
- Internal client helper (auto-sign requests)
- Timestamp validation (prevent replay attacks)

**Header Format**:
```
X-Internal-Signature: HMAC-SHA256 hash
X-Internal-Timestamp: Unix timestamp
```

---

### PR 1.12: Spring Boot 3.5.10 Upgrade ✅
**Status**: Complete (PR-REVIEW-2.5)
**Description**: Infrastructure upgrade

**Tasks**:
- Upgrade Spring Boot 3.4.1 → 3.5.10
- Upgrade Spring Cloud 2024.0.1 → 2025.0.0
- Fix Security DSL deprecation (Lambda DSL)
- Migrate tests
- Create gateway-ci.yml workflow

---

### PR 1.13: Trial User Authentication Support ⭐ NEW
**Status**: 📋 Planned (V4.1 Phase 2)
**Priority**: HIGH (blocker for Core PR 2.13)
**Estimated Effort**: 8-12 hours
**Dependencies**: Migration V12 (TRIAL_USER enum)
**Blocks**: Core PR 2.13 (Trial Registration)

#### Objective
Add TRIAL_USER role support for trial users authentication and authorization via magic links (passwordless).

#### Changes

**1. User Role Enum Extension**

**File**: Migration V12 handles enum
```java
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    TEACHER,
    STUDENT,
    TRIAL_USER  // ⭐ NEW
}
```

**2. JWT Token Claims**

**File**: `JwtTokenProvider.java`
- JWT claims already include `role` field
- Verify TRIAL_USER role serialized correctly
- Add unit tests for TRIAL_USER token generation

**Example JWT payload**:
```json
{
  "sub": "user-uuid",
  "email": "trial@example.com",
  "role": "TRIAL_USER",  // ⭐ NEW role
  "instanceId": "tenant-uuid",
  "exp": 1234567890
}
```

**3. Magic Link Authentication (NEW)**

**File**: `MagicLinkService.java` (create)

**Purpose**: Passwordless authentication for trial users via email magic links

**Key Methods**:
```java
@Service
public class MagicLinkService {
    /**
     * Generate magic link token for trial user registration
     * @param email Trial user email
     * @param instanceId Tenant ID
     * @return Magic link token (expires in 30 minutes)
     */
    public String generateMagicLink(String email, UUID instanceId);

    /**
     * Verify magic link token and authenticate user
     * @param token Magic link token
     * @return JWT access token
     */
    public AuthResponse verifyMagicLink(String token);

    private User createTrialUser(String email, UUID instanceId);
}
```

**Implementation Details**:
- Generate secure random token (UUID)
- Store in Redis with 30-minute expiry (key: `magic_link:{token}`)
- Send email with magic link URL
- On verification: Find or create User (role=TRIAL_USER), generate JWT, delete token
- One-time use (token deleted after verification)

**REST Endpoints**:
```java
@RestController
@RequestMapping("/api/v1/auth/magic-link")
public class MagicLinkController {

    @PostMapping("/send")
    public ResponseEntity<Void> sendMagicLink(@RequestBody @Valid MagicLinkRequest request);
    // Returns 202 Accepted, sends email async

    @GetMapping("/verify")
    public ResponseEntity<AuthResponse> verifyMagicLink(@RequestParam String token);
    // Returns 200 OK with JWT tokens
}
```

**DTOs**:
```java
public record MagicLinkRequest(
    @NotBlank @Email String email,
    @NotNull UUID instanceId
) {}

public record MagicLinkData(
    String email,
    UUID instanceId,
    Instant createdAt
) {}
```

**4. Authorization Rules**

**File**: `SecurityConfig.java`

Add TRIAL_USER to permitted roles for trial endpoints:
```java
.pathMatchers(HttpMethod.GET, "/api/v1/courses/*/lessons")
    .hasAnyRole("TRIAL_USER", "STUDENT", "TEACHER")
.pathMatchers(HttpMethod.GET, "/api/v1/lessons/*")
    .hasAnyRole("TRIAL_USER", "STUDENT", "TEACHER")
.pathMatchers(HttpMethod.POST, "/api/v1/leads/*/convert")
    .hasRole("TRIAL_USER")
```

**5. Rate Limiting (Trial Users)**

**File**: `RateLimitConfig.java`

Apply stricter rate limits for TRIAL_USER role:
```java
@Bean
public KeyResolver userRoleKeyResolver() {
    return exchange -> {
        String userId = exchange.getAttribute("userId");
        String role = exchange.getAttribute("role");

        // Trial users: stricter limits
        if ("TRIAL_USER".equals(role)) {
            return Mono.just("trial:" + userId);
        }

        return Mono.just("user:" + userId);
    };
}

@Bean
public RedisRateLimiter trialUserRateLimiter() {
    // Trial users: 30 requests per minute
    return new RedisRateLimiter(30, 60);
}

@Bean
public RedisRateLimiter normalUserRateLimiter() {
    // Normal users: 100 requests per minute
    return new RedisRateLimiter(100, 60);
}
```

#### Testing

**Unit Tests**:
- `MagicLinkServiceTest.java`:
  - Test magic link generation
  - Test magic link verification (valid token)
  - Test magic link expiration (30 minutes)
  - Test one-time use (token deleted after verification)
  - Test invalid token handling
- `JwtTokenProviderTest.java`:
  - Test TRIAL_USER role in JWT claims
  - Test token generation for trial users
  - Test token validation for TRIAL_USER role

**Integration Tests**:
- `MagicLinkAuthFlowTest.java`:
  - Test full flow: send magic link → receive email → verify → get JWT
  - Test multi-tenant isolation (trial user only sees own tenant data)
  - Test rate limiting for trial users

#### API Documentation

**Endpoint**: `POST /api/v1/auth/magic-link/send`
**Request**:
```json
{
  "email": "trial@example.com",
  "instanceId": "tenant-uuid"
}
```
**Response**: `202 Accepted`

**Endpoint**: `GET /api/v1/auth/magic-link/verify?token={token}`
**Response**:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

#### Security Considerations

- Magic link tokens expire in 30 minutes
- One-time use (token deleted after verification)
- HTTPS required for magic link URLs
- Rate limiting on magic link generation (prevent email spam)
- Email verification implicit (user receives email → proves ownership)

#### Migration Notes

- **Database**: Migration V12 must be applied first (TRIAL_USER enum)
- **Redis**: No schema changes (uses existing Redis for token storage)
- **Email Service**: Ensure email templates configured for magic links

#### Next Steps

After merging this PR:
- Core PR 2.13 can implement trial registration (creates Lead + calls Gateway magic link API)
- Frontend PR 3.13 can implement trial signup form

**Last Updated**: 2026-02-26

---

## 🗂️ Archived/Removed PRs

### ❌ PR 1.8: UserType + ReferenceId Pattern (REMOVED)
**Status**: Removed - architecture changed
**Reason**: V4.1 architecture simplified cross-service linking strategy. Direct user-to-entity references moved to Core Service responsibility.

### 📦 PR 1.13: Trial User Authentication Support (MOVED)
**Status**: Moved to Expand Services plan
**Reason**: Trial user system is now part of optional "Expand Services" feature set (deferred to Phase 2 post-KiteHub launch).

---

## 📊 Summary

**Total PRs**: 10 (core PRs)
**Completed**: 10 (100%)
**Status**: ✅ **COMPLETE - All core features done!**

**Notes**:
- PR 1.8 (UserType + ReferenceId): Removed - architecture changed, no longer needed
- PR 1.13 (Trial User Auth): Moved to Expand Services plan (future enhancement)

**Test Coverage**: 179 tests passing (149 unit + 30 integration), 32 skipped (repository tests)

**Key Achievements**:
- ✅ JWT authentication with refresh tokens
- ✅ Email service with Thymeleaf templates
- ✅ Rate limiting (100 req/min IP, 1000 req/min user)
- ✅ Request/Response logging with correlation IDs
- ✅ Internal API security (HMAC-SHA256)
- ✅ Spring Boot 3.5.10 + Spring Cloud 2025.0.0

**Next Steps**:
1. Begin KiteHub Platform Services implementation (PR 4.1+)
2. Integration tests with Core Service (end-to-end flows)
3. Performance testing under load (load testing, stress testing)
4. Production deployment preparation

---

**Last Updated**: 2026-02-26
