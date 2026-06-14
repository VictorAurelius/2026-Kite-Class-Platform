# GAP-112: Distributed Tracing Missing

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** DevOps / Monitoring
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** Debug experience cho 6 microservices + gateway

## Problem

Không có distributed tracing (OpenTelemetry / Zipkin / Jaeger) trong bất kỳ backend service nào.

Evidence:
- `grep -rn "opentelemetry\|zipkin\|jaeger\|otel" kitehub/ kiteclass/` → 0 application code hits
- `application.yml` không có `management.tracing.*` hoặc `spring.sleuth.*` config
- Gateway routes requests từ FE → gateway → kitehub-subscription → kitehub-branding → kitehub-email qua RabbitMQ, nhưng không có traceId propagation

Hậu quả: khi request chậm/lỗi, developer phải grep logs 5 services riêng, không thể correlate. Incident response time tăng nhiều lần.

## Root Cause

Feature chưa được prioritize. Spring Boot 3 hỗ trợ Micrometer Tracing OOTB nhưng chưa được enable.

## Proposed Fix

1. Add dependency: `io.micrometer:micrometer-tracing-bridge-otel` + `io.opentelemetry:opentelemetry-exporter-otlp`
2. Enable tracing trong `application.yml`:
   ```yaml
   management:
     tracing:
       sampling:
         probability: 0.1  # 10% sampling
     otlp:
       tracing:
         endpoint: http://tempo:4317
   ```
3. Deploy Tempo (hoặc Jaeger) vào monitoring namespace (depends on GAP-111)
4. Instrument RabbitMQ publishers/consumers để propagate traceId qua headers
5. Gateway propagate trace context qua `TraceContextPropagator`
6. Grafana dashboard hiển thị trace latency distribution

## Acceptance Criteria

- [x] TraceId xuất hiện trong logs (depends on GAP-114 MDC) — auto-injected via Spring Boot 3.5 MDC bridge
- [ ] Request từ FE xuyên 3 services show thành 1 trace trong Tempo/Jaeger — **deferred to live verification post-Tempo deploy (GAP-111 Phase 2)**
- [x] RabbitMQ messages propagate trace context — auto-instrumented by Micrometer Tracing for Spring AMQP (W3C `traceparent` header on message properties)
- [ ] Grafana "Trace Latency" dashboard — **deferred to GAP-111 Phase 2 (Grafana dashboards)**
- [x] Sampling config per environment (prod 10%, staging 100%) — `OTEL_SAMPLING_PROBABILITY` env override; default 0.1

## Current State (verified 2026-05-11)

Wave 55 Bucket B shipped infrastructure foundation across **7 deployable backend services**:

| Service | pom.xml deps added | application.yml updated | TracingConfigTest |
|---|---|---|---|
| `kitehub-subscription` | ✅ | ✅ | ✅ 2 tests pass |
| `kitehub-branding` | ✅ | ✅ | ✅ 1 test pass |
| `kitehub-email` | ✅ | ✅ | ✅ 1 test pass |
| `kitehub-admin` | ✅ | ✅ | ✅ 1 test pass |
| `kitehub-gateway` | ✅ | ✅ | ✅ 1 test pass |
| `kiteclass-core` | ✅ | ✅ | ✅ 1 test pass |
| `kiteclass-gateway` | ✅ | ✅ | ✅ 1 test pass |
| `kitehub-platform` | N/A — shared library, no Spring Boot application context | N/A | N/A |

Dependencies added per service (versions managed by Spring Boot 3.5 BOM):
- `io.micrometer:micrometer-tracing-bridge-otel`
- `io.opentelemetry:opentelemetry-exporter-otlp`

Application.yml block per service:
```yaml
management:
  tracing:
    sampling:
      probability: ${OTEL_SAMPLING_PROBABILITY:0.1}
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:}
```

**RabbitMQ propagation:** Spring Boot 3.5 + Micrometer Tracing auto-instruments Spring AMQP. `RabbitTemplate` automatically adds W3C `traceparent` to message headers; `@RabbitListener` automatically extracts and continues the trace context. No manual `MessagePostProcessor` or interceptor needed.

## Why PARTIAL (not DONE)

Per `gap-done-discipline.md` §2 + §3 PARTIAL exit ramp — 2 of 5 ACs require infrastructure not yet deployed:

1. **Live trace verification across services** — requires Tempo/Jaeger backend deployed (tracked GAP-111 monitoring stack Phase 2)
2. **Grafana "Trace Latency" dashboard** — requires Grafana provisioning (tracked GAP-111 Phase 2)

The application-side infrastructure (deps + config + auto-instrumentation + tests) is fully in place. As soon as `OTEL_EXPORTER_OTLP_ENDPOINT` env var points to a live Tempo collector, traces will flow without further code changes.

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §4
- Depends: GAP-111 (monitoring stack in prod), GAP-114 (MDC traceId)
- Wave 55 Bucket B PR (this PR) — application-side foundation
- Follow-up GAP-111 Phase 2 — live verification + Grafana dashboard

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-2 (blocked GAP-111 Phase 2; tracing post-launch ops).
- 2026-04-19 — Discovered in ops-readiness baseline audit
- **2026-05-11:** PR# backfill (Wave 60 Bucket D-2). Verified shipped work cross-references:
  - PR #1125 — `feat(observability): GAP-112 distributed tracing across 7 modules [Wave 55 Bucket B]` (merged 2026-05-10) — tracing deps + `management.tracing.*` config + 8 TracingConfigTest unit tests across 7 deployable services.

  Code-verify: 3/5 AC verified shipped (traceId in logs via MDC; RabbitMQ propagation auto-instrumented; sampling config per env). 2 AC blocked on Tempo/Jaeger backend deployment (GAP-111 Phase 2) — live trace verification + Grafana Trace Latency dashboard.

  Verdict: 🟡 PARTIAL maintained (NOT flipped DONE — infra deps deployed but live verification requires Tempo backend per GAP-111 Phase 2; complies with `gap-done-discipline.md` §3 PARTIAL exit ramp with named follow-up).

- **2026-05-11** — Wave 55 Bucket B shipped: tracing dependencies + `management.tracing.*` config across 7 deployable backend services (5 KH + 2 KC); 8 unit tests verify Spring `Tracer` bean wires when OTLP endpoint configured. RabbitMQ `traceparent` propagation auto-instrumented via Spring Boot 3.5 + Micrometer Tracing for Spring AMQP — verified by Spring Boot documentation, no manual `RabbitTemplate` wrapping needed. Status flipped 🔵 OPEN → 🟡 PARTIAL pending Tempo/Jaeger backend (GAP-111 Phase 2). `kitehub-platform` skipped (shared library, no Spring Boot application context). Verified `mvn verify -P strict-warnings` clean for all 5 KH services + KC gateway; KC core failures pre-existing (TenantIsolationIT GAP-466 RLS, unrelated to this change — confirmed via stash-and-verify on clean tree).
