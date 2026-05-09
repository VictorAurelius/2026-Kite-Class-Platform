# GAP-453: E2E Phase B — docker-compose stack in CI for KH+KC frontend E2E gates

**Status:** 🟡 PARTIAL — KH side DONE 2026-05-09 (Phase B Option B.2 narrow-subset gate); KC side deferred to GAP-454
**Priority:** 🟠 P1
**Domain:** DevOps / CI
**Found:** 2026-05-09 (Wave 47 Phase A pre-flight verify aborted)
**Affects:** `.github/workflows/frontend-ci.yml` (KC E2E job line 90 `if: false` — DEFERRED to GAP-454), `.github/workflows/kitehub-frontend-ci.yml` (KH E2E job — `if: true` ACTIVATED 2026-05-09); blocks GAP-403/404/420 closure (PARTIAL on KH path only)

## Problem

Wave 47 Phase A trial (mechanical flag flip `if: false` → `if: true` in 2 workflows) was aborted at pre-flight verify. Plan recon assumed E2E specs in `kiteclass-frontend/e2e/critical-journeys/` (17 tests) and `kitehub-frontend/e2e/beta-funnel/` (5 tests) were route-mocked via Playwright `page.route()` and thus would pass without backend stack. Local verify on 2026-05-09 disproved this:

### Evidence

**KC critical-journeys (chromium-only):**
- 17 tests run, **11 passed, 6 failed** (Wave 47 plan §1.3 expected 17/17).
- Failure mode: `expect(page).toHaveURL('/dashboard') failed` after login form submit. The login flow itself is NOT route-mocked — only 2 `page.route` hits in entire `critical-journeys/` folder (both in `class-lifecycle.spec.ts` mocking `/api/v1/classes/1`). `dashboard-navigation.spec.ts` and `course-to-class-flow.spec.ts` rely on real `/api/v1/auth/login` → Gateway → Core flow.
- Running full `pnpm test:e2e` (entire `e2e/` folder, 625 tests across 5 browser projects): 116 passed, ~499 fail or did-not-run.

**KH beta-funnel (chromium-only):**
- 5 tests passed (request-flow + admin-approve + signup-with-claim-code) — **scope plan called out PASS confirmed.**
- BUT `pnpm test:e2e:ci` runs ENTIRE `e2e/` folder. `auth.spec.ts` (login/logout) and `billing.spec.ts` failed (timed out at 30s waiting for backend that doesn't exist). KH suite timed out at 5min wall-clock during local verify.

### Root cause

Wave 47 plan recon §3 §State-Check Evidence rows were narrow ("KC critical-journeys 17 tests" + "KH beta-funnel 5 tests") but the workflow scripts (`pnpm test:e2e` for KC, `pnpm test:e2e:ci` for KH) run the ENTIRE `e2e/` folder, not just the route-mocked subsets. Most other specs (auth, billing, classes, students, branding, theme, attendance, feature-flags) depend on real backend.

Additional plan recon errors found:
- Plan §4 row stated KC `playwright.config.ts:14 fullyParallel:false`. Reality: `fullyParallel: true` (line 9). 5 browser projects (chromium / firefox / webkit / Mobile Chrome / Mobile Safari) compound test count.
- Plan assumed `NEXT_PUBLIC_API_URL: http://localhost:9000` was a smoking gun in `kitehub-frontend/package.json` line 20. Reality: that line is `start-server-and-test 'next dev --port 4701' http://localhost:4701 'playwright test'` — only waits for Next dev server (4701), not Gateway (9000). The `localhost:9000` env var IS injected at workflow job level (`kitehub-frontend-ci.yml:294`), but that's a no-op when specs route-mock.

## Root Cause (deeper)

Two-layer problem:
1. **Workflow scope** — both jobs run ENTIRE `e2e/` folder (`pnpm test:e2e` / `pnpm test:e2e:ci`), not just route-mocked subsets. Even if all route-mocked tests pass, real-backend tests will fail/timeout.
2. **Test-file scope** — within `critical-journeys/` itself, only 2/3 spec files have route-mocks; `dashboard-navigation.spec.ts` + `course-to-class-flow.spec.ts` need real auth flow.

Phase A "trial flag flip" approach assumed both layers were already route-mock-clean. Neither is.

## Proposed Fix (Phase B)

Pick ONE path:

### Option B.1 — Stack-in-CI via docker-compose (per Wave 47 plan §3 fallback)

Wire `docker-compose -f kitehub/docker-compose.kitehub.yml up -d` step before E2E job in both workflows, plus `wait-on http://localhost:9000/actuator/health`. Stack includes Gateway + Core + Postgres + Redis + RabbitMQ + MinIO. Cost: +5-8 min CI per PR per workflow, +1-2h plan effort for wiring + secret/env vars + smoke health gate.

Pros: tests run against actual product stack — highest signal.
Cons: heavy CI resource usage; Gateway boot time eats CI minutes; flake risk.

### Option B.2 — Narrow E2E job to route-mocked subset only

Modify `pnpm test:e2e:ci` (KH) and add `pnpm test:e2e:critical` (KC) scripts that pass `--grep` or specific test paths to Playwright, including ONLY route-mocked specs:

KH script: `playwright test e2e/beta-funnel/`
KC script: `playwright test e2e/critical-journeys/class-lifecycle.spec.ts` (only this file has route-mocks per Wave 47 recon)

Cost: ~30 min plan + script + workflow update + verify. Lower CI cost than B.1.

Pros: faster CI, no infra, route-mock paths are by design self-contained.
Cons: narrow coverage; tests outside subset stay un-gated until real-backend-in-CI lands.

### Option B.3 — MSW migration (per Wave 47 plan §1 Q2 deferred option C)

Migrate KC + KH tests to use MSW handlers instead of inline `page.route()`. 2-4h KC + 1h KH effort. Out of scope for this gap; track separately if Option B.2 chosen.

### Recommendation

**Option B.2 first** as smallest reversible step (matches Wave 47 LOW-stake principle). If subset coverage proves insufficient, escalate to B.1.

## Acceptance Criteria

- [x] Phase B option chosen (B.1 or B.2) with explicit rationale in PR body — **B.2 chosen**, fix-PR body documents
- [x] If B.2: new scripts `test:e2e:gates` (KC + KH) running ONLY route-mocked subsets; workflows updated to use these scripts; `if: false` flipped to `if: true` only after subset-only scripts verified locally green — **KH only**: `test:e2e:gates` + `test:e2e:gates:ci` added; workflow flipped + script updated. KC scripts NOT shipped — narrow subset insufficient (2/6 pass); see GAP-454.
- [ ] ~~If B.1: docker-compose-in-CI step added with `wait-on` health gate; Gateway port 9000 health verified before Playwright runs~~ — N/A (chose B.2)
- [x] Local verify: subset (or full stack) test command passes 100% locally on chromium — **KH 5/5 pass in 8.6s**; KC 2/6 (deferred GAP-454)
- [ ] CI run on the fix-PR branch shows the newly-activated job(s) green — **pending after fix-PR push**
- [x] PR body documents pass count + chosen option + rationale per `release-fix-retry-budget.md` discipline — see fix-PR body
- [x] PARTIAL exit ramp: KC follow-up filed (GAP-454) per `gap-done-discipline.md` §3

## Related

- Parent gaps: GAP-403 (KC E2E gate), GAP-404 (KH E2E gate), GAP-420 (E2E activation umbrella)
- Children: **GAP-454** (KC narrow-subset investigation P2) · **GAP-455** (KH beta-funnel coverage extension P2 — surfaced 2026-05-09 user audit: gate ships narrow ~28% scenario coverage; extend to ~80% in follow-up)
- Wave 47 plan: `documents/03-planning/waves/wave-47-e2e-activation.md`
- Wave 47 Phase A PR: (this wave's PR — closed without merge after pre-flight abort)
- KC workflow: `.github/workflows/frontend-ci.yml:90`
- KH workflow: `.github/workflows/kitehub-frontend-ci.yml:237`

Note KC workflow has `pnpm test:e2e || true` + `continue-on-error: true` (lines 117-118) which would render the gate non-blocking even if flipped. Phase B must address this — either remove the swallows OR justify advisory-only mode explicitly.

## Log

- **2026-05-09** Filed after Wave 47 Phase A pre-flight verify aborted. KC critical-journeys chromium 11/17 pass, KH beta-funnel chromium 5/5 pass, but full-folder E2E suites both fail because workflows run entire `e2e/` folder including real-backend-dependent specs (auth, billing, classes detail, etc.). Plan recon assumed narrow route-mock-only scope; reality is wider. Phase B docker-compose fallback OR subset-only scripts required before E2E gate can be activated as blocking.
- **2026-05-09** Status flipped 🔵 OPEN → 🟡 PARTIAL. Phase B Option B.2 shipped for **KH only**. Local verify revealed KC narrow subset insufficient: `class-lifecycle.spec.ts` 2/6 pass under chromium route-mock-only because 4 tests use `navigateToClassDetail()` helper that hits unmocked `/api/v1/classes` listing. KH `beta-funnel/` 5/5 pass in 8.6s (fully self-contained). Per `release-fix-retry-budget.md` §3 decision flow at retry #1 (Phase A = retry #0) + `gap-done-discipline.md` §3 PARTIAL exit ramp: ship KH, defer KC to GAP-454 follow-up with 4 path options (route-mock refactor / direct-navigation / MSW migration / docker-compose escalation). KH side closes its slice of GAP-403/404/420; KC slice remains pending until GAP-454 lands.
