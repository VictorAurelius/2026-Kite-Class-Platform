---
audience: dev
---

# GAP-761 — Zustand persist rehydrate route-guard sentinel (production code Option C)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (production code)
**Detected:** 2026-05-27 (PR #1886 GAP-760 fix empirical investigation per `release-fix-retry-budget.md` §3.5)
**Related Docs:** `kitehub/kitehub-frontend/src/stores/auth-store.ts` zustand v5.0.12 persist middleware
**Related Gaps:** GAP-760 (DONE 40% PARTIAL — Option B addInitScript shipped, improvement +1-2 PASS but không đủ vì root cause deeper); GAP-758 (DONE 100% — layout fix proven correct, this gap = orthogonal layout sweep)

## Current State (verified 2026-05-27)

PR #1886 ship Option B (`addInitScript` seed localStorage BEFORE page load) → KH spec gap-758-school-admin-phase-1-restrict.spec.ts cải thiện từ 13/20 PASS → **15/20 PASS** (Run 1) hoặc 14/20 (Run 2). Residual 5-6/20 fail **non-deterministic** vì root cause sâu hơn Option B có thể fix.

## Root Cause (Option C scope)

Zustand v5.0.12 `persist` middleware async rehydrate qua Promise microtask (per zustand source `persist.ts`). Layout's `useEffect` first-mount fires **BEFORE** zustand finishes reading localStorage:

```typescript
// Sequence ở production build:
// T0: page.goto → SSR render (no localStorage access — server side)
// T1: client hydration begin → zustand store imports → persist.rehydrate() returns Promise (async)
// T2: React useEffect fires post-mount → reads useAuthStore().isAuthenticated
// T3 (microtask later): zustand persist Promise resolves → setState({...}) → isAuthenticated=true
//
// Race: T2 < T3 → useEffect sees stale isAuthenticated=false → router.replace('/login') (existing AUTH guard)
```

Option B (`addInitScript`) ensures localStorage exists at T1 — zustand CAN read it correctly. NHƯNG vẫn không guarantee `T3 < T2` ordering (Promise microtask timing race).

Option C = pattern Zustand officially recommends cho persist-aware components: `useAuthStore.persist.hasHydrated()` sentinel.

## Suspected fix scope

### Phase 1 — Sentinel hook pattern (~1h)

Create reusable hook `useAuthStoreHydrated()`:

```typescript
// kitehub/kitehub-frontend/src/stores/auth-store.ts (extend store config)
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({ ... }),
    {
      name: 'kitehub-auth',
      partialize: (state) => ({ ... }),
      // NEW: onRehydrateStorage callback to set sentinel
      onRehydrateStorage: () => (state) => {
        // Called after rehydrate completes (or fails)
      },
    }
  )
);

// kitehub/kitehub-frontend/src/hooks/use-auth-hydrated.ts (NEW)
export function useAuthHydrated(): boolean {
  const [hydrated, setHydrated] = useState(
    useAuthStore.persist.hasHydrated()
  );
  useEffect(() => {
    const unsubFinish = useAuthStore.persist.onFinishHydration(() =>
      setHydrated(true)
    );
    return () => unsubFinish();
  }, []);
  return hydrated;
}
```

### Phase 2 — Route-guard layout sweep (~2-3h)

Edit production route-guard layouts để wait sentinel:

```typescript
// kitehub/kitehub-frontend/src/app/(school-admin)/layout.tsx (GAP-758 layout — needs update)
export default function SchoolAdminLayout({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();
  const storeHydrated = useAuthHydrated();  // NEW sentinel

  useEffect(() => {
    if (!storeHydrated) return;  // wait zustand finish read localStorage
    if (!isAuthenticated) {
      router.replace('/login');
      return;
    }
    router.replace('/dashboard');  // GAP-758 bounce-all guard
  }, [storeHydrated, isAuthenticated, router]);

  return <div>...LoadingSpinner...</div>;
}
```

Scope spans likely:
- `kitehub/kitehub-frontend/src/app/(school-admin)/layout.tsx` (GAP-758 confirmed affected)
- `kitehub/kitehub-frontend/src/app/(admin)/admin/layout.tsx` (PLATFORM admin)
- `kiteclass/kiteclass-frontend/src/app/(teacher)/layout.tsx` (GAP-758 sibling)
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/parent/layout.tsx` (GAP-758 sibling)
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/student/layout.tsx` (GAP-758 sibling)
- Any other route-guard layout reading `useAuthStore.getState().isAuthenticated` in useEffect

`grep -rl "useAuthStore()" --include="layout.tsx"` cả 2 FE projects để liệt kê đầy đủ.

### Phase 3 — Test verify (~1h)

Re-run KH spec `gap-758-school-admin-phase-1-restrict.spec.ts` workers=4 (parallel default) — kỳ vọng 20/20 PASS without timeout bump (back to 3s).

## Acceptance Criteria

- [ ] Hook `useAuthHydrated()` shipped trong `kitehub/kitehub-frontend/src/hooks/use-auth-hydrated.ts`
- [ ] Sibling hook trong `kiteclass/kiteclass-frontend/src/hooks/use-auth-hydrated.ts` (parallel pattern cho KC store)
- [ ] All route-guard layouts swept (grep evidence: count of files edited + paths)
- [ ] KH spec `gap-758-school-admin-phase-1-restrict.spec.ts` **20/20 PASS** với timeout reverted 8s → 3s
- [ ] No regression on `role-guard.spec.ts` (Wave 80 Bucket C GAP-562b) + other specs using setupMockAuth
- [ ] Sentinel pattern documented trong `kitehub/kitehub-frontend/src/stores/auth-store.ts` JSDoc

## Dependencies + Blockers

- **PR #1886 merge first** — Option B addInitScript shipped, sets foundation cho Option C
- **GAP-758 layout fix** đã ship — sweep affects same layouts, no conflict
- No external dependencies

## Effort estimate

**Phase 1 hook**: ~1h
**Phase 2 layout sweep**: ~2-3h
**Phase 3 verify**: ~1h
**Total**: ~4-5h (1 wave bucket)

## Risk

- **Cross-cut other tests:** route-guard pattern change affects all auth-required layouts; if pattern breaks one existing test, scope creep
- **Zustand v5 API stability:** `useAuthStore.persist.hasHydrated()` + `onFinishHydration()` là zustand v5 official API (per docs); v6 future migration may require update
- **Carry-forward:** until Phase 2 ships, KH E2E specs using setupMockAuth retain flake risk → ADMIN_MERGE_OVERRIDE: GAP-761 cho future FE PR nếu spec block

## Related

- GAP-760 (DONE 40% PARTIAL — Option B foundation)
- GAP-758 (DONE 100% — layout fix correct, this gap = sweep for hydration robustness)
- `kitehub/kitehub-frontend/src/stores/auth-store.ts` line 24-62 persist config
- `kitehub/kitehub-frontend/e2e/utils/test-helpers.ts` setupMockAuth (post-GAP-760 Option B)
- Zustand persist API docs: https://zustand.docs.pmnd.rs/integrations/persisting-store-data#hydrate-storage
- `release-fix-retry-budget.md` §3.5 investigation phase mandate (root cause confirmed via empirical 15/20 PASS post Option B)
- `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion (no new spec needed — existing gap-758 spec is the regression-guard)

## Log

- **2026-05-27 (Filed P1 OPEN):** Gap filed during PR #1886 GAP-760 fix review. Agent B (Option B addInitScript) ship + empirical 2 local runs surface deeper root cause: zustand v5.0.12 persist async rehydrate via Promise microtask races với React useEffect first-mount. Option B fix localStorage-seed timing nhưng KHÔNG fix rehydrate-vs-mount ordering. Option C = sentinel pattern using `useAuthStore.persist.hasHydrated()` + `onFinishHydration()` callbacks (zustand v5 official API). Per `release-fix-retry-budget.md` §3.5 investigation-first mandate satisfied. Per `gap-done-discipline.md` §3 PARTIAL exit ramp — GAP-760 stays PARTIAL 40%, GAP-761 tracks Option C completion. Effort ~4-5h (1 wave bucket).
