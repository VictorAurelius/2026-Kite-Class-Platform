# API Performance — Service Level Objectives (SLOs)

**Status:** 🟢 ACTIVE (v0.2, 2026-04-26 — instrumentation + alerts + dashboard landed Wave perf-D)
**Priority:** 🟠 MANDATORY (see §7 Enforcement)
**Owner:** Platform / SRE
**Closes:** GAP-135 (rubric + tagging on 16 controllers + 5 Prom alerts + Grafana dashboard; remaining controllers under follow-up gap)
**Applies to:** All HTTP endpoints across KiteHub (6 services + gateway) and KiteClass (core + gateway)

---

## 1. Purpose

Before this document, the only latency SLO anywhere in the platform was the AI
fair-queue p95 in `kitehub-branding` (free/pro/enterprise tiers). Every other
200+ HTTP endpoint shipped with **no p95 budget, no alert, no regression
detector**. A PR that doubled admin dashboard latency would ship undetected
until customers complained.

This file is the **source of truth** for per-endpoint-family SLOs. Every
controller method MUST be mapped to a tier; every tier has a p50/p95/p99 target.
The performance audit (`quality/performance-audit/SKILL.md`) measures against
these numbers — without them, the audit cannot grade latency objectively.

---

## 2. Tier Definitions

Latencies measured at the **gateway ingress** (KiteHub gateway / KiteClass gateway),
not the upstream service alone, so CDN + gateway auth + upstream call are all
included. Targets expressed as **steady-state** p-values over 5-minute rolling
windows at production load (≥100 RPS across the platform).

| Tier | Latency budget (p95) | p50 target | p99 target | Typical endpoints |
|------|---------------------:|-----------:|-----------:|-------------------|
| **A** Interactive reads | **< 200 ms** | < 80 ms | < 400 ms | GET `/students/{id}`, GET `/classes/{id}`, dashboard widget APIs, `/auth/me` |
| **B** List pages (paginated) | **< 500 ms** | < 200 ms | < 1,000 ms | GET `/students?page=...`, GET `/invoices?status=...`, `/admin/instances` |
| **C** Writes (mutating) | **< 800 ms** | < 300 ms | < 1,500 ms | POST `/students`, PUT `/enrollments/{id}`, POST `/payments/{id}/record` |
| **D** Heavy / batch | **< 5,000 ms** | < 2,000 ms | < 10,000 ms | POST `/bulk-imports/students`, POST `/reports/transcripts`, `/admin/revenue/export` |
| **E** Async / queue-bound | N/A sync, SLO on queue | N/A | N/A | AI generation (see `kitehub-branding` queue SLOs — already documented) |
| **F** Health / infra | **< 50 ms** | < 10 ms | < 100 ms | GET `/actuator/health`, GET `/actuator/info`, GET `/readiness` |

### Why these numbers

- **Tier A (200 ms)** — UX research: perceived-instant threshold; above this,
  users notice lag on individual widgets.
- **Tier B (500 ms)** — Standard list-page budget; includes count query +
  paginated rows. Above 500 ms the user perceives a "loading" flash.
- **Tier C (800 ms)** — Writes include side effects (cache evict, outbox, event
  publish). Slightly looser than reads; above 800 ms form submissions feel broken.
- **Tier D (5 s)** — Batch jobs the user knows are heavy; still need an upper
  bound so a request doesn't stall a whole worker for minutes.
- **Tier E** — Inherits queue SLAs from `kitehub-branding` (free 180s / pro 60s
  / enterprise 30s); sync HTTP response is immediate (jobId returned).
- **Tier F (50 ms)** — Liveness/readiness probes; anything slower risks
  Kubernetes restart loops.

---

## 3. Tier Assignment Rubric

When you add a new endpoint, pick the tier using this flowchart:

```
Is the response read-only?
  └─ Yes → Is it a single-entity lookup by ID/slug?
            └─ Yes → Tier A
            └─ No (list/search/aggregate)? → Tier B
  └─ No (write/mutation)? → Tier C
  └─ Is it bulk (>100 rows, or multi-entity orchestration)? → Tier D
  └─ Does it trigger an async pipeline (AI, export, email batch)? → Tier E
  └─ Is it an operator/health endpoint? → Tier F
```

If you can't decide between two tiers, pick the **stricter** one — it is always
easier to relax an SLO than tighten it later.

---

## 4. Current Baseline (as of 2026-04-20 performance audit /100)

The performance audit baseline (PR #364, 58/100 — refreshed to 64/100 on
2026-04-20) could not grade p95 objectively because no SLOs existed. That's the
hole this document fills.

**Measured hot paths from the audit** (rough p95 at dev workload; not production
load — treat as order-of-magnitude):

| Endpoint | Tier | Current p95 (dev) | Status |
|----------|------|------------------:|--------|
| GET `/admin/dashboard/stats` | A | ~800 ms | 🔴 **violates** — listed in GAP-126 |
| GET `/students?page=0` (paginated) | B | ~250 ms | 🟢 within budget |
| POST `/enrollments` (single) | C | ~400 ms | 🟢 within budget |
| GET `/api/v1/branding/{id}/package` | A | ~80 ms (cache hit) / ~150 ms (miss) | 🟢 within budget |
| POST `/bulk-imports/students` (1000 rows) | D | ~12 s | 🔴 **violates** — listed in GAP-127 |
| POST `/invoices/{id}/payments` | C | ~600 ms | 🟡 near budget (GAP-128 N+1 offender) |

**Existing queue SLOs (Tier E, `kitehub-branding` `application.yml`):**
```yaml
queue:
  sla:
    free-p95-seconds: 180
    pro-p95-seconds: 60
    enterprise-p95-seconds: 30
```
These stay authoritative for async AI generation.

---

## 5. Tagging Convention (implementation)

Each controller class receives a class-level `@Timed` annotation with `slo` tag
so Prometheus + Grafana can aggregate by tier. Class-level (not method-level) is
the default because:

- It applies to every endpoint on the class with one annotation.
- The metric name `http.server.requests` is the same one Spring Boot Actuator
  auto-records, so the `slo` tag joins the existing per-URI dimensions
  (`uri`, `method`, `status`) — Prometheus rules slice by `slo` while the
  built-in `uri` label keeps per-endpoint detail.
- Method-level overrides remain valid when one endpoint diverges from the
  class default (e.g., a Tier B controller exposing one Tier D batch route).

```java
@RestController
@RequestMapping("/api/v1/students")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-b", "controller", "student"})
public class StudentController { ... }

// Method-level override for a single Tier D batch route on a Tier B controller:
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-d", "controller", "student", "endpoint", "bulk-export"})
@GetMapping("/export.xlsx")
public ResponseEntity<Resource> exportAll() { ... }
```

Required infra per service:

1. `spring-boot-starter-aop` on the classpath (already in `kiteclass-core`;
   added Wave perf-D for `kitehub-subscription`, `kitehub-branding`,
   `kitehub-email`).
2. A `MetricsConfig` bean exposing `TimedAspect` so `@Timed` is honoured
   (`com.{kitehub.subscription,kitehub.branding,kitehub.email,kiteclass.core.common}.config.MetricsConfig`).
3. The same bean registers a `MeterFilter` that turns on
   `percentilesHistogram(true)` for `http.server.requests`, so Prometheus
   `histogram_quantile()` returns real percentiles instead of NaN.

### 5.1 Coverage status (as of Wave perf-D 2026-04-26)

| Service | Controllers tagged | Notes |
|---------|-------------------:|-------|
| `kitehub-subscription` | 5/10 | Auth, Instance, Payment, Subscription, Domain — rest as follow-up |
| `kitehub-branding` | 5/5 | All controllers covered |
| `kitehub-email` | 1/1 | All controllers covered |
| `kiteclass-core` | 5/13 | Student, Enrollment, Grade, BulkImport, BrandingSettings — rest as follow-up |
| `kitehub-admin` | 0/n | Owned by Agent A in Wave perf-D — separate cluster fix |
| `kitehub-gateway` | 0/n | Reverse-proxy; instrument when proxied-pass timing surfaces gap |

Out-of-scope controllers will be picked up by the follow-up gap referenced in
GAP-135 §Log.

---

## 5b. SLO → Alert Mapping

Each tier maps to a single PrometheusRule alert in
`infrastructure/helm/kitehub/templates/prometheusrule.yaml` (`api-latency-slo-alerts`
group). The rule uses `histogram_quantile()` over the
`http_server_requests_seconds_bucket` series filtered by the `slo` label.

| Tier | Alert name | p95 budget | `for:` window | Severity | Action on fire |
|------|------------|-----------:|---------------|----------|----------------|
| A | `ApiLatencyP95HighTierA` | 200 ms | 10 m | warning | Investigate cache miss / N+1 / downstream timeout |
| A | `ApiLatencyP99CriticalTierA` | 400 ms (p99) | 5 m | critical | Page on-call — interactive UX broken |
| B | `ApiLatencyP95HighTierB` | 500 ms | 10 m | warning | Profile list query, check pagination cost |
| C | `ApiLatencyP95HighTierC` | 800 ms | 10 m | warning | Verify outbox / event publish on hot path |
| D | `ApiLatencyP95HighTierD` | 5 s | 10 m | warning | Confirm batch is async; check worker count |
| E | _Inherits queue SLAs_ | per-tier (180 s / 60 s / 30 s) | n/a | n/a | See `kitehub-branding/application.yml` queue.sla |
| F | _Covered by `HighResponseTime`_ | 50 ms | 5 m | warning | Health probe slowness — check restart loop risk |

### How to tune

1. **Tighten an SLO:** decrease the threshold in the alert `expr` (e.g.,
   Tier A from `> 0.2` to `> 0.15`). Always relax first; never tighten without
   migration plan (see §8 anti-patterns).
2. **Lengthen the `for:` window:** flapping endpoints fire often but recover —
   raise from `10m` to `15m` rather than relax the budget.
3. **Add a per-route override:** if one endpoint in a Tier B controller is
   chronically near budget, add a separate rule with
   `{uri="/api/v1/students/search"}` filter so the noisy endpoint has its own
   SLO + history without dragging the whole tier.
4. **Disable a tier alert:** comment out the rule and file a gap explaining
   why. Never silently delete — auditors compare alert presence to baseline.

### Dashboard

`infrastructure/helm/kitehub/dashboards/api-latency.json` — auto-loaded into
Grafana as a ConfigMap when `monitoring.dashboards.enabled=true` (gated by
GAP-143 Grafana enablement). Panels:

1. p50 / p95 / p99 per route — joined `histogram_quantile()` queries
2. Request rate per route — `rate(...count[5m])`
3. 5xx error rate per route — error count / total count
4. Per-tier p95 stat tiles (A / B / C / D) — quick-glance SLO budget vs current
   value with thresholds matching the alert rules.

---

## 6. Relationship to Other Documents

| Doc | Relationship |
|-----|--------------|
| `documents/04-quality/audits/performance/performance-audit-2026-04-19.md` | Baseline audit that identified the SLO gap (GAP-135) |
| `documents/04-quality/audits/performance/performance-audit-2026-04-20.md` | Follow-up audit — uses this doc to grade latency once SLOs land |
| `.claude/rules/post-wave-audit-mandate.md` §2.3 | Performance audit cadence — this doc is its scoring rubric input |
| `kitehub/kitehub-branding/src/main/resources/application.yml` | Authoritative Tier E queue SLOs |
| `documents/05-guides/operations/incident-response-runbook.md` | Runbook references SLO breaches as page-worthy signals (GAP-086) |
| GAP-126 (admin dashboard) | Tier A violation — tracked separately |
| GAP-127 (bulk import) | Tier D violation — tracked separately |
| GAP-128 (installment payment N+1) | Tier C near-breach — tracked separately |
| GAP-134 (@EntityGraph) | Prevention — reduces N+1 impact on Tier A/B |

---

## 7. Enforcement

1. **PR template** — every PR adding a new controller method MUST declare its tier in the description:
   ```
   ## SLO Declaration
   - Endpoint: POST /foo/{id}
   - Tier: C (write)
   - Rationale: mutating, side-effect scope = single entity
   ```
2. **`check-pr` / `pr-health` skills** — will flag PRs that add a `@PostMapping`/`@GetMapping` without an SLO declaration (follow-up wiring tracked under GAP-135 AC).
3. **Performance audit /100** — uses this document as the rubric; endpoints without SLO or breaching SLO deduct points.
4. **Prometheus alert rule template** (to land with the `@Timed` wiring PR):
   ```yaml
   - alert: HighP95Latency
     expr: histogram_quantile(0.95, sum by (slo, le) (rate(http_api_seconds_bucket[5m])))
           > on (slo) group_left (slo_budget_seconds)
           slo_budget_seconds
     for: 15m
     severity: warning
   ```

---

## 8. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Ship a new endpoint without declaring its tier | Pick tier via §3 rubric; put it in the PR description |
| Tighten an SLO retroactively on existing endpoints without migration plan | Relax or split the endpoint first (e.g., separate "summary" vs "detail") |
| Average p50 across tiers — it washes out the worst offenders | Monitor per-tier; alert per-tier |
| Use `sum` of request times as a budget | Use percentiles (p95/p99); sums favour bulk endpoints incorrectly |
| Skip Tier F "because health probes are fast" | Slow health probes = restart loops; budget them |

---

## 9. Log

- **2026-04-26 (v0.2 — Wave perf-D Agent D):** AC follow-ups landed.
  - `@Timed` class-level annotations on 16 controllers across 4 services
    (kitehub-subscription, kitehub-branding, kitehub-email, kiteclass-core).
  - `MetricsConfig` bean per service registers `TimedAspect` + `MeterFilter`
    that forces percentile-histogram emission on `http.server.requests`.
  - `spring-boot-starter-aop` added to 3 service POMs that lacked it.
  - 5 new Prometheus alerts (Tier A / B / C / D p95 + Tier A p99 critical) in
    `infrastructure/helm/kitehub/templates/prometheusrule.yaml`.
  - Grafana dashboard `infrastructure/helm/kitehub/dashboards/api-latency.json`
    (8 panels: latency p50/p95/p99 per route, request rate, error rate,
    per-tier SLO stat tiles).
  - ConfigMap template + `monitoring.dashboards.enabled` toggle in `values.yaml`.
  - §5 expanded with class-level annotation pattern + coverage table.
  - §5b new section: SLO → Alert mapping + how to tune.
  - Out-of-scope controllers (kitehub-admin under Agent A; remaining
    controllers in subscription / kiteclass-core) tracked under follow-up gap.
- **2026-04-21 (v0.1):** Document created as Wave 9-E deliverable closing GAP-135 (draft portion). Draws tier definitions from the gap's Proposed Fix and the performance audit baseline. `@Timed` wiring + Grafana dashboard + Prometheus alert rules remain Acceptance Criteria follow-ups on GAP-135 — those PRs will reference this document as the rubric.
