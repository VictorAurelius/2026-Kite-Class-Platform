# KiteHub Admin Service

Admin dashboard API for platform management: instance oversight, suspend/activate operations, revenue analytics, and system health monitoring.

## Tech Stack

- **Java 21** + Spring Boot 3.x
- **Spring Data JPA** - Database access
- **PostgreSQL** - Shared database
- **Testcontainers** - Integration testing
- **Micrometer + Prometheus** - Monitoring

## Ports

| Context | Port |
|---------|------|
| Standalone | `8083` (default in application.yml) |
| Docker (internal) | `8080` |
| Docker (host) | `8085` |

## Dependencies

- **PostgreSQL** - `kite-postgres:5432` (shared database)
- **Redis** - `kite-redis:6379` (caching)
- **kitehub-subscription** - Internal dependency for shared entities

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/kitehub` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `kitehub` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | - | DB password (required) |
| `SERVER_PORT` | `8083` | Server port |
| `ENCRYPTION_MASTER_KEY` | - | Encryption key (required) |
| `JWT_SECRET` | - | JWT signing secret (required) |

## API Overview

All endpoints prefixed with `/api/platform/admin/`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/instances` | List all instances (paginated) |
| GET | `/instances/{id}` | Get instance details |
| POST | `/instances/{id}/suspend` | Suspend instance |
| POST | `/instances/{id}/activate` | Activate instance |
| GET | `/revenue` | Revenue analytics |
| GET | `/stats` | Platform statistics |

## Monitoring

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Links

- Business logic: [documents/01-business/](../../documents/01-business/)
- Architecture: [documents/02-architecture/](../../documents/02-architecture/)
- Quick start: [docs/QUICK-START.md](docs/QUICK-START.md)
