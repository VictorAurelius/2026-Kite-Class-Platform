# KiteHub Gateway

API Gateway built with Spring Cloud Gateway. Provides unified entry point for all platform and tenant APIs with rate limiting, circuit breakers, CORS, and tenant routing.

## Tech Stack

- **Java 21** + Spring Boot 3.x
- **Spring Cloud Gateway** - Reactive API gateway
- **Resilience4j** - Circuit breaker pattern
- **Redis** - Rate limiting token bucket
- **Spring Data JPA** - Tenant/instance lookup
- **PostgreSQL** - Instance routing data

## Port

| Context | Port |
|---------|------|
| All contexts | `9000` |

## Dependencies

- **PostgreSQL** - `kite-postgres:5432` (instance lookup)
- **Redis** - `kite-redis:6379` (rate limiting)
- All backend services (routes traffic to them)

## Route Configuration

| Route | Target Service | Path Pattern |
|-------|---------------|--------------|
| Auth | kitehub-subscription:8080 | `/api/auth/**` |
| Instances | kitehub-subscription:8080 | `/api/platform/instances/**` |
| Subscriptions | kitehub-subscription:8080 | `/api/platform/subscriptions/**` |
| Payments | kitehub-subscription:8080 | `/api/platform/payments/**` |
| Config | kitehub-subscription:8080 | `/api/platform/config/**` |
| Branding | kitehub-branding:8080 | `/api/platform/branding/**` |
| Admin | kitehub-admin:8080 | `/api/platform/admin/**` |
| Email | kitehub-email:8080 | `/api/platform/emails/**` |
| KiteClass | kiteclass-core:8080 | `/api/v1/**` (with TenantResolver) |

## Rate Limits (requests/second by tier)

- FREE: 100 | BASIC: 500 | PREMIUM: 2,000 | ENTERPRISE: 10,000
- Auth registration: 3 req/s burst 5

## Circuit Breaker

All routes use Resilience4j circuit breakers:
- Sliding window: 10 requests
- Failure threshold: 50% (60% for instance routes)
- Wait in open state: 10s
- Fallback endpoints: `/fallback/{service}`

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `9000` | Gateway port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5433/kitehub` | PostgreSQL |
| `DATABASE_USERNAME` | `kitehub` | DB username |
| `DATABASE_PASSWORD` | - | DB password (required) |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6380` | Redis port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3001,http://localhost:3000` | CORS origins |
| `KITECLASS_CORE_URL` | `http://kiteclass-core:8080` | KiteClass core URL |
| `BASE_DOMAIN` | `.kitehub.me` | Tenant domain suffix |

## Monitoring

- Health: `/actuator/health`
- Gateway routes: `/actuator/gateway/routes`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Links

- Architecture: [documents/02-architecture/](../../documents/02-architecture/)
- Quick start: [docs/QUICK-START.md](docs/QUICK-START.md)
