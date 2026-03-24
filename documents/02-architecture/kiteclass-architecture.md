# KiteClass Architecture

## Overview

KiteClass is a multi-tenant education platform where each tenant represents a school or institute. Tenants are provisioned and managed by KiteHub (SaaS lifecycle), while KiteClass handles all education business logic.

Tenants access the platform via subdomains: `{tenant}.kiteclass.com`.

## Services

| Service | Tech Stack | Port | Responsibility |
|---------|-----------|------|----------------|
| **kiteclass-gateway** | Spring WebFlux | 8080 | Authentication, JWT issuance/validation, routing, Flyway DB migrations, user management |
| **kiteclass-core** | Spring Boot | 8081 | All business logic modules (see Module List below) |
| **kiteclass-frontend** | Next.js | 3000 | Student, teacher, admin, and owner UI |

## Multi-Tenant Isolation

- **Strategy:** Shared database, tenant column isolation.
- Every entity includes a `tenantId` (UUID) column.
- Tenant is resolved from the JWT token or subdomain on each request.
- Queries are scoped by `tenantId` — no cross-tenant data access.

## Infrastructure

All infrastructure uses the `kite-` prefix (shared with KiteHub).

| Component | Usage |
|-----------|-------|
| **PostgreSQL** (`kite-postgres`) | Shared DB with tenant column isolation. Gateway owns migrations (Flyway). |
| **Redis** (`kite-redis`) | Caching, session data |
| **RabbitMQ** (`kite-rabbitmq`) | Async event messaging between modules |
| **MinIO** (`kite-minio`) | File/object storage (assignments, profile images, etc.) |

## Module List

All business modules reside in `kiteclass-core`:

| Module | Description |
|--------|-------------|
| **student** | Student profiles and management |
| **teacher** | Teacher profiles and management |
| **course** | Course catalog and configuration |
| **clazz** | Class scheduling and management |
| **enrollment** | Student enrollment in classes |
| **attendance** | Attendance tracking |
| **grade** | Grade management and reporting |
| **assignment** | Assignment creation, submission, grading |
| **payment** | Payment processing |
| **invoice** | Invoice generation and management |
| **gamification** | Points, achievements, student engagement |
| **lms** | Learning modules, lessons, progress tracking |
| **marketing** | Contact messages, leads, landing pages |
| **settings** | Branding, user preferences, tenant config |
| **storage** | File upload/download via MinIO |

Gateway modules:

| Module | Description |
|--------|-------------|
| **auth** | Login, JWT issuance, refresh tokens |
| **user** | User CRUD, role assignment |

## Authentication & Authorization

- **Mechanism:** JWT with refresh token rotation.
- **Roles:** `OWNER`, `ADMIN`, `TEACHER`, `STUDENT`
- Gateway issues and validates JWTs. Core trusts the gateway's internal requests.
- Role-based access control is enforced at the controller level.

## Internal Communication

Gateway and Core communicate via synchronous REST calls secured with HMAC authentication.

```
Gateway ──[REST + HMAC]──> Core (port 8081)
```

- **Header:** `X-Internal-Request` — contains the HMAC signature.
- Gateway signs requests using a shared secret; Core validates the signature via `InternalRequestFilter`.
- This prevents direct external access to Core endpoints.

## Request Flow

```
Client
  │
  ▼
kiteclass-frontend (Next.js :3000)
  │ API calls
  ▼
kiteclass-gateway (WebFlux :8080)
  │ JWT validation, tenant resolution
  │ HMAC-signed internal request
  ▼
kiteclass-core (Spring Boot :8081)
  │ Business logic execution
  ▼
PostgreSQL / Redis / RabbitMQ / MinIO
```

## Async Events

Modules communicate asynchronously via RabbitMQ for cross-cutting concerns:

- Payment events trigger invoice generation
- Enrollment events notify the LMS module
- Assignment events feed into gamification scoring

## Deployment

- **Docker Compose:** `kiteclass/docker-compose.dev.yml` (development), `kiteclass/docker-compose.standalone.yml` (standalone)
- **Scripts:** `kiteclass/scripts/` — use these instead of running Docker commands directly.
- **Kubernetes:** Helm charts in `infrastructure/helm/`, manifests in `infrastructure/k8s/`.
