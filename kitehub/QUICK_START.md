# KiteHub Quick Start Guide

Quick guide to build and run KiteHub locally.

## Prerequisites

- Docker & Docker Compose
- Git
- 16GB+ RAM recommended

## 1. Setup Environment

```bash
cd kitehub

# Copy .env template
cp .env.example .env

# Generate secure secrets
ENCRYPTION_KEY=$(openssl rand -base64 32 | tr -d '\n')
JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
INTERNAL_API_SECRET=$(openssl rand -base64 32 | tr -d '\n')

# Update .env with generated secrets
sed -i "s|POSTGRES_PASSWORD=CHANGE_ME|POSTGRES_PASSWORD=kitehub_dev_password|" .env
sed -i "s|RABBITMQ_PASSWORD=CHANGE_ME|RABBITMQ_PASSWORD=kitehub_dev_password|" .env
sed -i "s|MINIO_ROOT_PASSWORD=CHANGE_ME|MINIO_ROOT_PASSWORD=kitehub_dev_password|" .env
sed -i "s|ENCRYPTION_MASTER_KEY=CHANGE_ME|ENCRYPTION_MASTER_KEY=${ENCRYPTION_KEY}|" .env
sed -i "s#JWT_SECRET=CHANGE_ME#JWT_SECRET=${JWT_SECRET}#" .env
sed -i "s|INTERNAL_API_SECRET=CHANGE_ME|INTERNAL_API_SECRET=${INTERNAL_API_SECRET}|" .env

echo "✅ Environment configured"
```

## 2. Build Images

```bash
# Build base image
docker build -t kitehub-base:latest -f kitehub-base/Dockerfile .

# Build all services
docker-compose -f docker-compose.kitehub.yml build
```

**Build time:** ~15 minutes

## 3. Start Stack

```bash
docker-compose -f docker-compose.kitehub.yml up -d
```

## 4. Verify Services

Wait ~2 minutes for services to be healthy:

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Expected status:
- ✅ All infrastructure services: `(healthy)`
- ✅ kitehub-frontend, gateway, subscription, branding, email: `(healthy)`
- ✅ kitehub-admin: `(healthy)` (may take 1-2 minutes)

## 5. Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **KiteHub Frontend** | http://localhost:3001 | - |
| **API Gateway** | http://localhost:9000 | - |
| **RabbitMQ Console** | http://localhost:15673 | kitehub / kitehub_dev_password |
| **MinIO Console** | http://localhost:9191 | kitehub / kitehub_dev_password |
| **MailHog** | http://localhost:8025 | - |

## Troubleshooting

### Services restarting

Check logs:
```bash
docker logs kitehub-subscription --tail 50
docker logs kitehub-admin --tail 50
```

Common issues:
- **Missing JWT_SECRET**: Ensure .env is properly configured
- **Database password mismatch**: Run `docker-compose down -v` and restart
- **Line ending issues**: Run `sed -i 's/\r$//' docker/postgres/init-kiteclass-db.sh`

### Stop Stack

```bash
docker-compose -f docker-compose.kitehub.yml down

# Remove volumes (clean slate)
docker-compose -f docker-compose.kitehub.yml down -v
```

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

1. Register a new account: http://localhost:3001/register
2. Check email in MailHog: http://localhost:8025
3. Verify email and login
4. Create your first instance

## Scripts Reference

KiteHub includes helper scripts (work in progress - some may have line ending issues):

```bash
./scripts/up.sh         # Start stack
./scripts/down.sh       # Stop stack
./scripts/status.sh     # View status
./scripts/logs.sh       # View logs
./scripts/build-all.sh  # Build all images
```

**Note:** If scripts fail with `bad interpreter`, use docker-compose commands directly as shown above.
