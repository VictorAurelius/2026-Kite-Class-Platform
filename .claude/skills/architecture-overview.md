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
│  • {subdomain}.kiteclass.vn → Specific instance             │
│  • abc.kiteclass.vn → Instance "abc"                        │
│  • xyz.kiteclass.vn → Instance "xyz"                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

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

---

## Deployment Topology

### Development
```
localhost:
├── Frontend        → :3000
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
│   └── {tenant}.kiteclass.vn → frontend-svc
│
└── ConfigMaps/Secrets:
    ├── app-config
    └── db-credentials
```

---

## Key Design Decisions

| Quyết định | Lý do |
|------------|-------|
| **Microservices cho Instance** | Flexibility, independent scaling, feature toggles |
| **Monolith cho KiteHub** | Simpler ops, sufficient scale, faster development |
| **PostgreSQL** | ACID compliance, JSON support, mature ecosystem |
| **Next.js App Router** | Server components, SEO, performance |
| **RabbitMQ vs Kafka** | Simpler setup, sufficient throughput |
| **Database-per-tenant** | Complete isolation, compliance, easy backup |

## Actions

### Xem chi tiết module
Đọc source code trong thư mục `src/module/{module_name}`.

### Thêm module mới
1. Tạo folder trong `src/module/`
2. Tạo controller, service, repository, entity, dto
3. Đăng ký trong config nếu cần
