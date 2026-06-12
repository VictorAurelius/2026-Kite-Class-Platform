# Deployment Guide

Deployment instructions for the Kite Class Platform on Oracle Cloud Infrastructure.

## Infrastructure Overview

```
┌─────────────────────────────────────────────┐
│           Oracle Cloud (ARM Instance)        │
│                                              │
│  ┌─────────────────────────────────────┐     │
│  │         Docker Compose Stack         │     │
│  │                                      │     │
│  │  ┌──────────┐  ┌──────────────────┐ │     │
│  │  │ Gateway   │  │ KiteHub Frontend │ │     │
│  │  │ (8080)    │  │ (3000)           │ │     │
│  │  └────┬──────┘  └──────────────────┘ │     │
│  │       │                               │     │
│  │  ┌────┴──────────────────────────┐   │     │
│  │  │ Microservices                  │   │     │
│  │  │ ┌────────────┐ ┌────────────┐ │   │     │
│  │  │ │Subscription│ │  Billing   │ │   │     │
│  │  │ └────────────┘ └────────────┘ │   │     │
│  │  │ ┌────────────┐ ┌────────────┐ │   │     │
│  │  │ │   Email    │ │  Analytic  │ │   │     │
│  │  │ └────────────┘ └────────────┘ │   │     │
│  │  │ ┌────────────┐ ┌────────────┐ │   │     │
│  │  │ │   Auth     │ │ KiteClass  │ │   │     │
│  │  │ └────────────┘ └────────────┘ │   │     │
│  │  └───────────────────────────────┘   │     │
│  │                                      │     │
│  │  ┌──────────┐  ┌──────────────────┐ │     │
│  │  │PostgreSQL│  │      Redis       │ │     │
│  │  │ (5432)   │  │     (6379)       │ │     │
│  │  └──────────┘  └──────────────────┘ │     │
│  └─────────────────────────────────────┘     │
│                                              │
│  ┌──────────────┐  ┌────────────────┐        │
│  │  Ollama (AI) │  │ KiteClass FE   │        │
│  │  (11434)     │  │ (3001)         │        │
│  └──────────────┘  └────────────────┘        │
└─────────────────────────────────────────────┘
```

## Prerequisites

- Oracle Cloud account with Always-Free tier ARM instance
- Docker 24.x and Docker Compose 2.x installed
- Domain names configured: `kitehub.vn`, `*.kitehub.me`
- SSL certificates (auto-managed via Let's Encrypt)

## Environment Setup

### 1. Clone and Configure

```bash
git clone https://github.com/your-org/kite-class-platform.git
cd kite-class-platform
```

### 2. Environment Variables

Create `.env` files for each service (never committed to git):

```bash
# Database
POSTGRES_DB=kitehub
POSTGRES_USER=kitehub
POSTGRES_PASSWORD=<secure-password>

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT
JWT_SECRET=<256-bit-secret>
JWT_EXPIRATION=86400000

# Email (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<email>
MAIL_PASSWORD=<app-password>

# Domain
DOMAIN_KITEHUB=kitehub.vn
DOMAIN_KITECLASS=kitehub.me
```

### 3. Start the Stack

**Always use the provided scripts (never run docker-compose directly):**

```bash
# Build all images
./kitehub/scripts/build-all.sh

# Start the full stack
./kitehub/scripts/up.sh

# Check status
./kitehub/scripts/status.sh

# View logs
./kitehub/scripts/logs.sh
```

### 4. Database Migrations

Flyway migrations run automatically on service startup. Migration files are located at:
- `kitehub/kitehub-*/src/main/resources/db/migration/`
- `kiteclass/src/main/resources/db/migration/`

## Script Reference

| Script | Purpose |
|--------|---------|
| `scripts/up.sh` | Start all services |
| `scripts/down.sh` | Stop all services |
| `scripts/logs.sh` | View service logs |
| `scripts/build-all.sh` | Build all Docker images |
| `scripts/rebuild.sh <service>` | Rebuild a single service |
| `scripts/status.sh` | Check container health |
| `scripts/exec.sh <service> <cmd>` | Execute command in container |
| `scripts/clean.sh` | Remove containers, volumes, images |

## CI/CD Pipeline

### GitHub Actions Workflow

```
Push to branch → Lint → Type Check → Test → Build → (on main) Deploy
```

### Pipeline Stages

1. **Lint**: ESLint (frontend), Checkstyle (backend)
2. **Type Check**: `tsc --noEmit` (frontend)
3. **Test**: Vitest (frontend), JUnit + Testcontainers (backend)
4. **Build**: Docker images with multi-stage builds
5. **Deploy**: SSH to Oracle Cloud → pull images → restart services

## Monitoring

### Health Checks

Each service exposes a health endpoint:
- Gateway: `GET /actuator/health`
- Services: `GET /actuator/health` (internal network only)
- Frontend: `GET /api/health`

### Log Management

```bash
# All services
./kitehub/scripts/logs.sh

# Specific service
./kitehub/scripts/logs.sh kitehub-subscription

# Follow mode
./kitehub/scripts/logs.sh -f
```

## Backup Strategy

- **Database**: Daily PostgreSQL pg_dump with 30-day retention
- **Redis**: RDB snapshots every 15 minutes
- **Volumes**: Docker volume backups to object storage

## Troubleshooting

| Issue | Solution |
|-------|---------|
| Service won't start | Check `./scripts/logs.sh <service>` for errors |
| Database connection failed | Verify PostgreSQL container is healthy: `./scripts/status.sh` |
| Port conflict | Check `docker ps` for conflicting containers |
| Out of memory | Check ARM instance limits, consider scaling |
| Migration failed | Check Flyway migration files for syntax errors |
