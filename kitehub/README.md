# KiteHub Platform Services

**Version:** 1.0
**Purpose:** Multi-tenant SaaS platform management (subscriptions, payments, AI branding, email notifications)
**Architecture:** Microservices on Spring Boot 3.5.14

---

## Quick Start (Local Development)

### 1. Start Infrastructure Only

Start PostgreSQL, Redis, RabbitMQ, and MinIO for local development:

```bash
docker-compose -f docker-compose.kitehub.yml up -d kitehub-postgres kitehub-redis kitehub-rabbitmq kitehub-minio
```

### 2. Verify Infrastructure

**PostgreSQL:**
```bash
docker exec -it kitehub-postgres psql -U kitehub -d kitehub -c "SELECT version();"
# Expected: PostgreSQL 15.x
```

**Redis:**
```bash
docker exec -it kitehub-redis redis-cli ping
# Expected: PONG
```

**RabbitMQ Management UI:**
```bash
open http://localhost:15673
# Login: kitehub / kitehub_dev_password
```

**MinIO Console:**
```bash
open http://localhost:9191
# Login: kitehub / kitehub_dev_password
# API Endpoint: http://localhost:9100
# Default bucket: kitehub-assets
```

**Verify MinIO Bucket:**
```bash
docker exec -it kitehub-minio mc ls kitehub/
# Expected: kitehub-assets/
```

### 3. Run Services Locally (IntelliJ/VSCode)

Configure run configurations to point to local infrastructure:

**Environment Variables:**
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/kitehub
SPRING_DATASOURCE_USERNAME=kitehub
SPRING_DATASOURCE_PASSWORD=kitehub_dev_password
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6380
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5673
SPRING_RABBITMQ_USERNAME=kitehub
SPRING_RABBITMQ_PASSWORD=kitehub_dev_password
```

**IntelliJ Run Configuration:**
1. Right-click service's main class (e.g., `KitehubSubscriptionApplication.java`)
2. Edit Configurations → Add New Configuration → Spring Boot
3. Set Environment Variables (paste from above)
4. Run

### 4. Or Start Full Stack (Once Services Implemented)

```bash
docker-compose -f docker-compose.kitehub.yml up -d
```

**View logs:**
```bash
docker-compose -f docker-compose.kitehub.yml logs -f kitehub-subscription
```

**Stop all services:**
```bash
docker-compose -f docker-compose.kitehub.yml down
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  KiteHub Platform Services (Multi-Tenant SaaS Layer)        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │  Subscription    │  │    Payment       │               │
│  │    Service       │  │    Service       │               │
│  │  Port: 8081      │  │  Port: 8082      │               │
│  │                  │  │                  │               │
│  │ - Trial tracking │  │ - VietQR API     │               │
│  │ - Plan upgrades  │  │ - Payment logs   │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │    Branding      │  │      Email       │               │
│  │    Service       │  │    Service       │               │
│  │  Port: 8083      │  │  Port: 8084      │               │
│  │                  │  │                  │               │
│  │ - OpenAI GPT-4   │  │ - AWS SES        │               │
│  │ - DALL-E 3       │  │ - Templates      │               │
│  │ - S3 storage     │  │ - Queue consumer │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │     Admin        │  │    Gateway       │               │
│  │    Service       │  │  Port: 9000      │               │
│  │  Port: 8085      │  │                  │               │
│  │                  │  │ - API routing    │               │
│  │ - System mgmt    │  │ - Rate limiting  │               │
│  │ - Analytics      │  │ - Load balancing │               │
│  └──────────────────┘  └──────────────────┘               │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │               Infrastructure                          │ │
│  │  - PostgreSQL (5433): Platform metadata              │ │
│  │  - Redis (6380): Caching, rate limiting              │ │
│  │  - RabbitMQ (5673): Message queue                    │ │
│  │  - RabbitMQ Mgmt (15673): Admin UI                   │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ API Gateway Routes
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  KiteClass Instances (Tenant-specific LMS)                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐          │
│  │ Instance 1 │  │ Instance 2 │  │ Instance N │          │
│  │ (Core)     │  │ (Core)     │  │ (Core)     │          │
│  │            │  │            │  │            │          │
│  │ - Students │  │ - Students │  │ - Students │          │
│  │ - Courses  │  │ - Courses  │  │ - Courses  │          │
│  │ - Invoices │  │ - Invoices │  │ - Invoices │          │
│  └────────────┘  └────────────┘  └────────────┘          │
│                                                             │
│  Each instance: Isolated database, independent data        │
└─────────────────────────────────────────────────────────────┘
```

---

## Service Ports

| Service | Port | Purpose | Status |
|---------|------|---------|--------|
| **Infrastructure** |
| PostgreSQL | 5433 | Platform metadata database | ✅ Ready |
| Redis | 6380 | Caching, session storage | ✅ Ready |
| RabbitMQ | 5673 | Message queue (AMQP) | ✅ Ready |
| RabbitMQ Mgmt | 15673 | Admin UI | ✅ Ready |
| **Platform Services** |
| Subscription | 8081 | Trial, plan management, lifecycle state machine | ✅ Live |
| Payment | 8082 | VietQR payment processing | 🚧 Consolidated into subscription |
| Branding | 8083 | AI logo/content generation (Ollama, template-first) | ✅ Live |
| Email | 8084 | Email notifications (templates, queue consumer) | ✅ Live |
| Admin | 8085 | Admin portal API + analytics | ✅ Live |
| Platform | 808x | Cross-cutting platform features | ✅ Live |
| Gateway | 9000 | API Gateway (auth, routing, rate limiting) | ✅ Live |

**Port Strategy:**
- KiteHub uses ports 5433, 6380, 5673, 8081-8085, 9000
- KiteClass uses ports 5432, 6379, 5672, 8080, 8000, 3000
- No conflicts between the two systems

---

## Database Schema

**Platform Database (`kitehub`):**
- `instances`: Tenant instances (subdomain, database URL, status)
- `subscriptions`: Trial/paid subscriptions
- `payments`: Payment transactions (VietQR)
- `branding_jobs`: AI branding generation queue
- `email_logs`: Email sending history

**Instance Databases (`kiteclass_{instance_id}`):**
- Separate PostgreSQL database per tenant
- Provisioned dynamically by Subscription Service (PR 4.2)
- Complete isolation (no cross-tenant data access)

---

## Development Workflow

### Phase 1: Infrastructure Setup (Current)
```bash
# Start infrastructure
docker-compose -f docker-compose.kitehub.yml up -d kitehub-postgres kitehub-redis kitehub-rabbitmq

# Verify
docker ps
```

### Phase 2: Implement First Service (PR 4.1 - Subscription)
```bash
# Create service directory
mkdir -p kitehub-subscription/src/main/java/com/kiteclass/kitehub/subscription

# Build service
cd kitehub-subscription
./mvnw clean install

# Uncomment in docker-compose.kitehub.yml
# kitehub-subscription: ...

# Start with infrastructure
docker-compose -f docker-compose.kitehub.yml up -d
```

### Phase 3: Add More Services
Repeat for each service:
1. Implement service (Java + Spring Boot)
2. Create Dockerfile
3. Uncomment in docker-compose.kitehub.yml
4. Test integration with infrastructure
5. Test cross-service communication

---

## Environment Variables

### Required for All Services
```properties
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://kitehub-postgres:5432/kitehub
SPRING_DATASOURCE_USERNAME=kitehub
SPRING_DATASOURCE_PASSWORD=kitehub_dev_password
SPRING_REDIS_HOST=kitehub-redis
SPRING_REDIS_PORT=6379
```

### Service-Specific

**Payment Service:**
```properties
VIETQR_API_KEY=your_vietqr_key
VIETQR_API_SECRET=your_vietqr_secret
```

**Branding Service:**
```properties
OPENAI_API_KEY=sk-proj-...
AWS_S3_BUCKET=kitehub-branding-assets
AWS_REGION=ap-southeast-1
```

**Email Service:**
```properties
AWS_SES_REGION=ap-southeast-1
AWS_SES_FROM_EMAIL=noreply@kitehub.me
```

---

## Testing

### Integration Tests (Local)
```bash
# Start infrastructure
docker-compose -f docker-compose.kitehub.yml up -d kitehub-postgres kitehub-redis kitehub-rabbitmq

# Run tests
cd kitehub-subscription
./mvnw test

# Cleanup
docker-compose -f docker-compose.kitehub.yml down -v
```

### End-to-End Tests
```bash
# Start all services
docker-compose -f docker-compose.kitehub.yml up -d

# Test API endpoints
curl http://localhost:9000/api/v1/instances
curl http://localhost:9000/api/v1/subscriptions

# View logs
docker-compose -f docker-compose.kitehub.yml logs -f
```

---

## Next Steps

1. **Implement PR 4.1** (Subscription Service)
   - Instance provisioning
   - Trial tracking
   - Subscription management

2. **Uncomment `kitehub-subscription`** in `docker-compose.kitehub.yml`

3. **Test local development workflow**
   - Build service
   - Start with `docker-compose up -d`
   - Verify health checks
   - Test API endpoints

4. **Repeat for other services** (PR 4.2 - 4.13)

5. **Integration testing**
   - Cross-service communication
   - Message queue workflows
   - Database provisioning flow

---

## Troubleshooting

### PostgreSQL connection refused
```bash
# Check if container is running
docker ps | grep kitehub-postgres

# Check logs
docker logs kitehub-postgres

# Restart
docker-compose -f docker-compose.kitehub.yml restart kitehub-postgres
```

### Redis connection timeout
```bash
# Check if container is running
docker ps | grep kitehub-redis

# Test connection
docker exec -it kitehub-redis redis-cli ping

# Check port binding
netstat -an | grep 6380
```

### RabbitMQ not starting
```bash
# Check logs
docker logs kitehub-rabbitmq

# Verify management UI
open http://localhost:15673

# Restart
docker-compose -f docker-compose.kitehub.yml restart kitehub-rabbitmq
```

### Port conflicts
```bash
# Check what's using the port
lsof -i :5433  # PostgreSQL
lsof -i :6380  # Redis
lsof -i :5673  # RabbitMQ

# Kill process or change port in docker-compose.kitehub.yml
```

---

## Related Documentation

- **Master PR Index**: `documents/03-planning/prs/00-master-pr-index.md`
- **KiteHub PRs**: `documents/03-planning/prs/04-kitehub-prs.md`
- **Implementation Plan**: `documents/03-planning/implementation/kitehub-implementation-plan.md`
- **Architecture Overview**: `documents/03-planning/architecture/system-architecture-v3-final.md`

---

**Last Updated:** 2026-04-28
**Status:** Infrastructure live, all 6 platform services + gateway shipped (Wave 4–7 milestones)
