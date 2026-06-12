# Runbook — Monitoring Dashboards (Grafana)

**Status:** ACTIVE (Phase 1)
**Owner:** SRE / On-call rotation
**Created:** 2026-05-08 (Wave 41 Bucket F — GAP-115/135 Phase 1)
**Closes:** GAP-115 AC #6 ("Runbook: How to query logs"), GAP-135 dashboard catalog

This runbook is the on-call entry point for "where do I look first?" when an alert fires or a customer reports degradation. It catalogs the 5 baseline Grafana dashboards provisioned via Helm (`infrastructure/helm/kitehub/dashboards/`) and the on-call workflow that uses them.

---

## 1. Dashboard catalog

All dashboards are auto-loaded by the Grafana sidecar when:

```bash
helm install kitehub ./infrastructure/helm/kitehub \
  --set monitoring.enabled=true \
  --set monitoring.grafana.enabled=true \
  --set monitoring.dashboards.enabled=true
```

The sidecar discovers ConfigMaps labelled `grafana_dashboard=1` (see `monitoring.dashboards.label` in values.yaml).

| Dashboard | UID | Source | Datasource | Purpose |
|-----------|-----|--------|------------|---------|
| **API Latency / SLO Tiers** | `kitehub-api-latency` | `dashboards/api-latency.json` | Prometheus | p50/p95/p99 per route, request rate, error rate, per-tier SLO tiles. Drives GAP-135 SLO observability. |
| **HTTP Traffic** | `kitehub-http-traffic` | `dashboards/http-traffic.json` | Prometheus | Request volume, status code distribution, top routes. |
| **JVM Heap & GC** | `kitehub-jvm-heap-gc` | `dashboards/jvm-heap-gc.json` | Prometheus | Heap usage, GC pause distribution, thread counts. |
| **Infra Pools** | `kitehub-infra-pools` | `dashboards/infra-pools.json` | Prometheus | DB connection pool, RabbitMQ queue depth, Redis ops/s. |
| **Logs Overview** (Phase 1) | `kitehub-logs-overview` | `dashboards/logs-overview.json` | Loki | Log volume per service, ERROR/WARN rate, recent error lines (tenant-scoped). Phase 2 ships Loki backend; until then panels render "No data" but URL is stable. |

### Dashboard URLs (cluster-internal port-forward)

```bash
kubectl port-forward -n monitoring svc/kitehub-monitoring-grafana 3000:80
```

Then open:
- API Latency: http://localhost:3000/d/kitehub-api-latency
- HTTP Traffic: http://localhost:3000/d/kitehub-http-traffic
- JVM Heap & GC: http://localhost:3000/d/kitehub-jvm-heap-gc
- Infra Pools: http://localhost:3000/d/kitehub-infra-pools
- Logs Overview: http://localhost:3000/d/kitehub-logs-overview

For production (when ingress + OIDC SSO enabled per GAP-143 follow-up): `https://grafana.kitehub.me/d/<uid>`.

---

## 2. On-call workflow — alert → dashboard → runbook

When PagerDuty / Alertmanager fires an alert, follow this 4-step triage:

### Step 1 — identify the alert source

Each Prometheus alert (see `infrastructure/helm/kitehub/templates/prometheusrule.yaml`) has a `runbook_url` annotation pointing to a runbook in this folder. **Always click the runbook link first** — alert-specific guidance lives there.

If the alert is missing a runbook link, file a follow-up gap (this is itself a P2 audit finding per GAP-117 ops-readiness).

### Step 2 — pick the dashboard matching the alert family

| Alert family | Primary dashboard | Common alerts |
|--------------|-------------------|---------------|
| `ApiLatencyP95High*` / `ApiLatencyP99Critical*` | API Latency / SLO Tiers | Latency SLO breach |
| `HighErrorRate` / `ServiceDown` | HTTP Traffic + Logs Overview | 5xx spikes, service unreachable |
| `JvmHeapHigh` / `GcPauseLong` | JVM Heap & GC | Memory pressure, GC stalls |
| `DbPoolExhausted` / `RabbitQueueBacklog` / `RedisEvictionRate` | Infra Pools | Resource saturation |
| `BackupJobFailure` | (no dashboard — see backup-failure runbook) | Daily backup missed |
| Any generic "investigate logs" | Logs Overview | Free-form ERROR investigation |

### Step 3 — narrow scope with template variables

Each dashboard has template variables (top of page). For multi-tenant incidents:
- **`$service`** — filter by affected service (multi-select)
- **`$tenantId`** — Logs Overview only — filter by tenant ID (regex; `.*` = all)
- **Time range** — default `now-1h`; widen for slow-burn issues

### Step 4 — drill into logs

If the dashboard shows a clear pattern but you need raw evidence:

1. Open Logs Overview dashboard
2. Set `$service` to affected service
3. Set `$tenantId` if customer-specific (e.g., `tenant-abc-123`)
4. The bottom "Recent ERROR log lines" panel shows last 1h of ERROR-level events
5. For deeper LogQL exploration, click any panel → "Explore" → modify query

Example LogQL queries (Phase 2 — once Loki ships):

```logql
# All ERRORs from kitehub-subscription for a specific tenant in last 30m
{service="kitehub-subscription"} | json | level="ERROR" | tenantId="tenant-abc-123"

# Auth failures across all services
{service=~"kitehub-.*"} | json | message=~"(?i)auth.*fail"

# Per-tenant log volume (find noisy tenants)
sum by (tenantId) (rate({service=~"kitehub-.*"} | json [5m]))

# Trace one request end-to-end via traceId
{service=~"kitehub-.*"} | json | traceId="4bf92f3577b34da6a3ce929d0e0e4736"
```

The `traceId` / `tenantId` / `userId` fields are MDC fields injected per `documents/05-guides/operations/logging-standard.md` + `.claude/rules/logs-format-standard.md`.

---

## 3. Phase status & limitations

### Phase 1 (this runbook — shipped Wave 41)
- ✅ 4 Prometheus dashboards (api-latency, http-traffic, jvm-heap-gc, infra-pools) live
- ✅ Logs Overview dashboard provisioned (Loki datasource, panels render "No data" until Phase 2)
- ✅ On-call workflow documented
- ✅ LogQL example queries documented (forward-looking)

### Phase 2 (deferred — see follow-up gap referenced from GAP-115)
- ⏳ Loki/Promtail Helm subchart wiring
- ⏳ Promtail DaemonSet scrapes pod logs across namespaces
- ⏳ S3 cold storage backend (90 days retention per `logs-format-standard.md` §4)
- ⏳ Loki datasource auto-provisioning via Grafana subchart
- ⏳ Smoke test: `LOGS_OVERVIEW_E2E` — assert `count_over_time({service="kitehub-gateway"}[5m]) > 0` after deploy

Until Phase 2 lands, on-call investigates logs via:

```bash
kubectl logs -n kitehub deploy/<service> --since=30m | jq 'select(.level=="ERROR")'
```

(Each service emits structured JSON to stdout per `logs-format-standard.md` §2.)

### Phase 3 (deferred — see GAP-135 AC #5)
- ⏳ PR template: "If you added a new endpoint, declare its SLO tier"
- ⏳ `check-pr` skill integration

---

## 4. Updating dashboards

Dashboards are git-tracked JSON files. To modify:

1. Edit dashboard in Grafana UI (drafting mode)
2. Click "Settings → JSON Model" → copy
3. Paste into `infrastructure/helm/kitehub/dashboards/<name>.json`
4. Validate: `jq . infrastructure/helm/kitehub/dashboards/<name>.json`
5. Lint chart: `helm lint infrastructure/helm/kitehub`
6. Render: `helm template infrastructure/helm/kitehub --set monitoring.enabled=true --set monitoring.dashboards.enabled=true | grep -A5 ConfigMap`
7. Commit + PR — coordinator merge ships to cluster on next Helm upgrade

**Do NOT edit dashboards via Grafana UI in production** — changes are lost on Pod restart because ConfigMap is the source of truth (`provider.allowUiUpdates: true` is set for development convenience, NOT durability).

---

## 5. Adding a new dashboard

1. Author dashboard JSON in `infrastructure/helm/kitehub/dashboards/<name>.json`
2. Validate: `jq . dashboards/<name>.json`
3. Add ConfigMap template `templates/dashboard-<name>.yaml` (copy `dashboard-logs-overview.yaml` pattern)
4. Append row to §1 catalog above
5. Update on-call workflow §2 if new alert family maps to it
6. Helm verify (see §4 step 5-6)

---

## 6. Related

- **Logging standard:** `documents/05-guides/operations/logging-standard.md` (existing — schema, MDC fields, retention)
- **Logs format rule:** `.claude/rules/logs-format-standard.md` (governance — PII scrubbing, banned fields)
- **API SLO rubric:** `documents/05-guides/monitoring/api-performance-slo.md` (drives api-latency dashboard)
- **Alert rules:** `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- **Backup failure runbook:** `runbooks/backup-failure.md` (no dashboard — alert wires straight to runbook)
- **Disaster recovery:** `disaster-recovery-plan.md`
- **GAP-115:** Log Aggregation Pipeline (this runbook closes AC #6 Phase 1)
- **GAP-135:** API p95 latency SLOs (this runbook closes catalog AC for dashboards)
- **GAP-143:** Grafana visualization layer
- **GAP-144:** kube-prometheus-stack production tuning

---

## 7. Log

- **2026-05-08 — Wave 41 Bucket F:** Runbook created. Catalogs 4 existing Prometheus dashboards (api-latency, http-traffic, jvm-heap-gc, infra-pools) + 1 new Loki skeleton dashboard (logs-overview). On-call workflow + LogQL example queries documented forward-looking; Phase 2 (Loki backend) deferred to follow-up gap from GAP-115.
