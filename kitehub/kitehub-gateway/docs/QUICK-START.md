# Quick Start - KiteHub Gateway

## Prerequisites

- Java 21+
- Maven 3.9+ (or use `./mvnw`)
- PostgreSQL running on port 5433
- Redis running on port 6380
- Backend services running (subscription, branding, email, admin)

## Build

```bash
# From kitehub/ root
cd kitehub
./mvnw clean install -pl kitehub-gateway -am

# Skip tests
./mvnw clean install -pl kitehub-gateway -am -DskipTests
```

## Run Standalone

```bash
cd kitehub/kitehub-gateway

# Required env vars
export DATABASE_PASSWORD=kitehub_dev_password

../mvnw spring-boot:run
```

Gateway starts on `http://localhost:9000`.

## Run with Docker

```bash
# Start full stack (recommended - gateway depends on all services)
cd kitehub
./scripts/up.sh
```

## Test

```bash
# Run all tests
cd kitehub
./mvnw test -pl kitehub-gateway

# Run specific test
./mvnw test -pl kitehub-gateway -Dtest=GatewayRoutingTest
```

## Verify

```bash
# Health check
curl http://localhost:9000/actuator/health

# List all routes
curl http://localhost:9000/actuator/gateway/routes

# Test routing to subscription service
curl http://localhost:9000/api/platform/config/pricing

# Test CORS preflight
curl -X OPTIONS http://localhost:9000/api/auth/login \
  -H "Origin: http://localhost:3001" \
  -H "Access-Control-Request-Method: POST" -v
```

## Service Documentation Routes

Each backend service's Swagger UI is accessible through the gateway:

| Service | URL |
|---------|-----|
| Subscription | `http://localhost:9000/docs/subscription/swagger-ui.html` |
| Branding | `http://localhost:9000/docs/branding/swagger-ui.html` |
| Admin | `http://localhost:9000/docs/admin/swagger-ui.html` |
| Email | `http://localhost:9000/docs/email/swagger-ui.html` |
