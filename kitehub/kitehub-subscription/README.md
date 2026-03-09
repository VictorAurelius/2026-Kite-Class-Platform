# KiteHub Subscription Service

**Version:** 1.0.0
**Purpose:** Instance provisioning, trial tracking, and subscription management

---

## Features

- **Instance Management**: Create and manage KiteClass instances
- **Trial System**: 14-day free trial with automatic expiration tracking
- **Pricing Tiers**: FREE, BASIC, PREMIUM, ENTERPRISE
- **Soft Delete**: Safe deletion with recovery option
- **Database Provisioning** ⭐ NEW (PR 4.2):
  - Automatic database creation for each instance
  - Secure credential generation (32-char random passwords)
  - Dynamic connection pooling (HikariCP)
  - Tier-based pool limits (FREE: 5, BASIC: 10, PREMIUM: 20, ENTERPRISE: 50)
  - Scheduled daily backups (2:00 AM)
  - Weekly cleanup of deleted instances (30-day retention)
- **Trial Tracking & Expiration** 🆕 (PR 4.3):
  - Automatic 14-day trial period on instance creation
  - Daily expiration check (8:00 AM)
  - Trial extension (admin only, 1-90 days)
  - Warning levels: MEDIUM (2-3 days left), HIGH (1 day left), EXPIRED
  - Auto-suspend on expiration
  - Trial-to-subscription conversion

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

## Database Provisioning (PR 4.2)

### Automatic Provisioning

When a new instance is created, the system automatically:
1. Generates unique database name: `kiteclass_{uuid_short}`
2. Creates database credentials (32-char secure random password)
3. Updates instance with connection info

### Connection Pooling

**Tier-Based Limits:**
- FREE: 5 connections
- BASIC: 10 connections
- PREMIUM: 20 connections
- ENTERPRISE: 50 connections

**Pool Configuration (HikariCP):**
- Minimum idle: 50% of max pool size
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes
- Max lifetime: 30 minutes

**Example Usage:**
```java
@Autowired
private MultiTenantDataSourceConfig dataSourceConfig;

// Get DataSource for instance
DataSource ds = dataSourceConfig.getDataSource(instanceId);

// Close DataSource when instance deleted
dataSourceConfig.closeDataSource(instanceId);
```

### Scheduled Tasks

**Daily Backup (2:00 AM):**
- Backs up all ACTIVE instance databases
- TODO: Upload to S3 (s3://kiteclass-backups/)
- 7-day retention policy

**Weekly Cleanup (Sunday 3:00 AM):**
- Removes instances deleted > 30 days ago
- Permanent database deletion

---

## Trial Tracking & Expiration (PR 4.3)

### Automatic Trial Management

**Trial Period:**
- 14 days from instance creation
- Status: TRIAL
- Auto-starts with `instance.startTrial()`

**Trial Status Tracking:**
```java
@Autowired
private TrialService trialService;

// Get trial status
TrialStatusResponse status = trialService.getTrialStatus(instanceId);
// Returns: daysLeft, warningLevel, isOnTrial, etc.

// Check if expired
boolean expired = trialService.isTrialExpired(instanceId);
```

### Warning Levels

| Days Left | Warning Level | Action |
|-----------|--------------|--------|
| 4+ days | NONE | No warning |
| 2-3 days | MEDIUM | "Trial ending soon" |
| 1 day | HIGH | "Last day of trial" |
| 0 days | EXPIRED | "Trial expired" + Auto-suspend |

### Scheduled Expiration Check

**Daily at 8:00 AM:**
- Check all TRIAL instances
- Suspend if expired (status → SUSPENDED)
- TODO: Send email notifications (PR 4.12)
  - 3 days before: "Trial ending soon"
  - 1 day before: "Last day of trial"
  - On expiry: "Trial expired, please subscribe"

### Admin Operations

**Extend Trial (1-90 days):**
```bash
POST /api/platform/instances/{id}/extend-trial?days=7
```

**Convert to Subscription:**
```java
trialService.convertTrialToSubscription(instanceId);
// Status: TRIAL → ACTIVE
```

### API Endpoints

**Get Trial Status:**
```bash
GET /api/platform/instances/{id}/trial-status

Response:
{
  "instanceId": "uuid",
  "subdomain": "myschool",
  "status": "TRIAL",
  "isOnTrial": true,
  "trialStartedAt": "2026-03-01T00:00:00",
  "trialExpiresAt": "2026-03-15T00:00:00",
  "daysLeft": 7,
  "needsWarning": false,
  "warningLevel": "NONE"
}
```

**Extend Trial (Admin):**
```bash
POST /api/platform/instances/{id}/extend-trial?days=7
```

---

## Subscription Management (PR 4.4)

### Pricing Tiers & Billing Cycles

**Monthly Pricing:**
- **BASIC**: ₫500,000/month (500k VNĐ)
- **PREMIUM**: ₫1,500,000/month (1.5M VNĐ)
- **ENTERPRISE**: ₫3,000,000/month (3M VNĐ)

**Annual Pricing (10% discount):**
- **BASIC**: ₫5,400,000/year (saves ₫600k)
- **PREMIUM**: ₫16,200,000/year (saves ₫1.8M)
- **ENTERPRISE**: ₫32,400,000/year (saves ₫3.6M)

### Subscription Lifecycle

**Status Flow:**
```
ACTIVE → CANCELLED (manual cancel)
       → EXPIRED (auto-expire when expiresAt passed)
       → SUSPENDED (admin action)
```

**Auto-Renewal:**
- Default: `autoRenew = true`
- Renews automatically at end of billing cycle
- Can be disabled by user or on cancellation

### CRUD Operations

**Create Subscription:**
```bash
POST /api/platform/subscriptions
Content-Type: application/json

{
  "instanceId": "uuid",
  "tier": "BASIC",
  "billingCycle": "MONTHLY",
  "autoRenew": true
}

Response 201:
{
  "id": "uuid",
  "instanceId": "uuid",
  "tier": "BASIC",
  "billingCycle": "MONTHLY",
  "priceVnd": 500000,
  "status": "ACTIVE",
  "startedAt": "2026-03-09T10:00:00",
  "expiresAt": "2026-04-09T10:00:00",
  "autoRenew": true,
  "isActive": true,
  "isExpired": false
}
```

**Get Subscription by ID:**
```bash
GET /api/platform/subscriptions/{id}
```

**Get Active Subscription for Instance:**
```bash
GET /api/platform/subscriptions/instance/{instanceId}/active
```

**Get All Subscriptions for Instance:**
```bash
GET /api/platform/subscriptions/instance/{instanceId}
```

### Tier Changes

**Upgrade (Immediate with Prorated Charge):**

Upgrade happens immediately. Customer pays prorated charge for remaining days.

```bash
PATCH /api/platform/subscriptions/{id}/upgrade
Content-Type: application/json

{
  "newTier": "PREMIUM"
}
```

**Prorated Charge Formula:**
```
priceDifference = newTierPrice - oldTierPrice
dailyRate = priceDifference / cycleDays (30 for monthly, 365 for annual)
proratedCharge = dailyRate × daysLeft

Example:
- Current: BASIC (₫500k/month)
- Upgrade to: PREMIUM (₫1.5M/month)
- Days left: 15 days
- Calculation: (1,500,000 - 500,000) / 30 × 15 = ₫500,000
```

**Downgrade (End of Cycle):**

Downgrade happens at end of current billing cycle. No refund for current period.

```bash
PATCH /api/platform/subscriptions/{id}/downgrade
Content-Type: application/json

{
  "newTier": "BASIC"
}
```

**Note:** MVP implementation applies downgrade immediately (with log warning). Production should defer until cycle end.

### Cancellation

**Cancel Immediately:**
```bash
DELETE /api/platform/subscriptions/{id}?immediate=true
```
- Sets `expiresAt` to now
- Status → CANCELLED
- Instance access revoked immediately

**Cancel at End of Cycle (Default):**
```bash
DELETE /api/platform/subscriptions/{id}?immediate=false
```
- Sets `autoRenew = false`
- Status → CANCELLED
- Access continues until `expiresAt`
- No charge at renewal

### Business Rules & Validation

**Subscription Creation:**
- ✅ Instance must exist and not have active subscription
- ✅ Cannot create subscription for FREE tier (trial only)
- ✅ Price calculated based on tier + billing cycle
- ✅ Instance status updated to ACTIVE
- ✅ Trial period ended if upgrading from trial

**Tier Changes:**
- ✅ Upgrade: New tier must be higher than current (ordinal comparison)
- ✅ Downgrade: New tier must be lower than current
- ✅ Only ACTIVE subscriptions can be upgraded/downgraded
- ❌ Cannot upgrade FREE → BASIC (must create new subscription)

**Cancellation:**
- ✅ Can cancel ACTIVE subscriptions
- ✅ Already CANCELLED subscriptions are no-op (logged)
- ✅ Immediate cancel affects instance access immediately
- ✅ End-of-cycle cancel allows grace period

### Code Examples

**Create Subscription from Trial:**
```java
@Autowired
private SubscriptionService subscriptionService;
@Autowired
private TrialService trialService;

// Convert trial to paid subscription
CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
    .instanceId(instanceId)
    .tier(PricingTier.BASIC)
    .billingCycle(BillingCycle.MONTHLY)
    .autoRenew(true)
    .build();

SubscriptionResponse subscription = subscriptionService.createSubscription(request);

// Trial automatically ended
trialService.convertTrialToSubscription(instanceId);
```

**Upgrade with Prorated Charge:**
```java
// User wants to upgrade from BASIC to PREMIUM
UUID subscriptionId = subscription.getId();
PricingTier newTier = PricingTier.PREMIUM;

SubscriptionResponse upgraded = subscriptionService.upgradeSubscription(subscriptionId, newTier);

// Prorated charge calculated and logged
// TODO (PR 4.6): Create payment record for prorated charge
```

**Check Subscription Status:**
```java
@Autowired
private SubscriptionRepository subscriptionRepository;

Optional<Subscription> activeSub = subscriptionRepository.findActiveByInstanceId(instanceId);

if (activeSub.isPresent()) {
    Subscription sub = activeSub.get();
    boolean active = sub.isActive();  // status == ACTIVE
    boolean expired = sub.isExpired(); // expiresAt < now
}
```

### Integration with Other Components

**Trial → Subscription Flow:**
1. User starts 14-day trial (PR 4.3)
2. User decides to subscribe before trial expires
3. POST /api/platform/subscriptions (creates paid subscription)
4. `SubscriptionService.createSubscription()` updates instance:
   - Status: TRIAL → ACTIVE
   - Sets `subscriptionId` and `subscriptionExpiresAt`
5. Trial period ended, full access granted

**Payment Integration (PR 4.6 - TODO):**
- Create Payment record on subscription creation
- Create Payment record for prorated charge on upgrade
- Link subscription renewals to payment records
- Handle payment failures (suspend subscription)

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
