# KiteClass Implementation Prompts

Danh sách prompts để thực hiện các plans theo thứ tự. Mỗi prompt tương ứng với 1 PR/commit.

---

# GIAI ĐOẠN 1: KITECLASS-GATEWAY

## PR 1.1 - Gateway Project Setup

```
Thực hiện Phase 1 của kiteclass-gateway-plan.md:
- Tạo project structure trong thư mục kiteclass/kiteclass-gateway/
- Tạo pom.xml với dependencies theo plan
- Tạo application.yml cho các profiles (local, docker, prod)
- Tạo KiteclassGatewayApplication.java
- Verify project builds: mvn clean compile
```

## PR 1.2 - Gateway Common Components

```
Thực hiện Phase 2 của kiteclass-gateway-plan.md:
- Tạo common package (dto, exception, util)
- Tạo R2dbcConfig, SecurityConfig
- Tạo GlobalExceptionHandler
- Tạo các enums theo enums-constants.md
```

## PR 1.3 - User Module

```
Thực hiện Phase 3 (User Module) của kiteclass-gateway-plan.md:
- Tạo User entity với R2DBC annotations
- Tạo UserRepository (ReactiveCrudRepository)
- Tạo UserService và UserServiceImpl
- Tạo DTOs: UserResponse, CreateUserRequest, UpdateUserRequest
- Tạo UserController với endpoints theo api-design.md
```

## PR 1.4 - Auth Module

```
Thực hiện Phase 4 (Auth Module) của kiteclass-gateway-plan.md:
- Tạo JwtTokenProvider với access/refresh token
- Tạo AuthenticationManager
- Tạo AuthService: login, logout, refresh
- Tạo AuthController với endpoints
- Tạo JwtAuthenticationFilter cho Gateway
```

## PR 1.5 - Gateway Configuration

```
Thực hiện Phase 5 (Gateway Configuration) của kiteclass-gateway-plan.md:
- Cấu hình routes trong application.yml
- Tạo RateLimitingFilter với Bucket4j
- Tạo LoggingFilter
- Tạo CorsConfig
```

## PR 1.6 - Gateway Database & Docker

```
Thực hiện Phase 6 của kiteclass-gateway-plan.md:
- Tạo Flyway migrations cho users, roles, permissions, refresh_tokens
- Seed default owner account
- Tạo Dockerfile
- Tạo docker-compose.yml cho local dev
- Test: docker-compose up và verify login works
```

---

# GIAI ĐOẠN 2: KITECLASS-CORE

## PR 2.1 - Core Project Setup

```
Thực hiện Phase 1 của kiteclass-core-service-plan.md:
- Tạo project structure trong thư mục kiteclass/kiteclass-core/
- Tạo pom.xml với dependencies
- Tạo application.yml
- Tạo KiteclassCoreApplication.java
```

## PR 2.2 - Core Common Components

```
Thực hiện Phase 2 của kiteclass-core-service-plan.md:
- Tạo BaseEntity với audit fields
- Tạo common DTOs (ApiResponse, PageResponse, ErrorResponse)
- Tạo exception classes và GlobalExceptionHandler
- Tạo tất cả enums theo enums-constants.md
- Tạo JpaConfig, CacheConfig, RabbitConfig
```

## PR 2.3 - Student Module

```
Thực hiện Student Module của kiteclass-core-service-plan.md:
- Tạo Student entity
- Tạo StudentRepository với custom queries
- Tạo StudentMapper (MapStruct)
- Tạo StudentService và StudentServiceImpl
- Tạo StudentController với CRUD + search
- Viết unit tests cho StudentService
```

## PR 2.4 - Course Module

```
Thực hiện Course Module của kiteclass-core-service-plan.md:
- Tạo Course entity
- Tạo CourseRepository, CourseMapper
- Tạo CourseService và CourseController
- Viết unit tests
```

## PR 2.5 - Class Module

```
Thực hiện Class Module của kiteclass-core-service-plan.md:
- Tạo ClassEntity, ClassSchedule, ClassSession entities
- Tạo relationships với Course
- Tạo ClassRepository với queries cho sessions, schedules
- Tạo ClassService và ClassController
- Viết unit tests
```

## PR 2.6 - Enrollment Module

```
Thực hiện Enrollment Module của kiteclass-core-service-plan.md:
- Tạo Enrollment entity với relationship tới Student, Class
- Tạo EnrollmentRepository
- Tạo EnrollmentService với business logic (check class capacity, duplicate)
- Tạo endpoint POST /students/{id}/enroll
- Viết unit tests
```

## PR 2.7 - Attendance Module

```
Thực hiện Attendance Module của kiteclass-core-service-plan.md:
- Tạo Attendance entity
- Tạo AttendanceRepository
- Tạo AttendanceService với markAttendance, getAttendanceByClass
- Tạo AttendanceController
- Publish event attendance.marked tới RabbitMQ
- Viết unit tests
```

## PR 2.8 - Invoice & Payment Module

```
Thực hiện Invoice & Payment Module của kiteclass-core-service-plan.md:
- Tạo Invoice, InvoiceItem, Payment entities
- Tạo repositories và mappers
- Tạo InvoiceService với create, send, calculateBalance
- Tạo PaymentService với recordPayment
- Tạo controllers
- Viết unit tests
```

## PR 2.9 - Database Migrations

```
Tạo tất cả Flyway migrations cho kiteclass-core theo database-design.md:
- V1__init_schema.sql (create schemas)
- V2__create_student_tables.sql
- V3__create_course_class_tables.sql
- V4__create_enrollment_tables.sql
- V5__create_attendance_tables.sql
- V6__create_billing_tables.sql
- V7__create_settings_tables.sql
- V8__seed_initial_data.sql
```

## PR 2.10 - Core Docker & Integration Tests

```
Hoàn thiện kiteclass-core:
- Tạo Dockerfile
- Update docker-compose.yml để include core-service
- Viết integration tests với Testcontainers
- Verify tất cả endpoints hoạt động qua Swagger UI
```

---

# GIAI ĐOẠN 3: KITECLASS-FRONTEND

## PR 3.1 - Frontend Project Setup

```
Thực hiện Phase 1 của kiteclass-frontend-plan.md:
- Tạo Next.js project trong thư mục kiteclass/kiteclass-frontend/
- Install dependencies theo plan
- Setup Shadcn/UI với các components cần thiết
- Cấu hình Tailwind với custom theme
- Tạo folder structure theo plan
```

## PR 3.2 - Frontend Core Infrastructure

```
Thực hiện Phase 2 của kiteclass-frontend-plan.md:
- Tạo API client với axios interceptors
- Tạo API endpoints configuration
- Tạo TypeScript types cho tất cả entities (khớp với BE DTOs)
- Tạo Zustand stores (auth-store, ui-store)
```

## PR 3.3 - Providers & Layout

```
Thực hiện Phase 3-5 của kiteclass-frontend-plan.md:
- Tạo QueryProvider, ThemeProvider, AuthProvider
- Tạo root layout với providers
- Tạo Sidebar component với navigation config
- Tạo Header với UserNav và ThemeToggle
- Tạo Dashboard layout
```

## PR 3.4 - Shared Components

```
Tạo shared components theo kiteclass-frontend-plan.md:
- PageHeader
- LoadingSpinner
- StatusBadge
- EmptyState
- StatsCard
- ConfirmDialog
- DataTable với pagination
```

## PR 3.5 - Auth Pages

```
Thực hiện Auth pages:
- Tạo useAuth hook (login, logout)
- Tạo Auth layout
- Tạo Login page với form validation
- Tạo Forgot Password page
- Test login flow với Gateway API
```

## PR 3.6 - Dashboard & Students Module

```
Thực hiện Dashboard và Students module:
- Tạo Dashboard page với stats cards
- Tạo useStudents hook
- Tạo Students list page với search, filter, pagination
- Tạo Student detail page
- Tạo StudentForm component
- Tạo Create/Edit student pages
```

## PR 3.7 - Classes Module

```
Thực hiện Classes module:
- Tạo useClasses, useClassSessions hooks
- Tạo Classes list page
- Tạo Class detail page với tabs (Info, Students, Sessions)
- Tạo ClassForm component
```

## PR 3.8 - Attendance Module

```
Thực hiện Attendance module:
- Tạo useAttendance hook
- Tạo Attendance overview page
- Tạo Attendance marking page cho class/session
- Tạo AttendanceForm với student list và status selection
```

## PR 3.9 - Billing Module

```
Thực hiện Billing module:
- Tạo useInvoices, usePayments hooks
- Tạo Invoices list page
- Tạo Invoice detail page
- Tạo Payment recording form
```

## PR 3.10 - Settings & Branding

```
Thực hiện Settings module:
- Tạo useBranding hook
- Tạo Settings page layout
- Tạo Branding settings page (logo upload, colors)
- Tạo Profile settings page
- Integrate theme với branding API
```

## PR 3.11 - Frontend Testing

```
Viết tests cho Frontend:
- Setup Vitest và Testing Library
- Viết component tests cho shared components
- Viết hook tests với MSW
- Setup Playwright cho E2E tests
- Viết E2E test cho login flow và CRUD operations
```

---

# HƯỚNG DẪN SỬ DỤNG

## Workflow

1. Copy prompt vào Claude
2. Claude thực hiện code
3. Review và test
4. Commit với message: `feat: [PR name]`
5. Tiếp tục prompt tiếp theo

## Tips

- **Thêm context nếu cần**: "theo code-style.md", "dùng Vietnamese cho messages"
- **Debug**: Paste error message và hỏi Claude fix
- **Skip tests tạm thời**: Thêm "skip tests" nếu muốn code nhanh trước
- **Chỉ định file**: "chỉ tạo file X, Y, Z" nếu muốn giới hạn scope

## Dependencies giữa các PR

```
Gateway:  1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 1.6
                              ↓
Core:     2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 2.7 → 2.8 → 2.9 → 2.10
                              ↓
Frontend: 3.1 → 3.2 → 3.3 → 3.4 → 3.5 → 3.6 → 3.7 → 3.8 → 3.9 → 3.10 → 3.11
```

**Lưu ý**:
- PR 3.5 (Auth Pages) cần Gateway đã chạy được (PR 1.6)
- PR 3.6+ cần Core Service đã có API (PR 2.3+)

## Estimated PRs

| Giai đoạn | Số PRs | Ưu tiên |
|-----------|--------|---------|
| Gateway | 6 | P0 - Làm đầu tiên |
| Core | 10 | P0 - Làm sau Gateway |
| Frontend | 11 | P0 - Làm sau có API |
| **Tổng** | **27** | |

---

# QUICK START

Bắt đầu với PR 1.1:

```
Thực hiện Phase 1 của kiteclass-gateway-plan.md:
- Tạo project structure trong thư mục kiteclass/kiteclass-gateway/
- Tạo pom.xml với dependencies theo plan
- Tạo application.yml cho các profiles (local, docker, prod)
- Tạo KiteclassGatewayApplication.java
- Verify project builds: mvn clean compile
```
