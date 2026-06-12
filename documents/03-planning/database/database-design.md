# THIẾT KẾ DATABASE
## KiteClass Platform V4.1 (Bundled Model) ⭐

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Dự án** | KiteClass Platform V4.1 (Bundled Model) |
| **Loại tài liệu** | Database Design Document |
| **Version** | 4.1 |
| **Ngày tạo** | 23/12/2025 |
| **Last Updated** | 2026-02-26 ⭐ |
| **DBMS** | PostgreSQL 15+ |
| **Tham chiếu** | system-architecture-v4.md |

---

# MỤC LỤC

1. [Tổng quan Database Architecture](#1-tổng-quan-database-architecture)
2. [KiteHub Database](#2-kitehub-database)
3. [KiteClass Instance Database](#3-kiteclass-instance-database)
4. [LMS Module Schema (V4.1)](#4-lms-module-schema-v41) ⭐ NEW
5. [Marketing Module Schema (V4.1)](#5-marketing-module-schema-v41) ⭐ NEW
6. [Entity Relationship Diagrams](#6-entity-relationship-diagrams)
7. [Indexes & Performance](#7-indexes--performance)
8. [Data Migration & Seeding](#8-data-migration--seeding)

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
│  │  • classes          (class management)                    │ │
│  │  • enrollments      (student-class relationship)          │ │
│  │  • attendance       (attendance tracking)                 │ │
│  │  • invoices         (billing)                             │ │
│  │  • payments         (payment records)                     │ │
│  │  • gamification tables                                    │ │
│  │  (parents/parent_children → Parent Service future)        │ │
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

#### Core Database - students/teachers tables

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

-- Note: Parents table thuộc Parent Service (Optional Addon - Future)
-- Sẽ có separate database khi Parent Service được implement
```

### Mapping Logic

| user_type | reference_id links to | Ý nghĩa |
|-----------|----------------------|---------|
| `ADMIN` | `NULL` | Admin không có entity trong Core |
| `STAFF` | `NULL` | Staff không có entity trong Core |
| `TEACHER` | `teachers.id` | Teacher profile trong Core |
| `PARENT` | `parents.id` (future) | Parent profile trong Parent Service (optional addon) |
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
    -- abc.kitehub.me

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

**⚠️ Note về Parent Service:**
- `parents` và `parent_children` tables thuộc **Parent Service (Optional Addon - Future)**
- Parent Service là separate optional service theo Architecture V4.1
- Không thuộc Core Database scope
- Sẽ có separate database khi Parent Service được implement trong tương lai

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

# 4. LMS MODULE SCHEMA (V4.1) ⭐ NEW

## 4.1. Overview

LMS (Learning Management System) module cho phép tổ chức nội dung khóa học thành modules, lessons, và theo dõi tiến độ học tập của học viên. Hỗ trợ chế độ trial (guest có thể xem lessons miễn phí).

## 4.2. Tables

### 4.2.1. course_modules

```sql
CREATE TABLE course_modules (
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,

    -- Content
    title VARCHAR(200) NOT NULL,
    description TEXT,
    order_number INTEGER NOT NULL DEFAULT 0,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0 NOT NULL,

    -- Constraints
    CONSTRAINT uk_course_modules_course_order
        UNIQUE (course_id, order_number, instance_id, deleted)
);

-- Indexes
CREATE INDEX idx_course_modules_course_id ON course_modules(course_id) WHERE deleted = FALSE;
CREATE INDEX idx_course_modules_instance_id ON course_modules(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_course_modules_order ON course_modules(course_id, order_number) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE course_modules IS 'Learning modules within a course (V4.1)';
COMMENT ON COLUMN course_modules.order_number IS 'Display order within course (unique per course)';
COMMENT ON COLUMN course_modules.instance_id IS 'Tenant ID for multi-tenancy';
```

**Business Rules:**
- BR-LMS-001: `order_number` must be unique per course (within same tenant)
- BR-LMS-002: Deleting a course cascades to all modules
- BR-LMS-003: Soft delete preserves history

---

### 4.2.2. lessons

```sql
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    module_id BIGINT NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,

    -- Content
    title VARCHAR(200) NOT NULL,
    content TEXT,
    video_url VARCHAR(500),

    -- Access Control ⭐ KEY
    is_trial BOOLEAN DEFAULT FALSE NOT NULL,

    -- Metadata
    order_number INTEGER NOT NULL DEFAULT 0,
    estimated_duration INTEGER, -- minutes

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 0 NOT NULL,

    -- Constraints
    CONSTRAINT uk_lessons_module_order
        UNIQUE (module_id, order_number, instance_id, deleted),
    CONSTRAINT chk_lessons_duration
        CHECK (estimated_duration IS NULL OR estimated_duration > 0)
);

-- Indexes
CREATE INDEX idx_lessons_module_id ON lessons(module_id) WHERE deleted = FALSE;
CREATE INDEX idx_lessons_is_trial ON lessons(is_trial) WHERE deleted = FALSE;
CREATE INDEX idx_lessons_instance_id ON lessons(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_lessons_order ON lessons(module_id, order_number) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE lessons IS 'Individual lessons within modules (V4.1)';
COMMENT ON COLUMN lessons.is_trial IS 'Guest access flag: TRUE = public, FALSE = requires enrollment';
COMMENT ON COLUMN lessons.estimated_duration IS 'Estimated completion time in minutes';
```

**Business Rules:**
- BR-LMS-004: `is_trial = TRUE` → Guest users can access
- BR-LMS-005: `is_trial = FALSE` → Only enrolled students can access
- BR-LMS-006: `order_number` unique per module
- BR-LMS-007: Deleting module cascades to all lessons

---

### 4.2.3. learning_resources

```sql
CREATE TABLE learning_resources (
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,

    -- Resource Info
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    url VARCHAR(500),
    file_size BIGINT, -- bytes

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,

    -- Constraints
    CONSTRAINT chk_learning_resources_type
        CHECK (type IN ('PDF', 'VIDEO', 'SLIDES', 'QUIZ', 'OTHER')),
    CONSTRAINT chk_learning_resources_size
        CHECK (file_size IS NULL OR file_size > 0)
);

-- Indexes
CREATE INDEX idx_learning_resources_lesson_id ON learning_resources(lesson_id) WHERE deleted = FALSE;
CREATE INDEX idx_learning_resources_type ON learning_resources(type) WHERE deleted = FALSE;

-- Comments
COMMENT ON TABLE learning_resources IS 'Additional learning materials attached to lessons (V4.1)';
COMMENT ON COLUMN learning_resources.type IS 'Resource type: PDF, VIDEO, SLIDES, QUIZ, OTHER';
COMMENT ON COLUMN learning_resources.file_size IS 'File size in bytes (null for external URLs)';
```

**Business Rules:**
- BR-LMS-008: Type must be one of predefined values
- BR-LMS-009: URL or file_size (at least one) should be present
- BR-LMS-010: Access control inherits from parent lesson

---

### 4.2.4. lesson_progress

```sql
CREATE TABLE lesson_progress (
    id BIGSERIAL PRIMARY KEY,

    -- Relationship
    user_id BIGINT NOT NULL, -- From Gateway users table
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,

    -- Progress Tracking
    completed BOOLEAN DEFAULT FALSE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    progress_percent INTEGER DEFAULT 0 NOT NULL,

    -- Multi-tenant
    instance_id UUID NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT uk_lesson_progress_user_lesson
        UNIQUE (user_id, lesson_id, instance_id),
    CONSTRAINT chk_lesson_progress_percent
        CHECK (progress_percent >= 0 AND progress_percent <= 100),
    CONSTRAINT chk_lesson_progress_completed
        CHECK (
            (completed = FALSE AND completed_at IS NULL) OR
            (completed = TRUE AND completed_at IS NOT NULL)
        )
);

-- Indexes
CREATE INDEX idx_lesson_progress_user_id ON lesson_progress(user_id);
CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress(lesson_id);
CREATE INDEX idx_lesson_progress_completed ON lesson_progress(completed);
CREATE INDEX idx_lesson_progress_instance_id ON lesson_progress(instance_id);

-- Comments
COMMENT ON TABLE lesson_progress IS 'Student learning progress per lesson (V4.1)';
COMMENT ON COLUMN lesson_progress.completed IS 'Mark lesson as complete (triggers completed_at)';
COMMENT ON COLUMN lesson_progress.progress_percent IS 'Progress percentage (0-100)';
```

**Business Rules:**
- BR-LMS-011: One progress record per user-lesson pair
- BR-LMS-012: When `completed = TRUE`, `completed_at` must be set
- BR-LMS-013: `progress_percent` must be between 0-100
- BR-LMS-014: Progress auto-creates on first lesson access

---

## 4.3. LMS Module ERD

```
Course (existing)
  │
  └──< course_modules (1:N) ⭐ V4.1
        │ order_number (unique per course)
        │
        └──< lessons (1:N) ⭐ V4.1
              │ order_number (unique per module)
              │ is_trial (guest access flag)
              │
              ├──< learning_resources (1:N) ⭐ V4.1
              │     (PDF, VIDEO, SLIDES, QUIZ)
              │
              └──< lesson_progress (1:N) ⭐ V4.1
                    │ completed, progress_percent
                    └──> User (Gateway service)
```

---

## 4.4. Sample Data Flow

### Guest User (Trial Lesson)
```
1. Guest views course page
2. System lists modules (course_modules)
3. System lists lessons where is_trial = TRUE
4. Guest clicks trial lesson
5. System displays content + video_url
6. No progress tracking (guest not authenticated)
```

### Enrolled Student
```
1. Student logs in → JWT with enrollment check
2. System lists all lessons (trial + paid)
3. Student clicks lesson
4. System checks: enrollment exists? → Grant access
5. System creates/updates lesson_progress record
6. Student marks complete → progress_percent = 100, completed = TRUE
```

---

# 5. MARKETING MODULE SCHEMA (V4.1) ⭐ NEW

## 5.1. Overview

Marketing module quản lý landing pages, lead capture, và contact forms cho từng tenant. Mỗi tenant có một landing page duy nhất với branding riêng.

## 5.2. Tables

### 5.2.1. landing_pages

```sql
CREATE TABLE landing_pages (
    id BIGSERIAL PRIMARY KEY,

    -- Tenant Relationship (1:1)
    instance_id UUID NOT NULL UNIQUE,

    -- Hero Section
    hero_title VARCHAR(200),
    hero_subtitle VARCHAR(500),
    hero_image_url VARCHAR(500),

    -- About Section
    teacher_bio TEXT,
    logo_url VARCHAR(500),
    tagline VARCHAR(200),

    -- Branding
    primary_color VARCHAR(7), -- Hex #RRGGBB
    secondary_color VARCHAR(7),

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,

    -- Constraints
    CONSTRAINT chk_landing_pages_primary_color
        CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_landing_pages_secondary_color
        CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Indexes
CREATE INDEX idx_landing_pages_instance_id ON landing_pages(instance_id);

-- Comments
COMMENT ON TABLE landing_pages IS 'Tenant-specific landing page content (1:1 per tenant) - V4.1';
COMMENT ON COLUMN landing_pages.instance_id IS '1:1 with tenant (unique constraint)';
COMMENT ON COLUMN landing_pages.primary_color IS 'Primary brand color in hex format (#RRGGBB)';
```

**Business Rules:**
- BR-MKT-001: Each tenant has exactly ONE landing page (1:1 relationship)
- BR-MKT-002: Colors must be valid hex format (#RRGGBB)
- BR-MKT-003: Landing page auto-creates when tenant provisions
- BR-MKT-004: All fields optional (gradual setup)

---

### 5.2.2. leads

```sql
CREATE TABLE leads (
    id BIGSERIAL PRIMARY KEY,

    -- Tenant
    instance_id UUID NOT NULL,

    -- Lead Info
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(20),

    -- Source & Status
    source VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',

    -- Interest
    course_interest_id BIGINT REFERENCES courses(id),
    message TEXT,

    -- Workflow Tracking
    last_contacted_at TIMESTAMP WITH TIME ZONE,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT chk_leads_source
        CHECK (source IN ('LANDING_PAGE', 'CONTACT_FORM', 'TRIAL', 'REFERRAL')),
    CONSTRAINT chk_leads_status
        CHECK (status IN ('NEW', 'CONTACTED', 'CONVERTED', 'LOST'))
);

-- Indexes
CREATE INDEX idx_leads_instance_id ON leads(instance_id);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_created_at ON leads(created_at DESC);
CREATE INDEX idx_leads_source ON leads(source);

-- Comments
COMMENT ON TABLE leads IS 'Guest leads for conversion tracking (V4.1)';
COMMENT ON COLUMN leads.source IS 'Lead origin: LANDING_PAGE, CONTACT_FORM, TRIAL, REFERRAL';
COMMENT ON COLUMN leads.status IS 'Workflow: NEW → CONTACTED → CONVERTED/LOST';
```

**Business Rules:**
- BR-MKT-005: Status workflow: NEW → CONTACTED → CONVERTED/LOST
- BR-MKT-006: When guest signs up → Lead auto-creates with source = TRIAL
- BR-MKT-007: When contact form submitted → Lead + ContactMessage created
- BR-MKT-008: When lead converts → Status = CONVERTED + link to Student

**Status Transitions:**
```
NEW (initial)
  │
  ├──> CONTACTED (admin reaches out)
  │      │
  │      ├──> CONVERTED (signs up as student)
  │      └──> LOST (not interested)
  │
  └──> LOST (immediate rejection)
```

---

### 5.2.3. contact_messages

```sql
CREATE TABLE contact_messages (
    id BIGSERIAL PRIMARY KEY,

    -- Tenant
    instance_id UUID NOT NULL,

    -- Message Info
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    message TEXT NOT NULL,

    -- Read Tracking
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT chk_contact_messages_read
        CHECK (
            (is_read = FALSE AND read_at IS NULL) OR
            (is_read = TRUE AND read_at IS NOT NULL)
        )
);

-- Indexes
CREATE INDEX idx_contact_messages_instance_id ON contact_messages(instance_id);
CREATE INDEX idx_contact_messages_is_read ON contact_messages(is_read) WHERE is_read = FALSE;
CREATE INDEX idx_contact_messages_created_at ON contact_messages(created_at DESC);

-- Comments
COMMENT ON TABLE contact_messages IS 'Guest contact form submissions (V4.1)';
COMMENT ON COLUMN contact_messages.is_read IS 'Marked as read by admin';
```

**Business Rules:**
- BR-MKT-009: When `is_read = TRUE`, `read_at` must be set
- BR-MKT-010: Messages sorted by `created_at DESC` (newest first)
- BR-MKT-011: Contact form submission auto-creates Lead + ContactMessage

---

## 5.3. Marketing Module ERD

```
LandingPage (1:1 with Tenant/Instance) ⭐ V4.1
  │ instance_id (UNIQUE)
  │ hero_title, hero_image_url, primary_color
  │
  └──── (Tenant/Instance)

Lead ⭐ V4.1
  │ source, status (NEW → CONTACTED → CONVERTED/LOST)
  │
  ├──> Course (optional interest via course_interest_id)
  │
  └──> Instance/Tenant (via instance_id)

ContactMessage ⭐ V4.1
  │ message, is_read, read_at
  │
  └──> Instance/Tenant (via instance_id)
```

---

## 5.4. Integration Flow

### Guest Submits Contact Form
```
1. Guest fills contact form on landing page
2. Backend creates:
   a. Lead record (source = CONTACT_FORM, status = NEW)
   b. ContactMessage record (is_read = FALSE)
3. Admin sees notification (unread message count)
4. Admin marks message as read → is_read = TRUE, read_at = NOW()
5. Admin follows up → Lead status = CONTACTED
6. Guest signs up → Lead status = CONVERTED
```

### Guest Requests Trial
```
1. Guest clicks "Try Free Lesson" on landing page
2. Backend creates:
   a. Lead record (source = TRIAL, status = NEW)
3. System grants access to lessons where is_trial = TRUE
4. If guest converts → Lead status = CONVERTED
```

## 5.5. Trial Learning Extensions (V4.1 - Phase 2)

### Overview

Support for trial users (leads) to access limited course content before conversion to paid students. This extends the Marketing Module's lead capture with actual learning functionality.

**Key Features**:
- Trial users authenticated via magic links (passwordless)
- Daily quota limit (3 lessons/day)
- Self-paced learning (no class enrollment required in Phase 1)
- Progress preserved after conversion to paid student

### Tables

#### `leads` (Extended)

**Purpose**: Track trial users separately from paid students. Each lead has a user_id FK to support authentication and trial learning access.

```sql
CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    user_id UUID NOT NULL, -- FK to Gateway users(id) - soft reference
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    source VARCHAR(50) NOT NULL, -- LANDING_PAGE, CONTACT_FORM, TRIAL_SIGNUP, REFERRAL
    status VARCHAR(50) NOT NULL DEFAULT 'NEW', -- NEW, CONTACTED, CONVERTED, LOST
    course_interest_id BIGINT REFERENCES courses(id) ON DELETE SET NULL,
    registration_date TIMESTAMP NOT NULL DEFAULT NOW(),
    converted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_leads_email_instance UNIQUE (email, instance_id),
    CONSTRAINT chk_lead_source CHECK (source IN ('LANDING_PAGE', 'CONTACT_FORM', 'TRIAL_SIGNUP', 'REFERRAL')),
    CONSTRAINT chk_lead_status CHECK (status IN ('NEW', 'CONTACTED', 'CONVERTED', 'LOST'))
);

CREATE INDEX idx_leads_instance_id ON leads(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_leads_user_id ON leads(user_id);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_status ON leads(status) WHERE deleted = FALSE;
CREATE INDEX idx_leads_created_at ON leads(created_at);

COMMENT ON TABLE leads IS 'Trial users tracking - separate from students for different lifecycle';
COMMENT ON COLUMN leads.user_id IS 'FK to Gateway users.id (soft reference for cross-service)';
COMMENT ON COLUMN leads.status IS 'Lead lifecycle: NEW → CONTACTED → CONVERTED/LOST';
```

**Design Note**: This table already exists in Section 5.2.2 but is extended here with `user_id` column to support trial authentication. The table serves dual purpose: marketing lead capture + trial user tracking.

#### `trial_quotas` (NEW)

**Purpose**: Enforce daily lesson access limits (3 lessons/day) for trial users.

```sql
CREATE TABLE trial_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    user_id UUID NOT NULL, -- FK to Gateway users(id)
    quota_date DATE NOT NULL,
    lessons_accessed INT NOT NULL DEFAULT 0,
    quota_limit INT NOT NULL DEFAULT 3, -- Default 3 lessons per day
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_trial_quotas_user_date UNIQUE (user_id, quota_date, instance_id),
    CONSTRAINT chk_quota_limit_positive CHECK (quota_limit > 0),
    CONSTRAINT chk_lessons_accessed_non_negative CHECK (lessons_accessed >= 0)
);

CREATE INDEX idx_trial_quotas_user_id ON trial_quotas(user_id);
CREATE INDEX idx_trial_quotas_date ON trial_quotas(quota_date);
CREATE INDEX idx_trial_quotas_user_date ON trial_quotas(user_id, quota_date);

COMMENT ON TABLE trial_quotas IS 'Daily lesson access limits for trial users (default 3 lessons/day)';
COMMENT ON COLUMN trial_quotas.quota_date IS 'Date of quota (resets daily at midnight)';
COMMENT ON COLUMN trial_quotas.lessons_accessed IS 'Number of lessons accessed on quota_date';
```

**Quota Reset Logic**: New quota record created per user per day. No cleanup needed - records serve as access history.

#### Extensions to Existing Tables

**`courses` table extension**:
```sql
ALTER TABLE courses ADD COLUMN is_trial BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_courses_trial ON courses(is_trial) WHERE deleted = FALSE;

COMMENT ON COLUMN courses.is_trial IS 'Mark course as trial-accessible (single course approach, not separate trial course)';
```

**`lessons` table extension**:
```sql
ALTER TABLE lessons ADD COLUMN is_trial_accessible BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_lessons_trial ON lessons(is_trial_accessible) WHERE deleted = FALSE;

COMMENT ON COLUMN lessons.is_trial_accessible IS 'Mark lesson accessible to trial users (typically first 1-3 lessons per course)';
```

**Design Rationale**: Single course with flags instead of separate trial course to avoid content duplication and simplify conversion.

### Gateway Schema Extension

#### `users.role` enum extension

```sql
-- Migration V12 (Gateway Service)
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'TRIAL_USER';

COMMENT ON TYPE user_role IS 'User roles: SUPER_ADMIN, ADMIN, TEACHER, STUDENT, TRIAL_USER (as of V12)';
```

**Complete `user_role` enum values**:
- SUPER_ADMIN (system admin)
- ADMIN (tenant admin)
- TEACHER (course instructor)
- STUDENT (paid learner)
- TRIAL_USER (trial learner) ⭐ NEW

**Purpose**: Support TRIAL_USER role for authentication and authorization of trial users at Gateway layer.

## 5.6. Storage & File Management (V4.1 - Phase 2)

### Overview

Comprehensive file storage service supporting avatars, documents, videos, certificates, and assignments. Uses S3-compatible storage (MinIO dev, AWS S3 prod) with presigned URLs for secure upload/download, storage quota tracking, and multi-tenant isolation.

**Key Features**:
- Direct client-to-S3 uploads via presigned URLs (bypass backend)
- Storage quota enforcement (Trial: 500MB, Basic: 5GB, Pro: 50GB)
- Multi-tenant isolation (bucket prefixes + instance_id)
- File lifecycle tracking (UPLOADING → PROCESSING → READY → FAILED)
- Access control (PRIVATE, COURSE, PUBLIC)
- Video metadata support (duration, resolution, codec)
- Soft delete with 30-day grace period

**Related Documentation**: See [Storage Service Design](../implementation/storage-service-design.md) for complete architecture, API flows, and implementation details.

### Tables

#### `uploaded_files`

**Purpose**: Store metadata for all uploaded files (avatars, documents, videos, certificates, assignments). Actual file content stored in S3.

```sql
CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    uploaded_by UUID NOT NULL, -- FK to Gateway users(id) - soft reference
    file_type VARCHAR(50) NOT NULL, -- AVATAR, DOCUMENT, VIDEO, CERTIFICATE, ASSIGNMENT
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL UNIQUE, -- S3 path: {tenant-id}/{type}/{uuid}.ext
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADING', -- UPLOADING, PROCESSING, READY, FAILED
    duration_seconds INT, -- Video metadata
    resolution VARCHAR(20), -- Video metadata (e.g., "1920x1080")
    video_codec VARCHAR(50), -- Video metadata (e.g., "h264")
    access_level VARCHAR(50) NOT NULL DEFAULT 'PRIVATE', -- PRIVATE, COURSE, PUBLIC
    related_entity_type VARCHAR(50), -- student, teacher, course, assignment, etc.
    related_entity_id VARCHAR(50), -- UUID of related entity
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_file_type CHECK (file_type IN ('AVATAR', 'DOCUMENT', 'VIDEO', 'CERTIFICATE', 'ASSIGNMENT')),
    CONSTRAINT chk_file_status CHECK (status IN ('UPLOADING', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_access_level CHECK (access_level IN ('PRIVATE', 'COURSE', 'PUBLIC')),
    CONSTRAINT chk_file_size_positive CHECK (file_size_bytes > 0)
);

CREATE INDEX idx_uploaded_files_instance_id ON uploaded_files(instance_id) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_uploaded_by ON uploaded_files(uploaded_by) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_type ON uploaded_files(file_type) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_entity ON uploaded_files(related_entity_type, related_entity_id) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_status ON uploaded_files(status) WHERE deleted = FALSE;
CREATE INDEX idx_uploaded_files_created_at ON uploaded_files(created_at);

COMMENT ON TABLE uploaded_files IS 'File metadata storage - actual files in S3';
COMMENT ON COLUMN uploaded_files.uploaded_by IS 'FK to Gateway users.id (soft reference)';
COMMENT ON COLUMN uploaded_files.storage_path IS 'S3 object key: {tenant-id}/{type}/{uuid}.{ext}';
COMMENT ON COLUMN uploaded_files.status IS 'Upload lifecycle: UPLOADING → PROCESSING → READY → FAILED';
COMMENT ON COLUMN uploaded_files.access_level IS 'Access control: PRIVATE (uploader only), COURSE (teacher+students), PUBLIC (all authenticated)';
```

**File Type Limits**:
- AVATAR: max 10MB (image/png, image/jpeg, image/webp)
- DOCUMENT: max 50MB (application/pdf, .docx, .xlsx)
- VIDEO: max 2GB (video/mp4, video/webm)
- CERTIFICATE: max 5MB (application/pdf)
- ASSIGNMENT: max 50MB (application/pdf, .docx)

**Storage Path Format**: `{tenant-id}/{file-type}/{file-uuid}.{extension}`
- Example: `550e8400-e29b-41d4-a716-446655440000/avatars/abc123.png`

#### `storage_quotas`

**Purpose**: Track storage usage per tenant and enforce limits.

```sql
CREATE TABLE storage_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL UNIQUE, -- One quota per tenant
    quota_bytes BIGINT NOT NULL DEFAULT 1073741824, -- Default 1GB
    used_bytes BIGINT NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_quota_bytes_positive CHECK (quota_bytes > 0),
    CONSTRAINT chk_used_bytes_non_negative CHECK (used_bytes >= 0)
);

CREATE INDEX idx_storage_quotas_instance_id ON storage_quotas(instance_id);

COMMENT ON TABLE storage_quotas IS 'Per-tenant storage quota tracking';
COMMENT ON COLUMN storage_quotas.quota_bytes IS 'Maximum storage allowed (Trial: 500MB, Basic: 5GB, Pro: 50GB, Enterprise: custom)';
COMMENT ON COLUMN storage_quotas.used_bytes IS 'Current storage usage (calculated from uploaded_files)';
COMMENT ON COLUMN storage_quotas.last_calculated_at IS 'Last time quota was recalculated (scheduled job)';
```

**Quota Tiers** (example values):
- Trial: 500 MB (524,288,000 bytes)
- Basic: 5 GB (5,368,709,120 bytes)
- Pro: 50 GB (53,687,091,200 bytes)
- Enterprise: Custom (unlimited)

**Quota Calculation**:
```sql
-- Scheduled job (daily) to recalculate quotas
UPDATE storage_quotas sq
SET used_bytes = (
    SELECT COALESCE(SUM(file_size_bytes), 0)
    FROM uploaded_files uf
    WHERE uf.instance_id = sq.instance_id
      AND uf.status = 'READY'
      AND uf.deleted = FALSE
),
last_calculated_at = NOW(),
updated_at = NOW();
```

### ERD - Trial Learning Extension

```
┌─────────────────────────────────────────────────────────────────┐
│                    TRIAL LEARNING ERD                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Gateway Service                 Core Service                    │
│                                                                  │
│  ┌──────────────┐                                               │
│  │    users     │                                               │
│  ├──────────────┤                                               │
│  │ id (PK)      │──────┐                                        │
│  │ email        │      │                                        │
│  │ role (enum)  │      │ user_id (FK - soft reference)         │
│  │ instance_id  │      │                                        │
│  └──────────────┘      │                                        │
│       │                │                                        │
│       │ role =         ▼                                        │
│       │ TRIAL_USER  ┌──────────────┐                           │
│       │             │    leads     │                           │
│       │             ├──────────────┤                           │
│       │             │ id (PK)      │                           │
│       │             │ user_id (FK) │─────┐                     │
│       │             │ email        │     │                     │
│       │             │ status       │     │                     │
│       │             │ source       │     │                     │
│       │             │ course_id    │──┐  │                     │
│       │             └──────────────┘  │  │                     │
│       │                                │  │                     │
│       │                                │  │                     │
│       └─────┐                          │  │                     │
│             │                          │  │                     │
│             │ user_id (FK)             │  │                     │
│             ▼                          │  │                     │
│      ┌──────────────┐                 │  │                     │
│      │trial_quotas  │                 │  │                     │
│      ├──────────────┤                 │  │                     │
│      │ id (PK)      │                 │  │                     │
│      │ user_id (FK) │                 │  │                     │
│      │ quota_date   │                 │  │                     │
│      │ lessons_     │                 │  │                     │
│      │   accessed   │                 │  │                     │
│      │ quota_limit  │                 │  │                     │
│      └──────────────┘                 │  │                     │
│                                        │  │                     │
│                                        │  │                     │
│                                        ▼  ▼                     │
│                                    ┌──────────────┐            │
│                                    │   courses    │            │
│                                    ├──────────────┤            │
│                                    │ id (PK)      │            │
│                                    │ is_trial ⭐  │            │
│                                    └──────────────┘            │
│                                           │                     │
│                                           │ course_id           │
│                                           ▼                     │
│                                    ┌──────────────┐            │
│                                    │   lessons    │            │
│                                    ├──────────────┤            │
│                                    │ id (PK)      │            │
│                                    │ course_id    │            │
│                                    │ is_trial_    │            │
│                                    │  accessible⭐│            │
│                                    └──────────────┘            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

⭐ = New columns for trial learning
```

### Data Flow

**Trial Registration Flow**:
```
1. Guest submits trial signup form (email, name, phone)
   ↓
2. Core Service creates Lead record (status = NEW, source = TRIAL_SIGNUP)
   ↓
3. Core calls Gateway API to generate magic link
   ↓
4. Gateway creates User (role = TRIAL_USER) and sends magic link email
   ↓
5. Guest clicks magic link → Gateway verifies token → returns JWT
   ↓
6. Core updates Lead.user_id with Gateway user ID
```

**Trial Lesson Access Flow**:
```
1. Trial user requests lesson (JWT with role = TRIAL_USER)
   ↓
2. Core checks lesson.is_trial_accessible = TRUE
   ↓
3. Core checks trial_quotas: lessons_accessed < quota_limit (3)
   ↓
4. If quota OK: Increment lessons_accessed, return lesson content
   ↓
5. If quota exceeded: Return 429 Too Many Requests
```

**Lead → Student Conversion Flow**:
```
1. Trial user completes payment
   ↓
2. Payment service verifies transaction
   ↓
3. Core calls Gateway API to update user.role: TRIAL_USER → STUDENT
   ↓
4. Core updates Lead.status = CONVERTED, Lead.converted_at = NOW()
   ↓
5. Core creates Enrollment record (reuses existing user_id)
   ↓
6. Progress preserved (lesson_progress table uses user_id, not student_id)
```

### Business Rules

- **BR-TRIAL-001**: Trial users (TRIAL_USER role) can only access lessons where `is_trial_accessible = TRUE`
  - Implementation: LessonService checks role + lesson flag before returning content
  - Error: `TRIAL_USER_PAID_LESSON_ACCESS_DENIED` if accessing paid lesson

- **BR-TRIAL-002**: Trial users have 3 lessons/day quota limit enforced by `trial_quotas` table
  - Implementation: TrialQuotaService.checkAndIncrementQuota() called before lesson access
  - Quota resets daily (checked by `quota_date` column)
  - Error: `TRIAL_QUOTA_EXCEEDED` if quota exhausted

- **BR-TRIAL-003**: Lead → Student conversion updates `users.role` from TRIAL_USER to STUDENT (same user_id, progress preserved)
  - Implementation: Gateway API PUT /users/{id}/role
  - Same user_id → lesson_progress records automatically available to student

- **BR-TRIAL-004**: Each lead must have unique email per tenant (instance_id)
  - Implementation: `CONSTRAINT uq_leads_email_instance UNIQUE (email, instance_id)`
  - Error: `LEAD_EMAIL_EXISTS` if duplicate registration attempt

- **BR-TRIAL-005**: Trial quota resets daily at midnight (checked by `quota_date`)
  - Implementation: New TrialQuota record created per user per day
  - No cleanup job needed - records serve as access history

- **BR-TRIAL-006**: Trial users cannot enroll in classes (self-paced learning only in Phase 1)
  - Implementation: EnrollmentService rejects class_id if user role = TRIAL_USER
  - Error: `TRIAL_USER_CLASS_ENROLLMENT_NOT_ALLOWED`

### Design Rationale

#### Why separate Leads table instead of merging with Students?

**Decision**: Use separate `leads` table, not merge with `students` table.

**Rationale**:
- Different data lifecycle (leads can be NEW, CONTACTED, LOST - states not applicable to students)
- Different business processes (lead nurturing vs student management)
- Clear domain separation (sales/marketing vs education)
- Analytics needs (conversion funnel tracking, source attribution)
- Students may come from non-trial sources (direct signup, offline registration)

**Alternative considered**: Add `is_trial` flag to students table
- **Rejected**: Mixes two different domains, complicates student queries with trial-specific logic

#### Why TRIAL_USER role in Gateway instead of Core-only?

**Decision**: Add TRIAL_USER to Gateway's `user_role` enum.

**Rationale**:
- Authentication handled at Gateway layer (JWT tokens need role claims)
- API Gateway needs role-based routing (e.g., rate limiting stricter for trial users)
- Consistent with multi-tenant security model (all role authorization at Gateway)
- Frontend can show/hide features based on JWT role claim

**Alternative considered**: Keep TRIAL_USER status only in Core Service
- **Rejected**: Requires Core to make auth decisions, breaks Gateway responsibility

#### Why 3 lessons/day quota limit?

**Decision**: Default quota_limit = 3 lessons per day.

**Rationale**:
- Balance between "try before buy" (enough to evaluate quality) and "create urgency" (limited access encourages conversion)
- Prevent abuse (unlimited free access would reduce paid conversions)
- Psychological: 3 lessons ≈ 1-2 hours learning → enough to see value
- Conversion window: 7-10 days to complete ~20 trial lessons → encourages conversion within 2 weeks

**Alternative considered**: Unlimited trial access for 7 days
- **Rejected**: Users might binge-watch all content and not convert

#### Why single course with `is_trial` flag instead of separate trial course?

**Decision**: Mark existing courses with `is_trial = TRUE`, not create separate "trial" courses.

**Rationale**:
- Avoid content duplication (same lessons copied to trial + paid courses)
- Easier content management (update once, applies to both trial and paid users)
- Simpler conversion (no data migration between courses)
- Consistent progress tracking (same course_id before and after conversion)

**Alternative considered**: Create separate courses for trial users
- **Rejected**: Content duplication, complex conversion logic, sync issues

#### Why self-paced learning (no class enrollment) for trial Phase 1?

**Decision**: Trial users access lessons directly (no class enrollment required) in Phase 1.

**Rationale**:
- Simpler onboarding (no scheduling conflicts, no class selection complexity)
- Immediate access (no waiting for class to start)
- Lower barrier to entry (guest can start learning within 5 minutes)
- Phase 1 focus: Validate "try before buy" concept before adding class features

**Alternative considered**: Allow trial users to join classes
- **Deferred to Phase 2**: Requires class capacity management, teacher-student interaction, attendance tracking

#### Why UPDATE ROLE approach for Lead → Student conversion?

**Decision**: Update existing user's role (TRIAL_USER → STUDENT), not create new student record.

**Rationale**:
- Progress preservation automatic (same user_id → lesson_progress records preserved)
- Audit trail preserved (created_at, updated_at on user record)
- Simpler implementation (no data migration, no foreign key updates)
- Lead record kept for analytics (conversion funnel tracking)

**Alternative considered**: Create new student record, delete user
- **Rejected**: Loses progress, complicates foreign key relationships, loses audit trail

#### Why magic link authentication instead of password?

**Decision**: Trial users authenticate via magic links (passwordless), not traditional password.

**Rationale**:
- Faster onboarding (no password complexity requirements, no "forgot password" flow)
- Better UX for trial (guest clicks link in email → instant access)
- Security: One-time tokens (30-minute expiry) reduce credential theft risk
- Mobile-friendly (no typing passwords on small screens)

**Alternative considered**: Traditional email + password registration
- **Rejected**: Higher friction → lower conversion rate from guest to trial user

### Implementation Notes

- **Migration**: V12 required (see database-migration-plan.md Section 5.5)
- **Gateway Service**: Must handle TRIAL_USER authentication and JWT generation
- **Core Service**: Must implement TrialQuotaService for quota enforcement before lesson access
- **Frontend**: Must display quota counter (e.g., "2/3 lessons today") and upgrade CTA when quota exceeded
- **Testing**: Integration tests must cover multi-tenant isolation (trial users can't cross tenants)

### Performance Considerations

- **Index on `trial_quotas(user_id, quota_date)`**: Composite index for fast daily quota lookup
- **Soft delete on leads**: Use `deleted = FALSE` in WHERE clauses to filter soft-deleted records
- **Cache trial course lessons**: Cache `SELECT * FROM lessons WHERE course_id = X AND is_trial_accessible = TRUE` (static data)

### Security Notes

- **Rate limiting**: Trial users have stricter rate limits (30 req/min vs 100 req/min for students)
- **CORS**: Trial users access lessons from landing page domain → requires CORS whitelist per tenant
- **Magic link tokens**: Stored in Redis with 30-minute TTL, deleted after one use

---

# 6. ENTITY RELATIONSHIP DIAGRAMS

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

# 7. INDEXES & PERFORMANCE

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

# 8. DATA MIGRATION & SEEDING

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

## 7. AI Branding v2 Schema (V31-V45) — added 2026-04-26 (GAP-234)

After Waves 2-4 shipped AI Branding v2 to `kiteclass-core/`, the following tables were added to the **KiteClass tenant DB** (not the KiteHub platform DB as originally planned in `ai-branding-v2-redesign.md` — see §0 of that doc for the deviation note). All tables carry `instance_id UUID NOT NULL` for multi-tenant isolation.

### 7.1 Lifecycle + provisioning

#### `frontend_instances` (V31)

Tracks per-tenant frontend instance provisioning state machine.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | tenant scope |
| tenant_id | VARCHAR(100) NOT NULL | |
| slug | VARCHAR(80) NOT NULL | unique per `(instance_id, deleted=FALSE)` |
| frontend_url | VARCHAR(300) | populated on DEPLOYED |
| status | VARCHAR(20) NOT NULL | check constraint: NOT_STARTED / INITIALIZING / GENERATING / DEPLOYED / REGENERATING / FAILED |
| initializing_at, generating_at, deployed_at, last_regenerate_at, failed_at | TIMESTAMP | per-state timestamps |
| retry_count | INT NOT NULL DEFAULT 0 | |
| failure_reason | VARCHAR(1000) | |
| branding_version | INT NOT NULL DEFAULT 0 | bumps on rebrand |
| created_at, updated_at, created_by, updated_by, version, deleted | common audit cols | |

State transitions enforced by `TenantProvisioningSaga` only.

#### `rebrand_approvals` (V34)

Approval workflow for rebrand operations on existing DEPLOYED instances.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id, target_instance_id | UUID, BIGINT FK | scope + target |
| status | VARCHAR(16) | PENDING / APPROVED / REJECTED / EXPIRED |
| initiator_user_id, approver_user_id | BIGINT | |
| reason, rejection_reason | VARCHAR(500) | |
| requested_at, approved_at, rejected_at, expires_at | TIMESTAMP | |

### 7.2 Resource pipeline

#### `branding_resources` (V32)

Per-resource artifact tracker classified by `ResourceCategory` (STATIC / TEMPLATE / FULL_AI).

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | |
| type | VARCHAR(30) | LOGO / FAVICON / BANNER / HERO / COURSE_THUMBNAIL / SOCIAL_COVER / EMAIL_HEADER |
| category | VARCHAR(20) | STATIC / TEMPLATE / FULL_AI |
| storage_url | VARCHAR(500) | MinIO/S3 URL |
| template_id | BIGINT | required when category=TEMPLATE |
| ai_job_id | UUID | required when category=FULL_AI |
| metadata | JSONB | brand colors used, template params |

V45 adds composite index on `(instance_id, deleted)` for fast non-deleted lookups.

#### `branding` (V40)

Active branding snapshot (1 row per instance) — applied to KiteClass FE via `/api/v1/branding/{instanceId}/package`.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | |
| logo_url, favicon_url | VARCHAR(500) | |
| display_name | VARCHAR(200) NOT NULL | |
| tagline | VARCHAR(500) | |
| primary_color, secondary_color, accent_color | VARCHAR(7) | hex with `#` prefix |
| theme_config_json | TEXT | additional CSS vars |
| contact_email, contact_phone, address | VARCHAR | |
| facebook_url, zalo_url, website_url | VARCHAR(500) | |

#### `branding_versions` (V43)

Snapshot history for rebrand rollback.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | |
| version_number | INT NOT NULL | monotonic per instance |
| snapshot_json | JSONB NOT NULL | full theme+assets snapshot |
| rollback_of | BIGINT FK self | non-null if this version is a rollback |
| active | BOOLEAN | currently applied |

### 7.3 Quality + moderation

#### `quality_reports` (V39)

Output of `InstanceQualityReviewer.review()` — 5 sub-scores, total /100.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | |
| target_instance_id | BIGINT | FK frontend_instances |
| branding_version | INT NOT NULL | which version was reviewed |
| score, passed | INT, BOOLEAN | total /100; passed = score >= 70 |
| contrast_score, css_vars_score, asset_urls_score, visual_regression_score, logo_placement_score | INT | per-check sub-scores (each /20) |
| issues | JSONB | per-check findings list |

#### `moderation_queue` (V36)

3-stage content moderation pipeline state.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | |
| target_type, target_id | VARCHAR(100) | polymorphic ref (e.g. `BrandingResource:42`) |
| status | VARCHAR(32) | PENDING / APPROVED / REJECTED / ESCALATED |
| flagged_keywords | JSONB | keyword stage hits |
| reason | VARCHAR(500) | |
| assigned_reviewer_id | BIGINT | human stage assignment |
| decided_at | TIMESTAMP | |

#### `dmca_takedown_requests` (V37)

Inbound DMCA-style takedown tracking (used for VN-equivalent IP complaints too).

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | |
| reporter_email, reporter_name | VARCHAR(255) | |
| alleged_infringing_url | VARCHAR(2000) | |
| copyrighted_work_description | VARCHAR(4000) | |
| status | VARCHAR(16) | PENDING / APPROVED / REJECTED / CONTESTED / EXECUTED |
| counter_notice_email | VARCHAR(255) | |
| reviewer_user_id | BIGINT | |
| reviewed_at, executed_at, contested_at | TIMESTAMP | |
| rejection_reason | VARCHAR(500) | |

### 7.4 Cross-cutting

#### `outbox_events` (V33)

Transactional outbox for reliable event publishing per `design-patterns.md` §3.5.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | |
| aggregate_type, aggregate_id | VARCHAR(100) | source aggregate |
| event_type | VARCHAR(100) | e.g. `instance.deployed` |
| payload | JSONB NOT NULL | event body |
| status | VARCHAR(16) | PENDING / PUBLISHED / FAILED |
| retry_count | INT | |
| last_error | TEXT | |
| created_at, published_at, next_attempt_at, updated_at | TIMESTAMP | |

#### `audit_log` (V35)

Action audit trail for admin / sensitive operations.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id | UUID NOT NULL | |
| action_type | VARCHAR(100) | e.g. `branding.rebrand.approved` |
| aggregate_type, aggregate_id | VARCHAR(100) | |
| actor_user_id, actor_role | BIGINT, VARCHAR(50) | who performed |
| payload | JSONB | full action context |
| reason | VARCHAR(500) | optional human reason |

#### `deletion_requests` (V38)

Right-to-be-forgotten / GDPR-style deletion workflow with grace window.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| instance_id, tenant_id | UUID NOT NULL | |
| user_id | BIGINT NOT NULL | requester |
| status | VARCHAR(16) | REQUESTED / IN_GRACE / PROCESSING / COMPLETED / CANCELLED |
| requested_at | TIMESTAMP NOT NULL | |
| grace_starts_at, grace_ends_at | TIMESTAMP | grace period for reversal |
| processing_started_at, completed_at, cancelled_at | TIMESTAMP | |
| cancellation_reason | VARCHAR(500) | |
| data_export_url | VARCHAR(1024) | export bundle for user |

### 7.5 Other Wave 2-4 additions (non-AI)

- `student_bulk_import_jobs` (V41) — async student CSV import worker state.
- `parents` + `parent_student_links` (V42) — parent portal linking.
- `class_schedule_slots` (V44) — recurring schedule slots referencing `subject_sections`.

### 7.6 ER overview (AI Branding v2 cluster)

```
frontend_instances ──┬── 1:N ──→ branding_resources
                     ├── 1:N ──→ quality_reports
                     ├── 1:N ──→ rebrand_approvals (target)
                     ├── 1:N ──→ branding_versions
                     ├── 1:N ──→ outbox_events
                     ├── 1:N ──→ audit_log entries
                     └── 1:1 ──→ branding (active snapshot)

branding_versions ── self-FK rollback_of

moderation_queue   ── polymorphic ──→ BrandingResource | Branding | other
dmca_takedown_requests ── alleged URL → branding_resources (informational)
deletion_requests  ── 1:1 → frontend_instances (purge target)
```

**Diagrams:** `documents/06-diagrams/plantuml/03-erd.puml` (KiteClass ERD with v2 package), `04-architecture-full.puml` (v2 module boxes + queue topology), `14-ai-branding-pipeline.puml` (v2 Analyzer → Planner → Executor → Reviewer flow), `16-database-schema-full.puml` (full v2 tables in KiteClass DB with HISTORICAL annotations on legacy KiteHub tables).

---

*Tài liệu được tạo bởi: Claude Assistant*
*Ngày: 23/12/2025*
*Last sync: 2026-04-26 (GAP-234 — added §7 AI Branding v2 schema)*
