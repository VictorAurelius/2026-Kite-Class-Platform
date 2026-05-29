---
audience: dev
---

# GAP-760 — KH E2E setupMockAuth Zustand persist hydration race

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Frontend (test infra + production layout)
**Detected:** 2026-05-27 (GAP-758 KH spec local smoke run — 7/20 fail systematic)
**Updated:** 2026-05-27 (Option B `addInitScript` shipped — improvement 13→15/20 PASS; residual 5/20 fail traced tới zustand v5 async persist race, layout-side fix required — see §"Updated finding 2026-05-27" below)
**Related Docs:** `kitehub/kitehub-frontend/e2e/utils/test-helpers.ts` setupMockAuth, `kitehub/kitehub-frontend/src/stores/auth-store.ts`, `kitehub/kitehub-frontend/src/app/(school-admin)/layout.tsx`
**Related Gaps:** GAP-758 (DONE 2026-05-27 — layout fix proven correct, this gap = test infra hardening), GAP-759 (KC class-lifecycle E2E mock auth shape mismatch — sibling test infra concern at different test layer)
**Completion:** ~40% (Option B test-infra shipped; production-layout `useAuthStore.persist.hasHydrated()` wait gate remaining — Option C scope)

## Current State (verified 2026-05-27)

GAP-758 local smoke session 2026-05-27:
- KC spec `gap-758-persona-route-restrict.spec.ts` (5 tests Owner JWT real login) → 5/5 PASS workers=1 serialized
- KH spec `gap-758-school-admin-phase-1-restrict.spec.ts` (4 roles × 5 paths = 20 tests, mock auth via setupMockAuth) → 13/20 PASS với timeout 8s, workers=1

Failure pattern systematic — 7/20 fail bounce tới `/login` thay vì `/dashboard`:
```
Expected pattern: /\/dashboard$/
Received string:  "http://localhost:3001/login"
Call log:
  - 2 × unexpected value "http://localhost:3001/school-admin/teacher-management"
  - 10 × unexpected value "http://localhost:3001/login"
```

→ Layout's `useEffect` sees `isAuthenticated === false` at check time → fires `router.replace('/login')` (existing AUTH guard) thay vì GAP-758 bounce-all guard.

## Problem

`setupMockAuth` seeds `localStorage.setItem('kitehub-auth', JSON.stringify({state, version}))` SYNCHRONOUSLY, sau đó test `page.goto(path)` navigate đến target. Khi page render:

1. SSR initial state: zustand store returns initial `isAuthenticated: false` (Next.js production build hydrates client-side)
2. Layout `useAuthStore()` reads CURRENT state → `isAuthenticated === false`
3. Zustand `persist` middleware async rehydrates from `localStorage` (race against component mount)
4. Layout `useEffect(() => setIsHydrated(true), [])` fires ONCE on mount
5. Second `useEffect([isHydrated, isAuthenticated, router])` fires khi `isHydrated` flip → reads `isAuthenticated` → if zustand chưa rehydrated → `false` → `router.replace('/login')`

Race depends on:
- Page cold-start hydration time (lazy bundle load 5-7s on first hit per route group)
- Zustand `persist` middleware sync vs async hydrate behavior (default async cho zustand v4)
- React reconciler scheduling order

## Root Cause

`setupMockAuth` không **wait** cho zustand to rehydrate before returning. Test pattern:

```typescript
await clearBrowserStorage(page);    // sets localStorage
await setupMockAuth(page, role);    // sets localStorage 'kitehub-auth'
// race: page.goto fires before zustand persist middleware can rehydrate
await page.goto(path);
```

Layout's `useAuthStore()` may return stale `isAuthenticated: false` before zustand reads localStorage on first render.

## Suspected fix scope (require investigation)

### Option A — `onHydrate` callback in setupMockAuth (~30 min)

Add wait via `page.waitForFunction` checking `window.__zustand_hydrated` flag (requires store onRehydrateStorage callback to set flag):

```typescript
export async function setupMockAuth(...) {
  // ... existing localStorage seed
  await page.evaluate((state) => {
    localStorage.setItem('kitehub-auth', JSON.stringify(state));
  }, authState);
  // NEW: wait until zustand rehydrates
  await page.waitForFunction(() => {
    const stored = localStorage.getItem('kitehub-auth');
    return stored && JSON.parse(stored).state.isAuthenticated === true;
  }, { timeout: 5000 });
}
```

### Option B — Use `addInitScript` to seed BEFORE page load (~20 min)

```typescript
await page.addInitScript((state) => {
  localStorage.setItem('kitehub-auth', JSON.stringify(state));
}, authState);
await page.goto(path);  // localStorage already populated before any JS runs
```

This bypasses race entirely — localStorage exists before Next.js even loads, so zustand reads it synchronously on first import.

### Option C — Zustand store onRehydrateStorage + test sentinel (~1h)

Add `window.__zustandReady = true` callback trong auth-store.ts; test waits cho sentinel.

## Acceptance Criteria

- [x] Root cause confirmed (zustand v5 persist middleware async rehydrate — layout `useEffect` mount fires BEFORE store finishes reading localStorage; see §"Updated finding 2026-05-27")
- [x] Fix shipped trong separate PR (Option B `addInitScript` — least invasive test-infra change)
- [ ] KH spec `gap-758-school-admin-phase-1-restrict.spec.ts` 20/20 PASS với workers=1 (or 4) — **partial: 15/20 PASS post-fix (improvement +2 from 13/20 baseline); residual 5/20 fail require Option C layout-side `useAuthStore.persist.hasHydrated()` wait gate**
- [ ] No regression on other tests using setupMockAuth (`role-guard.spec.ts` Wave 80 Bucket C) — **pending verification; addInitScript pattern is strictly more correct than evaluate-after-navigate, expected no regression**
- [x] Update `test-helpers.ts` setupMockAuth docstring describing hydration handling

## Updated finding 2026-05-27 (post Option B ship)

Empirical verification with `addInitScript` seed BEFORE page load:
- 2 consecutive runs against KH stack `localhost:3001`: 14/20 + 15/20 PASS (improvement +1-2 from 13/20 baseline pre-fix)
- Residual ~25% fail rate non-deterministic — different paths/roles fail each run

Deeper root cause:
- `zustand` v5.0.12 uses async `persist` middleware by default
- Even with `localStorage` seeded BEFORE page JS via `addInitScript`, store hydration is async (Promise microtask)
- Layout's `useEffect(() => setIsHydrated(true), [])` fires on first mount → state still `isAuthenticated: false` (initial) before zustand finishes async rehydrate
- Second `useEffect([isHydrated, isAuthenticated, router])` reads stale `false` → `router.replace('/login')` fires incorrectly

Option B (test-infra `addInitScript`) eliminates the localStorage-seed-after-navigate race but cannot fix the layout-side `useEffect` fires-before-zustand-hydrate race — that requires production code change.

### Recommended next step (Option C scope)

`kitehub/kitehub-frontend/src/app/(school-admin)/layout.tsx` (and any other layout using `useAuthStore`) MUST wait for `useAuthStore.persist.hasHydrated()` before reading state:

```typescript
const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
const [storeHydrated, setStoreHydrated] = useState(useAuthStore.persist.hasHydrated());

useEffect(() => {
  const unsub = useAuthStore.persist.onFinishHydration(() => setStoreHydrated(true));
  return unsub;
}, []);

useEffect(() => {
  if (!storeHydrated) return;  // wait until zustand confirms localStorage read
  if (!isAuthenticated) { router.replace('/login'); return; }
  router.replace('/dashboard');
}, [storeHydrated, isAuthenticated, router]);
```

This applies to **all** route-guard layouts using `useAuthStore` — likely also affects KH `(admin)/admin/layout.tsx` and other guarded routes silently. Scope expansion makes Option C a Phase 1 BETA test-infra hardening sub-bucket, not gap-local fix.

File follow-up GAP-XXX for Option C if user accepts splitting. Otherwise this gap stays PARTIAL with documented finding + path forward.

## Dependencies + Blockers

- No external dependencies
- Sibling concern GAP-759 (KC class-lifecycle): different layer (real backend mock route instead of localStorage), but same class "test mock infra timing"

## Effort estimate

**Investigation: ~30 min** (verify hypothesis via DevTools + console.log inside test)
**Fix (Option B preferred): ~30 min** (small test-helpers.ts edit + spec run validation)
**Cross-check existing tests: ~30 min** (rerun all KH E2E specs using setupMockAuth)

Total ~1.5h.

## Risk

- **Other tests affected:** if pattern changes affect `role-guard.spec.ts` or other Wave 80+ specs using setupMockAuth, scope creep
- **False stability:** fix may pass locally but fail CI (different machine speed)
- **Carry-forward:** ADMIN_MERGE_OVERRIDE: GAP-760 trailer if PR blocked by KH E2E spec flake before fix lands

## Related

- `kitehub/kitehub-frontend/e2e/utils/test-helpers.ts` line 76-112 setupMockAuth
- `kitehub/kitehub-frontend/src/stores/auth-store.ts` line 24-62 useAuthStore với persist
- `kitehub/kitehub-frontend/src/app/(school-admin)/layout.tsx` (GAP-758 layout)
- GAP-758 (DONE 2026-05-27 — layout fix verified, this gap = orthogonal test infra concern)
- GAP-759 (KC class-lifecycle mock auth shape mismatch — sibling test infra concern, different mock pattern)
- `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate (spec must be stable for regression-guard role)

## Log

- **2026-05-27 (Updated → PARTIAL):** Option B `addInitScript` shipped trong `test-helpers.ts` setupMockAuth — `page.evaluate()` post-navigation replaced với `page.addInitScript()` pre-navigation seed. Local empirical verification on KH stack `localhost:3001`: 14-15/20 PASS (improvement +1-2 từ 13/20 baseline; non-deterministic 5/20 residual fail). Empirical root cause investigation per `release-fix-retry-budget.md` §3.5 mandate revealed zustand v5.0.12 persist middleware uses async rehydration (Promise microtask) — even với localStorage seeded BEFORE page JS, layout's `useEffect` first-mount fires BEFORE zustand finishes reading localStorage → state stale `isAuthenticated: false` → existing AUTH guard fires `router.replace('/login')` incorrectly. Test-infra Option B alone insufficient cho 100% pass; production-code Option C (`useAuthStore.persist.hasHydrated()` + `onFinishHydration()` wait gate trong layout) required cho full close. Option C scope likely affects MULTIPLE route-guard layouts (school-admin + admin + possibly others) — file follow-up gap nếu approved. Gap stays PARTIAL with documented finding + path forward + improvement landed prospectively cho any test infra using setupMockAuth.

- **2026-05-27 (Filed P1 OPEN):** Gap filed during GAP-758 local smoke verification. KH spec broader 20-test run 13/20 PASS với timeout 8s, workers=1. 7 fail systematic: bounce `/login` thay vì `/dashboard` → layout sees `isAuthenticated === false` → existing AUTH redirect (not GAP-758 layout bug). Spec timeout bumped 3s→8s same session in `gap-758-school-admin-phase-1-restrict.spec.ts` để mitigate cold-page hydration latency, vẫn không đủ. Recommend Option B (`addInitScript` seed localStorage BEFORE page load) — least invasive, bypasses race entirely. Investigation defer next session (~30 min) + fix (~30 min) per `release-fix-retry-budget.md` §3.5 investigation-first mandate.
