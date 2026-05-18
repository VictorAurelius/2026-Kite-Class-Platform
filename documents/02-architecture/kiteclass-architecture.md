# KiteClass Architecture

## Overview

KiteClass is a multi-tenant education platform where each tenant represents a school or institute. Tenants are provisioned and managed by KiteHub (SaaS lifecycle), while KiteClass handles all education business logic.

Tenants access the platform via subdomains: `{tenant}.kiteclass.com`.

## Services

> **Note (Wave 96, ADR-032):** `kiteclass-gateway` đã được removed. Routing upstream do shared `kite-gateway` xử lý (per ADR-023); auth/JWT/migrations/user management chuyển vào `kiteclass-core`.

| Service | Tech Stack | Port | Responsibility |
|---------|-----------|------|----------------|
| **kiteclass-core** | Spring Boot | 8081 | Authentication, JWT issuance/validation, Flyway DB migrations, user management, all business logic modules (see Module List below) |
| **kiteclass-frontend** | Next.js | 3000 | Student, teacher, admin, and owner UI |

Routing upstream (subdomain → service) do shared `kite-gateway` đảm nhiệm — xem ADR-023 (Gateway key resolver strategy) và `documents/02-architecture/adr/ADR-032-kiteclass-gateway-removal.md`.

## Multi-Tenant Isolation

- **Strategy:** Shared database with **layered defense** — code-level `instance_id` column **and** Postgres Row-Level Security (RLS) at the database layer (per GAP-466 / Wave 56).
- **Layer 1 — Code (Hibernate filter):** Every entity extends `BaseEntity`, which declares `@Column("instance_id")` plus the `tenantFilter` Hibernate `@FilterDef`. `TenantFilterInterceptor` enables the filter per HTTP request from the `X-Tenant-Id` header (forwarded by the shared `kite-gateway`).
- **Layer 2 — Database (RLS policy):** Every tenant-scoped table has `ENABLE ROW LEVEL SECURITY` plus `FORCE ROW LEVEL SECURITY` and a `tenant_isolation` policy `USING (instance_id = current_setting('app.current_tenant_id', true)::uuid)`. `TenantAwareDataSourceInterceptor` issues `SET LOCAL app.current_tenant_id = <uuid>` at every `@Transactional` boundary, so even raw `SELECT * FROM students` returns only current-tenant rows. If `TenantContext` is empty, the GUC stays NULL and the policy defaults to deny.
- **Why both layers?** Layer 1 is fast but bypassable by custom JPQL / native SQL / projection DTOs that forget the filter. Layer 2 makes a developer-error cross-tenant leak structurally impossible at the database boundary. AWS Well-Architected SaaS Lens recommends this pattern for "Pool" multi-tenant models.
- **Break-glass:** Documented in [`documents/05-guides/operations/runbooks/rls-policy-violation.md`](../05-guides/operations/runbooks/rls-policy-violation.md). DB superuser only; every invocation logs an audit trail.

## Infrastructure

All infrastructure uses the `kite-` prefix (shared with KiteHub).

| Component | Usage |
|-----------|-------|
| **PostgreSQL** (`kite-postgres`) | Shared DB with tenant column isolation. `kiteclass-core` owns migrations (Flyway). |
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

Auth modules (within `kiteclass-core`, moved from removed `kiteclass-gateway` per ADR-032):

| Module | Description |
|--------|-------------|
| **auth** | Login, JWT issuance, refresh tokens |
| **user** | User CRUD, role assignment |

## Authentication & Authorization

- **Mechanism:** JWT with refresh token rotation.
- **Roles:** `OWNER`, `ADMIN`, `TEACHER`, `STUDENT`
- `kiteclass-core` issues and validates JWTs trực tiếp. Shared `kite-gateway` (per ADR-023) forwards request kèm `X-Tenant-Id` header sau khi resolve subdomain.
- Role-based access control is enforced at the controller level.

## Internal Communication

Sau removal của `kiteclass-gateway` (ADR-032), routing upstream do shared `kite-gateway` đảm nhiệm. Request từ client đi qua single gateway boundary; HMAC internal-request layer cũ (gateway↔core) loại bỏ.

## Request Flow

```
Client
  │
  ▼
kiteclass-frontend (Next.js :3000)
  │ API calls
  ▼
kite-gateway (shared, per ADR-023)
  │ Subdomain → tenant resolve
  │ Forward X-Tenant-Id header
  ▼
kiteclass-core (Spring Boot :8081)
  │ JWT validation + business logic execution
  ▼
PostgreSQL / Redis / RabbitMQ / MinIO
```

## Async Events

Modules communicate asynchronously via RabbitMQ for cross-cutting concerns:

- Payment events trigger invoice generation
- Enrollment events notify the LMS module
- Assignment events feed into gamification scoring

## Deployment

- **Production (integrated mode):** `kitehub/docker-compose.kitehub.yml` — `kiteclass-core` + `kiteclass-frontend` chạy cạnh KiteHub services; routing via shared `kite-gateway`.
- **Development:** `kiteclass/docker-compose.dev.yml` — standalone dev sandbox cho KiteClass services (per ADR-032, không còn `kiteclass-gateway`).
- **Scripts:** `kiteclass/scripts/` — use these instead of running Docker commands directly.
- **Kubernetes:** Helm charts in `infrastructure/helm/`, manifests in `infrastructure/k8s/`.
