# GAP-454: KC frontend E2E narrow-subset gate — investigation needed

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / CI / Frontend
**Found:** 2026-05-09 (GAP-453 Phase B local verify)
**Affects:** `.github/workflows/frontend-ci.yml` E2E job (kept `if: false` until this gap resolves), `kiteclass/kiteclass-frontend/e2e/`

## Problem

GAP-453 Phase B Option B.2 (narrow-subset gate) shipped **only for KH** because KC's intended subset (`e2e/critical-journeys/class-lifecycle.spec.ts`) only passes 2/6 tests under chromium-only route-mock-only mode.

Local verify on 2026-05-09 (commit before GAP-453 fix-PR):
```
$ pnpm -F kiteclass-frontend test:e2e:gates  (= playwright --project=chromium e2e/critical-journeys/class-lifecycle.spec.ts)
4 failed (start-and-complete / cancel / generate-code / display-sessions)
2 passed (delete-protection / invalid-id-error)
```

Failure mode: 4 failing tests share `navigateToClassDetail()` helper which does `await page.goto('/classes')` first → KC FE renders class-list page → fires unmocked GET `/api/v1/classes` → no backend → list page never loads class-1 link → `expect(page).toHaveURL('/classes/1')` times out.

The 2 passing tests bypass listing — they navigate directly to `/classes/1` (mocked) or `/classes/999` (invalid ID error path).

## Root Cause

`class-lifecycle.spec.ts` route-mocks ONLY `/api/v1/classes/1` (detail endpoint). The class-list endpoint `/api/v1/classes` (collection) is NOT mocked, so any test path that opens the listing first fails.

Two-level mismatch (vs GAP-453 plan recon):
1. File-level: only 2/6 tests in `class-lifecycle.spec.ts` are truly self-contained
2. Helper-level: `navigateToClassDetail()` couples 4 tests to the unmocked listing flow

## Proposed Fix Options

### Option C.1 — Refactor `navigateToClassDetail()` to mock listing too

Add `page.route('/api/v1/classes**', ...)` covering both collection + detail. Mock returns 1 class with id=1. Effort: ~30 min in `class-lifecycle.spec.ts`. Tradeoff: test fixtures harder to maintain.

### Option C.2 — Direct-navigation pattern

Skip `navigateToClassDetail()` helper; test goes `await page.goto('/classes/1')` directly. Couples each test less but loses navigation-flow coverage.

### Option C.3 — Pivot to MSW

Use MSW handlers instead of `page.route()`. Per Wave 47 plan §1 Q2 deferred Option C. ~2-4h KC effort. Higher long-term value if more tests need adding to the gate.

### Option C.4 — Drop KC narrow-subset, escalate to docker-compose-in-CI

Per `release-fix-retry-budget.md`: this is retry #2 on KC E2E gate (Wave 47 Phase A = retry #0; Phase B Option B.2 KC subset = retry #1). At retry #2 the rule says STOP patching, redesign gate. For KC specifically, that may mean Option B.1 (full stack in CI).

### Recommendation

**Defer until KC E2E coverage is needed for a specific blocker.** KH gate alone (shipped GAP-453 Phase B) covers Phase 1 BETA Beta Access flow which is the immediate Phase 1 release-deploy critical-path. KC E2E gate adds value when more frontend regressions land per release; not Phase 1 BETA blocking.

When picked up, recommend Option C.1 first (smallest reversible, ~30 min), measure pass count, decide C.3 vs C.4 escalation based on additional spec files needing similar fixes.

## Acceptance Criteria

- [ ] Pick option C.1/C.2/C.3/C.4 with explicit rationale
- [ ] Local verify chromium-only: `pnpm test:e2e:gates` green 100% on chosen subset
- [ ] If C.1/C.2: route-mocks added/refactored, no real-backend dependency
- [ ] If C.3: MSW handlers in place; document worker setup in CI
- [ ] If C.4: docker-compose stack-in-CI step added per GAP-453 Option B.1
- [ ] `frontend-ci.yml` `if: false` flipped → `if: true`
- [ ] CI run on fix-PR shows newly-activated KC E2E job green

## Related

- Parent: GAP-453 (E2E Phase B umbrella) — closing PARTIAL after KH ship
- Wave 47 plan: `documents/03-planning/waves/wave-47-e2e-activation.md`
- KC workflow: `.github/workflows/frontend-ci.yml:90` (still `if: false` post-GAP-453 ship)
- KC test file: `kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts`
- Rule: `.claude/rules/release-fix-retry-budget.md` (retry #2 → pivot, not patch — applied to defer decision)

## Log

- **2026-05-09** Filed during GAP-453 Phase B local verify. KC narrow-subset insufficient (2/6 pass); KH ships separately. Defer per Phase 1 BETA scope decision — KH beta-funnel gate covers immediate critical-path; KC E2E gate revisit when needed.
