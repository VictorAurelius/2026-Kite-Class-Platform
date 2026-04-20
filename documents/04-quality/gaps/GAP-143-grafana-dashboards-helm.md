# GAP-143: Grafana Dashboards in Helm Chart

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps / Monitoring
**Found:** 2026-04-20 (split from GAP-111 foundation work)
**Affects:** Production observability — operators have Prometheus data but no UI dashboards

## Problem

GAP-111 foundation PR ships Prometheus + Alertmanager via `kube-prometheus-stack`
subchart but explicitly disables Grafana (`monitoring.kube-prometheus-stack.grafana.enabled: false`)
to keep the foundation install lightweight. Production operators currently have
no visualization layer for the metrics being scraped.

The dev `kitehub-overview.json` dashboard (referenced in GAP-111) has not been
imported into the Helm chart yet.

## Root Cause

GAP-111 was scoped to "foundation only" — Prometheus + Alertmanager + alert rule
wiring. Grafana + dashboard ConfigMaps deferred to keep PR review tractable.

## Proposed Fix

1. Flip `monitoring.kube-prometheus-stack.grafana.enabled: true` (gated behind a
   new sub-flag like `monitoring.grafana.enabled` so it stays opt-in)
2. Provision Grafana admin password via Secret (`grafana-admin-password`)
3. Import existing `kitehub-overview.json` dashboard via ConfigMap with the
   `grafana_dashboard: "1"` label (auto-discovery sidecar)
4. Add 2-3 baseline dashboards:
   - JVM heap + GC per service
   - HTTP request rate / error rate / p99 latency
   - HikariCP connection pool + RabbitMQ queue depth
5. Configure ingress for Grafana behind same ALB as platform (auth via OIDC)
6. Default datasource: `Prometheus` (auto-injected by subchart)

## Acceptance Criteria

- [ ] `helm install ... --set monitoring.enabled=true --set monitoring.grafana.enabled=true` brings up Grafana
- [ ] `kitehub-overview.json` loads with production data
- [ ] At least 3 baseline dashboards available out-of-the-box
- [ ] Grafana accessible via ingress with auth (OIDC or basic auth fallback)
- [ ] Dashboards render data within 60s after pod ready (scrape interval respected)

## Related

- Depends: GAP-111 (foundation — DONE in [PR-pending])
- Blocks: complete observability for ops team
- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §4

## Log

- 2026-04-20 — Split from GAP-111 foundation work; deferred to keep foundation PR scope-bounded.
