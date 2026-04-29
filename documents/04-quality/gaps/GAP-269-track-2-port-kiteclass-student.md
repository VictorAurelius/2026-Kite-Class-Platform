# GAP-269: Track 2 Port — kiteclass-student → production Next.js (mobile PWA, NEW route)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — Tier 2 S. Student persona, mobile-first)
**Domain:** Frontend
**Found:** 2026-04-29
**Affects:** `kiteclass-frontend/src/app/(dashboard)/student/` — NEW route to be created

## Problem

HTML prototype `kiteclass-student/` (avg **116/128** ⭐⭐ HIGHEST kit Round 3, 14 screens, R3 PR #700, mobile-first PWA-grade) is highest-scoring kit across both R2 + R3. Production student dashboard does NOT exist — entire route to be created.

## Current State (verified 2026-04-29)

`kiteclass-frontend/src/app/(dashboard)/student/` does NOT exist. Build from scratch.

## Proposed Fix

Create production student dashboard from prototype — 14 mobile screens.

**Scope:**
- Today (next class card + today's schedule + pending tasks)
- My Classes (list + class detail)
- Class detail (teacher info + schedule + classmates)
- Assignments (filterable: pending/submitted/graded)
- Assignment detail + submit interface (saved-draft pattern)
- Grades (overview + per-subject breakdown + trend)
- Grade detail (per-subject history)
- Attendance log (history + percentage stats)
- Payments balance + history (older students only)
- Notifications inbox (Zalo OA + Web Push mirror)
- Profile + settings
- Login + forgot password (social: Zalo + Google)
- Empty states gallery

**Tech direction:**
- 5-tab bottom nav (Today / Classes / Grades / Notif / Profile) — distinct from kiteclass-parent's 4-tab
- Web Push primary (per dossier S. Student persona spec)
- Saved-draft submit pattern with service-worker background sync
- PWA: manifest + sw.js
- Social login: Zalo + Google via existing auth providers

## Acceptance Criteria

- [ ] All 14 screens ≥110/128 (kit was 116 ⭐⭐)
- [ ] PWA installable (manifest + sw)
- [ ] Web Push permission UI working
- [ ] Saved-draft submit recovers offline submissions when back online
- [ ] Bottom 5-tab nav, 44px+ tap targets
- [ ] Social login Zalo OA + Google
- [ ] Lighthouse PWA score ≥90
- [ ] Vietnamese-only, realistic VN student data
- [ ] WCAG AA preserved
- [ ] E2E: student login → today → submit assignment offline → see synced when online

## Related

- HTML prototype: `ui_kits/kiteclass-student/` (Wave Round 3 PR #700)
- Sister Track 2 gaps: GAP-266..272 + GAP-273 (components)

## Effort estimate

~1-2 weeks. New route greenfield. Wave-pack candidate when sliced into auth+navigation / home+classes / assignments+grades / PWA-infrastructure.

## Log

- **2026-04-29:** Filed after user accepted Round 3 quality. HIGHEST-scoring kit Round 3 (116/128 ⭐⭐).
