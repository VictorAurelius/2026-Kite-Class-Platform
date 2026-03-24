# KiteHub Branding Service

AI-powered branding generation service for KiteClass instances. Analyzes logos, generates color themes, and provides template gallery for tenant landing pages.

## Tech Stack

- **Java 21** + Spring Boot 3.x
- **OpenAI GPT-4 Vision / DALL-E 3** (cloud) or **Ollama** (local)
- **PostgreSQL** - Branding job persistence
- **RabbitMQ** - Async job queue
- **AWS S3 / MinIO** - Asset storage
- **Spring WebFlux** - Non-blocking AI API calls

## Ports

| Context | Port |
|---------|------|
| Standalone | `8083` |
| Docker (internal) | `8080` |
| Docker (host) | `8083` |

## Dependencies

- **PostgreSQL** - `kite-postgres:5432` (shared database)
- **RabbitMQ** - `kite-rabbitmq:5672` (job queue)
- **MinIO / S3** - `kite-minio:9000` (asset storage)
- **Ollama** (optional) - `kite-ollama:11434` (local AI)

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5433/kitehub` | PostgreSQL connection |
| `DATABASE_USERNAME` | `kitehub` | DB username |
| `DATABASE_PASSWORD` | - | DB password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5673` | RabbitMQ port |
| `AI_PROVIDER` | `openai` | `openai` or `ollama` |
| `OPENAI_API_KEY` | - | OpenAI API key |
| `OLLAMA_BASE_URL` | `http://kite-ollama:11434` | Ollama endpoint |
| `S3_ENDPOINT` | - | MinIO/S3 endpoint |
| `S3_BUCKET` | `kitehub-assets` | Asset bucket |
| `CDN_DOMAIN` | `localhost:9100` | CDN/MinIO public URL |
| `S3_MOCK_MODE` | `true` | Skip real S3 in dev |

## API Overview

All endpoints prefixed with `/api/platform/branding/`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/analyze-logo` | Upload logo for AI analysis |
| POST | `/generate-theme` | Generate color theme from logo |
| GET | `/jobs/{id}` | Get branding job status |
| GET | `/templates` | List template gallery |

## Rate Limits (per day)

- FREE: 3 | BASIC: 10 | PREMIUM: 50 | ENTERPRISE: unlimited

## Monitoring

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Links

- Business logic: [documents/01-business/kitehub/ai-branding.md](../../documents/01-business/kitehub/ai-branding.md)
- Architecture: [documents/02-architecture/](../../documents/02-architecture/)
- Quick start: [docs/QUICK-START.md](docs/QUICK-START.md)
