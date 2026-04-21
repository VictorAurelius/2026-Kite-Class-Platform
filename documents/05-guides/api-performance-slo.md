# API Performance — Service Level Objectives (SLOs)

**Status:** 🟡 BASELINE (v0.1, 2026-04-21 — first-run document per GAP-135)
**Priority:** 🟠 MANDATORY (see §7 Enforcement)
**Owner:** Platform / SRE
**Closes:** GAP-135 (draft deliverable; Prometheus alerts + @Timed tag wiring remain AC follow-ups)
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

## 5. Tagging Convention (implementation — follow-up work)

Each controller method will receive a `@Timed` annotation with `slo` tag so
Prometheus + Grafana can aggregate by tier:

```java
@Timed(value = "http.api", extraTags = {"slo", "tier-a", "endpoint", "students-by-id"})
@GetMapping("/students/{id}")
public StudentDTO getStudent(@PathVariable Long id) { ... }
```

**This tagging is scheduled under GAP-135 AC — the writeup in this file unblocks
the Grafana dashboard + alert-rule PR that follows.**

---

## 6. Relationship to Other Documents

| Doc | Relationship |
|-----|--------------|
| `documents/04-quality/audits/performance/performance-audit-2026-04-19.md` | Baseline audit that identified the SLO gap (GAP-135) |
| `documents/04-quality/audits/performance/performance-audit-2026-04-20.md` | Follow-up audit — uses this doc to grade latency once SLOs land |
| `.claude/rules/post-wave-audit-mandate.md` §2.3 | Performance audit cadence — this doc is its scoring rubric input |
| `kitehub/kitehub-branding/src/main/resources/application.yml` | Authoritative Tier E queue SLOs |
| `documents/05-guides/incident-response-runbook.md` | Runbook references SLO breaches as page-worthy signals (GAP-086) |
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

- **2026-04-21 (v0.1):** Document created as Wave 9-E deliverable closing GAP-135 (draft portion). Draws tier definitions from the gap's Proposed Fix and the performance audit baseline. `@Timed` wiring + Grafana dashboard + Prometheus alert rules remain Acceptance Criteria follow-ups on GAP-135 — those PRs will reference this document as the rubric.
