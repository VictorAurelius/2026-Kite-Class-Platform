# GAP-135: No p95 API latency SLOs documented — perf regressions invisible

**Status:** 🔵 OPEN
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

- [ ] ADR committed with tier definitions + assignment rubric
- [ ] Every controller method tagged with `@Timed` + `slo` tag
- [ ] Prometheus alert rules committed in `infrastructure/`
- [ ] Grafana dashboard JSON committed
- [ ] PR template + `check-pr` skill reference the SLO declaration requirement

## Related

- Audit: performance-audit-2026-04-19.md §2
- Ops-readiness audit (upcoming Audit 2)
- GAP-086 (incident response runbook — DONE, but has no latency SLO section to reference)

## Log

- 2026-04-19 — Gap created from performance baseline audit
