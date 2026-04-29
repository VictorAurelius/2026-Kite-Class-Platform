# GAP-277: Track 2 Coverage — Error pages kit + best-practice fixes

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (UX hardening — degraded-UX moments + missing best-practice files)
**Domain:** Frontend / Design System / Hardening
**Found:** 2026-04-29 via audit §2.4 + `dossier/15-error-layout-inventory.md`
**Affects:** Both apps — error pages + global error boundaries + loading states

## Problem

6 error/404/loading files exist with ❌ NO kit coverage. Plus 5 tech-debt items (missing root error boundary in KC, inconsistent loading states, etc.).

## Current State

| File | App | Status |
|------|:---:|:------:|
| `(public)/error.tsx` | KC | exists, plain |
| `(public)/not-found.tsx` | KC | exists |
| `(public)/loading.tsx` | KC | exists |
| `error.tsx` | KH | exists |
| `global-error.tsx` | KH | exists |
| `not-found.tsx` | KH | exists |

**Tech-debt (P1+P2):**
- KC missing `global-error.tsx` (root error boundary) — runtime crashes uncaught at root
- KC missing root `not-found.tsx` (only public scope covers 404)
- KC missing `(dashboard)/error.tsx` + `(auth)/error.tsx` route-segment boundaries
- KH missing `(admin)/error.tsx`
- Loading states inconsistent — only `(public)/loading.tsx` in KC, none in KH

## Proposed Fix

**Phase 1 (kit):** `ui_kits/error-pages/` HTML kit with branded error UX:
- 404 page (humorous + helpful — sample VN copy "Trang này đi lạc rồi")
- 500 / runtime crash (apologetic + retry CTA + status-page link)
- Maintenance mode (scheduled-downtime banner + ETA)
- Offline detection (PWA-aware: detected-offline vs intermittent)
- Loading skeletons (per route group: dashboard / auth / public / admin)
- Empty states gallery (extends parent kit empty states)

**Phase 2 (port + best-practice fixes):**
- Apply branded error pages to existing error.tsx files
- Add missing files: KC `global-error.tsx`, root `not-found.tsx`, segment-level `error.tsx`
- Add loading states for all route groups in KH

## Acceptance Criteria

- [ ] HTML kit ≥105/128
- [ ] All 5 missing best-practice files added (KC global-error / KC not-found root / KC dashboard error / KC auth error / KH admin error)
- [ ] Loading states added for KH route groups
- [ ] Branded error UX consistent KC + KH (different brands)
- [ ] Status-page integration (link to public status page if exists)
- [ ] WCAG AA preserved
- [ ] Vietnamese-only error messages, helpful tone

## Related

- Audit evidence: §2.4
- Tech-debt source: `dossier/15-error-layout-inventory.md` §"Best-practice gaps"

## Effort estimate

~1 wave (kit + port + best-practice fixes combined — small scope per file).

## Log

- **2026-04-29:** Filed from audit synthesis. P1 priority because graceful degradation affects all users in incidents.
