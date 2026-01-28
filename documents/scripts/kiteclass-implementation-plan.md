# KiteClass Implementation Prompts

Danh sách prompts để thực hiện các plans theo thứ tự.

**Nguyên tắc bắt buộc:**
1. Tuân thủ tất cả skills trong `.claude/skills/`
2. Mỗi module phải có tests đi kèm ngay trong PR đó
3. Tests nằm trong thư mục `src/test/` (BE) hoặc `src/__tests__/` (FE)
4. **Branch theo service:** feature/gateway, feature/core, feature/frontend
5. **Commit sau khi hoàn thành PR**, format ngắn gọn: `feat(service): PR X.X - description`

---

# 📚 AVAILABLE SKILLS REFERENCE

Tất cả skills trong `.claude/skills/` - tham chiếu khi cần:

## Core Development Skills
- **`architecture-overview.md`** - Tổng quan kiến trúc microservices, service boundaries, cross-service patterns
- **`api-design.md`** - REST API conventions, request/response patterns, service-to-service communication
- **`code-style.md`** - Java/Spring Boot naming conventions, package structure, JavaDoc requirements
- **`database-design.md`** - Schema design, entity relationships, migration practices
- **`enums-constants.md`** - Enum design patterns, constant management

## Testing & Quality Skills
- **`testing-guide.md`** - Cách viết tests từ đầu (JUnit, Mockito, Testcontainers, React Testing Library)
- **`spring-boot-testing-quality.md`** ⭐ **NEW** - Fix warnings & deprecated APIs (Spring Boot 3.4+, @MockBean → @TestConfiguration, MapStruct, code quality checklist)

## Cross-Service & Integration Skills
- **`cross-service-data-strategy.md`** - UserType + ReferenceId pattern, Feign Client, Saga pattern, cross-service linking
- **`email-service.md`** - Email templates, SMTP configuration, Thymeleaf integration

## Infrastructure & DevOps Skills
- **`cloud-infrastructure.md`** - AWS deployment, Docker, Kubernetes, CI/CD
- **`environment-setup.md`** - Local dev setup, Docker Compose, database initialization

## Frontend Skills
- **`frontend-development.md`** - React/TypeScript patterns, component structure, state management, UI design system
- **`frontend-code-quality.md`** ⭐ **NEW** - TypeScript strict mode, React best practices, testing requirements, code quality checklist

## Project Management Skills
- **`development-workflow.md`** - Git workflow, PR process, branch strategy
- **`documentation-structure.md`** - Documentation standards, README templates
- **`maven-dependencies.md`** - Dependency versions, conflict resolution
- **`project-schedule.md`** - Timeline, milestones, priorities
- **`required-knowledge.md`** - Tech stack requirements
- **`skills-compliance-checklist.md`** - Pre-commit checklist, quality gates
- **`troubleshooting.md`** - Common issues, solutions, debugging tips
- **`error-logging.md`** - Logging patterns, error handling, monitoring

## 🎯 When to Use Each Skill

**Before starting any Backend PR:**
1. ✅ Check `architecture-overview.md` for service boundaries
2. ✅ Review `code-style.md` for naming conventions
3. ✅ Consult `api-design.md` for endpoint design
4. ✅ Read `testing-guide.md` for test structure
5. ✅ Reference `maven-dependencies.md` for correct versions

**Before starting any Frontend PR:**
1. ✅ Review `frontend-development.md` for UI patterns & design system
2. ✅ Check `frontend-code-quality.md` for TypeScript/React best practices
3. ✅ Consult `api-design.md` for API integration patterns
4. ✅ Read `testing-guide.md` Part 2 for React Testing Library patterns

**When writing tests:**
1. ✅ Backend: Use `spring-boot-testing-quality.md` for fixing warnings & quality issues
2. ✅ Frontend: Use `frontend-code-quality.md` Part 3 for React Testing Library patterns

**When encountering issues:**
1. ✅ Check `troubleshooting.md` first
2. ✅ Review `error-logging.md` for logging patterns
3. ✅ Consult specific skill for the domain (e.g., `cross-service-data-strategy.md` for integration issues)

**Before committing:**
1. ✅ Backend: Run through `spring-boot-testing-quality.md` checklist (no warnings, no deprecated APIs)
2. ✅ Frontend: Run through `frontend-code-quality.md` Part 8 checklist (no `any`, tests pass, ESLint clean)
3. ✅ All: Check `development-workflow.md` for commit message format
4. ✅ Git hooks will run automatically (checks JavaDoc, error codes, TypeScript types, etc.)

---

# 📊 PROGRESS TRACKING

## 🔀 Git Workflow Update (2026-01-27)

**NEW WORKFLOW:** Merge to main after each milestone, create new branch from main for next work.

**Completed Merges:**
- ✅ `feature/gateway` → `main` (2026-01-26) - Gateway PRs 1.1-1.6
- ✅ `feature/core` → `main` (2026-01-27) - Core PRs 2.1-2.3, 2.11

**Current Branch:** `feature/gateway-cross-service` (for PR 1.8)

**Strategy:** Keep code unified in main, branch out for specific features, merge back when complete.

---

## Gateway Service
- ✅ PR 1.1: Project Setup
- ✅ PR 1.2: Common Components
- ✅ PR 1.3: User Module
- ✅ PR 1.4: Auth Module
- ✅ **PR 1.4.1**: Docker Setup & Integration Tests *(added to plan)*
- ✅ **PR 1.5**: Email Service *(added to plan)*
- ✅ **PR 1.6**: Gateway Configuration (Rate Limiting + Logging)

**Gateway Status:** 7/8 PRs completed (87.5%) - ⚠️ NEEDS CROSS-SERVICE FIX
**Tests:** 95 passing (55 unit + 40 integration)
**Docker:** ✅ PostgreSQL, Redis configured
**Email:** ✅ Integrated with Thymeleaf templates
**Rate Limiting:** ✅ Bucket4j (100 req/min IP, 1000 req/min user)
**Logging:** ✅ Request/Response logging with correlation IDs
**⚠️ CRITICAL:** Missing UserType + ReferenceId pattern (PR 1.8 needed)

## Core Service (feature/core branch)
- ✅ PR 2.1: Core Project Setup
- ✅ PR 2.2: Core Common Components
- ✅ PR 2.3: Student Module
- ⏳ PR 2.4: Course Module
- ⏳ PR 2.5: Class Module
- ⏳ PR 2.6: Enrollment Module
- ⏳ PR 2.7: Attendance Module
- ⏳ PR 2.8: Invoice & Payment Module
- ⏳ PR 2.9: Settings & Parent Module
- ⏳ PR 2.10: Core Docker & Final Integration
- ✅ **PR 2.11: Internal APIs for Gateway** *(cross-service linking)*

**Core Status:** 4/14 PRs completed (28.6%) ✅ PR 2.11 COMPLETE
**Tests:** 50/50 passing (100%) - 40 from PR 2.3 + 10 internal API tests
**Latest:** PR 2.11 Internal APIs complete - InternalRequestFilter + InternalStudentController
**Cross-Service APIs Ready:**
- ✅ GET /internal/students/{id} - Retrieve student profile
- ✅ POST /internal/students - Create student during registration
- ✅ DELETE /internal/students/{id} - Soft delete student
**🚨 NEXT PRIORITY:** PR 1.8 Gateway Integration (now unblocked)

**New PRs Added (2026-01-28):**
- PR 2.3.1: Teacher Module (BLOCKING for Course/Class) - from teacher-module-business-logic.md
- PR 2.7.1: Assignment Module - from assignment-module-business-logic.md
- PR 2.7.2: Grade Module - from grade-module-business-logic.md
- PR 2.8 renamed to: Invoice Module (split from old PR 2.8)
- PR 2.8.1 (new): Payment Module (split from old PR 2.8)
- PR 2.9 updated: Settings & Preferences (removed Parent Module - moved to Engagement Service P1)

**Updated PR Count:**
- Old count: 11 Core PRs
- New count: 14 Core PRs (added 3 new PRs: 2.3.1, 2.7.1, 2.7.2; split PR 2.8 into 2.8 + 2.8.1)

## Frontend (feature/frontend branch)
⏳ **NOT STARTED** - All 11 PRs pending

### 🎯 PAIRED DEVELOPMENT STRATEGY (NEW)

**Philosophy:** Backend PHẢI có Frontend đi kèm để test business logic trực quan, thay vì chỉ dựa vào documentation.

**Development Flow:**
1. Implement Backend module (API endpoints, business logic, tests)
2. IMMEDIATELY implement corresponding Frontend (UI, forms, integration)
3. Test end-to-end trên UI thực tế
4. Verify business rules visually trước khi move to next module

### Frontend PRs Status

**Phase 1: Infrastructure** (Required first)
- ⏳ PR 3.1: Project Setup & Core Infrastructure
- ⏳ PR 3.2: Shared Components & Layout System
- ⏳ PR 3.3: Authentication Pages → **NEEDS: PR 1.4 ✅ (Done)**

**Phase 2: IMMEDIATE PRIORITY** (Backend APIs already available)
- ⏳ PR 3.4: Student Management Pages → **NEEDS: PR 2.3 ✅ (Done)**
- ⏳ PR 3.5: Teacher Management Pages → **NEEDS: PR 2.3.1 ✅ (Done)**
- ⏳ PR 3.6: Course Management Pages → **NEEDS: PR 2.4 ✅ (Done)**

**Phase 3: Remaining Modules** (Backend pending)
- ⏳ PR 3.7: Class Management Pages → NEEDS: PR 2.5 (pending)
- ⏳ PR 3.8: Attendance Management → NEEDS: PR 2.7 (pending)
- ⏳ PR 3.9: Billing Pages → NEEDS: PR 2.8, 2.8.1 (pending)
- ⏳ PR 3.10: Parent Portal → NEEDS: PR 2.9 (pending)
- ⏳ PR 3.11: Settings & Reports → NEEDS: PR 2.9 (pending)

**Frontend Status:** 0/11 PRs completed (0%)
**Tech Stack:** Next.js 14, TypeScript, Tailwind CSS, Shadcn/UI, React Query, Zustand
**CRITICAL:** Frontend PRs 3.1-3.6 can start NOW (Backend APIs ready)

**Overall Progress:** 11/44 PRs completed (25%)
**Last Updated:** 2026-01-28 (Updated with Paired Development Strategy)

---

## 🚀 IMMEDIATE EXECUTION ROADMAP (With Paired Development)

### ✅ Backend Ready - Frontend Needed NOW:

**Week 1-2: Frontend Infrastructure + Core Modules**

**Sprint 1: Setup Frontend** (2-3 days)
1. ⏳ PR 3.1: Project Setup & Core Infrastructure
   - Next.js project setup, dependencies, API client
   - No backend dependency

2. ⏳ PR 3.2: Shared Components & Layout System
   - Sidebar, Header, DataTable, shared UI components
   - No backend dependency

3. ⏳ PR 3.3: Authentication Pages
   - Login, Forgot Password, Reset Password
   - **Backend:** PR 1.4 Auth Module ✅ Ready
   - **Test:** Login flow end-to-end

**Sprint 2: Student Module** (2-3 days)
4. ⏳ PR 3.4: Student Management Pages
   - **Backend:** PR 2.3 Student Module ✅ Ready
   - List, Create, Edit, Delete students
   - **Test trực quan:** All student CRUD operations, search, pagination
   - **Verify:** Email/phone uniqueness, validation errors

**Sprint 3: Teacher Module** (2-3 days)
5. ⏳ PR 3.5: Teacher Management Pages
   - **Backend:** PR 2.3.1 Teacher Module ✅ Ready
   - List, Create, Edit, Delete teachers
   - **Test trực quan:** Teacher CRUD, status changes (ACTIVE/ON_LEAVE/TERMINATED)

**Sprint 4: Course Module** (3-4 days)
6. ⏳ PR 3.6: Course Management Pages
   - **Backend:** PR 2.4 Course Module ✅ Ready
   - List, Create, Edit courses
   - Publish/Archive lifecycle actions
   - **Test trực quan:**
     - Course status transitions (DRAFT → PUBLISHED → ARCHIVED)
     - Edit restrictions (ARCHIVED read-only, PUBLISHED limited fields)
     - Validation (required fields for publish)
     - Soft delete restrictions

**Estimated Timeline:** 2 weeks to have fully functional Student, Teacher, Course management with UI

---

### 🔄 Future Paired Development:

**When implementing next Backend modules:**
- PR 2.5 (Class Module) → IMMEDIATELY do PR 3.7 (Class Management Pages)
- PR 2.7 (Attendance) → IMMEDIATELY do PR 3.8 (Attendance Management)
- PR 2.8 (Invoice) + PR 2.8.1 (Payment) → IMMEDIATELY do PR 3.9 (Billing Pages)

**Benefits:**
- ✅ Visual testing of business logic
- ✅ Catch API design issues early
- ✅ Better understanding of user flows
- ✅ No need to rely solely on documentation
- ✅ Faster feedback loop

---

# 🚨 CRITICAL: CROSS-SERVICE DATA LINKING FIX REQUIRED

## Vấn Đề Phát Hiện

Trong quá trình review architecture, phát hiện **thiếu sót nghiêm trọng** trong thiết kế:

❌ **Gateway có User entity** (authentication) nhưng **Core có Student/Teacher/Parent entities** (business logic)
❌ **KHÔNG CÓ thiết kế liên kết** giữa User và các entity này
❌ Student/Teacher/Parent **KHÔNG THỂ LOGIN** vào hệ thống
❌ Registration flow **KHÔNG TẠO ĐƯỢC** profile records trong Core

## Giải Pháp

✅ **UserType + ReferenceId Pattern** đã được thiết kế và document:
- User entity có thêm `userType` enum (ADMIN/STAFF/TEACHER/PARENT/STUDENT)
- User entity có thêm `referenceId` (link tới Core entity ID)
- Gateway call Core API để lấy/tạo profile data
- Saga pattern cho registration flow (tạo User + Core entity atomically)

**Tài liệu đã được cập nhật:**
- ✅ `.claude/skills/cross-service-data-strategy.md` (585 dòng implementation guide)
- ✅ `.claude/skills/architecture-overview.md` (Cross-Service Relationships section)
- ✅ `documents/plans/database-design.md` (Microservices Database Strategy)
- ✅ `.claude/skills/api-design.md` (Service-to-Service Communication)
- ✅ `documents/reports/gateway-core-separation-rationale.md` (Architecture justification)

## Action Items (PHẢI LÀM NGAY)

### 1️⃣ PRIORITY 1: PR 2.11 - Core Internal APIs
**Branch:** feature/core
**Prerequisite:** PR 2.3 (Student Module) phải complete trước
**Tasks:**
- Tạo InternalStudentController với GET/POST/DELETE endpoints
- Tạo InternalRequestFilter để bảo vệ internal APIs
- Tạo Response DTOs cho internal APIs
- Viết tests (~10-15 tests)

**Prompt:** Xem section "PR 2.11" bên dưới

### 2️⃣ PRIORITY 2: PR 1.8 - Gateway Cross-Service Integration
**Branch:** feature/gateway
**Prerequisite:** PR 2.11 phải complete trước
**Tasks:**
- Migration thêm user_type, reference_id vào users table
- Tạo UserType enum
- Update User entity
- Implement Feign Client để call Core APIs
- Update login flow (fetch profile từ Core)
- Implement registration flow với Saga pattern
- Viết tests (~15-20 tests)

**Prompt:** Xem section "PR 1.8" bên dưới

### 3️⃣ Sau khi fix: Tiếp tục Core development
- PR 2.3: Student Module (có thể bắt đầu ngay)
- PR 2.11: Internal APIs (sau PR 2.3)
- PR 1.8: Gateway Integration (sau PR 2.11)
- PR 2.4+: Continue với remaining Core modules

## Execution Order

```
┌─────────────────────────────────────────────────────────────┐
│ CURRENT STATE: Gateway 7/8 PRs done, Core 2/11 PRs done    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 1: Complete PR 2.3 (Student Module)                   │
│ Branch: feature/core                                        │
│ Time: ~2-3 hours                                           │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 2: Complete PR 2.11 (Core Internal APIs)              │
│ Branch: feature/core                                        │
│ Time: ~1-2 hours                                           │
│ Depends on: PR 2.3                                         │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 3: Complete PR 1.8 (Gateway Cross-Service)            │
│ Branch: feature/gateway                                     │
│ Time: ~2-3 hours                                           │
│ Depends on: PR 2.11                                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 4: Continue with remaining PRs                        │
│ PR 2.4, 2.5, 2.6... (Core modules)                         │
│ PR 3.1, 3.2, 3.3... (Frontend)                             │
└─────────────────────────────────────────────────────────────┘
```

---

# GIAI ĐOẠN 1: KITECLASS-GATEWAY

## ✅ PR 1.1 - Gateway Project Setup

```
Thực hiện Phase 1 của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- maven-dependencies.md: versions chuẩn, PHẢI check trước khi tạo pom.xml
- architecture-overview.md: cấu trúc thư mục Backend
- code-style.md: Java naming conventions, package structure
- environment-setup.md: cấu hình local dev
- testing-guide.md: test structure & patterns
- spring-boot-testing-quality.md: code quality checklist, fix warnings trước khi commit

**Tasks:**
1. Tạo project structure trong thư mục kiteclass/kiteclass-gateway/
2. Tạo pom.xml với dependencies theo plan
3. Tạo application.yml cho các profiles (local, docker, prod)
4. Tạo KiteclassGatewayApplication.java

**Verification:**
- mvn clean compile phải pass
- Application context loads thành công
```

## ✅ PR 1.2 - Gateway Common Components

```
Thực hiện Phase 2 của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- code-style.md: Java conventions, annotation ordering
- enums-constants.md: định nghĩa enums đúng format
- error-logging.md: exception handling patterns
- testing-guide.md: test structure & patterns
- spring-boot-testing-quality.md: code quality checklist, no warnings before commit

**Tasks:**
1. Tạo common package structure:
   - common/dto/ (ApiResponse, ErrorResponse)
   - common/exception/ (BusinessException, GlobalExceptionHandler)
   - common/constant/ (enums)
2. Tạo R2dbcConfig, SecurityConfig cơ bản
3. Tạo các enums: UserRole, UserStatus theo enums-constants.md

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/gateway/common/
  - ApiResponseTest.java
  - ErrorResponseTest.java
  - GlobalExceptionHandlerTest.java

**Verification:**
- mvn test phải pass tất cả tests
```

## ✅ PR 1.3 - User Module

```
Thực hiện Phase 3 (User Module) của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- code-style.md: Entity, Repository, Service, Controller conventions
- api-design.md: User Management API endpoints
- database-design.md: users table schema
- testing-guide.md: unit test patterns & structure
- spring-boot-testing-quality.md: Spring Boot 3.4+ patterns, @TestConfiguration, fix all warnings

**Tasks:**
1. Tạo User entity với R2DBC annotations
2. Tạo Role, Permission entities
3. Tạo UserRepository (ReactiveCrudRepository)
4. Tạo UserService interface và UserServiceImpl
5. Tạo DTOs: UserResponse, CreateUserRequest, UpdateUserRequest
6. Tạo UserController với endpoints:
   - GET /api/v1/users
   - GET /api/v1/users/{id}
   - POST /api/v1/users
   - PUT /api/v1/users/{id}
   - DELETE /api/v1/users/{id}

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/gateway/module/user/
  - service/UserServiceTest.java (unit test với Mockito)
  - controller/UserControllerTest.java (WebFluxTest)
  - repository/UserRepositoryTest.java (DataR2dbcTest)
- src/test/java/com/kiteclass/gateway/testutil/
  - UserTestDataBuilder.java

**Verification:**
- mvn test phải pass
- Coverage cho UserService >= 80%
```

## ✅ PR 1.4 - Auth Module

```
Thực hiện Phase 4 (Auth Module) của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- code-style.md: Service patterns
- api-design.md: Authentication API endpoints
- testing-guide.md: testing security components, unit & integration test patterns
- spring-boot-testing-quality.md: JWT testing patterns, security test setup, fix warnings

**Tasks:**
1. Tạo JwtTokenProvider:
   - generateAccessToken(user)
   - generateRefreshToken(user)
   - validateToken(token)
   - extractUserId(token)
2. Tạo RefreshToken entity và repository
3. Tạo AuthService:
   - login(email, password) -> AuthResponse
   - logout(refreshToken)
   - refreshToken(refreshToken) -> AuthResponse
4. Tạo AuthController với endpoints:
   - POST /api/v1/auth/login
   - POST /api/v1/auth/logout
   - POST /api/v1/auth/refresh
   - GET /api/v1/auth/me
5. Tạo JwtAuthenticationFilter cho Gateway

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/gateway/module/auth/
  - service/JwtTokenProviderTest.java
  - service/AuthServiceTest.java
  - controller/AuthControllerTest.java
- src/test/java/com/kiteclass/gateway/security/
  - JwtAuthenticationFilterTest.java

**Verification:**
- mvn test phải pass
- Test các case: valid token, expired token, invalid token
- Test login success/failure
```

---

### ✅ PR 1.4.1 - Docker Setup & Integration Tests *(ADDED TO PLAN)*

**Note:** This PR was added between 1.4 and 1.5 to complete Docker infrastructure early.

```
Hoàn thiện Docker setup và integration tests với Testcontainers.

**Tuân thủ skills:**
- database-design.md: Flyway migrations
- cloud-infrastructure.md: Docker configuration
- testing-guide.md: integration tests with Testcontainers
- spring-boot-testing-quality.md: Testcontainers resource leak fix, container reuse, integration test templates

**Tasks:**
1. Tạo Flyway migrations (V1-V4):
   - V1: Create schema
   - V2: Create users table
   - V3: Create roles & permissions
   - V4: Seed default owner account
2. Tạo docker-compose.yml với PostgreSQL, Redis
3. Viết integration tests với Testcontainers
4. Document Docker setup

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/gateway/integration/
  - UserIntegrationTest.java (13 tests)
  - AuthIntegrationTest.java (9 tests)
  - JwtIntegrationTest.java (10 tests)

**Verification:**
- docker-compose up thành công
- Integration tests pass với Testcontainers
- Login với owner@kiteclass.local / Admin@123 thành công
```

---

### ✅ PR 1.5 - Email Service *(ADDED TO PLAN)*

**Note:** This PR was added to implement email functionality needed for password reset.

```
Thực hiện Email Service với Spring Boot Mail và Thymeleaf.

**Tuân thủ skills:**
- code-style.md: Service patterns, reactive wrapping
- api-design.md: Password reset endpoints
- database-design.md: password_reset_tokens table
- testing-guide.md: testing async operations

**Tasks:**
1. Add dependencies: spring-boot-starter-mail, spring-boot-starter-thymeleaf
2. Tạo EmailService interface và EmailServiceImpl:
   - sendPasswordResetEmail()
   - sendWelcomeEmail()
   - sendAccountLockedEmail()
   - Wrap blocking JavaMailSender với Mono + boundedElastic
3. Tạo PasswordResetToken entity và repository
4. Integrate với AuthService:
   - forgotPassword() endpoint
   - resetPassword() endpoint
5. Tạo HTML email templates với Thymeleaf
6. Configure SMTP settings (Gmail)

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/gateway/service/
  - EmailServiceTest.java (5 unit tests)
- src/test/java/com/kiteclass/gateway/integration/
  - PasswordResetIntegrationTest.java (8 integration tests)

**Verification:**
- mvn test phải pass (82 total tests)
- Email sending works with real SMTP
- Password reset flow hoàn chỉnh
```

---

## ✅ PR 1.6 - Gateway Configuration (ORIGINAL PR 1.5)

**Note:** This is the original PR 1.5 from the plan, renumbered to 1.6 after additions.
**Status:** ✅ COMPLETE (2026-01-27)

```
Thực hiện Phase 5 (Gateway Configuration) của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- architecture-overview.md: service communication
- cloud-infrastructure.md: rate limiting config

**Tasks:**
1. Cấu hình routes trong application.yml:
   - /api/v1/auth/** -> local auth service
   - /api/v1/users/** -> local user service
   - /api/v1/** -> lb://kiteclass-core
2. Tạo RateLimitingFilter với Bucket4j:
   - 100 requests/minute per IP
   - 1000 requests/minute per authenticated user
3. Tạo LoggingFilter (request/response logging)
4. Tạo CorsConfig

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/gateway/filter/
  - RateLimitingFilterTest.java
  - LoggingFilterTest.java
- src/test/java/com/kiteclass/gateway/config/
  - CorsConfigTest.java
  - RouteConfigTest.java

**Verification:**
- mvn test phải pass
- Test rate limiting với nhiều requests
```

## ✅ PR 1.7 - Gateway Database & Docker (ORIGINAL PR 1.6)

**Note:** This is the original PR 1.6, renumbered to 1.7. Most tasks already completed in PR 1.4.1.

**Status:** ✅ MOSTLY COMPLETE via PR 1.4.1

```
Thực hiện Phase 6 của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- database-design.md: schema cho users, roles, permissions
- cloud-infrastructure.md: Docker configuration
- environment-setup.md: docker-compose setup

**Tasks:**
1. Tạo Flyway migrations:
   - V1__create_users_schema.sql
   - V2__create_users_table.sql
   - V3__create_roles_permissions.sql
   - V4__create_refresh_tokens.sql
   - V5__seed_default_owner.sql (owner@kiteclass.local / Admin@123)
2. Tạo Dockerfile (multi-stage build)
3. Tạo docker-compose.yml:
   - gateway service
   - postgres
   - redis

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/gateway/integration/
  - AuthIntegrationTest.java (với Testcontainers)
  - UserIntegrationTest.java (với Testcontainers)
- src/test/resources/
  - application-test.yml

**Verification:**
- docker-compose up phải start thành công
- Login với owner@kiteclass.local / Admin@123 phải thành công
- Integration tests pass với Testcontainers
```

---

## 🚨 PR 1.8 - Cross-Service Data Integration (CRITICAL FIX)

**Priority:** 🚨 HIGH - Must complete before continuing Core development
**Status:** ⚠️ PARTIALLY COMPLETE (2026-01-28)
**Dependencies:**
- ✅ PR 2.11 (Core Internal APIs) - Complete
- ⏳ Core Teacher Module - Not yet implemented
- ⏳ Core Parent Module - Not yet implemented

**Implementation Status:**
- ✅ Part 1: Database migration, UserType enum, User entity update (commit d655444)
- ✅ Part 2: Feign client, ProfileFetcher service, Login integration (commit 455174c)
- ✅ Tests: ProfileFetcherTest (12), AuthServiceTest updates (11) (commit c88c434)
- ⚠️ **Incomplete:** Teacher and Parent profile fetching (placeholders only)

**⚠️ IMPORTANT NOTE:**
PR 1.8 is functionally complete for STUDENT profile fetching. However, Teacher and Parent
profile fetching will return null until their respective modules are implemented in Core Service.

**What works now:**
- ✅ ADMIN/STAFF login (no profile needed)
- ✅ STUDENT login with full profile from Core
- ✅ Graceful degradation when Core service unavailable
- ✅ All unit tests passing (23/23)

**What needs Core modules:**
- ⏳ TEACHER login with profile → Requires Core Teacher Module (future PR)
- ⏳ PARENT login with profile → Requires Core Parent Module (future PR)

**Action Items:**
1. When Core Teacher Module is implemented:
   - Uncomment `ProfileFetcher.fetchTeacherProfile()` Feign call
   - Add integration tests for teacher login with profile
   - Update documentation

2. When Core Parent Module is implemented:
   - Uncomment `ProfileFetcher.fetchParentProfile()` Feign call
   - Add integration tests for parent login with profile
   - Update documentation

**Testing:**
- 23/23 unit tests passing
- Integration tests require Docker (7 tests pending Docker setup)

```
Implement UserType + ReferenceId pattern để liên kết Gateway User với Core entities.

**Vấn đề cần fix:**
- Gateway User entity thiếu userType và referenceId
- Không có cách liên kết User với Student/Teacher/Parent trong Core
- Login flow không trả về profile data
- Registration flow không tạo được Student/Teacher/Parent records

**Tuân thủ skills:**
- cross-service-data-strategy.md: implementation guide đầy đủ
- architecture-overview.md: Cross-Service Data Relationships
- database-design.md: Microservices Database Strategy
- api-design.md: Service-to-Service Communication
- testing-guide.md: Feign Client testing, integration tests
- spring-boot-testing-quality.md: Feign Client mocking, WebFluxTest patterns, fix warnings

**Tasks:**

### 1. Database Migration
1. Tạo V6__add_user_type_reference_id.sql:
   ```sql
   ALTER TABLE users
       ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
       ADD COLUMN reference_id BIGINT NULL;

   CREATE INDEX idx_users_user_type ON users(user_type);
   CREATE INDEX idx_users_reference_id ON users(reference_id);

   -- Update existing owner account
   UPDATE users SET user_type = 'ADMIN' WHERE email = 'owner@kiteclass.local';
   ```

### 2. Update User Entity
1. Thêm UserType enum vào common/constant/:
   ```java
   public enum UserType {
       ADMIN,      // No referenceId - internal staff
       STAFF,      // No referenceId - internal staff
       TEACHER,    // referenceId → teachers.id in Core
       PARENT,     // referenceId → parents.id in Core
       STUDENT     // referenceId → students.id in Core
   }
   ```
2. Update User entity thêm fields:
   ```java
   @Enumerated(EnumType.STRING)
   @Column(name = "user_type", nullable = false)
   private UserType userType = UserType.ADMIN;

   @Column(name = "reference_id")
   private Long referenceId;
   ```

### 3. Implement Feign Client
1. Add dependency spring-cloud-starter-openfeign vào pom.xml
2. Tạo CoreServiceClient interface:
   ```java
   @FeignClient(name = "core-service", url = "${core.service.url}")
   public interface CoreServiceClient {
       @GetMapping("/internal/students/{id}")
       StudentProfileResponse getStudent(@PathVariable Long id,
           @RequestHeader("X-Internal-Request") String header);

       @GetMapping("/internal/teachers/{id}")
       TeacherProfileResponse getTeacher(@PathVariable Long id,
           @RequestHeader("X-Internal-Request") String header);

       @GetMapping("/internal/parents/{id}")
       ParentProfileResponse getParent(@PathVariable Long id,
           @RequestHeader("X-Internal-Request") String header);

       @PostMapping("/internal/students")
       StudentProfileResponse createStudent(@RequestBody CreateStudentRequest req,
           @RequestHeader("X-Internal-Request") String header);
   }
   ```
3. Tạo DTOs: StudentProfileResponse, TeacherProfileResponse, ParentProfileResponse
4. Enable Feign: @EnableFeignClients trong main application class

### 4. Update AuthService - Login Flow
1. Update login() method:
   - Sau khi generate JWT, gọi Core để lấy profile
   - Logic: if (userType == STUDENT) fetch student profile
   - Thêm profile vào LoginResponse
2. Tạo ProfileFetcher service:
   ```java
   public Object fetchProfile(UserType userType, Long referenceId) {
       return switch (userType) {
           case STUDENT -> coreClient.getStudent(referenceId, "true");
           case TEACHER -> coreClient.getTeacher(referenceId, "true");
           case PARENT -> coreClient.getParent(referenceId, "true");
           case ADMIN, STAFF -> null;
       };
   }
   ```

### 5. Update UserService - Registration Flow (Saga Pattern)
1. Tạo UserRegistrationService:
   ```java
   @Transactional
   public UserRegistrationResponse registerStudent(StudentRegistrationRequest req) {
       // 1. Create User in Gateway (without referenceId)
       User user = createUser(req);

       try {
           // 2. Create Student in Core via API
           StudentProfileResponse student = coreClient.createStudent(...);

           // 3. Update User with referenceId
           user.setReferenceId(student.getId());
           user.setStatus(UserStatus.ACTIVE);
           userRepository.save(user);

           return success(user, student);
       } catch (Exception e) {
           // Compensating transaction: rollback User
           userRepository.delete(user);
           throw new RegistrationFailedException(e);
       }
   }
   ```
2. Tương tự cho registerTeacher, registerParent

### 6. Update DTOs
1. Update LoginResponse thêm profile field:
   ```java
   public class LoginResponse {
       private String accessToken;
       private String refreshToken;
       private UserDTO user;
       private Object profile;  // StudentProfile/TeacherProfile/ParentProfile
   }
   ```
2. Tạo StudentRegistrationRequest, TeacherRegistrationRequest

### 7. Configuration
1. Thêm vào application.yml:
   ```yaml
   core:
     service:
       url: ${CORE_SERVICE_URL:http://localhost:8081}

   feign:
     client:
       config:
         default:
           connectTimeout: 5000
           readTimeout: 10000
   ```

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/gateway/client/
  - CoreServiceClientTest.java (với WireMock)
- src/test/java/com/kiteclass/gateway/service/
  - ProfileFetcherTest.java
  - UserRegistrationServiceTest.java (test saga pattern)
- src/test/java/com/kiteclass/gateway/module/auth/
  - AuthServiceTest.java (update existing tests)
- src/test/java/com/kiteclass/gateway/integration/
  - CrossServiceIntegrationTest.java (với Testcontainers + WireMock)
  - UserRegistrationIntegrationTest.java

**Test Cases Cần Cover:**
- Login với STUDENT userType → fetch student profile từ Core
- Login với ADMIN userType → không fetch profile
- Register student → tạo User + Student, link bằng referenceId
- Register student fails → rollback User creation
- Core service unavailable → graceful degradation
- Invalid referenceId → handle error

**Verification:**
- mvn test phải pass (thêm ~15-20 tests)
- Login response chứa profile data
- Registration tạo đúng User + Core entity
- Saga rollback hoạt động khi Core API fails
- Feign client retry logic hoạt động

**Documentation:**
- Update Gateway README với cross-service communication
- Document internal API authentication (X-Internal-Request header)

---

## ✅ PR 1.8 - COMPLETED WORK (2026-01-28)

**Commits:**
- d655444: PR 1.8 Part 1 - Add UserType and cross-service foundation
- 455174c: PR 1.8 Part 2 - Implement cross-service profile fetching
- c88c434: test(gateway): PR 1.8 - Add comprehensive tests for cross-service profile fetching
- 0ff4448: fix(test): fix MessageService and EmailService test failures

**What Was Implemented:**

### ✅ Database & Entities
- V6 migration: Added `user_type` and `reference_id` to users table
- UserType enum with 5 types (ADMIN, STAFF, TEACHER, PARENT, STUDENT)
- Helper methods: `requiresReferenceId()`, `isInternalStaff()`
- User entity updated with userType and referenceId fields

### ✅ Feign Client Integration
- Added spring-cloud-starter-openfeign dependency
- Created CoreServiceClient interface
- 3 endpoints: getStudent(), getTeacher(), getParent()
- All use X-Internal-Request header for authentication
- Configuration: core.service.url in application.yml

### ✅ Profile DTOs
- StudentProfileResponse (8 fields) - ACTIVE
- TeacherProfileResponse (7 fields) - PLACEHOLDER
- ParentProfileResponse (7 fields) - PLACEHOLDER

### ✅ ProfileFetcher Service
- fetchProfile(UserType, Long referenceId)
- Returns appropriate profile based on UserType
- Returns null for ADMIN/STAFF (internal staff)
- Comprehensive error handling (404, 503, 500)
- Graceful degradation when Core unavailable

### ✅ Login Integration
- LoginResponse.UserInfo updated with profile field
- AuthServiceImpl.login() now fetches profiles
- Profile included in login response
- Works for STUDENT userType
- Returns null for ADMIN/STAFF (no Core entity)
- Returns null for TEACHER/PARENT (not implemented yet)

### ✅ Tests
- ProfileFetcherTest: 12/12 passing
  - Internal staff tests (ADMIN, STAFF)
  - External user tests (STUDENT, TEACHER, PARENT)
  - Validation tests (null referenceId)
  - Error handling tests (404, 503, 500)
- AuthServiceTest: 11/11 passing (updated for profile fetching)
- All unit tests passing: 86/86
- Integration tests: 7 pending (require Docker)

### ✅ Documentation
- Created docs/guides/business-logic.md (comprehensive)
- Updated all Javadocs
- Clear notes about Teacher/Parent placeholders

**What Remains (Blocked by Core):**

### ⏳ Teacher Profile Fetching
- CoreServiceClient.getTeacher() defined but not called
- ProfileFetcher.fetchTeacherProfile() returns null
- Waiting for: Core Teacher Module implementation

### ⏳ Parent Profile Fetching
- CoreServiceClient.getParent() defined but not called
- ProfileFetcher.fetchParentProfile() returns null
- Waiting for: Core Parent Module implementation

### ⏳ Registration Flow (Not Started)
- Student registration saga pattern - NOT IMPLEMENTED
- Teacher registration - NOT IMPLEMENTED
- Parent registration - NOT IMPLEMENTED
- Note: Current PR focused on READ operations (profile fetching during login)

**Future Work:**

When Core Teacher Module is ready:
1. Uncomment ProfileFetcher.fetchTeacherProfile() line 136-137
2. Test teacher login with profile
3. Add integration tests

When Core Parent Module is ready:
1. Uncomment ProfileFetcher.fetchParentProfile() line 154-155
2. Test parent login with profile
3. Add integration tests

When Registration Flow is needed:
1. Implement UserRegistrationService with Saga pattern
2. Add createStudent/Teacher/Parent to CoreServiceClient
3. Implement compensating transactions
4. Add comprehensive integration tests

```

---

# GIAI ĐOẠN 2: KITECLASS-CORE

## ✅ PR 2.1 - Core Project Setup

**Status:** ✅ COMPLETE (2026-01-27)

```
Thực hiện Phase 1 của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- maven-dependencies.md: versions chuẩn, PHẢI check trước khi tạo pom.xml
- architecture-overview.md: cấu trúc thư mục Backend
- code-style.md: Java naming conventions
- environment-setup.md: cấu hình local dev

**Tasks:**
1. Tạo project structure trong thư mục kiteclass/kiteclass-core/
2. Tạo pom.xml với dependencies theo plan
3. Tạo application.yml cho các profiles
4. Tạo KiteclassCoreApplication.java

**Verification:**
- mvn clean compile phải pass
- Application context loads thành công
```

## ✅ PR 2.2 - Core Common Components

**Status:** ✅ COMPLETE (2026-01-27)
**Tests:** 22 passing (ApiResponse, PageResponse, ErrorResponse, GlobalExceptionHandler, JpaConfig)

```
Thực hiện Phase 2 của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: Java conventions, JavaDoc requirements
- enums-constants.md: tất cả enums cho Core service
- error-logging.md: exception handling, logging patterns
- testing-guide.md: test patterns for DTOs & exception handlers
- spring-boot-testing-quality.md: @ExtendWith(MockitoExtension.class), fix warnings

**Tasks:**
1. Tạo BaseEntity với audit fields (createdAt, updatedAt, createdBy, updatedBy, deleted, version)
2. Tạo common DTOs:
   - ApiResponse<T>
   - PageResponse<T>
   - ErrorResponse
3. Tạo exception classes:
   - BusinessException
   - EntityNotFoundException
   - DuplicateResourceException
   - ValidationException
4. Tạo GlobalExceptionHandler
5. Tạo tất cả enums theo enums-constants.md:
   - StudentStatus, Gender
   - ClassStatus, SessionStatus
   - AttendanceStatus
   - InvoiceStatus, PaymentStatus, PaymentMethod
   - EnrollmentStatus
6. Tạo config classes: JpaConfig, CacheConfig, RabbitConfig

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/common/
  - dto/ApiResponseTest.java
  - dto/PageResponseTest.java
  - exception/GlobalExceptionHandlerTest.java
- src/test/java/com/kiteclass/core/config/
  - JpaConfigTest.java

**Verification:**
- mvn test phải pass
```

## ✅ PR 2.3 - Student Module

**Status:** ✅ COMPLETE (2026-01-27)
**Tests:** 40/40 passing (100%)
**Commits:**
- 92a9979: Initial implementation (code complete, tests failing)
- fa348df: Fix test issues (security config + Docker condition)

```
Thực hiện Student Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: Entity, Repository, Service, Controller, DTO conventions
- api-design.md: Student API endpoints
- database-design.md: students table schema
- testing-guide.md: unit test patterns, TestDataBuilder, integration tests
- spring-boot-testing-quality.md: @TestConfiguration for mocks, MapStruct warnings, Testcontainers setup

**Tasks:**
1. Tạo Student entity với JPA annotations
2. Tạo StudentRepository với custom queries:
   - findByIdAndDeletedFalse
   - existsByEmailAndDeletedFalse
   - findBySearchCriteria (search, status, pageable)
3. Tạo StudentMapper (MapStruct)
4. Tạo StudentService interface
5. Tạo StudentServiceImpl với:
   - createStudent
   - getStudentById
   - getStudents (paginated, searchable)
   - updateStudent
   - deleteStudent (soft delete)
6. Tạo StudentController với endpoints theo api-design.md

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/student/
  - service/StudentServiceTest.java
  - controller/StudentControllerTest.java
  - repository/StudentRepositoryTest.java
  - mapper/StudentMapperTest.java
- src/test/java/com/kiteclass/core/testutil/
  - StudentTestDataBuilder.java
  - IntegrationTestBase.java

**Flyway Migration:**
- V2__create_student_tables.sql

**Verification:**
- mvn test phải pass
- Coverage cho StudentService >= 80%
- Swagger UI hiển thị đúng endpoints

**Implementation Details:**
✅ **Completed Components:**
- Student Entity với BaseEntity audit fields
- StudentRepository với custom queries
- StudentMapper (MapStruct) với toResponse, toEntity, updateEntity
- StudentService + StudentServiceImpl với full business logic
  - Email/phone uniqueness validation
  - Redis caching (@Cacheable/@CacheEvict)
  - Transaction management
  - Soft delete pattern
- StudentController với 5 REST API endpoints
- Flyway migration V2__create_student_tables.sql
- Test utilities (StudentTestDataBuilder, IntegrationTestBase)

✅ **Tests: 40/40 passing (100%)**
- StudentServiceTest: 10 tests ✅
- StudentMapperTest: 3 tests ✅
- StudentControllerTest: 5 tests ✅ (fixed with TestSecurityConfig)
- StudentRepositoryTest: 4 tests (disabled by default, enable with ENABLE_INTEGRATION_TESTS=true)
- Common tests: 22 tests ✅

**Test Fixes Applied (commit fa348df):**
1. StudentControllerTest: Added TestSecurityConfig to disable security for tests
2. StudentRepositoryTest: Added @EnabledIfEnvironmentVariable for Docker requirement

**To run integration tests:**
```bash
# Requires Docker running
mvn test -DENABLE_INTEGRATION_TESTS=true
```
```

## ⏳ PR 2.3.1 - Teacher Module (BLOCKING PR)

**Status:** ⏳ NOT STARTED
**Dependencies:** None (PR 2.3 Student Module completed)
**Business Logic:** docs/modules/teacher-module-business-logic.md
**BLOCKING FOR:** PR 2.4 Course Module, PR 2.5 Class Module

```
Thực hiện Teacher Module - BLOCKING PR for Course and Class Modules.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Teacher API endpoints
- database-design.md: teachers, teacher_courses, teacher_classes schema
- testing-guide.md: test patterns
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Teacher entity với JPA annotations:
   - id, name, email, phone_number, specialization
   - bio, qualification, experience_years, avatar_url
   - status (ACTIVE, INACTIVE, ON_LEAVE)
2. Tạo TeacherCourse entity (Course-level permissions):
   - teacher_id, course_id, role (CREATOR, INSTRUCTOR, ASSISTANT)
   - assigned_at, assigned_by
3. Tạo TeacherClass entity (Class-level permissions):
   - teacher_id, class_id, role (MAIN_TEACHER, ASSISTANT)
   - assigned_at, assigned_by
4. Tạo TeacherRepository với custom queries:
   - findByIdAndDeletedFalse
   - existsByEmail
   - findBySpecialization
5. Tạo TeacherCourseRepository và TeacherClassRepository
6. Tạo TeacherMapper (MapStruct)
7. Tạo TeacherService và TeacherServiceImpl với:
   - createTeacher (BR-TEACHER-001: email unique)
   - assignToCourse (UC-TEACHER-003)
   - assignToClass (UC-TEACHER-004)
   - removeFromClass (UC-TEACHER-005, BR-TEACHER-004: must have 1 MAIN_TEACHER)
   - getTeacherPermissions (UC-TEACHER-006)
   - Permission check methods (canAccessClass, canModifyClass, canTakeAttendance)
8. Tạo TeacherController với endpoints theo api-design.md
9. Tạo InternalTeacherController (cho Gateway):
   - GET /internal/teachers/{id} (profile fetching)

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/teacher/
  - service/TeacherServiceTest.java
  - controller/TeacherControllerTest.java
  - controller/InternalTeacherControllerTest.java
  - repository/TeacherRepositoryTest.java
  - mapper/TeacherMapperTest.java
- src/test/java/com/kiteclass/core/testutil/
  - TeacherTestDataBuilder.java

**Flyway Migration:**
- V3__create_teacher_tables.sql (teachers, teacher_courses, teacher_classes)

**Verification:**
- mvn test phải pass
- Coverage cho TeacherService >= 80%
- Internal API /internal/teachers/{id} hoạt động

**Key Business Rules (from business-logic.md):**
- BR-TEACHER-001: Email unique
- BR-TEACHER-004: Class phải có ít nhất 1 MAIN_TEACHER
- BR-TEACHER-005: Chỉ ACTIVE teachers assign được
- BR-TEACHER-006: Course CREATOR có full control
- BR-TEACHER-008: Attendance chỉ MAIN_TEACHER hoặc CREATOR

**Integration Points:**
- Gateway: Internal API cho teacher profile fetching
- Course Module: TeacherCourse relationship (course_id FK)
- Class Module: TeacherClass relationship (class_id FK)
- Attendance Module: Permission check cho điểm danh
- Assignment Module: Permission check cho create/grade assignments

**Permission Model:**
- Two-level hierarchy: Course-level (CREATOR/INSTRUCTOR/ASSISTANT) > Class-level (MAIN_TEACHER/ASSISTANT)
- CREATOR của course → Auto có quyền với tất cả classes trong course
- INSTRUCTOR của course → Access all classes trong course
- MAIN_TEACHER của class → Full control class đó
- Support Use Case 1: Language Center (resource-level permissions)
- Support Use Case 2: Independent Teacher (OWNER bypass)
```

## ⏳ PR 2.4 - Course Module

**Status:** ⏳ NOT STARTED
**Dependencies:** PR 2.3.1 Teacher Module (REQUIRED - teacher_id FK, created_by)
**Business Logic:** docs/modules/course-module-business-logic.md

```
Thực hiện Course Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Course API endpoints
- database-design.md: courses table schema
- testing-guide.md: test patterns
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Course entity với JPA annotations:
   - id, name, code, description
   - level (Beginner, Intermediate, Advanced)
   - duration_weeks, max_students, price
   - created_by (teacher_id FK to teachers.id)
   - status (DRAFT, PUBLISHED, ARCHIVED)
2. Tạo CourseRepository với custom queries:
   - findByIdAndDeletedFalse
   - findByCreatedBy (teacher's courses)
   - findByStatus
3. Tạo CourseMapper (MapStruct)
4. Tạo CourseService và CourseServiceImpl với:
   - createCourse (UC-TEACHER-002: Teacher as Creator)
   - Auto-create TeacherCourse (CREATOR role) when course created
   - updateCourse
   - deleteCourse (soft delete)
   - getCourses (với teacher permission filter)
5. Tạo CourseController với CRUD endpoints

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/course/
  - service/CourseServiceTest.java
  - controller/CourseControllerTest.java
  - repository/CourseRepositoryTest.java
  - mapper/CourseMapperTest.java
- src/test/java/com/kiteclass/core/testutil/
  - CourseTestDataBuilder.java

**Flyway Migration:**
- V4__create_course_tables.sql

**Verification:**
- mvn test phải pass
- Coverage >= 80%
- Teacher tạo course → TeacherCourse (CREATOR) được tạo tự động

**Key Business Rules:**
- Course creator (teacher) tự động có full control
- TeacherCourse record (CREATOR role) được tạo khi course created
- Integration với Teacher Module qua created_by và teacher_courses

**Integration Points:**
- Teacher Module: Course.created_by FK, auto-create TeacherCourse
- Class Module: Classes reference course_id
```

## ⏳ PR 2.5 - Class Module

**Status:** ⏳ NOT STARTED
**Dependencies:** PR 2.3.1 Teacher Module, PR 2.4 Course Module
**Business Logic:** docs/modules/class-module-business-logic.md

```
Thực hiện Class Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: Entity relationships, complex queries
- api-design.md: Class API endpoints
- database-design.md: classes, class_schedules, class_sessions tables
- testing-guide.md: testing với relationships
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Class entity với relationships:
   - @ManyToOne Course (course_id FK)
   - name, code, max_students, status (UPCOMING, ONGOING, COMPLETED, CANCELLED)
   - start_date, end_date, location
2. Tạo ClassSchedule entity:
   - @ManyToOne Class
   - day_of_week, start_time, end_time, room
3. Tạo ClassSession entity:
   - @ManyToOne Class
   - session_date, session_number, status (SCHEDULED, COMPLETED, CANCELLED)
   - topic, notes
4. Tạo repositories với custom queries:
   - findByIdAndDeletedFalse
   - findByCourseId
   - findByStatus
5. Tạo ClassMapper (MapStruct)
6. Tạo ClassService và ClassServiceImpl với:
   - createClass (với schedules, UC-TEACHER-011)
   - Auto-assign MAIN_TEACHER via TeacherClass
   - generateSessions (từ schedules với recurrence rules)
   - getClassStudents (from Enrollment)
   - getClassSessions
   - Permission check integration (via TeacherService)
7. Tạo ClassController với endpoints theo api-design.md

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/clazz/
  - service/ClassServiceTest.java
  - controller/ClassControllerTest.java
  - repository/ClassRepositoryTest.java
  - mapper/ClassMapperTest.java
- src/test/java/com/kiteclass/core/testutil/
  - ClassTestDataBuilder.java

**Flyway Migration:**
- V5__create_class_tables.sql (classes, class_schedules, class_sessions)

**Verification:**
- mvn test phải pass
- Test session generation logic (recurrence rules)
- Coverage >= 80%
- Teacher assignment via TeacherClass hoạt động

**Key Business Rules:**
- Class phải có ít nhất 1 MAIN_TEACHER (BR-TEACHER-004)
- Sessions được generate tự động từ class schedules
- Teacher permissions check via TeacherClass/TeacherCourse

**Integration Points:**
- Teacher Module: TeacherClass for assignments, permission checks
- Course Module: Class.course_id FK
- Enrollment Module: Class-Student relationship
- Attendance Module: ClassSession-Attendance relationship
```

## ⏳ PR 2.6 - Enrollment Module

**Status:** ⏳ NOT STARTED
**Dependencies:** PR 2.3 Student Module, PR 2.5 Class Module
**Business Logic:** docs/modules/enrollment-module-business-logic.md

```
Thực hiện Enrollment Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: business logic patterns
- api-design.md: Enrollment API endpoints
- database-design.md: enrollments table
- testing-guide.md: testing business rules
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Enrollment entity với JPA annotations:
   - @ManyToOne Student
   - @ManyToOne Class
   - enrollment_date, start_date, end_date
   - tuition_amount, discount_percent, final_amount
   - status (ACTIVE, PENDING_PAYMENT, COMPLETED, WITHDRAWN, CANCELLED)
2. Tạo EnrollmentRepository với custom queries:
   - findByStudentIdAndClassId
   - existsByStudentIdAndClassIdAndStatus
   - countActiveEnrollmentsByClassId (capacity check)
3. Tạo EnrollmentMapper (MapStruct)
4. Tạo EnrollmentService và EnrollmentServiceImpl với:
   - enrollStudent (với business rule checks):
     - Class capacity check
     - Duplicate enrollment check
     - calculateFinalAmount (tuition - discount)
   - updateEnrollmentStatus
   - withdrawStudent
5. Tạo EnrollmentController với endpoints:
   - POST /api/v1/students/{id}/enroll
   - GET /api/v1/enrollments/{id}
   - PUT /api/v1/enrollments/{id}/status
6. Publish ENROLLMENT_CREATED event (cho Invoice auto-generation)

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/enrollment/
  - service/EnrollmentServiceTest.java (test business rules)
  - controller/EnrollmentControllerTest.java
  - repository/EnrollmentRepositoryTest.java
- src/test/java/com/kiteclass/core/testutil/
  - EnrollmentTestDataBuilder.java
- Test cases:
  - Enroll thành công
  - Class đã full → error
  - Student đã enrolled → error
  - Calculate discount correctly
  - Event publishing

**Flyway Migration:**
- V6__create_enrollment_tables.sql

**Verification:**
- mvn test phải pass
- Business rules được enforce đúng
- Coverage >= 80%
- ENROLLMENT_CREATED event được publish

**Key Business Rules:**
- Class capacity check trước khi enroll
- Không cho phép duplicate enrollment
- Auto-calculate final_amount = tuition_amount * (1 - discount_percent/100)

**Integration Points:**
- Student Module: Enrollment.student_id FK
- Class Module: Enrollment.class_id FK
- Invoice Module: ENROLLMENT_CREATED event triggers invoice generation
- Grade Module: Auto-initialize grade record when enrolled
```

## ⏳ PR 2.7 - Attendance Module

**Status:** ⏳ NOT STARTED
**Dependencies:** PR 2.3 Student Module, PR 2.5 Class Module, PR 2.3.1 Teacher Module
**Business Logic:** docs/modules/attendance-module-business-logic.md

```
Thực hiện Attendance Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Attendance API endpoints
- database-design.md: attendance table
- testing-guide.md: test patterns
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Attendance entity với JPA annotations:
   - @ManyToOne ClassSession
   - @ManyToOne Student
   - status (PRESENT, ABSENT, LATE, EXCUSED)
   - checkin_time, note
   - marked_by (teacher_id FK to teachers.id)
2. Tạo AttendanceRepository với custom queries:
   - findBySessionIdAndStudentId
   - findByClassIdAndDateRange
   - calculateAttendanceRateByStudent
3. Tạo AttendanceMapper (MapStruct)
4. Tạo AttendanceService và AttendanceServiceImpl với:
   - markAttendance (UC-TEACHER-007):
     - Permission check: Only MAIN_TEACHER or CREATOR
     - Bulk mark attendance for session
   - getAttendanceByClass (date range filter)
   - getStudentAttendanceStats (calculate attendance rate)
   - Permission check integration (via TeacherService)
5. Tạo AttendanceController với endpoints:
   - POST /api/v1/classes/{classId}/sessions/{sessionId}/attendance
   - GET /api/v1/classes/{classId}/attendance
   - GET /api/v1/students/{studentId}/attendance/stats
6. Publish ATTENDANCE_MARKED event (cho Grade Module update)

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/attendance/
  - service/AttendanceServiceTest.java
  - controller/AttendanceControllerTest.java
  - repository/AttendanceRepositoryTest.java
- src/test/java/com/kiteclass/core/testutil/
  - AttendanceTestDataBuilder.java
- Test cases:
  - Mark attendance cho multiple students
  - Update existing attendance
  - Get attendance statistics
  - Permission check (only MAIN_TEACHER)
  - Event publishing

**Flyway Migration:**
- V7__create_attendance_tables.sql

**Verification:**
- mvn test phải pass
- Event được publish correctly
- Coverage >= 80%
- Permission checks enforced (BR-TEACHER-008)

**Key Business Rules (from Teacher Module):**
- BR-TEACHER-008: Chỉ MAIN_TEACHER hoặc CREATOR mới có quyền điểm danh
- Attendance rate auto-calculated cho Grade Module

**Integration Points:**
- Class Module: Attendance.session_id FK to class_sessions
- Student Module: Attendance.student_id FK
- Teacher Module: Permission checks via TeacherService
- Grade Module: ATTENDANCE_MARKED event updates Attendance component score
```

## ⏳ PR 2.7.1 - Assignment Module

**Status:** ⏳ NOT STARTED
**Dependencies:** PR 2.5 Class Module, PR 2.3 Student Module, PR 2.3.1 Teacher Module
**Business Logic:** docs/modules/assignment-module-business-logic.md

```
Thực hiện Assignment Module - Assignment lifecycle, late penalties, grading workflow.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Assignment API endpoints
- database-design.md: assignments, submissions tables
- testing-guide.md: test patterns
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Assignment entity với JPA annotations:
   - @ManyToOne Class
   - title, description, instructions
   - due_date, max_score, weight_percent
   - allow_late_submission, late_penalty_percent
   - status (DRAFT, PUBLISHED, CLOSED)
   - created_by (teacher_id FK)
2. Tạo Submission entity:
   - @ManyToOne Assignment
   - @ManyToOne Student
   - submission_date, content_url, notes
   - score, adjusted_score (after late penalty)
   - status (PENDING, GRADED, RETURNED)
   - graded_by (teacher_id FK)
3. Tạo AssignmentRepository và SubmissionRepository với custom queries:
   - findByClassId
   - findByStudentId
   - findPendingGrading
4. Tạo AssignmentMapper và SubmissionMapper (MapStruct)
5. Tạo AssignmentService và AssignmentServiceImpl với:
   - createAssignment (UC-ASSIGN-001, permission check)
   - publishAssignment (UC-ASSIGN-002)
   - submitAssignment (UC-ASSIGN-003):
     - Late submission check
     - Calculate late penalty if applicable
   - gradeSubmission (UC-ASSIGN-004):
     - Permission check (only grader or MAIN_TEACHER)
     - Apply late penalty
     - Calculate adjusted_score
   - returnGradedAssignment (UC-ASSIGN-005)
   - Permission check integration (via TeacherService)
6. Tạo AssignmentController với endpoints theo api-design.md
7. Publish ASSIGNMENT_GRADED event (cho Grade Module)

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/assignment/
  - service/AssignmentServiceTest.java
  - controller/AssignmentControllerTest.java
  - repository/AssignmentRepositoryTest.java
  - mapper/AssignmentMapperTest.java
- src/test/java/com/kiteclass/core/testutil/
  - AssignmentTestDataBuilder.java
  - SubmissionTestDataBuilder.java
- Test cases:
  - Create and publish assignment
  - Submit on time vs late submission
  - Late penalty calculation
  - Grade submission (permission check)
  - Event publishing

**Flyway Migration:**
- V8__create_assignment_tables.sql (assignments, submissions)

**Verification:**
- mvn test phải pass
- Coverage >= 80%
- Late penalty calculated correctly
- ASSIGNMENT_GRADED event được publish
- Permission checks enforced

**Key Business Rules (from business-logic.md):**
- BR-ASSIGN-004: Late submissions get penalty (default 10% per day)
- BR-ASSIGN-005: Only assigned grader or MAIN_TEACHER can grade
- BR-ASSIGN-006: Late penalty calculation: adjusted_score = original_score * (1 - penalty%)
- Assignment weight_percent affects final grade calculation

**Integration Points:**
- Class Module: Assignment.class_id FK
- Student Module: Submission.student_id FK
- Teacher Module: Permission checks, assignment.created_by, submission.graded_by
- Grade Module: ASSIGNMENT_GRADED event updates Assignment component score
```

## ⏳ PR 2.7.2 - Grade Module

**Status:** ⏳ NOT STARTED
**Dependencies:** PR 2.7.1 Assignment Module, PR 2.7 Attendance Module
**Business Logic:** docs/modules/grade-module-business-logic.md

```
Thực hiện Grade Module - Weighted grade calculation, GPA, transcripts.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Grade API endpoints
- database-design.md: grades, grade_components, grading_scales, transcripts tables
- testing-guide.md: test patterns
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Grade entity với JPA annotations:
   - @ManyToOne Student
   - @ManyToOne Class
   - final_score (0-100), letter_grade (A+, A, B+, etc.), gpa (0-4.0)
   - status (IN_PROGRESS, FINALIZED, PASSED, FAILED)
   - pass_threshold (default 50), comments
   - calculated_at, finalized_at, finalized_by
2. Tạo GradeComponent entity:
   - @ManyToOne Grade
   - component_type (ATTENDANCE, ASSIGNMENT, MIDTERM, FINAL, QUIZ, PROJECT)
   - component_name, component_ref_id (assignment_id, etc.)
   - score, max_score, weight_percent, weighted_score
3. Tạo GradingScale entity (configuration):
   - scale_name (Standard), letter_grade, min_score, max_score, gpa_value
   - is_default
4. Tạo Transcript entity:
   - @ManyToOne Student
   - semester, academic_year, total_credits
   - semester_gpa, cumulative_gpa
   - total_courses, passed_courses, failed_courses
5. Tạo repositories với custom queries
6. Tạo GradeMapper (MapStruct)
7. Tạo GradeService và GradeServiceImpl với:
   - initializeGrade (UC-GRADE-001, auto on enrollment)
   - updateGradeComponent (UC-GRADE-002, event-driven):
     - Listen to ATTENDANCE_MARKED event
     - Listen to ASSIGNMENT_GRADED event
   - calculateFinalScore (UC-GRADE-003):
     - Validate weights = 100%
     - Calculate weighted scores
     - Map to letter grade and GPA
     - Determine pass/fail
   - finalizeGrade (UC-GRADE-004, permission check)
   - generateTranscript (UC-GRADE-009)
8. Tạo GradeController với endpoints theo api-design.md
9. Event listeners cho auto-update components

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/grade/
  - service/GradeServiceTest.java
  - controller/GradeControllerTest.java
  - repository/GradeRepositoryTest.java
  - mapper/GradeMapperTest.java
- src/test/java/com/kiteclass/core/testutil/
  - GradeTestDataBuilder.java
- Test cases:
  - Initialize grade on enrollment
  - Update component from attendance event
  - Update component from assignment event
  - Calculate final score (weighted average)
  - Letter grade mapping
  - GPA calculation
  - Finalize grade (validation)
  - Generate transcript

**Flyway Migration:**
- V9__create_grade_tables.sql (grades, grade_components, grading_scales, transcripts)

**Verification:**
- mvn test phải pass
- Coverage >= 80%
- Grade calculation accuracy verified
- Event-driven updates working
- Transcript generation tested

**Key Business Rules (from business-logic.md):**
- BR-GRADE-002: Component weights phải tổng = 100%
- BR-GRADE-003: Final score = Tổng weighted scores của components
- BR-GRADE-004: Letter grade mapping theo grading_scales table
- BR-GRADE-005: Pass/Fail: final_score >= pass_threshold
- BR-GRADE-006: Không finalize khi thiếu components
- BR-GRADE-007: FINALIZED grades read-only (chỉ ADMIN update được)
- BR-GRADE-008: Cumulative GPA = weighted average by credits

**Calculation Logic:**
```
1. Component Score → Weighted Score:
   normalized = score/max_score * 100
   weighted = normalized * weight% / 100

2. Weighted Scores → Final Score:
   final = sum of all weighted scores

3. Final Score → Letter Grade:
   lookup in grading_scales (e.g., 87.04 → B+)

4. Letter Grade → GPA:
   from grading_scales (B+ → 3.3)

5. Course GPAs → Cumulative GPA:
   weighted average by credits
```

**Integration Points:**
- Student Module: Grade.student_id FK
- Class Module: Grade.class_id FK
- Enrollment Module: ENROLLMENT_CREATED event → initializeGrade
- Attendance Module: ATTENDANCE_MARKED event → update Attendance component
- Assignment Module: ASSIGNMENT_GRADED event → update Assignment component
- Teacher Module: Permission checks for finalize
```

## ⏳ PR 2.8 - Invoice Module

**Status:** ⏳ NOT STARTED
**Dependencies:** PR 2.6 Enrollment Module, PR 2.3 Student Module, PR 2.5 Class Module
**Business Logic:** docs/modules/invoice-module-business-logic.md

```
Thực hiện Invoice Module - Hóa đơn học phí, trả góp, late fees, refund handling.

**Tuân thủ skills:**
- code-style.md: complex business logic
- api-design.md: Invoice API endpoints
- database-design.md: invoices, invoice_items, invoice_adjustments, installment_plans tables
- testing-guide.md: testing financial calculations
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Invoice entity với JPA annotations:
   - invoice_number (unique, auto-generated INV-YYYY-NNNNNN)
   - @ManyToOne Student
   - @ManyToOne Class
   - @ManyToOne Enrollment
   - total_amount, paid_amount, refund_amount
   - status (DRAFT, PENDING, PAID, OVERDUE, CANCELLED, REFUNDED)
   - due_date, issued_date, paid_at
2. Tạo InvoiceItem entity:
   - @ManyToOne Invoice
   - type (TUITION, MATERIALS, REGISTRATION_FEE, EXAM_FEE, OTHER)
   - description, quantity, unit_price, amount, paid_amount
3. Tạo InvoiceAdjustment entity:
   - @ManyToOne Invoice
   - type (DISCOUNT, ADDITIONAL_CHARGE, LATE_FEE, REFUND)
   - description, amount, paid_amount, reason
   - applied_by, applied_at
4. Tạo InstallmentPlan và Installment entities:
   - InstallmentPlan: @OneToOne Invoice, number_of_installments, status
   - Installment: @ManyToOne InstallmentPlan, installment_number, amount, due_date, status
5. Tạo RefundRequest entity:
   - @ManyToOne Invoice
   - refund_amount, refund_method, bank_account, reason
   - status (PENDING, APPROVED, REJECTED, COMPLETED)
6. Tạo repositories với custom queries
7. Tạo InvoiceMapper (MapStruct)
8. Tạo InvoiceService và InvoiceServiceImpl với:
   - createInvoiceForEnrollment (UC-INV-001, auto on ENROLLMENT_CREATED event):
     - Get course price
     - Create invoice với due_date = enrolled_at + 7 days
     - Create InvoiceItem (TUITION)
     - Publish INVOICE_CREATED event
   - generateQRCode (UC-INV-004)
   - applyPayment (UC-INV-006, listen to PAYMENT_COMPLETED event):
     - Payment allocation (late fees first, then items)
     - Update invoice status (PAID if balance = 0)
   - calculateLateFee (UC-INV-008, cron job daily):
     - 0.1% per day, max 10%
     - Create/update InvoiceAdjustment (LATE_FEE)
   - applyAdjustment (UC-INV-009, admin only)
   - processRefund (UC-INV-010, UC-INV-011)
9. Tạo InstallmentPlanService:
   - requestInstallmentPlan (UC-INV-005)
   - approveInstallmentPlan (UC-INV-007)
10. Tạo controllers

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/invoice/
  - service/InvoiceServiceTest.java
  - service/InstallmentPlanServiceTest.java
  - controller/InvoiceControllerTest.java
  - repository/InvoiceRepositoryTest.java
- src/test/java/com/kiteclass/core/testutil/
  - InvoiceTestDataBuilder.java
- Test cases:
  - Auto-create invoice on enrollment
  - Calculate late fee correctly
  - Payment allocation priority
  - Installment plan validation
  - Refund calculation
  - Event publishing

**Flyway Migration:**
- V10__create_invoice_tables.sql (invoices, invoice_items, invoice_adjustments, installment_plans, installments, refund_requests)

**Verification:**
- mvn test phải pass
- Coverage >= 80%
- Financial calculations chính xác
- Event-driven invoice creation working
- Late fee calculation tested

**Key Business Rules (from business-logic.md):**
- BR-INV-001: Auto-generate invoice on enrollment
- BR-INV-003: Installment plan validation (2-12 kỳ, sum = total)
- BR-INV-004: Late fee 0.1%/day, max 10%
- BR-INV-005: Payment allocation priority (late fees → items)
- BR-INV-008: Refund calculation based on class progress

**Integration Points:**
- Enrollment Module: ENROLLMENT_CREATED event → createInvoice
- Payment Module: PAYMENT_COMPLETED event → applyPayment
- Student Module: Invoice.student_id FK
- Class Module: Invoice.class_id FK, refund calculation
```

## ⏳ PR 2.8.1 - Payment Module

**Status:** ⏳ NOT STARTED
**Dependencies:** PR 2.8 Invoice Module
**Business Logic:** docs/modules/payment-module-business-logic.md

```
Thực hiện Payment Module - Payment processing, gateways, reconciliation.

**Tuân thủ skills:**
- code-style.md: complex business logic
- api-design.md: Payment API endpoints
- database-design.md: payments, payout_tasks tables
- testing-guide.md: testing payment flows
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Payment entity với JPA annotations:
   - @ManyToOne Invoice
   - @ManyToOne Student
   - amount, payment_method (CASH, BANK_TRANSFER, VNPAY, MOMO, ZALOPAY, CREDIT_CARD)
   - payment_type (INVOICE_PAYMENT, REFUND, ADJUSTMENT)
   - status (PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED)
   - transaction_id (unique), gateway_transaction_id
   - payment_url, gateway_response, expires_at
   - receipt_number, bank_transaction_id, transfer_date, proof_attachment_url
   - reference_payment_id (for refunds)
   - completed_at, failed_at, failure_reason
   - reconciled, reconciled_at
   - received_by
2. Tạo PayoutTask entity (for refunds via bank transfer):
   - @ManyToOne Payment
   - recipient_name, bank_account, bank_name, amount
   - status (PENDING, PROCESSING, COMPLETED, FAILED)
   - processed_by, processed_at
3. Tạo repositories với custom queries:
   - findByTransactionId (idempotency check)
   - findByStatusAndCreatedAtBefore (timeout detection)
   - findByCompletedAtBetween (reconciliation)
4. Tạo PaymentMapper (MapStruct)
5. Tạo PaymentService và PaymentServiceImpl với:
   - createPayment (UC-PAY-001)
   - initiateGatewayPayment (UC-PAY-002):
     - VNPay integration
     - MoMo integration
     - ZaloPay integration
   - processWebhook (UC-PAY-003):
     - Signature verification (BR-PAY-005)
     - Idempotency check (BR-PAY-001)
     - Update payment status
     - Publish PAYMENT_COMPLETED event
   - recordCashPayment (UC-PAY-004, staff only)
   - recordBankTransfer (UC-PAY-005, staff only)
   - processRefund (UC-PAY-010)
   - reconcilePayments (UC-PAY-009)
   - Cron job: cancelTimedOutPayments (15 minutes timeout)
6. Tạo Payment Gateway integrations:
   - VNPayService: initiate, webhook, refund
   - MoMoService: initiate, webhook, refund
   - ZaloPayService: initiate, webhook, refund
7. Tạo controllers:
   - PaymentController: create, view, retry
   - PaymentWebhookController: VNPay, MoMo, ZaloPay callbacks
   - InternalPaymentController: reconciliation (admin only)

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/payment/
  - service/PaymentServiceTest.java
  - service/VNPayServiceTest.java
  - controller/PaymentControllerTest.java
  - controller/PaymentWebhookControllerTest.java
  - repository/PaymentRepositoryTest.java
- src/test/java/com/kiteclass/core/testutil/
  - PaymentTestDataBuilder.java
- Test cases:
  - Create payment for invoice
  - Initiate VNPay payment
  - Process webhook (success/failed)
  - Signature verification
  - Idempotency check (duplicate webhook)
  - Timeout detection
  - Refund processing
  - Reconciliation logic
  - Event publishing

**Flyway Migration:**
- V11__create_payment_tables.sql (payments, payout_tasks)

**Verification:**
- mvn test phải pass
- Coverage >= 80%
- Webhook signature verification working
- Idempotency enforced
- Timeout detection tested
- PAYMENT_COMPLETED event được publish

**Key Business Rules (from business-logic.md):**
- BR-PAY-001: Payment uniqueness per transaction_id (idempotency)
- BR-PAY-003: Payment method validation rules
- BR-PAY-004: Payment amount validation (<= invoice balance)
- BR-PAY-005: Webhook signature verification (security)
- BR-PAY-006: Payment timeout auto-cancellation (15 minutes)
- BR-PAY-007: Daily payment reconciliation
- BR-PAY-008: Refund validation and processing

**Integration Points:**
- Invoice Module: Payment.invoice_id FK, PAYMENT_COMPLETED event
- Student Module: Payment.student_id FK
- VNPay/MoMo/ZaloPay: External payment gateways
```

## ⏳ PR 2.9 - Settings & Preferences Module

**Status:** ⏳ NOT STARTED
**Dependencies:** None (independent module)
**Note:** Parent Module moved to Engagement Service (P1 priority)

```
Thực hiện Settings & Preferences Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Settings API endpoints
- database-design.md: settings tables
- theme-system.md: branding settings
- spring-boot-testing-quality.md: code quality checklist

**Tasks:**
1. Tạo Branding entity (settings schema):
   - logo_url, favicon_url, display_name, tagline
   - primary_color, secondary_color, accent_color
   - contact_email, contact_phone, address
   - facebook_url, zalo_url, website_url
2. Tạo UserPreferences entity:
   - user_id (link to Gateway User via referenceId)
   - language (en, vi), timezone
   - theme (light, dark, auto)
   - notification_preferences (JSON)
3. Tạo BrandingRepository và UserPreferencesRepository
4. Tạo BrandingMapper và UserPreferencesMapper (MapStruct)
5. Tạo BrandingService và BrandingServiceImpl:
   - getBranding (default or customized)
   - updateBranding (admin only)
   - uploadLogo (S3 integration)
6. Tạo UserPreferencesService và UserPreferencesServiceImpl:
   - GET/PATCH /api/v1/users/me/preferences
   - initializeDefaultPreferences (on user registration)
7. Tạo controllers:
   - BrandingController:
     - GET /api/v1/settings/branding (public)
     - PUT /api/v1/settings/branding (admin only)
     - POST /api/v1/settings/branding/logo (admin only)
   - UserPreferencesController:
     - GET /api/v1/users/me/preferences
     - PATCH /api/v1/users/me/preferences

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/settings/
  - service/BrandingServiceTest.java
  - service/UserPreferencesServiceTest.java
  - controller/BrandingControllerTest.java
  - controller/UserPreferencesControllerTest.java
  - repository/BrandingRepositoryTest.java
- src/test/java/com/kiteclass/core/testutil/
  - BrandingTestDataBuilder.java
  - UserPreferencesTestDataBuilder.java

**Flyway Migration:**
- V12__create_settings_tables.sql (branding, user_preferences)

**Verification:**
- mvn test phải pass
- Coverage >= 80%
- Public branding API accessible without auth
- User preferences CRUD working

**Integration Points:**
- Gateway: Branding data fetched by Frontend for theme
- All modules: UserPreferences for user-specific settings
```

## ⏳ PR 2.10 - Core Docker & Final Integration

**Status:** ⏳ NOT STARTED
**Dependencies:** All Core Service PRs (2.1 - 2.9)

```
Hoàn thiện kiteclass-core - Docker, integration tests, seed data.

**Tuân thủ skills:**
- cloud-infrastructure.md: Docker, docker-compose
- testing-guide.md: integration tests với Testcontainers
- environment-setup.md: local dev setup
- spring-boot-testing-quality.md: integration test patterns

**Tasks:**
1. Tạo Dockerfile (multi-stage build):
   - Maven build stage
   - Runtime stage với optimized JRE
2. Update docker-compose.yml:
   - core-service
   - Kết nối với gateway, postgres, redis, rabbitmq
   - Health checks
   - Resource limits
3. Tạo V13__seed_test_data.sql (sample data cho testing):
   - Sample teachers
   - Sample courses
   - Sample classes
   - Sample students
   - Sample enrollments
   - Sample invoices
4. Viết integration tests cho full flows:
   - StudentFlowIntegrationTest (create → update → soft delete)
   - EnrollmentFlowIntegrationTest (enroll → invoice created → grade initialized)
   - AttendanceFlowIntegrationTest (mark attendance → grade updated)
   - AssignmentFlowIntegrationTest (create → submit → grade → grade updated)
   - InvoiceFlowIntegrationTest (create → payment → status update)
   - PaymentFlowIntegrationTest (gateway → webhook → invoice update)

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/integration/
  - StudentFlowIntegrationTest.java
  - EnrollmentFlowIntegrationTest.java
  - AttendanceFlowIntegrationTest.java
  - AssignmentFlowIntegrationTest.java
  - InvoiceFlowIntegrationTest.java
  - PaymentFlowIntegrationTest.java

**Verification:**
- docker-compose up phải start tất cả services successfully
- Integration tests pass với Testcontainers
- Swagger UI hoạt động: http://localhost:8081/swagger-ui.html
- Tất cả API endpoints hoạt động đúng
- Event-driven flows working (enrollment → invoice → grade)
- Health check endpoints responding
```

---

## ✅ PR 2.11 - Internal APIs for Gateway (CRITICAL FIX)

**Status:** ✅ COMPLETE (2026-01-27)
**Tests:** 10/10 passing (100%)
**Commit:** f13097f
**Dependencies:** PR 2.3 Student Module (completed)
**Unblocks:** PR 1.8 Gateway Cross-Service Integration

```
Tạo Internal APIs để Gateway có thể lấy profile data cho Student/Teacher/Parent.

**Vấn đề cần fix:**
- Core không có API nào cho Gateway gọi để lấy Student/Teacher/Parent profile
- Cần internal endpoints riêng, không expose ra public
- Cần authentication mechanism cho service-to-service calls

**Tuân thủ skills:**
- cross-service-data-strategy.md: Service-to-service communication patterns
- api-design.md: Internal API design
- code-style.md: Controller và Service conventions
- testing-guide.md: Testing internal APIs, filter testing
- spring-boot-testing-quality.md: Controller test setup, @TestConfiguration, fix warnings

**Tasks:**

### 1. Create Internal API Security
1. Tạo InternalRequestFilter:
   ```java
   @Component
   @Order(1)
   public class InternalRequestFilter extends OncePerRequestFilter {
       @Override
       protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain) {
           if (request.getRequestURI().startsWith("/internal/")) {
               String header = request.getHeader("X-Internal-Request");
               if (!"true".equals(header)) {
                   response.setStatus(403);
                   return;
               }
           }
           filterChain.doFilter(request, response);
       }
   }
   ```

2. Update SecurityConfig:
   ```java
   http.authorizeHttpRequests(auth -> auth
       .requestMatchers("/internal/**").permitAll()  // Handled by InternalRequestFilter
       .requestMatchers("/api/**").authenticated()
   );
   ```

### 2. Student Internal APIs
1. Tạo InternalStudentController:
   ```java
   @RestController
   @RequestMapping("/internal/students")
   public class InternalStudentController {

       @GetMapping("/{id}")
       public ResponseEntity<ApiResponse<StudentResponse>> getStudent(
               @PathVariable Long id,
               @RequestHeader("X-Internal-Request") String internalHeader) {
           // Already validated by InternalRequestFilter
           Student student = studentService.getById(id);
           return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(student)));
       }

       @PostMapping
       public ResponseEntity<ApiResponse<StudentResponse>> createStudent(
               @RequestBody @Valid CreateStudentRequest request,
               @RequestHeader("X-Internal-Request") String internalHeader) {
           Student student = studentService.create(request);
           return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(student)));
       }

       @DeleteMapping("/{id}")
       public ResponseEntity<ApiResponse<Void>> deleteStudent(
               @PathVariable Long id,
               @RequestHeader("X-Internal-Request") String internalHeader) {
           studentService.delete(id);
           return ResponseEntity.ok(ApiResponse.success(null));
       }
   }
   ```

### 3. Teacher Internal APIs (if Teacher module exists)
1. Tạo InternalTeacherController (tương tự Student):
   - GET /internal/teachers/{id}
   - POST /internal/teachers
   - DELETE /internal/teachers/{id}

### 4. Parent Internal APIs (if Parent module exists)
1. Tạo InternalParentController (tương tự Student):
   - GET /internal/parents/{id}
   - POST /internal/parents
   - DELETE /internal/parents/{id}

### 5. Update Student Module (if needed)
1. Nếu PR 2.3 chưa implement, cần đảm bảo:
   - StudentService có method getById(Long id)
   - StudentService có method create(CreateStudentRequest)
   - StudentService có method delete(Long id)
   - StudentMapper có method toResponse(Student)

### 6. Response DTOs cho Internal APIs
1. Tạo StudentResponse (nếu chưa có):
   ```java
   public class StudentResponse {
       private Long id;
       private String name;
       private String email;
       private String phoneNumber;
       private LocalDate dateOfBirth;
       private Gender gender;
       private StudentStatus status;
       private String address;
       // Không trả về sensitive data
   }
   ```

2. Tương tự cho TeacherResponse, ParentResponse

### 7. Error Handling
1. Update GlobalExceptionHandler:
   - Handle EntityNotFoundException → 404
   - Handle DuplicateResourceException → 409
   - Return consistent ApiResponse format

### 8. Documentation
1. Document internal APIs:
   ```
   # Internal APIs (Service-to-Service Only)

   ## Authentication
   All internal APIs require header: `X-Internal-Request: true`
   These endpoints are NOT accessible from public internet.

   ## Endpoints
   - GET /internal/students/{id} - Get student profile
   - POST /internal/students - Create student
   - DELETE /internal/students/{id} - Soft delete student
   ```

2. Add Swagger annotation để exclude internal APIs khỏi public docs:
   ```java
   @Hidden  // Hide from public Swagger UI
   @RestController
   @RequestMapping("/internal/students")
   ```

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/controller/internal/
  - InternalStudentControllerTest.java
  - InternalTeacherControllerTest.java (if applicable)
  - InternalParentControllerTest.java (if applicable)
- src/test/java/com/kiteclass/core/filter/
  - InternalRequestFilterTest.java
- src/test/java/com/kiteclass/core/integration/
  - InternalApiSecurityTest.java

**Test Cases Cần Cover:**
- GET /internal/students/{id} với X-Internal-Request header → 200 OK
- GET /internal/students/{id} KHÔNG CÓ header → 403 Forbidden
- GET /internal/students/999 → 404 Not Found
- POST /internal/students với valid data → 201 Created
- POST /internal/students với duplicate email → 409 Conflict
- DELETE /internal/students/{id} → 200 OK, soft delete

**Verification:**
- mvn test phải pass (thêm ~10-15 tests)
- Internal APIs chỉ accessible với X-Internal-Request header
- Swagger UI không hiển thị /internal/** endpoints
- Response format nhất quán với public APIs (ApiResponse wrapper)

**Configuration:**
1. Thêm logging cho internal API calls:
   ```java
   @Slf4j
   public class InternalRequestFilter {
       log.info("Internal API call: {} from Gateway", request.getRequestURI());
   }
   ```

**Security Considerations:**
- X-Internal-Request header là simple check, chỉ phù hợp với internal network
- Trong production, nên thêm:
  - IP whitelist (chỉ accept từ Gateway IP)
  - Service-to-service JWT
  - mTLS (mutual TLS)
- Document trong architecture-overview.md

**Dependencies cho Gateway PR 1.8:**
Sau khi PR này complete, Gateway có thể:
- Call GET /internal/students/{id} để lấy student profile khi login
- Call POST /internal/students để tạo student khi registration
- Call DELETE /internal/students/{id} khi xóa user account

**Note về Teacher và Parent:**
- Nếu Teacher/Parent modules chưa có trong PR 2.3-2.9, có thể skip phần đó
- Chỉ cần implement Student Internal APIs là đủ để test pattern
- Có thể thêm Teacher/Parent Internal APIs sau khi modules đó được implement
```

---

# GIAI ĐOẠN 3: KITECLASS-FRONTEND

## ✅ PR 3.1 - Frontend Project Setup & Testing Infrastructure

```
Thực hiện Phase 1 của kiteclass-frontend-plan.md + Testing setup.

**Tuân thủ skills:**
- frontend-development.md: UI design system, Shadcn/UI patterns
- frontend-code-quality.md: TypeScript strict mode, ESLint config, Testing setup
- architecture-overview.md: cấu trúc thư mục Frontend

**Tasks:**
1. ✅ Tạo Next.js project: kiteclass/kiteclass-frontend/
2. ✅ Install core dependencies (React Query, Zustand, Axios, Zod, etc.)
3. ✅ Setup Shadcn/UI với 23 components
4. ✅ Cấu hình Tailwind với custom theme
5. ✅ Tạo folder structure theo plan
6. ⏳ BỔ SUNG: Enhanced ESLint configuration
   - Install @typescript-eslint/eslint-plugin
   - Install @typescript-eslint/parser
   - Install eslint-plugin-react-hooks
   - Configure rules: no-explicit-any: error, react-hooks rules
7. ⏳ BỔ SUNG: Complete TypeScript strict config
   - Add noUnusedLocals, noUnusedParameters
   - Add noImplicitReturns, noFallthroughCasesInSwitch
   - Add noUncheckedIndexedAccess
8. ⏳ BỔ SUNG: Testing Infrastructure
   - Install Vitest + @vitejs/plugin-react
   - Install @testing-library/react, @testing-library/jest-dom
   - Install @testing-library/user-event
   - Install MSW (Mock Service Worker)
   - Install @playwright/test
   - Create vitest.config.ts
   - Create src/test/setup.ts
   - Create playwright.config.ts
   - Add test scripts to package.json

**Verification:**
- pnpm dev phải start thành công
- pnpm lint không có errors (with strict rules)
- pnpm tsc --noEmit passes
- pnpm test runs (even with no tests yet)
- pnpm test:e2e setup ready
- Git hooks check passed

**Quality Checklist (frontend-code-quality.md):**
- [x] tsconfig.json has strict: true
- [x] Basic ESLint setup
- [x] Prettier plugin for Tailwind installed
- [x] No `any` types in codebase
- [ ] ESLint with @typescript-eslint/no-explicit-any: error
- [ ] Vitest configured
- [ ] Testing Library installed
- [ ] MSW installed for API mocking
- [ ] Playwright configured for E2E
```

## ⏳ PR 3.2 - Frontend Core Infrastructure

```
Thực hiện Phase 2 của kiteclass-frontend-plan.md.

**Tuân thủ skills:**
- frontend-code-quality.md: TypeScript types, testing patterns
- api-design.md: API response format
- enums-constants.md: TypeScript enum definitions

**Tasks:**
1. Tạo API client (src/lib/api/client.ts):
   - Axios instance với interceptors
   - Auto refresh token
   - Error handling
   - Proper TypeScript types (NO any!)
2. Tạo API endpoints config (src/lib/api/endpoints.ts)
3. Tạo TypeScript types (src/types/):
   - api.ts (ApiResponse, PageResponse, ErrorResponse)
   - student.ts, class.ts, course.ts
   - attendance.ts, invoice.ts
   - user.ts
   - Match Backend DTOs exactly
4. Tạo Zustand stores:
   - auth-store.ts (with TypeScript interface)
   - ui-store.ts

**Tests (bắt buộc - frontend-code-quality.md Part 3):**
- src/__tests__/lib/api/
  - client.test.ts (test interceptors, error handling)
- src/__tests__/stores/
  - auth-store.test.ts
  - ui-store.test.ts
- Use MSW for API mocking

**Verification:**
- pnpm test phải pass (minimum 80% coverage)
- pnpm lint passes
- pnpm tsc --noEmit passes
- Types khớp với BE DTOs
- No `any` types in codebase

**Quality Checklist (frontend-code-quality.md Part 8):**
- [ ] All types properly defined (no `any`)
- [ ] API client has proper error handling
- [ ] Tests use MSW for API mocking
- [ ] Zustand stores have TypeScript interfaces
```

## ⏳ PR 3.3 - Providers & Layout

```
Thực hiện Phase 3-5 của kiteclass-frontend-plan.md.

**Tuân thủ skills:**
- ui-components.md: layout patterns
- theme-system.md: ThemeProvider implementation
- code-style.md: React component conventions

**Tasks:**
1. Tạo Providers:
   - QueryProvider (React Query)
   - ThemeProvider (next-themes + API theme)
   - AuthProvider (protected routes)
   - ToasterProvider
2. Tạo root layout với providers
3. Tạo Layout components:
   - Sidebar với navigation config
   - Header với UserNav, ThemeToggle
   - Breadcrumb
4. Tạo Dashboard layout (src/app/(dashboard)/layout.tsx)
5. Tạo Auth layout (src/app/(auth)/layout.tsx)

**Tests (bắt buộc):**
- src/__tests__/providers/
  - auth-provider.test.tsx
  - theme-provider.test.tsx
- src/__tests__/components/layout/
  - sidebar.test.tsx
  - header.test.tsx

**Verification:**
- pnpm test phải pass
- Layout renders correctly
```

## ⏳ PR 3.4 - Shared Components

```
Tạo shared components theo kiteclass-frontend-plan.md.

**Tuân thủ skills:**
- ui-components.md: component patterns, design tokens
- code-style.md: React/TypeScript conventions

**Tasks:**
1. Tạo shared components (src/components/shared/):
   - page-header.tsx
   - loading-spinner.tsx
   - status-badge.tsx
   - empty-state.tsx
   - stats-card.tsx
   - confirm-dialog.tsx
   - data-table.tsx (với pagination, sorting)
   - data-table-toolbar.tsx
   - data-table-pagination.tsx

**Tests (bắt buộc - mỗi component 1 test file):**
- src/__tests__/components/shared/
  - page-header.test.tsx
  - loading-spinner.test.tsx
  - status-badge.test.tsx
  - empty-state.test.tsx
  - stats-card.test.tsx
  - confirm-dialog.test.tsx
  - data-table.test.tsx

**Verification:**
- pnpm test phải pass
- Components render correctly với các props
```

## ⏳ PR 3.5 - Auth Pages

```
Thực hiện Auth pages của kiteclass-frontend-plan.md.

**Tuân thủ skills:**
- ui-components.md: form conventions
- api-design.md: Auth API endpoints
- code-style.md: React Hook Form + Zod patterns

**Tasks:**
1. Tạo Zod validations (src/lib/validations/auth.ts)
2. Tạo useAuth hook (src/hooks/use-auth.ts):
   - useLogin mutation
   - useLogout mutation
3. Tạo Auth pages:
   - src/app/(auth)/login/page.tsx
   - src/app/(auth)/forgot-password/page.tsx
4. Integrate với Gateway Auth API

**Tests (bắt buộc):**
- src/__tests__/hooks/
  - use-auth.test.ts (với MSW mock)
- src/__tests__/app/auth/
  - login-page.test.tsx
  - forgot-password-page.test.tsx
- Test cases:
  - Login success -> redirect to dashboard
  - Login failure -> show error
  - Form validation

**Verification:**
- pnpm test phải pass
- Login với owner@kiteclass.local / Admin@123 thành công
```

## ⏳ PR 3.6 - Dashboard & Students Module

```
Thực hiện Dashboard và Students module.

**Tuân thủ skills:**
- ui-components.md: page layout, data table
- api-design.md: Student API endpoints
- code-style.md: React Query hooks pattern
- testing-guide.md: hook testing với MSW

**Tasks:**
1. Tạo Dashboard page với stats cards
2. Tạo useStudents hook (src/hooks/use-students.ts):
   - useStudents (list với pagination)
   - useStudent (single)
   - useCreateStudent
   - useUpdateStudent
   - useDeleteStudent
3. Tạo Zod validation (src/lib/validations/student.ts)
4. Tạo StudentForm component
5. Tạo Students pages:
   - src/app/(dashboard)/students/page.tsx (list)
   - src/app/(dashboard)/students/[id]/page.tsx (detail)
   - src/app/(dashboard)/students/new/page.tsx (create)
   - src/app/(dashboard)/students/[id]/edit/page.tsx (edit)

**Tests (bắt buộc):**
- src/__tests__/hooks/
  - use-students.test.ts
- src/__tests__/components/forms/
  - student-form.test.tsx
- src/__tests__/app/dashboard/
  - dashboard-page.test.tsx
  - students-list-page.test.tsx
  - student-detail-page.test.tsx

**Verification:**
- pnpm test phải pass
- CRUD operations hoạt động với Core API
```

## ⏳ PR 3.7 - Courses & Classes Module

```
Thực hiện Courses và Classes module.

**Tuân thủ skills:**
- ui-components.md: tabs, complex forms
- api-design.md: Course, Class API endpoints
- code-style.md: React patterns

**Tasks:**
1. Tạo useCourses hook
2. Tạo useClasses, useClassSessions hooks
3. Tạo validation schemas
4. Tạo CourseForm, ClassForm components
5. Tạo pages:
   - Courses: list, detail, create/edit
   - Classes: list, detail (với tabs), create/edit
   - Class detail tabs: Info, Students, Sessions

**Tests (bắt buộc):**
- src/__tests__/hooks/
  - use-courses.test.ts
  - use-classes.test.ts
- src/__tests__/components/forms/
  - course-form.test.tsx
  - class-form.test.tsx
- src/__tests__/app/dashboard/
  - courses-page.test.tsx
  - classes-page.test.tsx
  - class-detail-page.test.tsx

**Verification:**
- pnpm test phải pass
- Class schedules hiển thị đúng
```

## ⏳ PR 3.8 - Attendance Module

```
Thực hiện Attendance module.

**Tuân thủ skills:**
- ui-components.md: form với nhiều items
- api-design.md: Attendance API endpoints
- code-style.md: React patterns

**Tasks:**
1. Tạo useAttendance hook:
   - useAttendance (get by class/date)
   - useMarkAttendance mutation
2. Tạo AttendanceForm component:
   - Hiển thị list students
   - Select status cho mỗi student
   - Bulk actions (mark all present)
3. Tạo pages:
   - Attendance overview (by date)
   - Class attendance marking page

**Tests (bắt buộc):**
- src/__tests__/hooks/
  - use-attendance.test.ts
- src/__tests__/components/forms/
  - attendance-form.test.tsx
- src/__tests__/app/dashboard/
  - attendance-page.test.tsx

**Verification:**
- pnpm test phải pass
- Mark attendance cho class hoạt động
```

## ⏳ PR 3.9 - Billing Module

```
Thực hiện Billing module.

**Tuân thủ skills:**
- ui-components.md: data display, forms
- api-design.md: Invoice & Payment API endpoints
- code-style.md: React patterns

**Tasks:**
1. Tạo useInvoices, usePayments hooks
2. Tạo InvoiceForm, PaymentForm components
3. Tạo pages:
   - Invoices list với filters (status, date range)
   - Invoice detail với payment history
   - Create invoice
   - Record payment dialog

**Tests (bắt buộc):**
- src/__tests__/hooks/
  - use-invoices.test.ts
  - use-payments.test.ts
- src/__tests__/components/forms/
  - invoice-form.test.tsx
  - payment-form.test.tsx
- src/__tests__/app/dashboard/
  - invoices-page.test.tsx
  - invoice-detail-page.test.tsx

**Verification:**
- pnpm test phải pass
- Invoice totals hiển thị đúng
```

## ⏳ PR 3.10 - Settings & Branding

```
Thực hiện Settings module.

**Tuân thủ skills:**
- ui-components.md: form patterns
- api-design.md: Settings API endpoints
- theme-system.md: branding integration

**Tasks:**
1. Tạo useBranding hook:
   - useBranding (get)
   - useUpdateBranding mutation
   - useUploadLogo mutation
2. Tạo BrandingForm component:
   - Logo upload với preview
   - Color picker
   - Contact info fields
3. Tạo pages:
   - Settings layout với tabs
   - Branding settings page
   - Profile settings page
4. Integrate branding với ThemeProvider

**Tests (bắt buộc):**
- src/__tests__/hooks/
  - use-branding.test.ts
- src/__tests__/components/forms/
  - branding-form.test.tsx
- src/__tests__/app/dashboard/
  - settings-page.test.tsx
  - branding-page.test.tsx

**Verification:**
- pnpm test phải pass
- Logo upload hoạt động
- Color changes apply real-time
```

## ⏳ PR 3.11 - E2E Tests & Polish

```
Hoàn thiện Frontend với E2E tests.

**Tuân thủ skills:**
- testing-guide.md: E2E test patterns với Playwright

**Tasks:**
1. Setup Playwright
2. Viết E2E tests:
   - auth.spec.ts: login, logout flow
   - students.spec.ts: CRUD operations
   - classes.spec.ts: create class, add students
   - attendance.spec.ts: mark attendance
   - billing.spec.ts: create invoice, record payment
3. Polish UI:
   - Loading states
   - Error states
   - Empty states
   - Responsive design fixes

**Tests (bắt buộc):**
- e2e/
  - auth.spec.ts
  - students.spec.ts
  - classes.spec.ts
  - attendance.spec.ts
  - billing.spec.ts

**Verification:**
- pnpm test phải pass
- pnpm test:e2e phải pass
- UI hoạt động smooth trên mobile
```

---

# HƯỚNG DẪN SỬ DỤNG

## Branch Strategy (UPDATED 2026-01-27)

**NEW STRATEGY:** Merge to main after milestones, create feature branches from main

```
main (always up-to-date with completed work)
├── feature/gateway-cross-service (PR 1.8 - active)
├── feature/core-modules (PR 2.4+ - future)
└── feature/frontend (PR 3.1+ - future)
```

**OLD branches (already merged, can delete):**
- ~~feature/gateway~~ → merged to main (PRs 1.1-1.6)
- ~~feature/core~~ → merged to main (PRs 2.1-2.3, 2.11)

**New Workflow:**
1. Work on feature branch
2. Complete PR(s) with tests
3. Merge feature branch → main (keep code unified)
4. Create new feature branch from main for next work
5. Repeat

**Benefits:**
- ✅ Code always unified in main
- ✅ No long-lived feature branches
- ✅ Easier to switch between different features
- ✅ Conflicts resolved incrementally

## Commit Convention

Format ngắn gọn:
```
feat(gateway): PR 1.1 - project setup
feat(gateway): PR 1.2 - common components
feat(core): PR 2.3 - student module
fix(frontend): PR 3.5 - login validation
```

## Workflow cho mỗi PR

```
1. Checkout branch: git checkout feature/{service}
2. Copy prompt vào Claude
3. Claude thực hiện code + tests
4. Chạy tests: mvn test (BE) hoặc pnpm test (FE)
5. Review code
6. Commit ngắn gọn: git commit -m "feat(service): PR X.X - description"
7. Tiếp tục PR tiếp theo trên cùng branch
```

## Test Coverage Requirements

| Layer | Minimum Coverage |
|-------|-----------------|
| Service | 80% |
| Controller | 70% |
| Repository | 60% |
| React Hooks | 80% |
| React Components | 70% |

## Thư mục Tests

```
# Backend (Java)
src/test/java/com/kiteclass/{service}/
├── module/{name}/
│   ├── service/
│   ├── controller/
│   └── repository/
├── integration/
└── testutil/

# Frontend (TypeScript)
src/__tests__/
├── hooks/
├── components/
│   ├── shared/
│   ├── forms/
│   └── layout/
├── app/
│   ├── auth/
│   └── dashboard/
└── lib/

e2e/
├── auth.spec.ts
└── ...
```

## Dependencies

```
Gateway:  1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 1.6 → 1.7
                                                ↓
                                              1.8 ← (wait for 2.11)
                                                ↓
Core:     2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 2.7 → 2.8 → 2.9 → 2.10
                      ↓                                             ↓
                    2.11 (Internal APIs) ──────────────────────────┘
                      ↓
Frontend: 3.1 → 3.2 → 3.3 → 3.4 → 3.5 ←─────────────────────────────┘
                                  ↓
          3.6 → 3.7 → 3.8 → 3.9 → 3.10 → 3.11
```

**CRITICAL PATH (must complete first):**
1. PR 2.3 (Student Module) - Tạo Student entity và service
2. PR 2.11 (Internal APIs) - Tạo internal endpoints cho Gateway
3. PR 1.8 (Cross-Service Integration) - Connect Gateway với Core
4. Continue with remaining PRs

**Why this order?**
- PR 2.11 cần Student entity từ PR 2.3
- PR 1.8 cần internal APIs từ PR 2.11
- Frontend development cần completed authentication flow từ PR 1.8

## Tổng kết

| Giai đoạn | PRs | Có Tests | Status |
|-----------|-----|----------|--------|
| Gateway | 8 | 7 (từ 1.2) | ⚠️ 7/8 complete, PR 1.8 next (unblocked) |
| Core | 11 | 10 (từ 2.2) | ✅ 4/11 complete, PR 2.11 DONE |
| Frontend | 11 | 10 (từ 3.2) | ⏳ Not started |
| **Tổng** | **30** | **27** | **11/30 completed (36.7%)** |

**Recent Updates (2026-01-27):**
- ✅ PR 2.3 Student Module COMPLETE
  - Commits: 92a9979 (implementation), fa348df (test fixes)
  - 40 tests passing (100%)

- ✅ PR 2.11 Internal APIs COMPLETE
  - Commit: f13097f
  - InternalRequestFilter (security for /internal/** endpoints)
  - InternalStudentController (GET/POST/DELETE)
  - 10 tests passing (100%)
  - Unblocks PR 1.8 Gateway Integration

## 🚨 Critical Issues Found

**Architecture Gap:** Cross-service data linking between Gateway User and Core entities (Student/Teacher/Parent) was missing from original design.

**Solution:** UserType + ReferenceId pattern documented in:
- `.claude/skills/cross-service-data-strategy.md`
- `.claude/skills/architecture-overview.md`
- `documents/plans/database-design.md`
- `documents/reports/gateway-core-separation-rationale.md`

**Implementation Required:**
1. **PR 2.11** - Core Internal APIs (must do FIRST)
2. **PR 1.8** - Gateway Cross-Service Integration (depends on 2.11)

**Impact:** Cannot proceed with Core development (PR 2.3+) until cross-service pattern is implemented, as Student/Teacher/Parent entities need to integrate with Gateway authentication.

---

# FRONTEND SERVICE PRs

## PR 3.1: Project Setup & Core Infrastructure

**Branch:** feature/frontend  
**Prerequisites:** None (can start immediately)  
**Dependencies:** Next.js 14, TypeScript, Tailwind CSS, Shadcn/UI

### Tasks:
1. Create Next.js project with TypeScript and Tailwind
2. Install core dependencies (React Query, Zustand, Axios, Zod, React Hook Form)
3. Setup Shadcn/UI with essential components
4. Configure project structure (app router, src directory)
5. Setup API client with Axios interceptors
6. Configure environment variables
7. Setup Prettier and ESLint
8. Create base layout and providers (QueryProvider, ThemeProvider)

### Files:
- `package.json` - Dependencies
- `tsconfig.json` - TypeScript config
- `tailwind.config.ts` - Tailwind + Shadcn config
- `src/lib/api/client.ts` - Axios instance
- `src/lib/api/endpoints.ts` - API endpoints
- `src/app/providers.tsx` - Global providers
- `src/app/layout.tsx` - Root layout

### Tests:
- API client configuration tests
- Provider rendering tests

---

## PR 3.2: Shared Components & Layout System

**Branch:** feature/frontend  
**Prerequisites:** PR 3.1  
**Depends on Backend:** None (pure UI)

### Tasks:
1. Create Sidebar component with navigation
2. Create Header component with user menu
3. Create Breadcrumb component
4. Create DataTable component (reusable)
5. Create shared components (PageHeader, LoadingSpinner, EmptyState, StatusBadge, etc.)
6. Setup dark/light theme toggle
7. Create dashboard layout structure

### Files:
- `src/components/layout/sidebar/` - Sidebar components
- `src/components/layout/header/` - Header components
- `src/components/tables/data-table.tsx` - Reusable table
- `src/components/shared/` - Shared UI components
- `src/app/(dashboard)/layout.tsx` - Dashboard layout

### Tests:
- Component rendering tests
- Theme toggle tests
- Navigation tests

---

## PR 3.3: Authentication Pages

**Branch:** feature/frontend  
**Prerequisites:** PR 3.2  
**Depends on Backend:** PR 1.4 (Auth Module)

### Tasks:
1. Create Login page with form validation
2. Create Forgot Password page
3. Create Reset Password page
4. Implement useAuth hook with React Query
5. Setup JWT token management
6. Implement auth middleware/guards
7. Add error handling for auth flows

### Files:
- `src/app/(auth)/login/page.tsx`
- `src/app/(auth)/forgot-password/page.tsx`
- `src/app/(auth)/reset-password/page.tsx`
- `src/hooks/use-auth.ts`
- `src/lib/auth.ts` - Token management
- `src/middleware.ts` - Auth guards

### Tests:
- Login form validation tests
- Auth hook tests
- Token management tests
- Protected route tests

---

## PR 3.4: Student Management Pages

**Branch:** feature/frontend  
**Prerequisites:** PR 3.3  
**Depends on Backend:** PR 2.3 (Student Module)

### Tasks:
1. Create Students list page with search/filter
2. Create Student detail page
3. Create Create Student form
4. Create Edit Student form
5. Implement useStudents hook (list, get, create, update, delete)
6. Add student columns for DataTable
7. Implement soft delete confirmation

### Files:
- `src/app/(dashboard)/students/page.tsx` - List
- `src/app/(dashboard)/students/[id]/page.tsx` - Detail
- `src/app/(dashboard)/students/new/page.tsx` - Create
- `src/app/(dashboard)/students/[id]/edit/page.tsx` - Edit
- `src/hooks/use-students.ts` - React Query hooks
- `src/components/forms/student-form.tsx`
- `src/components/tables/columns/student-columns.tsx`

### Tests:
- Student list rendering tests
- Student form validation tests
- CRUD operations tests
- Search/filter tests

---

## PR 3.5: Teacher Management Pages

**Branch:** feature/frontend  
**Prerequisites:** PR 3.3  
**Depends on Backend:** PR 2.3.1 (Teacher Module)

### Tasks:
1. Create Teachers list page with search/filter
2. Create Teacher detail page
3. Create Create Teacher form
4. Create Edit Teacher form
5. Implement useTeachers hook (list, get, create, update, delete)
6. Add teacher columns for DataTable
7. Display teacher status (ACTIVE, ON_LEAVE, TERMINATED)

### Files:
- `src/app/(dashboard)/teachers/page.tsx` - List
- `src/app/(dashboard)/teachers/[id]/page.tsx` - Detail
- `src/app/(dashboard)/teachers/new/page.tsx` - Create
- `src/app/(dashboard)/teachers/[id]/edit/page.tsx` - Edit
- `src/hooks/use-teachers.ts`
- `src/components/forms/teacher-form.tsx`
- `src/components/tables/columns/teacher-columns.tsx`

### Tests:
- Teacher list rendering tests
- Teacher form validation tests
- CRUD operations tests
- Status badge tests

---

## PR 3.6: Course Management Pages

**Branch:** feature/frontend  
**Prerequisites:** PR 3.5  
**Depends on Backend:** PR 2.4 (Course Module)

### Tasks:
1. Create Courses list page with filters (status, teacher)
2. Create Course detail page with lifecycle actions
3. Create Create Course form
4. Create Edit Course form (with restrictions based on status)
5. Implement useCourses hook
6. Add publish/archive/delete actions
7. Display course status (DRAFT, PUBLISHED, ARCHIVED)
8. Show validation errors (missing required fields)

### Files:
- `src/app/(dashboard)/courses/page.tsx`
- `src/app/(dashboard)/courses/[id]/page.tsx`
- `src/app/(dashboard)/courses/new/page.tsx`
- `src/app/(dashboard)/courses/[id]/edit/page.tsx`
- `src/hooks/use-courses.ts`
- `src/components/forms/course-form.tsx`
- `src/components/tables/columns/course-columns.tsx`

### Tests:
- Course list rendering tests
- Course lifecycle tests (publish, archive)
- Form validation tests (required fields for publish)
- Edit restrictions tests (ARCHIVED read-only, PUBLISHED limited edit)

---

## PR 3.7: Class Management Pages

**Branch:** feature/frontend  
**Prerequisites:** PR 3.6  
**Depends on Backend:** PR 2.5 (Class Module)

### Tasks:
1. Create Classes list page with filters
2. Create Class detail page with student roster
3. Create Create Class form (select course, assign teacher)
4. Create Schedule management
5. Implement useClasses hook
6. Add student enrollment to class
7. Display class status and schedule

### Files:
- `src/app/(dashboard)/classes/page.tsx`
- `src/app/(dashboard)/classes/[id]/page.tsx`
- `src/app/(dashboard)/classes/[id]/students/page.tsx`
- `src/app/(dashboard)/classes/new/page.tsx`
- `src/hooks/use-classes.ts`
- `src/components/forms/class-form.tsx`
- `src/components/tables/columns/class-columns.tsx`

### Tests:
- Class list rendering tests
- Student enrollment tests
- Schedule display tests
- Form validation tests

---

## PR 3.8: Attendance Management

**Branch:** feature/frontend  
**Prerequisites:** PR 3.7  
**Depends on Backend:** PR 2.7 (Attendance Module)

### Tasks:
1. Create Attendance overview page (calendar view)
2. Create Take Attendance page (for specific class session)
3. Create Attendance reports page
4. Implement useAttendance hook
5. Add attendance status (PRESENT, ABSENT, LATE, EXCUSED)
6. Display attendance statistics
7. Export attendance reports

### Files:
- `src/app/(dashboard)/attendance/page.tsx` - Overview
- `src/app/(dashboard)/classes/[id]/attendance/page.tsx` - Take attendance
- `src/hooks/use-attendance.ts`
- `src/components/forms/attendance-form.tsx`
- `src/components/shared/attendance-calendar.tsx`

### Tests:
- Attendance marking tests
- Calendar view tests
- Report generation tests
- Statistics calculation tests

---

## PR 3.9: Billing Pages (Invoices & Payments)

**Branch:** feature/frontend  
**Prerequisites:** PR 3.7  
**Depends on Backend:** PR 2.8 (Invoice Module), PR 2.8.1 (Payment Module)

### Tasks:
1. Create Invoices list page with filters
2. Create Invoice detail page
3. Create Generate Invoice form
4. Create Payments list page
5. Create Record Payment form
6. Implement useInvoices and usePayments hooks
7. Display invoice status (DRAFT, SENT, PAID, OVERDUE, CANCELLED)
8. Display payment methods and history
9. Add payment reminders

### Files:
- `src/app/(dashboard)/billing/invoices/page.tsx`
- `src/app/(dashboard)/billing/invoices/[id]/page.tsx`
- `src/app/(dashboard)/billing/payments/page.tsx`
- `src/hooks/use-invoices.ts`
- `src/hooks/use-payments.ts`
- `src/components/forms/invoice-form.tsx`
- `src/components/tables/columns/invoice-columns.tsx`

### Tests:
- Invoice list rendering tests
- Payment recording tests
- Invoice status tests
- Payment history tests

---

## PR 3.10: Parent Portal

**Branch:** feature/frontend  
**Prerequisites:** PR 3.8, PR 3.9  
**Depends on Backend:** PR 2.9 (Settings & Parent linking)

### Tasks:
1. Create Parent dashboard page
2. Create Children list/detail pages
3. Create Child attendance view
4. Create Child grades view
5. Create Parent invoices view
6. Implement useParent hook
7. Display child performance overview

### Files:
- `src/app/(parent)/page.tsx` - Parent dashboard
- `src/app/(parent)/children/[id]/page.tsx` - Child detail
- `src/app/(parent)/children/[id]/attendance/page.tsx`
- `src/app/(parent)/children/[id]/grades/page.tsx`
- `src/app/(parent)/invoices/page.tsx`
- `src/hooks/use-parent.ts`

### Tests:
- Parent dashboard tests
- Child info display tests
- Attendance view tests
- Invoice access tests

---

## PR 3.11: Settings & Reports

**Branch:** feature/frontend  
**Prerequisites:** PR 3.10  
**Depends on Backend:** PR 2.9 (Settings Module)

### Tasks:
1. Create Settings page (profile, branding, preferences)
2. Create Profile edit page
3. Create Branding configuration (logo, colors)
4. Create Reports dashboard
5. Implement useSettings and useBranding hooks
6. Add analytics charts
7. Export reports functionality

### Files:
- `src/app/(dashboard)/settings/page.tsx`
- `src/app/(dashboard)/settings/profile/page.tsx`
- `src/app/(dashboard)/settings/branding/page.tsx`
- `src/app/(dashboard)/reports/page.tsx`
- `src/hooks/use-settings.ts`
- `src/hooks/use-branding.ts`
- `src/components/charts/` - Chart components

### Tests:
- Settings update tests
- Branding upload tests
- Report generation tests
- Chart rendering tests

---

## Frontend Testing Strategy

### Unit Tests (Jest + React Testing Library)
- Component rendering tests
- Form validation tests
- Hook tests (React Query)
- Utility function tests

### Integration Tests
- Page flow tests (login → dashboard → CRUD operations)
- API integration tests (MSW for mocking)
- Form submission tests
- Navigation tests

### E2E Tests (Playwright - Optional)
- Critical user flows
- Authentication flows
- CRUD operations
- Multi-page workflows

### Test Coverage Target: 80%+

---

## Frontend Development Guidelines

1. **Component Structure:** Atomic design (atoms → molecules → organisms)
2. **State Management:** React Query for server state, Zustand for client state
3. **Form Handling:** React Hook Form + Zod validation
4. **API Calls:** Centralized in custom hooks using React Query
5. **Styling:** Tailwind CSS utility classes + Shadcn components
6. **Error Handling:** Consistent error boundaries and toast notifications
7. **Loading States:** Skeleton loaders from Shadcn
8. **Accessibility:** WCAG 2.1 AA compliance
9. **Responsive:** Mobile-first approach
10. **Performance:** Code splitting, lazy loading, image optimization

---


---

## 📋 PAIRED PRs TRACKING TABLE

| Backend PR | Status | Frontend PR | Status | Can Start | Notes |
|------------|--------|-------------|--------|-----------|-------|
| **Infrastructure** |
| PR 1.4: Auth Module | ✅ Done | PR 3.3: Auth Pages | ⏳ TODO | ✅ YES | Login, Forgot Password, Reset |
| **Core Modules** |
| PR 2.3: Student Module | ✅ Done | PR 3.4: Student Management | ⏳ TODO | ✅ YES | CRUD, Search, Validation |
| PR 2.3.1: Teacher Module | ✅ Done | PR 3.5: Teacher Management | ⏳ TODO | ✅ YES | CRUD, Status management |
| PR 2.4: Course Module | ✅ Done | PR 3.6: Course Management | ⏳ TODO | ✅ YES | Lifecycle (DRAFT→PUBLISHED→ARCHIVED) |
| PR 2.5: Class Module | ⏳ TODO | PR 3.7: Class Management | ⏳ TODO | ❌ NO | Need Backend first |
| PR 2.7: Attendance | ⏳ TODO | PR 3.8: Attendance Management | ⏳ TODO | ❌ NO | Need Backend first |
| PR 2.8: Invoice | ⏳ TODO | PR 3.9: Billing (partial) | ⏳ TODO | ❌ NO | Need Backend first |
| PR 2.8.1: Payment | ⏳ TODO | PR 3.9: Billing (full) | ⏳ TODO | ❌ NO | Need Backend first |
| PR 2.9: Settings | ⏳ TODO | PR 3.10: Parent Portal | ⏳ TODO | ❌ NO | Need Backend first |
| PR 2.9: Settings | ⏳ TODO | PR 3.11: Settings & Reports | ⏳ TODO | ❌ NO | Need Backend first |

**Summary:**
- ✅ **Ready to implement NOW:** PR 3.1, 3.2, 3.3, 3.4, 3.5, 3.6 (6 PRs)
- ⏳ **Waiting for Backend:** PR 3.7, 3.8, 3.9, 3.10, 3.11 (5 PRs)

---

