# AI Agent Workflow — Business Rules

**Domain:** ai-agent-workflow
**Source:** GAP-008, Wave 3 Sub-PR 3.5, ADR-006, GAP-005a (Wave 3 Phase 1 fair queue), GAP-104 (rules backfill)
**Last verified:** 2026-04-19

## Rules

### Step contract
| ID | Rule |
|----|------|
| BR-STEP-001 | Step.execute() MUST throw StepException on failure (no silent returns) |
| BR-STEP-002 | Step.name() is unique across the deployed catalogue — used in logs, metrics, outbox events |
| BR-STEP-003 | Step reads/writes StepContext via explicit constants (e.g. KEY_PALETTE) documented in javadoc |
| BR-STEP-004 | Heavy work (image gen) MUST enqueue async (ai-branding-guidelines.md §3.3); execute() returns promptly |
| BR-STEP-005 | fallback() runs only when hasFallback() returns true; caller catches second failure as saga abort |

### PlanExecutor (Saga orchestration)
| ID | Rule |
|----|------|
| BR-EXEC-001 | Steps run strictly in declared order — no parallelism within a single plan |
| BR-EXEC-002 | On StepException + hasFallback → call fallback(), continue |
| BR-EXEC-003 | On StepException + no-fallback → emit ai.plan.failed + rethrow (aborts plan) |
| BR-EXEC-004 | Outbox events emitted: ai.plan.{started,completed,failed}, ai.step.{completed,fallback} |
| BR-EXEC-005 | Entire execute() runs in one @Transactional — all outbox rows commit atomically with domain changes |

### Analyzer / Planner
| ID | Rule |
|----|------|
| BR-AGENT-001 | Analyzer MUST go through ResilientAIClient (auto-primary) — CB + Bulkhead + Retry applied |
| BR-AGENT-002 | Planner honors AnalysisResult.templateOnly — plans produced regardless, Steps handle the template-only path |
| BR-AGENT-003 | Planner output is deterministic given same AnalysisResult (no random step selection) |

### Fair-queue scheduler (Wave 3 Phase 1, GAP-005a)

Tier-aware AI job queueing trên RabbitMQ — đảm bảo Enterprise jobs không bị starve bởi backlog Free, đồng thời giới hạn cost / concurrency per tenant. Chỉ active trong service `kitehub-branding`. Khi `fair-queue-enabled=false`, fallback về single-queue legacy (`branding-jobs`).

| ID | Rule | Value | Config key | Code reference |
|----|------|-------|-----------|----------------|
| BR-QUEUE-001 | Fair-queue feature flag — disable → revert single-queue legacy (backward compat) | true | `ai.queue.fair-queue-enabled` (env: `AI_FAIR_QUEUE_ENABLED`) | `AIQueueProperties.fairQueueEnabled`, `AIQueueConfig` (`@ConditionalOnProperty`), `AIJobConsumer` (`@ConditionalOnProperty`), `AIQueueDispatcher#dispatch` |
| BR-QUEUE-002 | Tier-weighted round-robin weight — ENTERPRISE | 3 | `ai.queue.tier-weights.enterprise` | `AIQueueProperties.TierWeights.enterprise`, `AIJobPriority.ENTERPRISE` |
| BR-QUEUE-003 | Tier-weighted round-robin weight — PRO (PREMIUM + BASIC) | 2 | `ai.queue.tier-weights.pro` | `AIQueueProperties.TierWeights.pro`, `AIJobPriority.PRO` |
| BR-QUEUE-004 | Tier-weighted round-robin weight — FREE / TRIAL | 1 | `ai.queue.tier-weights.free` | `AIQueueProperties.TierWeights.free`, `AIJobPriority.FREE` |
| BR-QUEUE-005 | Per-instance concurrency cap (Redis semaphore) — FREE | 1 in-flight job | `ai.queue.concurrency.free` | `AIQueueProperties.Concurrency.free`, `AIJobConsumer#concurrencyCapFor`, `DistributedRateLimiter#tryAcquireConcurrencySlot` |
| BR-QUEUE-006 | Per-instance concurrency cap (Redis semaphore) — PRO | 3 in-flight jobs | `ai.queue.concurrency.pro` | `AIQueueProperties.Concurrency.pro`, `AIJobConsumer#concurrencyCapFor` |
| BR-QUEUE-007 | Per-instance concurrency cap (Redis semaphore) — ENTERPRISE | 10 in-flight jobs | `ai.queue.concurrency.enterprise` | `AIQueueProperties.Concurrency.enterprise`, `AIJobConsumer#concurrencyCapFor` |
| BR-QUEUE-008 | SLA target wait p95 — FREE (informational, drives alerts) | 180s | `ai.queue.sla.free-p95-seconds` | `AIQueueProperties.Sla.freeP95Seconds` |
| BR-QUEUE-009 | SLA target wait p95 — PRO (informational, drives alerts) | 60s | `ai.queue.sla.pro-p95-seconds` | `AIQueueProperties.Sla.proP95Seconds` |
| BR-QUEUE-010 | SLA target wait p95 — ENTERPRISE (informational, drives alerts) | 30s | `ai.queue.sla.enterprise-p95-seconds` | `AIQueueProperties.Sla.enterpriseP95Seconds` |
| BR-QUEUE-011 | Backpressure threshold — FREE tier degrades to template fallback khi enterprise queue depth > N | 50 jobs | `ai.queue.backpressure.enterprise-backlog-threshold` | `AIQueueProperties.Backpressure.enterpriseBacklogThreshold`, `AIJobConsumer#handle` (degraded path), `BacklogInspector#enterpriseBacklog` |
| BR-QUEUE-012 | Concurrency cap exceeded → NACK + redeliver (KHÔNG drop job) | exception → Rabbit retry | (managed by listener retry config) | `AIJobConsumer.ConcurrencyLimitedException`, `spring.rabbitmq.listener.simple.retry` |
| BR-QUEUE-013 | Tier resolution — unknown / null tier maps tới FREE (fail-safe) | default FREE | n/a (code-level) | `AIJobPriority.fromTier`, `AIQueueDispatcher#dispatch` (null-safe) |
| BR-QUEUE-014 | RabbitMQ topology — 3 primary queues + 3 DLQs trên direct exchange `ai.request.exchange` | `ai.request.{enterprise,pro,free}` + `.dlq` | n/a (code constants) | `AIQueueConfig.QUEUE_*`, `AIQueueConfig.DLQ_*`, `AIQueueConfig.AI_EXCHANGE` |
| BR-QUEUE-015 | Circuit breaker around AI provider — failure rate threshold | 50% | `resilience4j.circuitbreaker.instances.ai-provider.failureRateThreshold` | `kitehub-branding/client/ResilientAIClient` (GAP-148, Wave 9-D — wraps `analyzeLogo`/`generateImage`/`generateText`); separate `ResilientAIClient` in `kiteclass-core` uses CB name `ai` (BR-AGENT-001) |
| BR-QUEUE-016 | Circuit breaker — wait duration trong open state | 30s | `resilience4j.circuitbreaker.instances.ai-provider.waitDurationInOpenState` | `kitehub-branding/client/ResilientAIClient` |
| BR-QUEUE-017 | Circuit breaker — sliding window size cho failure rate calc | 20 calls | `resilience4j.circuitbreaker.instances.ai-provider.slidingWindowSize` | `kitehub-branding/client/ResilientAIClient` |
| BR-QUEUE-018 | Circuit breaker — minimum calls trước khi đánh giá failure rate | 10 calls | `resilience4j.circuitbreaker.instances.ai-provider.minimumNumberOfCalls` | `kitehub-branding/client/ResilientAIClient` |

**Service scope:** rules above hiện chỉ áp dụng cho `kitehub-branding` (PR #341, Wave 3 Phase 1). Khi mở rộng fair-queue cho service khác (KiteClass core AI agents), copy config keys nguyên xi và reference các BR-QUEUE-* tương ứng.

**Phase 2 (deferred to GAP-005):** horizontal scaling (auto-scale workers per tier), per-tier dedicated worker pool, dynamic re-weighting based on real backlog. Phase 1 ships chỉ topology + dispatcher + consumer skeleton.

## Event catalogue

| Event | Trigger |
|-------|---------|
| ai.plan.started | PlanExecutor enters execute() |
| ai.step.completed | Step execute() returned normally |
| ai.step.fallback | Step execute() threw, fallback() recovered |
| ai.plan.completed | All steps done |
| ai.plan.failed | Step threw without fallback OR fallback also failed |

## Metrics (Wave 3 Phase 1)

Các metric Micrometer publish bởi `AIQueueDispatcher` + `AIJobConsumer` — dùng cho dashboards và SLA alerts (BR-QUEUE-008..010).

| Metric | Type | Tags | Source |
|--------|------|------|--------|
| `ai.queue.dispatched` | Counter | `tier`, `mode` (fair / legacy) | `AIQueueDispatcher#dispatch` |
| `ai.queue.dispatch.failed` | Counter | `tier` | `AIQueueDispatcher#dispatch` (catch block) |
| `ai.job.outcome` | Counter | `tier`, `outcome` (success / failure / degraded / concurrency_limited) | `AIJobConsumer#handle` |
| `ai.job.wait.time` | Timer | `tier` | `AIJobConsumer#handle` (enqueue → process start) |
| `ai.job.duration` | Timer | `tier` | `AIJobConsumer#handle` (process latency) |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `ai.queue.fair-queue-enabled` | `true` | BR-QUEUE-001 fair-queue toggle |
| `ai.queue.tier-weights.{enterprise,pro,free}` | 3 / 2 / 1 | BR-QUEUE-002..004 weighted RR |
| `ai.queue.concurrency.{free,pro,enterprise}` | 1 / 3 / 10 | BR-QUEUE-005..007 per-instance concurrency |
| `ai.queue.sla.{free,pro,enterprise}-p95-seconds` | 180 / 60 / 30 | BR-QUEUE-008..010 SLA targets (alerts) |
| `ai.queue.backpressure.enterprise-backlog-threshold` | 50 | BR-QUEUE-011 free-tier degrade threshold |
| `resilience4j.circuitbreaker.instances.ai-provider.failureRateThreshold` | 50 | BR-QUEUE-015 CB failure threshold |
| `resilience4j.circuitbreaker.instances.ai-provider.waitDurationInOpenState` | 30s | BR-QUEUE-016 CB open duration |
| `resilience4j.circuitbreaker.instances.ai-provider.slidingWindowSize` | 20 | BR-QUEUE-017 CB window |
| `resilience4j.circuitbreaker.instances.ai-provider.minimumNumberOfCalls` | 10 | BR-QUEUE-018 CB warmup |
| (Resilience4j Bulkhead/Retry inherits from `ai-provider` config defaults) | — | CB/Bulkhead/Retry for AI calls |

## Log
- 2026-04-21 — GAP-148 (Wave 9-D): BR-QUEUE-015..018 now backed by `kitehub-branding/src/main/java/com/kitehub/branding/client/ResilientAIClient.java` (decorator, `@Primary`, `@CircuitBreaker(name="ai-provider")` on `analyzeLogo`/`generateImage`/`generateText`, fallbacks return template-safe domain defaults). Previously config was dead (loaded but unreferenced). `AIProviderConfig.aiClient()` demoted from `@Primary` → named `aiClient`, injected into the wrapper via `@Qualifier`.
- 2026-04-19 — GAP-104: backfill BR-QUEUE-001..018 cho Wave 3 Phase 1 fair-queue (8 ai.queue config keys + 4 resilience4j keys), thêm UC-AGENT-08..10 và metrics catalogue. Source: `kitehub-branding/application.yml:60-91` + `AIQueueProperties` / `AIQueueConfig` / `AIJobPriority` / `AIJobConsumer` / `AIQueueDispatcher` / `BacklogInspector`.
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.5, ADR-006)
