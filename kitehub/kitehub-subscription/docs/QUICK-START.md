# Quick Start — kitehub-subscription

## Prerequisites

- Java 21 (`JAVA_HOME` set)
- Maven 3.9+ (or use `../mvnw`)
- PostgreSQL running (port 5433)
- Redis running (port 6380)
- RabbitMQ running (port 5673)

## Build

```bash
cd kitehub/
./mvnw clean package -pl kitehub-subscription -am -DskipTests
```

## Run

```bash
# Via Docker (recommended)
./scripts/up.sh kitehub-subscription

# Via Maven (standalone)
cd kitehub-subscription/
../mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Port:** 8081 (host) → 8080 (container)

## Test

```bash
# Unit tests
cd kitehub/
./mvnw test -pl kitehub-subscription

# E2E (requires full stack)
./scripts/test-api-e2e.sh
```

## Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/instances | Create instance (trial) |
| GET | /api/instances/{id} | Get instance details |
| POST | /api/subscriptions | Create subscription |
| GET | /api/subscriptions/active | Get active subscription |
| POST | /api/domains/setup | Setup custom domain |
| GET | /actuator/health | Health check |

## Configuration

See `src/main/resources/application.yml` for all config.
Key properties: `kitehub.trial.*`, `kitehub.subscription.*`, `kitehub.data-retention.*`

## Business Logic

See [documents/01-business/kitehub/](../../../documents/01-business/kitehub/) for all business rules.
