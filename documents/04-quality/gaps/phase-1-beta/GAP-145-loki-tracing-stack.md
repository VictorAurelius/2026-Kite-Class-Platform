# GAP-145: Loki Log Aggregation + Tempo Tracing Stack

**Status:** ⚫ WONTFIX (Wave 96 — scope consolidated by GAP-434+111+112)
**Priority:** 🟡 P2
**Domain:** DevOps / Observability
**Found:** 2026-04-20 (split from GAP-111 follow-up scope)
**Affects:** Incident debugging — metrics alone insufficient for root cause analysis

## Problem

GAP-111 foundation ships Prometheus (metrics) but no log aggregation or
distributed tracing. When an alert fires, on-call must SSH into individual pods
or `kubectl logs` each replica manually — slow and brittle for multi-service
investigations.

Without Loki + Tempo, the existing OpenTelemetry instrumentation in services
has no centralized backend.

## Root Cause

GAP-111 scoped to "monitoring foundation" (Prometheus + Alertmanager). Logs
and traces are separate observability dimensions deferred until metrics layer
proves stable.

## Proposed Fix

1. Add `grafana/loki-stack` (or `grafana/loki` + `grafana/promtail`) as
   optional Helm dependency, gated by `monitoring.loki.enabled`
2. Add `grafana/tempo` for distributed tracing backend
3. Configure Promtail DaemonSet to scrape pod logs, ship to Loki
4. Configure OTLP receiver in Tempo for OpenTelemetry traces from services
5. Update Grafana datasources (depends on GAP-143) to include Loki + Tempo
6. Add log-based alerting examples (e.g., spike in ERROR-level logs)
7. Retention strategy:
   - Loki: 7 days hot, 30 days cold (S3 backend)
   - Tempo: 7 days (sampling enabled)

## Acceptance Criteria

- [ ] `helm install ... --set monitoring.loki.enabled=true` brings up Loki
- [ ] All pod logs queryable from Grafana within 30s of emission
- [ ] OpenTelemetry traces from kitehub-subscription visible in Tempo
- [ ] Trace IDs in logs correlate with traces (LogQL → Tempo deep-link works)
- [ ] Storage backend configured (S3 for Loki, S3 for Tempo)
- [ ] Retention policies enforced

## Related

- Depends: GAP-111 (foundation — DONE), GAP-143 (Grafana for visualization)
- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §4 §5

## Log

- **2026-05-18 (Wave 96 PR1 — WONTFIX DUPLICATE):** Per Wave 96 gap re-triage full sweep (Agent 1 finding), scope của GAP-145 (Loki + Tempo combined stack) đã được consolidated bởi 3 gap riêng biệt:
  - **GAP-434** — Loki / Promtail log aggregation stack (Phase 2 deferred)
  - **GAP-111** — Distributed tracing missing (Tempo OpenTelemetry foundation)
  - **GAP-112** — Frontend error tracking missing (separate FE scope)

  Original GAP-145 đã propose combined "Loki + Tempo" 2026-04-20 — sau đó scope tách thành 3 gap riêng theo concern. GAP-145 trở thành duplicate umbrella. Mark WONTFIX per `audit-to-gap-pipeline.md` §3 (1 gap = 1 issue rõ ràng); cross-link sang 3 gap kế thừa.

  No actual work lost — chỉ là cleanup duplicate filing. Future work tracked qua sister gaps.

- 2026-04-20 — Split from GAP-111 follow-up scope; logs + tracing deferred until metrics foundation proves stable.
