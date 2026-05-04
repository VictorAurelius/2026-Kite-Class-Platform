# GAP-296: P2 mobile signup + tier picker + class tuition + recurring schedule

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Feature-P1 — bundles 2 onboarding ACs + 1 student wizard mode)
**Domain:** Frontend / Backend
**Found:** 2026-05-04 (P2 Small Center persona review round 1 — see `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md`)
**Persona blocked:** P2 Small Tutoring Center
**Wave:** TBD (post-Wave-17 fix wave)

## Problem

Three onboarding gaps surfaced at P2 review that share owner-onboarding flow surface and should ship together:

1. **AC-ONBOARD-001 (P2 owner):** Signup wizard does not run cleanly on smartphone within the AC's ≤30-min budget. Tier picker (PRO/PREMIUM) — which lives in `kitehub-subscription` — is not wired into the KiteClass tenant signup. Owner today cannot select tier during signup.
2. **AC-ONBOARD-004 (P2 owner):** `Class.java` entity has NO tuition/fee column (grep `tuition|fee|price` in `kiteclass-core/.../module/clazz/entity/Class.java` returns 0). `ClassSession.java` exists per single session but no recurrence rule generator. Owner cannot create "Toán 9A T2-T4-T6 19h-21h, 1M VND/month" in one go — must hand-create every session.
3. **AC-ONBOARD-003 (student-in-P2):** No "under-13 first-login wizard mode" where a parent sets up on the child's device. Generic auth flow doesn't ship preset avatars / parent-on-behalf option.

## Root Cause

Onboarding flows landed in early waves before persona-specific UX requirements were enumerated. KiteHub↔KiteClass tenant signup hand-off and `Class` entity schema were designed for university-shaped courses (single price per enrollment, no recurring weekly cadence) rather than tutoring-center recurring weekly classes.

## Proposed Fix

| Sub-task | Surface | Estimate |
|---|---|---|
| Add tuition/fee column to `Class` entity + migration + DTO | Backend | 0.5d |
| Add recurrence rule (RRULE-lite: weekday mask + start/end time + start/end date) to `Class` + session generator | Backend | 1d |
| Wire kitehub-subscription tier picker into KiteClass tenant signup | Backend + Frontend | 1d |
| Mobile-optimise signup wizard (test viewports 360 / 414 / 480px) | Frontend | 0.5d |
| Add "under-13 setup-on-behalf" mode to first-login wizard with preset avatars | Frontend | 1d |

## Acceptance Criteria

- [ ] `Class` entity has `monthlyTuition` (BigDecimal) + Flyway migration
- [ ] Creating a class with weekly schedule + 3-month duration auto-generates `ClassSession` rows for every weekday-time slot
- [ ] Signup wizard from a smartphone (Chrome 414×896) completes signup → tier select PRO → first-class-create in ≤30 min wall clock with mock data
- [ ] First-login wizard detects DOB < 13 years → shows "Parent setup mode" with no photo upload + preset avatar grid (≥6 options)
- [ ] AC-ONBOARD-001/003/004 (P2 owner) and AC-ONBOARD-003 (student-in-P2) all flip to PASS in next P2 review

## Related

- Audit: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` §1 + §7
- Sibling gaps: GAP-186 (child-protection alignment for under-13 mode)
- Reference AC docs: `documents/00-brd/persona-criteria/P2-small-center.md` §1, `documents/00-brd/persona-criteria/secondary/student-in-P2.md` §1
