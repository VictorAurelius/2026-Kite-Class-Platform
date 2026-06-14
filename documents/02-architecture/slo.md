# Gateway SLO — Per-Endpoint-Class Service Level Objectives

**Status:** 🟢 ACTIVE (2026-06-15)
**Owner:** Platform / SRE
**Closes:** GAP-1366 (performance audit 2026-06-14, F-010 — per-endpoint-class gateway SLO documentation)
**Applies to:** All HTTP endpoints routed through `kitehub-gateway` (`:8080`) and `kiteclass`
ingress (nginx → `kiteclass-core` `:8081`).

> **Architecture-level index.** The authoritative latency rubric (tier definitions, assignment
> flowchart, tagging convention, coverage status) lives in
> [`../05-guides/monitoring/api-performance-slo.md`](../05-guides/monitoring/api-performance-slo.md).
> This file is the per-endpoint-**class** summary that the gateway routing layer and the performance
> audit reference, mapping the audit's `read / write / heavy-gen / auth` vocabulary onto the
> established Tier A–F budgets.

---

## 1. Per-endpoint-class SLO table

Latencies measured at **gateway ingress** (includes CDN/tunnel + gateway auth + upstream call),
steady-state p-values over 5-minute rolling windows at production load.

| Endpoint class | Tier (rubric) | p50 | **p95 budget** | p99 | Examples |
|---|---|---:|---:|---:|---|
| **Auth** (login / token / `/auth/me`) | A | < 80 ms | **< 200 ms** | < 400 ms | `POST /api/platform/auth/login`, `GET /auth/me` |
| **Interactive read** (single entity by id/slug) | A | < 80 ms | **< 200 ms** | < 400 ms | `GET /students/{id}`, `GET /classes/{id}`, branding package |
| **List / search read** (paginated, aggregate) | B | < 200 ms | **< 500 ms** | < 1,000 ms | `GET /students?page=`, `GET /invoices?status=`, `/admin/instances` |
| **Write** (mutating, single entity) | C | < 300 ms | **< 800 ms** | < 1,500 ms | `POST /students`, `PUT /enrollments/{id}`, `POST /payments/{id}/record` |
| **Heavy-gen / batch** (bulk, export, report) | D | < 2,000 ms | **< 5,000 ms** | < 10,000 ms | `POST /bulk-imports/students`, `POST /reports/transcripts` |
| **Async / queue-bound** (AI generation, email batch) | E | — | per-queue SLA (free 180 s / pro 60 s / ent 30 s) | — | AI branding generation — HTTP returns jobId immediately |
| **Health / infra** (probes) | F | < 10 ms | **< 50 ms** | < 100 ms | `GET /actuator/health`, `/readiness` |

Assignment rubric (which tier a new endpoint gets) + rationale for each budget:
[`api-performance-slo.md` §2–§3](../05-guides/monitoring/api-performance-slo.md).

---

## 2. Enforcement — Prometheus alert thresholds

Each class maps to an alert in
[`infrastructure/helm/kitehub/templates/prometheusrule.yaml`](../../infrastructure/helm/kitehub/templates/prometheusrule.yaml)
(`api-latency-slo-alerts` group), evaluated via `histogram_quantile()` over
`http_server_requests_seconds_bucket` filtered by the `slo` label (class-level `@Timed` tag).

| Class / Tier | Alert | Threshold | `for:` | Severity |
|---|---|---:|---|---|
| Auth / Interactive read (A) | `ApiLatencyP95HighTierA` | 200 ms p95 | 10 m | warning |
| Auth / Interactive read (A) | `ApiLatencyP99CriticalTierA` | 400 ms p99 | 5 m | critical (page) |
| List read (B) | `ApiLatencyP95HighTierB` | 500 ms p95 | 10 m | warning |
| Write (C) | `ApiLatencyP95HighTierC` | 800 ms p95 | 10 m | warning |
| Heavy-gen (D) | `ApiLatencyP95HighTierD` | 5 s p95 | 10 m | warning |
| Health (F) | `HighResponseTime` (generic) | 50 ms / 2 s spike | 5 m | warning |

CloudWatch P0 edge alarms (5xx spike) live in
[`cloudwatch-p0-alarms.tf`](../../infrastructure/terraform-aws/cloudwatch-p0-alarms.tf) Alarm 6
(`Nginx5xxCount`) — orthogonal error-rate signal complementing these latency SLOs.

---

## 3. Where this is referenced

- **Gateway routing** — [`service-catalog-and-auth-flow.md` §Gateway routing](service-catalog-and-auth-flow.md)
  links here so route owners know the latency budget per class.
- **Platform SLO Registry** — [`compliance-control-map.md` §SLO Registry](compliance-control-map.md)
  holds the per-service composite SLOs (availability, error budget); this file is the
  per-endpoint-class latency slice.
- **Performance audit** — `.claude/skills/quality/performance-audit/SKILL.md` sub-check 2.4 grades
  gateway latency against this table.

---

## 4. Open follow-ups

- `@Timed` SLO-tag coverage is partial (see `api-performance-slo.md` §5.1 — subscription 5/10,
  kiteclass-core 5/13, gateway 0/n). Untagged endpoints fall into the generic `HighResponseTime`
  alert, not a per-tier alert. Completing coverage tracked under GAP-135 follow-up.
- Production-load P95 numbers are not yet measured (load test AWS-gated — GAP-1365). Until then the
  budgets above are targets, not measured baselines.

---

## 5. Related

- [`../05-guides/monitoring/api-performance-slo.md`](../05-guides/monitoring/api-performance-slo.md) — authoritative tier rubric (GAP-135)
- GAP-135 (SLO rubric + instrumentation — PARTIAL), GAP-1366 (this per-class doc), GAP-1365 (load-test the budgets — AWS-gated)
