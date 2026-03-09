# KiteHub Subscription Service

**Version:** 1.0.0
**Purpose:** Instance provisioning, trial tracking, and subscription management

---

## Features

- **Instance Management**: Create and manage KiteClass instances
- **Trial System**: 14-day free trial with automatic expiration tracking
- **Pricing Tiers**: FREE, BASIC, PREMIUM, ENTERPRISE
- **Soft Delete**: Safe deletion with recovery option
- **Multi-Tenant Ready**: Foundation for database provisioning (PR 4.2)

---

## Quick Start

### 1. Start Infrastructure

```bash
cd kitehub
docker-compose -f docker-compose.kitehub.yml up -d kitehub-postgres
```

### 2. Build Project

```bash
cd kitehub
./mvnw clean install
```

### 3. Run Service

```bash
cd kitehub-subscription
../mvnw spring-boot:run
```

Service will start on `http://localhost:8081`

---

## API Endpoints

### Create Trial Instance

```bash
curl -X POST http://localhost:8081/api/platform/instances \
  -H "Content-Type: application/json" \
  -d '{
    "subdomain": "myschool",
    "organizationName": "My School",
    "ownerId": "550e8400-e29b-41d4-a716-446655440000",
    "tier": "BASIC"
  }'
```

### Get Instance by ID

```bash
curl http://localhost:8081/api/platform/instances/{id}
```

### Get Instance by Subdomain

```bash
curl http://localhost:8081/api/platform/instances/subdomain/myschool
```

### Delete Instance

```bash
curl -X DELETE http://localhost:8081/api/platform/instances/{id}
```

---

## Database Schema

```sql
CREATE TABLE instances (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(50) UNIQUE NOT NULL,
    custom_domain VARCHAR(255),
    organization_name VARCHAR(200) NOT NULL,
    owner_id UUID NOT NULL,
    tier VARCHAR(20) NOT NULL,              -- FREE, BASIC, PREMIUM, ENTERPRISE
    status VARCHAR(20) NOT NULL,            -- TRIAL, ACTIVE, SUSPENDED, DELETED
    database_url VARCHAR(500) NOT NULL,
    database_username VARCHAR(100) NOT NULL,
    database_password VARCHAR(255) NOT NULL,
    trial_started_at TIMESTAMP,
    trial_expires_at TIMESTAMP,
    subscription_id UUID,
    subscription_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);
```

---

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=InstanceServiceTest

# Run integration tests
./mvnw verify
```

**Test Coverage:** ≥80%

---

## Configuration

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/kitehub
    username: kitehub
    password: kitehub_dev_password

server:
  port: 8081
```

---

## Next Steps (PR 4.2)

- Database provisioning service
- Automatic PostgreSQL database creation
- Flyway migration runner for instance databases
- Kubernetes deployment integration

---

## Related Documentation

- [KiteHub Infrastructure Design](../documents/03-planning/infrastructure/kitehub-infrastructure.md)
- [Database Provisioning Design](../documents/03-planning/infrastructure/kitehub-database-provisioning.md)
- [PR 4.1 Specification](../documents/03-planning/prs/04-kitehub-prs.md)

---

**Last Updated:** 2026-03-09
