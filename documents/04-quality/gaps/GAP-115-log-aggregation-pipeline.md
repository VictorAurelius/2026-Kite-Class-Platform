# GAP-115: Log Aggregation Pipeline (ELK/Loki)

**Status:** 🔵 OPEN
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

- [ ] Log aggregation stack deploy vào staging + prod
- [ ] Logs từ 6+ services aggregated + searchable
- [ ] Query example: `{service="kitehub-subscription"} | tenantId="abc-123"` → hiển thị mọi log entries của tenant
- [ ] Retention policy configured
- [ ] Cost baseline documented
- [ ] Runbook: "How to query logs" trong `documents/05-guides/operations/`

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §5
- Depends: GAP-114 (structured JSON logging — prerequisite)
- Depends: GAP-111 (monitoring stack for Loki/Grafana)

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
