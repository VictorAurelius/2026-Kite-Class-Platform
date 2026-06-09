# GAP-1084: E2E gate `Test — KiteClass Frontend` (class-lifecycle route-mocked) RED suốt branch — e2e auth helper stale post-GAP-1074

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-09 (session-start CI verify, branch `fix/v87-attendance-status-normalize-kc5`)
**Resolved:** 2026-06-09 (same session — root cause confirmed + fix + local gate green)
**Affects:** `kiteclass/kiteclass-frontend` E2E gate (`.github/workflows/frontend-ci.yml` job "E2E Tests (Playwright — class-lifecycle gate, route-mocked)") · `pnpm test:e2e:gates` (`e2e/critical-journeys/class-lifecycle.spec.ts`)

## Problem

Khi verify CI cho PR #2274 (session-start 2026-06-09), job **`Test — KiteClass Frontend` = FAILURE** trên HEAD `56a98272`. 6 test class-lifecycle timeout `page.waitForFunction: Test timeout of 30000ms exceeded`, kèm log WebServer `getaddrinfo EAI_AGAIN kite-gateway`.

**Bisect (empirical):** gate đã RED liên tục từ commit `e874d900` (2026-06-08 17:56) — TRƯỚC commit middleware `c1b09c88` (GAP-1077/811) và TRƯỚC landing-100. → KHÔNG phải regression từ middleware hay landing-100. CI history hygiene (~50-run cap) đã xoá run cũ nên không truy được commit GREEN cuối.

## Root cause (CONFIRMED via local repro)

Local `pnpm test:e2e:gates` → **6 failed**, tất cả timeout tại `e2e/helpers/auth.ts:128` `page.waitForFunction(() => sessionStorage.getItem('auth-storage') ...)`.

**Đây là test-helper regression, KHÔNG phải production bug.** GAP-1074 (commit `ce72eadf`, "Option B tenant-scoped localStorage") đổi auth storage:
- Token cũ (GAP-830): `sessionStorage['accessToken'/'auth-storage']`.
- Token mới (GAP-1074): `localStorage['kc:<tenantId>:accessToken' / ':refreshToken' / ':auth-store']`, resolve tenant qua `sessionStorage['kc:currentTenant']` → `localStorage['kc:activeTenant']` (set bởi `setTokens()`/`bindTenant()`).

`login()` helper chạy UI login thật (mock flat shape) → app persist auth ĐÚNG vào location mới + navigate away (`waitForURL` line 121 PASS) → nhưng line 128 vẫn **chờ `sessionStorage['auth-storage']` (key GAP-830 cũ)** → unsatisfiable → 30s test-timeout TRƯỚC khi tới class page. `getAccessToken()` chỉ đọc tenant-scoped key; `LEGACY_LOCAL_KEYS` chỉ swept khi logout (không fallback đọc) → key cũ hoàn toàn vô hình.

Production login + class-detail flow OK (GAP-1074 24 unit test PASS; `src/lib/api/classes.ts` vẫn `response.data.data!` wrapped khớp mock). Gate đỏ chỉ vì e2e auth-injection helper stale.

## Resolution

Cross-flow sweep (per `cross-flow-bug-class-sweep.md`) bug class = "e2e helper dùng key auth GAP-830 cũ" → fix 5 site / 3 file sang scheme GAP-1074 tenant-scoped:

| File | Site | Fix |
|---|---|---|
| `e2e/helpers/auth.ts` | `login()` waitForFunction (130) | chờ `localStorage['kc:<tenant>:auth-store'].isAuthenticated` qua pointer `kc:activeTenant`; timeout 5s→10s |
| `e2e/helpers/auth.ts` | `isAuthenticated()` (181) | đọc `localStorage['kc:<tenant>:accessToken']` |
| `e2e/helpers/auth.ts` | `injectAuthTokens()` (200-221) | seed `kc:currentTenant` + `kc:activeTenant` + `kc:<tenant>:{accessToken,refreshToken,auth-store}` |
| `e2e/wave-49-followups/_helpers.ts` | `loginAs()` (65-84) | same scheme (persona-aware) |
| `e2e/gap-759-flat-auth-shape-contract.spec.ts` | assertion (100-105) | đọc token từ `localStorage['kc:<tenant>:accessToken']` |

**Verification:** local `pnpm test:e2e:gates` → **6 passed (35.7s)** (before fix: 6 failed). Gate self-contained route-mocked (không cần BE) → local green = CI green high confidence. CI confirm trên branch HEAD pending next push.

## Acceptance Criteria

- [x] Root-cause phân định: test-helper stale post-GAP-1074 (KHÔNG phải CI-dev-server-slowness, KHÔNG phải class-detail render regression)
- [x] `Test — KiteClass Frontend` GREEN local (6/6 PASS); CI-on-HEAD confirm pending next push
- [x] N/A render regression — root cause là helper, không phải `/classes/1` render → không cần re-walk KC class-detail flow

## Related

- Discovered in: session-start CI verify 2026-06-09 (PR #2274), per `discovery-to-gap-inline-filing.md`
- Root cause commit: `ce72eadf` GAP-1074 (tenant-scoped localStorage)
- Gate origin: GAP-454 (class-lifecycle route-mocked gate design)
- Cross-flow sweep: 5 sites / 3 files per `cross-flow-bug-class-sweep.md`
- Sibling: GAP-405 (visual regression Playwright), GAP-346 (FE test skip-ratio)
