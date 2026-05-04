# GAP-323: Period-based Attendance + Multi-subject Gradebook + ĐTBmHK Formula (TT 22/2021)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend + Data Model + Migration
**Detected:** 2026-05-04 (Wave 17 Bucket D — P5 K-12 persona review)
**Related Docs:**
- `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` Finding 3
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-OPS-001..003
- Existing GAP-060 (period-based attendance — original), GAP-054 (multi-subject — original)

## Current State (verified 2026-05-04)

| Piece | File / Path | Status |
|-------|-------------|--------|
| `Attendance` entity (per-day model) | `kiteclass-core/.../attendance/Attendance.java` (assumed) | ⚠️ partial — center model: 1 record per (student, day) |
| Period-based schema | — | ❌ missing — need 1 record per (student, day, period, subject) |
| `Subject` entity multi-class | `kiteclass-core/.../course` | ⚠️ partial — single course-class link |
| TT 22/2021 weighted formula (TX×1 + GK×2 + CK×3) | — | ❌ missing |
| Gradebook UI for 12-15 môn / HS | `kiteclass/kiteclass-frontend/src/app/grades` (assumed) | ❌ missing K-12 layout |
| Tổ trưởng approval chain | — | ❌ missing |
| Existing GAP-060 status | OPEN | (this gap consolidates) |
| Existing GAP-054 status | OPEN | (this gap consolidates) |

**Grep commands run:**
```bash
grep -rl "Attendance" kiteclass/kiteclass-core/src/main/java --include="*.java" | head
grep -rl "ĐTB\|TXGKCK\|TT.22" kiteclass/ --include="*.java"
ls kiteclass/kiteclass-core/src/main/resources/db/migration/ | grep -i attendance
```
Result: existing attendance schema is per-day; no period-based field; no MOET formula constants.

## Problem

K-12 fundamentally differs from centers:

1. **Per-period attendance:** 5-10 tiết/day, each tiết a different GV bộ môn — not single per-day attendance
2. **Multi-subject gradebook:** 12-15 subjects per student per semester (Toán, Văn, Anh, KHTN, KHXH, Lý, Hóa, Sinh, Sử, Địa, GDCD, Tin, Công nghệ, Thể dục, Nhạc, Mỹ thuật)
3. **TT 22/2021 weighted formula:** ĐTBmHK = (TB.TX + GK×2 + CK×3) / 6; ĐTBmCN = (ĐTBmHK1 + 2×ĐTBmHK2) / 3
4. **Tổ trưởng approval chain:** GV → Tổ trưởng review → publish → permanent in học bạ

Without this:
- AC-OPS-001 (GVCN điểm danh ≤2 min) FAIL — no period model on mobile
- AC-OPS-002 (period-based) FAIL — schema mismatch
- AC-OPS-003 (12-15 môn gradebook + ĐTBmHK formula) FAIL — single-subject
- Conduct grade (GAP-059) downstream — depends on attendance threshold
- Học bạ MOET format (GAP-055) downstream — depends on grade structure
- Phổ cập escalation (GAP-341) downstream — depends on multi-day attendance threshold

## Context

P5 K-12 review Finding 3. This is the **core daily operations blocker**. Without it, GVCN cannot điểm danh, bộ môn cannot grade, học bạ cannot generate, conduct grade impossible. Single largest data-model migration in K-12 program.

**Migration risk:** Existing center tenants have per-day attendance data. Migration must preserve their data while adding period dimension as optional. Solution: new `attendance_period` table (period = nullable for centers, required for K-12 tenants based on tenant `verticalType`).

## Evidence

- TT 22/2021/TT-BGDĐT Điều 7: ĐTBmHK formula explicitly weighted
- TT 32/2018/TT-BGDĐT: chương trình GDPT 2018 — 13 môn THCS, 17 môn THPT
- P5 review Finding 3: 0% daily ops coverage
- Persona simulation: 30 GVCN concurrent điểm danh tiết 1 (07:00-08:00) requires period dimension

## Proposed Fix

### Phase 1 — Data model migration (Stage 1, Q3 2026)

1. **New table:** `attendance_period (id, student_id, class_id, subject_id, period_no, date, status, recorded_by, recorded_at)`
2. **Tenant flag:** `tenant.vertical_type = 'CENTER' | 'K12_SCHOOL'` — period_no required when K12_SCHOOL
3. **Backwards compat:** Existing per-day `Attendance` table preserved; CENTER tenants unchanged
4. **Aggregation view:** Daily roll-up view for GVCN dashboard (vắng cả ngày = vắng ≥7 tiết)
5. **MOET subject taxonomy seed (GAP-327 dependency):** subject_id references seeded MOET TT 32/2018 subjects

### Phase 2 — Multi-subject gradebook (Stage 2, Q4 2026)

1. **Grade entity refactor:** `Grade (id, student_id, subject_id, semester_id, type=TX|GK|CK, value, weight, recorded_by, status=DRAFT|REVIEWED|PUBLISHED, reviewed_by, published_at)`
2. **TT 22/2021 formula service:** `GradeFormulaService.computeDTBmHK(studentId, subjectId, semesterId)` — weighted average
3. **Tổ trưởng approval chain:** State machine `DRAFT → REVIEWED → PUBLISHED` per `design-patterns.md` §3.3 State Pattern

### Phase 3 — Mobile UI (Stage 1, Q3 2026 — concurrent with Phase 1)

1. **GVCN mobile điểm danh:** Tap-grid for 42 HS, 4 status (P/A-excused/A-unexcused/Late), submit ≤2min target
2. **Bộ môn per-period:** Inherit GVCN tiết 1 status, add tiết-specific deltas
3. **Auto-aggregation:** Daily roll-up for GVCN dashboard

## Acceptance Criteria

- [ ] Migration `V<N>__attendance_period.sql` + `V<N+1>__grade_refactor.sql` shipped (backward compatible)
- [ ] Tenant `vertical_type` enum added (CENTER, K12_SCHOOL); period_no required when K12_SCHOOL
- [ ] `GradeFormulaService` implements TT 22/2021 ĐTBmHK + ĐTBmCN formulas with unit tests
- [ ] Grade state machine DRAFT → REVIEWED → PUBLISHED enforced via State Pattern (no direct status-set)
- [ ] Mobile UI điểm danh ≤2 min for 42 HS (Playwright performance test)
- [ ] Daily aggregation view returns vắng cả ngày = ≥7 tiết vắng
- [ ] Period attendance + grade exposed on parent portal (GAP-321)
- [ ] Documentation 3-layer per `documents/01-business/kiteclass/period-attendance/` + `documents/01-business/kiteclass/multi-subject-gradebook/`
- [ ] business-logic-review.md 5-attribute on rules.md (Source: TT 22/2021 + TT 32/2018; Compliance: Compliant per TT 22/2021 Đ.7; Cadence: Annual + event-driven on TT amendment)
- [ ] Test scenario: 30 GVCN concurrent điểm danh trong 5 phút without DB lock contention

## Related

- **Consolidates K-12 scope of:** GAP-060 (period attendance), GAP-054 (multi-subject) — close those once this lands
- **Blocks:** GAP-055 (học bạ MOET), GAP-059 (conduct), GAP-328 (exam workflow), GAP-341 (phổ cập escalation), GAP-321 (parent portal data feeds)
- **Depends on:** GAP-327 (MOET subject taxonomy seed), GAP-053 (academic year/semester structure)
- **Cross-cuts:** GAP-058 (role hierarchy — Tổ trưởng + GVCN), GAP-056 (GVCN module)
- **Wave plan:** Bucket D Stage 1+2

## Log

- **2026-05-04** — Filed during Wave 17 Bucket D P5 review. State-check: existing attendance is per-day (center model); migration required. Consolidates K-12-specific scope on top of generic GAP-060 + GAP-054.
