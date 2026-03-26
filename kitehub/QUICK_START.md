# KiteHub Quick Start Guide

Quick guide to build and run KiteHub locally.

## Prerequisites

- Docker Desktop (running)
- Bash shell
- 16GB+ RAM recommended

## 1. Setup Environment

```bash
cd kitehub
./scripts/setup.sh    # Generates .env with secrets
```

## 2. Build & Start

```bash
./scripts/build-all.sh   # Build all images (~20 min first time)
./scripts/up.sh           # Start stack
```

## 3. Wait for Healthy

```bash
./scripts/wait-for-healthy.sh    # Wait for all services (timeout 180s)
```

## 4. Verify

```bash
./scripts/status.sh --health     # Check all services
./scripts/test-api-e2e.sh        # Run E2E API tests
```

## Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| **KiteHub Frontend** | http://localhost:3001 | - |
| **KiteClass Frontend** | http://localhost:3000 | - |
| **API Gateway** | http://localhost:9000 | - |
| **MailHog (email)** | http://localhost:8025 | - |
| **RabbitMQ Console** | http://localhost:15673 | kitehub / (from .env) |
| **MinIO Console** | http://localhost:9191 | kitehub / (from .env) |

### With Monitoring Profile

```bash
./scripts/up.sh --profile monitoring
```

| Service | URL | Credentials |
|---------|-----|-------------|
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3002 | admin / admin |

### With AI Local Profile (Ollama)

Dùng khi muốn chạy AI branding với local LLM thay vì OpenAI API key.

```bash
# Set AI_PROVIDER=ollama trong .env (hoặc export)
AI_PROVIDER=ollama ./scripts/up.sh --profile ai-local
```

Lần đầu chạy sẽ tự pull model `llama3.1:8b` (~4GB). Sau đó AI endpoints hoạt động hoàn toàn offline.

| Service | URL | Purpose |
|---------|-----|---------|
| Ollama | http://localhost:11434 | Local LLM (llama3.1:8b text, llava:13b vision) |

**Lưu ý:** Không cần `--profile ai-local` nếu dùng `OPENAI_API_KEY=sk-mock-*` — mock mode tự động trả sample response mà không gọi API nào.

**Pull vision model thủ công (llava:13b ~8GB):**
```bash
docker exec kite-ollama ollama pull llava:13b
```

## Common Commands

```bash
./scripts/logs.sh gateway -f      # Follow gateway logs
./scripts/rebuild.sh subscription  # Rebuild single service
./scripts/down.sh                  # Stop stack
./scripts/clean.sh --all           # Full cleanup
./scripts/help.sh                  # Show all commands
```

## Troubleshooting

**Services not starting:**
```bash
./scripts/logs.sh <service>        # Check logs
./scripts/status.sh --health       # Check health
```

**Cold start slow:** First run builds all images (~20 min). Subsequent starts use cached images (~30s).

**Port conflicts:** Check `docker-compose.kitehub.yml` for port mappings. Common conflicts: 3000 (frontend), 5433 (postgres), 9000 (gateway).

**Missing secrets:** Run `./scripts/setup.sh` to regenerate `.env`.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Frontend (Next.js)                  │
│              http://localhost:3001                   │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│              Gateway (Spring Cloud)                  │
│              http://localhost:9000                   │
└──┬──────────┬──────────┬──────────┬─────────────────┘
   │          │          │          │
┌──▼────┐ ┌──▼──────┐ ┌─▼──────┐ ┌─▼─────┐
│Subscr.│ │Branding │ │ Email  │ │ Admin │
│ :8081 │ │  :8083  │ │ :8084  │ │ :8085 │
└───────┘ └─────────┘ └────────┘ └───────┘
     │         │          │         │
     └─────────┴──────────┴─────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
   ┌────▼─────┐         ┌──────▼──────┐
   │PostgreSQL│         │    Redis    │
   │  :5433   │         │    :6380    │
   └──────────┘         └─────────────┘
```

## Next Steps

1. Register: http://localhost:3001/register
2. Check email: http://localhost:8025
3. Verify email and login
4. Create your first instance
