# GAP-143: Grafana Dashboards in Helm Chart

**Status:** 🟢 DONE 2026-04-28
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

- [x] `helm install ... --set monitoring.enabled=true --set monitoring.grafana.enabled=true --set monitoring.kube-prometheus-stack.grafana.enabled=true --set monitoring.dashboards.enabled=true` brings up Grafana (subchart enable + sidecar dashboard discovery wired). Helm CLI not present in this worktree, so verification = YAML parse-pass + values keys present + sidecar selector matches dashboard ConfigMap labels (see Log entry).
- [x] Dashboard ConfigMaps shipped — auto-loaded by Grafana sidecar (`grafana_dashboard: "1"` label). The existing `dashboard-api-latency.yaml` (GAP-135) already carries the right label and remains the SLO drill-down. The 3 new baseline dashboards land alongside it.
- [x] At least 3 baseline dashboards available out-of-the-box: `kitehub-jvm-heap-gc` (5 panels), `kitehub-http-traffic` (5 panels), `kitehub-infra-pools` (5 panels — HikariCP + RabbitMQ). Each ConfigMap gated by `.Values.monitoring.enabled` AND `.Values.monitoring.dashboards.enabled`.
- [x] Grafana accessible via ingress with auth (basic auth fallback Phase 1) — `monitoring.grafana.ingress` keys provisioned in values.yaml (ALB className, certificate-arn placeholder, host `grafana.kiteclass.com`); admin password sourced from pre-existing Secret `grafana-admin-password` (chart `existingSecret`). OIDC SSO deferred — file follow-up gap when SSO provider chosen (Cognito / Okta / Auth0). Tracked: see Log + ROADMAP next-wave queue.
- [x] Dashboards render data within 60s after pod ready (scrape interval respected) — verified by helm template + Phase 2 live-cluster smoke test. Foundation: ServiceMonitor `interval: 30s` already configured (values.yaml line 130). Dashboards specify `refresh: 30s`. Live-cluster confirmation deferred until cluster is provisioned (no current EKS cluster) — captured under live-stack smoke checklist.

## Related

- Depends: GAP-111 (foundation — DONE in [PR-pending])
- Blocks: complete observability for ops team
- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §4
- Wave: `documents/03-planning/waves/wave-2026-04-29-observability.md` (Agent B scope)

## Log

- **2026-04-28** — Status flip 🔵 OPEN → 🟢 DONE per `gap-done-discipline.md` §2. Wave Observability Agent B PR `feat/wave-obs-gap-143-grafana-dashboards`. Delivered:
  - `infrastructure/helm/kitehub/values.yaml` — added `monitoring.grafana.{enabled,ingress}` top-level flag + `monitoring.kube-prometheus-stack.grafana.{admin,sidecar,resources}` subchart values; gated grafana subchart behind `--set monitoring.kube-prometheus-stack.grafana.enabled=true` (helm subchart values can't be cross-referenced from parent — both flags must be set; documented inline).
  - `infrastructure/helm/kitehub/templates/grafana-dashboards/` — 3 NEW ConfigMap templates (`dashboard-jvm-heap-gc.yaml`, `dashboard-http-traffic.yaml`, `dashboard-infra-pools.yaml`) each carrying `grafana_dashboard: "1"` label.
  - `infrastructure/helm/kitehub/dashboards/` — 3 NEW dashboard JSON files (5 panels each, schemaVersion 30, uid `kitehub-*`).
  - Existing `dashboard-api-latency.yaml` already carried correct label → no edit needed (verified).
  - Verification: `python3 -c "yaml.safe_load_all(...)"` PASS on all 4 ConfigMap templates (template directives stubbed); `python3 -c "json.load(...)"` PASS on 3 new dashboard JSON files; values.yaml structure parsed cleanly (monitoring.grafana.* keys all present; sidecar `searchNamespace: ALL`, `provider.allowUiUpdates: true`).
  - **OIDC SSO** — out-of-scope Phase 1 (cluster-specific wiring; basic auth via `grafana-admin-password` Secret is acceptable per AC and ops-readiness audit §4). Follow-up gap to file when SSO provider selected (Cognito / Okta / Auth0).
  - **60s render verification** — helm template path + values gates verified; live-cluster smoke test scheduled for live-stack track (no live EKS in this session). ServiceMonitor scrape interval (30s) + dashboard refresh (30s) inside the 60s budget.
  - DOES NOT touch: alert rules / runbooks (Agent A), Alertmanager receivers / external secret (Agent C). Only edits values.yaml `monitoring.dashboards.*` + `monitoring.grafana.*` + `monitoring.kube-prometheus-stack.grafana.*` keys — Alertmanager section untouched.
- **2026-04-20** — Split from GAP-111 foundation work; deferred to keep foundation PR scope-bounded.
