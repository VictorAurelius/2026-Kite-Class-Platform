---
title: Wave 7-Perf — Performance Audit Follow-ups (4 parallel agents)
status: complete
created: 2026-04-26
updated: 2026-04-28
gaps: [GAP-126, GAP-127, GAP-130, GAP-135]
parent_audit: documents/04-quality/audits/performance/performance-audit-2026-04-19.md
parent_session: 20260426-164325
consolidation_pr: 575
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 7-Perf — Performance Audit Follow-ups

**Status:** ✅ COMPLETE (closed 2026-04-26 via consolidation PR #575)
**Outcomes:**
- GAP-126 Admin dashboard cache → 🟢 DONE (PR #569)
- GAP-127 FE code-splitting → 🟡 PARTIAL (PR #570 — bundle analyzer + ≤10 pages each app; remaining 44+ pages tracked as follow-up)
- GAP-130 Docker resource limits → ✅ DONE
- GAP-135 SLO instrumentation → 🟡 PARTIAL (PR #571 — `@Timed` on 16 controllers + Prom rules + Grafana; residual controllers tracked as follow-up)

**Trigger:** Performance baseline audit 2026-04-19 (58/100 F → 64 D after Part B). 4 P0/P1 gaps remain open after Wave 9.5 caching/EntityGraph fixes (GAP-131/132/134/146 closed).
**Strategy (executed):** 4 parallel `isolation: worktree` agents; lead owns ROADMAP consolidation.

---

## Wave-eligibility verification (Step 0)

| Q | Answer |
|---|--------|
| ≥3 sub-tasks? | ✅ YES — 4 gaps |
| Disjoint files? | ✅ YES — admin-svc / both-FE / docker-compose / infra-helm (no overlap) |
| Self-contained TDD/build? | ✅ YES — each agent runs own Maven/pnpm/promtool/docker validate |

→ Wave-eligible. Spawn 4 agents.

---

## Agent assignments

### Agent A — GAP-126 Admin dashboard Redis cache
**Branch:** `feature/wave-perf-A-gap-126-admin-cache`
**Files (exclusive):**
- `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/service/AnalyticsService.java`
- `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/AdminController.java`
- `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/PaymentController.java` (getAllPayments only)
- `kitehub/kitehub-admin/src/test/java/com/kitehub/admin/service/AnalyticsServiceTest.java`
- `kitehub/kitehub-admin/src/main/resources/application.yml` (cache config only — guarded)
**Out-of-bounds:** any other service, any docker-compose file, any FE file
**Acceptance:** §AC of GAP-126; integration test asserts <5 SQL/dashboard request

### Agent B — GAP-127 FE code-splitting
**Branch:** `feature/wave-perf-B-gap-127-fe-code-splitting`
**Files (exclusive):**
- `kiteclass/kiteclass-frontend/next.config.js`
- `kiteclass/kiteclass-frontend/package.json` (add `@next/bundle-analyzer`)
- `kiteclass/kiteclass-frontend/src/app/**/page.tsx` (≤10 highest-impact pages — NOT all 64; prioritize admin dashboard + reports)
- `kitehub/kitehub-frontend/next.config.js`
- `kitehub/kitehub-frontend/package.json`
- `kitehub/kitehub-frontend/src/app/**/page.tsx` (≤10 highest-impact pages)
**Out-of-bounds:** any backend file, any docker file, any infra file
**Scope cap:** 10 pages each FE app maximum; document remaining 44+ pages as follow-up gap
**Acceptance:** Bundle analyzer wired; First Load JS < 300KB on 5 highest-traffic routes; lighthouse-ish manual measurement documented

### Agent C — GAP-130 Docker resource limits
**Branch:** `feature/wave-perf-C-gap-130-docker-limits`
**Files (exclusive):**
- `kitehub/docker-compose.kitehub.yml`
- `kitehub/docker-compose.kitehub-only.yml`
- `kitehub/docker-compose.oracle-backend.yml`
- `kitehub/docker-compose.oracle-frontend.yml`
**Out-of-bounds:** any code file, any helm/k8s file, any infra/terraform
**Acceptance:** All services declare `deploy.resources.limits` (memory + cpus); validate with `docker compose config`; document chosen limits in `documents/05-guides/monitoring/docker-resource-limits.md`

### Agent D — GAP-135 Performance SLO instrumentation
**Branch:** `feature/wave-perf-D-gap-135-slo-instrumentation`
**Files (exclusive):**
- `infrastructure/helm/kitehub/templates/prometheus-rules.yaml` (or values.yaml additions)
- `infrastructure/helm/kitehub/values.yaml` (Grafana dashboard ConfigMap reference)
- `infrastructure/helm/kitehub/dashboards/api-latency.json` (NEW)
- Any `@RestController` files needing `@Timed` annotation — but **scope cap to 5 highest-traffic controllers** per service
**Out-of-bounds:** any docker-compose file, any FE file, any kitehub-admin code (Agent A territory)
**Scope cap:** Apply `@Timed` to ≤5 controllers/service (auth, instance, branding, payment, dashboard); rest as follow-up gap
**Acceptance:** Prometheus rules fire on p95 > SLO; Grafana dashboard renders; document in `documents/05-guides/monitoring/api-performance-slo.md` (extend existing)

---

## Hard rules (per `feedback_parallel_agent_strategy.md`)

1. **No agent touches ROADMAP.md, MEMORY.md, output-review-mandate.md, or any application.yml beyond own service** — parent consolidates post-merge
2. **GAP file updates** — each agent updates only their own GAP-XXX file's Log section + Status (e.g. OPEN → DONE)
3. **Worktree path** — agents work in `/tmp/claude-worktree-<agent>` or `~/.claude/worktrees/`; do NOT write to main repo working copy
4. **No new gaps filed by agents** — if agent finds out-of-scope issues, return them in agent summary; parent files follow-up gaps
5. **Migration version slots** — none required (no DB changes in this wave)
6. **Test-profile escape hatch** — if agent adds @Component requiring config, ensure `@ConditionalOnProperty` or test profile bypass

## Consolidation (parent post-merge)

After all 4 PRs merged:
1. Update `ROADMAP.md` Current Status Snapshot with wave-7-perf entry
2. Mark GAP-126/127/130 as DONE; GAP-135 partial → DONE (or refile follow-up)
3. Update Block GA tier counts
4. Memory entry: `feedback_wave_7_perf_retro.md` if any new lessons surface

## Risk mitigation

| Risk | Mitigation |
|------|------------|
| Agent B (FE code-splitting) too broad — 64 pages | Hard cap 10 pages each app; refile 44+ as follow-up gap |
| Agent D (Helm) needs running cluster | Scope to YAML config + promtool validation only; no live cluster test |
| Test failures cascading across modules | Each agent runs only own module tests |
| `docker compose config` validation fails | Agent C uses `docker compose -f <file> config` to verify YAML before commit |
| Grafana dashboard JSON malformed | Agent D uses `jq` validation + reference existing dashboards in repo |
