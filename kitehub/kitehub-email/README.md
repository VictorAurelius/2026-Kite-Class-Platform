# KiteHub Email Service

Transactional email service with 13 Thymeleaf templates, supporting SMTP (MailHog for dev), AWS SES (production), and mock mode.

## Tech Stack

- **Java 21** + Spring Boot 3.x
- **Thymeleaf** - HTML email templates
- **AWS SES** - Production email delivery
- **Spring Mail** - SMTP integration (MailHog for dev)
- **Micrometer + Prometheus** - Monitoring

## Ports

| Context | Port |
|---------|------|
| Standalone | `8084` |
| Docker (internal) | `8080` |
| Docker (host) | `8084` |

## Dependencies

- **MailHog** (dev) - `kite-mailhog:1025` (SMTP), `localhost:8025` (Web UI)
- **AWS SES** (prod) - Configured via environment variables

## Email Templates (13)

| Template | Trigger |
|----------|---------|
| `welcome` | New registration |
| `email-verification` | Email verification |
| `trial-midpoint` | 7 days into trial |
| `trial-expiration-warning` | 3 days before trial ends |
| `trial-expired` | Trial ended |
| `subscription-created` | New subscription |
| `subscription-renewal-reminder` | Before renewal |
| `subscription-expired` | Subscription ended |
| `subscription-suspended` | Account suspended |
| `onboarding-tips` | Onboarding sequence |
| `data-retention-warning` | Data deletion warning |
| `data-retention-final-warning` | Final data deletion warning |
| `data-deleted` | Data deleted confirmation |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_PROVIDER` | `mock` | `smtp`, `ses`, or `mock` |
| `SMTP_HOST` | `kite-mailhog` | SMTP server host |
| `SMTP_PORT` | `1025` | SMTP server port |
| `AWS_SES_REGION` | `ap-southeast-1` | AWS SES region |
| `AWS_SES_FROM_EMAIL` | `noreply@localhost` | Sender email |
| `AWS_SES_FROM_NAME` | `Local Dev Platform` | Sender name |
| `AWS_SES_MOCK_MODE` | `true` | Skip real SES in dev |

## API Overview

All endpoints prefixed with `/api/platform/emails/`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/send` | Send email via template |
| GET | `/templates` | List available templates |
| GET | `/logs` | Email delivery logs |

## Monitoring

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Links

- Business logic: [documents/01-business/kitehub/email-lifecycle.md](../../documents/01-business/kitehub/email-lifecycle.md)
- Architecture: [documents/02-architecture/](../../documents/02-architecture/)
- Quick start: [docs/QUICK-START.md](docs/QUICK-START.md)
