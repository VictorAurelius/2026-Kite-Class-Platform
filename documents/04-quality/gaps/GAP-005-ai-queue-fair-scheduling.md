# GAP-005: AI queue fair scheduling + horizontal scaling

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (production blocker ở scale)
**Domain:** AI / Backend / DevOps
**Detected:** 2026-04-14
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

## Example: 100 concurrent users scenario

**Với GAP-005 implemented:**

| Tier | Users | Concurrent limit | Total in-flight | Wait time |
|------|-------|------------------|-----------------|-----------|
| Free (30%) | 30 | 1 each | 30 → queue | Best-effort, fallback template |
| Pro (40%) | 40 | 5 each | Max 200 → shared workers | ~2 min |
| Premium (30%) | 30 | 5 each | Max 150 → shared workers with priority | ~1 min |

**Scaling:**
- 4 shared workers × 30 req/min = 120 req/min throughput
- Với 100 concurrent spike → burst handled trong ~2 phút
- Nếu sustained load → HPA scale up thêm workers

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

## Log

- 2026-04-14 — Phát hiện qua scenario 100 concurrent users; P0 blocker cho production launch
