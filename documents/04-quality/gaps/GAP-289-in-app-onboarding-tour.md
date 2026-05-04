# GAP-289: In-app feature tour highlighting persona-relevant features

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend) + UX
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** All Tier-1 personas (P1/P2/P3/P5) — first-login experience

## Problem

P1 AC-ONBOARD-004 requires "Onboarding tour highlight ≤5 features quan trọng nhất cho solo teacher (schedule, attendance, gradebook, invoice, communicate)". State-check `kiteclass-frontend/src` and `kitehub-frontend/src` for `OnboardingTour|ProductTour|joyride|driver.js` → **0 matches**.

`OnboardingWizard.tsx` exists in kitehub-frontend but is a 4-step welcome MODAL (account-setup-only), not an in-app feature tour anchored to UI elements (e.g., "click here to schedule a class").

Without persona-aware tour, solo teacher must self-discover features and is exposed to enterprise features (Payroll, Teacher commission, MOET report card) that overwhelm.

## Root Cause

Tour library never integrated. Persona context (P1/P2/P3/P5) not surfaced post-login (depends on GAP-293 feature gating).

## Proposed Fix

1. Pick lightweight tour library — recommend `driver.js` (~5KB, no deps, MIT) or `react-joyride` (more feature-rich, larger).
2. Define per-persona tour scripts:
   - Solo: 5 highlights (Dashboard → "+Class" → "+Student" → Attendance → Invoice → Send)
   - Center: different 5
   - School: different 7
3. Implement `useTour(personaType)` hook reading from auth profile.
4. Trigger on first login (flag in user profile `tourCompleted: false`).
5. Always provide "Skip tour" + "Restart tour from settings" affordances.

## Acceptance Criteria

- [ ] Tour library integrated, bundle impact ≤10KB gzipped
- [ ] Per-persona tour scripts (≤5 steps for solo)
- [ ] First-login trigger; skip-anytime
- [ ] Restart from Settings
- [ ] E2E test: solo first-login → tour appears → 5 steps highlighted → can skip
- [ ] AC-ONBOARD-004 passes

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §1
- AC: AC-ONBOARD-004
- Depends on: GAP-293 (persona context must exist before tour can adapt)
- Sibling: GAP-053 (academic year — solo tour MUST hide it)

## Log

- 2026-05-04 — Created from Wave 17 Bucket A. State-check 0 matches for tour libraries.
