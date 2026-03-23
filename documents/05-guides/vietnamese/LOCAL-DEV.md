# KiteHub Local Development Guide

## Quick Start (< 5 minutes)

```bash
# 1. Clone repo
git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git
cd 2026-Kite-Class-Platform/kitehub

# 2. One-command setup (generates .env, builds images, starts stack)
./scripts/setup.sh

# 3. Seed test data
./scripts/seed-data.sh

# 4. Open browser
# Frontend: http://localhost:3001
# Login: dev@kiteteam.com / KiteTeam@Dev123
```

---

## Prerequisites

- Docker Desktop (with Docker Compose v2)
- 8GB+ RAM available for Docker
- Git
- openssl (for key generation)

---

## URLs & Ports

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:3001 | KiteHub customer portal |
| **Gateway API** | http://localhost:9000 | API gateway |
| **KiteClass Core** | http://localhost:8088 | KiteClass business logic |
| **Subscription** | http://localhost:8081 | Instance management |
| **Branding** | http://localhost:8083 | AI branding service |
| **Email** | http://localhost:8084 | Email notifications |
| **Admin** | http://localhost:8085 | Admin dashboard |
| **PostgreSQL** | localhost:5433 | Database |
| **Redis** | localhost:6380 | Cache |
| **RabbitMQ Console** | http://localhost:15673 | Message queue UI |
| **MinIO Console** | http://localhost:9191 | Object storage UI |

---

## Test Accounts (after seed-data.sh)

| Email | Password | Role | Instance |
|-------|----------|------|----------|
| dev@kiteteam.com | KiteTeam@Dev123 | OWNER | kiteteam-dev (5 students, 3 teachers) |
| demo@kiteteam.com | KiteTeam@Demo123 | OWNER | kiteteam-demo (showcase) |
| admin@kitehub.com | Admin@KiteHub123 | OWNER | admin-portal |

---

## Common Tasks

### Start/Stop
```bash
./scripts/up.sh      # Start all services
./scripts/down.sh    # Stop all services
./scripts/status.sh  # Check service status
```

### Rebuild a service
```bash
./scripts/rebuild.sh subscription  # After code changes
./scripts/rebuild.sh frontend      # After frontend changes
./scripts/rebuild.sh gateway       # After gateway changes
./scripts/build-all.sh             # Rebuild everything
```

### View logs
```bash
./scripts/logs.sh subscription -f      # Follow subscription logs
./scripts/logs.sh gateway --tail 50    # Last 50 lines
./scripts/logs-pretty.sh               # All logs with color highlighting
./scripts/logs-pretty.sh --errors      # Show only errors/warnings
./scripts/logs-pretty.sh gateway -f    # Follow gateway logs (colored)
```

### Monitor system health
```bash
./scripts/status.sh                    # Full status: health + resources + errors
./scripts/status.sh --simple           # Quick status without resources
./scripts/monitor.sh                   # Background monitor (30s interval)
./scripts/monitor.sh --interval 60     # Custom interval
./scripts/monitor.sh --notify          # Desktop notifications on failure
```

### Run tests
```bash
./scripts/test-api-e2e.sh           # 63 API E2E tests
cd kitehub-frontend && pnpm test:e2e # 110 Frontend E2E tests
```

### Reset data
```bash
./scripts/down.sh
docker volume rm kitehub_kitehub-postgres-data  # Delete all data
./scripts/up.sh
./scripts/seed-data.sh  # Re-seed test data
```

### Access KiteClass API (via gateway)
```bash
# Register → get token
TOKEN=$(curl -s -X POST http://localhost:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"organizationName":"Test","subdomain":"test1","ownerEmail":"test@test.com","ownerPassword":"Test@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# Access KiteClass students
curl -H "X-Instance-Subdomain: test1" \
     -H "Authorization: Bearer $TOKEN" \
     http://localhost:9000/api/v1/students
```

---

## Architecture (Local)

```
Browser → Frontend (3001) → Gateway (9000) → Services
                                 ↓
                    ┌────────────┼────────────┐
                    ↓            ↓            ↓
              Subscription   Branding     Admin
               (8081)        (8083)      (8085)
                    ↓
              PostgreSQL (5433)    Redis (6380)
              RabbitMQ (5673)     MinIO (9100)

Gateway also routes /api/v1/** → KiteClass Core (8088)
  via TenantResolver (X-Instance-Subdomain header)
```

---

## Mock Services

All external services run in mock mode by default:

| Service | Mock Behavior |
|---------|---------------|
| **OpenAI** | Returns sample analysis + placeholder images |
| **VietQR** | Returns placeholder QR code URL |
| **Email (SES)** | Logs email content, no real send |
| **S3 Storage** | Uses MinIO (local S3-compatible) |

To use real APIs, update `.env`:
```bash
OPENAI_API_KEY=sk-real-key-here
PAYMENT_MOCK_MODE=false
```

---

## Troubleshooting

### Services won't start
```bash
./scripts/status.sh              # Check which service is unhealthy
./scripts/logs-pretty.sh --errors # See recent errors
docker logs kitehub-subscription # Detailed logs for specific service
```

**Common causes:**
- Docker not running → Start Docker Desktop
- Ports in use → `lsof -i :9000` to find process
- Database not ready → Wait 10s after start

### "POSTGRES_PASSWORD is required"
```bash
# .env file missing or incomplete
./scripts/setup.sh  # Regenerates .env with secure defaults
```

### Database connection errors after rebuild
```bash
# Subscription service in-memory users are lost on restart
# Wait 10s then retry, or re-run seed-data.sh
./scripts/seed-data.sh
```

### High CPU usage
```bash
./scripts/status.sh              # Check resource usage
docker stats                     # Real-time stats
# Consider: Reduce replicas in docker-compose.yml
```

### Service keeps restarting
```bash
./scripts/monitor.sh --interval 10 --notify  # Monitor in background
./scripts/logs-pretty.sh gateway --follow    # Watch logs live
# Check: Memory limits in docker-compose.yml
```

### Port already in use
```bash
# Check what's using the port
lsof -i :9000
# Kill the process or change port in .env
```

### Monitor logs continuously
```bash
# Run monitor in background (logs to logs/monitor.log)
./scripts/monitor.sh --notify &

# Check monitor logs
tail -f kitehub/logs/monitor.log
```
