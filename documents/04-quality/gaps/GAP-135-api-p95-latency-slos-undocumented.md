# GAP-135: No p95 API latency SLOs documented — perf regressions invisible

**Status:** 🟡 PARTIAL — SLO rubric document landed in Wave 9-E (`documents/05-guides/api-performance-slo.md`); `@Timed` tag wiring + Prometheus alerts + Grafana dashboard remain AC follow-ups
**Priority:** 🟡 P2
**Domain:** Observability / Performance / Governance
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** All HTTP endpoints across 8 services
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

The only service-level objective documented anywhere in the codebase is the AI fair-queue p95 (kitehub-branding application.yml):
```yaml
queue:
  sla:
    free-p95-seconds: 180
    pro-p95-seconds: 60
    enterprise-p95-seconds: 30
```

For all other HTTP endpoints (200+ across the platform): **no p95 budget, no SLO, no alert**.

Consequences:
- A PR that doubles admin dashboard latency ships undetected.
- Customer complaints are the only regression signal.
- Load tests (when run) have no target to validate against.
- `ops-readiness-audit` (next in catch-up plan) has no "latency SLO" acceptance criterion to grade against.

## Context

Prometheus + Micrometer are already wired (`management.endpoints.web.exposure.include: prometheus,metrics`). The measurement infrastructure exists; the policy does not.

## Evidence

- `grep 'p95|sla|slo' **/application*.yml` → only kitehub-branding matches
- `grep 'p95' documents/` → scattered mentions in gap files, no authoritative target
- Performance audit §2

## Proposed Fix

1. Draft `documents/02-architecture/adr/XXX-api-latency-slos.md` with tier definitions:
   - **Tier A (interactive, p95 < 200ms):** GET /students/{id}, GET /classes/{id}, dashboard widgets
   - **Tier B (list pages, p95 < 500ms):** GET /students, GET /classes, /admin/instances
   - **Tier C (write, p95 < 800ms):** POST /students, PUT /enrollments
   - **Tier D (batch/heavy, p95 < 5s):** bulk import, report generation
   - **Tier E (async, queue-bound):** AI generation (already has SLOs)
2. Tag each endpoint with `@Timed(value = "http.api", extraTags = {"slo", "tier-a"})`.
3. Grafana dashboard + Prometheus alert: fire if 5min rolling p95 > SLO for 3 consecutive periods.
4. Add SLO check to PR template: "If you added a new endpoint, declare its SLO tier."
5. Integrate into `check-pr` / `pr-health` skills.

## Acceptance Criteria

- [x] SLO rubric document with tier definitions + assignment rubric — shipped Wave 9-E as `documents/05-guides/api-performance-slo.md` (classified as operational guide, not ADR, because it is measurable runtime policy — ADR may still be filed later if the tier *values* prove contentious)
- [ ] Every controller method tagged with `@Timed` + `slo` tag — deferred, estimated 1 PR per service (8 services × <1 hr) — tracked as follow-up under this gap
- [ ] Prometheus alert rules committed in `infrastructure/` — deferred, depends on `@Timed` wiring above
- [ ] Grafana dashboard JSON committed — deferred, depends on alert rules above
- [ ] PR template + `check-pr` skill reference the SLO declaration requirement — deferred meta follow-up (1-2 lines in PR template)

## Related

- Audit: performance-audit-2026-04-19.md §2
- Ops-readiness audit (upcoming Audit 2)
- GAP-086 (incident response runbook — DONE, but has no latency SLO section to reference; SLO doc now lands so §runbook can link to it in future edit)
- GAP-043 (cache stampede) — complementary; cache reduces p95 on cacheable endpoints, SLO doc sets the budget

## Log

- 2026-04-21 — Wave 9-E: SLO rubric document shipped at `documents/05-guides/api-performance-slo.md`. 6-tier model (A interactive / B list / C write / D batch / E async / F health) with concrete p50/p95/p99 budgets. Captures 2026-04-20 audit baseline as "current vs target" so the next performance audit can grade objectively. `@Timed` wiring + Prometheus + Grafana are independent PR work tracked under remaining AC.
- 2026-04-19 — Gap created from performance baseline audit
