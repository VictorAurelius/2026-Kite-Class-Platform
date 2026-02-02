# THIẾT KẾ DATABASE
## KiteClass Platform V3.1

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Dự án** | KiteClass Platform V3.1 |
| **Loại tài liệu** | Database Design Document |
| **Ngày tạo** | 23/12/2025 |
| **DBMS** | PostgreSQL 15+ |
| **Tham chiếu** | system-architecture-v3-final.md |

---

# MỤC LỤC

1. [Tổng quan Database Architecture](#1-tổng-quan-database-architecture)
2. [KiteHub Database](#2-kitehub-database)
3. [KiteClass Instance Database](#3-kiteclass-instance-database)
4. [Entity Relationship Diagrams](#4-entity-relationship-diagrams)
5. [Indexes & Performance](#5-indexes--performance)
6. [Data Migration & Seeding](#6-data-migration--seeding)

---

# 1. TỔNG QUAN DATABASE ARCHITECTURE

## 1.1. Database Strategy

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                       DATABASE ARCHITECTURE V3.1                                  │
│                         (Microservices Model)                                     │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                        KITEHUB DATABASE                                     │ │
│  │                     (Single shared database)                                │ │
│  │                                                                             │ │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       │ │
│  │  │    SALES     │ │   MESSAGES   │ │ MAINTAINING  │ │  AI_AGENTS   │       │ │
│  │  │    Schema    │ │    Schema    │ │    Schema    │ │    Schema    │       │ │
│  │  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       │ │
│  │                                                                             │ │
│  │  Customers, Orders, Subscriptions, Chat, Instances, AI Sessions            │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                   │
│                                    │                                              │
│                                    │ Provisioning                                 │
│                                    ▼                                              │
│                                                                                   │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐   │
│  │ KITECLASS INSTANCE 1 │  │ KITECLASS INSTANCE 2 │  │ KITECLASS INSTANCE N │   │
│  │    (Tenant: ABC)     │  │    (Tenant: XYZ)     │  │    (Tenant: ...)     │   │
│  │                      │  │                      │  │                      │   │
│  │  ┌────────────────┐  │  │  ┌────────────────┐  │  │  ┌────────────────┐   │   │
│  │  │  GATEWAY DB    │  │  │  │  GATEWAY DB    │  │  │  │  GATEWAY DB    │   │   │
│  │  │ (PostgreSQL)   │  │  │  │ (PostgreSQL)   │  │  │  │ (PostgreSQL)   │   │   │
│  │  ├────────────────┤  │  │  ├────────────────┤  │  │  ├────────────────┤   │   │
│  │  │ • users        │  │  │  │ • users        │  │  │  │ • users        │   │   │
│  │  │ • roles        │  │  │  │ • roles        │  │  │  │ • roles        │   │   │
│  │  │ • permissions  │  │  │  │ • permissions  │  │  │  │ • permissions  │   │   │
│  │  │ • user_roles   │  │  │  │ • user_roles   │  │  │  │ • user_roles   │   │   │
│  │  │ • refresh_...  │  │  │  │ • refresh_...  │  │  │  │ • refresh_...  │   │   │
│  │  └────────────────┘  │  │  └────────────────┘  │  │  └────────────────┘   │   │
│  │         ↕             │  │         ↕             │  │         ↕             │   │
│  │  reference_id links  │  │  reference_id links  │  │  reference_id links  │   │
│  │         ↕             │  │         ↕             │  │         ↕             │   │
│  │  ┌────────────────┐  │  │  ┌────────────────┐  │  │  ┌────────────────┐   │   │
│  │  │    CORE DB     │  │  │  │    CORE DB     │  │  │  │    CORE DB     │   │   │
│  │  │ (PostgreSQL)   │  │  │  │ (PostgreSQL)   │  │  │  │ (PostgreSQL)   │   │   │
│  │  ├────────────────┤  │  │  ├────────────────┤  │  │  ├────────────────┤   │   │
│  │  │ • students     │  │  │  │ • students     │  │  │  │ • students     │   │   │
│  │  │ • teachers     │  │  │  │ • teachers     │  │  │  │ • teachers     │   │   │
│  │  │ • parents      │  │  │  │ • parents      │  │  │  │ • parents      │   │   │
│  │  │ • classes      │  │  │  │ • classes      │  │  │  │ • classes      │   │   │
│  │  │ • attendance   │  │  │  │ • attendance   │  │  │  │ • attendance   │   │   │
│  │  │ • invoices     │  │  │  │ • invoices     │  │  │  │ • invoices     │   │   │
│  │  │ • gamification │  │  │  │ • gamification │  │  │  │ • gamification │   │   │
│  │  └────────────────┘  │  │  └────────────────┘  │  │  └────────────────┘   │   │
│  └──────────────────────┘  └──────────────────────┘  └──────────────────────┘   │
│                                                                                   │
│  STRATEGY: Database-per-tenant + Microservices                                    │
│  - Gateway DB: Authentication, Authorization (JWT, Roles, Users)                  │
│  - Core DB: Business Logic (Students, Classes, Billing, etc.)                     │
│  - Cross-DB Relationship: Gateway.users.reference_id → Core.[students/teachers]   │
│  BENEFITS: Security, Service Independence, Clear Separation of Concerns           │
│                                                                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

## 1.2. Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| **Tables** | snake_case, plural | `students`, `class_schedules` |
| **Columns** | snake_case | `first_name`, `created_at` |
| **Primary Keys** | `id` (BIGSERIAL) | `id` |
| **Foreign Keys** | `{table}_id` | `student_id`, `class_id` |
| **Indexes** | `idx_{table}_{columns}` | `idx_students_email` |
| **Unique Constraints** | `uk_{table}_{columns}` | `uk_students_email` |
| **Check Constraints** | `chk_{table}_{description}` | `chk_invoices_amount_positive` |

## 1.3. Common Columns

```sql
-- Audit columns (all tables)
created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
created_by BIGINT,     -- User ID from Gateway (NO FK constraint in Core DB)
updated_by BIGINT,     -- User ID from Gateway (NO FK constraint in Core DB)

-- Soft delete
deleted BOOLEAN DEFAULT FALSE NOT NULL,
deleted_at TIMESTAMP WITH TIME ZONE,

-- Version for optimistic locking
version INTEGER DEFAULT 0 NOT NULL
```

**⚠️ Quan trọng về Audit Fields trong Microservices:**

- **Gateway DB tables:** `created_by/updated_by` CÓ THỂ reference `users(id)` (cùng DB)
- **Core DB tables:** `created_by/updated_by` KHÔNG THỂ có FK constraint (khác DB)
  - Lưu user_id từ Gateway dưới dạng BIGINT
  - Validate tại application layer, không phải DB layer
  - Nếu cần thông tin user, call Gateway Service API

## 1.4. Microservices Database Strategy

### KiteClass Instance Architecture

Mỗi KiteClass instance (tenant) sử dụng **2 databases riêng biệt** theo kiến trúc microservices:

```
┌─────────────────────────────────────────────────────────────────┐
│                  KITECLASS INSTANCE (Tenant: ABC)               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              GATEWAY SERVICE                              │ │
│  │                                                           │ │
│  │  Database: kiteclass_abc_gateway                          │ │
│  │  ───────────────────────────────────                      │ │
│  │  Trách nhiệm: Authentication & Authorization              │ │
│  │                                                           │ │
│  │  Tables:                                                  │ │
│  │  • users            (credentials, user_type, ref_id)      │ │
│  │  • roles            (OWNER, ADMIN, TEACHER, etc.)         │ │
│  │  • permissions      (granular permissions)                │ │
│  │  • user_roles       (many-to-many)                        │ │
│  │  • refresh_tokens   (JWT refresh token storage)           │ │
│  │  • password_reset_tokens                                  │ │
│  └───────────────────────────────────────────────────────────┘ │
│                            ↕                                   │
│                   reference_id links to                        │
│                            ↕                                   │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              CORE SERVICE                                 │ │
│  │                                                           │ │
│  │  Database: kiteclass_abc_core                             │ │
│  │  ───────────────────────────                              │ │
│  │  Trách nhiệm: Business Logic                              │ │
│  │                                                           │ │
│  │  Tables:                                                  │ │
│  │  • students         (student profiles)                    │ │
│  │  • teachers         (teacher profiles)                    │ │
│  │  • parents          (parent profiles)                     │ │
│  │  • classes          (class management)                    │ │
│  │  • enrollments      (student-class relationship)          │ │
│  │  • attendance       (attendance tracking)                 │ │
│  │  • invoices         (billing)                             │ │
│  │  • payments         (payment records)                     │ │
│  │  • gamification tables                                    │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Cross-Database Relationship Pattern

**Vấn đề:** Gateway và Core ở 2 databases khác nhau, làm sao liên kết User với Student/Teacher/Parent?

**Giải pháp:** UserType + ReferenceId Pattern

#### Gateway Database - users table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,

    -- Cross-service linking fields
    user_type VARCHAR(20) NOT NULL,     -- ADMIN, STAFF, TEACHER, PARENT, STUDENT
    reference_id BIGINT,                -- ID trong Core database

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_users_user_type CHECK (
        user_type IN ('ADMIN', 'STAFF', 'TEACHER', 'PARENT', 'STUDENT')
    )
);

CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_reference_id ON users(reference_id);
```

#### Core Database - students/teachers/parents tables

```sql
-- Students table (Core DB)
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    date_of_birth DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- NO userId field - linked via Gateway.users.reference_id
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Teachers table (Core DB)
CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    department VARCHAR(100),
    specialization VARCHAR(100),
    -- NO userId field - linked via Gateway.users.reference_id
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Parents table (Core DB)
CREATE TABLE parents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    relationship VARCHAR(50),
    -- NO userId field - linked via Gateway.users.reference_id
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### Mapping Logic

| user_type | reference_id links to | Ý nghĩa |
|-----------|----------------------|---------|
| `ADMIN` | `NULL` | Admin không có entity trong Core |
| `STAFF` | `NULL` | Staff không có entity trong Core |
| `TEACHER` | `teachers.id` | Teacher profile trong Core |
| `PARENT` | `parents.id` | Parent profile trong Core |
| `STUDENT` | `students.id` | Student profile trong Core |

### Ví dụ: Student Login Flow

```sql
-- 1. Gateway authenticates user
SELECT * FROM gateway_db.users
WHERE email = 'student@example.com' AND deleted = FALSE;
-- Result: id=123, user_type='STUDENT', reference_id=456

-- 2. Gateway calls Core Service API to get profile
-- Core Service queries:
SELECT * FROM core_db.students WHERE id = 456;
-- Result: Student profile (name, date_of_birth, status, etc.)

-- 3. Gateway returns combined response:
{
  "user": {
    "id": 123,
    "email": "student@example.com",
    "userType": "STUDENT"
  },
  "profile": {
    "studentId": 456,
    "name": "Nguyễn Văn An",
    "dateOfBirth": "2010-05-15",
    "status": "ACTIVE"
  }
}
```

### Ưu điểm của kiến trúc này

| Ưu điểm | Giải thích |
|---------|------------|
| ✅ **Service Independence** | Gateway và Core hoàn toàn độc lập về database |
| ✅ **Clear Separation** | Authentication logic ≠ Business logic |
| ✅ **Single Source of Truth** | Credentials chỉ trong Gateway, business data chỉ trong Core |
| ✅ **Scalability** | Scale Gateway và Core service độc lập |
| ✅ **Security** | JWT generation/validation chỉ trong Gateway |
| ✅ **Flexibility** | Admin/Staff không cần entity trong Core |

### Nhược điểm và giải pháp

| Nhược điểm | Giải pháp |
|------------|-----------|
| ⚠️ **No Foreign Key Constraints** | Validate tại application layer + API contracts |
| ⚠️ **Two Database Queries** | Cache profile data trong Gateway (Redis) |
| ⚠️ **Data Consistency** | Transaction log + eventual consistency patterns |
| ⚠️ **Complex Queries** | Denormalize if needed, use API Gateway aggregation |

### Naming Convention cho Databases

```
KiteHub:
  kitehub_production

KiteClass Instances:
  Tenant: abc → kiteclass_abc_gateway + kiteclass_abc_core
  Tenant: xyz → kiteclass_xyz_gateway + kiteclass_xyz_core
```

---

# 2. KITEHUB DATABASE

## 2.1. Schema Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         KITEHUB DATABASE SCHEMA                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  sales.*                    messages.*               maintaining.*              │
│  ─────────                  ───────────              ──────────────              │
│  • customers                • chat_sessions          • instances                 │
│  • orders                   • chat_messages          • instance_configs          │
│  • subscriptions            • notifications          • provisioning_logs         │
│  • payments                 • notification_logs      • health_checks             │
│  • invoices                                                                      │
│  • pricing_plans                                                                 │
│                                                                                  │
│  ai_agents.*                auth.*                                               │
│  ───────────                ──────                                               │
│  • ai_sessions              • admin_users                                        │
│  • marketing_assets         • admin_roles                                        │
│  • generated_content        • admin_permissions                                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 2.2. Tables Definition

### 2.2.1. sales.customers

```sql
CREATE TABLE sales.customers (
    id BIGSERIAL PRIMARY KEY,

    -- Basic info
    organization_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),

    -- Business info
    industry VARCHAR(100),
    company_size VARCHAR(50), -- small, medium, large

    -- Address
    address TEXT,
    city VARCHAR(100),
    province VARCHAR(100),

    -- Status
    status VARCHAR(50) DEFAULT 'active' NOT NULL,
    -- active, suspended, churned

    -- Marketing
    logo_url TEXT,
    slogan TEXT,
    referral_source VARCHAR(100),

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT chk_customers_status CHECK (status IN ('active', 'suspended', 'churned'))
);

CREATE INDEX idx_customers_email ON sales.customers(email);
CREATE INDEX idx_customers_status ON sales.customers(status);
```

### 2.2.2. sales.pricing_plans

```sql
CREATE TABLE sales.pricing_plans (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE, -- BASIC, STANDARD, PREMIUM
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Pricing
    monthly_price DECIMAL(12, 2) NOT NULL,
    yearly_price DECIMAL(12, 2), -- discount for yearly

    -- Limits
    max_students INTEGER,
    max_teachers INTEGER,
    max_classes INTEGER,
    storage_gb INTEGER DEFAULT 10,

    -- Features
    features JSONB DEFAULT '{}',
    -- {"engagement": true, "media": false, "forum": true}

    -- Services included
    includes_engagement BOOLEAN DEFAULT FALSE,
    includes_media BOOLEAN DEFAULT FALSE,

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Seed data
INSERT INTO sales.pricing_plans (code, name, monthly_price, yearly_price, max_students, includes_engagement, includes_media) VALUES
('BASIC', 'Gói Cơ Bản', 500000, 5000000, 50, FALSE, FALSE),
('STANDARD', 'Gói Tiêu Chuẩn', 1000000, 10000000, 200, TRUE, FALSE),
('PREMIUM', 'Gói Cao Cấp', 2000000, 20000000, NULL, TRUE, FALSE);
```

### 2.2.3. sales.subscriptions

```sql
CREATE TABLE sales.subscriptions (
    id BIGSERIAL PRIMARY KEY,

    customer_id BIGINT NOT NULL REFERENCES sales.customers(id),
    plan_id BIGINT NOT NULL REFERENCES sales.pricing_plans(id),

    -- Subdomain
    subdomain VARCHAR(50) NOT NULL UNIQUE,
    -- abc.kiteclass.com

    -- Billing cycle
    billing_cycle VARCHAR(20) DEFAULT 'monthly' NOT NULL,
    -- monthly, yearly

    -- Dates
    start_date DATE NOT NULL,
    end_date DATE,
    next_billing_date DATE,

    -- Add-ons
    addons JSONB DEFAULT '[]',
    -- [{"code": "ENGAGEMENT_PACK", "price": 300000}, {"code": "MEDIA_PACK", "price": 500000}]

    -- Status
    status VARCHAR(50) DEFAULT 'active' NOT NULL,
    -- pending, active, suspended, cancelled, expired

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_subscriptions_status CHECK (
        status IN ('pending', 'active', 'suspended', 'cancelled', 'expired')
    ),
    CONSTRAINT chk_subscriptions_billing_cycle CHECK (
        billing_cycle IN ('monthly', 'yearly')
    )
);

CREATE INDEX idx_subscriptions_customer ON sales.subscriptions(customer_id);
CREATE INDEX idx_subscriptions_status ON sales.subscriptions(status);
CREATE UNIQUE INDEX idx_subscriptions_subdomain ON sales.subscriptions(subdomain);
```

### 2.2.4. sales.orders

```sql
CREATE TABLE sales.orders (
    id BIGSERIAL PRIMARY KEY,

    order_number VARCHAR(50) NOT NULL UNIQUE,
    -- ORD-2025-0001

    customer_id BIGINT NOT NULL REFERENCES sales.customers(id),
    subscription_id BIGINT REFERENCES sales.subscriptions(id),

    -- Order details
    plan_id BIGINT NOT NULL REFERENCES sales.pricing_plans(id),
    billing_cycle VARCHAR(20) NOT NULL,

    -- Pricing
    subtotal DECIMAL(12, 2) NOT NULL,
    discount DECIMAL(12, 2) DEFAULT 0,
    tax DECIMAL(12, 2) DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL,

    -- Add-ons
    addons JSONB DEFAULT '[]',

    -- Status
    status VARCHAR(50) DEFAULT 'pending' NOT NULL,
    -- pending, paid, provisioning, completed, cancelled, refunded

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_orders_status CHECK (
        status IN ('pending', 'paid', 'provisioning', 'completed', 'cancelled', 'refunded')
    )
);

CREATE INDEX idx_orders_customer ON sales.orders(customer_id);
CREATE INDEX idx_orders_status ON sales.orders(status);
CREATE INDEX idx_orders_created ON sales.orders(created_at DESC);
```

### 2.2.5. sales.payments

```sql
CREATE TABLE sales.payments (
    id BIGSERIAL PRIMARY KEY,

    payment_number VARCHAR(50) NOT NULL UNIQUE,
    -- PAY-2025-0001

    order_id BIGINT NOT NULL REFERENCES sales.orders(id),

    -- Amount
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',

    -- Payment method
    payment_method VARCHAR(50) NOT NULL,
    -- bank_transfer, momo, zalopay, credit_card

    -- Transaction info
    transaction_id VARCHAR(100),
    payment_gateway VARCHAR(50),

    -- Status
    status VARCHAR(50) DEFAULT 'pending' NOT NULL,
    -- pending, processing, completed, failed, refunded

    -- Timestamps
    paid_at TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_payments_status CHECK (
        status IN ('pending', 'processing', 'completed', 'failed', 'refunded')
    )
);

CREATE INDEX idx_payments_order ON sales.payments(order_id);
CREATE INDEX idx_payments_status ON sales.payments(status);
```

### 2.2.6. maintaining.instances

```sql
CREATE TABLE maintaining.instances (
    id BIGSERIAL PRIMARY KEY,

    subscription_id BIGINT NOT NULL REFERENCES sales.subscriptions(id),

    -- Identification
    instance_code VARCHAR(50) NOT NULL UNIQUE,
    -- INST-ABC-001
    subdomain VARCHAR(50) NOT NULL UNIQUE,

    -- Configuration
    config JSONB DEFAULT '{}',
    -- {"services": ["user-gateway", "core", "engagement"], "resources": {...}}

    -- Infrastructure
    cluster VARCHAR(100),
    namespace VARCHAR(100),

    -- Status
    status VARCHAR(50) DEFAULT 'provisioning' NOT NULL,
    -- provisioning, running, stopped, failed, terminated

    -- Health
    last_health_check TIMESTAMP WITH TIME ZONE,
    health_status VARCHAR(50) DEFAULT 'unknown',
    -- healthy, unhealthy, unknown

    -- Timestamps
    provisioned_at TIMESTAMP WITH TIME ZONE,
    terminated_at TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_instances_status CHECK (
        status IN ('provisioning', 'running', 'stopped', 'failed', 'terminated')
    )
);

CREATE INDEX idx_instances_subscription ON maintaining.instances(subscription_id);
CREATE INDEX idx_instances_status ON maintaining.instances(status);
```

---

# 3. KITECLASS INSTANCE DATABASE

## 3.1. Schema Overview (Microservices Architecture)

### Gateway Database (Authentication & Authorization)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    GATEWAY DATABASE SCHEMA                                       │
│                kiteclass_{tenant}_gateway                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  AUTH_MODULE (Gateway Service)                                                   │
│  ──────────────────────────────                                                  │
│  • users                    (credentials, user_type, reference_id)               │
│  • roles                    (OWNER, ADMIN, TEACHER, PARENT, STAFF, STUDENT)      │
│  • permissions              (granular permissions per module)                    │
│  • user_roles               (many-to-many: users ↔ roles)                        │
│  • refresh_tokens           (JWT refresh token storage)                          │
│  • password_reset_tokens    (password reset flow)                                │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Core Database (Business Logic)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                       CORE DATABASE SCHEMA                                       │
│                    kiteclass_{tenant}_core                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  STUDENT_MODULE            TEACHER_MODULE            PARENT_MODULE              │
│  ───────────────           ──────────────            ──────────────              │
│  • students                • teachers                • parents                   │
│                            • teacher_assignments     • parent_children           │
│                                                      • parent_notifications      │
│                                                                                  │
│  CLASS_MODULE              LEARNING_MODULE           BILLING_MODULE             │
│  ─────────────             ───────────────           ──────────────              │
│  • courses                 • attendance               • tuition_configs          │
│  • classes                 • grades                   • invoices                 │
│  • class_schedules         • assignments              • invoice_items            │
│  • class_sessions          • submissions              • payments                 │
│  • enrollments             • learning_materials       • payment_reminders        │
│  • rooms                                                                         │
│                                                                                  │
│  GAMIFICATION_MODULE       FORUM_MODULE              NOTIFICATION_MODULE        │
│  ────────────────────      ────────────              ───────────────────         │
│  • point_rules             • forum_topics            • notification_templates    │
│  • student_points          • forum_posts             • notification_logs         │
│  • badges                  • forum_comments                                      │
│  • student_badges                                    MEDIA_MODULE                │
│  • rewards                                           ────────────                │
│  • reward_redemptions                                • videos                    │
│                                                      • video_views               │
│                                                      • live_sessions             │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**⚠️ Quan trọng:**
- **Gateway DB** chứa authentication data (users, roles, JWT tokens)
- **Core DB** chứa business logic data (students, teachers, classes, billing)
- **NO direct FK** giữa 2 databases
- **Link via:** Gateway.users.reference_id → Core.students/teachers/parents.id
- **Communication:** REST API calls giữa Gateway Service và Core Service

---

## 3.2. Gateway Database Tables

**Database:** `kiteclass_{tenant}_gateway`
**Service:** Gateway Service (Authentication & Authorization)

### 3.2.1. users

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    -- Authentication
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),

    -- Profile
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url TEXT,
    date_of_birth DATE,
    gender VARCHAR(10),

    -- Address
    address TEXT,
    ward VARCHAR(100),
    district VARCHAR(100),
    city VARCHAR(100),

    -- OAuth
    oauth_provider VARCHAR(50),
    oauth_id VARCHAR(255),

    -- Status
    status VARCHAR(50) DEFAULT 'active' NOT NULL,
    -- active, inactive, suspended
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,

    -- Cross-service linking (Microservices pattern)
    user_type VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    -- ADMIN, STAFF, TEACHER, PARENT, STUDENT
    reference_id BIGINT,
    -- ID của entity tương ứng trong Core DB (students/teachers/parents)

    -- Security
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('active', 'inactive', 'suspended'))
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted = FALSE;
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_id);
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_reference_id ON users(reference_id);

-- Comments
COMMENT ON COLUMN users.user_type IS 'User type: ADMIN, STAFF, TEACHER, PARENT, STUDENT';
COMMENT ON COLUMN users.reference_id IS 'ID của entity tương ứng trong Core DB (students.id / teachers.id / parents.id)';
```

### 3.2.2. roles

```sql
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,
    -- CENTER_OWNER, CENTER_ADMIN, TEACHER, STUDENT, PARENT

    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Hierarchy (for inheritance)
    parent_role_id BIGINT REFERENCES roles(id),

    -- System role (cannot be deleted)
    is_system BOOLEAN DEFAULT FALSE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Seed data
INSERT INTO roles (code, name, is_system) VALUES
('CENTER_OWNER', 'Chủ trung tâm', TRUE),
('CENTER_ADMIN', 'Quản trị viên', TRUE),
('TEACHER', 'Giáo viên', TRUE),
('STUDENT', 'Học viên', TRUE),
('PARENT', 'Phụ huynh', TRUE);
```

### 3.2.3. permissions

```sql
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(100) NOT NULL UNIQUE,
    -- users:read, users:write, classes:manage, billing:view

    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Grouping
    module VARCHAR(50) NOT NULL,
    -- user, class, learning, billing, gamification, forum, media

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Seed data
INSERT INTO permissions (code, name, module) VALUES
('users:read', 'Xem danh sách người dùng', 'user'),
('users:write', 'Thêm/sửa người dùng', 'user'),
('users:delete', 'Xóa người dùng', 'user'),
('classes:read', 'Xem lớp học', 'class'),
('classes:manage', 'Quản lý lớp học', 'class'),
('attendance:mark', 'Điểm danh', 'learning'),
('grades:manage', 'Quản lý điểm', 'learning'),
('billing:view', 'Xem hóa đơn', 'billing'),
('billing:manage', 'Quản lý hóa đơn', 'billing');
```

### 3.2.4. user_roles

```sql
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),

    CONSTRAINT uk_user_roles UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
```

## 3.3. Core Database Tables

**Database:** `kiteclass_{tenant}_core`
**Service:** Core Service (Business Logic)

**⚠️ Quan trọng:**
- Core DB KHÔNG có trực tiếp foreign key đến Gateway DB
- Students, Teachers, Parents là business entities riêng
- Link với Gateway qua REST API calls (không phải FK)

### 3.3.1. students

```sql
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,

    -- Profile
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),

    -- Address
    address TEXT,

    -- Avatar
    avatar_url VARCHAR(500),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- PENDING, ACTIVE, INACTIVE, GRADUATED, DROPPED

    -- Notes
    note TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_students_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'GRADUATED', 'DROPPED')
    )
);

CREATE INDEX idx_students_email ON students(email) WHERE deleted = FALSE;
CREATE INDEX idx_students_phone ON students(phone);
CREATE INDEX idx_students_status ON students(status) WHERE deleted = FALSE;

-- NO userId field - linked via Gateway.users.reference_id
```

### 3.3.2. teachers

```sql
CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,

    -- Profile
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),

    -- Professional info
    department VARCHAR(100),
    specialization VARCHAR(100),
    qualifications TEXT,
    bio TEXT,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_teachers_email ON teachers(email) WHERE deleted = FALSE;
CREATE INDEX idx_teachers_department ON teachers(department);

-- NO userId field - linked via Gateway.users.reference_id
```

### 3.3.3. parents

```sql
CREATE TABLE parents (
    id BIGSERIAL PRIMARY KEY,

    -- Profile
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),

    -- Relationship
    relationship VARCHAR(50),
    -- father, mother, guardian

    -- Address
    address TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_parents_email ON parents(email) WHERE deleted = FALSE;
CREATE INDEX idx_parents_phone ON parents(phone);

-- NO userId field - linked via Gateway.users.reference_id
```

### 3.3.4. parent_children

```sql
CREATE TABLE parent_children (
    id BIGSERIAL PRIMARY KEY,

    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,

    relationship VARCHAR(50) NOT NULL,
    is_primary_contact BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_parent_children UNIQUE (parent_id, student_id)
);

CREATE INDEX idx_parent_children_parent ON parent_children(parent_id);
CREATE INDEX idx_parent_children_student ON parent_children(student_id);
```

---

## 3.4. Core Database - Class Module Tables

### 3.4.1. courses

```sql
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Category
    category VARCHAR(100),
    -- math, english, physics, etc.

    -- Media
    thumbnail_url TEXT,

    -- Pricing
    suggested_tuition DECIMAL(12, 2),

    -- Settings
    default_sessions INTEGER, -- Số buổi mặc định

    -- Status
    status VARCHAR(50) DEFAULT 'active',

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_courses_category ON courses(category);
CREATE INDEX idx_courses_status ON courses(status) WHERE deleted = FALSE;
```

### 3.3.2. classes

```sql
CREATE TABLE classes (
    id BIGSERIAL PRIMARY KEY,

    course_id BIGINT NOT NULL REFERENCES courses(id),

    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,

    -- Teacher (Core DB FK)
    teacher_id BIGINT REFERENCES teachers(id),

    -- Schedule
    start_date DATE NOT NULL,
    end_date DATE,

    -- Room
    room_id BIGINT REFERENCES rooms(id),

    -- Capacity
    max_students INTEGER DEFAULT 30,

    -- Tuition
    tuition_amount DECIMAL(12, 2) NOT NULL,
    tuition_type VARCHAR(20) DEFAULT 'fixed',
    -- fixed, per_session

    -- Status
    status VARCHAR(50) DEFAULT 'upcoming',
    -- upcoming, ongoing, completed, cancelled

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,  -- User ID from Gateway (no FK constraint across DBs)
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT chk_classes_status CHECK (
        status IN ('upcoming', 'ongoing', 'completed', 'cancelled')
    )
);

CREATE INDEX idx_classes_course ON classes(course_id);
CREATE INDEX idx_classes_teacher ON classes(teacher_id);
CREATE INDEX idx_classes_status ON classes(status) WHERE deleted = FALSE;
CREATE INDEX idx_classes_start_date ON classes(start_date);
```

### 3.3.3. class_schedules

```sql
CREATE TABLE class_schedules (
    id BIGSERIAL PRIMARY KEY,

    class_id BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,

    -- Recurring pattern
    day_of_week INTEGER NOT NULL, -- 0=Sunday, 1=Monday, etc.
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    -- Override room for this schedule
    room_id BIGINT REFERENCES rooms(id),

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_schedules_day CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT chk_schedules_time CHECK (end_time > start_time)
);

CREATE INDEX idx_class_schedules_class ON class_schedules(class_id);
CREATE INDEX idx_class_schedules_day ON class_schedules(day_of_week);
```

### 3.3.4. class_sessions

```sql
CREATE TABLE class_sessions (
    id BIGSERIAL PRIMARY KEY,

    class_id BIGINT NOT NULL REFERENCES classes(id),

    -- Session info
    session_number INTEGER NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    -- Topic/content
    topic VARCHAR(255),
    notes TEXT,

    -- Status
    status VARCHAR(50) DEFAULT 'scheduled',
    -- scheduled, completed, cancelled

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_class_sessions UNIQUE (class_id, session_date)
);

CREATE INDEX idx_class_sessions_class ON class_sessions(class_id);
CREATE INDEX idx_class_sessions_date ON class_sessions(session_date);
```

### 3.3.5. enrollments

```sql
CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,

    class_id BIGINT NOT NULL REFERENCES classes(id),
    student_id BIGINT NOT NULL REFERENCES students(id),  -- Core DB FK

    -- Enrollment info
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Status
    status VARCHAR(50) DEFAULT 'active',
    -- active, completed, dropped, transferred

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,  -- User ID from Gateway (no FK constraint)

    CONSTRAINT uk_enrollments UNIQUE (class_id, student_id)
);

CREATE INDEX idx_enrollments_class ON enrollments(class_id);
CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
```

## 3.4. Learning Module Tables

### 3.4.1. attendance

```sql
CREATE TABLE attendance (
    id BIGSERIAL PRIMARY KEY,

    session_id BIGINT NOT NULL REFERENCES class_sessions(id),
    student_id BIGINT NOT NULL REFERENCES students(id),  -- Core DB FK

    -- Attendance status
    status VARCHAR(20) NOT NULL,
    -- present, absent, late, excused

    -- Check-in time
    check_in_time TIMESTAMP WITH TIME ZONE,

    -- Notes
    notes TEXT,

    -- Marked by (User ID from Gateway - no FK constraint)
    marked_by BIGINT,  -- Teacher or Admin user ID
    marked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_attendance UNIQUE (session_id, student_id),
    CONSTRAINT chk_attendance_status CHECK (
        status IN ('present', 'absent', 'late', 'excused')
    )
);

CREATE INDEX idx_attendance_session ON attendance(session_id);
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_attendance_status ON attendance(status);
```

### 3.4.2. grades

```sql
CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,

    class_id BIGINT NOT NULL REFERENCES classes(id),
    student_id BIGINT NOT NULL REFERENCES users(id),

    -- Grade info
    grade_type VARCHAR(50) NOT NULL,
    -- quiz, midterm, final, assignment, participation

    title VARCHAR(255) NOT NULL,

    -- Score
    score DECIMAL(5, 2) NOT NULL,
    max_score DECIMAL(5, 2) DEFAULT 10,
    weight DECIMAL(3, 2) DEFAULT 1.0, -- For weighted average

    -- Feedback
    feedback TEXT,

    -- Date
    graded_date DATE NOT NULL,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    graded_by BIGINT REFERENCES users(id),

    CONSTRAINT chk_grades_score CHECK (score >= 0 AND score <= max_score)
);

CREATE INDEX idx_grades_class ON grades(class_id);
CREATE INDEX idx_grades_student ON grades(student_id);
CREATE INDEX idx_grades_type ON grades(grade_type);
CREATE INDEX idx_grades_date ON grades(graded_date);
```

### 3.4.3. assignments

```sql
CREATE TABLE assignments (
    id BIGSERIAL PRIMARY KEY,

    class_id BIGINT NOT NULL REFERENCES classes(id),

    -- Assignment info
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT,

    -- Attachments
    attachments JSONB DEFAULT '[]',
    -- [{"name": "homework.pdf", "url": "...", "size": 1024}]

    -- Dates
    assigned_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Grading
    max_score DECIMAL(5, 2) DEFAULT 10,

    -- Status
    status VARCHAR(50) DEFAULT 'active',
    -- draft, active, closed

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_assignments_class ON assignments(class_id);
CREATE INDEX idx_assignments_due ON assignments(due_date);
CREATE INDEX idx_assignments_status ON assignments(status);
```

### 3.4.4. submissions

```sql
CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,

    assignment_id BIGINT NOT NULL REFERENCES assignments(id),
    student_id BIGINT NOT NULL REFERENCES users(id),

    -- Submission content
    content TEXT,
    attachments JSONB DEFAULT '[]',

    -- Status
    status VARCHAR(50) DEFAULT 'submitted',
    -- draft, submitted, late, graded

    -- Grading
    score DECIMAL(5, 2),
    feedback TEXT,
    graded_at TIMESTAMP WITH TIME ZONE,
    graded_by BIGINT REFERENCES users(id),

    -- Timestamps
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_submissions UNIQUE (assignment_id, student_id)
);

CREATE INDEX idx_submissions_assignment ON submissions(assignment_id);
CREATE INDEX idx_submissions_student ON submissions(student_id);
CREATE INDEX idx_submissions_status ON submissions(status);
```

## 3.5. Billing Module Tables

### 3.5.1. invoices

```sql
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,

    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    -- INV-2025-0001

    student_id BIGINT NOT NULL REFERENCES users(id),
    class_id BIGINT REFERENCES classes(id),

    -- Invoice period
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,

    -- Amount
    subtotal DECIMAL(12, 2) NOT NULL,
    discount DECIMAL(12, 2) DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL,
    amount_paid DECIMAL(12, 2) DEFAULT 0,
    balance_due DECIMAL(12, 2) GENERATED ALWAYS AS (total - amount_paid) STORED,

    -- Dates
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NOT NULL,

    -- Status
    status VARCHAR(50) DEFAULT 'pending',
    -- draft, pending, partially_paid, paid, overdue, cancelled

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),

    CONSTRAINT chk_invoices_amounts CHECK (
        subtotal >= 0 AND discount >= 0 AND total >= 0 AND amount_paid >= 0
    ),
    CONSTRAINT chk_invoices_status CHECK (
        status IN ('draft', 'pending', 'partially_paid', 'paid', 'overdue', 'cancelled')
    )
);

CREATE INDEX idx_invoices_student ON invoices(student_id);
CREATE INDEX idx_invoices_class ON invoices(class_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE status IN ('pending', 'partially_paid');
CREATE INDEX idx_invoices_period ON invoices(period_start, period_end);
```

### 3.5.2. invoice_items

```sql
CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,

    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,

    -- Item details
    description VARCHAR(255) NOT NULL,
    quantity INTEGER DEFAULT 1,
    unit_price DECIMAL(12, 2) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,

    -- Reference
    item_type VARCHAR(50), -- tuition, material, other
    reference_id BIGINT, -- class_id, session_id, etc.

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
```

### 3.5.3. payments

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,

    payment_number VARCHAR(50) NOT NULL UNIQUE,
    -- PAY-2025-0001

    invoice_id BIGINT NOT NULL REFERENCES invoices(id),

    -- Amount
    amount DECIMAL(12, 2) NOT NULL,

    -- Payment method
    payment_method VARCHAR(50) NOT NULL,
    -- cash, bank_transfer, momo, zalopay, qr

    -- Transaction info
    transaction_id VARCHAR(100),

    -- QR Payment
    qr_code_url TEXT,

    -- Payer info (for parent payments)
    payer_id BIGINT REFERENCES users(id),
    payer_name VARCHAR(255),

    -- Status
    status VARCHAR(50) DEFAULT 'pending',
    -- pending, completed, failed, refunded

    -- Notes
    notes TEXT,
    receipt_url TEXT,

    -- Timestamps
    paid_at TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    received_by BIGINT REFERENCES users(id),

    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_status CHECK (
        status IN ('pending', 'completed', 'failed', 'refunded')
    )
);

CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_payer ON payments(payer_id);
CREATE INDEX idx_payments_date ON payments(paid_at);
```

## 3.6. Gamification Module Tables

### 3.6.1. point_rules

```sql
CREATE TABLE point_rules (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,
    -- ATTENDANCE, GRADE_A, ASSIGNMENT_SUBMIT, etc.

    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Points
    points INTEGER NOT NULL,

    -- Event trigger
    event_type VARCHAR(50) NOT NULL,
    -- attendance_present, grade_submitted, assignment_submitted

    -- Conditions (JSONB for flexibility)
    conditions JSONB DEFAULT '{}',
    -- {"min_score": 8, "on_time": true}

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Seed data
INSERT INTO point_rules (code, name, points, event_type, conditions) VALUES
('ATTENDANCE_PRESENT', 'Có mặt', 10, 'attendance_present', '{}'),
('GRADE_EXCELLENT', 'Điểm xuất sắc', 50, 'grade_submitted', '{"min_score": 9}'),
('GRADE_GOOD', 'Điểm giỏi', 30, 'grade_submitted', '{"min_score": 8}'),
('ASSIGNMENT_ON_TIME', 'Nộp bài đúng hạn', 20, 'assignment_submitted', '{"on_time": true}');
```

### 3.6.2. student_points

```sql
CREATE TABLE student_points (
    id BIGSERIAL PRIMARY KEY,

    student_id BIGINT NOT NULL REFERENCES users(id),
    rule_id BIGINT REFERENCES point_rules(id),

    -- Points
    points INTEGER NOT NULL,

    -- Reference
    reference_type VARCHAR(50), -- attendance, grade, assignment
    reference_id BIGINT,

    -- Description
    description VARCHAR(255),

    -- Timestamp
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_student_points_student ON student_points(student_id);
CREATE INDEX idx_student_points_earned ON student_points(earned_at);

-- View for total points
CREATE VIEW student_total_points AS
SELECT
    student_id,
    SUM(points) as total_points,
    COUNT(*) as transaction_count
FROM student_points
GROUP BY student_id;
```

### 3.6.3. badges

```sql
CREATE TABLE badges (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Visual
    icon_url TEXT,
    color VARCHAR(20),

    -- Requirements
    requirement_type VARCHAR(50) NOT NULL,
    -- points, streak, special

    requirement_value INTEGER,
    -- e.g., 1000 points, 10 day streak

    requirement_conditions JSONB DEFAULT '{}',

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Seed data
INSERT INTO badges (code, name, requirement_type, requirement_value) VALUES
('FIRST_100', '100 điểm đầu tiên', 'points', 100),
('POINT_MASTER', '1000 điểm', 'points', 1000),
('STREAK_7', 'Đi học 7 ngày liên tiếp', 'streak', 7),
('PERFECT_SCORE', 'Điểm 10', 'special', NULL);
```

### 3.6.4. student_badges

```sql
CREATE TABLE student_badges (
    id BIGSERIAL PRIMARY KEY,

    student_id BIGINT NOT NULL REFERENCES users(id),
    badge_id BIGINT NOT NULL REFERENCES badges(id),

    -- Earned info
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_student_badges UNIQUE (student_id, badge_id)
);

CREATE INDEX idx_student_badges_student ON student_badges(student_id);
CREATE INDEX idx_student_badges_badge ON student_badges(badge_id);
```

### 3.6.5. rewards

```sql
CREATE TABLE rewards (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Visual
    image_url TEXT,

    -- Cost
    points_required INTEGER NOT NULL,

    -- Inventory
    quantity_available INTEGER, -- NULL = unlimited
    quantity_redeemed INTEGER DEFAULT 0,

    -- Validity
    valid_from DATE,
    valid_until DATE,

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### 3.6.6. reward_redemptions

```sql
CREATE TABLE reward_redemptions (
    id BIGSERIAL PRIMARY KEY,

    student_id BIGINT NOT NULL REFERENCES users(id),
    reward_id BIGINT NOT NULL REFERENCES rewards(id),

    -- Points spent
    points_spent INTEGER NOT NULL,

    -- Status
    status VARCHAR(50) DEFAULT 'pending',
    -- pending, approved, delivered, cancelled

    -- Approval
    approved_by BIGINT REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,

    -- Delivery
    delivered_at TIMESTAMP WITH TIME ZONE,

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_redemptions_student ON reward_redemptions(student_id);
CREATE INDEX idx_redemptions_status ON reward_redemptions(status);
```

## 3.7. Parent Module Tables

### 3.7.1. parents

```sql
CREATE TABLE parents (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),

    -- Zalo verification
    zalo_phone VARCHAR(20),
    zalo_verified BOOLEAN DEFAULT FALSE,
    zalo_otp_code VARCHAR(10),
    zalo_otp_expires_at TIMESTAMP WITH TIME ZONE,

    -- Preferences
    notification_preferences JSONB DEFAULT '{"attendance": true, "grades": true, "payments": true}',

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_parents_user ON parents(user_id);
CREATE INDEX idx_parents_zalo ON parents(zalo_phone);
```

### 3.7.2. parent_children

```sql
CREATE TABLE parent_children (
    id BIGSERIAL PRIMARY KEY,

    parent_id BIGINT NOT NULL REFERENCES parents(id),
    child_id BIGINT NOT NULL REFERENCES users(id),

    -- Relationship
    relationship VARCHAR(50) DEFAULT 'parent',
    -- parent, guardian, other

    -- Verification
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by BIGINT REFERENCES users(id),

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uk_parent_children UNIQUE (parent_id, child_id)
);

CREATE INDEX idx_parent_children_parent ON parent_children(parent_id);
CREATE INDEX idx_parent_children_child ON parent_children(child_id);
```

---

# 4. ENTITY RELATIONSHIP DIAGRAMS

## 4.1. Core ERD

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              CORE ERD                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────┐         ┌─────────────┐         ┌─────────────┐               │
│  │   users     │         │   roles     │         │permissions  │               │
│  ├─────────────┤         ├─────────────┤         ├─────────────┤               │
│  │ id          │◄───┐    │ id          │◄───┐    │ id          │               │
│  │ email       │    │    │ code        │    │    │ code        │               │
│  │ first_name  │    │    │ name        │    │    │ name        │               │
│  │ last_name   │    │    └─────────────┘    │    │ module      │               │
│  │ status      │    │          │            │    └─────────────┘               │
│  └─────────────┘    │          │            │          │                        │
│        │            │    ┌─────┴─────┐      │    ┌─────┴─────┐                  │
│        │            │    │user_roles │      │    │role_perms │                  │
│        │            └────┤           ├──────┘    │           │                  │
│        │                 │ user_id   │           │ role_id   │                  │
│        │                 │ role_id   │           │ perm_id   │                  │
│        │                 └───────────┘           └───────────┘                  │
│        │                                                                         │
│        │ student_id                                                              │
│        │                                                                         │
│        ▼                                                                         │
│  ┌─────────────┐                                                                │
│  │ enrollments │                                                                │
│  ├─────────────┤         ┌─────────────┐         ┌─────────────┐               │
│  │ id          │         │   classes   │         │   courses   │               │
│  │ student_id  │◄────────┤             ├────────►│             │               │
│  │ class_id    │─────────►│ id          │         │ id          │               │
│  │ status      │         │ course_id   │─────────►│ code        │               │
│  └─────────────┘         │ teacher_id  │         │ name        │               │
│                          │ name        │         └─────────────┘               │
│                          │ status      │                                        │
│                          └─────────────┘                                        │
│                                 │                                                │
│                                 │ class_id                                       │
│                                 ▼                                                │
│                          ┌─────────────┐                                        │
│                          │class_session│                                        │
│                          ├─────────────┤                                        │
│                          │ id          │                                        │
│                          │ class_id    │                                        │
│                          │ session_date│                                        │
│                          │ status      │                                        │
│                          └─────────────┘                                        │
│                                 │                                                │
│                                 │ session_id                                     │
│                                 ▼                                                │
│                          ┌─────────────┐                                        │
│                          │ attendance  │                                        │
│                          ├─────────────┤                                        │
│                          │ id          │                                        │
│                          │ session_id  │                                        │
│                          │ student_id  │◄─────── users.id                       │
│                          │ status      │                                        │
│                          └─────────────┘                                        │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 4.2. Billing ERD

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              BILLING ERD                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────┐                                                                │
│  │   users     │                                                                │
│  │  (student)  │                                                                │
│  ├─────────────┤                                                                │
│  │ id          │◄─────────────────────────┐                                     │
│  └─────────────┘                          │                                     │
│                                           │                                     │
│                                           │ student_id                          │
│                                           │                                     │
│  ┌─────────────┐         ┌────────────────┴────────────┐                       │
│  │   classes   │         │         invoices            │                       │
│  ├─────────────┤         ├─────────────────────────────┤                       │
│  │ id          │◄────────┤ id                          │                       │
│  └─────────────┘         │ invoice_number              │                       │
│        ▲                 │ student_id                  │                       │
│        │ class_id        │ class_id                    │                       │
│        │                 │ period_start                │                       │
│        │                 │ period_end                  │                       │
│        │                 │ total                       │                       │
│        │                 │ amount_paid                 │                       │
│        │                 │ balance_due (computed)      │                       │
│        │                 │ status                      │                       │
│        │                 └─────────────────────────────┘                       │
│        │                        │                                               │
│        │                        │ invoice_id                                    │
│        │                        │                                               │
│        │                 ┌──────┴───────┐       ┌───────────────┐              │
│        │                 ▼              ▼       │               │              │
│        │          ┌──────────┐    ┌──────────┐  │               │              │
│        │          │ invoice_ │    │ payments │  │   users       │              │
│        │          │  items   │    ├──────────┤  │  (parent)     │              │
│        │          ├──────────┤    │ id       │  ├───────────────┤              │
│        │          │ id       │    │invoice_id│  │ id            │◄────┐       │
│        └──────────┤invoice_id│    │ amount   │  └───────────────┘     │       │
│                   │ desc     │    │ method   │         ▲              │       │
│                   │ amount   │    │ status   │         │ payer_id     │       │
│                   └──────────┘    │ payer_id ├─────────┘              │       │
│                                   │ paid_at  │                        │       │
│                                   └──────────┘                        │       │
│                                                                       │       │
│                                                                       │       │
│  ┌─────────────────────┐                                              │       │
│  │    parents          │◄─────────────────────────────────────────────┘       │
│  ├─────────────────────┤                                                      │
│  │ id                  │                                                      │
│  │ user_id             │──────────────────────────────────────────────────────┘
│  │ zalo_phone          │                                                       │
│  └─────────────────────┘                                                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## 4.3. Gamification ERD

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           GAMIFICATION ERD                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────┐                                                                │
│  │   users     │                                                                │
│  │  (student)  │                                                                │
│  ├─────────────┤                                                                │
│  │ id          │◄───────────────────────┬────────────────────────┐             │
│  └─────────────┘                        │                        │             │
│                                         │                        │             │
│                    student_id           │          student_id    │             │
│                         │               │               │        │             │
│  ┌──────────────┐       │        ┌──────┴───────┐      │        │             │
│  │ point_rules  │       │        │student_badges│      │        │             │
│  ├──────────────┤       │        ├──────────────┤      │        │             │
│  │ id           │◄──┐   │        │ id           │      │        │             │
│  │ code         │   │   │        │ student_id   │◄─────┘        │             │
│  │ points       │   │   │        │ badge_id     │───────┐       │             │
│  │ event_type   │   │   │        │ earned_at    │       │       │             │
│  └──────────────┘   │   │        └──────────────┘       │       │             │
│                     │   │                               │       │             │
│                     │   │                               ▼       │             │
│              rule_id│   │                        ┌─────────────┐│             │
│                     │   │                        │   badges    ││             │
│                     │   ▼                        ├─────────────┤│             │
│               ┌─────┴───────────┐                │ id          ││             │
│               │ student_points  │                │ code        ││             │
│               ├─────────────────┤                │ name        ││             │
│               │ id              │                │ requirement │◄┘             │
│               │ student_id      │                └─────────────┘              │
│               │ rule_id         │                                              │
│               │ points          │                                              │
│               │ reference_type  │                                              │
│               │ reference_id    │                                              │
│               │ earned_at       │                                              │
│               └─────────────────┘                                              │
│                                                                                  │
│                                    student_id                                    │
│                                         │                                        │
│  ┌──────────────┐                      │                                        │
│  │   rewards    │                      ▼                                        │
│  ├──────────────┤              ┌───────────────────┐                            │
│  │ id           │◄─────────────┤reward_redemptions │                            │
│  │ code         │    reward_id │                   │                            │
│  │ name         │              │ id                │                            │
│  │ points_req   │              │ student_id        │                            │
│  │ quantity     │              │ reward_id         │                            │
│  └──────────────┘              │ points_spent      │                            │
│                                │ status            │                            │
│                                └───────────────────┘                            │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# 5. INDEXES & PERFORMANCE

## 5.1. Index Strategy

```sql
-- =====================================================
-- CRITICAL INDEXES (P0)
-- =====================================================

-- User lookup (authentication)
CREATE UNIQUE INDEX idx_users_email_active
ON users(email)
WHERE deleted = FALSE;

-- Student enrollment lookup
CREATE INDEX idx_enrollments_student_active
ON enrollments(student_id)
WHERE status = 'active';

-- Class sessions for today
CREATE INDEX idx_class_sessions_date
ON class_sessions(session_date, class_id)
WHERE status = 'scheduled';

-- Unpaid invoices
CREATE INDEX idx_invoices_unpaid
ON invoices(student_id, due_date)
WHERE status IN ('pending', 'partially_paid');

-- =====================================================
-- IMPORTANT INDEXES (P1)
-- =====================================================

-- Attendance by session
CREATE INDEX idx_attendance_session_status
ON attendance(session_id, status);

-- Grades by student (report cards)
CREATE INDEX idx_grades_student_date
ON grades(student_id, graded_date DESC);

-- Student points (leaderboard)
CREATE INDEX idx_student_points_earned
ON student_points(student_id, earned_at DESC);

-- Parent-child relationship
CREATE INDEX idx_parent_children_verified
ON parent_children(parent_id)
WHERE is_verified = TRUE;

-- =====================================================
-- FULL-TEXT SEARCH INDEXES
-- =====================================================

-- Search students by name
ALTER TABLE users ADD COLUMN search_vector tsvector;

CREATE INDEX idx_users_search
ON users USING GIN(search_vector);

-- Update trigger
CREATE OR REPLACE FUNCTION users_search_trigger() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.first_name, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.last_name, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.email, '')), 'B');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER tsvector_update BEFORE INSERT OR UPDATE
ON users FOR EACH ROW EXECUTE FUNCTION users_search_trigger();
```

## 5.2. Query Optimization Examples

```sql
-- =====================================================
-- OPTIMIZED QUERIES
-- =====================================================

-- Get student dashboard data (single query)
WITH student_data AS (
    SELECT
        u.id,
        u.first_name,
        u.last_name,
        COALESCE(sp.total_points, 0) as total_points,
        COUNT(DISTINCT sb.badge_id) as badge_count
    FROM users u
    LEFT JOIN student_total_points sp ON u.id = sp.student_id
    LEFT JOIN student_badges sb ON u.id = sb.student_id
    WHERE u.id = :student_id
    GROUP BY u.id, u.first_name, u.last_name, sp.total_points
),
enrollments_data AS (
    SELECT
        e.student_id,
        json_agg(json_build_object(
            'class_id', c.id,
            'class_name', c.name,
            'course_name', co.name,
            'teacher_name', t.first_name || ' ' || t.last_name
        )) as classes
    FROM enrollments e
    JOIN classes c ON e.class_id = c.id
    JOIN courses co ON c.course_id = co.id
    LEFT JOIN users t ON c.teacher_id = t.id
    WHERE e.student_id = :student_id AND e.status = 'active'
    GROUP BY e.student_id
),
recent_attendance AS (
    SELECT
        a.student_id,
        COUNT(*) FILTER (WHERE a.status = 'present') as present_count,
        COUNT(*) as total_count
    FROM attendance a
    JOIN class_sessions cs ON a.session_id = cs.id
    WHERE a.student_id = :student_id
      AND cs.session_date >= CURRENT_DATE - INTERVAL '30 days'
    GROUP BY a.student_id
)
SELECT
    s.*,
    e.classes,
    ra.present_count,
    ra.total_count,
    ROUND(ra.present_count::numeric / NULLIF(ra.total_count, 0) * 100, 1) as attendance_rate
FROM student_data s
LEFT JOIN enrollments_data e ON s.id = e.student_id
LEFT JOIN recent_attendance ra ON s.id = ra.student_id;

-- Get overdue invoices with student and parent info
SELECT
    i.id,
    i.invoice_number,
    i.total,
    i.balance_due,
    i.due_date,
    s.first_name || ' ' || s.last_name as student_name,
    s.email as student_email,
    p.zalo_phone as parent_phone,
    pu.email as parent_email
FROM invoices i
JOIN users s ON i.student_id = s.id
LEFT JOIN parent_children pc ON pc.child_id = s.id AND pc.is_verified = TRUE
LEFT JOIN parents p ON pc.parent_id = p.id
LEFT JOIN users pu ON p.user_id = pu.id
WHERE i.status IN ('pending', 'partially_paid')
  AND i.due_date < CURRENT_DATE
ORDER BY i.due_date;
```

## 5.3. Performance Monitoring

```sql
-- Find slow queries
SELECT
    query,
    calls,
    total_time / 1000 as total_seconds,
    mean_time as mean_ms,
    rows
FROM pg_stat_statements
ORDER BY total_time DESC
LIMIT 10;

-- Table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_total_relation_size(schemaname||'.'||tablename) as size_bytes
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY size_bytes DESC;

-- Index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan as times_used,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

---

# 6. DATA MIGRATION & SEEDING

## 6.1. Initial Data Seeding

```sql
-- =====================================================
-- SYSTEM DATA (Run once per database)
-- =====================================================

-- Roles
INSERT INTO roles (code, name, is_system) VALUES
('CENTER_OWNER', 'Chủ trung tâm', TRUE),
('CENTER_ADMIN', 'Quản trị viên', TRUE),
('TEACHER', 'Giáo viên', TRUE),
('STUDENT', 'Học viên', TRUE),
('PARENT', 'Phụ huynh', TRUE);

-- Permissions
INSERT INTO permissions (code, name, module) VALUES
-- User module
('users:read', 'Xem người dùng', 'user'),
('users:write', 'Thêm/sửa người dùng', 'user'),
('users:delete', 'Xóa người dùng', 'user'),
-- Class module
('classes:read', 'Xem lớp học', 'class'),
('classes:manage', 'Quản lý lớp học', 'class'),
('enrollments:manage', 'Quản lý ghi danh', 'class'),
-- Learning module
('attendance:view', 'Xem điểm danh', 'learning'),
('attendance:mark', 'Điểm danh', 'learning'),
('grades:view', 'Xem điểm', 'learning'),
('grades:manage', 'Quản lý điểm', 'learning'),
-- Billing module
('billing:view', 'Xem hóa đơn', 'billing'),
('billing:manage', 'Quản lý hóa đơn', 'billing'),
('payments:receive', 'Thu tiền', 'billing'),
-- Gamification module
('gamification:view', 'Xem điểm thưởng', 'gamification'),
('gamification:manage', 'Quản lý gamification', 'gamification'),
-- Reports
('reports:view', 'Xem báo cáo', 'reports'),
('reports:export', 'Xuất báo cáo', 'reports');

-- Role-Permission mapping (CENTER_OWNER has all)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'CENTER_OWNER';

-- Point rules
INSERT INTO point_rules (code, name, points, event_type, conditions) VALUES
('ATTENDANCE_PRESENT', 'Có mặt đúng giờ', 10, 'attendance_present', '{}'),
('ATTENDANCE_LATE', 'Đi muộn', 5, 'attendance_late', '{}'),
('GRADE_EXCELLENT', 'Điểm 9-10', 50, 'grade_submitted', '{"min_score": 9}'),
('GRADE_GOOD', 'Điểm 8-9', 30, 'grade_submitted', '{"min_score": 8, "max_score": 9}'),
('GRADE_AVERAGE', 'Điểm 6.5-8', 10, 'grade_submitted', '{"min_score": 6.5, "max_score": 8}'),
('ASSIGNMENT_ON_TIME', 'Nộp bài đúng hạn', 20, 'assignment_submitted', '{"on_time": true}'),
('ASSIGNMENT_LATE', 'Nộp bài muộn', 5, 'assignment_submitted', '{"on_time": false}'),
('STREAK_7', '7 ngày liên tiếp', 100, 'streak', '{"days": 7}'),
('STREAK_30', '30 ngày liên tiếp', 500, 'streak', '{"days": 30}');

-- Badges
INSERT INTO badges (code, name, description, requirement_type, requirement_value) VALUES
('NEWBIE', 'Tân binh', 'Tham gia hệ thống', 'special', NULL),
('FIRST_100', '100 điểm', 'Đạt 100 điểm đầu tiên', 'points', 100),
('RISING_STAR', '500 điểm', 'Đạt 500 điểm', 'points', 500),
('POINT_MASTER', '1000 điểm', 'Đạt 1000 điểm', 'points', 1000),
('STREAK_WEEK', 'Đi học 7 ngày liên tiếp', 'Duy trì streak 7 ngày', 'streak', 7),
('STREAK_MONTH', 'Đi học 30 ngày liên tiếp', 'Duy trì streak 30 ngày', 'streak', 30),
('PERFECT_ATTENDANCE', 'Điểm danh hoàn hảo', 'Không vắng buổi nào trong tháng', 'special', NULL),
('TOP_SCORER', 'Điểm 10', 'Đạt điểm tuyệt đối', 'special', NULL);
```

## 6.2. Development Data

```sql
-- =====================================================
-- DEVELOPMENT/TESTING DATA
-- =====================================================

-- Create test admin user
INSERT INTO users (email, password_hash, first_name, last_name, status)
VALUES ('admin@test.com', '$2a$10$...', 'Admin', 'Test', 'active')
RETURNING id;

-- Assign CENTER_OWNER role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@test.com' AND r.code = 'CENTER_OWNER';

-- Create test course
INSERT INTO courses (code, name, category, suggested_tuition)
VALUES ('MATH10', 'Toán lớp 10', 'math', 500000);

-- Create test class
INSERT INTO classes (course_id, code, name, teacher_id, start_date, tuition_amount)
SELECT c.id, 'MATH10-001', 'Toán 10A - Khóa 1', u.id, CURRENT_DATE, 500000
FROM courses c, users u
WHERE c.code = 'MATH10' AND u.email = 'admin@test.com';
```

---

*Tài liệu được tạo bởi: Claude Assistant*
*Ngày: 23/12/2025*
