# GAP-005: AI queue fair scheduling + horizontal scaling

**Status:** 🟡 IN_PROGRESS (Phase 1 done 2026-04-18; Phase 2 open)
**Priority:** 🔴 P0 (production blocker ở scale)
**Domain:** AI / Backend / DevOps
**Detected:** 2026-04-14
**Phase 1 resolved:** 2026-04-18 (Wave 3 MVP — GAP-005a)
**Related Docs:**
- `documents/03-planning/implementation/ai-local-implementation-plan.md`
- `documents/01-business/kitehub/ai-branding/rules.md`
- `GAP-002` (async pipeline) — prerequisite

## Problem

Ví dụ: 100 users đồng thời gọi AI services (30% premium, 40% pro, 30% free). Hệ thống hiện tại:

- ✅ Có rate limit **theo ngày** (AIB-01 đến AIB-04): free 3/d, basic 10/d, premium 50/d
- ❌ **KHÔNG** có concurrent request queue
- ❌ **KHÔNG** có priority scheduling per tier
- ❌ **KHÔNG** có horizontal scaling (1 Ollama instance = bottleneck)
- ❌ **KHÔNG** có SLA guarantees per tier
- ❌ **KHÔNG** có backpressure / circuit breaker

Kết quả: 100 users queue FIFO → free user submit trước → premium phải chờ → vi phạm SLA tier.

## Context

Hiện tại: 1 Ollama container. Text model `llama3.1:8b` có throughput ~3-10 req/s trên CPU, ~30-50 req/s trên GPU. Image generation ~2-5 phút/request. Không scale được khi load tăng.

Rate limit theo ngày KHÔNG giải quyết được burst concurrent. Ví dụ 50 premium users cùng click "Generate banner" lúc 9am → tất cả trong queue FIFO, mỗi user chờ 10-250 phút (5 phút × 50).

## Evidence

- `application.yml` line 42-46: rate-limit chỉ có `-per-day`
- `AIProviderConfig.java`: không có concurrent limit, không có queue config
- `docker-compose.kitehub.yml`: 1 Ollama instance duy nhất, không replica
- RabbitMQ có nhưng chưa dùng cho AI (xem GAP-002)

## Proposed Fix — Best Practice Architecture

### 1. Weighted Fair Queueing (WFQ) per Tier

```
Queue Topology (RabbitMQ):
  ai.generate.enterprise  (priority 10, guaranteed workers)
  ai.generate.premium     (priority 8)
  ai.generate.pro         (priority 5)
  ai.generate.free        (priority 2)

Worker pool:
  enterprise-worker-1,2   (dedicated, 2 instances)
  shared-worker-1,2,3,4   (pulls from premium/pro/free with weights)
```

**Scheduling algorithm:** Weighted Round Robin
- Enterprise: dedicated workers → 0 wait time (SLA)
- Premium weight 4, Pro weight 2, Free weight 1
- Mỗi round: shared worker lấy 4 premium + 2 pro + 1 free

### 2. Concurrent Request Limit per User

Thêm vào `ai.rate-limit` config:

```yaml
ai:
  rate-limit:
    # Daily quota (existing)
    free-per-day: 3
    basic-per-day: 10
    premium-per-day: 50
    enterprise-per-day: -1

    # NEW: Concurrent in-flight requests per user
    concurrent:
      free: 1        # max 1 pending job at a time
      basic: 2
      premium: 5
      enterprise: 20

    # NEW: Queue depth limits (backpressure)
    queue-max-depth:
      free: 100      # 429 if queue full
      basic: 200
      premium: 500
      enterprise: 1000

    # NEW: SLA targets (latency)
    sla-seconds:
      free: 300      # 5 min best-effort
      basic: 120     # 2 min
      premium: 60    # 1 min
      enterprise: 30 # 30s guaranteed
```

### 3. Horizontal Scaling

```yaml
# docker-compose.kitehub.yml
ollama:
  deploy:
    replicas: 3  # multiple workers
    resources:
      reservations:
        devices:
          - capabilities: [gpu]

# Load balancer (nginx or HAProxy) in front
ollama-lb:
  image: nginx:alpine
  volumes:
    - ./nginx/ollama-lb.conf:/etc/nginx/conf.d/default.conf
```

**Production (Kubernetes):**
- HPA (Horizontal Pod Autoscaler) dựa trên queue depth
- Spot instances cho Standard tier, on-demand cho Premium/Enterprise
- GPU node pool separate (chỉ provision khi cần)

### 4. Circuit Breaker + Backpressure

```java
@CircuitBreaker(name = "ai-service", fallbackMethod = "degradedResponse")
public CompletableFuture<Image> generateImage(Request req) {
  // If circuit open: return template fallback (GAP-004)
  // If queue full: return 429 with Retry-After header
}
```

### 5. Observability

Metrics cần track (Prometheus + Grafana):
- `ai_queue_depth{tier}` — queue size per tier
- `ai_request_latency_seconds{tier}` — histogram
- `ai_sla_violations_total{tier}` — count
- `ai_worker_utilization{instance}` — %
- `ai_circuit_breaker_state{service}` — open/closed/half-open

### 6. Template Fallback (ref GAP-004)

Khi hệ thống overload hoặc user quá quota:
- Free tier full → show template gallery, không AI
- Premium tier full → queue với ETA hiển thị
- Enterprise có dedicated workers → luôn khả dụng

## Capacity Planning (Concrete Numbers)

### Hardware Baseline

| Component | Specs | Throughput |
|-----------|-------|------------|
| Oracle Cloud Always Free | ARM 4 cores, 24GB RAM | Text: 5-10 req/s (CPU), Image: ~2-5 min/img |
| AWS g4dn.xlarge (GPU) | NVIDIA T4, 16GB RAM | Text: 30-50 req/s, Image: ~30s/img |
| Template composer (CPU) | 1 CPU core | ~100 compose/s (SVG→PNG) |

### Scenario: 100 Concurrent Users (30% premium / 40% pro / 30% free)

**Assumption với template-first architecture (ref GAP-004, GAP-007):**
- Premium: avg 2 req/session, mix 50% template + 50% AI
- Pro: avg 1 req/session, mix 70% template + 30% AI
- Free: avg 1 req/session, mix 90% template + 10% AI

**Request volume trong 1 min peak:**

| Tier | Users | Total req | Template req | AI req |
|------|-------|-----------|--------------|--------|
| Premium (30%) | 30 | 60 | 30 | 30 |
| Pro (40%) | 40 | 40 | 28 | 12 |
| Free (30%) | 30 | 30 | 27 | 3 |
| **TOTAL** | **100** | **130** | **85** | **45** |

**Capacity check:**
- Template path: 85/min → 1.4/s → **1 CPU worker đủ** ✓
- AI path: 45/min → 0.75/s
  - Oracle CPU: 5-10/s → dư sức ✓
  - GPU: 30-50/s → dư sức ✓

**Kết luận: 100 concurrent users feasible trên Oracle Cloud Always Free** nếu template-first architecture implemented (GAP-004 + GAP-007).

### Scaling Thresholds

| Concurrent Users | Template Share | Infrastructure | Notes |
|------------------|---------------|----------------|-------|
| 100 | 80%+ | Oracle Cloud Free | Current plan |
| 500 | 80%+ | Oracle + 1 GPU | Phase 2 |
| 1000 | 80%+ | Multi-region + CDN + 2-3 GPU | Phase 3 |
| 5000+ | 80%+ | K8s HPA + spot GPU pool | Enterprise scale |

### Example Queue Behavior (100 concurrent, 30/40/30 split)

**Với GAP-005 implemented + GAP-007 classification:**

| Tier | Concurrent limit | Path split | Actual wait |
|------|------------------|-----------|-------------|
| Premium | 5/user | 50% template (<3s), 50% AI queue | Priority 8, ~30s for AI |
| Pro | 2/user | 70% template, 30% AI queue | Priority 5, ~1min for AI |
| Free | 1/user | 90% template, 10% AI fallback | Priority 2, template-only if queue full |

**Spike handling:**
- Template path instant (no queue)
- AI burst 45 req → queue drains in <1 min with 4 workers
- HPA scale up if sustained queue depth >100

## Acceptance Criteria

- [ ] WFQ implemented với 4 priority queues
- [ ] Concurrent limit per user per tier
- [ ] Queue depth limits + 429 backpressure
- [ ] SLA metrics tracked (p50/p95/p99 per tier)
- [ ] Horizontal scaling (Docker Compose replicas hoặc K8s HPA)
- [ ] Circuit breaker với template fallback
- [ ] Load test: 100 concurrent users (30/40/30 split) → premium < 1 min, free < 5 min
- [ ] Grafana dashboard cho AI queue metrics

## Dependencies

- **Blocked by GAP-002** (async pipeline) — queue infrastructure prerequisite
- **Integrates with GAP-004** (template fallback) — degraded mode

## References

- [Weighted Fair Queueing](https://en.wikipedia.org/wiki/Weighted_fair_queueing)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- [AWS Queue Priority Pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/patterns/implement-priority-based-task-scheduling.html)
- OpenAI's approach: dedicated capacity for enterprise, shared pool for others

## Phase 1 Resolution (Wave 3 — 2026-04-18, GAP-005a)

**Shipped (single-instance fair queueing):**
- **Priority topology:** 3 tier queues + 3 DLQs — `ai.request.{enterprise,pro,free}` behind `ai.request.exchange` (direct). Feature-flagged via `ai.queue.fair-queue-enabled` (default on).
- **`AIJobPriority`** enum — weights 3:2:1; `fromTier()` maps PricingTier → priority with FREE as safe default for unknown/null tiers.
- **`AIQueueDispatcher`** — publishes to correct tier queue; falls back to legacy `branding-jobs` when flag off. Metrics: `ai.queue.dispatched{tier,mode}`, `ai.queue.dispatch.failed{tier}`.
- **`DistributedRateLimiter`** (Redis) — atomic `INCR` for daily counter (24h TTL) + soft concurrency semaphore. Graceful fallback when Redis unavailable (returns -1, caller uses DB).
- **`AIRateLimitService`** — Redis-first; falls back to existing JPA `AIUsageLogRepository` on failure.
- **`AIJobConsumer`** — 3 `@RabbitListener` methods (one per tier queue); acquires/releases Redis semaphore in finally; NACKs on cap reached. Backpressure: free-tier jobs degrade to template fallback when `enterpriseBacklog > ai.queue.backpressure.enterprise-backlog-threshold` (default 50).
- **`BacklogInspector`** — live gauge `ai.queue.depth{tier}` via `AmqpAdmin`.
- **Metrics instrumented:** `ai.queue.dispatched`, `ai.queue.depth`, `ai.job.wait.time`, `ai.job.duration`, `ai.job.outcome{tier,outcome=success|failure|concurrency_limited|degraded}`.
- **Resilience4j** circuit breaker config (`ai-provider`) scaffolded in application.yml — applied in follow-up when real AI calls route through consumer.
- **SLA config defaults** (user-confirmed): Free P95=180s/concurrent=1, Pro P95=60s/concurrent=3, Enterprise P95=30s/concurrent=10, weighted RR 3:2:1, backpressure threshold 50.

**Tests:**
- `AIJobPriorityTest` (6), `AIQueueDispatcherTest` (6), `DistributedRateLimiterTest` (13), `AIJobConsumerTest` (5), `AIRateLimitServiceTest` (14 DB fallback path), `AIRateLimitServiceRedisTest` (5 Redis-first path).
- `mvn test -pl kitehub-branding` → 138 tests pass, 0 failures.

**Phase 2 (still open — deferred):**
- Horizontal scaling: Ollama replicas + HAProxy/nginx LB + K8s HPA on queue depth.
- Full Grafana dashboards + SLA violation alerting (Wave 6).
- `@CircuitBreaker` annotations on real AI client calls (wired when consumer dispatches actual jobs).
- Load test: 100 concurrent users (30/40/30 split) → proven in dev with scripted harness.

## Log

- 2026-04-14 — Phát hiện qua scenario 100 concurrent users; P0 blocker cho production launch
- 2026-04-18 — Phase 1 shipped (Wave 3 — GAP-005a). Horizontal scaling + full observability deferred to Phase 2.
