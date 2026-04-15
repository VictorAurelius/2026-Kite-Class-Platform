# Outbox Events — Use Cases

### UC-OBX-01: Service Emits Event

- **Actor:** Any domain service inside a `@Transactional` method
- **Steps:**
  1. Service mutates aggregate (e.g. `instance.transitionTo(DEPLOYED)`)
  2. Service calls `outbox.enqueue(eventType, aggregateType, aggregateId, payloadJson)`
  3. Both writes commit in single transaction
- **Postcondition:** OutboxEvent row persisted with status=PENDING

### UC-OBX-02: Publisher Drains Batch

- **Actor:** `OutboxEventPublisher` `@Scheduled` job
- **Trigger:** every `outbox.publisher.interval-ms` (default 5s)
- **Steps:**
  1. Query `findDispatchable(now, limit=50)` — PENDING rows whose `next_attempt_at <= now`
  2. For each row, call `EventDispatcher.dispatch(event)` inside own transaction
  3. On success: `event.markPublished()` → save
  4. On `DispatchException`: `markFailureAndScheduleRetry()` → save
- **Postcondition:** Published rows transitioned; failed rows re-scheduled with exponential backoff

### UC-OBX-03: Retry Exhausted

- **Actor:** Publisher (continuation of UC-OBX-02)
- **Precondition:** `retry_count >= MAX_RETRIES` (default 10)
- **Steps:**
  1. `markFailureAndScheduleRetry()` sets status=FAILED
  2. Row becomes terminal; publisher no longer picks it up
  3. (Future) Alert ops team via monitoring
- **Postcondition:** Manual investigation required; event NOT delivered

### UC-OBX-04: Broker Recovery

- **Actor:** Admin / ops recovering broker
- **Steps:**
  1. Fix broker
  2. PENDING events with elapsed `next_attempt_at` auto-drain on next tick
  3. FAILED events (hit MAX_RETRIES) can be requeued by updating status=PENDING via manual SQL (with audit)
- **Postcondition:** Events catch up with no code change

### UC-OBX-05: Consumer Handles At-Least-Once

- **Actor:** Downstream event consumer (external or internal)
- **Steps:**
  1. Receive event with unique `id` (or aggregate+event combo)
  2. Check idempotency key in local store
  3. If already processed → skip
  4. Else → apply, record key
- **Notes:** Outbox guarantees delivery, NOT exactly-once — consumers must be idempotent (BR-OBX-004)

### UC-OBX-06: Observability

- **Actor:** Ops / SRE
- **Queries:**
  - `SELECT COUNT(*) FROM outbox_events WHERE status='PENDING' AND deleted=false` — backlog
  - `SELECT COUNT(*) FROM outbox_events WHERE status='FAILED' AND deleted=false` — dead letters
  - `SELECT event_type, COUNT(*) FROM outbox_events WHERE published_at > now() - interval '1 hour' GROUP BY event_type` — throughput
- **Targets:** backlog < 100 rows steady-state; FAILED count = 0 after ops review

## Log
- 2026-04-14 — Initial UCs
