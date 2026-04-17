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
