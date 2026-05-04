# GAP-323: Period-based Attendance + Multi-subject Gradebook + ĐTBmHK Formula (TT 22/2021)

**Status:** 🟡 PARTIAL — Multi-subject infra (GAP-054 + GAP-099) shipped Phase 1; period dimension on Attendance + formula service + UI NOT shipped
**Priority:** 🔴 P0
**Domain:** Backend + Data Model + Migration
**Detected:** 2026-05-04 (Wave 17 Bucket D — P5 K-12 persona review)
**Revised:** 2026-05-04 (per GAP-345 state-check audit — initial filing mis-classified multi-subject infrastructure as missing)
**Related Docs:**
- `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` Finding 3
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-OPS-001..003
- Existing GAP-060 (period-based attendance — original), GAP-054 (multi-subject — Phase 1 shipped), GAP-099 (ClassScheduleSlot — Phase 1 shipped)
- GAP-345 (state-check audit revising this gap)

## Current State (verified 2026-05-04 per GAP-345)

### ✅ SHIPPED earlier waves (multi-subject infrastructure)

| Piece | File / Path | Notes |
|-------|-------------|-------|
| `SubjectSection` (Lớp bộ môn) | `kiteclass-core/module/k12/entity/SubjectSection.java` | HomeroomClass + Course + Teacher + schedule + weeklyHours; since 3.15.0 (GAP-054 Phase 1) |
| `SubjectGrade` (điểm 1 HS / 1 môn / 1 HK) | `kiteclass-core/module/k12/entity/SubjectGrade.java` | TT 22/2021 formula in javadoc: "Average = (regular × 1 + midterm × 2 + final × 3) / 6" — formula IS captured |
| `HomeroomClass` | `kiteclass-core/module/k12/entity/HomeroomClass.java` | K-12 lớp chủ nhiệm |
| `Curriculum` | `kiteclass-core/module/k12/entity/Curriculum.java` | curriculum totals |
| `ClassScheduleSlot` (structured weekly schedule) | `kiteclass-core/module/k12/entity/ClassScheduleSlot.java` | GAP-099 Phase 1; javadoc note "Phase 2 future: iCal feed + attendance session generator" |
| `Grade` + `GradeComponent` + `GradingScale` + `Transcript` | `kiteclass-core/module/grade/entity/` | center-model grading infrastructure |
| `Attendance` (per-day model) | `kiteclass-core/module/attendance/entity/Attendance.java` | center model: 1 record per (student, day) |

### ❌ MISSING (this gap's actual scope)

| Piece | Status |
|-------|--------|
| Period dimension on Attendance | ❌ — no `attendance_period` table, no `period_no` column |
| Tenant `vertical_type = K12_SCHOOL` discriminator | ❌ |
| `GradeFormulaService.computeDTBmHK()` service class | ❌ — formula is documented in javadoc but no executable service class |
| Grade state machine (DRAFT → REVIEWED → PUBLISHED) | ❌ — no Tổ trưởng approval chain |
| Mobile UI điểm danh ≤2 min for 42 HS | ❌ |
| Multi-subject gradebook UI 12-15 môn / HS | ❌ — data layer exists (SubjectGrade), FE doesn't render |
| Daily aggregation view (vắng cả ngày = vắng ≥7 tiết) | ❌ |
| Period attendance + grade exposed on parent portal | ❌ depends GAP-321 |
| MOET subject taxonomy seed | ❌ depends GAP-327 |
| Concurrent điểm danh load test (30 GVCN) | ❌ |

**Grep + verification commands run 2026-05-04:**
```bash
ls kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/k12/entity/
# → SubjectSection.java, SubjectGrade.java, HomeroomClass.java, Curriculum.java, ClassScheduleSlot.java
ls kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/entity/
# → Attendance.java (185 LOC, per-day model)
ls kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/grade/entity/
# → Grade.java, GradeComponent.java, GradingScale.java, Transcript.java
grep -rl "vertical_type\|verticalType\|K12_SCHOOL\|attendance_period" kiteclass/ --include="*.java" --include="*.sql"
# → 0 hits (confirms missing)
grep "regular × 1 + midterm × 2 + final × 3" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/k12/entity/SubjectGrade.java
# → match in javadoc (formula documented but NOT in service class)
```
**Verdict:** Multi-subject infrastructure (entities + grade scale + transcript) is PARTIAL — Phase 1 shipped via GAP-054 + GAP-099. Period-attendance dimension AND formula service AND state machine AND UI are missing. This gap reframes as: extend existing infrastructure, do NOT recreate entities.

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

### Phase 2 — Period-attendance dimension + tenant discriminator (Stage 1, Q3 2026)

(Phase 1 = multi-subject entities SubjectSection + SubjectGrade + ClassScheduleSlot SHIPPED earlier waves via GAP-054 + GAP-099; reuse them, do NOT recreate.)

1. **New table:** `attendance_period (id, student_id, class_id, subject_section_id, period_no, date, status, recorded_by, recorded_at)` — references existing SubjectSection, NOT a new Subject entity.
2. **Tenant flag:** Add `tenant.vertical_type = 'CENTER' | 'K12_SCHOOL'` discriminator — period_no required when K12_SCHOOL.
3. **Backwards compat:** Existing per-day `Attendance` table preserved; CENTER tenants unchanged.
4. **Aggregation view:** Daily roll-up view for GVCN dashboard (vắng cả ngày = vắng ≥7 tiết).
5. **MOET subject taxonomy seed (GAP-327 dependency):** Seed Course rows with MOET TT 32/2018 subjects, used by existing SubjectSection.courseId FK.

### Phase 3 — Formula service + state machine + grade publishing (Stage 2, Q4 2026)

(Reuse existing `SubjectGrade` entity + `Grade` + `GradeComponent` + `Transcript`. Do NOT refactor entities; add fields incrementally.)

1. **Add fields to existing SubjectGrade:** `type (enum TX|GK|CK)`, `weight (BigDecimal)`, `status (DRAFT|REVIEWED|PUBLISHED)`, `reviewed_by`, `published_at`. Migration extends existing table, NOT recreates.
2. **TT 22/2021 formula service NEW:** `GradeFormulaService.computeDTBmHK(studentId, subjectSectionId, semesterId)` — wraps formula already documented in `SubjectGrade.java` javadoc into executable service per `design-patterns.md` §1.1 Strategy.
3. **Tổ trưởng approval chain:** State machine `DRAFT → REVIEWED → PUBLISHED` per `design-patterns.md` §3.3 State Pattern (no direct status-set; transition via service method).

### Phase 4 — Mobile UI (Stage 1, Q3 2026 — concurrent with Phase 2)

1. **GVCN mobile điểm danh:** Tap-grid for 42 HS, 4 status (P/A-excused/A-unexcused/Late), submit ≤2min target
2. **Bộ môn per-period:** Inherit GVCN tiết 1 status, add tiết-specific deltas
3. **Auto-aggregation:** Daily roll-up for GVCN dashboard

## Acceptance Criteria

- [x] ~~Multi-subject infrastructure (SubjectSection + SubjectGrade entities)~~ — DONE GAP-054 Phase 1
- [x] ~~ClassScheduleSlot structured weekly schedule~~ — DONE GAP-099 Phase 1
- [x] ~~TT 22/2021 formula documented~~ — DONE in `SubjectGrade.java` javadoc (NOT yet in service class)
- [ ] Migration `V<N>__attendance_period.sql` + `V<N+1>__extend_subject_grade.sql` shipped (backward compatible — extends, not recreates)
- [ ] Tenant `vertical_type` enum added (CENTER, K12_SCHOOL); period_no required when K12_SCHOOL
- [ ] `GradeFormulaService` NEW class implements TT 22/2021 ĐTBmHK + ĐTBmCN formulas with unit tests (wraps formula already in SubjectGrade javadoc)
- [ ] SubjectGrade extended with `type, weight, status, reviewed_by, published_at` fields (no entity recreation)
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

- **2026-05-04 (revision per GAP-345 state-check audit)** — Status flipped 🔵 OPEN → 🟡 PARTIAL. Initial filing claimed "Subject entity multi-class — partial" + "TT 22/2021 weighted formula — missing" but Wave 18b plan brainstorm 2026-05-04 found GAP-054 Phase 1 + GAP-099 Phase 1 SHIPPED: SubjectSection (Lớp bộ môn) + SubjectGrade (with TT 22/2021 formula in javadoc) + ClassScheduleSlot + Curriculum + HomeroomClass entities exist. Period dimension on Attendance + GradeFormulaService class + state machine + UI are still missing. Revised to PARTIAL with accurate Current State + reframed Proposed Fix (Phase 2-4 build-on existing entities, NO entity recreation, extend SubjectGrade fields incrementally). Anti-pattern recurrence — `feedback_audit_grep_scope.md` head-truncation cause.
- **2026-05-04 (initial filing)** — Filed during Wave 17 Bucket D P5 review. State-check: claimed existing attendance per-day only; missed multi-subject infrastructure shipped GAP-054 + GAP-099.
