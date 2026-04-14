# ADR-004: Frontend Instance Provisioning Lifecycle

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Architect
**Related Gap:** GAP-009

## Context

**Problem 1:** Hiện có `InstanceStatus` enum (kitehub-platform) với 5 states PENDING/TRIAL/ACTIVE/SUSPENDED/DELETED — nhưng đây là **subscription lifecycle**, không phải **provisioning lifecycle**.

**Problem 2:** Sau tenant signup, frontend instance (tenant's branded site) cần qua nhiều states:
- Chưa start
- Initializing (creating resources)
- Generating (AI branding running)
- Deployed (live)
- Regenerating (user rebrand)
- Failed (error)

Naming confusion + no state tracking → GAP-009 blocker.

## Decision

**Rename + Split:**

```java
// Rename existing (semantic clarity)
kitehub-platform:
  InstanceStatus → SubscriptionStatus
  // Existing 5 states: PENDING, TRIAL, ACTIVE, SUSPENDED, DELETED

// New enum + entity (provisioning lifecycle)
kitehub-branding:
  FrontendInstanceStatus {
    NOT_STARTED, INITIALIZING, GENERATING,
    DEPLOYED, REGENERATING, FAILED
  }

  FrontendInstance entity:
    - id, tenantId, slug, frontendUrl
    - status: FrontendInstanceStatus
    - retryCount, failureReason
    - brandingVersion (incremented per rebrand)
    - timestamps per state transition
```

State Pattern implementation:
```java
interface InstanceState {
  void initiate(Instance i);
  void generate(Instance i);
  void deploy(Instance i);
  void fail(Instance i, String reason);
}

class NotStartedState, InitializingState, ..., FailedState
```

Invalid transitions throw `IllegalStateException` at compile-time.

Events published per transition (Outbox pattern):
- instance.initializing, instance.generating
- instance.deployed, instance.failed, instance.regenerating

## Consequences

### Positive
- ✅ Semantic clarity (subscription vs provisioning)
- ✅ Type-safe transitions (State Pattern)
- ✅ Event-driven downstream (DNS, CDN, emails)
- ✅ Observability (per-state timestamps)
- ✅ Retry logic with state preserved

### Negative
- ❌ Rename requires find/replace across codebase
- ❌ Two enums now (developers must not confuse)
- ❌ State machine maintenance

## Alternatives Considered

### Alternative A: Reuse InstanceStatus, add new states
Pros: no rename
Cons: semantic confusion (subscription ≠ provisioning states mixed)

**Rejected:** bad naming = bad design

### Alternative B: Single enum with prefixes
`SUBSCRIPTION_PENDING, PROVISIONING_INITIALIZING`
Pros: single enum
Cons: verbose, separates concerns artificially

**Rejected:** split entities better match domain

## Implementation Notes

Migration V31:
```sql
-- Rename existing table column type if needed
-- Create frontend_instances:
CREATE TABLE frontend_instances (
  id VARCHAR(36) PRIMARY KEY,       -- UUID
  tenant_id UUID NOT NULL,
  slug VARCHAR(100) UNIQUE,
  frontend_url TEXT,
  status VARCHAR(20) NOT NULL,
  retry_count INT DEFAULT 0,
  failure_reason TEXT,
  branding_version INT DEFAULT 0,
  created_at TIMESTAMP,
  initializing_at TIMESTAMP,
  generating_at TIMESTAMP,
  deployed_at TIMESTAMP,
  failed_at TIMESTAMP,
  last_regenerate_at TIMESTAMP
);

CREATE INDEX idx_fi_tenant ON frontend_instances(tenant_id);
CREATE INDEX idx_fi_status ON frontend_instances(status);

-- Outbox for events
CREATE TABLE outbox_events (
  id BIGSERIAL PRIMARY KEY,
  event_type VARCHAR(100),
  aggregate_id VARCHAR(36),
  payload JSONB,
  published BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP,
  published_at TIMESTAMP
);
```

### Rename plan

Find/replace in:
- Java: `InstanceStatus` → `SubscriptionStatus` (kitehub-platform only)
- Tests, docs, configs

Atomic PR với grep-based verification.

## References

- GAP-009
- Design patterns: State Machine, Observer + Outbox
- Related ADR: ADR-001 (provisioning integrates with K-12 model)

## Log
- 2026-04-14 — Accepted
