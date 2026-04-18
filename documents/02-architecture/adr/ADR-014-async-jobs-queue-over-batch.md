# ADR-014: Async Jobs Queue (RabbitMQ) over Batch Framework

**Status:** ACCEPTED
**Date:** 2026-04-18
**Deciders:** Tech Lead + Architect
**Reviewers:** Backend Lead, DevOps
**Related Gap(s):** GAP-005 (AI queue fair scheduling), GAP-002 (async pipeline), GAP-102 (ADR kickoff)

## Context

Platform có nhiều tác vụ heavy không phù hợp xử lý đồng bộ trong request-response cycle:

- **AI image generation** — 2-5 phút per banner (Ollama/OpenAI)
- **AI logo analysis** — 30-60 giây per upload
- **Email delivery** — ≥1000 tenants per campaign, rate-limited bởi SMTP provider
- **Document generation** — PDF report card, payroll slip per student (Wave 8-11)
- **Branding propagation** — event-driven refresh across ≥3 consumer services (Wave 4)
- **Bulk import** — 500-row chunks với validation (Wave 1)

Question: **Nên dùng pattern nào cho async/long-running work?** Ứng viên chính:

1. Spring Batch — framework batch processing chính thức của Spring
2. RabbitMQ + Spring AMQP work queues
3. Quartz Scheduler + DB-backed jobs
4. Kafka + Kafka Streams

Constraints:
- Team đã có RabbitMQ trong stack (từ Wave 1 email, Wave 4 branding events)
- Thesis cần defensible pattern có thể cite literature
- Quy mô hiện tại ~1k msg/s peak, không phải stream-scale
- Priority theo subscription tier (Free/Pro/Enterprise) là requirement

`.claude/rules/design-patterns.md` §2 bảng requires "Pipeline of steps with retry/undo" → Command + Composite, nhưng không specify transport.

## Decision

**We will use RabbitMQ + Spring AMQP work queues pattern cho tất cả user-triggered async work.** Spring Batch chỉ dùng cho scheduled batch jobs (nightly ETL, cleanup cron) nếu có trong tương lai.

### Implementation shape

```java
// Producer (chokepoint: never bypass):
@Service
public class AIQueueDispatcher {
  public String enqueue(AIRequest req) {
    String jobId = UUID.randomUUID().toString();
    rabbit.convertAndSend("ai.exchange", routingKey(req.tier()), new Job(jobId, req));
    return jobId;
  }
}

// Consumer (stateless, horizontal-scalable):
@RabbitListener(queues = "ai.request.enterprise")
public void handle(Job job) {
  // process, publish result event via Outbox (ADR-007)
}
```

### Per-tier priority queues

Weighted round-robin 3:2:1 giữa `ai.request.enterprise` → `.pro` → `.free`. Consumer cạnh tranh dựa trên routing key, không dùng single-queue priority headers (dễ starvation).

### Infrastructure primitives

- Exchange type: `direct` (routing by tier key)
- Queue feature: `x-dead-letter-exchange` → `ai.dlx` cho failed messages
- Consumer ack: `MANUAL` với explicit `basicAck`/`basicNack`
- Backpressure: Redis semaphore per tenant (Free 1 / Pro 3 / Enterprise 10 concurrent)

## Consequences

### Positive
- Non-blocking UX: HTTP return 202 + jobId, FE poll/SSE trong 200ms
- Reliability: consumer ack + DLQ — work không mất khi consumer crash
- Priority-aware: Enterprise precedence built-in qua queue topology
- Horizontal scale: thêm consumer instance, RabbitMQ fair-dispatch tự load-balance
- Code reuse: `EmailQueueConfig.java` pattern → `AIQueueConfig`, `DocumentQueueConfig` (Wave 8)
- Citable pattern: Microsoft Cloud Design Patterns — "Queue-Based Load Leveling" + "Priority Queue" → thesis defense easy

### Negative
- Operational complexity: monitor queue depth, DLQ triage, consumer lag (Wave 6 observability)
- Debug distributed: request không follow 1 thread, phải correlate qua traceId
- Idempotency burden: consumers phải handle duplicate messages (at-least-once delivery)
- Self-hosted ops: không có managed cost như SQS nhưng cần own cluster management

### Neutral
- Polling vs SSE cho FE: mỗi use-case tự chọn, không có chuẩn project-wide (yet)
- At-least-once semantics — app layer phải đảm bảo idempotency (consistent với ADR-007 Outbox)

## Alternatives Considered

### Alternative A: Spring Batch

**Pros:**
- Built-in chunking + JobExecution restart capability
- JobRepository metadata DB schema mature
- Integrates native Spring ecosystem

**Cons:**
- Designed for **scheduled batch processing**, not user-triggered async
- No native priority queues (phải implement riêng trong JobLauncher)
- Heavyweight: ~8 DB tables cho metadata
- Poor fit cho long-running AI tasks — state trong DB không scale horizontal tốt

**Rejected because:** Use-case mismatch. 80% of async work là user-triggered (AI generation, email send, doc gen), chỉ có nightly cleanup phù hợp Spring Batch. Thêm framework cho 20% use-case không đáng.

### Alternative B: Quartz Scheduler + DB-backed jobs

**Pros:**
- Simple mental model (cron-like)
- Không cần new infra (reuse PostgreSQL)

**Cons:**
- DB polling inefficient — load tăng theo tần suất poll interval
- No priority queues native
- Scaling horizontal cần distributed locking (complex)
- Poor fit real-time jobs — polling interval tradeoff với latency

**Rejected because:** DB polling anti-pattern ở scale, và không có priority queue native. Tốt cho scheduled cron tasks, không cho user async.

### Alternative C: Kafka + Kafka Streams

**Pros:**
- Log replay + event sourcing possible
- High throughput (>100k msg/s)
- Ordering guarantees per partition

**Cons:**
- **Overkill** cho scale hiện tại (~1k msg/s peak)
- Operational complexity: ZooKeeper/KRaft, partition rebalancing, retention tuning
- Learning curve cao (không ai trong team từng vận hành production Kafka)
- Storage cost: long retention đòi hỏi nhiều disk

**Rejected because:** Over-engineering cho scale hiện tại. Revisit nếu sustained queue depth >10k hoặc multi-region active-active requirement.

## Implementation Notes

### Migration strategy
- Already implemented cho email (Wave 1), branding events (Wave 4), AI priority queues (Wave 3)
- Document generation (Wave 8-11) sẽ dùng cùng pattern qua `DocumentQueueConfig`

### Rollback plan
- Không rollback — pattern đã ship production từ Wave 1
- Nếu discover limit ở scale: migrate sang Kafka per-queue, adapter layer giữ nguyên business code

### Feature flags
- `kitehub.queue.priority-routing.enabled=true` — có thể disable priority routing nếu bug DLQ, fallback single-queue mode

### Revisit triggers
- Sustained queue depth >10k for 1 week → Kafka evaluation
- Cross-region active-active requirement → RabbitMQ federation vs Kafka
- Enterprise customer demands stronger ordering guarantees

## References

- **Design patterns used:**
  - Strategy Pattern — queue selection per tier
  - Chain of Responsibility — priority consumption order
  - Outbox Pattern — result event publishing (ADR-007)

- **Related ADRs:**
  - ADR-006 (AI Agent Orchestration) — workflow steps consumed by this queue
  - ADR-007 (Outbox Pattern) — complementary for event reliability
  - ADR-008 (Resilience) — applied tại consumer (Circuit Breaker + Retry)

- **External references:**
  - [Microsoft Cloud Design Patterns — Queue-Based Load Leveling](https://learn.microsoft.com/en-us/azure/architecture/patterns/queue-based-load-leveling)
  - [Microsoft Cloud Design Patterns — Priority Queue](https://learn.microsoft.com/en-us/azure/architecture/patterns/priority-queue)
  - [RabbitMQ Priority Queues Documentation](https://www.rabbitmq.com/docs/priority)

- **Code pointers:**
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/EmailQueueConfig.java` — pattern template
  - `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/config/AIQueueConfig.java` — Wave 3 priority queues
  - `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/queue/BrandingJobConsumer.java` — consumer shape

## Log

- **2026-04-18:** Retroactively captured decision implicit từ Wave 1 (email) và explicit từ Wave 3 (AI priority queues). GAP-102 ADR kickoff — documented now because user asked "why RabbitMQ not batch?" without authoritative answer available.
