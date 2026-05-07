# GAP-434: Loki/Promtail Stack (Phase 2 of GAP-115)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps / Observability
**Found:** 2026-05-08 (Wave 41 Bucket F — split from GAP-115 after Phase 1 dashboards shipped)
**Affects:** Log searchability across all services — currently only stdout via `kubectl logs`

## Problem

Phase 1 of GAP-115 (Wave 41 Bucket F) shipped the Grafana skeleton dashboard `kitehub-logs-overview` + on-call runbook, but the Loki/Promtail backend that powers it is NOT deployed. Until Phase 2 ships:

- Logs Overview dashboard panels render "No data"
- LogQL queries documented in the runbook are aspirational only
- On-call must fall back to `kubectl logs deploy/<service> --since=30m | jq` per service
- Tenant-scoped log query (`{service=...} | tenantId=...`) — not possible without Loki indexing

## Root Cause

Phase 1 intentionally split off the dashboard provisioning from the backend stack so the runbook + dashboard URLs are stable BEFORE Loki ships. The Helm subchart wiring + DaemonSet + S3 backend is the bigger lift and was deferred to keep Wave 41 Bucket F scope tight.

## Proposed Fix (Phase 2 — Option A from GAP-115)

1. **Loki Helm dependency** — add `grafana/loki-stack` chart to `infrastructure/helm/kitehub/Chart.yaml` `dependencies:` block (sibling of `kube-prometheus-stack`).
2. **Promtail DaemonSet** — config in values.yaml: scrape `/var/log/containers/*.log`, parse JSON, forward to Loki Distributor.
3. **Loki backend** — single-binary mode for cost (Phase 2a), microservices mode (Phase 2b future) once volume warrants.
4. **S3 storage** — Loki schema config: index BoltDB on local PVC (24h hot), chunks on S3 (90d cold per `logs-format-standard.md` §4).
5. **Grafana datasource provisioning** — auto-add Loki via subchart `datasources` block; set uid `loki` so dashboard `${DS_LOKI}` resolves.
6. **Smoke test** — extend `scripts/smoke-test.sh` (GAP-377) with `LOGS_OVERVIEW_E2E`: `helm test loki` + `count_over_time({service="kitehub-gateway"}[5m]) > 0`.

## Acceptance Criteria

- [ ] `loki-stack` dependency added + `helm dependency build` clean
- [ ] Promtail DaemonSet scrapes pod logs in all kitehub namespaces
- [ ] Loki backend persists chunks to S3 (90d retention)
- [ ] Grafana sidecar auto-provisions Loki datasource (uid `loki`)
- [ ] `kitehub-logs-overview` dashboard panels show real data within 5min of any service log
- [ ] Tenant-scoped query verified: `{service="kitehub-subscription"} | json | tenantId="<seed-tenant>"` returns ≥1 line in dev cluster
- [ ] `scripts/smoke-test.sh LOGS_OVERVIEW_E2E` passes
- [ ] Cost baseline documented (S3 storage + egress + Loki compute)
- [ ] Runbook §3 Phase 2 → "✅ Active"

## Related

- **Parent:** GAP-115 (Phase 1 closed Wave 41 Bucket F — flipped to PARTIAL)
- **Phase 1 dashboard:** `infrastructure/helm/kitehub/dashboards/logs-overview.json`
- **Phase 1 runbook:** `documents/05-guides/operations/runbooks/monitoring-dashboards.md`
- **Logging schema:** `.claude/rules/logs-format-standard.md` (PII scrubbing prerequisite — GAP-116)
- **Prereq depends:** GAP-114 (structured JSON logging across all services)
- **Sibling:** GAP-145 (tracing — Tempo/Jaeger for distributed traces; complementary)
- **Audit reference:** `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §5

## Log

- **2026-05-08 — Wave 41 Bucket F:** Gap created when Phase 1 (dashboard + runbook) shipped. Phase 2 scope = Loki/Promtail backend + S3 retention + smoke test. Estimated effort: ~6-8h Helm wiring + cluster smoke test (single bucket — backend stack is internally cohesive).
