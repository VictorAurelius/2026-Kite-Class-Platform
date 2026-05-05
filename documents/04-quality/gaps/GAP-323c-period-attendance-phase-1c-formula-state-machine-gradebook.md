# GAP-323c: Period Attendance Phase 1C — GradeFormulaService TT 22/2021 + Tổ trưởng state machine + multi-subject gradebook UI

**Status:** 🟡 PARTIAL — Wave 19 Bucket B v1 SHIPPED 2026-05-05; Phase 1C remainder follow-up: GAP-360-multi-subject-gradebook-phase-1c-remainder
**Priority:** 🔴 P0 (sister of GAP-323 Phase 1A SHIPPED Wave 18b1; sister of GAP-323b Phase 1B)
**Domain:** Backend + Frontend (gradebook)
**Detected:** 2026-05-04 (Wave 18b1 Bucket F closure)
**Affects:** P5 K-12 (TT 22/2021 mandate + AC-OPS-003 12-15 môn gradebook)

## Context

Phase 1A SHIPPED Wave 18b1 (PR #765): AttendancePeriod foundation. Phase 1B (GAP-323b) adds write API + mobile UI. This gap (1C) adds **grading**: TT 22/2021 formula service + Tổ trưởng approval state machine + multi-subject gradebook UI.

Phase 1A state-check confirmed: SubjectGrade entity exists (GAP-054 Phase 1) with TT 22/2021 formula in javadoc; this gap turns javadoc → executable + adds workflow.

## Problem

VN K-12 grading per TT 22/2021/TT-BGDĐT Đ.7:
- ĐTBmHK = (TB.TX + GK×2 + CK×3) / 6
- ĐTBmCN = (ĐTBmHK1 + 2×ĐTBmHK2) / 3
- Tổ trưởng approval chain: GV → review → publish → permanent in học bạ

Without:
- AC-OPS-003 FAIL (12-15 môn gradebook)
- Học bạ MOET format cannot generate (GAP-055 downstream)
- Conduct grade GAP-059 cannot compute

## Proposed Fix

### 1C.1 — Extend SubjectGrade entity
- Add fields: `type (enum TX|GK|CK)`, `weight (BigDecimal)`, `status (enum DRAFT|REVIEWED|PUBLISHED)`, `reviewed_by`, `published_at`
- Migration V<N> kiteclass-core (extends existing `subject_grades` table — additive, backward compat per Phase 1A migration discipline)
- Do NOT recreate entity per GAP-345 audit guidance

### 1C.2 — GradeFormulaService NEW
- `GradeFormulaService.computeDTBmHK(studentId, subjectSectionId, semesterId)` — wraps formula already in SubjectGrade.java javadoc
- `GradeFormulaService.computeDTBmCN(studentId, subjectSectionId, academicYearId)` — annual average
- Strategy Pattern per `design-patterns.md` §1.1 (in case TT formula amendments)
- Unit tests: edge cases (zero TX, missing GK, decimal precision HALF_EVEN scale=1 per MOET convention)

### 1C.3 — Tổ trưởng approval state machine
- State machine `DRAFT → REVIEWED → PUBLISHED` per `design-patterns.md` §3.3 State Pattern
- No direct status-set; transitions via service method `gradeService.review(gradeId, reviewerId)`, `gradeService.publish(gradeId, publisherId)`
- Tổ trưởng role: assign per subject (e.g., Tổ trưởng Toán reviews all Toán grades for school)
- Notification to Tổ trưởng on grade DRAFT submission (depends GAP-063b notification engine)

### 1C.4 — Multi-subject gradebook UI
- Admin/Hiệu trưởng page: per-class grid (HS rows × môn columns × kỳ tabs)
- GV editing: only assigned môn editable; others read-only
- Tổ trưởng review queue: list pending REVIEWED grades for their subject
- Hiệu trưởng publish queue: list REVIEWED grades for final publish
- Bulk operations (publish all REVIEWED in one click after spot-check)

### 1C.5 — Multi-subject gradebook 3-layer business docs
- Documents: `documents/01-business/kiteclass/multi-subject-gradebook/{rules.md, use-cases.md, api-contract.md}` NEW
- 5-attribute frontmatter (Source: TT 22/2021 + TT 32/2018; Compliance: Compliant; Cadence: Annual + event-driven on TT amendment)

### 1C.6 — Học bạ generation hook
- After all subject grades PUBLISHED for academic year, hook into học bạ generator (depends GAP-055 for MOET format)
- Phase 1C lays foundation; full học bạ in separate gap

## Acceptance Criteria

### Phase 1C v1 (Wave 19 Bucket B, this PR — DONE)

- [x] SubjectGrade extended with type/weight/status/reviewed_by/published_at
- [x] V55 migration backward compat verified (existing rows default DRAFT/TX/1.0)
- [x] GradeFormulaService implements ĐTBmHK + ĐTBmCN + 12 unit tests
- [x] Business docs `multi-subject-gradebook/` 3-layer with 5-attribute frontmatter
- [x] Tests: GradeFormulaService unit (10+ edge cases) — 12/12 green; SubjectGradeRepository status/type queries IT (3 tests, env-gated)
- [x] mvn green on `kiteclass-core`

### Phase 1C remainder (tracked in GAP-360, not closed in this PR)

- [ ] State machine DRAFT → REVIEWED → PUBLISHED enforced via Pattern (no direct status-set, ArchUnit test if practical) — GAP-360.1
- [ ] Tổ trưởng approval workflow with notification (coordinate GAP-063b) — GAP-360.2
- [ ] Multi-subject gradebook UI for admin/Hiệu trưởng/GV/Tổ trưởng (4 view variants) — GAP-360.3
- [ ] Bulk publish action for Hiệu trưởng — GAP-360.4
- [ ] Học bạ generation hook — GAP-360.5
- [ ] pnpm green (FE — no FE shipped Phase 1C v1) — GAP-360.3

## Estimated Effort

~3-4 weeks:
- 323c.1: SubjectGrade extension (~2 days)
- 323c.2: GradeFormulaService (~3 days)
- 323c.3: State machine + Tổ trưởng workflow (~5 days)
- 323c.4: Multi-subject gradebook UI (~10-15 days, complex)
- 323c.5: Business docs (~2 days)
- 323c.6: Học bạ hook (~1 day, prep for GAP-055)

## Related

- **Sister of:** GAP-323 Phase 1A (PR #765) + GAP-323b Phase 1B
- **Depends on:** GAP-063b (notification for Tổ trưởng), GAP-058 (role hierarchy — Tổ trưởng exists)
- **Cross-cuts:** GAP-055 (học bạ MOET format downstream), GAP-059 (conduct grade), GAP-327 (MOET subject taxonomy seed)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-18b1-k12-legal-phase-1a.md`

## Log

- **2026-05-05** — Wave 19 Bucket B v1 SHIPPED. Salvaged from pre-WSL-restart agent work + verified by closure agent. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3. Ships: SubjectGrade extension (`type` SubjectGradeType TX/GK/CK + `weight` BigDecimal + `status` SubjectGradeStatus DRAFT/REVIEWED/PUBLISHED + `reviewedBy` Long + `publishedAt` Instant), `GradeFormulaService` Strategy Pattern interface + `GradeFormulaServiceImpl` (`computeDTBmHK` + `computeDTBmCN` per TT 22/2021 Đ.7 with HALF_EVEN scale=1), V55 migration (additive, backward compat — existing rows default DRAFT/TX/1.0/null), 3 new repository methods (status/type queries), 3-layer `multi-subject-gradebook/` business docs with 5-attribute frontmatter (Source: TT 22/2021/TT-BGDĐT + TT 32/2018; Compliance: Compliant; Cadence: Annual + event-driven), BR-GRADEBOOK-001..005. Tests: 12/12 `GradeFormulaServiceImplTest` unit green (full case + multiple TX mean + missing GK/CK/TX → null + HALF_EVEN boundary 5.85→5.8 + zero-vs-null + null inputs + ĐTBmCN both-semesters / missing HK1 / missing HK2 data); 3 `SubjectGradeRepositoryIT` integration tests (env-gated `ENABLE_INTEGRATION_TESTS=true`, compile + ENV-skip verified). mvn green on `kiteclass-core`. Phase 1C remainder (state machine enforcement + Tổ trưởng workflow + 4-variant gradebook UI + bulk publish + học bạ hook) filed as **GAP-360**.
- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
