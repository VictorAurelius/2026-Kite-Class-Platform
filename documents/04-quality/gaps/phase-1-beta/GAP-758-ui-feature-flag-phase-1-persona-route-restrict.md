# GAP-758 — UI feature-flag Phase 1 BETA persona-mismatched routes (flow-bug class)

**Status:** 🟡 PARTIAL 75% — Option A fix shipped (4 layout edits + 2 E2E specs); local smoke + CSV addendum pending stack restart
**Priority:** 🔴 P0
**Domain:** Frontend
**Detected:** 2026-05-27 (UI exposure audit per session-handoff 2026-05-27)
**Related Docs:** `documents/03-planning/session-handoffs/2026-05-27-rst-scope-discussion.md`
**Related Gaps:** GAP-725 (KC Parent/Teacher auth path architectural — Phase 2), GAP-756 (Wave beta-prep-1 production deploy + RST verify)

## Current State (verified 2026-05-27)

UI exposure audit (session 2026-05-27) phát hiện 4 nhóm route persona-mismatched accessible cho Phase 1 BETA Owner JWT:

| Route | Layout guard | Page guard | Owner JWT access | Flow state |
|---|---|---|:---:|---|
| KH `/school-admin/*` (5 routes) | AUTH only (`isAuthenticated` redirect /login) | None | ✅ Accessible | Renders Phase 3 K-12 SCHOOL_ADMIN mock UI |
| KC `(dashboard)/parent/*` (5 routes) | None (inherits dashboard auth only) | `userType !== PARENT → /dashboard` | ✅ Accessible (bypass via undefined userType) | Renders parent portal broken |
| KC `(dashboard)/student/*` | Layout `userType !== STUDENT → /dashboard` | None | ✅ Accessible (bypass via undefined userType) | Renders student PWA broken |
| KC `(teacher)/*` | AUTH only (no role check) | None | ✅ Accessible | Renders teacher dashboard với `TEACHER_PROFILE` mock data |

## Problem

User distinction từ session 2026-05-27: **beta accept edge-case bugs ≠ accept flow bugs**. Phase 1 BETA tester (Owner persona) typing URL `/teacher` `/parent` `/student` `/school-admin/bulk-import` etc. → page renders broken state thay vì redirect/403 — **flow gãy class**.

### Root cause

KC FE `User` interface declares `userType: UserType` (TEACHER / PARENT / STUDENT / ADMIN / STAFF). KH `POST /api/auth/login` response shape (verified session 2026-05-27 smoke):

```json
{
  "user": {
    "id": "00000000-0000-0000-0000-000000000099",
    "email": "admin@kitehub.com",
    "role": "PLATFORM_ADMIN"
  },
  "accessToken": "...",
  "refreshToken": "..."
}
```

Response không có `userType` field → KC FE stores `user` với `userType = undefined`. Guard logic in page:

```typescript
if (userType && userType !== UserType.PARENT) {
  router.replace('/dashboard');
}
```

Falsy `userType` (undefined) → guard bypassed → page renders.

**Architectural sibling của GAP-725** — GAP-725 documents Parent/Teacher routes "redirect /login" assumption (no JWT issued for Parent/Teacher). Thực tế Owner JWT bypasses persona guards via undefined userType, KHÔNG redirect.

## Impact

Phase 1 BETA Owner persona có thể:

1. Typing URL `/teacher` → see fake teacher dashboard với mock GVCN profile
2. Typing URL `/parent` → see broken parent portal (data fetch fail)
3. Typing URL `/student` → see broken student PWA
4. Typing URL `/school-admin/bulk-import` (etc.) → see Phase 3 K-12 mock UI

Beta tester confusion + lost trust + support burden. Violates user flow-bug-class strict standard.

## Proposed Fix (3 options)

### Option A — Add explicit `userType === OWNER` reject guards (~2-3h)

Edit 4 route layouts/pages để explicit reject Owner persona:

```typescript
// KC (teacher)/layout.tsx — add persona guard
useEffect(() => {
  if (isHydrated && isAuthenticated) {
    // Per GAP-758: Owner JWT may bypass undefined userType guards
    // Explicit Owner-reject: only TEACHER allowed
    const userRole = useAuthStore.getState().user?.role;
    if (userRole === 'OWNER' || userRole === 'PLATFORM_ADMIN' || userRole === 'STAFF') {
      router.replace('/dashboard'); // bounce KH personas back
    }
  }
}, [isHydrated, isAuthenticated, router]);
```

Same pattern cho `(dashboard)/parent/*` + `(dashboard)/student/*` + KH `(school-admin)/layout.tsx`.

### Option B — Feature flag hide routes Phase 1 (~3-4h)

Add env var `NEXT_PUBLIC_PHASE_1_PERSONA_RESTRICT=true` checked in 4 layouts:

```typescript
if (process.env.NEXT_PUBLIC_PHASE_1_PERSONA_RESTRICT === 'true' && pathname.startsWith('/teacher')) {
  router.replace('/dashboard');
}
```

Flip flag = false sau Phase 2 GAP-725 architectural fix.

### Option C — Server-side middleware role enforcement (~5-8h)

Add Next.js `middleware.ts` cho both FE projects, enforcing role check server-side. Per `agent-aws-access.md` Tier 3 — proper architectural fix nhưng cao effort.

Per `gap-done-discipline.md` §3 — Phase 1 BETA blocker tier = Option A preferred (lowest effort + closes flow-bug-class).

## Acceptance Criteria

- [x] KC `(teacher)/layout.tsx` adds explicit `userType === TEACHER` REQUIRE guard (commit 9c7270b3+)
- [x] KC `(dashboard)/parent/page.tsx` extends existing guard với explicit REQUIRE; new `(dashboard)/parent/layout.tsx` covers entire `/parent/*` tree
- [x] KC `(dashboard)/student/layout.tsx` extends existing guard với explicit STUDENT REQUIRE (+ short-circuit render)
- [x] KH `(school-admin)/layout.tsx` Phase 1 BETA feature-flag hide entire scope — bounce all authenticated personas to `/dashboard`
- [ ] Smoke test: Owner JWT login → browser walk `/teacher` `/parent/billing` `/student/today` `/school-admin/bulk-import` → all redirect `/dashboard` — PENDING (need stack restart + Playwright run)
- [x] E2E spec added per `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate: `kiteclass-frontend/e2e/gap-758-persona-route-restrict.spec.ts` (5 tests Owner persona) + `kitehub-frontend/e2e/gap-758-school-admin-phase-1-restrict.spec.ts` (4 roles × 5 paths = 20 tests)
- [ ] CSV row added to `phase-1-beta-acceptance-self-test.csv` cho persona-route-restrict smoke check — DEFERRED (CSV scope per Wave 106 plan canonical; persona-route-restrict E2E coverage canonical)
- [x] PR body documents GAP-758 closure + E2E spec promotion (PR #1882)

## Dependencies + Blockers

- **GAP-725** Phase 2 architectural fix (long-term proper solution — KC auth path issuing JWT với userType field)
- This GAP-758 = short-term defensive guard cho Phase 1 BETA

## Effort estimate

**Option A: ~2-3h** (4 layout edits + smoke + E2E spec)
**Option B: ~3-4h** (Option A + feature flag mechanism)
**Option C: ~5-8h** (full middleware refactor)

Recommend **Option A** cho Phase 1 BETA timeline + lowest risk.

## Risk

- **Layout cascade:** Owner accessing `/teacher` đã render `TeacherShell` component before guard fires (client-side useEffect post-render). Loading flicker acceptable; ensure redirect within 100ms.
- **Mock data leak:** `TEACHER_PROFILE` mock data may briefly render before redirect. Loading state suppression mandatory.
- **E2E spec maintenance:** if persona auth architecture changes in Phase 2 (GAP-725 fix), E2E specs need update.

## Related

- `documents/03-planning/session-handoffs/2026-05-27-rst-scope-discussion.md` §UI exposure audit
- GAP-725 — KC Parent/Teacher auth path architectural (Phase 2 long-term fix)
- GAP-756 — Wave beta-prep-1 production deploy (this gap blocks Phase 1 BETA beta cohort launch)
- `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate (E2E spec required pair with fix)
- `pre-handoff-self-test-completeness.md` §2.4 admin-flow + §2.7 multi-tenant tenant-switch (similar persona-flow verification)

## Log

- **2026-05-27 (PARTIAL 75% — Option A fix shipped):** Per user direction "làm Option A fix luôn". Edited 4 layouts với explicit `userType === X` REQUIRE guards (not `!== X` bypass) — eliminate undefined userType bypass class:
  - `kiteclass-frontend/src/app/(teacher)/layout.tsx` — TEACHER REQUIRE + LoadingSpinner suppress until verified
  - `kiteclass-frontend/src/app/(dashboard)/parent/page.tsx` — extended page guard + early `return null` short-circuit
  - **NEW** `kiteclass-frontend/src/app/(dashboard)/parent/layout.tsx` — covers entire `/parent/*` tree (sibling pages billing/attendance/grades/settings)
  - `kiteclass-frontend/src/app/(dashboard)/student/layout.tsx` — STUDENT REQUIRE + early return
  - `kitehub-frontend/src/app/(school-admin)/layout.tsx` — Phase 1 BETA hide entire scope, bounce all personas to /dashboard; removed `SchoolAdminShell` render
  - E2E specs paired same PR per `e2e-rst-test-layer-boundary.md` §3:
    - `kiteclass-frontend/e2e/gap-758-persona-route-restrict.spec.ts` (5 tests Owner JWT × {teacher, parent, parent/billing, student, dashboard-positive-control})
    - `kitehub-frontend/e2e/gap-758-school-admin-phase-1-restrict.spec.ts` (4 roles × 5 paths = 20 tests)
  - PENDING: local Playwright run smoke (stack already running per GAP-756 Phase 1 — Owner seed `owner.test@test.vn / Test@1234` from Wave 105/107)
  - DEFERRED: CSV row addendum cho `phase-1-beta-acceptance-self-test.csv` — E2E spec coverage canonical (per `e2e-rst-test-layer-boundary.md` §2.2 functional regression owns table)
- **2026-05-27 (Filed P0 OPEN):** Gap filed in response to UI exposure audit session 2026-05-27 per session-handoff `2026-05-27-rst-scope-discussion.md`. User flag: "beta accept bug nhưng flow phải luôn thông cho mọi nghiệp vụ KH+KC". Audit phát hiện 4 nhóm route persona-mismatched accessible cho Owner JWT vì KC FE `userType` undefined when KH login response không có field (architectural mismatch sibling GAP-725). Phase 1 BETA beta cohort blocker — fix required trước launch để eliminate flow-bug class. Recommend Option A explicit reject guards (~2-3h). RST→E2E promotion: E2E spec mandatory paired same PR per `e2e-rst-test-layer-boundary.md` §3.
