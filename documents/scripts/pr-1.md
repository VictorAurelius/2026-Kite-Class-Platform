# KiteClass Implementation Prompts

Danh sách prompts để thực hiện các plans theo thứ tự.

**Nguyên tắc bắt buộc:**
1. Tuân thủ tất cả skills trong `.claude/skills/`
2. Mỗi module phải có tests đi kèm ngay trong PR đó
3. Tests nằm trong thư mục `src/test/` (BE) hoặc `src/__tests__/` (FE)
4. **Branch theo service:** feature/gateway, feature/core, feature/frontend
5. **Commit sau khi hoàn thành PR**, format ngắn gọn: `feat(service): PR X.X - description`

---

# GIAI ĐOẠN 1: KITECLASS-GATEWAY

## PR 1.1 - Gateway Project Setup

```
Thực hiện Phase 1 của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- maven-dependencies.md: versions chuẩn, PHẢI check trước khi tạo pom.xml
- architecture-overview.md: cấu trúc thư mục Backend
- code-style.md: Java naming conventions, package structure
- environment-setup.md: cấu hình local dev

**Tasks:**
1. Tạo project structure trong thư mục kiteclass/kiteclass-gateway/
2. Tạo pom.xml với dependencies theo plan
3. Tạo application.yml cho các profiles (local, docker, prod)
4. Tạo KiteclassGatewayApplication.java

**Verification:**
- mvn clean compile phải pass
- Application context loads thành công
```

## PR 1.2 - Gateway Common Components

```
Thực hiện Phase 2 của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- code-style.md: Java conventions, annotation ordering
- enums-constants.md: định nghĩa enums đúng format
- error-logging.md: exception handling patterns

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

## PR 1.3 - User Module

```
Thực hiện Phase 3 (User Module) của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- code-style.md: Entity, Repository, Service, Controller conventions
- api-design.md: User Management API endpoints
- database-design.md: users table schema
- testing-guide.md: unit test patterns

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

## PR 1.4 - Auth Module

```
Thực hiện Phase 4 (Auth Module) của kiteclass-gateway-plan.md.

**Tuân thủ skills:**
- code-style.md: Service patterns
- api-design.md: Authentication API endpoints
- testing-guide.md: testing security components

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

## PR 1.5 - Gateway Configuration

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

## PR 1.6 - Gateway Database & Docker

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

# GIAI ĐOẠN 2: KITECLASS-CORE

## PR 2.1 - Core Project Setup

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

## PR 2.2 - Core Common Components

```
Thực hiện Phase 2 của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: Java conventions, JavaDoc requirements
- enums-constants.md: tất cả enums cho Core service
- error-logging.md: exception handling, logging patterns

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

## PR 2.3 - Student Module

```
Thực hiện Student Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: Entity, Repository, Service, Controller, DTO conventions
- api-design.md: Student API endpoints
- database-design.md: students table schema
- testing-guide.md: unit test patterns, TestDataBuilder

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
```

## PR 2.4 - Course Module

```
Thực hiện Course Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Course endpoints (nếu có)
- database-design.md: courses table schema
- testing-guide.md: test patterns

**Tasks:**
1. Tạo Course entity:
   - id, name, code, description
   - totalSessions, defaultTuitionFee
   - status (CourseStatus enum)
2. Tạo CourseRepository
3. Tạo CourseMapper
4. Tạo CourseService và CourseServiceImpl
5. Tạo CourseController với CRUD endpoints

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/course/
  - service/CourseServiceTest.java
  - controller/CourseControllerTest.java
  - repository/CourseRepositoryTest.java
- src/test/java/com/kiteclass/core/testutil/
  - CourseTestDataBuilder.java

**Flyway Migration:**
- V3__create_course_tables.sql

**Verification:**
- mvn test phải pass
- Coverage >= 80%
```

## PR 2.5 - Class Module

```
Thực hiện Class Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: Entity relationships, complex queries
- api-design.md: Class API endpoints
- database-design.md: classes, class_schedules, class_sessions tables
- testing-guide.md: testing với relationships

**Tasks:**
1. Tạo ClassEntity với relationships:
   - @ManyToOne Course
   - @ManyToOne User (teacher)
   - @OneToMany ClassSchedule
2. Tạo ClassSchedule entity (dayOfWeek, startTime, endTime, room)
3. Tạo ClassSession entity (sessionDate, sessionNumber, status, topic)
4. Tạo repositories với custom queries
5. Tạo ClassMapper
6. Tạo ClassService với:
   - createClass (với schedules)
   - generateSessions (từ schedules)
   - getClassStudents
   - getClassSessions
7. Tạo ClassController

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/clazz/
  - service/ClassServiceTest.java
  - controller/ClassControllerTest.java
  - repository/ClassRepositoryTest.java
- src/test/java/com/kiteclass/core/testutil/
  - ClassTestDataBuilder.java

**Flyway Migration:**
- V4__create_class_tables.sql

**Verification:**
- mvn test phải pass
- Test session generation logic
```

## PR 2.6 - Enrollment Module

```
Thực hiện Enrollment Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: business logic patterns
- api-design.md: POST /students/{id}/enroll
- database-design.md: enrollments table
- testing-guide.md: testing business rules

**Tasks:**
1. Tạo Enrollment entity:
   - @ManyToOne Student
   - @ManyToOne ClassEntity
   - enrollmentDate, startDate, endDate
   - tuitionAmount, discountPercent, finalAmount
   - status (EnrollmentStatus)
2. Tạo EnrollmentRepository
3. Tạo EnrollmentService với business logic:
   - enrollStudent: kiểm tra class capacity, duplicate enrollment
   - calculateFinalAmount
   - updateEnrollmentStatus
4. Tạo endpoint POST /api/v1/students/{id}/enroll

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/enrollment/
  - service/EnrollmentServiceTest.java (test business rules)
  - controller/EnrollmentControllerTest.java
- Test cases:
  - Enroll thành công
  - Class đã full -> error
  - Student đã enrolled -> error
  - Calculate discount correctly

**Flyway Migration:**
- V5__create_enrollment_tables.sql

**Verification:**
- mvn test phải pass
- Business rules được enforce đúng
```

## PR 2.7 - Attendance Module

```
Thực hiện Attendance Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Attendance API endpoints
- database-design.md: attendance table
- testing-guide.md: test patterns

**Tasks:**
1. Tạo Attendance entity:
   - @ManyToOne ClassSession
   - @ManyToOne Student
   - status (AttendanceStatus)
   - checkinTime, note
   - @ManyToOne User (markedBy)
2. Tạo AttendanceRepository
3. Tạo AttendanceService:
   - markAttendance(sessionId, List<MarkAttendanceRequest>)
   - getAttendanceByClass(classId, dateFrom, dateTo)
   - getStudentAttendanceStats(studentId, classId)
4. Tạo AttendanceController:
   - POST /api/v1/classes/{classId}/attendance
   - GET /api/v1/classes/{classId}/attendance
5. Publish event "attendance.marked" tới RabbitMQ

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/attendance/
  - service/AttendanceServiceTest.java
  - controller/AttendanceControllerTest.java
- Test cases:
  - Mark attendance cho multiple students
  - Update existing attendance
  - Get attendance statistics
  - Event publishing

**Flyway Migration:**
- V6__create_attendance_tables.sql

**Verification:**
- mvn test phải pass
- RabbitMQ event được publish
```

## PR 2.8 - Invoice & Payment Module

```
Thực hiện Invoice & Payment Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: complex business logic
- api-design.md: Invoice & Payment API endpoints
- database-design.md: invoices, invoice_items, payments tables
- testing-guide.md: testing financial calculations

**Tasks:**
1. Tạo Invoice entity:
   - invoiceNo (unique, auto-generated)
   - @ManyToOne Student
   - issueDate, dueDate
   - subtotal, discountAmount, totalAmount, paidAmount, balanceDue
   - status (InvoiceStatus)
   - @OneToMany InvoiceItem
2. Tạo InvoiceItem entity
3. Tạo Payment entity:
   - @ManyToOne Invoice
   - amount, method (PaymentMethod)
   - transactionRef, paidAt
   - status (PaymentStatus)
4. Tạo InvoiceService:
   - createInvoice
   - sendInvoice (update status)
   - calculateTotals
   - updateInvoiceStatus (check if paid)
5. Tạo PaymentService:
   - recordPayment
   - Update invoice balanceDue và status
6. Tạo controllers

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/billing/
  - service/InvoiceServiceTest.java
  - service/PaymentServiceTest.java
  - controller/InvoiceControllerTest.java
  - controller/PaymentControllerTest.java
- Test cases:
  - Calculate totals correctly
  - Partial payment -> PARTIAL status
  - Full payment -> PAID status
  - Overdue detection

**Flyway Migration:**
- V7__create_billing_tables.sql

**Verification:**
- mvn test phải pass
- Financial calculations chính xác
```

## PR 2.9 - Settings & Parent Module

```
Thực hiện Settings và Parent Module của kiteclass-core-service-plan.md.

**Tuân thủ skills:**
- code-style.md: coding conventions
- api-design.md: Settings API, Parent Portal API
- database-design.md: settings tables
- theme-system.md: branding settings

**Tasks:**
1. Tạo Branding entity (settings schema):
   - logoUrl, faviconUrl, displayName, tagline
   - primaryColor, secondaryColor
   - contactEmail, contactPhone, address
   - facebookUrl, zaloUrl
2. Tạo UserPreferences entity
3. Tạo BrandingService và controller:
   - GET/PUT /api/v1/settings/branding
   - POST /api/v1/settings/branding/logo
4. Tạo UserPreferencesService:
   - GET/PATCH /api/v1/users/me/preferences
5. Tạo Parent module:
   - ParentService với getChildren, getChildAttendance, getChildGrades
   - ParentController với endpoints

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/module/settings/
  - service/BrandingServiceTest.java
  - controller/BrandingControllerTest.java
- src/test/java/com/kiteclass/core/module/parent/
  - service/ParentServiceTest.java
  - controller/ParentControllerTest.java

**Flyway Migration:**
- V8__create_settings_tables.sql

**Verification:**
- mvn test phải pass
```

## PR 2.10 - Core Docker & Final Integration

```
Hoàn thiện kiteclass-core.

**Tuân thủ skills:**
- cloud-infrastructure.md: Docker, docker-compose
- testing-guide.md: integration tests với Testcontainers
- environment-setup.md: local dev setup

**Tasks:**
1. Tạo Dockerfile (multi-stage build)
2. Update docker-compose.yml:
   - core-service
   - Kết nối với gateway, postgres, redis, rabbitmq
3. Tạo V9__seed_test_data.sql (sample data cho testing)
4. Viết integration tests cho full flow

**Tests (bắt buộc):**
- src/test/java/com/kiteclass/core/integration/
  - StudentFlowIntegrationTest.java
  - EnrollmentFlowIntegrationTest.java
  - AttendanceFlowIntegrationTest.java
  - BillingFlowIntegrationTest.java

**Verification:**
- docker-compose up phải start tất cả services
- Integration tests pass với Testcontainers
- Swagger UI hoạt động: http://localhost:8081/swagger-ui.html
- Tất cả API endpoints hoạt động đúng
```

---

# GIAI ĐOẠN 3: KITECLASS-FRONTEND

## PR 3.1 - Frontend Project Setup

```
Thực hiện Phase 1 của kiteclass-frontend-plan.md.

**Tuân thủ skills:**
- architecture-overview.md: cấu trúc thư mục Frontend
- ui-components.md: design tokens, Shadcn setup
- code-style.md: TypeScript conventions

**Tasks:**
1. Tạo Next.js project: kiteclass/kiteclass-frontend/
2. Install dependencies theo plan
3. Setup Shadcn/UI với components cần thiết
4. Cấu hình Tailwind với custom theme theo ui-components.md
5. Tạo folder structure theo plan
6. Setup ESLint, Prettier

**Verification:**
- pnpm dev phải start thành công
- pnpm lint không có errors
```

## PR 3.2 - Frontend Core Infrastructure

```
Thực hiện Phase 2 của kiteclass-frontend-plan.md.

**Tuân thủ skills:**
- code-style.md: TypeScript conventions, file naming
- api-design.md: API response format
- enums-constants.md: TypeScript enum definitions

**Tasks:**
1. Tạo API client (src/lib/api/client.ts):
   - Axios instance với interceptors
   - Auto refresh token
   - Error handling
2. Tạo API endpoints config (src/lib/api/endpoints.ts)
3. Tạo TypeScript types (src/types/):
   - api.ts (ApiResponse, PageResponse, ErrorResponse)
   - student.ts, class.ts, course.ts
   - attendance.ts, invoice.ts
   - user.ts
4. Tạo Zustand stores:
   - auth-store.ts
   - ui-store.ts

**Tests (bắt buộc):**
- src/__tests__/lib/api/
  - client.test.ts
- src/__tests__/stores/
  - auth-store.test.ts
  - ui-store.test.ts

**Verification:**
- pnpm test phải pass
- Types khớp với BE DTOs
```

## PR 3.3 - Providers & Layout

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

## PR 3.4 - Shared Components

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

## PR 3.5 - Auth Pages

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

## PR 3.6 - Dashboard & Students Module

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

## PR 3.7 - Courses & Classes Module

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

## PR 3.8 - Attendance Module

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

## PR 3.9 - Billing Module

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

## PR 3.10 - Settings & Branding

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

## PR 3.11 - E2E Tests & Polish

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

## Branch Strategy

```
main
├── feature/gateway     # Tất cả PRs 1.1 - 1.6
├── feature/core        # Tất cả PRs 2.1 - 2.10
└── feature/frontend    # Tất cả PRs 3.1 - 3.11
```

**Quy tắc:**
- Mỗi service = 1 branch duy nhất
- Commit sau khi hoàn thành mỗi PR
- Merge vào main khi hoàn thành service

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
Gateway:  1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 1.6
                                        ↓
Core:     2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 2.7 → 2.8 → 2.9 → 2.10
                                                                   ↓
Frontend: 3.1 → 3.2 → 3.3 → 3.4 → 3.5 ←──────────────────────────┘
                                  ↓
          3.6 → 3.7 → 3.8 → 3.9 → 3.10 → 3.11
```

## Tổng kết

| Giai đoạn | PRs | Có Tests |
|-----------|-----|----------|
| Gateway | 6 | 5 (từ 1.2) |
| Core | 10 | 9 (từ 2.2) |
| Frontend | 11 | 10 (từ 3.2) |
| **Tổng** | **27** | **24** |
