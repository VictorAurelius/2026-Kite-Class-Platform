# KiteHub Subscription Service

Core platform service handling instance provisioning, trial management, subscription billing, and payment processing for the KiteHub multi-tenant SaaS platform.

## Tech Stack

- **Java 21** + Spring Boot 3.x
- **Spring Data JPA** + Flyway migrations
- **PostgreSQL** - Primary database
- **Redis** - Caching and session management
- **RabbitMQ** - Event publishing (email triggers, branding jobs)
- **Micrometer + Prometheus** - Monitoring

## Ports

| Context | Port |
|---------|------|
| Standalone | `8081` |
| Docker (internal) | `8080` |
| Docker (host) | `8081` |

## Dependencies

- **PostgreSQL** - `kite-postgres:5432`
- **Redis** - `kite-redis:6379`
- **RabbitMQ** - `kite-rabbitmq:5672`

## Features

- **Instance Management** - Create, manage, soft-delete KiteClass instances
- **Trial System** - 14-day free trial with expiration tracking and auto-suspend
- **Subscription Billing** - BASIC/PREMIUM/ENTERPRISE tiers, monthly/annual cycles
- **Payment Processing** - VietQR, MoMo, VNPay, bank transfer
- **Database Provisioning** - Auto-create tenant databases with connection pooling
- **Custom Domains** - DNS verification for tenant custom domains

## API Overview

### Instance APIs (`/api/platform/instances/`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create trial instance |
| GET | `/{id}` | Get instance by ID |
| GET | `/subdomain/{subdomain}` | Get by subdomain |
| DELETE | `/{id}` | Soft delete instance |
| GET | `/{id}/trial-status` | Get trial status |
| POST | `/{id}/extend-trial` | Extend trial (admin) |

### Subscription APIs (`/api/platform/subscriptions/`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create subscription |
| GET | `/{id}` | Get subscription |
| GET | `/instance/{id}/active` | Active subscription |
| PATCH | `/{id}/upgrade` | Upgrade tier |
| PATCH | `/{id}/downgrade` | Downgrade tier |
| DELETE | `/{id}` | Cancel subscription |

### Payment APIs (`/api/platform/payments/`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create payment |
| GET | `/{id}` | Get payment status |
| GET | `/subscription/{id}` | Payment history |

### Auth APIs (`/api/auth/`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new user |
| POST | `/login` | Login |
| POST | `/refresh` | Refresh token |
| GET | `/me` | Current user |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/kitehub` | PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `kitehub` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | - | DB password (required) |
| `SPRING_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_REDIS_PORT` | `6380` | Redis port |
| `SPRING_RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `ENCRYPTION_MASTER_KEY` | - | Encryption key (required) |
| `JWT_SECRET` | - | JWT signing secret (required) |

## Configuration

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/kitehub
    username: kitehub
```

## Testing

```bash
cd kitehub
./mvnw test -pl kitehub-subscription
./mvnw test -pl kitehub-subscription -Dtest=InstanceServiceTest
```

## Monitoring

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Links

- Business logic: [documents/01-business/kitehub/](../../documents/01-business/kitehub/)
- Architecture: [documents/02-architecture/](../../documents/02-architecture/)
- Detailed docs (archived): [documents/07-archived/kiteclass-legacy-docs/kitehub-subscription-detailed.md](../../documents/07-archived/kiteclass-legacy-docs/kitehub-subscription-detailed.md)
- Quick start: [docs/QUICK-START.md](docs/QUICK-START.md)
