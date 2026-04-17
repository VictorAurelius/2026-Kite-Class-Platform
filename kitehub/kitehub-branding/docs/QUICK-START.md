# Quick Start - KiteHub Branding Service

## Prerequisites

- Java 21+
- Maven 3.9+ (or use `./mvnw`)
- PostgreSQL running on port 5433
- RabbitMQ running on port 5673
- MinIO running on port 9100 (optional, mock mode default)

## Build

```bash
# From kitehub/ root (builds all modules including kitehub-platform)
cd kitehub
./mvnw clean install -pl kitehub-branding -am

# Skip tests for faster build
./mvnw clean install -pl kitehub-branding -am -DskipTests
```

## Run Standalone

```bash
cd kitehub/kitehub-branding
../mvnw spring-boot:run
```

Service starts on `http://localhost:8083`.

## Run with Docker

```bash
# Start full stack (recommended)
cd kitehub
./scripts/up.sh

# Or rebuild just branding service
./scripts/rebuild.sh kitehub-branding
```

## Test

```bash
# Run all tests
cd kitehub
./mvnw test -pl kitehub-branding

# Run specific test class
./mvnw test -pl kitehub-branding -Dtest=BrandingServiceTest
```

## Verify

```bash
# Health check
curl http://localhost:8083/actuator/health

# Via gateway
curl http://localhost:9000/api/platform/branding/templates
```

## API Documentation

Swagger UI available at: `http://localhost:8083/swagger-ui.html`

Via gateway: `http://localhost:9000/docs/branding/swagger-ui.html`
