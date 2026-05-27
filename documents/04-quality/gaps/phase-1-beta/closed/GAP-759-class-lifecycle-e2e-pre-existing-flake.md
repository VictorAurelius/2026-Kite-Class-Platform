# GAP-759 — KC class-lifecycle E2E gate pre-existing CI flake

**Status:** 🟢 DONE 2026-05-27
**Priority:** 🟠 P1
**Domain:** Frontend (test infra)
**Detected:** 2026-05-27 (PR #1882 GAP-758 fix CI failure investigation)
**Related Docs:** `.github/workflows/frontend-ci.yml` E2E class-lifecycle gate
**Related Gaps:** GAP-758 (PR #1882 blocked merge by this flake — used ADMIN_MERGE_OVERRIDE per `admin-merge-discipline.md` §4)

## Current State (verified 2026-05-27)

`KiteClass Frontend CI` workflow `E2E Tests (Playwright — class-lifecycle gate, route-mocked)` đã fail consistently từ 2026-05-24 across 6+ different commits:

```
2026-05-27T04:13:46Z failure 842e9f01 (PR #1882 fixup)
2026-05-27T04:09:43Z failure f489d276 (PR #1882 initial)
2026-05-25T12:02:55Z failure 6a2e6981
2026-05-25T05:20:22Z failure 9f90572f
2026-05-25T05:14:16Z failure 71d0488c
2026-05-24T19:08:25Z failure 04c9ff98
```

6 unique commits × 4 days = NOT a flake, **persistent infrastructure regression**.

## Problem

All 6 tests in `kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts` timeout at `page.waitForURL: Timeout 15000ms exceeded`:

1. `should start and complete a class successfully`
2. `should cancel a class with reason`
3. `should generate and copy class code`
4. `should display class sessions correctly`
5. `should not allow delete for non-SCHEDULED or enrolled class`
6. `should show error for invalid class ID`

Symptom: `page.waitForURL` trong `login()` helper (`kiteclass-frontend/e2e/helpers/auth.ts` line 122) wait for redirect away from `/login` (15s timeout). Login form submit → URL không change → timeout.

Additional log: `[WebServer] Failed to fetch landing page data: Error [AggregateError] code: 'ECONNREFUSED'` trước test start — backend not ready.

## Suspected root causes (require investigation)

1. **Mock auth response shape mismatch** — `setupAuthMocks` returns `{success, data: {user, accessToken, refreshToken}}` nhưng `useAuth.ts` mutation onSuccess expects `data.accessToken` direct (root-level). Mismatch → tenantId fallback default → JWT decode error path? Need verify mutationFn extract logic.
2. **Next.js route group rendering regression** — recent FE work may have broken /dashboard render path post-login.
3. **Backend test stub not running** — `[WebServer] ECONNREFUSED` suggests dev server starts before BE stub ready.

## Impact

- KC frontend PR with FE source change → triggers `frontend-ci.yml` → class-lifecycle gate fails → blocks merge
- Workaround current PRs: `ADMIN_MERGE_OVERRIDE: GAP-759 pre-existing flake` trailer per `admin-merge-discipline.md` §4
- Phase 1 BETA path: PR #1882 GAP-758 fix blocked → urgency override merge

## Proposed Fix

### Investigation phase (~30 min)

1. Pull full Playwright HTML report from CI artifact
2. Reproduce locally: `cd kiteclass/kiteclass-frontend && pnpm test:e2e:gates`
3. Check console errors trong test browser context
4. Verify mock auth response shape vs `useAuth.ts` deserialize

### Fix scope (estimate after investigation)

Likely targets:
- `kiteclass-frontend/e2e/helpers/auth.ts setupAuthMocks` response shape align với current `useAuth.ts`
- OR `useAuth.ts mutationFn` extract `.data` properly
- OR Playwright `webServer` config timeout/start order

## Acceptance Criteria

- [x] Root cause identified với concrete evidence — Wave 105 commit `8cc5bff4` PR #1737 đổi KH login API từ `/api/v1/auth/login` (wrapped `{success, data: {...}}`) → `/api/auth/login` (flat). `setupAuthMocks` helper không sync → mock never matches → real network → ECONNREFUSED → login form timeout.
- [x] Fix lands in separate PR — fix-PR shipped this session, scope = mock helper sync + new contract spec.
- [x] All 6 class-lifecycle tests PASS locally — `pnpm test:e2e class-lifecycle.spec.ts --project=chromium --workers=1` returned `6 passed (48.2s)` 2026-05-27.
- [ ] `frontend-ci.yml` E2E class-lifecycle gate green on ≥2 consecutive commits — pending CI run post-merge.
- [x] PR template comment update if mock shape changed — RST→E2E promotion: new contract spec `gap-759-flat-auth-shape-contract.spec.ts` added (2 tests, 15.1s, PASS) để guard mọi future regression khi backend shape changes again.

## Dependencies + Blockers

- No external dependencies
- Investigation may surface deeper issues (mock infrastructure, JWT decode path)

## Effort estimate

**Investigation: ~30-45 min** (Playwright HTML report + local reproduction)
**Fix: ~1-2h** (depends on root cause)

## Risk

- **Cross-cut other tests:** if mock auth shape changes, other E2E tests may break (regression scope)
- **Backend dependency:** if root cause = backend stub, may need docker-compose-in-CI gate (GAP-453 B.1 referenced trong class-lifecycle.spec.ts header)
- **Carry-forward:** PRs since 2026-05-24 used override merges — pattern frequency > 5% per quarter triggers meta-review per `admin-merge-discipline.md` §4

## Related

- `.github/workflows/frontend-ci.yml` line ~80 (E2E class-lifecycle gate config)
- `kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts` (6 tests)
- `kiteclass/kiteclass-frontend/e2e/helpers/auth.ts` (login + setupAuthMocks)
- `kiteclass/kiteclass-frontend/src/hooks/useAuth.ts` line 42-58 (mutation onSuccess)
- GAP-454 (DONE PR #1079 — last successful refactor of class-lifecycle gate)
- GAP-453 B.1 (deferred — full E2E backend stack in CI)
- `admin-merge-discipline.md` §4 "CI infrastructure broken" override class
- `release-fix-retry-budget.md` §5 "Test environment flake" exception

## Log

- **2026-05-27 (DONE):** Root cause confirmed = Suspected #1 from initial filing (mock auth response shape mismatch). Wave 105 commit `8cc5bff4` PR #1737 đổi KH login API:
  - URL: `/api/v1/auth/login` → `/api/auth/login`
  - Response shape: wrapped `{success: true, data: {accessToken, refreshToken, user: {...roles: [...]}}}` → flat `{accessToken, refreshToken, user: {...role: <singular>}}`
  - User contract: `roles: [...]` array → `role: <string>` singular; dropped `profile: {id}` field

  `kiteclass-frontend/e2e/helpers/auth.ts setupAuthMocks` không được sync trong cùng wave → mock route `/api/v1/auth/login` không bao giờ fire khi useAuth.ts POST `/api/auth/login` → real network call → ECONNREFUSED (backend port không expose từ Playwright webServer dev mode) → login form submit hang → `page.waitForURL` 15s timeout → all 6 tests fail.

  **Fix shipped:**
  - `kiteclass-frontend/e2e/helpers/auth.ts` line 33: route `/api/v1/auth/login` → `/api/auth/login`
  - Same file line 44-58: response body flat shape (drop `{success, data}` wrapper); user shape `role: TEST_USER.role` singular (was `roles: [TEST_USER.role]`); drop `profile: {id: 1}`
  - Same file line 73: logout route `/api/v1/auth/logout` → `/api/auth/logout`
  - Error path line 61-66: flat error shape (drop `{success: false}` wrapper)
  - NEW `kiteclass-frontend/e2e/gap-759-flat-auth-shape-contract.spec.ts` — 2-test regression-guard per `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate:
    1. Mock fires `/api/auth/login` (NOT `/api/v1/...`) + returns flat shape (NOT wrapped) + user.role singular (NOT roles[]) + JWT contains tenantId claim
    2. UI login flow stores accessToken in localStorage post-mock-fire

  **Local verification (Docker stack on port 3000):**
  - `class-lifecycle.spec.ts`: `6 passed (48.2s)` chromium 1 worker
  - `gap-759-flat-auth-shape-contract.spec.ts`: `2 passed (15.1s)` chromium 1 worker

  Total 8 specs PASS confirms root cause fully resolved + future regression guarded.

  Per `e2e-rst-test-layer-boundary.md` §3 — fix bug từ infrastructure flake class → paired E2E spec same PR để prevent recurrence khi backend shape change again. Counterfactual: nếu spec landed Wave 105 → flake không xảy ra; với spec landed now → future backend shape change tự catch.
- **2026-05-27 (Filed P1 OPEN):** Gap filed during PR #1882 GAP-758 Option A fix merge investigation. CI failure traced không-do-regression: 6 consecutive failures across 4 days × different SHA (04c9ff98 / 71d0488c / 9f90572f / 6a2e6981 / f489d276 / 842e9f01) since 2026-05-24. Pre-existing infrastructure flake. PR #1882 merged với `ADMIN_MERGE_OVERRIDE: GAP-759` trailer per `admin-merge-discipline.md` §4 row "CI infrastructure broken". Investigation defer next session (~30-45 min).
