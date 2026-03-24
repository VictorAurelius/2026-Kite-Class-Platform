# Quick Start - KiteHub Admin Service

## Prerequisites

- Java 21+
- Maven 3.9+ (or use `./mvnw`)
- PostgreSQL running on port 5433
- Redis running on port 6380

## Build

```bash
# From kitehub/ root (builds kitehub-platform + kitehub-subscription + kitehub-admin)
cd kitehub
./mvnw clean install -pl kitehub-admin -am

# Skip tests
./mvnw clean install -pl kitehub-admin -am -DskipTests
```

## Run Standalone

```bash
cd kitehub/kitehub-admin

# Required env vars
export DATABASE_PASSWORD=kitehub_dev_password
export ENCRYPTION_MASTER_KEY=your-master-key
export JWT_SECRET=your-jwt-secret

../mvnw spring-boot:run
```

Service starts on `http://localhost:8083` (standalone) or `http://localhost:8085` (Docker host).

## Run with Docker

```bash
# Start full stack
cd kitehub
./scripts/up.sh

# Or rebuild just admin service
./scripts/rebuild.sh kitehub-admin
```

## Test

```bash
# Run all tests
cd kitehub
./mvnw test -pl kitehub-admin

# Run specific test
./mvnw test -pl kitehub-admin -Dtest=AdminControllerTest
```

## Verify

```bash
# Health check (standalone)
curl http://localhost:8083/actuator/health

# Health check (Docker)
curl http://localhost:8085/actuator/health

# Via gateway
curl http://localhost:9000/api/platform/admin/stats
```

## API Documentation

Swagger UI: `http://localhost:8085/swagger-ui.html`

Via gateway: `http://localhost:9000/docs/admin/swagger-ui.html`
