# Docker Platform Architecture

## Overview

Kite Platform is a unified Docker Compose stack that runs two products sharing common infrastructure:

- **KiteHub** — SaaS management platform (subscription, branding, email, admin)
- **KiteClass** — Multi-tenant LMS instances (core backend + student/teacher frontend)

All services run on a single `kite-network` bridge network and share PostgreSQL, Redis, RabbitMQ, MinIO, and the API Gateway.

## Naming Convention

| Prefix | Scope | Examples |
|--------|-------|---------|
| `kite-` | Shared infrastructure used by both KiteHub + KiteClass | `kite-postgres`, `kite-redis`, `kite-gateway` |
| `kitehub-` | KiteHub-specific services | `kitehub-subscription`, `kitehub-branding` |
| `kiteclass-` | KiteClass-specific services | `kiteclass-core`, `kiteclass-frontend` |

## Service Topology

| Container | Image | Port | Role |
|-----------|-------|------|------|
| `kite-postgres` | postgres:15-alpine | 5433:5432 | Shared PostgreSQL (kitehub DB + kiteclass_shared DB) |
| `kite-redis` | redis:7-alpine | 6380:6379 | Shared cache & session store |
| `kite-rabbitmq` | rabbitmq:3-management-alpine | 5673:5672, 15673:15672 | Shared message broker |
| `kite-minio` | minio/minio:latest | 9100:9000, 9191:9091 | Shared S3-compatible object storage |
| `kite-minio-setup` | minio/mc:latest | — | Init container: creates buckets |
| `kite-mailhog` | mailhog/mailhog:latest | 1025:1025, 8025:8025 | Dev email capture (SMTP + Web UI) |
| `kite-gateway` | kite-gateway:latest | 9000:9000 | API Gateway (routes to all backend services) |
| `kite-base` | kite-base:latest | — | Build-only base image with Maven dependencies |
| `kitehub-subscription` | kitehub-subscription:latest | 8081:8080 | Tenant lifecycle, billing, auth |
| `kitehub-branding` | kitehub-branding:latest | 8083:8080 | AI-powered branding & assets |
| `kitehub-email` | kitehub-email:latest | 8084:8080 | Transactional email service |
| `kitehub-admin` | kitehub-admin:latest | 8085:8080 | Platform admin dashboard API |
| `kitehub-frontend` | kitehub-frontend:latest | 3001:3001 | KiteHub Next.js frontend |
| `kiteclass-core` | kiteclass-core:latest | 8088:8080 | Multi-tenant LMS backend |
| `kiteclass-frontend` | kiteclass-frontend:latest | 3000:3000 | Student/Teacher Next.js frontend |
| `kite-prometheus` | prom/prometheus:v2.51.0 | 9090:9090 | Metrics collection (monitoring profile) |
| `kite-grafana` | grafana/grafana:10.4.0 | 3002:3002 | Metrics dashboards (monitoring profile) |
| `kite-ollama` | ollama/ollama:latest | 11434:11434 | Local AI inference (ai-local profile) |
| `kite-ollama-setup` | ollama/ollama:latest | — | Init container: pulls AI models |

## Shared Infrastructure

PostgreSQL, Redis, RabbitMQ, MinIO, and the API Gateway are shared because:

1. **PostgreSQL** — KiteHub uses the `kitehub` database; KiteClass uses `kiteclass_shared`. Both in the same Postgres instance for simplicity in dev and small-scale production.
2. **Redis** — Session store and cache for all services. Namespace isolation via key prefixes.
3. **RabbitMQ** — Event bus for async communication (email events, tenant provisioning, etc.).
4. **MinIO** — S3-compatible storage for branding assets, student uploads, etc.
5. **Gateway** — Single entry point routing `/api/v1/subscriptions/*` to KiteHub services and `/api/v1/courses/*` to KiteClass.

## Network

All containers connect to `kite-network` (Docker bridge). Services reference each other by container name (DNS resolution within the bridge network).

## Volumes

| Volume | Used By | Purpose |
|--------|---------|---------|
| `kite-postgres-data` | kite-postgres | Database files |
| `kite-minio-data` | kite-minio | Object storage files |
| `kite-ollama-models` | kite-ollama | AI model weights |
| `kite-prometheus-data` | kite-prometheus | Metrics time-series data |
| `kite-grafana-data` | kite-grafana | Dashboard configs |

## Docker Compose Files

| File | Purpose | When to Use |
|------|---------|-------------|
| `docker-compose.kitehub.yml` | **Canonical** full stack (all services) | Local development, CI |
| `docker-compose.kitehub-only.yml` | KiteHub only (no KiteClass) | Testing KiteHub features in isolation |
| `docker-compose.oracle-backend.yml` | Production backend (Oracle Cloud VM 1) | Deploy infra + backend services |
| `docker-compose.oracle-frontend.yml` | Production frontend + AI (Oracle Cloud VM 2) | Deploy frontends + Ollama + Nginx |

### Profiles

- **Default** — Core services (infra + platform + frontends)
- `monitoring` — Adds Prometheus + Grafana
- `ai-local` — Adds Ollama + model setup
- `build-only` — Base image build target

## AI Branding v2 — Runtime Topology

**Updated 2026-04-26 (GAP-234).** Original design (`ai-branding-v2-redesign.md` 2026-04-14) placed all AI Branding workflow code in `kitehub-branding`. Wave 2-4 implementation moved the workflow + persistence into `kiteclass-core` so branding lives next to the tenant data it themes; `kitehub-branding` retains queue dispatch + worker pool for fair-scheduled async AI jobs.

### Module ownership

| Layer | Container / module | Code path | Role |
|-------|-------------------|-----------|------|
| Provisioning saga | `kiteclass-core` | `module/provisioning/TenantProvisioningSaga` | Listens to `tenant.created`; creates `FrontendInstance`; drives state machine |
| Workflow engine | `kiteclass-core` | `module/ai/workflow/{AnalyzerService,PlannerService,PlanExecutor,Step}` | Agent pipeline (analyze → plan → execute) |
| Steps | `kiteclass-core` | `module/ai/workflow/step/*` | `ExtractPaletteStep`, `PickTemplateStep`, `QualityReviewStep`, `PublishPackageStep` |
| Resource handlers | `kiteclass-core` | `module/branding/handler/*` | `StaticResourceHandler`, `TemplateResourceHandler`, `AIResourceHandler`, `FallbackHandler` (Chain-of-Responsibility per `ResourceCategory`) |
| Quality reviewer | `kiteclass-core` | `module/quality/service/InstanceQualityReviewer` + `module/quality/check/*` | 5-check scoring (`Contrast`, `CssVars`, `AssetUrls`, `VisualRegression`, `LogoPlacement`) → persists to `quality_reports` (V39) |
| Content moderation | `kiteclass-core` | `module/moderation/ContentModerationService` | 3-stage pipeline: keyword → ML scaffold → human queue (`moderation_queue` V36) |
| AI client (Strategy) | `kiteclass-core` | `module/ai/client/AIClient` impls | Wraps `kite-ollama`; vendor-isolated per `design-patterns.md` §3.10 |
| Queue topology | `kitehub-branding` | `config/AIQueueConfig`, `config/AIQueueProperties` | Owns `ai.request.{enterprise,pro,free}` exchanges + DLQs |
| Queue dispatcher | `kitehub-branding` | `queue/AIQueueDispatcher` | Routes `AIJobPayload` to tier queue (Exception D dispatcher per `design-patterns.md` §3.5.1) |
| Worker pool | `kitehub-branding` | `@RabbitListener` consumers | Drains tier queues, calls `kite-ollama`, posts results back via callback |
| Backlog inspector | `kitehub-branding` | `queue/BacklogInspector` | Emits `ai.queue.depth{tier}` metrics; triggers backpressure degradation |

### RabbitMQ queue topology

```
exchange: ai.request.exchange (DirectExchange)
  ├── routing-key=ai.request.enterprise → queue: ai.request.enterprise (weight 3)
  │     └── DLQ: ai.request.enterprise.dlq
  ├── routing-key=ai.request.pro        → queue: ai.request.pro        (weight 2)
  │     └── DLQ: ai.request.pro.dlq
  └── routing-key=ai.request.free       → queue: ai.request.free       (weight 1)
        └── DLQ: ai.request.free.dlq
```

**Tier weights** (`AIQueueProperties.tierWeights`): ENTERPRISE 3 : PRO 2 : FREE 1 (weighted round-robin).

**Concurrency caps** (`AIQueueProperties.concurrency`, Redis semaphore per tenant per tier): FREE 1, PRO 3, ENTERPRISE 10.

**SLA targets (p95 wait seconds)**: FREE 180, PRO 60, ENTERPRISE 30.

**Backpressure**: when `ai.request.enterprise` depth exceeds `enterpriseBacklogThreshold` (default 50), free tier degrades to STATIC/TEMPLATE-only fallback.

### Quality + moderation gate

`PlanExecutor` runs `QualityReviewStep` after asset generation. `InstanceQualityReviewer.review()` returns score `/100` (5 checks × 20 each, persisted to `quality_reports`). Below 70 → `TenantProvisioningSaga.markFailed()`. At/above 70 → `ContentModerationService` runs 3-stage pipeline; `APPROVED` triggers `markDeployed()`, `REJECTED`/`ESCALATED` routes through `moderation_queue` (V36).

### Lifecycle state machine

`FrontendInstance.status` (V31) transitions only via `TenantProvisioningSaga`:

```
NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING
                  ↓              ↓          ↑
                FAILED ←──────  FAILED  ────┘ (retry)
```

State transitions emit outbox events (`outbox_events` V33) and audit entries (`audit_log` V35). Branding snapshots persist to `branding_versions` (V43) for rollback.

### AI provider stance

Per `ai-branding-guidelines.md` §9 + GAP-006: AI inference goes to **local** `kite-ollama` (`llama3.1` for text, `llava` for vision) by default. Gemma 4 9B migration tracked by GAP-006. Direct cloud calls (GPT-4 Vision, DALL-E 3) referenced in v1 docs are NOT used in current deployment — original `14-ai-branding-pipeline.puml` (pre-2026-04-26) was stale; corrected by GAP-234.

### Compose / profile interaction

- `kitehub-branding` (port `8083`) starts in default profile; queue topology declared at startup via `AIQueueConfig`.
- `kite-ollama` only starts under `ai-local` profile; absent in production unless GPU-equipped node enabled.
- `kiteclass-core` (port `8088`) hosts all workflow code; persists to `kiteclass_shared` DB (V31-V45 migrations).
- Outbox publisher polls `outbox_events` table → publishes to RabbitMQ → cross-service consumers (FE invalidation via SSE).
