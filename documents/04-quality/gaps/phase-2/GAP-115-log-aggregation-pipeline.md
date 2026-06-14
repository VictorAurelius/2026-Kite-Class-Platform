# GAP-115: Log Aggregation Pipeline (ELK/Loki)

**Status:** 🟡 PARTIAL — Phase 1 (Grafana skeleton dashboard + on-call runbook) shipped Wave 41 Bucket F 2026-05-08; Phase 2 (Loki/Promtail backend stack) tracked in [GAP-434](GAP-434-loki-promtail-stack-phase2.md)
**Priority:** 🟠 P1
**Domain:** DevOps / Observability
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** Debugability cho toàn platform

## Problem

Không có log aggregation stack. Logs hiện tại stuck trong container stdout → disappear khi container restart. Không search được qua services.

Evidence:
- `infrastructure/helm/kitehub/values.yaml` — không có Loki/Promtail/Fluentd/ELK config
- `kitehub/docker-compose.kitehub.yml` monitoring profile chỉ có Prometheus + Grafana, không có Loki
- AWS deployment không có CloudWatch Logs agent/FluentBit
- Oracle deployment không có log shipper

## Root Cause

Log aggregation chưa được infrastructure team build. Thiếu shared logging backend.

## Proposed Fix

**Option A (recommended cho OCI deploy):** Loki stack
1. Add Loki vào Helm chart (sibling của Prometheus — depends on GAP-111)
2. Deploy Promtail as DaemonSet → scrape pod logs
3. Grafana datasource: Loki (already có Grafana)
4. LogQL queries: filter by tenantId, service, level

**Option B (AWS-specific):** CloudWatch Logs + Insights
1. Install aws-for-fluent-bit addon cho EKS
2. Stream pod logs → CloudWatch Log Groups per service
3. CloudWatch Insights cho query

**Retention:** 30 days hot, 90 days cold storage (S3)

## Acceptance Criteria

- [ ] Log aggregation stack deploy vào staging + prod — Phase 2 (GAP-434)
- [ ] Logs từ 6+ services aggregated + searchable — Phase 2 (GAP-434)
- [ ] Query example: `{service="kitehub-subscription"} | tenantId="abc-123"` → hiển thị mọi log entries của tenant — Phase 2 (GAP-434)
- [ ] Retention policy configured — Phase 2 (GAP-434)
- [ ] Cost baseline documented — Phase 2 (GAP-434)
- [x] Runbook: "How to query logs" trong `documents/05-guides/operations/` — Phase 1 shipped Wave 41 Bucket F 2026-05-08 at `documents/05-guides/operations/runbooks/monitoring-dashboards.md` (catalog + on-call workflow + LogQL example queries)
- [x] Grafana skeleton dashboard provisioned — `infrastructure/helm/kitehub/dashboards/logs-overview.json` (Phase 1; renders "No data" until Phase 2 Loki backend ships)

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §5
- Depends: GAP-114 (structured JSON logging — prerequisite)
- Depends: GAP-111 (monitoring stack for Loki/Grafana)

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-2 (remaining Loki = Phase 2 observability (GAP-434)).
- **2026-05-08 — Wave 41 Bucket F:** Phase 1 shipped — Grafana skeleton dashboard `infrastructure/helm/kitehub/dashboards/logs-overview.json` (Loki datasource, 4 panels: log volume / error rate / per-service ERROR count last 5m / recent ERROR log lines tenant-scoped) + ConfigMap template `templates/dashboard-logs-overview.yaml` (auto-loaded via Grafana sidecar discovery) + on-call runbook `documents/05-guides/operations/runbooks/monitoring-dashboards.md` cataloging all 5 baseline dashboards (api-latency / http-traffic / jvm-heap-gc / infra-pools / logs-overview) with alert→dashboard→runbook 4-step triage workflow + forward-looking LogQL example queries. Phase 2 (Loki/Promtail backend stack + S3 retention + smoke test) tracked in **GAP-434**. Helm template renders verified via `helm template --show-only templates/dashboard-logs-overview.yaml`. JSON validated via `jq .`. Status flipped 🔵 OPEN → 🟡 PARTIAL.
- 2026-04-19 — Discovered in ops-readiness baseline audit
