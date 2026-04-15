# GAP-009: Frontend Instance Provisioning Lifecycle

**Status:** 🟢 DONE (Wave 2 Sub-PR 2.5, merged 2026-04-14; state machine + entity + service landed; REST + RabbitMQ outbox deferred to later wave)
**Branch:** wave/02-data-model
**ADR:** ADR-000
**Priority:** 🟠 P1
**Domain:** Backend / DevOps
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md` §4

## Problem

Khi tenant đăng ký → frontend instance cần provisioning với branding resources. Hiện tại chỉ có `JobStatus` enum 5 states cho branding job (QUEUED/PROCESSING/COMPLETED/FAILED/CANCELLED) — **không đủ** cho full lifecycle của frontend instance.

User đã nêu cần states: mới khởi tạo, đang tạo, đã lên lần 1, tạo lại, ...

## Evidence

- `JobStatus.java` chỉ có 5 states (branding job level, không phải instance level)
- **Không có** `InstanceStatus` enum
- **Không có** `FrontendInstance` entity
- Branding job completes không trigger "instance deployed" state
- Không có webhook notify kiteclass-frontend khi instance ready
- Không có retry/regenerate workflow rõ ràng

## Proposed State Machine

```
NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING
                     ↓              ↓          ↑
                   FAILED ←───── FAILED ───────┘ (retry)
```

### State definitions

| State | Ý nghĩa | Transitions |
|-------|---------|-------------|
| `NOT_STARTED` | Tenant tạo, chưa provision | → INITIALIZING |
| `INITIALIZING` | Create instance shell (DB schema, storage bucket, DNS record) | → GENERATING, → FAILED |
| `GENERATING` | Run branding pipeline (analyzer + planner + executor) | → DEPLOYED, → FAILED |
| `DEPLOYED` | Instance live, FE accessible tại `{slug}.kiteclass.com` | → REGENERATING |
| `REGENERATING` | User rebrand hoặc admin retry, instance vẫn live với branding cũ | → DEPLOYED, → FAILED |
| `FAILED` | Provisioning failed, retry count tracked | → INITIALIZING (retry), → abandoned |

## Proposed Fix

### Step 0: Rename existing enum to avoid conflict

**Existing:** `kitehub-platform/.../InstanceStatus.java` — chứa SUBSCRIPTION states (PENDING/TRIAL/ACTIVE/SUSPENDED/DELETED). Tên confuse.

**Rename:**
```java
// OLD (kitehub-platform, confusing name)
InstanceStatus { PENDING, TRIAL, ACTIVE, SUSPENDED, DELETED }

// NEW (clearer semantic)
SubscriptionStatus { PENDING, TRIAL, ACTIVE, SUSPENDED, DELETED }
```

### Step 1: New enum cho provisioning (kitehub-branding)

```java
public enum FrontendInstanceStatus {
  NOT_STARTED, INITIALIZING, GENERATING, DEPLOYED, REGENERATING, FAILED
}

@Entity
public class FrontendInstance {
  @Id String instanceId;
  String tenantId;
  String slug;  // subdomain
  String frontendUrl;

  @Enumerated(EnumType.STRING)
  InstanceStatus status;

  // Lifecycle timestamps
  Timestamp createdAt, initializingAt, generatingAt, deployedAt;
  Timestamp lastRegenerateAt, failedAt;

  Integer retryCount;
  String failureReason;

  Integer brandingVersion;  // increment mỗi lần rebrand
}
```

### Step 2: State machine service

```java
@Service
public class InstanceLifecycleService {

  public void initiate(String tenantId) {
    // NOT_STARTED → INITIALIZING
    var instance = new FrontendInstance(tenantId);
    instance.setStatus(INITIALIZING);
    provisionInfrastructure(instance);  // DB, S3, DNS
    eventPublisher.publish(new InstanceInitializingEvent(instance));
  }

  public void onInfrastructureReady(String instanceId) {
    // INITIALIZING → GENERATING
    transitionTo(instanceId, GENERATING);
    brandingJobService.enqueue(instanceId);
  }

  public void onBrandingCompleted(String instanceId) {
    // GENERATING → DEPLOYED (or REGENERATING → DEPLOYED)
    transitionTo(instanceId, DEPLOYED);
    eventPublisher.publish(new InstanceDeployedEvent(instanceId));  // Webhook → FE
  }

  public void rebrand(String instanceId, RebrandRequest req) {
    // DEPLOYED → REGENERATING
    transitionTo(instanceId, REGENERATING);
    brandingJobService.enqueueRebrand(instanceId, req);
  }

  public void onFailed(String instanceId, Exception cause) {
    // * → FAILED
    instance.retryCount++;
    instance.failureReason = cause.getMessage();
    transitionTo(instanceId, FAILED);

    if (instance.retryCount < MAX_RETRIES) {
      scheduleRetry(instanceId);
    } else {
      notifyOpsAbandoned(instanceId);
    }
  }
}
```

### Step 3: Event-driven via RabbitMQ

```
Events published:
- tenant.created        → trigger initiate()
- infrastructure.ready  → trigger onInfrastructureReady()
- branding.completed    → trigger onBrandingCompleted()
- branding.failed       → trigger onFailed()
- instance.deployed     → webhook to kiteclass-frontend
- instance.rebrand      → user trigger
```

### Step 4: FE notification

```java
// Webhook endpoint: kiteclass-core exposes
POST /internal/notify/instance-deployed
  body: { instanceId, frontendUrl, brandingVersion }

// OR SSE stream for real-time progress
GET /api/v1/instances/{id}/status/stream
  → SSE events: { status, progress, currentStep }
```

### Step 5: Admin dashboard

- List instances với status badges
- Filter by state (all FAILED needs attention)
- Action: Retry button cho FAILED instances
- Action: Rebrand button cho DEPLOYED
- Audit log: state transitions với timestamps

## Acceptance Criteria

- [ ] `InstanceStatus` enum + `FrontendInstance` entity + DB migration
- [ ] `InstanceLifecycleService` với tất cả transition methods
- [ ] State machine validation (invalid transitions throw error)
- [ ] RabbitMQ events published cho mỗi transition
- [ ] Webhook endpoint `/internal/notify/instance-deployed` trigger FE refresh
- [ ] Admin UI list + filter + retry actions
- [ ] Unit tests: valid transitions, invalid transitions, retry limits
- [ ] Integration test: full lifecycle end-to-end (NOT_STARTED → DEPLOYED)
- [ ] Metrics: `instance_transitions_total{from,to}`, `instance_provisioning_duration_seconds`

## Dependencies

- **Integrates with GAP-002** (async pipeline via RabbitMQ)
- **Integrates with GAP-008** (branding executor → triggers GENERATING state)
- **Integrates with GAP-010** (webhook → FE integration)

## Log

- 2026-04-14 — Created from AI Branding redesign §4
