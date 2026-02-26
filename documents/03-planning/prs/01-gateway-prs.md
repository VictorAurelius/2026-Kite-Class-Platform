# Gateway Service - PR Implementation List

**Service**: kiteclass-gateway
**Tech Stack**: Spring Boot 3.5.10, Spring Cloud 2025.0.0, Spring Cloud Gateway
**Total PRs**: 10
**Completed**: 9 (90%)
**Status**: ✅ Near complete

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

## ⏳ Pending PRs (1/10)

### PR 1.8: UserType + ReferenceId Pattern ⏳
**Status**: Blocked (need finalize cross-service strategy)
**Dependencies**: Finalize architecture decision
**Estimated**: 1 week

**Purpose**: Link users to Student/Teacher records in Core Service

**Entities**:
- Add `userType` (STUDENT, TEACHER, PARENT, ADMIN) to User entity
- Add `referenceId` (points to Student.id, Teacher.id, etc.)

**Endpoints**:
- GET /api/users/{id}/reference - Get linked entity
- POST /api/users/{id}/link - Link to Student/Teacher

**Business Rules**:
- STUDENT user must link to Core's Student.id
- TEACHER user must link to Core's Teacher.id
- PARENT user can link to multiple Students
- ADMIN user has no referenceId

**Cross-Service Communication**:
- Gateway calls Core's internal API to verify referenceId exists
- Feign Client or RestTemplate
- HMAC signature for security

**Testing**:
- Integration test: Link user to student
- Integration test: Verify cross-service call
- Integration test: Multi-tenant isolation

**Reference**: `cross-service-data-strategy.md` skill

---

## 📊 Summary

**Total PRs**: 10
**Completed**: 9 (90%)
**Blocked**: 1 (PR 1.8 - architecture decision needed)

**Test Coverage**: 179 tests passing (149 unit + 30 integration), 32 skipped (repository tests)

**Key Achievements**:
- ✅ JWT authentication with refresh tokens
- ✅ Email service with Thymeleaf templates
- ✅ Rate limiting (100 req/min IP, 1000 req/min user)
- ✅ Request/Response logging with correlation IDs
- ✅ Internal API security (HMAC-SHA256)
- ✅ Spring Boot 3.5.10 + Spring Cloud 2025.0.0

**Next Steps**:
1. Finalize cross-service linking strategy (PR 1.8)
2. Implement UserType + ReferenceId pattern
3. Integration tests with Core Service
4. Performance testing under load

---

**Last Updated**: 2026-02-26
