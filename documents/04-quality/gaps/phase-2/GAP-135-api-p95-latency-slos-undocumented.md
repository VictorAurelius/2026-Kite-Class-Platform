# GAP-135: No p95 API latency SLOs documented — perf regressions invisible

**Status:** 🟡 PARTIAL — SLO rubric, `@Timed` instrumentation on 16 controllers (4 of 5 services), Prometheus rules (5 alerts) and Grafana dashboard all shipped Wave perf-D 2026-04-26. Remaining: tag the rest of subscription/kiteclass-core controllers (≈13) plus full kitehub-admin coverage (Agent A territory). Closing the residual is a smaller follow-up gap.
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

- [x] SLO rubric document with tier definitions + assignment rubric — shipped Wave 9-E as `documents/05-guides/monitoring/api-performance-slo.md` (classified as operational guide, not ADR, because it is measurable runtime policy — ADR may still be filed later if the tier *values* prove contentious)
- [x] Highest-traffic controllers tagged with `@Timed` + `slo` tag — Wave perf-D: 16 controllers across 4 services (subscription 5, branding 5, email 1, kiteclass-core 5). Class-level annotation per `documents/05-guides/monitoring/api-performance-slo.md` §5. Remaining controllers tracked as follow-up.
- [x] Prometheus alert rules committed in `infrastructure/` — Wave perf-D: 5 rules (`ApiLatencyP95HighTier{A,B,C,D}` + `ApiLatencyP99CriticalTierA`) added to `infrastructure/helm/kitehub/templates/prometheusrule.yaml` group `api-latency-slo-alerts`.
- [x] Grafana dashboard JSON committed — Wave perf-D: `infrastructure/helm/kitehub/dashboards/api-latency.json` (8 panels: p50/p95/p99 per route, request rate, error rate, per-tier SLO stat tiles). Auto-loaded via ConfigMap when `monitoring.dashboards.enabled=true`.
- [ ] PR template + `check-pr` skill reference the SLO declaration requirement — deferred meta follow-up (1-2 lines in PR template)
- [ ] Remaining controllers tagged (subscription 5, kiteclass-core 8, kitehub-admin all) — follow-up gap.

## Related

- Audit: performance-audit-2026-04-19.md §2
- Ops-readiness audit (upcoming Audit 2)
- GAP-086 (incident response runbook — DONE, but has no latency SLO section to reference; SLO doc now lands so §runbook can link to it in future edit)
- GAP-043 (cache stampede) — complementary; cache reduces p95 on cacheable endpoints, SLO doc sets the budget

## Log

- **2026-05-08 — Wave 41 Bucket F:** Dashboard catalog runbook shipped at `documents/05-guides/operations/runbooks/monitoring-dashboards.md` — catalogs all 5 baseline Grafana dashboards (api-latency / http-traffic / jvm-heap-gc / infra-pools / logs-overview) + 4-step alert→dashboard→runbook triage workflow tying `ApiLatencyP95High*` + `ApiLatencyP99Critical*` alerts to API Latency dashboard for on-call consumption. AC #4 (Grafana dashboard JSON committed) was already checked Wave perf-D; this bucket adds operator-facing runbook so SLO observability is actionable. Status remains 🟡 PARTIAL — AC #5 (PR template SLO declaration) + AC #6 (remaining ~13 controllers) are separate follow-ups.
- 2026-04-26 — Wave perf-D Agent D: SLO instrumentation suite shipped.
  - 16 controllers tagged class-level `@Timed(value="http.server.requests", percentiles={0.5,0.95,0.99}, extraTags={"slo","tier-X","controller","..."})`:
    - kitehub-subscription (5): Auth=tier-c, Instance=tier-b, Payment=tier-c, Subscription=tier-b, Domain=tier-c
    - kitehub-branding (5): BrandingJob=tier-b, AssetStorage=tier-d, TemplateGallery=tier-b, AIBranding=tier-c, ContentGeneration=tier-c
    - kitehub-email (1): Email=tier-c
    - kiteclass-core (5): Student=tier-b, Enrollment=tier-c, Grade=tier-b, BulkImport=tier-d, BrandingSettings=tier-a
  - `MetricsConfig` added to each service: registers `TimedAspect` bean + `MeterFilter` forcing `percentilesHistogram(true)` on `http.server.requests`.
  - `spring-boot-starter-aop` added to kitehub-subscription, kitehub-branding, kitehub-email POMs (kiteclass-core already had it).
  - PrometheusRule extended (`api-latency-slo-alerts` group): 5 alerts — `ApiLatencyP95HighTier{A,B,C,D}` (warning, 10m for) + `ApiLatencyP99CriticalTierA` (critical, 5m for).
  - Grafana dashboard `infrastructure/helm/kitehub/dashboards/api-latency.json` (8 panels) packaged via new `dashboard-api-latency.yaml` ConfigMap template, gated by `monitoring.dashboards.enabled` flag in values.yaml.
  - SLO doc extended with §5 instrumentation pattern + coverage table + §5b SLO→alert mapping & tuning guide. Status flipped 🟡 BASELINE → 🟢 ACTIVE.
  - kitehub-admin out-of-scope (Agent A wave territory). Remaining controllers in subscription + kiteclass-core (≈13 total) tracked under follow-up.
- 2026-04-21 — Wave 9-E: SLO rubric document shipped at `documents/05-guides/monitoring/api-performance-slo.md`. 6-tier model (A interactive / B list / C write / D batch / E async / F health) with concrete p50/p95/p99 budgets. Captures 2026-04-20 audit baseline as "current vs target" so the next performance audit can grade objectively. `@Timed` wiring + Prometheus + Grafana are independent PR work tracked under remaining AC.
- 2026-04-19 — Gap created from performance baseline audit
