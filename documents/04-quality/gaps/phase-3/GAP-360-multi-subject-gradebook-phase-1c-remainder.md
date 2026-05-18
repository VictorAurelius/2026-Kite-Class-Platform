# GAP-360: Multi-Subject Gradebook Phase 1C Remainder — state machine + Tổ trưởng workflow + UI + bulk publish + học bạ hook

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Phase 1C remainder of GAP-323c v1; sister of GAP-323c)
**Domain:** Backend + Frontend (gradebook + workflow)
**Detected:** 2026-05-05 (Wave 19 Bucket B closure)
**Affects:** P5 K-12 (TT 22/2021 mandate + AC-OPS-003 12-15 môn gradebook); GVCN, Tổ trưởng, Hiệu trưởng workflows

## Context

Wave 19 Bucket B (GAP-323c v1, this PR) shipped the SubjectGrade Phase 1C
backend foundation (extended entity + GradeFormulaService TT 22/2021 + V55
migration + 5-attribute multi-subject-gradebook 3-layer business docs +
12 unit tests + 3 integration tests). Per `gap-done-discipline.md` §3
PARTIAL exit ramp the deferred items are filed here as the explicit
follow-up scope so they are tracked instead of relying on memory.

## Problem

Phase 1C v1 ships the column persistence + read-path formula service. The
remaining slices block AC-OPS-003 (12-15 môn gradebook) and downstream
hooks (GAP-055 học bạ MOET format, GAP-059 conduct grade):

1. **State-machine enforcement** — `SubjectGradeStatus` column persists
   but transitions are not gated; callers can still call
   `subjectGrade.setStatus(PUBLISHED)` directly. BR-GRADEBOOK-003 mandates
   `DRAFT → REVIEWED → PUBLISHED` only. Without enforcement, the column is
   advisory.
2. **Tổ trưởng approval workflow** — no service method exists for
   `gradeService.review(...)` / `gradeService.publish(...)` with
   notification to Tổ trưởng on DRAFT submission. Depends on GAP-063b
   notification engine + GAP-058 role hierarchy (Tổ trưởng per subject).
3. **Multi-subject gradebook UI** — 4 view variants required (Admin,
   Hiệu trưởng, GV bộ môn, Tổ trưởng); Phase 1C v1 ships zero FE.
4. **Bulk publish action** — Hiệu trưởng "publish all REVIEWED in one
   click after spot-check" UX still missing.
5. **Học bạ generation hook** — after all subject grades PUBLISHED for
   academic year, hook into học bạ generator (depends GAP-055 for MOET
   form format).

## Proposed Fix

### 360.1 — State machine enforcement (State Pattern, `design-patterns.md` §3.3)
- Move `SubjectGrade.setStatus(...)` to package-private; expose only
  `gradeService.review(gradeId, reviewerId)` /
  `gradeService.publish(gradeId, publisherId)` mutators.
- Throw `IllegalStateException` for invalid transitions
  (`DRAFT → PUBLISHED` skipping REVIEWED, any backwards transition).
- ArchUnit test if practical: no other class invokes setStatus.

### 360.2 — Tổ trưởng approval workflow + notification
- Notification on DRAFT submission to assigned Tổ trưởng (per subject).
- Wait for **GAP-063b** notification engine + **GAP-058** role hierarchy
  (Tổ trưởng assignment per subject) before this slice unblocks.

### 360.3 — Multi-subject gradebook UI (4 variants)
- **Admin / Hiệu trưởng:** per-class grid (HS rows × môn columns × kỳ tabs).
- **GV bộ môn:** edit-only-own-môn; others read-only.
- **Tổ trưởng:** review queue listing pending DRAFT → review action.
- **Hiệu trưởng:** publish queue listing REVIEWED → bulk publish.
- ~10–15 days FE work (most expensive remainder slice).

### 360.4 — Bulk publish action
- "Publish all REVIEWED" mass action (with confirmation modal) for
  Hiệu trưởng. Depends 360.1 state-machine enforcement.

### 360.5 — Học bạ generation hook
- After all subject grades PUBLISHED for an academic year, fire a
  domain event (Outbox per `design-patterns.md` §3.5) consumed by
  GAP-055 học bạ generator.
- Phase 1C remainder lays the **trigger only**; full học bạ format is
  separate (GAP-055).

### 360.6 — `api-contract.md` endpoints
- Phase 1C v1 left the file empty of endpoints. Wire endpoints once UI +
  state-machine ship: `POST /api/v1/grades/{id}/review`,
  `POST /api/v1/grades/{id}/publish`, batch variants, gradebook read
  endpoints with role-based projections.

## Acceptance Criteria

- [x] State-machine enforcement via Service mutators + invalid-transition
      `IllegalGradeTransitionException` (HTTP 409 INVALID_GRADE_TRANSITION).
      Shipped Wave 24 Bucket B §360.1.
- [ ] ArchUnit (or unit test) preventing direct setStatus from other packages.
      Deferred — no ArchUnit dep on classpath; reviewer-checklist + service
      contract + 11 unit tests cover the boundary for now. Tracked as
      follow-up under §Out-of-scope below.
- [ ] Tổ trưởng approval workflow + notification integrated with GAP-063b.
      §360.2 — depends on notification engine (out of scope this PR).
- [ ] Tổ trưởng-per-subject role wiring (GAP-058 dependency).
      §360.2 — depends on role hierarchy.
- [ ] 4-variant gradebook UI (Admin / Hiệu trưởng / GV bộ môn / Tổ trưởng).
      §360.3 — Wave 25 FE.
- [x] Bulk publish action for Hiệu trưởng. Shipped Wave 24 Bucket B §360.4
      (`POST /api/v1/grades/subjects/bulk-publish`, max 500 ids,
      best-effort semantics).
- [x] Học bạ Outbox event published on all-PUBLISHED-per-AY. Shipped Wave 24
      Bucket B §360.5 — routing key `kiteclass.k12.grades.all-published`.
- [x] `api-contract.md` filled with concrete endpoints + samples.
      Shipped Wave 24 Bucket B §360.6.
- [x] Tests: state-machine unit (11) + listener unit (5) + controller slice (3)
      = 19 tests passing. Wave 24 Bucket B.
- [ ] Tổ trưởng workflow integration test + Playwright UI smoke.
      §360.2 + §360.3 follow-up.
- [x] mvn green on touched module (`SubjectGradeServiceImplTest`,
      `SubjectGradeAllPublishedListenerTest`, `SubjectGradeControllerTest`).

## Dependencies

- **GAP-063b** — notification engine (hard-blocks 360.2).
- **GAP-058** — role hierarchy / Tổ trưởng-per-subject (hard-blocks 360.2 wiring).
- **GAP-055** — học bạ MOET format (consumes 360.5 event).
- **GAP-323c** — Phase 1C v1 foundation (this gap's predecessor; PARTIAL).

## Estimated Effort

~3 weeks (combined):
- 360.1: State machine — ~3 days
- 360.2: Workflow + notification — ~5 days (after deps land)
- 360.3: UI 4 variants — ~10–15 days
- 360.4: Bulk publish — ~2 days
- 360.5: Học bạ hook — ~1 day
- 360.6: api-contract.md fill — ~1 day

## Related

- **Predecessor:** GAP-323c Phase 1C v1 (SubjectGrade extension + GradeFormulaService backend) shipped Wave 19 Bucket B 2026-05-05.
- **Sister of:** GAP-359 (Child Protection Phase 1C remainder, Wave 19 Bucket A).
- **Cross-cuts:** GAP-055 (học bạ), GAP-058 (role hierarchy), GAP-063b (notification engine), GAP-059 (conduct grade), GAP-327 (MOET subject taxonomy seed).
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-05-19-k12-legal-phase-1c.md` §3 Bucket B.
- **Business docs:** `documents/01-business/kiteclass/multi-subject-gradebook/{rules.md, use-cases.md, api-contract.md}`.

## Log

- **2026-05-06** — Wave 24 Bucket B partial closure (PR pending). Shipped
  §360.1 (state machine enforcement via `SubjectGradeService` +
  `IllegalGradeTransitionException` + `EnumMap<,Set>` ALLOWED_TRANSITIONS table)
  + §360.4 (`POST /bulk-publish` controller, best-effort, max 500 cap) +
  §360.5 (`SubjectGradeAllPublishedListener` + `SubjectGradeAllPublishedEvent`
  via `OutboxEventWriter`, routing key `kiteclass.k12.grades.all-published`)
  + §360.6 (`api-contract.md` filled with 4 endpoints + Outbox event spec +
  error codes + verification chain). 19/19 tests pass
  (`SubjectGradeServiceImplTest` 11, `SubjectGradeAllPublishedListenerTest` 5,
  `SubjectGradeControllerTest` 3). Status stays 🔵 OPEN — coordinator flips
  at wave closure when §360.2 (Tổ trưởng workflow, depends GAP-063b/058) +
  §360.3 (UI 4 variants, Wave 25 FE) ship. ArchUnit boundary test deferred —
  no dep on classpath; tracked under Out-of-scope.
- **2026-05-05** — Filed by Wave 19 Bucket B closure agent (salvage path; original Bucket B agent died on PC restart). Per `gap-done-discipline.md` §3 PARTIAL exit ramp — captures explicit deferred Phase 1C scope so it is tracked instead of being lost.
