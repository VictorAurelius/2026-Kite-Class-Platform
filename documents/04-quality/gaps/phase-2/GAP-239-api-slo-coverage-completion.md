# GAP-239: API SLO coverage completion + PR template SLO declaration

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (parent GAP-135 covered foundation + 16/29 high-traffic controllers; remaining 13 controllers + governance enforcement are polish)
**Domain:** Observability / Performance / Process
**Detected:** 2026-04-26 (Wave 7-Perf Agent D return finding)
**Related:** Parent GAP-135 (PARTIAL after Wave 7-Perf)

## Current State (verified 2026-04-26)

Wave 7-Perf Agent D (PR #571) shipped:
- ✅ `@Timed` on 16 controllers across 4 services
- ✅ 5 Prometheus rules in `api-latency-slo-alerts` group (P95 tier A/B/C/D + P99 critical tier A)
- ✅ 8-panel Grafana dashboard JSON (schema 39, jq-validated)
- ✅ `MetricsConfig` bean per service registers `TimedAspect`
- ✅ `api-performance-slo.md` extended with §"SLO → Alert mapping" + tuning guide

Remaining (Agent D return finding):
- ❌ 13 controllers NOT yet annotated:
  - kitehub-subscription (5): AdminEmail, PaymentWebhook, TrialToPaid, PublicConfig, AdminMigration
  - kiteclass-core (8): Storage, InternalStudent, BrandingVersion, UserPreferences, plus class/course/attendance/teacher/parent module controllers
- ❌ kitehub-admin: ALL controllers skipped (Agent A territory in Wave 7-Perf — no overlap)
- ❌ No PR template enforcement preventing new endpoints shipping without tier choice

## Problem

Without 100% controller coverage:
1. Endpoints in unannotated controllers don't emit `http.server.requests` percentile histograms → Prometheus rules can't fire on them → silent SLO breaches
2. New PRs can ship endpoints without `@Timed` annotation → coverage drifts down over time
3. `kitehub-admin` controllers are P1 (admin SLA expectations) but currently zero coverage in Wave 7-Perf scope

## Proposed Fix

### Sub-task A: Annotate remaining 13 + admin controllers
- 5 subscription, 8 kiteclass-core, all kitehub-admin = ~25 controllers
- Same pattern as Wave 7-Perf Agent D: class-level `@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99})`
- Choose tier (A/B/C/D) per controller's SLA expectation per `api-performance-slo.md` rubric

### Sub-task B: PR template SLO line
Add to `.github/pull_request_template.md`:
```
## API/SLO Declaration (required if PR adds/modifies @RestController endpoints)
- [ ] Endpoint(s) annotated with `@Timed` at controller class level
- [ ] SLO tier chosen (A=critical / B=normal / C=batch / D=admin) — see `documents/05-guides/monitoring/api-performance-slo.md`
```

### Sub-task C: `check-pr` / `pr-health` skill check
- Skill detects PR adds new `@RestController`-annotated class
- Asserts class has `@Timed` annotation
- Failure = PR check fail

## Acceptance Criteria

- [ ] All ~29 controllers annotated (16 from Wave 7-Perf + 13 remaining + admin coverage)
- [ ] PR template includes SLO declaration checkbox
- [ ] `check-pr` or `pr-health` skill enforces `@Timed` on new controllers
- [ ] Coverage table in `api-performance-slo.md` shows 100%

## Out-of-scope

- Per-endpoint (method-level) `@Timed` — class-level sufficient for most cases
- Custom histogram buckets per endpoint — defer until tuning data shows need

## Related

- Parent: GAP-135 (PARTIAL → DONE pending this gap)
- Doc: `documents/05-guides/monitoring/api-performance-slo.md`
- Rules: `.claude/rules/output-review-mandate.md` (output review mandate ensures coverage)
- Skill: `.claude/skills/workflow/check-pr/`, `.claude/skills/workflow/pr-health.md`

## Log

- **2026-04-26** — Filed during Wave 7-Perf consolidation. Agent D's scope cap (≤5 controllers/service) intentionally left residual; this gap closes the gap.
