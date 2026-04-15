# Outbox Events — API Contract

> Internal Java API (no REST endpoints — outbox is infrastructure, not a tenant-facing feature).

## OutboxEventWriter (for services)

### `enqueue(eventType, aggregateType, aggregateId, payloadJson) → OutboxEvent`

**Propagation:** `MANDATORY` — caller must already be inside `@Transactional`. Throws otherwise.

```java
@Transactional
public void markBrandingCompleted(long id, String url) {
    var i = load(id);
    i.transitionTo(DEPLOYED);
    repository.save(i);
    outbox.enqueue("instance.deployed", "FrontendInstance",
                    String.valueOf(id),
                    "{\"instanceId\":" + id + ",\"url\":\"" + url + "\"}");
}
```

Payload should be valid JSON; publisher does not re-validate.

## OutboxEventRepository (for queries)

| Method | Returns |
|--------|---------|
| `findDispatchable(now, pageable)` | Up to N PENDING events with `next_attempt_at <= now`, oldest first |
| `countByStatusAndDeletedFalse(status)` | Row count for monitoring dashboards |
| Standard `JpaRepository` methods (findById, delete…) | — |

## EventDispatcher (SPI — extend for new brokers)

```java
public interface EventDispatcher {
    void dispatch(OutboxEvent event) throws DispatchException;
}
```

- `DispatchException`: transient failure — publisher retries with backoff
- `RuntimeException`: treated same as DispatchException (publisher logs + retries)
- Return normally: publisher marks PUBLISHED

**Current impls:**
- `LoggingEventDispatcher` (default) — logs to slf4j, always succeeds
- `RabbitMQEventDispatcher` (future, profile `rabbitmq-live`) — publishes to exchange

Adapter pattern — domain code never references broker types directly (§3.10 design rule).

## OutboxEventPublisher (internal)

- `@Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:5000}")`
- Batches up to 50 rows per tick
- Each row saved in its own transaction (one bad event doesn't block the batch)
- Not directly invoked — scheduling handles it

## Payload conventions

| Field | Type | Notes |
|-------|------|-------|
| `aggregateType` | string | e.g. `FrontendInstance` |
| `aggregateId` | string | entity PK as string |
| `timestamp` | ISO-8601 | event creation time (optional — `created_at` on row also available) |
| domain fields | varies | per event type, see catalogue in rules.md |

## Log
- 2026-04-14 — Initial contract
