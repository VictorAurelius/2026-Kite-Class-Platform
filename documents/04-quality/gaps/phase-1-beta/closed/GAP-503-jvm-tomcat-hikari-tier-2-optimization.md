# GAP-503: Tier 2 config optimization — JVM container ergonomics + Tomcat threads + HikariCP right-size + healthcheck grace period

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (no-cost / no-infra-change improvements; pre-cohort hardening; depends on GAP-502 stability fix)
**Domain:** Backend / DevOps / Performance
**Found:** 2026-05-13 (audit-of-trust pass + best-practice gap analysis)
**Affects:** 5 kitehub-* Java services on kh_backend + kiteclass-core on kc_app — all production Java services

## Problem

Audit-of-trust pass 2026-05-13 (`documents/04-quality/audits/aws-verification/2026-05-13-audit-of-trust-production-instability.md`) surface GAP-502 (RabbitMQ auth + OOM thrash). Trong khi diagnose root causes, best-practice gap analysis cho phần thread/pool config phát hiện **default Spring Boot settings KHÔNG fit constrained container environment** (320-480 MiB cgroup limits, 2 vCPU shared across 5 services).

**Gap matrix:**

| Config | Industry best practice | Current | Risk |
|---|---|---|---|
| JVM heap sizing | `-XX:MaxRAMPercentage=50.0 -XX:+UseContainerSupport` (auto-size theo container limit) | `-Xmx384m -Xms192m` fixed (subscription/admin/branding); `-Xmx256m -Xms128m` (gateway/email) | Non-heap (metaspace + code cache + native + threads) không có budget → OOM kill (GAP-502 RC2 observed) |
| Tomcat thread pool | `max-threads ≈ (vCPU × 100 / N-services)` ~50/service | Default 200/service × 5 services = up to 1000 threads trên 2 vCPU | Context-switch storm khi load up; thread stack memory bloat |
| Healthcheck grace period | `start_period ≥ JVM cold start (60-120s)` | `start_period` default 0s | Healthcheck fires immediately → curl fails khi Spring chưa boot xong → marked unhealthy → restart loop premature |
| HikariCP pool size | `(RDS max-connections × 0.5) / N-services` ~5-8 per service | Default 10 per service × 5 = 50 potential | RDS db.t3.micro `max_connections ~85` → 60% committed pre-user; surge → connection starvation |
| HikariCP min-idle | Min active baseline (= concurrent steady-state load) | 2 | Cold start = pool warm-up latency mỗi request đầu |
| Rabbit consumer concurrency | `concurrency=3-5` per listener tier-aware | Default 1 per @RabbitListener | Serial message processing per service; backlog không drain song song |
| Spring `@Async` cho I/O endpoints | Async non-blocking cho long-running calls | Synchronous everything | Thread held suốt request lifecycle → reduces effective concurrent capacity |

## Root Cause

- Default Spring Boot config được tune cho **dedicated VM** (multiple vCPU, GB-scale heap) chứ không phải **container with tight cgroup limits**
- GAP-447 sizing decision focus on EC2 instance size + container memory budget, KHÔNG cover JVM internal tuning
- Java 17 có `UseContainerSupport` enabled by default từ Java 11+, nhưng `-Xmx` fixed override defeats container-aware sizing
- Healthcheck `start_period` không explicit trong `docker-compose.production.yml` — relies on default (0s, fires immediately)

## Proposed Fix

### Phase A — Container ergonomic JVM (no cost)

**Replace fixed `-Xmx` với percentage-based trong `docker-compose.production.yml`:**

```yaml
environment:
  JAVA_OPTS: >-
    -XX:MaxRAMPercentage=50.0
    -XX:InitialRAMPercentage=25.0
    -XX:+UseContainerSupport
    -XX:+UseSerialGC
    -XX:+ExitOnOutOfMemoryError
```

Hiệu ứng cho container 480 MiB limit:
- Max heap = 240 MB (50%) — leave 240 MB non-heap (metaspace + code cache + threads + native)
- Initial heap = 120 MB (25%) — không grab full upfront
- `UseSerialGC` giữ cho low-memory environment (đã có hiện tại)
- `ExitOnOutOfMemoryError` — fail-fast thay vì zombie process; docker-compose restart cleanly

### Phase B — Tomcat thread pool right-size

**Add per application.yml:**

```yaml
server:
  tomcat:
    threads:
      max: 50           # default 200 → 50; 5 services × 50 = 250 total trên 2 vCPU
      min-spare: 5      # default 10 → 5
    accept-count: 50    # default 100 → 50 (queue cho thread pool exhausted)
    max-connections: 200 # default 8192 → 200 (memory-bounded)
    connection-timeout: 20000
  shutdown: graceful   # đảm bảo graceful drain on restart
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Hiệu ứng:
- Total thread budget: 5 services × 50 = 250 worker threads + ~50 Tomcat NIO + ~25 platform = ~325 trên 2 vCPU → context-switch manageable
- accept-count tight → fail-fast khi overloaded thay vì queue indefinitely (proper backpressure)

### Phase C — Healthcheck grace period

**Update `docker-compose.production.yml` per service:**

```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -fsS http://localhost:8080/actuator/health/liveness || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 120s    # cho phép Spring Boot 60-120s cold start
```

Hiệu ứng:
- Healthcheck không fire trong 120s đầu sau container start → JVM + Spring boot xong rồi mới check
- Sử dụng `/actuator/health/liveness` thay vì `/actuator/health` (subset, không include downstream deps cascade)
- retries=5 × 30s = 2.5 min grace window post-cold-start trước khi mark unhealthy

### Phase D — HikariCP per-service right-size

**Audit per-service DB load → tune pool size:**

| Service | Suggested `HIKARI_MAX_POOL` | Suggested `minimum-idle` | Rationale |
|---|---|---|---|
| kitehub-subscription | 8 | 3 | High DB activity (billing + tenant lifecycle) |
| kitehub-admin | 6 | 2 | Moderate (admin dashboards) |
| kitehub-branding | 5 | 2 | Light DB; heavy on Redis + RabbitMQ |
| kitehub-email | 4 | 1 | Mostly outbound; small DB writes |
| kitehub-gateway | 0 | 0 | Stateless gateway — không cần JPA datasource (verify, may need 2 for rate-limit persistence) |
| kiteclass-core | 10 | 3 | Heavy DB (student/class/attendance core) |

Total: ~33 connections vs current 60 default = 35% reduction; RDS `max_connections` headroom ~50%.

Update via env override trong compose:
```yaml
kitehub-subscription:
  environment:
    HIKARI_MAX_POOL: 8
```

### Phase E — Rabbit consumer concurrency (defer Phase 1.5)

Per-listener `concurrency=2-3,5` config tunable; defer cho Phase 1.5 PAID khi backlog patterns rõ + per-service load profile cụ thể. Currently AI Branding tier-aware semaphore đã handle prioritization → Phase 1 OK.

### Phase F — Async endpoint conversion (defer Phase 2)

Convert long-running endpoint (file upload, AI generation status poll, etc.) sang `@Async` hoặc Spring WebFlux. Defer cho Phase 2 vì impact rộng (require ApplicationEventPublisher + CompletableFuture redesign).

## Acceptance Criteria

- [ ] **Phase A** — JVM config replaced trong `docker-compose.production.yml` cho 5 kitehub services + kiteclass-core; deploy verified; no OOM kill trong 1h post-deploy
- [ ] **Phase B** — Tomcat thread config added vào application.yml cho 6 services (5 kitehub + kiteclass-core); load test verify max-threads honored (50 not 200)
- [ ] **Phase C** — Healthcheck với `start_period=120s` + `liveness` endpoint trong docker-compose; verify 0 premature restarts trong 1h sau cold start
- [ ] **Phase D** — Per-service `HIKARI_MAX_POOL` env trong docker-compose; RDS `max_connections` headroom ≥50% verified via `pg_stat_activity` query
- [ ] **Load test baseline** — `k6 run --vus 50 --duration 5m` against production OR staging; document p95 latency + error rate; gap pass nếu error rate <1% + p95 <1s
- [ ] **Memory headroom** — `docker stats` shows mỗi container <70% mem limit at steady state
- [ ] **No regression** — Plan 1 Bước 1-7 still work post-config-change

## Out-of-scope (track separately)

- Phase E Rabbit consumer concurrency tuning → Phase 1.5 prep
- Phase F Async endpoint conversion → Phase 2 redesign
- Global rate limiting at gateway level (per-IP / per-token) → separate gap
- Distributed tracing OTel wiring → GAP-115 PARTIAL Wave 7
- Circuit breaker expansion (chỉ AI provider hiện tại) → separate gap
- HTTP cache headers + ETag strategy → Phase 1.5 prep
- Connection pool warmup → batch với Phase D
- k6/JMeter load test runbook → bundle với this gap's load test AC

## Related

- **Parent finding:** audit-of-trust pass 2026-05-13 `documents/04-quality/audits/aws-verification/2026-05-13-audit-of-trust-production-instability.md`
- **Sister gap (P0 BLOCKING — must fix first):** GAP-502 (RabbitMQ auth + OOM thrash)
- **Sizing context:** GAP-447 (EC2 right-sizing) + `documents/05-guides/deploy/aws-architecture-sizing-matrix.md`
- **Architecture decisions:** ADR-025 (AWS Free Tier), ADR-028 (ECS Fargate vs EKS deferred Phase 1.5)
- **Phase 1.5 prep:** `documents/03-planning/roadmap/phase-2-eks-migration.md` trigger gates
- **Pattern reference:** Java 17 Container Ergonomics (https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html — `-XX:UseContainerSupport`)
- **Spring Boot Tomcat tuning:** https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.server.server.tomcat.threads.max

## Effort estimate

- Phase A (JVM): ~1h (6 service env updates + 1 deploy verify)
- Phase B (Tomcat): ~1h (application.yml × 6 services + verify)
- Phase C (Healthcheck): ~30 min (compose update + cold start test)
- Phase D (HikariCP): ~1h (audit current usage qua `pg_stat_activity` + tune)
- Load test baseline: ~2h (k6 script + run + analyze)
- **Total: ~5-6h** — 1 wave bucket scope

## Log

- **2026-05-13:** Filed during Wave 69 audit-of-trust pass. Surfaced 8 best-practice gaps in default Spring Boot config under constrained container environment. 6 phases proposed; Phases A-D in scope cho Phase 1 BETA hardening; Phases E-F deferred Phase 1.5/2. Sister to GAP-502 (must fix RC1+RC2 first vì GAP-503 changes won't help nếu services still OOM-killing).
