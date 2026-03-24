# KiteClass Quick Start

## Prerequisites

- Docker & Docker Compose installed
- KiteHub stack running (provides database, Redis, gateway, and other infrastructure)

## Start

KiteClass runs as part of the KiteHub Docker stack:

```bash
# 1. Start the full stack (includes all KiteClass services)
cd kitehub && ./scripts/up.sh

# 2. Check services are running
cd kitehub && ./scripts/status.sh
```

## Access

| Service | URL |
|---------|-----|
| KiteClass Frontend | http://localhost:3000 |
| API (via Gateway) | http://localhost:9000/api/v1/ |

### Example API calls

```bash
# List students (via gateway)
curl http://localhost:9000/api/v1/students?page=0&size=10

# Create a student
curl -X POST http://localhost:9000/api/v1/students \
  -H "Content-Type: application/json" \
  -d '{"name": "Nguyen Van A", "email": "a@test.com", "phone": "0901234567"}'
```

## Standalone Development (no KiteHub dependency)

If you only need to work on KiteClass without running the full KiteHub stack:

```bash
# 1. Start minimal infra (PostgreSQL on 5434, Redis on 6381)
./scripts/dev-up.sh

# 2. Run Core service with standalone profile
cd kiteclass-core && ./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5434/kiteclass_dev \
  --spring.datasource.username=kiteclass \
  --spring.datasource.password=kiteclass_dev_password \
  --spring.data.redis.host=localhost \
  --spring.data.redis.port=6381"

# 3. Stop when done
docker compose -f docker-compose.standalone.yml down
```

Ports are chosen to avoid conflicts with the full KiteHub stack (5432/6379).

## Testing

```bash
# Run all backend tests
cd kiteclass/kiteclass-core && ./mvnw test

# Run frontend tests
cd kiteclass/kiteclass-frontend && npm test
```

## Logs & Debugging

```bash
# View kiteclass-core logs
cd kitehub && ./scripts/logs.sh kiteclass-core

# View all logs
cd kitehub && ./scripts/logs.sh

# Execute command in container
cd kitehub && ./scripts/exec.sh kiteclass-core bash
```

## Rebuild After Changes

```bash
# Rebuild a single service
cd kitehub && ./scripts/rebuild.sh kiteclass-core

# Rebuild all
cd kitehub && ./scripts/build-all.sh
```

## Related

- [KiteHub QUICK_START](../kitehub/QUICK_START.md) - Full stack setup details
- [KiteClass Core README](kiteclass-core/README.md) - Architecture and module docs
- [Business Rules](../documents/01-business/kiteclass/) - Business logic documentation
