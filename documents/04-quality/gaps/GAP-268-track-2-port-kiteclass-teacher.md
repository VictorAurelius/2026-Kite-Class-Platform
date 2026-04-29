# GAP-268: Track 2 Port — kiteclass-teacher → production Next.js

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — Tier 2 KC Teacher persona)
**Domain:** Frontend
**Found:** 2026-04-29
**Affects:** `kiteclass-frontend/src/app/(dashboard)/teacher/` — teacher dashboard routes

## Problem

HTML prototype `kiteclass-teacher/` (avg 108/128, 24 screens, R2 PR #674) covers homeroom (GVCN) + subject teacher workflows. Production teacher route exists but predates Round 2.

## Current State

`kiteclass-frontend/src/app/(dashboard)/teacher/` exists. Components G2 (attendance roster), G3 (gradebook), G4 (schedule), G8 (attendance calendar) inline in HTML prototype — production needs all 4 ported via GAP-273.

## Proposed Fix

Port 24 teacher screens covering daily attendance + grade entry + schedule + reports + settings.

**Scope:**
- Today screen (current period + queue)
- Class roster (per-class student list)
- Daily attendance (G2 component)
- Gradebook (G3 component, VN 10-pt scale)
- Class schedule (G4 component)
- Attendance calendar month-view (G8 component)
- Report card generation (subject teacher input)
- Reports + analytics
- Settings + theme

## Acceptance Criteria

- [ ] All 24 screens ≥105/128
- [ ] G2/G3/G4/G8 components imported from shared lib (post-GAP-273)
- [ ] VN 10-pt grade scale validation working
- [ ] Daily attendance saves to backend (existing endpoints)
- [ ] Recurring schedule rules (G4 conflict detection)
- [ ] Subject teacher MoET-format report card output
- [ ] Vietnamese-only, realistic VN teacher data
- [ ] WCAG AA preserved
- [ ] E2E: teacher login → mark attendance Lớp 6A1 → enter grades → see report

## Related

- HTML prototype: `ui_kits/kiteclass-teacher/`
- Components dependency: GAP-273 (G2/G3/G4/G8 must port first)
- Sister gaps: GAP-266 (owner), GAP-267 (parent), GAP-269 (student)

## Effort estimate

~1-2 weeks (after GAP-273 lands components). Wave-pack candidate.

## Log

- **2026-04-29:** Filed after user accepted Round 3 quality.
