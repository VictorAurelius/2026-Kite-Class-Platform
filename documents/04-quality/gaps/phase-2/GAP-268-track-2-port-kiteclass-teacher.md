# GAP-268: Track 2 Port — kiteclass-teacher → production Next.js

**Status:** 🟡 PARTIAL — Wave 49 Bucket B shipped 24-screen production port via consolidated `(teacher)/teacher/*` route group (PR Wave 49 Bucket B). Real backend wiring of attendance / gradebook / schedule / reports beyond the existing `attendance-period` API + per-screen UI scoring re-audit deferred to follow-ups.
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

- [ ] All 24 screens ≥105/128 — **per-screen ui-review /128 audit deferred to GAP-268-followup-ui-score** (production routes shipped, scoring depends on full UI audit suite which is post-wave per `post-wave-audit-mandate.md` §2.1)
- [x] G2/G3/G4/G8 components imported from shared lib (post-GAP-273) — verified via grep: `AttendanceRoster`, `GradebookEntryGrid`, `ClassScheduleManager` consumed from `@kite/shared-ui`; G8 AttendanceCalendar surfaced via existing `(teacher)/attendance/period` route. No inline copies.
- [x] VN 10-pt grade scale validation working — G3 native `validateGrade` wired in `/teacher/grades/[classId]`
- [ ] Daily attendance saves to backend (existing endpoints) — **PARTIAL**: existing `(teacher)/attendance/period/[classId]/[periodNo]/[date]` route already wires `attendancePeriodApi`. New `/teacher/attendance/[classId]` overview-by-class route saves via stub (TODO) — backend extension tracked in GAP-268-followup-attendance-overview-api.
- [x] Recurring schedule rules (G4 conflict detection) — wired via G4 `detectConflicts` in `/teacher/schedule`
- [x] Subject teacher MoET-format report card output — `/teacher/reports/[classId]` outputs MoET classification (Xuất sắc / Giỏi / Khá / TB / Yếu) per Thông tư 22/2021/TT-BGDĐT
- [x] Vietnamese-only, realistic VN teacher data — 25 VN names + class codes (Lớp 10A2 / 11B1 / 12C1) + currency (VND) + dates (vi-VN locale)
- [x] WCAG AA preserved — kept shared-ui component contrast (≥4.5:1 inherited from prototype + Tailwind theme)
- [ ] E2E: teacher login → mark attendance Lớp 6A1 → enter grades → see report — **PARTIAL**: routes navigable end-to-end + smoke test passes (`teacher-shell.test.tsx` 4 PASS); full Playwright E2E flow tracked in GAP-268-followup-e2e-flow.

## Related

- HTML prototype: `ui_kits/kiteclass-teacher/`
- Components dependency: GAP-273 (G2/G3/G4/G8 must port first)
- Sister gaps: GAP-266 (owner), GAP-267 (parent), GAP-269 (student)

## Effort estimate

~1-2 weeks (after GAP-273 lands components). Wave-pack candidate.

## Log

- **2026-05-10 (Wave 49 Bucket B):** Production port shipped — 11 routes under canonical `(teacher)/teacher/*` route group consolidating the legacy `(dashboard)/teacher/dashboard` page (which was deleted) + the existing `(teacher)/attendance/period/[classId]/[periodNo]/[date]` per-tiết route from Wave 18b2 (preserved). New shell `TeacherShell` ships top nav (Điểm danh / Vào điểm / Lịch dạy / Báo cáo + Settings + identity badge). Build green (`pnpm build` PASS). All 705 unit tests PASS (4 new `teacher-shell.test.tsx`). Per `gap-done-discipline.md` §3, gap stays 🟡 PARTIAL: per-screen UI score audit + full backend wiring + Playwright E2E deferred to follow-up gaps. Coordinator + reviewer should file the 3 follow-ups before flipping DONE: (1) GAP-268-followup-ui-score for /128 per-screen audit; (2) GAP-268-followup-attendance-overview-api for `/teacher/attendance/[classId]` save-batch endpoint; (3) GAP-268-followup-e2e-flow for Playwright login → attendance → grade → report flow.
- **2026-04-29:** Filed after user accepted Round 3 quality.

- **2026-05-11 (Wave 53 Phase 4 milestone audit — UI /128 ❌ NOT DONE-eligible):** Bucket A static-analysis audit (PR #1106) avg 107.8/128 (range 100-113); 3 screens <105 (loading/empty/error states). Carry-forward to existing GAP-429 umbrella (transient-state UX pattern: loading skeletons + empty states + error recovery) — coordinator confirmed NO new gap needed. Status stays 🟡 PARTIAL pending GAP-429 cluster closure.
