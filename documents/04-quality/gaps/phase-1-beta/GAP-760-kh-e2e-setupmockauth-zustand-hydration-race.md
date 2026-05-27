---
audience: dev
---

# GAP-760 — KH E2E setupMockAuth Zustand persist hydration race

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (test infra)
**Detected:** 2026-05-27 (GAP-758 KH spec local smoke run — 7/20 fail systematic)
**Related Docs:** `kitehub/kitehub-frontend/e2e/utils/test-helpers.ts` setupMockAuth
**Related Gaps:** GAP-758 (DONE 2026-05-27 — layout fix proven correct, this gap = test infra hardening), GAP-759 (KC class-lifecycle E2E mock auth shape mismatch — sibling test infra concern at different test layer)

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

- [ ] Root cause confirmed (zustand persist sync vs async hydrate behavior)
- [ ] Fix shipped trong separate PR (Option B preferred — least invasive)
- [ ] KH spec `gap-758-school-admin-phase-1-restrict.spec.ts` 20/20 PASS với workers=1 (or 4)
- [ ] No regression on other tests using setupMockAuth (`role-guard.spec.ts` Wave 80 Bucket C)
- [ ] Update `test-helpers.ts` setupMockAuth docstring describing hydration handling

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

- **2026-05-27 (Filed P1 OPEN):** Gap filed during GAP-758 local smoke verification. KH spec broader 20-test run 13/20 PASS với timeout 8s, workers=1. 7 fail systematic: bounce `/login` thay vì `/dashboard` → layout sees `isAuthenticated === false` → existing AUTH redirect (not GAP-758 layout bug). Spec timeout bumped 3s→8s same session in `gap-758-school-admin-phase-1-restrict.spec.ts` để mitigate cold-page hydration latency, vẫn không đủ. Recommend Option B (`addInitScript` seed localStorage BEFORE page load) — least invasive, bypasses race entirely. Investigation defer next session (~30 min) + fix (~30 min) per `release-fix-retry-budget.md` §3.5 investigation-first mandate.
