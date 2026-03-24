# KiteHub Subscription Service - Detailed Documentation

> Archived from kitehub-subscription/README.md during docs standardization (Wave 8).
> For current README, see: `kitehub/kitehub-subscription/README.md`

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
- Suspend if expired (status -> SUSPENDED)
- Send email notifications

### Admin Operations

**Extend Trial (1-90 days):**
```bash
POST /api/platform/instances/{id}/extend-trial?days=7
```

**Convert to Subscription:**
```java
trialService.convertTrialToSubscription(instanceId);
// Status: TRIAL -> ACTIVE
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

---

## Subscription Management (PR 4.4)

### Pricing Tiers & Billing Cycles

**Monthly Pricing:**
- **BASIC**: 500,000 VND/month
- **PREMIUM**: 1,500,000 VND/month
- **ENTERPRISE**: 3,000,000 VND/month

**Annual Pricing (10% discount):**
- **BASIC**: 5,400,000 VND/year
- **PREMIUM**: 16,200,000 VND/year
- **ENTERPRISE**: 32,400,000 VND/year

### Subscription Lifecycle

**Status Flow:**
```
ACTIVE -> CANCELLED (manual cancel)
       -> EXPIRED (auto-expire when expiresAt passed)
       -> SUSPENDED (admin action)
```

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
```

### Tier Changes

**Upgrade (Immediate with Prorated Charge):**
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
proratedCharge = dailyRate x daysLeft
```

**Downgrade (End of Cycle):**
```bash
PATCH /api/platform/subscriptions/{id}/downgrade
Content-Type: application/json

{
  "newTier": "BASIC"
}
```

### Cancellation

**Cancel Immediately:**
```bash
DELETE /api/platform/subscriptions/{id}?immediate=true
```

**Cancel at End of Cycle (Default):**
```bash
DELETE /api/platform/subscriptions/{id}?immediate=false
```

### Business Rules & Validation

- Instance must exist and not have active subscription
- Cannot create subscription for FREE tier (trial only)
- Upgrade: New tier must be higher than current
- Downgrade: New tier must be lower than current
- Only ACTIVE subscriptions can be upgraded/downgraded

---

## Database Schema

**Total Tables:** 5

### V1: Instances Table

```sql
CREATE TABLE instances (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(50) UNIQUE NOT NULL,
    custom_domain VARCHAR(255),
    organization_name VARCHAR(200) NOT NULL,
    owner_id UUID NOT NULL,
    tier VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
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
    instance_id UUID NOT NULL,
    tier VARCHAR(20) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    price_vnd BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
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
    subscription_id UUID NOT NULL,
    amount_vnd BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
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
    instance_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress INTEGER DEFAULT 0,
    current_step VARCHAR(100),
    logo_url VARCHAR(500),
    organization_name VARCHAR(200) NOT NULL,
    assets_generated TEXT,
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
    instance_id UUID,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    message_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    queued_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE
);
```

### Code Examples

**Create Subscription from Trial:**
```java
CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
    .instanceId(instanceId)
    .tier(PricingTier.BASIC)
    .billingCycle(BillingCycle.MONTHLY)
    .autoRenew(true)
    .build();

SubscriptionResponse subscription = subscriptionService.createSubscription(request);
trialService.convertTrialToSubscription(instanceId);
```

**Upgrade with Prorated Charge:**
```java
UUID subscriptionId = subscription.getId();
PricingTier newTier = PricingTier.PREMIUM;
SubscriptionResponse upgraded = subscriptionService.upgradeSubscription(subscriptionId, newTier);
```

---

**Archived from README.md on 2026-03-24**
