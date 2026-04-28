# GAP-248: KC `(auth)/layout.tsx` chunk hoist — providers re-bundle into every auth route

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (perf — bucket already within 250KB budget; refactor for headroom)
**Domain:** Frontend (KiteClass) / Performance
**Detected:** 2026-04-28 (Wave GAP-236 Agent A return finding)
**Affects:** `kiteclass-frontend` `(auth)/*` routes (login, register, register/student, forgot-password, reset-password, parent-invite)

## Problem

Agent A (PR #601) measured `(auth)/layout.tsx` contributing **~131 KB** to the common chunk shared across every auth route. Wave GAP-236 Agent A code-split heavy form bodies (top 3 auth routes saved 119 KB combined), but the shared providers/contexts hoisted into `layout.tsx` still ship the same 131 KB on every auth page first paint.

Layout source itself is only 24 lines (state-check 2026-04-28: `wc -l kiteclass-frontend/src/app/\(auth\)/layout.tsx` = 24); the bulk is provider transitive imports (likely toast/theme/i18n/query-client wrappers pulling Radix sub-trees + framer/lucide barrels).

## Root Cause

Next.js layouts are server components by default; their child providers (`<ThemeProvider>`, `<Toaster>`, `<QueryClientProvider>`, etc.) usually run in `'use client'` components imported synchronously. Each auth page route inherits the full provider chunk.

Concretely:
- Providers shared across the WHOLE app belong in `app/layout.tsx` (root) — they load once and stay
- Auth-specific providers should be in `(auth)/layout.tsx` — but if they pull heavy deps, the chunk is paid on every `(auth)/*` first paint
- Lazy-loading inside a layout is awkward — clients hit the layout before they hit the page, so a `next/dynamic` wrapper here would still block first paint

## Proposed Fix

Investigate-then-decide refactor (not pure code change):

1. **Audit `(auth)/layout.tsx` provider tree** — `pnpm analyze` then trace the 131 KB chunk back to specific imports
2. **Categorize**:
   - Providers that *also* exist in root `app/layout.tsx` → remove duplicate from auth layout
   - Providers genuinely auth-only (e.g. anonymous-session context) → keep, but check if any heavy deps are dev-time-only or can be feature-flagged
   - UI primitives accidentally re-imported (e.g. `Toaster` if also in root) → consolidate
3. **Quantify** — re-run bundle-budget after each change; report delta
4. **Document** — if some providers genuinely must duplicate, note in `(auth)/layout.tsx` why

## Acceptance Criteria

- [ ] Provider chunk audit committed (analyzer output diff or written summary)
- [ ] Duplicate providers consolidated to root layout where applicable
- [ ] `(auth)/*` routes' First Load JS drops measurably (target ≥10 KB reduction)
- [ ] Bundle budget green; auth flow tests green
- [ ] If no measurable win possible, gap closed as **EXPLAINED** with documentation in layout.tsx

## Related

- Parent wave: `documents/03-planning/waves/wave-gap-236-fe-code-split.md`
- PR #601 (Agent A finding)
- Sub-PR C analyzer baseline: `documents/04-quality/audits/performance/bundle-analyzer-baseline-kc.html` — useful for chunk attribution

## Log

- **2026-04-28** — Filed during Wave GAP-236 consolidation. Agent A flagged 131 KB common-chunk hoist; deferred from Wave because investigation needs analyzer trace, not just page-level code-split.
