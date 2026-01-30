# Deployment Quality Standards

**Version:** 1.0.0
**Last Updated:** 2026-01-30
**Status:** Production Standard

---

## Table of Contents

1. [Overview](#overview)
2. [Database Migration Testing](#database-migration-testing)
3. [Zero-Downtime Deployment](#zero-downtime-deployment)
4. [Feature Flag Management](#feature-flag-management)
5. [Payment System Deployment](#payment-system-deployment)
6. [AI Service Deployment](#ai-service-deployment)
7. [Rollback Strategies](#rollback-strategies)
8. [Smoke Testing](#smoke-testing)
9. [Deployment Checklist](#deployment-checklist)

---

## Overview

### Purpose

This document defines **deployment quality standards** for KiteClass Platform, ensuring:

- **Zero-downtime deployments** for production
- **Safe database migrations** (idempotent, rollback-ready)
- **Feature flag management** for gradual rollouts
- **Payment system safety** (no financial data loss)
- **AI service resilience** (graceful degradation on failure)
- **Fast rollback** capability (< 5 minutes)

### Deployment Environments

| Environment | Purpose | Deployment Trigger | Approval Required |
|-------------|---------|-------------------|-------------------|
| **Local** | Development | Manual | No |
| **Staging** | QA/Testing | Auto (main branch) | No |
| **Production** | Live users | Manual (tagged release) | ✅ Yes |

---

## Database Migration Testing

### Migration Principles

1. **Idempotency** - Migrations can run multiple times safely
2. **Backward Compatibility** - New code works with old schema during rollout
3. **Forward Compatibility** - Old code works with new schema during rollback
4. **No Data Loss** - Never drop columns/tables without retention period
5. **Transactional** - Use transactions where possible (DDL support varies by DB)

### Flyway Configuration

```properties
# application.properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=false
spring.flyway.clean-disabled=true
```

### Migration Naming Convention

```
V{version}__{description}.sql

Examples:
V1__initial_schema.sql
V2__add_trial_system.sql
V3__add_payment_tables.sql
V4__add_ai_branding_job_table.sql
```

### Idempotent Migration Example

**❌ BAD (Not Idempotent):**
```sql
-- V5__add_tier_column.sql
ALTER TABLE instance_config ADD COLUMN tier VARCHAR(20) NOT NULL;
```

**Problem:** Fails if run twice (column already exists)

**✅ GOOD (Idempotent):**
```sql
-- V5__add_tier_column.sql
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'instance_config'
        AND column_name = 'tier'
    ) THEN
        ALTER TABLE instance_config ADD COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'TRIAL';
    END IF;
END $$;
```

### Backward-Compatible Migration Example

**Scenario:** Rename column `trial_end_date` → `subscription_valid_until`

**Step 1: Add new column (backward compatible)**
```sql
-- V6__add_subscription_valid_until.sql
ALTER TABLE instance_config
ADD COLUMN subscription_valid_until TIMESTAMP;

-- Copy existing data
UPDATE instance_config
SET subscription_valid_until = trial_end_date
WHERE trial_end_date IS NOT NULL;
```

**Deploy Step 1** → New code reads `subscription_valid_until`, old code still reads `trial_end_date`

**Step 2: Deprecate old column (after new code deployed everywhere)**
```sql
-- V7__deprecate_trial_end_date.sql
-- Mark as deprecated (keep for 1 release cycle)
COMMENT ON COLUMN instance_config.trial_end_date IS 'DEPRECATED: Use subscription_valid_until';
```

**Step 3: Drop old column (after 1 release cycle)**
```sql
-- V8__drop_trial_end_date.sql
ALTER TABLE instance_config
DROP COLUMN trial_end_date;
```

### Migration Testing Checklist

```yaml
# Test each migration before deploying

1. Test on clean database:
   - ✅ Migration runs successfully
   - ✅ All indexes created
   - ✅ All constraints applied

2. Test idempotency:
   - ✅ Run migration twice
   - ✅ No errors on second run

3. Test with existing data:
   - ✅ Seed database with production-like data
   - ✅ Run migration
   - ✅ Verify data integrity

4. Test rollback:
   - ✅ Run migration
   - ✅ Rollback to previous version
   - ✅ Verify application still works

5. Test performance:
   - ✅ Large table migrations complete in < 5 minutes
   - ✅ No table locks longer than 10 seconds
```

### Automated Migration Testing

```java
// src/test/java/com/kiteclass/migration/MigrationTest.java
@SpringBootTest
@Testcontainers
public class MigrationTest {

    @Container
    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("kiteclass_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void migrationsRunSuccessfully() {
        // Flyway migrations run on app startup
        // If we reach here, migrations succeeded
        assertTrue(true);
    }

    @Test
    void migrationIsIdempotent() throws Exception {
        // Run migrations twice
        Flyway flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .load();

        flyway.migrate();
        MigrateResult result = flyway.migrate(); // Run again

        assertEquals(0, result.migrationsExecuted); // No new migrations
    }

    @Test
    void migrationPreservesExistingData() {
        // Insert test data
        jdbcTemplate.update(
            "INSERT INTO instance_config (instance_id, tier) VALUES (?, ?)",
            UUID.randomUUID(), "TRIAL"
        );

        // Run migrations
        Flyway flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .load();

        flyway.migrate();

        // Verify data still exists
        int count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM instance_config",
            Integer.class
        );

        assertEquals(1, count);
    }
}
```

---

## Zero-Downtime Deployment

### Blue-Green Deployment Strategy

**Architecture:**
```
Load Balancer (NGINX)
    ├── Blue Environment (Current Production)
    │   ├── Backend Pod 1
    │   ├── Backend Pod 2
    │   └── Frontend Pod
    └── Green Environment (New Version)
        ├── Backend Pod 1
        ├── Backend Pod 2
        └── Frontend Pod
```

**Deployment Steps:**

1. **Deploy to Green Environment**
   ```bash
   kubectl apply -f k8s/green-deployment.yml
   ```

2. **Run Health Checks on Green**
   ```bash
   curl https://green.kiteclass.internal/actuator/health
   # Expected: {"status": "UP"}
   ```

3. **Run Smoke Tests on Green**
   ```bash
   npm run test:smoke -- --env=green
   ```

4. **Switch Traffic to Green (gradual)**
   ```nginx
   # NGINX config - 10% traffic to green
   upstream backend {
       server blue-backend:8080 weight=9;
       server green-backend:8080 weight=1;
   }
   ```

5. **Monitor Metrics (5 minutes)**
   - Error rate < 0.1%
   - Response time < 500ms
   - No increase in 5xx errors

6. **Switch 100% Traffic to Green**
   ```nginx
   upstream backend {
       server green-backend:8080;
   }
   ```

7. **Keep Blue Running (1 hour)**
   - Fast rollback capability
   - Monitor for issues

8. **Shut Down Blue**
   ```bash
   kubectl delete -f k8s/blue-deployment.yml
   ```

### Rolling Update Strategy (Kubernetes)

```yaml
# k8s/backend-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kiteclass-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # Create 1 extra pod during update
      maxUnavailable: 0  # Never have 0 pods available
  template:
    spec:
      containers:
      - name: backend
        image: kiteclass/backend:v1.2.3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
```

**Deployment Process:**
1. Pod 1 (old version) remains running
2. Pod 4 (new version) starts
3. Readiness probe passes on Pod 4
4. Traffic routed to Pod 4
5. Pod 1 (old version) shut down
6. Repeat for Pods 2 and 3

**Timeline:** ~5 minutes for full rollout (3 pods × 90 seconds each)

### Database Connection Pooling During Deployment

```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**Graceful Shutdown:**
```java
@Component
public class GracefulShutdownConfig {

    @PreDestroy
    public void onShutdown() {
        logger.info("Shutting down gracefully...");

        // 1. Stop accepting new requests
        tomcat.getConnector().pause();

        // 2. Wait for existing requests to complete (max 30 seconds)
        Thread.sleep(30000);

        // 3. Close database connections
        dataSource.close();

        logger.info("Shutdown complete");
    }
}
```

---

## Feature Flag Management

### LaunchDarkly Integration

```java
// src/main/java/com/kiteclass/config/FeatureFlagConfig.java
@Configuration
public class FeatureFlagConfig {

    @Bean
    public LDClient ldClient(@Value("${launchdarkly.sdk-key}") String sdkKey) {
        return new LDClient(sdkKey);
    }
}

// Usage in service
@Service
public class PaymentService {

    @Autowired
    private LDClient ldClient;

    public PaymentOrder createOrder(UUID instanceId, PricingTier tier) {
        // Feature flag: vietqr-v2-api
        boolean useVietQRv2 = ldClient.boolVariation(
            "vietqr-v2-api",
            LDContext.builder(instanceId.toString()).build(),
            false // Default: use v1
        );

        if (useVietQRv2) {
            return vietQRServiceV2.createOrder(instanceId, tier);
        } else {
            return vietQRServiceV1.createOrder(instanceId, tier);
        }
    }
}
```

### Feature Flag Rollout Strategy

**Stage 1: Internal Testing (0%)**
```json
{
  "flag": "vietqr-v2-api",
  "enabled": true,
  "targeting": [
    {
      "values": ["test-instance-1", "test-instance-2"],
      "variation": true
    }
  ],
  "fallthrough": false
}
```

**Stage 2: Canary (5%)**
```json
{
  "flag": "vietqr-v2-api",
  "enabled": true,
  "targeting": [],
  "rollout": {
    "variations": [
      { "variation": true, "weight": 5000 },   // 5%
      { "variation": false, "weight": 95000 }  // 95%
    ]
  }
}
```

**Stage 3: Gradual Rollout (25% → 50% → 100%)**
```json
// Day 1: 25%
{ "variation": true, "weight": 25000 }

// Day 3: 50%
{ "variation": true, "weight": 50000 }

// Day 7: 100%
{ "variation": true, "weight": 100000 }
```

**Stage 4: Cleanup (remove old code)**
```java
// After 100% rollout for 2 weeks, remove flag
public PaymentOrder createOrder(UUID instanceId, PricingTier tier) {
    return vietQRServiceV2.createOrder(instanceId, tier);
}
```

### Feature Flag Best Practices

**DO ✅:**
- Use feature flags for **risky changes** (payment, AI, multi-tenant)
- Set **default to safe value** (old behavior)
- **Monitor metrics** for flagged vs unflagged users
- **Remove flags** after 100% rollout (technical debt)
- Use **instance-based targeting** for multi-tenant

**DON'T ❌:**
- Don't use flags for **simple changes** (UI text, colors)
- Don't keep flags **indefinitely** (max 3 months)
- Don't create **nested flags** (hard to reason about)
- Don't forget to **test both branches** (flag on/off)

---

## Payment System Deployment

### Critical Safety Measures

1. **No Data Loss**
   - All payment orders persisted to database BEFORE API call
   - Transaction logs immutable (append-only)
   - VietQR callbacks idempotent

2. **No Double Charging**
   - Order status check before processing
   - Unique constraint on `order_id`
   - Payment verification checks existing payments

3. **Rollback Safety**
   - Old code can read new schema (backward compatibility)
   - New code can read old schema (forward compatibility)
   - Payment verification API versioned (`/v1/verify`, `/v2/verify`)

### Deployment Steps for Payment Changes

**Example: Upgrade VietQR API from v1 to v2**

**Step 1: Add v2 Implementation (feature-flagged)**
```java
@Service
public class VietQRServiceV2 {
    public String generateQRCode(PaymentOrder order) {
        // New implementation using VietQR v2 API
    }
}
```

**Step 2: Deploy with Flag OFF (0%)**
```bash
# Staging deployment
kubectl apply -f k8s/backend-deployment.yml

# Verify v1 still works
curl https://staging.kiteclass.com/api/payments/orders \
  -d '{"tier": "STANDARD"}'

# Response should use v1 QR format
```

**Step 3: Enable Flag for Test Instances (1%)**
```json
{
  "flag": "vietqr-v2-api",
  "targeting": [
    { "values": ["test-instance-1"], "variation": true }
  ]
}
```

**Step 4: Test Payment Flow End-to-End**
```bash
# 1. Create order
ORDER_ID=$(curl -X POST https://test-instance-1.kiteclass.com/api/payments/orders \
  -d '{"tier": "STANDARD"}' | jq -r '.orderId')

# 2. Simulate payment
curl -X POST https://api.kiteclass.com/api/test/simulate-payment \
  -d "{\"orderId\": \"$ORDER_ID\", \"status\": \"PAID\"}"

# 3. Verify tier upgraded
curl https://test-instance-1.kiteclass.com/api/instance/config | jq '.tier'
# Expected: "STANDARD"
```

**Step 5: Monitor Metrics (24 hours)**
- Payment success rate: ≥ 99.5%
- Payment verification latency: < 2 seconds
- Error rate: < 0.1%

**Step 6: Gradual Rollout (5% → 25% → 100%)**
```bash
# Day 1: 5%
launchdarkly update-flag vietqr-v2-api --rollout 5

# Day 3: 25%
launchdarkly update-flag vietqr-v2-api --rollout 25

# Day 7: 100%
launchdarkly update-flag vietqr-v2-api --rollout 100
```

**Step 7: Remove Flag (after 2 weeks at 100%)**
```java
// Delete VietQRServiceV1
// Delete feature flag check
public String generateQRCode(PaymentOrder order) {
    return vietQRServiceV2.generateQRCode(order);
}
```

### Payment Monitoring During Deployment

```yaml
# Prometheus alerts
- alert: PaymentSuccessRateDrop
  expr: |
    rate(payment_orders_success[5m]) /
    rate(payment_orders_total[5m]) < 0.995
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Payment success rate dropped below 99.5%"

- alert: PaymentVerificationLatencyHigh
  expr: |
    histogram_quantile(0.95,
      rate(payment_verification_duration_seconds_bucket[5m])
    ) > 2
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Payment verification P95 latency > 2s"
```

**Rollback Trigger:**
If payment success rate drops below 99%, immediately rollback:
```bash
# Disable feature flag
launchdarkly update-flag vietqr-v2-api --rollout 0

# Or rollback deployment
kubectl rollout undo deployment/kiteclass-backend
```

---

## AI Service Deployment

### Graceful Degradation Strategy

**Principle:** AI failures should NOT block core functionality

**Example: AI Branding Logo Generation**

```java
@Service
public class AIBrandingService {

    @CircuitBreaker(name = "openai", fallbackMethod = "fallbackGenerate")
    @Retry(name = "openai")
    public AIBrandingJob generateLogo(String schoolName, String style) {
        // Call OpenAI API (GPT-4 + DALL-E)
        return openAIClient.generateLogo(schoolName, style);
    }

    // Fallback: Use template-based logo
    private AIBrandingJob fallbackGenerate(String schoolName, String style, Exception e) {
        logger.warn("OpenAI unavailable, using template fallback", e);

        return AIBrandingJob.builder()
            .status(JobStatus.COMPLETED)
            .result(AIBrandingResult.builder()
                .logoUrl(templateLogoService.generate(schoolName, style))
                .concept("Template-based logo (AI service unavailable)")
                .build())
            .build();
    }
}
```

**Resilience4j Configuration:**
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      openai:
        failureRateThreshold: 50          # Open circuit if 50% fail
        slowCallRateThreshold: 50         # Open if 50% slow
        slowCallDurationThreshold: 10s    # "Slow" = > 10 seconds
        waitDurationInOpenState: 60s      # Stay open for 60s
        permittedNumberOfCallsInHalfOpenState: 3
        slidingWindowSize: 10

  retry:
    instances:
      openai:
        maxAttempts: 3
        waitDuration: 2s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - org.springframework.web.client.ResourceAccessException
          - java.net.SocketTimeoutException
```

### AI Service Deployment Steps

**Step 1: Deploy with Circuit Breaker OPEN (0% AI calls)**
```yaml
resilience4j.circuitbreaker.instances.openai.forceOpen: true
```

**Result:** All requests use fallback (template logos)

**Step 2: Close Circuit Breaker Gradually (10% → 50% → 100%)**
```yaml
# Use feature flag to control circuit breaker
resilience4j.circuitbreaker.instances.openai.forceOpen: ${launchdarkly.ai-enabled:true}
```

**Step 3: Monitor AI Service Health**
```yaml
# Prometheus metrics
- aibranding_jobs_total{status="success"}
- aibranding_jobs_total{status="failed"}
- aibranding_generation_duration_seconds
- openai_circuit_breaker_state{state="open|closed|half_open"}
```

**Step 4: Verify Cost Controls**
```java
@Service
public class OpenAICostGuard {

    private static final int MAX_REQUESTS_PER_HOUR = 100;
    private static final int MAX_COST_PER_DAY_USD = 50;

    @Before("execution(* OpenAIClient.generateLogo(..))")
    public void checkCostLimits() {
        int requestsThisHour = redisTemplate.opsForValue()
            .increment("openai:requests:hour:" + getCurrentHour());

        if (requestsThisHour > MAX_REQUESTS_PER_HOUR) {
            throw new QuotaExceededException("OpenAI hourly quota exceeded");
        }

        double costToday = getCostToday();
        if (costToday > MAX_COST_PER_DAY_USD) {
            throw new BudgetExceededException("OpenAI daily budget exceeded");
        }
    }
}
```

---

## Rollback Strategies

### Fast Rollback (< 5 minutes)

**Option 1: Kubernetes Rollback**
```bash
# Rollback to previous deployment
kubectl rollout undo deployment/kiteclass-backend

# Verify rollback
kubectl rollout status deployment/kiteclass-backend
# Expected: "deployment successfully rolled out"

# Check pod versions
kubectl get pods -o jsonpath='{.items[*].spec.containers[*].image}'
# Expected: previous image tag
```

**Option 2: Feature Flag Disable**
```bash
# Disable problematic feature
launchdarkly update-flag new-payment-flow --enabled false

# Verify instantly applied (no restart needed)
```

**Option 3: Blue-Green Swap**
```nginx
# NGINX config - switch back to blue
upstream backend {
    server blue-backend:8080;
}

# Reload NGINX
nginx -s reload
```

### Database Rollback (Schema Changes)

**Scenario:** Migration adds column, new code depends on it

**Problem:** Cannot simply rollback deployment (new code needs new schema)

**Solution: Two-Phase Rollback**

**Phase 1: Deploy Hotfix (read from old column)**
```java
// Hotfix: Make new code read from old column temporarily
@Column(name = "trial_end_date") // Changed from subscription_valid_until
private LocalDateTime subscriptionValidUntil;
```

**Phase 2: Rollback Migration**
```sql
-- V9__rollback_subscription_valid_until.sql
ALTER TABLE instance_config
DROP COLUMN subscription_valid_until;
```

**Better Approach: Avoid DB Rollbacks**
- Use backward-compatible migrations (keep old + new columns)
- Deprecate old columns after full rollout
- Never drop columns in same release as adding them

---

## Smoke Testing

### Post-Deployment Smoke Tests

**Purpose:** Verify critical flows work after deployment

```typescript
// scripts/smoke-test.ts
import { test, expect } from '@playwright/test';

test.describe('Smoke Tests', () => {
  test('health check endpoint returns OK', async ({ request }) => {
    const response = await request.get('/actuator/health');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.status).toBe('UP');
  });

  test('guest landing page loads', async ({ page }) => {
    await page.goto('https://demo.kiteclass.com');
    await expect(page.locator('h1')).toBeVisible();
  });

  test('login flow works', async ({ page }) => {
    await page.goto('https://demo.kiteclass.com/login');
    await page.fill('input[name="email"]', 'smoke-test@kiteclass.com');
    await page.fill('input[name="password"]', 'TestPass123!');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Dashboard')).toBeVisible();
  });

  test('create student works', async ({ request }) => {
    const response = await request.post('/api/students', {
      data: {
        name: 'Smoke Test Student',
        email: `smoke-${Date.now()}@test.com`,
      },
      headers: {
        'Authorization': `Bearer ${process.env.SMOKE_TEST_TOKEN}`,
      },
    });

    expect(response.status()).toBe(201);
  });

  test('payment order creation works', async ({ request }) => {
    const response = await request.post('/api/payments/orders', {
      data: { tier: 'STANDARD' },
      headers: {
        'Authorization': `Bearer ${process.env.SMOKE_TEST_TOKEN}`,
      },
    });

    expect(response.status()).toBe(201);

    const body = await response.json();
    expect(body.orderId).toMatch(/ORD-\d{8}-[A-Z0-9]+/);
    expect(body.qrCodeUrl).toContain('vietqr.io');
  });
});
```

**Run After Deployment:**
```bash
# Staging
npm run smoke-test -- --env=staging

# Production
npm run smoke-test -- --env=production
```

**Rollback Trigger:** If any smoke test fails, immediately rollback.

---

## Deployment Checklist

### Pre-Deployment Checklist

```markdown
## Pre-Deployment Checklist

- [ ] All CI/CD quality gates passed (tests, coverage, security)
- [ ] Database migrations tested on staging
- [ ] Database migration is idempotent
- [ ] Database migration is backward compatible
- [ ] Feature flags configured (default: safe value)
- [ ] Rollback plan documented
- [ ] Smoke tests passing on staging
- [ ] Performance benchmarks show no regression
- [ ] Security scan shows no HIGH/CRITICAL vulnerabilities
- [ ] Multi-tenant isolation tests passed
- [ ] Payment flow tested end-to-end (if applicable)
- [ ] AI service fallback tested (if applicable)
- [ ] Deployment announcement sent to team
- [ ] Deployment time scheduled (low-traffic window)
- [ ] On-call engineer notified
```

### During Deployment Checklist

```markdown
## During Deployment

- [ ] Monitor error rate (< 0.1% threshold)
- [ ] Monitor response time (< 500ms P95)
- [ ] Monitor database connections (< 80% pool usage)
- [ ] Monitor payment success rate (≥ 99.5%)
- [ ] Monitor AI service success rate (≥ 90%)
- [ ] Watch Slack #deployments channel for alerts
- [ ] Verify health checks passing
- [ ] Run smoke tests
```

### Post-Deployment Checklist

```markdown
## Post-Deployment

- [ ] All smoke tests passed
- [ ] Error rate normal (< 0.1%)
- [ ] Response time normal (P95 < 500ms)
- [ ] No spike in 5xx errors
- [ ] Payment success rate ≥ 99.5%
- [ ] Database migration completed successfully
- [ ] Old deployment kept running for 1 hour (rollback safety)
- [ ] Feature flags updated (if applicable)
- [ ] Deployment tagged in git
- [ ] Deployment documented in changelog
- [ ] Team notified of successful deployment
```

---

## Best Practices Summary

### DO ✅

1. **Use blue-green or rolling deployments** for zero downtime
2. **Test database migrations** on staging with production-like data
3. **Make migrations idempotent** and backward compatible
4. **Use feature flags** for risky changes (payment, AI)
5. **Monitor metrics** during deployment (error rate, latency)
6. **Run smoke tests** after every deployment
7. **Keep rollback capability** for at least 1 hour
8. **Document rollback plan** before deploying
9. **Deploy during low-traffic windows** (2-6 AM local time)
10. **Use gradual rollouts** for high-risk changes

### DON'T ❌

1. **Don't deploy on Fridays** (reduced support over weekend)
2. **Don't skip smoke tests** (catches obvious breakage)
3. **Don't drop database columns** in same release as adding them
4. **Don't deploy payment changes** without thorough testing
5. **Don't disable feature flags permanently** (remove code instead)
6. **Don't deploy without rollback plan**
7. **Don't ignore alerts** during deployment
8. **Don't deploy multiple risky changes** at once (isolate variables)

---

**Document Version:** 1.0.0
**Last Updated:** 2026-01-30
**Next Review:** 2026-02-28
