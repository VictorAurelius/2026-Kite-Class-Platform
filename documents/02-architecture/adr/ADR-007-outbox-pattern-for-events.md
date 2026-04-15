# ADR-007: Outbox Pattern for Reliable Event Publishing

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Architect
**Related Gap:** GAP-009 (Wave 2 deferred) + Wave 3 foundations

## Context

Services must emit events when aggregates change state — e.g. `InstanceLifecycleService.markBrandingCompleted()` publishing `instance.deployed`.

Two naive options both wrong:

1. **Publish inside `@Transactional`** — if DB commits but RabbitMQ down, event lost.
2. **Publish after `@Transactional` commit** — if broker down or process crashes between commit and publish, event lost AND we can't easily retry (no record of intent).

`.claude/rules/design-patterns.md` §3.5 explicitly bans direct event publishing inside transactions.

## Decision

**Transactional Outbox Pattern:**

1. Service writes domain row + an `outbox_events` row **in the same JPA transaction** (one commit).
2. A separate `OutboxEventPublisher` (`@Scheduled`, every 5s) polls `outbox_events WHERE status='PENDING'`, dispatches to an `EventDispatcher` bean, marks row `PUBLISHED` on success.
3. Failed dispatches bump `retry_count` + capture `last_error`; exponential backoff handled by scheduler skipping rows until `next_attempt_at`.

```java
@Transactional
public void markBrandingCompleted(long id, String url) {
  var instance = load(id);
  instance.transitionTo(DEPLOYED);
  repository.save(instance);
  outbox.enqueue("instance.deployed", "FrontendInstance", id, payloadJson);
}
```

`EventDispatcher` interface:
```java
interface EventDispatcher {
  void dispatch(OutboxEvent event);  // throws on transient failure
}
```

Two impls:
- `LoggingEventDispatcher` (default for tests / dev) — prints to log
- `RabbitMQEventDispatcher` (profile `rabbitmq-live`) — publishes to exchange

Adapter pattern: services never touch `RabbitTemplate` directly.

## Consequences

### Positive
- ✅ At-least-once delivery guaranteed under partial failure
- ✅ Broker-agnostic (swap RabbitMQ → Kafka by swapping dispatcher)
- ✅ Tests trivially plug in-memory dispatcher
- ✅ Audit trail (every published event recorded)
- ✅ Satisfies `design-patterns.md` §3.5

### Negative
- ❌ Polling latency (≤5s; acceptable for branding use cases)
- ❌ Extra table + scheduled job to maintain
- ❌ Consumers must be idempotent (at-least-once, may see duplicates)

## Alternatives

- **A. Debezium CDC from Postgres → Kafka** — rejected: heavy infra for the current traffic shape; outbox table is simpler and sufficient.
- **B. Eventuate / Axon frameworks** — rejected: overkill for the scope.
- **C. Publish-after-commit via `TransactionSynchronizationManager`** — rejected: crash between commit and publish still loses events.

## Implementation Notes

### Schema (V33)

```sql
CREATE TABLE outbox_events (
  id              BIGSERIAL PRIMARY KEY,
  instance_id     UUID        NOT NULL,
  aggregate_type  VARCHAR(100) NOT NULL,
  aggregate_id    VARCHAR(100) NOT NULL,
  event_type      VARCHAR(100) NOT NULL,
  payload         JSONB        NOT NULL,
  status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
  retry_count     INT          NOT NULL DEFAULT 0,
  last_error      TEXT,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at    TIMESTAMP,
  next_attempt_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED'))
);
CREATE INDEX idx_outbox_pending ON outbox_events(next_attempt_at)
  WHERE status = 'PENDING';
```

### Retention

Published events older than 30 days → pruned by separate `@Scheduled` job (daily). Keeps table small while preserving recent audit.

### Events catalogue (current)

| event_type | aggregate | producer |
|-----------|-----------|----------|
| `instance.initializing` | FrontendInstance | InstanceLifecycleService |
| `instance.generating` | FrontendInstance | InstanceLifecycleService |
| `instance.deployed` | FrontendInstance | InstanceLifecycleService |
| `instance.regenerating` | FrontendInstance | InstanceLifecycleService |
| `instance.failed` | FrontendInstance | InstanceLifecycleService |

Catalogue extended by Wave 3 Sub-PRs 3.5/3.6.

## References

- GAP-009 (deferred Wave 2 item)
- design-patterns.md §3.5 (mandatory)
- Fowler, "Transactional Outbox"
- microservices.io/patterns/data/transactional-outbox.html

## Log

- 2026-04-14 — Accepted
