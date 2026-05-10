# GAP-434: Loki/Promtail Stack (Phase 2 of GAP-115)

**Status:** 🟡 PARTIAL (chart-level wiring shipped Wave 55 Bucket A; live-cluster smoke gated on first deploy per Risk C)
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

- [x] `loki-stack` dependency added (`Chart.yaml` v1.2.0, grafana/loki-stack 2.10.2)
- [x] Promtail DaemonSet config provisioned in `values.yaml` (scrapes `/var/log/containers/*.log`, JSON pipeline extracts `service`/`level`/`tenantId`/`traceId`/`spanId`/`userId` per `logs-format-standard.md` §2)
- [x] Loki single-binary backend configured with S3 chunk storage + BoltDB index + 90d retention (matches `logs-format-standard.md` §4)
- [x] Grafana datasource auto-provisioning ConfigMap (`templates/grafana-datasource-loki.yaml`, uid `loki`) — sidecar discovers via `grafana_datasource=1` label
- [x] S3 bucket name supplied via env-var/install flag (NOT hardcoded — matches `terraform-partial-backend-public-repo.md` defense-in-depth pattern)
- [x] `scripts/smoke-test.sh` extended with `check_logs_overview_e2e` (gated by `SMOKE_LOGS_E2E=1` + `LOKI_URL` env vars; auto-warns when not enabled)
- [ ] Live `helm dependency update` succeeds — DEFERRED: blocked by pre-existing PR #984 Go-template-in-values.yaml issue (tracked separately; not a Bucket A regression)
- [ ] `kitehub-logs-overview` dashboard panels show real data within 5min of any service log — DEFERRED to first cluster deploy (no local k8s in solo-dev mode per Wave 55 plan §1 Q3 Risk C)
- [ ] Tenant-scoped query verified: `{service="kitehub-subscription"} | json | tenantId="<seed-tenant>"` returns ≥1 line — DEFERRED to first cluster deploy
- [ ] `scripts/smoke-test.sh` LOGS_OVERVIEW_E2E green run on live cluster — DEFERRED (script committed, execution gated on cluster)
- [ ] Cost baseline documented (S3 storage + egress + Loki compute) — DEFERRED to first deploy (cost = empirical from CloudWatch/billing post-cutover)
- [ ] Runbook `documents/05-guides/operations/runbooks/monitoring-dashboards.md` §3 Phase 2 → "✅ Active" — DEFERRED to first deploy

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
- **2026-05-11 — Wave 55 Bucket A:** Chart-level wiring shipped. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp. Scope shipped:
  - `Chart.yaml` v1.1.0 → v1.2.0; added `grafana/loki-stack` v2.10.2 dependency (condition `loki.enabled`).
  - `values.yaml` `loki:` section: single-binary StatefulSet + BoltDB index on PVC + S3 chunk storage (90d retention per `logs-format-standard.md` §4) + Promtail DaemonSet with JSON pipeline extracting `service`/`level`/`tenantId`/`traceId`/`spanId`/`userId` MDC labels.
  - `templates/grafana-datasource-loki.yaml`: ConfigMap labelled `grafana_datasource=1` so kube-prometheus-stack Grafana sidecar auto-loads the datasource (uid `loki`, matches dashboard `${DS_LOKI}` placeholder).
  - `scripts/smoke-test.sh`: `check_logs_overview_e2e` function gated by `SMOKE_LOGS_E2E=1` + `LOKI_URL` env vars. Default invocation warns once; operator enables explicitly post-deploy.
  - S3 bucket name NEVER hardcoded — flows via `--set loki.s3.bucket=<from-terraform-output>` (defense-in-depth per `terraform-partial-backend-public-repo.md` pattern).
  - Local verify: `helm lint` + `helm dependency update` BLOCKED by pre-existing PR #984 Go-template-in-`values.yaml` issue (`monitoring.alertmanager.config` block uses `{{- if ... }}` inside values.yaml which is invalid YAML). Confirmed pre-existing on `main` HEAD baseline (same error before Bucket A changes). NOT a Bucket A regression — tracked as separate issue. YAML validity of new `loki:` block verified via `python3 -c yaml.safe_load(...)` standalone (PASS).
  - Live cluster verification (helm install + Promtail scrape + Loki query) DEFERRED per Wave 55 plan §1 Q3 Risk C (no local k8s in solo-dev mode). Re-verify checklist enforced when first deploy lands.
