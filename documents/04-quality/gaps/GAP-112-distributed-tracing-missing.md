# GAP-112: Distributed Tracing Missing

**Status:** 🔵 OPEN
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

- [ ] TraceId xuất hiện trong logs (depends on GAP-114 MDC)
- [ ] Request từ FE xuyên 3 services show thành 1 trace trong Tempo/Jaeger
- [ ] RabbitMQ messages propagate trace context
- [ ] Grafana "Trace Latency" dashboard
- [ ] Sampling config per environment (prod 10%, staging 100%)

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §4
- Depends: GAP-111 (monitoring stack in prod), GAP-114 (MDC traceId)

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
