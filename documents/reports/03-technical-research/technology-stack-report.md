# BÁO CÁO CÔNG NGHỆ CHI TIẾT - KITECLASS PLATFORM
## Technology Stack & AWS Deployment Architecture

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Tên dự án** | KiteClass Platform |
| **Phiên bản** | 2.0 |
| **Ngày tạo** | 16/12/2025 |
| **Loại tài liệu** | Technology Stack Report |

---

# PHẦN 1: TỔNG QUAN CÔNG NGHỆ

## 1.1. Technology Decision Matrix

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      TECHNOLOGY SELECTION CRITERIA                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Backend Priority: Java Spring Boot                                         │
│  ─────────────────────────────────                                          │
│  ✅ Ưu điểm:                                                                │
│     • Mature ecosystem, enterprise-ready                                    │
│     • Strong typing, compile-time safety                                    │
│     • Excellent ORM (Hibernate/JPA)                                         │
│     • Great for business logic & CRUD operations                            │
│     • Large talent pool                                                     │
│                                                                              │
│  ⚠️ Ngoại lệ (khi cần performance/real-time):                              │
│     • Streaming Service → Node.js (Socket.io, WebRTC)                       │
│     • Message Service → Node.js (WebSocket native support)                  │
│     • AI Agent → Python (ML libraries ecosystem)                            │
│                                                                              │
│  Frontend Priority: Next.js (React)                                         │
│  ──────────────────────────────────                                         │
│  ✅ Lý do:                                                                  │
│     • SSR/SSG for SEO (important for Sale landing pages)                    │
│     • Great developer experience                                            │
│     • API routes for BFF pattern                                            │
│     • Image optimization built-in                                           │
│                                                                              │
│  Database Priority: PostgreSQL                                              │
│  ──────────────────────────────────                                         │
│  ✅ Lý do:                                                                  │
│     • ACID compliance (critical for education data)                         │
│     • JSON support (flexible when needed)                                   │
│     • Full-text search (pg_trgm)                                            │
│     • Mature, reliable, open-source                                         │
│                                                                              │
│  Deployment: AWS                                                            │
│  ──────────────                                                             │
│  ✅ Services sử dụng:                                                       │
│     • EKS (Kubernetes) - container orchestration                            │
│     • RDS (PostgreSQL) - managed database                                   │
│     • ElastiCache (Redis) - caching layer                                   │
│     • S3 - object storage                                                   │
│     • CloudFront - CDN                                                      │
│     • Route 53 - DNS                                                        │
│     • ALB - load balancing                                                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 1.2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    TECHNOLOGY ARCHITECTURE MAP                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                           ┌──────────────┐                                  │
│                           │   Route 53   │ (DNS)                            │
│                           └──────┬───────┘                                  │
│                                  │                                          │
│                           ┌──────▼───────┐                                  │
│                           │  CloudFront  │ (CDN)                            │
│                           └──────┬───────┘                                  │
│                                  │                                          │
│                           ┌──────▼───────┐                                  │
│                           │     ALB      │ (Load Balancer)                  │
│                           └──────┬───────┘                                  │
│                                  │                                          │
│          ┌───────────────────────┼───────────────────────┐                  │
│          │                       │                       │                  │
│    ┌─────▼──────┐         ┌──────▼──────┐         ┌─────▼──────┐           │
│    │  KiteHub   │         │ KiteClass   │         │ KiteClass  │           │
│    │ (Monolith) │         │  Instance#1 │         │ Instance#N │           │
│    │            │         │             │         │            │           │
│    │ Java       │         │ Java/Node   │         │ Java/Node  │           │
│    │ Spring Boot│         │ Microservices│        │Microservices│          │
│    └─────┬──────┘         └──────┬──────┘         └─────┬──────┘           │
│          │                       │                       │                  │
│          │         ┌─────────────┴───────────────┬───────┘                  │
│          │         │                             │                          │
│    ┌─────▼─────────▼─────┐               ┌───────▼────────┐                │
│    │   RDS PostgreSQL    │               │ ElastiCache    │                │
│    │   (Multi-AZ)        │               │ Redis          │                │
│    └─────────────────────┘               └────────────────┘                │
│                                                                              │
│    ┌─────────────────────┐               ┌────────────────┐                │
│    │    S3 Buckets       │               │      SQS       │                │
│    │  (Assets, Videos)   │               │  (Message Queue)│               │
│    └─────────────────────┘               └────────────────┘                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 2: KITEHUB - MODULAR MONOLITH

## 2.1. KiteHub Technology Stack

### 2.1.1. Overall Stack

| Layer | Technology | Version | Lý do chọn |
|-------|------------|---------|------------|
| **Backend** | Java Spring Boot | 3.2+ | Enterprise-grade, mature ecosystem |
| **Frontend** | Next.js (React) | 14+ | SSR/SSG for landing pages, SEO |
| **Database** | PostgreSQL | 16+ | ACID, JSON support, full-text search |
| **Cache** | Redis | 7+ | Session storage, caching |
| **Message Queue** | AWS SQS | - | Managed, reliable, cost-effective |
| **Object Storage** | AWS S3 | - | Scalable, cheap storage |
| **Container** | Docker | 24+ | Standardized packaging |
| **Orchestration** | AWS EKS | 1.28+ | Managed Kubernetes |

### 2.1.2. Backend Architecture (Java Spring Boot Monolith)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    KITEHUB SPRING BOOT MONOLITH                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  kitehub-backend/                                                           │
│  ├── src/main/java/com/kiteclass/kitehub/                                   │
│  │   ├── KiteHubApplication.java                                            │
│  │   │                                                                       │
│  │   ├── modules/                                                           │
│  │   │   ├── sale/                          # Sale Module                   │
│  │   │   │   ├── controller/                                                │
│  │   │   │   │   ├── LandingController.java                                 │
│  │   │   │   │   ├── ProductController.java                                 │
│  │   │   │   │   └── OrderController.java                                   │
│  │   │   │   ├── service/                                                   │
│  │   │   │   │   ├── OrderService.java                                      │
│  │   │   │   │   ├── PaymentService.java                                    │
│  │   │   │   │   └── PricingService.java                                    │
│  │   │   │   ├── repository/                                                │
│  │   │   │   │   ├── OrderRepository.java                                   │
│  │   │   │   │   └── ProductRepository.java                                 │
│  │   │   │   ├── entity/                                                    │
│  │   │   │   │   ├── Order.java                                             │
│  │   │   │   │   ├── Product.java                                           │
│  │   │   │   │   └── Transaction.java                                       │
│  │   │   │   └── dto/                                                       │
│  │   │   │       ├── OrderRequest.java                                      │
│  │   │   │       └── OrderResponse.java                                     │
│  │   │   │                                                                   │
│  │   │   ├── message/                       # Message Module (Exception!)   │
│  │   │   │   # ⚠️ WebSocket cần Node.js cho performance                    │
│  │   │   │   # Đề xuất: Tách riêng thành Node.js service                    │
│  │   │   │                                                                   │
│  │   │   ├── maintaining/                   # Maintaining Module            │
│  │   │   │   ├── controller/                                                │
│  │   │   │   │   ├── InstanceController.java                                │
│  │   │   │   │   └── MonitoringController.java                              │
│  │   │   │   ├── service/                                                   │
│  │   │   │   │   ├── ProvisionerService.java                                │
│  │   │   │   │   ├── KubernetesService.java  # Fabric8 K8s client          │
│  │   │   │   │   └── MonitoringService.java                                 │
│  │   │   │   ├── repository/                                                │
│  │   │   │   │   └── InstanceRepository.java                                │
│  │   │   │   └── entity/                                                    │
│  │   │   │       └── KiteClassInstance.java                                 │
│  │   │   │                                                                   │
│  │   │   └── aiagent/                       # AI Agent Module (Exception!)  │
│  │   │       # ⚠️ AI/ML cần Python cho libraries                            │
│  │   │       # Đề xuất: Tách riêng thành Python service                     │
│  │   │                                                                       │
│  │   ├── shared/                            # Shared Infrastructure         │
│  │   │   ├── config/                                                        │
│  │   │   │   ├── SecurityConfig.java                                        │
│  │   │   │   ├── DatabaseConfig.java                                        │
│  │   │   │   └── RedisConfig.java                                           │
│  │   │   ├── security/                                                      │
│  │   │   │   ├── JwtTokenProvider.java                                      │
│  │   │   │   └── JwtAuthenticationFilter.java                               │
│  │   │   ├── exception/                                                     │
│  │   │   │   ├── GlobalExceptionHandler.java                                │
│  │   │   │   └── BusinessException.java                                     │
│  │   │   └── util/                                                          │
│  │   │       ├── DateUtil.java                                              │
│  │   │       └── ValidationUtil.java                                        │
│  │   │                                                                       │
│  │   └── integration/                       # External Integrations         │
│  │       ├── payment/                                                       │
│  │       │   ├── VNPayService.java                                          │
│  │       │   └── StripeService.java                                         │
│  │       └── storage/                                                       │
│  │           └── S3Service.java                                             │
│  │                                                                           │
│  └── src/main/resources/                                                    │
│      ├── application.yml                                                    │
│      ├── application-dev.yml                                                │
│      ├── application-prod.yml                                               │
│      └── db/migration/                      # Flyway migrations             │
│          ├── V1__create_sales_schema.sql                                    │
│          ├── V2__create_instances_schema.sql                                │
│          └── V3__create_messaging_schema.sql                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.1.3. Key Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.kiteclass</groupId>
    <artifactId>kitehub-backend</artifactId>
    <version>1.0.0</version>
    <name>KiteHub Backend</name>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- Flyway Migration -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>

        <!-- Kubernetes Client (for Maintaining module) -->
        <dependency>
            <groupId>io.fabric8</groupId>
            <artifactId>kubernetes-client</artifactId>
            <version>6.9.2</version>
        </dependency>

        <!-- AWS SDK -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>2.21.0</version>
        </dependency>

        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>sqs</artifactId>
            <version>2.21.0</version>
        </dependency>

        <!-- MapStruct for DTO mapping -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>1.5.5.Final</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 2.1.4. ĐỀ XUẤT: Tách Message Module thành Node.js Service

**Lý do:**
- WebSocket/Socket.io native trong Node.js
- Real-time performance tốt hơn
- Event-driven architecture phù hợp

**Architecture điều chỉnh:**

```
KiteHub Backend
├── Java Spring Boot Monolith
│   ├── Sale Module
│   ├── Maintaining Module
│   └── AI Agent Module (hoặc Python microservice)
│
└── Node.js Message Service (Tách riêng)
    ├── WebSocket Server (Socket.io)
    ├── Chat Service
    ├── Notification Service
    └── Chatbot Integration
```

### 2.1.5. Message Service (Node.js/TypeScript)

```
message-service/
├── src/
│   ├── app.ts
│   ├── server.ts
│   ├── controllers/
│   │   ├── chat.controller.ts
│   │   └── notification.controller.ts
│   ├── services/
│   │   ├── chat.service.ts
│   │   ├── notification.service.ts
│   │   └── chatbot.service.ts
│   ├── gateways/
│   │   └── chat.gateway.ts          # Socket.io
│   ├── repositories/
│   │   ├── message.repository.ts
│   │   └── conversation.repository.ts
│   └── models/
│       ├── message.model.ts
│       └── conversation.model.ts
├── package.json
└── tsconfig.json
```

**package.json:**

```json
{
  "name": "message-service",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.2",
    "socket.io": "^4.7.2",
    "pg": "^8.11.3",
    "redis": "^4.6.10",
    "aws-sdk": "^2.1489.0",
    "joi": "^17.11.0",
    "winston": "^3.11.0"
  },
  "devDependencies": {
    "@types/express": "^4.17.21",
    "@types/node": "^20.10.0",
    "typescript": "^5.3.2"
  }
}
```

### 2.1.6. AI Agent Module (Python Microservice)

**Lý do:**
- ML/AI libraries (TensorFlow, PyTorch, Transformers)
- Image processing (Pillow, OpenCV)
- Integration với Replicate, OpenAI, Stability AI

```
ai-agent-service/
├── app/
│   ├── main.py
│   ├── api/
│   │   ├── routes/
│   │   │   ├── generation.py
│   │   │   └── assets.py
│   │   └── dependencies.py
│   ├── services/
│   │   ├── image_generation.py
│   │   ├── text_generation.py
│   │   ├── background_removal.py
│   │   └── asset_management.py
│   ├── models/
│   │   ├── generation_job.py
│   │   └── generated_asset.py
│   └── utils/
│       ├── image_processor.py
│       └── color_extractor.py
├── requirements.txt
└── Dockerfile
```

**requirements.txt:**

```txt
fastapi==0.104.1
uvicorn==0.24.0
sqlalchemy==2.0.23
psycopg2-binary==2.9.9
redis==5.0.1
pillow==10.1.0
replicate==0.22.0
openai==1.3.5
boto3==1.29.7
python-multipart==0.0.6
pydantic==2.5.0
```

### 2.1.7. Frontend Architecture (Next.js)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    KITEHUB FRONTEND (Next.js)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Mục đích: Public-facing website + Admin portal                             │
│                                                                              │
│  kitehub-frontend/                                                          │
│  ├── src/                                                                   │
│  │   ├── app/                              # App Router (Next.js 14)        │
│  │   │   ├── (public)/                     # Public routes                  │
│  │   │   │   ├── page.tsx                  # Landing page                   │
│  │   │   │   ├── pricing/page.tsx                                           │
│  │   │   │   ├── features/page.tsx                                          │
│  │   │   │   └── contact/page.tsx                                           │
│  │   │   │                                                                   │
│  │   │   ├── (auth)/                       # Auth routes                    │
│  │   │   │   ├── login/page.tsx                                             │
│  │   │   │   └── register/page.tsx                                          │
│  │   │   │                                                                   │
│  │   │   ├── admin/                        # Admin portal                   │
│  │   │   │   ├── dashboard/page.tsx                                         │
│  │   │   │   ├── instances/                                                 │
│  │   │   │   │   ├── page.tsx              # List instances                 │
│  │   │   │   │   └── [id]/page.tsx         # Instance detail                │
│  │   │   │   ├── orders/page.tsx                                            │
│  │   │   │   ├── monitoring/page.tsx                                        │
│  │   │   │   └── settings/page.tsx                                          │
│  │   │   │                                                                   │
│  │   │   └── api/                          # API routes (BFF pattern)       │
│  │   │       ├── orders/route.ts                                            │
│  │   │       └── instances/route.ts                                         │
│  │   │                                                                       │
│  │   ├── components/                                                        │
│  │   │   ├── public/                       # Public components              │
│  │   │   │   ├── Navbar.tsx                                                 │
│  │   │   │   ├── Hero.tsx                                                   │
│  │   │   │   └── PricingCard.tsx                                            │
│  │   │   │                                                                   │
│  │   │   └── admin/                        # Admin components               │
│  │   │       ├── Sidebar.tsx                                                │
│  │   │       ├── InstanceCard.tsx                                           │
│  │   │       └── MonitoringChart.tsx                                        │
│  │   │                                                                       │
│  │   ├── lib/                              # Utilities                      │
│  │   │   ├── api-client.ts                 # Axios wrapper                  │
│  │   │   └── utils.ts                                                       │
│  │   │                                                                       │
│  │   └── types/                            # TypeScript types               │
│  │       ├── order.ts                                                       │
│  │       └── instance.ts                                                    │
│  │                                                                           │
│  ├── public/                                                                │
│  │   ├── images/                                                            │
│  │   └── fonts/                                                             │
│  │                                                                           │
│  ├── package.json                                                           │
│  ├── tsconfig.json                                                          │
│  ├── tailwind.config.js                                                     │
│  └── next.config.js                                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

**package.json:**

```json
{
  "name": "kitehub-frontend",
  "version": "1.0.0",
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start"
  },
  "dependencies": {
    "next": "14.0.3",
    "react": "18.2.0",
    "react-dom": "18.2.0",
    "axios": "^1.6.2",
    "tailwindcss": "^3.3.5",
    "@tanstack/react-query": "^5.8.4",
    "recharts": "^2.10.3",
    "lucide-react": "^0.294.0",
    "zod": "^3.22.4",
    "react-hook-form": "^7.48.2"
  },
  "devDependencies": {
    "@types/node": "20.10.0",
    "@types/react": "18.2.42",
    "typescript": "5.3.2",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32"
  }
}
```

### 2.1.8. Database Schema (PostgreSQL)

```sql
-- KiteHub Database: kitehub_db

-- Schema: sales
CREATE SCHEMA sales;

CREATE TABLE sales.products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    package VARCHAR(50) NOT NULL, -- BASIC, STANDARD, PREMIUM
    price_monthly DECIMAL(10,2) NOT NULL,
    features JSONB,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE sales.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID REFERENCES sales.products(id),
    customer_email VARCHAR(255) NOT NULL,
    org_name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, PAID, PROVISIONING, ACTIVE, CANCELLED
    payment_method VARCHAR(50),
    total_amount DECIMAL(10,2),
    image_upload_id UUID, -- Reference to uploaded image for AI
    metadata JSONB, -- slogan, brand_colors, etc
    created_at TIMESTAMP DEFAULT NOW(),
    paid_at TIMESTAMP,
    activated_at TIMESTAMP
);

CREATE TABLE sales.transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES sales.orders(id),
    payment_gateway VARCHAR(50), -- VNPAY, STRIPE, MOMO
    transaction_id VARCHAR(255) UNIQUE,
    amount DECIMAL(10,2),
    status VARCHAR(50), -- PENDING, SUCCESS, FAILED
    gateway_response JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Schema: instances
CREATE SCHEMA instances;

CREATE TABLE instances.kiteclass_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    order_id UUID REFERENCES sales.orders(id),
    package VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL, -- PROVISIONING, ACTIVE, SUSPENDED, DELETED
    config JSONB, -- services enabled, quotas, branding
    namespace VARCHAR(100),
    eks_cluster VARCHAR(255),
    database_endpoint VARCHAR(255),
    redis_endpoint VARCHAR(255),
    frontend_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    activated_at TIMESTAMP,
    suspended_at TIMESTAMP
);

CREATE TABLE instances.instance_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID REFERENCES instances.kiteclass_instances(id),
    cpu_usage DECIMAL(5,2), -- percentage
    memory_usage DECIMAL(10,2), -- GB
    storage_usage DECIMAL(10,2), -- GB
    active_users INTEGER,
    total_classes INTEGER,
    recorded_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_metrics_instance_recorded ON instances.instance_metrics(instance_id, recorded_at DESC);

-- Schema: messaging (for Message Service - shared DB)
CREATE SCHEMA messaging;

CREATE TABLE messaging.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email VARCHAR(255),
    user_name VARCHAR(255),
    agent_id UUID,
    status VARCHAR(50), -- OPEN, CLOSED, RESOLVED
    created_at TIMESTAMP DEFAULT NOW(),
    closed_at TIMESTAMP
);

CREATE TABLE messaging.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES messaging.conversations(id),
    sender_type VARCHAR(50), -- USER, AGENT, BOT
    sender_id VARCHAR(255),
    content TEXT,
    attachments JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation ON messaging.messages(conversation_id, created_at DESC);

-- Schema: ai_assets
CREATE SCHEMA ai_assets;

CREATE TABLE ai_assets.generation_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID REFERENCES instances.kiteclass_instances(id),
    order_id UUID REFERENCES sales.orders(id),
    status VARCHAR(50) NOT NULL, -- QUEUED, PROCESSING, COMPLETED, FAILED
    input_data JSONB, -- original_image_url, org_name, slogan, etc
    output_data JSONB, -- generated asset URLs, copy, colors
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE ai_assets.generated_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES ai_assets.generation_jobs(id),
    instance_id UUID REFERENCES instances.kiteclass_instances(id),
    asset_type VARCHAR(50) NOT NULL, -- PROFILE_CIRCLE, HERO_BANNER, LOGO_PRIMARY, etc
    asset_url TEXT NOT NULL,
    prompt_used TEXT,
    metadata JSONB, -- dimensions, model_used, generation_params
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_assets_instance ON ai_assets.generated_assets(instance_id);
```

---

# PHẦN 3: KITECLASS INSTANCES - MICROSERVICES

## 3.1. Frontend Strategy cho KiteClass

### 3.1.1. Phân tích: Shared vs Separate Frontend

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    FRONTEND ARCHITECTURE ANALYSIS                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Option A: Shared Frontend (1 codebase cho tất cả instances)                │
│  ───────────────────────────────────────────────────────────────            │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────┐               │
│  │        kiteclass-frontend (Single Next.js app)           │               │
│  │  ┌────────────────────────────────────────────────────┐  │               │
│  │  │  Multi-tenant detection:                           │  │               │
│  │  │  • Read subdomain from URL (acme.kiteclass.com)    │  │               │
│  │  │  • Fetch instance config from API                  │  │               │
│  │  │  • Apply branding dynamically                      │  │               │
│  │  └────────────────────────────────────────────────────┘  │               │
│  └──────────────────────────────────────────────────────────┘               │
│                           │                                                  │
│          ┌────────────────┼────────────────┐                                │
│          │                │                │                                │
│          ▼                ▼                ▼                                │
│     acme.kiteclass.com   xyz.kiteclass.com   abc.kiteclass.com              │
│     (Same code)          (Same code)         (Same code)                    │
│                                                                              │
│  ✅ Ưu điểm:                                                                │
│     • Maintain 1 codebase → dễ update, fix bugs                             │
│     • Deploy 1 lần cho tất cả instances                                     │
│     • Consistent UX across all instances                                    │
│     • Reduce infrastructure cost                                            │
│                                                                              │
│  ❌ Nhược điểm:                                                             │
│     • Khó customize sâu cho từng instance                                   │
│     • Single point of failure                                               │
│     • Performance: Phải fetch config mỗi lần load                           │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  Option B: Separate Frontend (1 deployment per instance)                    │
│  ────────────────────────────────────────────────────────                   │
│                                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │   Instance   │    │   Instance   │    │   Instance   │                  │
│  │   ACME FE    │    │    XYZ FE    │    │    ABC FE    │                  │
│  │ (Customized) │    │ (Customized) │    │ (Customized) │                  │
│  └──────────────┘    └──────────────┘    └──────────────┘                  │
│                                                                              │
│  ✅ Ưu điểm:                                                                │
│     • Hoàn toàn customizable                                                │
│     • Isolated failures                                                     │
│     • Better performance (no config fetch)                                  │
│     • Có thể dùng different Next.js versions                                │
│                                                                              │
│  ❌ Nhược điểm:                                                             │
│     • Maintain N codebases → nightmare                                      │
│     • Update phải deploy N lần                                              │
│     • Higher infrastructure cost (N deployments)                            │
│     • Inconsistent UX                                                       │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  ĐỀ XUẤT: HYBRID APPROACH ⭐                                                 │
│  ──────────────────────────                                                 │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────┐               │
│  │  Shared Next.js Codebase with Theme System                │               │
│  │  ┌──────────────────────────────────────────────────┐    │               │
│  │  │  1 Next.js app deployed N times                  │    │               │
│  │  │  • Build-time config injection                   │    │               │
│  │  │  • Subdomain-based routing                       │    │               │
│  │  │  • Theme override system                         │    │               │
│  │  └──────────────────────────────────────────────────┘    │               │
│  └──────────────────────────────────────────────────────────┘               │
│                           │                                                  │
│          ┌────────────────┼────────────────┐                                │
│          │                │                │                                │
│          ▼                ▼                ▼                                │
│     Deployment #1    Deployment #2    Deployment #3                         │
│     (ACME theme)     (XYZ theme)      (ABC theme)                           │
│                                                                              │
│  ✅ Best of both worlds:                                                    │
│     • Single codebase (easy maintain)                                       │
│     • Isolated deployments (failure isolation)                              │
│     • Customizable per instance (via themes)                                │
│     • Good performance (build-time config)                                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.1.2. ĐỀ XUẤT: Shared Codebase + Per-Instance Deployment

**Kiến trúc:**

```
kiteclass-frontend/  (Single codebase)
├── src/
│   ├── app/
│   │   ├── (student)/              # Student portal
│   │   │   ├── dashboard/
│   │   │   ├── classes/
│   │   │   ├── assignments/
│   │   │   └── grades/
│   │   │
│   │   ├── (instructor)/           # Instructor portal
│   │   │   ├── dashboard/
│   │   │   ├── my-classes/
│   │   │   ├── students/
│   │   │   └── analytics/
│   │   │
│   │   └── (public)/               # Public pages
│   │       ├── page.tsx            # Landing
│   │       ├── courses/
│   │       └── about/
│   │
│   ├── components/
│   │   ├── shared/                 # Shared components
│   │   ├── student/
│   │   └── instructor/
│   │
│   ├── lib/
│   │   ├── theme.ts                # Theme system
│   │   └── config.ts               # Instance config loader
│   │
│   └── styles/
│       └── themes/                 # Theme CSS variables
│
├── config/                         # Build-time configs
│   ├── acme.json                   # ACME instance config
│   ├── xyz.json
│   └── default.json
│
└── scripts/
    └── build-instance.sh           # Build script per instance
```

**Build process:**

```bash
#!/bin/bash
# scripts/build-instance.sh

INSTANCE_ID=$1  # e.g., "acme"

# Load instance config
CONFIG_FILE="config/${INSTANCE_ID}.json"

# Inject config as environment variables
export NEXT_PUBLIC_INSTANCE_ID=$INSTANCE_ID
export NEXT_PUBLIC_ORG_NAME=$(jq -r '.org_name' $CONFIG_FILE)
export NEXT_PUBLIC_PRIMARY_COLOR=$(jq -r '.colors.primary' $CONFIG_FILE)
export NEXT_PUBLIC_HERO_BANNER=$(jq -r '.assets.hero_banner' $CONFIG_FILE)

# Build Next.js
npm run build

# Output: .next/ folder with instance-specific config baked in
```

**Deployment:**

```yaml
# Kubernetes Deployment per instance
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend-acme
  namespace: kiteclass-acme
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: frontend
        image: kiteclass/frontend:latest
        env:
        - name: INSTANCE_ID
          value: "acme"
        - name: API_BASE_URL
          value: "http://api-gateway.kiteclass-acme.svc.cluster.local"
```

## 3.2. Backend Services Technology Stack

### 3.2.1. Main Class Service

| Layer | Technology | Lý do |
|-------|------------|-------|
| **Backend** | Java Spring Boot 3.2+ | CRUD operations, business logic, JPA perfect fit |
| **Database** | PostgreSQL 16+ | Relational data (classes, lessons, schedules) |
| **Cache** | Redis | Session, frequently accessed class data |

**Project Structure:**

```
main-class-service/
├── src/main/java/com/kiteclass/mainclass/
│   ├── MainClassServiceApplication.java
│   ├── controller/
│   │   ├── ClassController.java
│   │   ├── LessonController.java
│   │   └── ScheduleController.java
│   ├── service/
│   │   ├── ClassService.java
│   │   ├── LessonService.java
│   │   └── ScheduleService.java
│   ├── repository/
│   │   ├── ClassRepository.java
│   │   ├── LessonRepository.java
│   │   └── ScheduleRepository.java
│   ├── entity/
│   │   ├── Class.java
│   │   ├── Lesson.java
│   │   ├── Schedule.java
│   │   └── Material.java
│   └── dto/
│       ├── ClassRequest.java
│       └── ClassResponse.java
└── pom.xml
```

**Key Dependencies:**

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <!-- Full-text search -->
    <dependency>
        <groupId>org.hibernate.search</groupId>
        <artifactId>hibernate-search-mapper-orm</artifactId>
        <version>7.0.0.Final</version>
    </dependency>
</dependencies>
```

**Database Schema:**

```sql
CREATE SCHEMA main_class;

CREATE TABLE main_class.classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    instructor_id UUID NOT NULL,
    category VARCHAR(100),
    status VARCHAR(50), -- DRAFT, PUBLISHED, ARCHIVED
    max_students INTEGER,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE main_class.lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID REFERENCES main_class.classes(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content TEXT,
    lesson_order INTEGER,
    duration_minutes INTEGER,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE main_class.schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID REFERENCES main_class.classes(id) ON DELETE CASCADE,
    lesson_id UUID REFERENCES main_class.lessons(id),
    scheduled_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER,
    location VARCHAR(255), -- ONLINE, ROOM_A, etc
    meeting_url TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE main_class.materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID REFERENCES main_class.lessons(id) ON DELETE CASCADE,
    title VARCHAR(255),
    file_url TEXT NOT NULL,
    file_type VARCHAR(50), -- PDF, DOCX, VIDEO, etc
    file_size BIGINT,
    uploaded_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_classes_instance ON main_class.classes(instance_id);
CREATE INDEX idx_classes_instructor ON main_class.classes(instructor_id);
CREATE INDEX idx_lessons_class ON main_class.lessons(class_id);
CREATE INDEX idx_schedules_class ON main_class.schedules(class_id);
```

### 3.2.2. User Service

| Layer | Technology | Lý do |
|-------|------------|-------|
| **Backend** | Java Spring Boot 3.2+ | Security, authentication, Spring Security native |
| **Database** | PostgreSQL 16+ | User data, roles, permissions |
| **Cache** | Redis | Session storage, JWT blacklist |

**Project Structure:**

```
user-service/
├── src/main/java/com/kiteclass/user/
│   ├── UserServiceApplication.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   └── RoleController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   └── JwtService.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── RoleRepository.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Role.java
│   │   └── Permission.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityConfig.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       └── UserResponse.java
└── pom.xml
```

**Key Dependencies:**

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>
</dependencies>
```

**Database Schema:**

```sql
CREATE SCHEMA users;

CREATE TABLE users.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255), -- NULL for OAuth users
    full_name VARCHAR(255),
    avatar_url TEXT,
    phone VARCHAR(20),
    status VARCHAR(50), -- ACTIVE, SUSPENDED, DELETED
    email_verified BOOLEAN DEFAULT false,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE users.roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL, -- ADMIN, INSTRUCTOR, STUDENT
    description TEXT,
    permissions JSONB, -- Array of permission strings
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE users.user_roles (
    user_id UUID REFERENCES users.users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES users.roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE users.oauth_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users.users(id) ON DELETE CASCADE,
    provider VARCHAR(50), -- GOOGLE, FACEBOOK
    provider_user_id VARCHAR(255),
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(provider, provider_user_id)
);

CREATE INDEX idx_users_instance ON users.users(instance_id);
CREATE INDEX idx_users_email ON users.users(email);
CREATE INDEX idx_roles_instance ON users.roles(instance_id);
```

### 3.2.3. CMC Service (Class Management Core)

| Layer | Technology | Lý do |
|-------|------------|-------|
| **Backend** | Java Spring Boot 3.2+ | Complex business logic, grading calculations |
| **Database** | PostgreSQL 16+ | Transactional data (grades, attendance) |
| **Cache** | Redis | Leaderboards, statistics |

**Database Schema:**

```sql
CREATE SCHEMA cmc;

CREATE TABLE cmc.attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL,
    student_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    status VARCHAR(50), -- PRESENT, ABSENT, LATE, EXCUSED
    checked_in_at TIMESTAMP,
    location_lat DECIMAL(10, 8),
    location_lng DECIMAL(11, 8),
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE cmc.assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL,
    lesson_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    max_score DECIMAL(5,2),
    due_date TIMESTAMP,
    allow_late_submission BOOLEAN DEFAULT false,
    late_penalty_percent DECIMAL(5,2),
    created_by UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE cmc.submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID REFERENCES cmc.assignments(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    submission_url TEXT,
    content TEXT,
    submitted_at TIMESTAMP DEFAULT NOW(),
    is_late BOOLEAN DEFAULT false,
    grade DECIMAL(5,2),
    feedback TEXT,
    graded_by UUID,
    graded_at TIMESTAMP
);

CREATE TABLE cmc.grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL,
    student_id UUID NOT NULL,
    assignment_id UUID REFERENCES cmc.assignments(id),
    grade_type VARCHAR(50), -- ASSIGNMENT, MIDTERM, FINAL, PARTICIPATION
    score DECIMAL(5,2) NOT NULL,
    max_score DECIMAL(5,2) NOT NULL,
    weight DECIMAL(5,2), -- For weighted average
    notes TEXT,
    graded_by UUID NOT NULL,
    graded_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_attendance_class_student ON cmc.attendance(class_id, student_id);
CREATE INDEX idx_submissions_assignment ON cmc.submissions(assignment_id);
CREATE INDEX idx_grades_class_student ON cmc.grades(class_id, student_id);
```

### 3.2.4. Video Learning Service

| Layer | Technology | Lý do |
|-------|------------|-------|
| **Backend** | Java Spring Boot 3.2+ | Video metadata, progress tracking |
| **Processing** | FFmpeg (separate worker) | Video transcoding |
| **Database** | PostgreSQL 16+ | Video metadata, watch progress |
| **Storage** | AWS S3 | Video files storage |
| **CDN** | AWS CloudFront | Video streaming delivery |

**ĐỀ XUẤT: Tách Video Processing Worker**

```
Video Learning Service Architecture:
├── Java Spring Boot API
│   ├── Video CRUD
│   ├── Progress tracking
│   └── Analytics
│
└── Python/Go Video Processing Worker
    ├── FFmpeg wrapper
    ├── Transcoding jobs
    ├── Thumbnail generation
    └── HLS packaging
```

**Database Schema:**

```sql
CREATE SCHEMA video_learning;

CREATE TABLE video_learning.videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    original_file_url TEXT NOT NULL,
    original_file_size BIGINT,
    duration_seconds INTEGER,
    thumbnail_url TEXT,
    processing_status VARCHAR(50), -- UPLOADING, PROCESSING, READY, FAILED
    created_by UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE video_learning.video_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID REFERENCES video_learning.videos(id) ON DELETE CASCADE,
    quality VARCHAR(50), -- 360p, 720p, 1080p
    file_url TEXT NOT NULL,
    file_size BIGINT,
    bitrate_kbps INTEGER,
    codec VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE video_learning.watch_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID REFERENCES video_learning.videos(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    current_position_seconds INTEGER DEFAULT 0,
    completed BOOLEAN DEFAULT false,
    last_watched_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (video_id, student_id)
);

CREATE INDEX idx_videos_lesson ON video_learning.videos(lesson_id);
CREATE INDEX idx_progress_student ON video_learning.watch_progress(student_id);
```

### 3.2.5. Streaming Service

| Layer | Technology | Lý do |
|-------|------------|-------|
| **Backend** | **Node.js + TypeScript** | WebRTC, Socket.io native support |
| **Real-time** | Socket.io | WebSocket for signaling |
| **WebRTC** | mediasoup / Jitsi | SFU for video conferencing |
| **Database** | PostgreSQL 16+ | Session metadata |
| **Recording** | FFmpeg | Record streams to video |

**⚠️ EXCEPTION: Node.js thay vì Java**

**Lý do:**
- WebRTC signaling cần real-time performance
- Socket.io ecosystem mature trong Node.js
- mediasoup (SFU) là Node.js library
- Event-driven architecture perfect fit

**Project Structure:**

```
streaming-service/
├── src/
│   ├── app.ts
│   ├── server.ts
│   ├── controllers/
│   │   ├── stream.controller.ts
│   │   └── recording.controller.ts
│   ├── services/
│   │   ├── mediasoup.service.ts
│   │   ├── signaling.service.ts
│   │   └── recording.service.ts
│   ├── gateways/
│   │   └── webrtc.gateway.ts        # Socket.io
│   ├── repositories/
│   │   └── stream.repository.ts
│   └── models/
│       └── stream.model.ts
├── package.json
└── tsconfig.json
```

**package.json:**

```json
{
  "name": "streaming-service",
  "dependencies": {
    "express": "^4.18.2",
    "socket.io": "^4.7.2",
    "mediasoup": "^3.13.0",
    "mediasoup-client": "^3.7.0",
    "pg": "^8.11.3",
    "redis": "^4.6.10",
    "fluent-ffmpeg": "^2.1.2",
    "aws-sdk": "^2.1489.0"
  }
}
```

**Database Schema:**

```sql
CREATE SCHEMA streaming;

CREATE TABLE streaming.live_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    title VARCHAR(255),
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    status VARCHAR(50), -- SCHEDULED, LIVE, ENDED
    max_participants INTEGER,
    recording_enabled BOOLEAN DEFAULT false,
    recording_url TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE streaming.participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES streaming.live_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(50), -- HOST, PARTICIPANT
    joined_at TIMESTAMP DEFAULT NOW(),
    left_at TIMESTAMP,
    connection_quality VARCHAR(50)
);

CREATE INDEX idx_sessions_class ON streaming.live_sessions(class_id);
CREATE INDEX idx_participants_session ON streaming.participants(session_id);
```

### 3.2.6. Forum Service

| Layer | Technology | Lý do |
|-------|------------|-------|
| **Backend** | Java Spring Boot 3.2+ | CRUD, moderation logic |
| **Database** | PostgreSQL 16+ | Threaded discussions, full-text search |
| **Search** | PostgreSQL Full-Text | Built-in pg_trgm for search |

**Database Schema:**

```sql
CREATE SCHEMA forum;

CREATE TABLE forum.topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL,
    category_id UUID,
    author_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_pinned BOOLEAN DEFAULT false,
    is_resolved BOOLEAN DEFAULT false,
    view_count INTEGER DEFAULT 0,
    vote_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Full-text search index
CREATE INDEX idx_topics_search ON forum.topics
USING GIN (to_tsvector('english', title || ' ' || content));

CREATE TABLE forum.replies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID REFERENCES forum.topics(id) ON DELETE CASCADE,
    parent_reply_id UUID REFERENCES forum.replies(id), -- For nested replies
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    is_answer BOOLEAN DEFAULT false, -- Marked by instructor
    vote_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE forum.votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    votable_type VARCHAR(50), -- TOPIC, REPLY
    votable_id UUID NOT NULL,
    vote_type VARCHAR(10), -- UPVOTE, DOWNVOTE
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, votable_type, votable_id)
);

CREATE INDEX idx_topics_class ON forum.topics(class_id);
CREATE INDEX idx_replies_topic ON forum.replies(topic_id);
```

---

# PHẦN 4: AWS DEPLOYMENT ARCHITECTURE

## 4.1. AWS Services Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AWS SERVICES USED                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Compute:                                                                   │
│  ├── EKS (Elastic Kubernetes Service)    - Container orchestration          │
│  ├── EC2 (for EKS worker nodes)          - Compute instances                │
│  └── Fargate (optional)                  - Serverless containers            │
│                                                                              │
│  Database:                                                                  │
│  ├── RDS PostgreSQL                      - Managed database                 │
│  └── ElastiCache Redis                   - Managed cache                    │
│                                                                              │
│  Storage:                                                                   │
│  ├── S3                                  - Object storage                   │
│  └── EFS (optional)                      - Shared file system               │
│                                                                              │
│  Networking:                                                                │
│  ├── VPC                                 - Virtual private cloud            │
│  ├── ALB (Application Load Balancer)    - Load balancing                   │
│  ├── Route 53                            - DNS management                   │
│  └── CloudFront                          - CDN                              │
│                                                                              │
│  Messaging & Queue:                                                         │
│  └── SQS                                 - Message queue                    │
│                                                                              │
│  Monitoring:                                                                │
│  ├── CloudWatch                          - Logs & metrics                   │
│  └── X-Ray (optional)                    - Distributed tracing              │
│                                                                              │
│  Security:                                                                  │
│  ├── IAM                                 - Access management                │
│  ├── Secrets Manager                     - Secrets storage                  │
│  └── ACM (Certificate Manager)           - SSL certificates                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4.2. AWS Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      AWS DEPLOYMENT ARCHITECTURE                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                         INTERNET                                            │
│                            │                                                │
│                            ▼                                                │
│                    ┌───────────────┐                                        │
│                    │   Route 53    │                                        │
│                    │  (DNS)        │                                        │
│                    └───────┬───────┘                                        │
│                            │                                                │
│                            ▼                                                │
│                    ┌───────────────┐                                        │
│                    │  CloudFront   │                                        │
│                    │  (CDN)        │                                        │
│                    └───────┬───────┘                                        │
│                            │                                                │
│                            ▼                                                │
│  ┌─────────────────────────────────────────────────────────────────┐        │
│  │                         AWS VPC                                  │        │
│  │                    (10.0.0.0/16)                                 │        │
│  │                                                                  │        │
│  │  ┌─────────────────────────────────────────────────────────┐    │        │
│  │  │               PUBLIC SUBNETS                             │    │        │
│  │  │  ┌────────────────────────────────────────────────┐     │    │        │
│  │  │  │  Application Load Balancer (ALB)               │     │    │        │
│  │  │  │  • SSL Termination (ACM)                       │     │    │        │
│  │  │  │  • Path-based routing                          │     │    │        │
│  │  │  └────────────────┬───────────────────────────────┘     │    │        │
│  │  └───────────────────┼───────────────────────────────────  │    │        │
│  │                      │                                      │    │        │
│  │  ┌───────────────────┼───────────────────────────────────┐ │    │        │
│  │  │         PRIVATE SUBNETS (EKS)                         │ │    │        │
│  │  │  ┌────────────────▼───────────────────────────────┐   │ │    │        │
│  │  │  │          EKS CLUSTER                            │   │ │    │        │
│  │  │  │  ┌──────────────────────────────────────────┐  │   │ │    │        │
│  │  │  │  │  Namespace: kitehub                      │  │   │ │    │        │
│  │  │  │  │  ┌────────────────────────────────────┐  │  │   │ │    │        │
│  │  │  │  │  │ Java Spring Boot Monolith          │  │  │   │ │    │        │
│  │  │  │  │  │ Pods: 3 replicas                   │  │  │   │ │    │        │
│  │  │  │  │  └────────────────────────────────────┘  │  │   │ │    │        │
│  │  │  │  │  ┌────────────────────────────────────┐  │  │   │ │    │        │
│  │  │  │  │  │ Node.js Message Service            │  │  │   │ │    │        │
│  │  │  │  │  │ Pods: 3 replicas                   │  │  │   │ │    │        │
│  │  │  │  │  └────────────────────────────────────┘  │  │   │ │    │        │
│  │  │  │  │  ┌────────────────────────────────────┐  │  │   │ │    │        │
│  │  │  │  │  │ Python AI Agent Service            │  │  │   │ │    │        │
│  │  │  │  │  │ Pods: 2 replicas                   │  │  │   │ │    │        │
│  │  │  │  │  └────────────────────────────────────┘  │  │   │ │    │        │
│  │  │  │  └──────────────────────────────────────────┘  │   │ │    │        │
│  │  │  │  ┌──────────────────────────────────────────┐  │   │ │    │        │
│  │  │  │  │  Namespace: kiteclass-acme               │  │   │ │    │        │
│  │  │  │  │  ┌────────────────────────────────────┐  │  │   │ │    │        │
│  │  │  │  │  │ Main Class Service (Java)          │  │  │   │ │    │        │
│  │  │  │  │  │ User Service (Java)                │  │  │   │ │    │        │
│  │  │  │  │  │ CMC Service (Java)                 │  │  │   │ │    │        │
│  │  │  │  │  │ Video Service (Java)               │  │  │   │ │    │        │
│  │  │  │  │  │ Streaming Service (Node.js)        │  │  │   │ │    │        │
│  │  │  │  │  │ Forum Service (Java)               │  │  │   │ │    │        │
│  │  │  │  │  │ Frontend (Next.js)                 │  │  │   │ │    │        │
│  │  │  │  │  └────────────────────────────────────┘  │  │   │ │    │        │
│  │  │  │  └──────────────────────────────────────────┘  │   │ │    │        │
│  │  │  │  │  ... more namespaces for other instances   │   │ │    │        │
│  │  │  └────┴──────────────────────────────────────────┴───┘ │    │        │
│  │  └───────────────────────────────────────────────────────  │    │        │
│  │                      │                                      │    │        │
│  │  ┌───────────────────┼───────────────────────────────────┐ │    │        │
│  │  │         PRIVATE SUBNETS (DATA)                        │ │    │        │
│  │  │  ┌────────────────▼───────────────────────────────┐   │ │    │        │
│  │  │  │  RDS PostgreSQL (Multi-AZ)                     │   │ │    │        │
│  │  │  │  ┌──────────────────────────────────────────┐  │   │ │    │        │
│  │  │  │  │ Instance: kitehub-db                     │  │   │ │    │        │
│  │  │  │  │ Instance: kiteclass-acme-db              │  │   │ │    │        │
│  │  │  │  │ Instance: kiteclass-xyz-db               │  │   │ │    │        │
│  │  │  │  │ ... (1 DB per KiteClass instance)        │  │   │ │    │        │
│  │  │  │  └──────────────────────────────────────────┘  │   │ │    │        │
│  │  │  └────────────────────────────────────────────────┘   │ │    │        │
│  │  │  ┌────────────────────────────────────────────────┐   │ │    │        │
│  │  │  │  ElastiCache Redis (Cluster Mode)              │   │ │    │        │
│  │  │  │  ┌──────────────────────────────────────────┐  │   │ │    │        │
│  │  │  │  │ Shared Redis cluster for all instances   │  │   │ │    │        │
│  │  │  │  │ • Namespace isolation by key prefix      │  │   │ │    │        │
│  │  │  │  └──────────────────────────────────────────┘  │   │ │    │        │
│  │  │  └────────────────────────────────────────────────┘   │ │    │        │
│  │  └─────────────────────────────────────────────────────  │    │        │
│  └──────────────────────────────────────────────────────────┘    │        │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐        │
│  │                    EXTERNAL SERVICES                             │        │
│  │  ┌─────────────────────────────────────────────────────────┐    │        │
│  │  │  S3 Buckets                                              │    │        │
│  │  │  • kiteclass-assets (user uploads)                       │    │        │
│  │  │  │• kiteclass-videos (video files)                        │    │        │
│  │  │  • kiteclass-ai-generated (AI assets)                    │    │        │
│  │  └─────────────────────────────────────────────────────────┘    │        │
│  │  ┌─────────────────────────────────────────────────────────┐    │        │
│  │  │  SQS Queues                                              │    │        │
│  │  │  • ai-generation-queue                                   │    │        │
│  │  │  • video-processing-queue                                │    │        │
│  │  │  • notification-queue                                    │    │        │
│  │  └─────────────────────────────────────────────────────────┘    │        │
│  └─────────────────────────────────────────────────────────────────┘        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4.3. EKS Cluster Configuration

### 4.3.1. Cluster Specification

```yaml
# eks-cluster-config.yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: kiteclass-cluster
  region: ap-southeast-1  # Singapore
  version: "1.28"

vpc:
  cidr: 10.0.0.0/16
  nat:
    gateway: HighlyAvailable  # NAT Gateway in each AZ

iam:
  withOIDC: true

managedNodeGroups:
  - name: general-purpose
    instanceType: t3.large
    minSize: 3
    maxSize: 10
    desiredCapacity: 5
    volumeSize: 100
    labels:
      role: general
    tags:
      nodegroup-role: general
    iam:
      attachPolicyARNs:
        - arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy
        - arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy
        - arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
        - arn:aws:iam::aws:policy/AmazonS3FullAccess

  - name: streaming-optimized
    instanceType: c5.2xlarge  # CPU optimized for WebRTC
    minSize: 1
    maxSize: 5
    desiredCapacity: 2
    labels:
      role: streaming
    taints:
      - key: workload
        value: streaming
        effect: NoSchedule

addons:
  - name: vpc-cni
  - name: coredns
  - name: kube-proxy
  - name: aws-ebs-csi-driver  # For persistent volumes
```

### 4.3.2. Deploy EKS Cluster

```bash
# Create cluster
eksctl create cluster -f eks-cluster-config.yaml

# Configure kubectl
aws eks update-kubeconfig --region ap-southeast-1 --name kiteclass-cluster

# Install AWS Load Balancer Controller
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=kiteclass-cluster
```

## 4.4. RDS PostgreSQL Configuration

### 4.4.1. Database Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      DATABASE PROVISIONING STRATEGY                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Option A: Shared RDS with Multiple Databases                               │
│  ───────────────────────────────────────────                                │
│                                                                              │
│  ┌──────────────────────────────────────────────┐                           │
│  │    RDS PostgreSQL (db.r6g.2xlarge)           │                           │
│  │  ┌────────────────────────────────────────┐  │                           │
│  │  │ Database: kitehub_db                   │  │                           │
│  │  │ Database: kiteclass_acme_db            │  │                           │
│  │  │ Database: kiteclass_xyz_db             │  │                           │
│  │  │ ... (up to ~100 databases)             │  │                           │
│  │  └────────────────────────────────────────┘  │                           │
│  └──────────────────────────────────────────────┘                           │
│                                                                              │
│  ✅ Ưu điểm: Cost-effective, easy management                                │
│  ❌ Nhược điểm: Single point of failure, harder to scale individually       │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  Option B: Separate RDS per Instance (ĐỀ XUẤT)                              │
│  ──────────────────────────────────────────────                             │
│                                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                      │
│  │ RDS kitehub  │  │ RDS acme     │  │ RDS xyz      │                      │
│  │ (shared)     │  │ (isolated)   │  │ (isolated)   │                      │
│  │ db.t4g.medium│  │ db.t4g.small │  │ db.t4g.small │                      │
│  └──────────────┘  └──────────────┘  └──────────────┘                      │
│                                                                              │
│  ✅ Ưu điểm: Perfect isolation, independent scaling, better security        │
│  ⚠️ Chi phí: Higher cost (~$30/month per RDS instance)                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

**ĐỀ XUẤT: Hybrid Approach**
- **KiteHub**: 1 RDS instance (db.t4g.medium)
- **KiteClass**: 1 shared RDS cluster với Aurora PostgreSQL (Serverless v2)
  - Auto-scaling based on load
  - 1 database per instance
  - Cost-effective cho small instances

### 4.4.2. Aurora PostgreSQL Serverless Configuration

```bash
# Create Aurora Serverless cluster
aws rds create-db-cluster \
  --db-cluster-identifier kiteclass-aurora-cluster \
  --engine aurora-postgresql \
  --engine-version 15.4 \
  --engine-mode provisioned \
  --master-username admin \
  --master-user-password <secret> \
  --db-subnet-group-name kiteclass-db-subnet-group \
  --vpc-security-group-ids sg-xxxxx \
  --serverlessv2-scaling-configuration MinCapacity=0.5,MaxCapacity=4 \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "Mon:04:00-Mon:05:00"

# Create serverless instances
aws rds create-db-instance \
  --db-instance-identifier kiteclass-aurora-instance-1 \
  --db-instance-class db.serverless \
  --engine aurora-postgresql \
  --db-cluster-identifier kiteclass-aurora-cluster
```

**Cost Estimate:**
- Aurora Serverless v2: $0.12/ACU-hour
- Minimum: 0.5 ACU = $0.06/hour = $43/month
- Scale up to 4 ACU during peak = $0.48/hour

## 4.5. ElastiCache Redis Configuration

```bash
# Create Redis cluster
aws elasticache create-replication-group \
  --replication-group-id kiteclass-redis \
  --replication-group-description "KiteClass Redis Cluster" \
  --engine redis \
  --cache-node-type cache.t4g.medium \
  --num-cache-clusters 2 \
  --automatic-failover-enabled \
  --multi-az-enabled \
  --cache-subnet-group-name kiteclass-cache-subnet-group \
  --security-group-ids sg-xxxxx \
  --snapshot-retention-limit 5 \
  --snapshot-window "02:00-03:00"
```

**Configuration:**
- Instance: cache.t4g.medium (3.09 GiB memory)
- Replicas: 1 (Multi-AZ)
- Cost: ~$50/month

## 4.6. S3 Buckets Configuration

```bash
# Create S3 buckets
aws s3 mb s3://kiteclass-assets --region ap-southeast-1
aws s3 mb s3://kiteclass-videos --region ap-southeast-1
aws s3 mb s3://kiteclass-ai-generated --region ap-southeast-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket kiteclass-assets \
  --versioning-configuration Status=Enabled

# Configure lifecycle policies
aws s3api put-bucket-lifecycle-configuration \
  --bucket kiteclass-videos \
  --lifecycle-configuration file://video-lifecycle.json
```

**video-lifecycle.json:**

```json
{
  "Rules": [
    {
      "Id": "Archive old videos",
      "Status": "Enabled",
      "Transitions": [
        {
          "Days": 90,
          "StorageClass": "INTELLIGENT_TIERING"
        }
      ]
    }
  ]
}
```

## 4.7. CloudFront CDN Configuration

```bash
# Create CloudFront distribution
aws cloudfront create-distribution --distribution-config file://cdn-config.json
```

**cdn-config.json:**

```json
{
  "CallerReference": "kiteclass-cdn-001",
  "Origins": {
    "Items": [
      {
        "Id": "S3-kiteclass-assets",
        "DomainName": "kiteclass-assets.s3.ap-southeast-1.amazonaws.com",
        "S3OriginConfig": {
          "OriginAccessIdentity": ""
        }
      },
      {
        "Id": "S3-kiteclass-videos",
        "DomainName": "kiteclass-videos.s3.ap-southeast-1.amazonaws.com",
        "S3OriginConfig": {
          "OriginAccessIdentity": ""
        }
      }
    ]
  },
  "DefaultCacheBehavior": {
    "TargetOriginId": "S3-kiteclass-assets",
    "ViewerProtocolPolicy": "redirect-to-https",
    "AllowedMethods": {
      "Items": ["GET", "HEAD", "OPTIONS"],
      "CachedMethods": {
        "Items": ["GET", "HEAD"]
      }
    },
    "Compress": true,
    "MinTTL": 0,
    "DefaultTTL": 86400,
    "MaxTTL": 31536000
  },
  "Enabled": true,
  "PriceClass": "PriceClass_All"
}
```

---

# PHẦN 5: DEPLOYMENT WORKFLOWS

## 5.1. CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/deploy-java-service.yml
name: Deploy Java Service

on:
  push:
    branches: [main]
    paths:
      - 'services/main-class-service/**'

env:
  AWS_REGION: ap-southeast-1
  ECR_REPOSITORY: kiteclass/main-class-service
  EKS_CLUSTER: kiteclass-cluster

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Build with Maven
        run: |
          cd services/main-class-service
          mvn clean package -DskipTests

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build, tag, and push image to ECR
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          cd services/main-class-service
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG

      - name: Update kubeconfig
        run: |
          aws eks update-kubeconfig --region $AWS_REGION --name $EKS_CLUSTER

      - name: Deploy to EKS
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          kubectl set image deployment/main-class-service \
            main-class-service=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
            -n kiteclass-acme
```

## 5.2. Dockerfile Examples

### 5.2.1. Java Spring Boot Dockerfile

```dockerfile
# Dockerfile for Java services
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar file
COPY target/*.jar app.jar

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.2.2. Node.js Dockerfile

```dockerfile
# Dockerfile for Node.js services
FROM node:20-alpine

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci --only=production

# Copy source
COPY . .

# Build TypeScript
RUN npm run build

# Non-root user
USER node

EXPOSE 3000

CMD ["node", "dist/server.js"]
```

---

# PHẦN 6: COST ESTIMATION

## 6.1. Monthly AWS Costs (100 KiteClass instances)

| Service | Configuration | Quantity | Monthly Cost |
|---------|---------------|----------|--------------|
| **EKS Cluster** | Control plane | 1 | $73 |
| **EC2 (EKS Nodes)** | t3.large (5 nodes) | 5 | $380 |
| **Aurora PostgreSQL** | Serverless v2 (0.5-4 ACU) | 1 cluster | $200 |
| **ElastiCache Redis** | cache.t4g.medium | 1 cluster | $50 |
| **ALB** | Application Load Balancer | 1 | $25 |
| **S3 Storage** | Standard (500 GB) | - | $12 |
| **S3 Requests** | GET/PUT requests | - | $5 |
| **CloudFront** | Data transfer (1 TB) | - | $85 |
| **Route 53** | Hosted zones (10) | 10 | $5 |
| **Secrets Manager** | Secrets (20) | 20 | $8 |
| **CloudWatch Logs** | Logs (50 GB) | - | $25 |
| **Data Transfer** | Egress (500 GB) | - | $45 |
| **TOTAL** | | | **~$913/month** |

**Per Instance Cost:** $913 / 100 = **$9.13/month**

**Revenue per instance (BASIC):** $99/month

**Gross Margin:** ($99 - $9.13) / $99 = **90.8%**

---

# PHẦN 7: CONCLUSION

## 7.1. Technology Stack Summary

| Component | Technology | Rationale |
|-----------|------------|-----------|
| **KiteHub Backend** | Java Spring Boot 3.2+ | Enterprise-grade, mature, team expertise |
| **Message Service** | Node.js + Socket.io | Real-time performance, WebSocket native |
| **AI Agent** | Python + FastAPI | ML libraries, AI service integrations |
| **KiteClass Services (Most)** | Java Spring Boot | Consistency, shared libraries |
| **Streaming Service** | Node.js + mediasoup | WebRTC performance critical |
| **Frontend** | Next.js 14 | SSR/SSG, great DX, SEO |
| **Database** | PostgreSQL 16+ (Aurora) | ACID, reliability, JSON support |
| **Cache** | Redis 7+ | Fast, versatile |
| **Container Orchestration** | AWS EKS | Managed Kubernetes, scalable |
| **Storage** | AWS S3 | Cheap, reliable |
| **CDN** | AWS CloudFront | Global distribution |

## 7.2. Key Decisions

1. **Java Spring Boot as default backend**: Enterprise-ready, team can maintain easily
2. **Node.js for real-time services**: Better performance for WebSocket/WebRTC
3. **Python for AI**: Best ecosystem for ML/AI workloads
4. **PostgreSQL over NoSQL**: Educational data is relational, ACID important
5. **Shared frontend codebase**: Easier maintenance, deploy per instance
6. **AWS EKS**: Managed Kubernetes, auto-scaling, cost-effective
7. **Aurora Serverless**: Auto-scaling database, pay for what you use

---

**Tài liệu được tạo bởi:** KiteClass Development Team
**Ngày cập nhật:** 16/12/2025
**Phiên bản:** 2.0
