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

**Total Tables:** 5 (created upfront in PR 4.1 following best practice)

### V1: Instances Table

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

### V2: Subscriptions Table

```sql
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    instance_id UUID NOT NULL,             -- FK to instances
    tier VARCHAR(20) NOT NULL,             -- FREE, BASIC, PREMIUM, ENTERPRISE
    billing_cycle VARCHAR(20) NOT NULL,    -- MONTHLY, ANNUALLY
    price_vnd BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,           -- ACTIVE, SUSPENDED, CANCELLED, EXPIRED
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);
```

### V3: Payments Table

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,         -- FK to subscriptions
    amount_vnd BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    payment_method VARCHAR(30) NOT NULL,   -- VIETQR, MOMO, VNPAY, BANK_TRANSFER
    status VARCHAR(20) NOT NULL,           -- PENDING, COMPLETED, FAILED, REFUNDED
    qr_code_url VARCHAR(500),
    transaction_id VARCHAR(100),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);
```

### V4: Branding Jobs Table

```sql
CREATE TABLE branding_jobs (
    id UUID PRIMARY KEY,
    instance_id UUID NOT NULL,             -- FK to instances
    status VARCHAR(20) NOT NULL,           -- QUEUED, PROCESSING, COMPLETED, FAILED
    progress INTEGER DEFAULT 0,            -- 0-100
    current_step VARCHAR(100),
    logo_url VARCHAR(500),
    organization_name VARCHAR(200) NOT NULL,
    assets_generated TEXT,                 -- JSON array of generated assets
    error_message TEXT,
    queued_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);
```

### V5: Email Logs Table

```sql
CREATE TABLE email_logs (
    id UUID PRIMARY KEY,
    instance_id UUID,                      -- Nullable: platform emails have no instance
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100) NOT NULL,   -- welcome, trial-ending, payment-confirmation
    message_id VARCHAR(255),               -- AWS SES Message ID
    status VARCHAR(20) NOT NULL,           -- QUEUED, SENT, DELIVERED, BOUNCED, COMPLAINED
    queued_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);
```

**Schema Design:**
- All tables created upfront (V1-V5) before feature development
- Foreign key constraints enforce referential integrity
- Indexes on frequently queried columns (subdomain, status, FK columns)
- Soft delete pattern with `deleted` boolean flag
- Audit fields: `created_at`, `updated_at`, `created_by`, `updated_by`

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
