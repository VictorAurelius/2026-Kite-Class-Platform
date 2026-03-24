# Quick Start - KiteHub Email Service

## Prerequisites

- Java 21+
- Maven 3.9+ (or use `./mvnw`)
- MailHog running (for dev SMTP testing)

## Build

```bash
# From kitehub/ root
cd kitehub
./mvnw clean install -pl kitehub-email -am

# Skip tests
./mvnw clean install -pl kitehub-email -am -DskipTests
```

## Run Standalone

```bash
cd kitehub/kitehub-email
../mvnw spring-boot:run
```

Service starts on `http://localhost:8084`.

## Run with Docker

```bash
# Start full stack
cd kitehub
./scripts/up.sh

# Or rebuild just email service
./scripts/rebuild.sh kitehub-email
```

## Test

```bash
# Run all tests
cd kitehub
./mvnw test -pl kitehub-email

# Run specific test
./mvnw test -pl kitehub-email -Dtest=EmailServiceTest
```

## View Emails in MailHog

When running with Docker stack, MailHog Web UI is available at:

```
http://localhost:8025
```

All emails sent via SMTP provider in dev mode will appear here.

## Verify

```bash
# Health check
curl http://localhost:8084/actuator/health

# Via gateway
curl http://localhost:9000/api/platform/emails/templates
```

## Email Providers

| Provider | Config | Usage |
|----------|--------|-------|
| `mock` | `EMAIL_PROVIDER=mock` | Logs only, no actual send |
| `smtp` | `EMAIL_PROVIDER=smtp` | MailHog or real SMTP |
| `ses` | `EMAIL_PROVIDER=ses` | AWS SES (production) |

## API Documentation

Swagger UI: `http://localhost:8084/swagger-ui.html`

Via gateway: `http://localhost:9000/docs/email/swagger-ui.html`
