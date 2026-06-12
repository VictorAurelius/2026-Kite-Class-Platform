# Skill: Architecture Overview

Tổng quan kiến trúc KiteClass Platform V3.1.

## Mô tả

Tài liệu mô tả kiến trúc hệ thống:
- System architecture diagram
- Service communication
- Folder structure (Backend/Frontend)
- Multi-tenancy implementation
- Deployment topology

## Trigger phrases

- "kiến trúc hệ thống"
- "architecture"
- "folder structure"
- "cấu trúc thư mục"
- "service communication"

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           KITECLASS PLATFORM V3.1                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌───────────────────────────────────────────────────────────────────────────┐ │
│  │                              KITEHUB                                       │ │
│  │                     (SaaS Management Platform)                             │ │
│  │                                                                            │ │
│  │   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │ │
│  │   │  Next.js    │  │  Spring     │  │ PostgreSQL  │  │   Redis     │     │ │
│  │   │  Frontend   │──│  Backend    │──│  (Shared)   │  │   Cache     │     │ │
│  │   │             │  │  Monolith   │  │             │  │             │     │ │
│  │   └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘     │ │
│  │         │                │                                                 │ │
│  │         │    ┌───────────┴───────────┐                                    │ │
│  │         │    │      RabbitMQ         │                                    │ │
│  │         │    │   (Event Bus)         │                                    │ │
│  │         │    └───────────────────────┘                                    │ │
│  └─────────┼────────────────────────────────────────────────────────────────┘ │
│            │                                                                    │
│            │ Provisioning API                                                   │
│            ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      KITECLASS INSTANCES                                 │   │
│  │                    (Per-Tenant Deployment)                               │   │
│  │                                                                          │   │
│  │   ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │   │                    INSTANCE: center-abc                          │   │   │
│  │   │                                                                  │   │   │
│  │   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │   │   │
│  │   │  │ Next.js  │  │ Gateway  │  │   Core   │  │Engagement│        │   │   │
│  │   │  │ Frontend │──│ + User   │──│ Service  │  │ Service  │        │   │   │
│  │   │  │          │  │ Service  │  │          │  │(Optional)│        │   │   │
│  │   │  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │   │   │
│  │   │        │              │             │             │             │   │   │
│  │   │        │              └─────────────┼─────────────┘             │   │   │
│  │   │        │                            │                           │   │   │
│  │   │        │              ┌─────────────┴─────────────┐             │   │   │
│  │   │        │              │       PostgreSQL          │             │   │   │
│  │   │        │              │    (Isolated per tenant)  │             │   │   │
│  │   │        │              └───────────────────────────┘             │   │   │
│  │   └────────┼─────────────────────────────────────────────────────────┘   │   │
│  │            │                                                              │   │
│  │   ┌────────┼─────────────────────────────────────────────────────────┐   │   │
│  │   │        │           INSTANCE: center-xyz                          │   │   │
│  │   │        │                  (same structure)                       │   │   │
│  │   └────────┼─────────────────────────────────────────────────────────┘   │   │
│  └────────────┼──────────────────────────────────────────────────────────────┘   │
│               │                                                                   │
│  ┌────────────┴───────────────────────────────────────────────────────────────┐  │
│  │                         SHARED INFRASTRUCTURE                               │  │
│  │                                                                             │  │
│  │   ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐              │  │
│  │   │CloudFlare │  │    S3     │  │Prometheus │  │    ELK    │              │  │
│  │   │   CDN     │  │  Storage  │  │ + Grafana │  │  Logging  │              │  │
│  │   └───────────┘  └───────────┘  └───────────┘  └───────────┘              │  │
│  └─────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                   │
└───────────────────────────────────────────────────────────────────────────────────┘
```

---

## Service Descriptions

### KiteHub (SaaS Platform)

| Component | Tech | Mô tả |
|-----------|------|-------|
| **Frontend** | Next.js 14 | Landing page, Admin dashboard, Customer portal |
| **Backend** | Spring Boot Monolith | Sales, Billing, Instance provisioning, AI Marketing |
| **Database** | PostgreSQL | Shared database với schema separation |
| **Cache** | Redis | Session, Rate limiting |
| **Queue** | RabbitMQ | Async tasks, Event broadcasting |

### KiteClass Instance (Per-Tenant)

| Service | Tech | Mô tả | Status |
|---------|------|-------|--------|
| **Frontend** | Next.js 14 | Teacher portal, Student portal, Parent portal | 🔄 Planning |
| **Gateway Service** | Spring Boot + Cloud Gateway | JWT Auth, User Management, API Routing, Rate limiting | ✅ **PR 1.4 Complete** |
| **Core Service** | Spring Boot | Classes, Students, Attendance, Billing | 🔄 Planning |
| **Engagement Service** | Spring Boot (Optional) | Gamification, Forum, Notifications | 📋 Future |
| **Media Service** | Node.js + FFmpeg (Optional) | Video processing, Streaming | 📋 Future |

**Gateway Service Features (PR 1.4 - Implemented 2026-01-26):**
- ✅ JWT Authentication (access + refresh tokens)
- ✅ User Management (CRUD, roles, permissions)
- ✅ Login/Logout/Refresh endpoints
- ✅ Account locking after failed attempts
- ✅ Role-Based Access Control (RBAC)
- ✅ Security context from JWT
- ✅ Gateway filter for downstream services (adds X-User-Id, X-User-Roles headers)

---

## Multi-Tenancy Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    MULTI-TENANCY MODEL                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  KiteHub: SHARED DATABASE                                   │
│  ─────────────────────────                                  │
│  • Single database, schema separation                       │
│  • Tables: sales.*, messages.*, maintaining.*               │
│  • tenant_id column trong các bảng customer-related         │
│                                                             │
│  KiteClass: DATABASE-PER-TENANT                             │
│  ─────────────────────────────                              │
│  • Mỗi instance có database riêng                           │
│  • Complete data isolation                                  │
│  • Database name: kiteclass_{tenant_id}                     │
│                                                             │
│  URL Routing:                                               │
│  ─────────────                                              │
│  • {subdomain}.kitehub.me → Specific instance             │
│  • abc.kitehub.me → Instance "abc"                        │
│  • xyz.kitehub.me → Instance "xyz"                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Cross-Service Data Relationships

### ⚠️ Critical Design: User Identity & Business Entities

**Vấn đề:**
- **Gateway Service**: Có `users` table (authentication, roles, JWT)
- **Core Service**: Có `students`, `teachers`, `parents` tables (business logic)
- **Câu hỏi**: Làm sao Student/Teacher/Parent login vào hệ thống?

### Kiến trúc Microservices yêu cầu:

```
┌────────────────────────────────────────────────────────────────────┐
│              CROSS-SERVICE DATA RELATIONSHIP                       │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  GATEWAY SERVICE (Authentication)                                  │
│  ─────────────────────────────────                                 │
│  Database: gateway_db                                              │
│                                                                    │
│  ┌──────────────────────────────────────┐                         │
│  │           users table                │                         │
│  ├──────────────────────────────────────┤                         │
│  │ id                  BIGSERIAL PK     │                         │
│  │ email               VARCHAR UNIQUE   │                         │
│  │ password_hash       VARCHAR          │                         │
│  │ name                VARCHAR          │                         │
│  │ user_type           VARCHAR(20)      │ ◄─── STUDENT/TEACHER/  │
│  │ reference_id        BIGINT           │ ◄─── PARENT/ADMIN/STAFF│
│  │ status              VARCHAR          │                         │
│  └──────────────────────────────────────┘                         │
│           │                     │                                  │
│           │                     │ reference_id links to:           │
│           │                     └────────────────────┐             │
│           │                                          │             │
│           ▼                                          ▼             │
│  ┌─────────────────┐                    ┌─────────────────────┐   │
│  │  roles table    │                    │  Core Service DB    │   │
│  │  permissions    │                    │  (Business Logic)   │   │
│  │  user_roles     │                    └─────────────────────┘   │
│  └─────────────────┘                              │               │
│                                                    │               │
│  CORE SERVICE (Business Logic)                    │               │
│  ──────────────────────────────                   │               │
│  Database: core_db                                │               │
│                                                    │               │
│  ┌─────────────────────┐  ┌─────────────────────┐│               │
│  │  students table     │  │  teachers table     ││               │
│  ├─────────────────────┤  ├─────────────────────┤│               │
│  │ id         PK       │◄─┤ id         PK       │◄┘              │
│  │ name               │  │ name               │                 │
│  │ email              │  │ email              │                 │
│  │ phone              │  │ department         │                 │
│  │ status             │  │ specialization     │                 │
│  │ date_of_birth      │  │ salary             │                 │
│  └─────────────────────┘  └─────────────────────┘                 │
│                                                                    │
│  ┌─────────────────────┐                                          │
│  │  parents table      │                                          │
│  ├─────────────────────┤                                          │
│  │ id         PK       │◄─────────────────────────────────────────┤
│  │ name               │                                          │
│  │ email              │                                          │
│  │ phone              │                                          │
│  │ relationship       │                                          │
│  └─────────────────────┘                                          │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### Giải pháp: UserType + ReferenceId Pattern

**Implementation:**

```java
// Gateway Service - User entity
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;

    private String email;
    private String passwordHash;
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;  // STUDENT, TEACHER, PARENT, ADMIN, STAFF

    @Column(name = "reference_id")
    private Long referenceId;    // ID trong Core Service
}

public enum UserType {
    ADMIN,      // Admin - không có referenceId
    STAFF,      // Nhân viên - không có referenceId
    TEACHER,    // referenceId → teachers.id trong Core
    PARENT,     // referenceId → parents.id trong Core
    STUDENT     // referenceId → students.id trong Core
}
```

**Login Flow với Profile Retrieval:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    LOGIN FLOW WITH PROFILE                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Frontend                Gateway              Core Service      │
│     │                       │                       │           │
│     │──POST /login─────────►│                       │           │
│     │  {email, password}    │                       │           │
│     │                       │──Verify credentials   │           │
│     │                       │──Load User entity     │           │
│     │                       │  (user_type, ref_id)  │           │
│     │                       │                       │           │
│     │                       │──Generate JWT tokens  │           │
│     │                       │  (include user_type)  │           │
│     │                       │                       │           │
│     │                       │──GET /students/{id}──►│           │
│     │                       │  (if user_type=STUDENT)│          │
│     │                       │◄─Student profile──────│           │
│     │                       │                       │           │
│     │◄─Login response───────│                       │           │
│     │  {                    │                       │           │
│     │    accessToken,       │                       │           │
│     │    refreshToken,      │                       │           │
│     │    user: {            │                       │           │
│     │      id, email, name, │                       │           │
│     │      userType         │                       │           │
│     │    },                 │                       │           │
│     │    profile: {         │                       │           │
│     │      studentId,       │                       │           │
│     │      dateOfBirth,     │                       │           │
│     │      status, ...      │                       │           │
│     │    }                  │                       │           │
│     │  }                    │                       │           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Tạo Student Account Flow:**

```java
// 1. Tạo User trong Gateway (authentication)
User user = User.builder()
    .email("student@example.com")
    .passwordHash(bcrypt("password"))
    .name("Nguyễn Văn An")
    .userType(UserType.STUDENT)
    .status(UserStatus.ACTIVE)
    .build();
User savedUser = userRepository.save(user);

// 2. Gọi Core Service API để tạo Student
StudentCreateRequest coreRequest = StudentCreateRequest.builder()
    .name("Nguyễn Văn An")
    .email("student@example.com")
    .phone("0912345678")
    .build();

StudentResponse student = coreServiceClient.createStudent(coreRequest);

// 3. Update User với reference_id
savedUser.setReferenceId(student.getId());
userRepository.save(savedUser);
```

### Ưu điểm của pattern này:

| Ưu điểm | Giải thích |
|---------|------------|
| ✅ **Service Independence** | Gateway và Core hoàn toàn độc lập về database |
| ✅ **Clear Separation** | Authentication logic ≠ Business logic |
| ✅ **Single Source of Truth** | User credentials chỉ trong Gateway |
| ✅ **Flexible Roles** | Admin/Staff không cần entity trong Core |
| ✅ **Profile Extensibility** | Student/Teacher/Parent có full business data trong Core |
| ✅ **Microservices Best Practice** | Tuân thủ cross-service data relationship pattern |

### Database Migration:

```sql
-- Gateway Database
ALTER TABLE users
    ADD COLUMN user_type VARCHAR(20) DEFAULT 'ADMIN',
    ADD COLUMN reference_id BIGINT NULL;

CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_reference_id ON users(reference_id);

COMMENT ON COLUMN users.user_type IS
    'Type: ADMIN, STAFF, TEACHER, PARENT, STUDENT';
COMMENT ON COLUMN users.reference_id IS
    'ID của entity tương ứng trong Core Service';
```

**Chi tiết implementation:** Xem `auth-module.md` section "🔗 Mối Quan Hệ User-Entity"

---

## Backend Folder Structure

### Spring Boot Service Structure

```
kiteclass-core-service/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/kiteclass/core/
│       │       ├── KiteclassCoreApplication.java
│       │       │
│       │       ├── config/                    # Configuration
│       │       │   ├── SecurityConfig.java
│       │       │   ├── JpaConfig.java
│       │       │   ├── CacheConfig.java
│       │       │   └── RabbitConfig.java
│       │       │
│       │       ├── common/                    # Shared components
│       │       │   ├── exception/
│       │       │   │   ├── BusinessException.java
│       │       │   │   ├── ErrorCode.java
│       │       │   │   └── GlobalExceptionHandler.java
│       │       │   ├── dto/
│       │       │   │   ├── PageResponse.java
│       │       │   │   └── ApiResponse.java
│       │       │   └── util/
│       │       │       └── DateUtils.java
│       │       │
│       │       ├── module/                    # Business modules
│       │       │   │
│       │       │   ├── student/               # Student module
│       │       │   │   ├── controller/
│       │       │   │   │   └── StudentController.java
│       │       │   │   ├── service/
│       │       │   │   │   ├── StudentService.java
│       │       │   │   │   └── StudentServiceImpl.java
│       │       │   │   ├── repository/
│       │       │   │   │   └── StudentRepository.java
│       │       │   │   ├── entity/
│       │       │   │   │   └── Student.java
│       │       │   │   └── dto/
│       │       │   │       ├── StudentDTO.java
│       │       │   │       ├── CreateStudentRequest.java
│       │       │   │       └── UpdateStudentRequest.java
│       │       │   │
│       │       │   ├── class/                 # Class module
│       │       │   │   ├── controller/
│       │       │   │   ├── service/
│       │       │   │   ├── repository/
│       │       │   │   ├── entity/
│       │       │   │   └── dto/
│       │       │   │
│       │       │   ├── attendance/            # Attendance module
│       │       │   │   └── ...
│       │       │   │
│       │       │   ├── billing/               # Billing module
│       │       │   │   └── ...
│       │       │   │
│       │       │   └── notification/          # Notification module
│       │       │       └── ...
│       │       │
│       │       └── integration/               # External integrations
│       │           ├── zalo/
│       │           │   └── ZaloNotificationService.java
│       │           └── payment/
│       │               └── VnPayService.java
│       │
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/migration/                  # Flyway migrations
│               ├── V1__init_schema.sql
│               └── V2__add_gamification.sql
│
├── pom.xml
├── Dockerfile
└── README.md
```

---

## Frontend Folder Structure

### Next.js 14 App Router Structure

```
kiteclass-frontend/
├── src/
│   ├── app/                              # App Router
│   │   ├── (auth)/                       # Auth layout group
│   │   │   ├── login/
│   │   │   │   └── page.tsx
│   │   │   ├── forgot-password/
│   │   │   │   └── page.tsx
│   │   │   └── layout.tsx
│   │   │
│   │   ├── (dashboard)/                  # Dashboard layout group
│   │   │   ├── layout.tsx                # Sidebar + Header
│   │   │   ├── page.tsx                  # Dashboard home
│   │   │   │
│   │   │   ├── students/
│   │   │   │   ├── page.tsx              # List students
│   │   │   │   ├── [id]/
│   │   │   │   │   └── page.tsx          # Student detail
│   │   │   │   └── new/
│   │   │   │       └── page.tsx          # Create student
│   │   │   │
│   │   │   ├── classes/
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/
│   │   │   │       ├── page.tsx
│   │   │   │       ├── attendance/
│   │   │   │       │   └── page.tsx
│   │   │   │       └── students/
│   │   │   │           └── page.tsx
│   │   │   │
│   │   │   ├── billing/
│   │   │   │   ├── invoices/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── payments/
│   │   │   │       └── page.tsx
│   │   │   │
│   │   │   └── settings/
│   │   │       └── page.tsx
│   │   │
│   │   ├── api/                          # API Routes (if needed)
│   │   │   └── auth/
│   │   │       └── [...nextauth]/
│   │   │           └── route.ts
│   │   │
│   │   ├── globals.css
│   │   └── layout.tsx                    # Root layout
│   │
│   ├── components/                       # Shared components
│   │   ├── ui/                           # Shadcn UI components
│   │   │   ├── button.tsx
│   │   │   ├── input.tsx
│   │   │   ├── table.tsx
│   │   │   ├── dialog.tsx
│   │   │   └── ...
│   │   │
│   │   ├── layout/                       # Layout components
│   │   │   ├── sidebar.tsx
│   │   │   ├── header.tsx
│   │   │   └── breadcrumb.tsx
│   │   │
│   │   ├── forms/                        # Form components
│   │   │   ├── student-form.tsx
│   │   │   └── class-form.tsx
│   │   │
│   │   └── shared/                       # Other shared
│   │       ├── data-table.tsx
│   │       ├── pagination.tsx
│   │       └── loading-spinner.tsx
│   │
│   ├── hooks/                            # Custom hooks
│   │   ├── use-auth.ts
│   │   ├── use-students.ts
│   │   ├── use-classes.ts
│   │   └── use-debounce.ts
│   │
│   ├── lib/                              # Utilities
│   │   ├── api-client.ts                 # Axios/Fetch wrapper
│   │   ├── utils.ts                      # Helper functions
│   │   └── validations.ts                # Zod schemas
│   │
│   ├── stores/                           # State management
│   │   ├── auth-store.ts                 # Zustand auth store
│   │   └── ui-store.ts                   # UI state (sidebar, theme)
│   │
│   └── types/                            # TypeScript types
│       ├── api.ts                        # API response types
│       ├── student.ts
│       ├── class.ts
│       └── index.ts
│
├── public/
│   └── images/
│
├── .env.local
├── .env.example
├── next.config.js
├── tailwind.config.js
├── tsconfig.json
├── package.json
└── README.md
```

---

## Service Communication

### Synchronous (REST)
```
Frontend ──HTTP──► Gateway ──HTTP──► Core Service
                      │
                      └──HTTP──► Engagement Service
```

### Asynchronous (RabbitMQ)
```
Core Service ──publish──► RabbitMQ ──consume──► Notification Service
     │                                                   │
     │                                                   ▼
     │                                            Zalo/Email/SMS
     │
     └──publish──► RabbitMQ ──consume──► Gamification Service
                                                   │
                                                   ▼
                                            Update Points/Badges
```

### Event Examples
| Event | Producer | Consumer | Mô tả |
|-------|----------|----------|-------|
| `attendance.marked` | Core | Notification, Gamification | Khi điểm danh xong |
| `invoice.created` | Core | Notification | Gửi hóa đơn cho PH |
| `payment.received` | Core | Notification | Xác nhận thanh toán |
| `student.enrolled` | Core | Gamification | Thưởng điểm đăng ký |

### Service-to-Service REST Calls

**Gateway → Core Service:**

```java
// Gateway gọi Core để lấy Student profile sau khi login
@FeignClient(name = "core-service", url = "${services.core.url}")
public interface CoreServiceClient {

    @GetMapping("/api/v1/students/{id}")
    StudentResponse getStudent(@PathVariable Long id);

    @GetMapping("/api/v1/teachers/{id}")
    TeacherResponse getTeacher(@PathVariable Long id);

    @GetMapping("/api/v1/parents/{id}")
    ParentResponse getParent(@PathVariable Long id);
}
```

**Core → Gateway Service:**

```java
// Core gọi Gateway để verify user permissions (nếu cần)
@FeignClient(name = "gateway-service", url = "${services.gateway.url}")
public interface GatewayServiceClient {

    @GetMapping("/api/v1/users/{id}")
    UserResponse getUser(@PathVariable Long id);

    @GetMapping("/api/v1/users/{id}/permissions")
    List<String> getUserPermissions(@PathVariable Long id);
}
```

### Data Contracts (DTOs)

**Shared DTOs giữa Gateway và Core:**

```java
// Không share entities, chỉ share DTOs
public record StudentResponse(
    Long id,
    String name,
    String email,
    String phone,
    LocalDate dateOfBirth,
    String status
) {}

public record UserResponse(
    Long id,
    String email,
    String name,
    String userType,
    Long referenceId,
    String status
) {}
```

**Best Practices:**
- ❌ **KHÔNG share Entity classes** giữa services
- ✅ **Chỉ share DTOs** qua REST API
- ✅ **Version APIs** để tránh breaking changes
- ✅ **Circuit Breaker** cho resilience (Resilience4j)
- ✅ **Timeout configuration** cho mọi service call

### Request Headers từ Gateway

Gateway tự động thêm headers cho downstream services:

```java
// Gateway Filter adds authentication context
X-User-Id: 123
X-User-Email: user@example.com
X-User-Roles: TEACHER,ADMIN
X-User-Type: TEACHER
X-Reference-Id: 456  // Teacher ID trong Core
X-Tenant-Id: abc     // Tenant isolation
```

**Core Service sử dụng headers:**

```java
@RestController
public class StudentController {

    @GetMapping("/students")
    public PageResponse<StudentDTO> getStudents(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Type") String userType,
        @RequestHeader("X-Reference-Id") Long referenceId
    ) {
        // Authorization logic based on userType
        if ("TEACHER".equals(userType)) {
            // Teacher chỉ xem students trong classes của mình
            return studentService.getStudentsByTeacher(referenceId);
        }
        // ...
    }
}
```

---

## Deployment Topology

### Development
```
localhost:
├── Frontend        → :4700
├── Gateway         → :8080
├── Core Service    → :8081
├── PostgreSQL      → :5432
├── Redis           → :6379
└── RabbitMQ        → :5672 (UI: :15672)
```

### Production (Kubernetes)
```
Namespace: kiteclass-{tenant}
├── Deployments:
│   ├── frontend (replicas: 2)
│   ├── gateway (replicas: 2)
│   ├── core-service (replicas: 2)
│   └── engagement-service (replicas: 1)
│
├── Services:
│   ├── frontend-svc (ClusterIP)
│   ├── gateway-svc (ClusterIP)
│   └── core-svc (ClusterIP)
│
├── Ingress:
│   └── {tenant}.kitehub.me → frontend-svc
│
└── ConfigMaps/Secrets:
    ├── app-config
    └── db-credentials
```

---

## Key Design Decisions

| Quyết định | Lý do | Tác động |
|------------|-------|----------|
| **Microservices cho Instance** | Flexibility, independent scaling, feature toggles | Gateway (Auth) ≠ Core (Business) |
| **Monolith cho KiteHub** | Simpler ops, sufficient scale, faster development | Shared DB với schema separation |
| **PostgreSQL** | ACID compliance, JSON support, mature ecosystem | Relational + JSON flexibility |
| **Next.js App Router** | Server components, SEO, performance | Server-side rendering |
| **RabbitMQ vs Kafka** | Simpler setup, sufficient throughput | Event-driven architecture |
| **Database-per-tenant** | Complete isolation, compliance, easy backup | Mỗi instance có DB riêng |
| **UserType + ReferenceId Pattern** | Cross-service data relationship, clear separation | Gateway.User links to Core.Student/Teacher/Parent |
| **No Shared Entities** | Service independence, avoid tight coupling | Chỉ share DTOs qua REST API |
| **Gateway adds X-Headers** | Downstream services nhận auth context | Core không cần query Gateway |
| **JWT in Gateway only** | Single source of authentication truth | Core không xử lý JWT |

## Actions

### Xem chi tiết module
Đọc source code trong thư mục `src/module/{module_name}`.

### Thêm module mới
1. Tạo folder trong `src/module/`
2. Tạo controller, service, repository, entity, dto
3. Đăng ký trong config nếu cần
