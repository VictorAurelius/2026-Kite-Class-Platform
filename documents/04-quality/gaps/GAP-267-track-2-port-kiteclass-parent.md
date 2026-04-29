# GAP-267: Track 2 Port — kiteclass-parent → production Next.js (mobile PWA)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — Pa. Parent persona, mobile-first 85% sessions)
**Domain:** Frontend
**Found:** 2026-04-29 (post-Round 3 user accept)
**Affects:** `kiteclass-frontend/src/app/(dashboard)/parent/` — parent portal routes

## Problem

HTML prototype `kiteclass-parent/` (avg 114/128 ⭐, 17 screens, R2 PR #670, Direction D pivot to web responsive PWA-grade NOT native) is highest-scoring R2 kit. Production parent route exists but reflects pre-R2 design (~73/128).

## Current State (verified 2026-04-29)

`kiteclass-frontend/src/app/(dashboard)/parent/` exists. Mobile responsiveness + PWA features (manifest, service worker, Web Push, Zalo OA card) NOT verified — likely missing.

## Proposed Fix

Port 17 mobile-first parent screens + add PWA infrastructure.

**Scope:**
- Parent home (children switcher + today + pending)
- Child detail (schedule + grades + attendance + payments)
- Notifications inbox (Zalo OA mirror)
- Payments (balance + history + pay action)
- Empty states (no children invited yet)
- Login + parent invite redemption flow
- PWA: `manifest.json` + service worker + Web Push permission UI
- Zalo OA primary push card (vs Web Push fallback)

**Tech direction:**
- Bottom tab nav (4 tabs: Home, Children, Payments, Profile)
- Mobile-first 320–414px primary, scale to tablet/desktop secondary
- Reuse component G7 (parent-invite redemption flow) from production component lib (port via GAP-273)

## Acceptance Criteria

- [ ] All 17 mobile screens match HTML prototype (≥110/128 per screen — kit was ⭐)
- [ ] PWA: `manifest.json` + service worker registered
- [ ] Web Push permission UI works (subscribe/unsubscribe round-trip)
- [ ] Zalo OA primary card visible above Web Push fallback
- [ ] Bottom tab nav with 44px+ tap targets
- [ ] Lighthouse PWA score ≥90
- [ ] Parent invite redemption end-to-end (token → child binding)
- [ ] Vietnamese-only content, realistic VN data
- [ ] WCAG AA preserved
- [ ] E2E: invite redemption → child detail → pay tuition

## Related

- HTML prototype: `ui_kits/kiteclass-parent/`
- Component G7 (parent invite): GAP-273 batch
- Sister Track 2 gaps: GAP-266 (owner), GAP-268 (teacher), GAP-269 (student)

## Effort estimate

~2 weeks. PWA work + mobile responsiveness adds vs Track 2 baseline. Wave-pack candidate when sliced into auth+invite / home+navigation / detail-screens / PWA-infrastructure.

## Log

- **2026-04-29:** Filed after user accepted Round 3 quality.
