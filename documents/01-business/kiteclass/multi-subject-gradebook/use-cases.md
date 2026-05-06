---
title: Multi-Subject Gradebook — Use Cases (K-12 schools, TT 22/2021)
status: draft
created: 2026-05-05
updated: 2026-05-06
domain: kiteclass.multi-subject-gradebook
gaps: [GAP-323c, GAP-360]
---

# Multi-Subject Gradebook — Use Cases

> Phase 1C v1 covers two backend-facing use cases (formula computation read
> path). UI use cases (gradebook editing, Tổ trưởng review queue, Hiệu trưởng
> publish queue) are deferred to the Phase 1C remainder follow-up gap per
> `gap-done-discipline.md` §3 PARTIAL exit ramp.

## UC-GRADE-COMPUTE-DTBmHK — Compute semester average (ĐTBmHK)

**Actor:** Backend caller (initially `GradebookReadService` — Phase 1C
remainder; in Phase 1C v1 the service is callable but not yet wired to a
controller). Future actors: GVCN viewing class-wide ĐTBmHK, Hiệu trưởng
running end-of-semester report, transcript generator.

**Trigger:** Caller requests a single (student, subject_section, semester)
ĐTBmHK.

**Pre-conditions:**
1. Tenant has `vertical_type = 'K12_SCHOOL'`.
2. SubjectGrade rows exist for at least one TX, GK, CK each in (student, section,
   semester) — otherwise null is returned.
3. V55 migration has run; existing rows are backfilled to type=TX/status=DRAFT.

**Main flow:**
1. Caller invokes `GradeFormulaService.computeDTBmHK(studentId, subjectSectionId, semesterId)`.
2. Service queries SubjectGradeRepository for all TX records → arithmetic mean
   with HALF_EVEN scale=1.
3. Service queries for GK records → mean (or single value).
4. Service queries for CK records → mean (or single value).
5. Service computes `(TXmean + GK*2 + CK*3) / 6` rounded HALF_EVEN scale=1
   per BR-GRADEBOOK-001 + BR-GRADEBOOK-005.
6. Returns `BigDecimal` scale=1 OR `null` when any component is missing.

**Alternate flows:**
- **A1 (no TX records):** Step 2 returns null → service returns null. UI
  renders "—".
- **A2 (no GK record):** Step 3 returns null → service returns null
  per BR-GRADEBOOK-001 partial-state semantics.
- **A3 (no CK record):** Same as A2 but for CK.
- **A4 (multiple GK or CK records):** Service averages defensively
  (real-world TT 22 caps at 1, but DB doesn't enforce).
- **A5 (zero scores):** Score=0 is preserved as 0.0 result, distinct from
  null (BR-GRADEBOOK-001).

**Errors:**
- Caller passes null for any id → service returns null (defensive; no
  exception). Logged at DEBUG.
- Repository query fails (DB down) → exception propagates per existing
  global error handler.

**Post-conditions:** No state change (read-only). No event emitted (formula
computation is pure read).

**FE behavior:** Phase 1C v1 — N/A (no controller wired yet). Phase 1C
remainder will add a `GET /api/v1/gradebook/student/{id}/semester/{sid}`
endpoint with this service as backing.

## UC-GRADE-COMPUTE-DTBmCN — Compute annual average (ĐTBmCN)

**Actor:** Backend caller (`TranscriptGenerator` future actor, conduct grade
service GAP-059, học bạ generator GAP-055).

**Trigger:** Caller requests a single (student, subject_section, academic_year)
ĐTBmCN at year-end.

**Pre-conditions:**
1. Tenant K-12 school.
2. Both HK1 and HK2 semesters exist for the academic_year.
3. Both semesters have completed TX/GK/CK assessments (otherwise null).

**Main flow:**
1. Caller invokes `GradeFormulaService.computeDTBmCN(studentId, subjectSectionId, academicYearId)`.
2. Service resolves HK1 + HK2 semester IDs from `SemesterRepository`.
3. Service computes ĐTBmHK1 (recursive call to UC-GRADE-COMPUTE-DTBmHK).
4. Service computes ĐTBmHK2 (recursive call).
5. Service computes `(HK1 + 2*HK2) / 3` rounded HALF_EVEN scale=1 per
   BR-GRADEBOOK-002 + BR-GRADEBOOK-005.
6. Returns BigDecimal scale=1 OR null when either ĐTBmHK is null.

**Alternate flows:**
- **A1 (HK1 semester missing):** Step 2 returns null → service returns null.
  Indicates academic_year not fully configured (admin onboarding gap).
- **A2 (HK1 incomplete):** Step 3 returns null → service returns null.
- **A3 (HK2 incomplete):** Step 4 returns null → service returns null.

**Errors:** Same as UC-GRADE-COMPUTE-DTBmHK.

**Post-conditions:** Read-only.

## UC-GRADEBOOK-REVIEW — Tổ trưởng marks DRAFT → REVIEWED

**Actor:** Tổ trưởng chuyên môn (subject-area lead).

**Trigger:** `POST /api/v1/grades/subjects/{id}/review` with `X-User-Reference-Id` header set to the reviewer's user id.

**Pre-conditions:**
1. Grade exists, deleted=false.
2. Grade.status = DRAFT.

**Main flow:**
1. Caller invokes `SubjectGradeService.review(gradeId, reviewerId)`.
2. Service loads grade via `findByIdAndDeletedFalse`.
3. Service validates current=DRAFT against `ALLOWED_TRANSITIONS` (allowed: DRAFT → REVIEWED).
4. Service sets `status=REVIEWED`, `reviewedBy=reviewerId`.
5. Repository.save persists the change.
6. Returns the saved entity.

**Errors:**
- `INVALID_GRADE_TRANSITION` (409) when current ∈ {REVIEWED, PUBLISHED}.
- `GRADE_NOT_FOUND` (404) when id missing or soft-deleted.

**Post-conditions:** Grade locked from GV bộ môn edits (downstream UI must
respect REVIEWED status — out of scope for §360.1).

## UC-GRADEBOOK-PUBLISH — Hiệu trưởng marks REVIEWED → PUBLISHED

**Actor:** Hiệu trưởng (principal).

**Trigger:** `POST /api/v1/grades/subjects/{id}/publish`.

**Pre-conditions:**
1. Grade exists, deleted=false.
2. Grade.status = REVIEWED.

**Main flow:**
1. Caller invokes `SubjectGradeService.publish(gradeId, publisherId)`.
2. Service loads + validates transition.
3. Service sets `status=PUBLISHED`, `publishedAt=Instant.now()`.
4. Repository.save persists.
5. Service invokes `SubjectGradeAllPublishedListener.onPublish(saved)` INSIDE
   the same transaction (see UC-GRADEBOOK-PUBLISH-COMPLETE).
6. Returns the saved entity.

**Errors:**
- `INVALID_GRADE_TRANSITION` (409) when current ∈ {DRAFT, PUBLISHED}.
- `GRADE_NOT_FOUND` (404).

**Post-conditions:** Grade is permanent in học bạ; no further mutations
allowed (PUBLISHED is terminal).

## UC-GRADEBOOK-BULK-PUBLISH — Hiệu trưởng publishes a batch (best-effort)

**Actor:** Hiệu trưởng.

**Trigger:** `POST /api/v1/grades/subjects/bulk-publish` with
`{gradeIds: [...]}` body (max 500 ids per BR-GRADEBOOK-007).

**Main flow:**
1. Controller iterates `gradeIds`, calls `SubjectGradeService.publish` for each.
2. Catches `IllegalGradeTransitionException` + generic `BusinessException`;
   adds entry `"<gradeId>: <errorCode>"` to `errors[]`, increments `skippedCount`.
3. Successful publishes increment `publishedCount`; the all-published listener
   fires per grade (so partial-class batches still trigger học bạ events as
   their last subject lands).
4. Response: `{publishedCount, skippedCount, errors[]}`. HTTP 200 always (best-
   effort semantics — clients inspect counts).

**Errors:** None abort the batch; per-grade errors surface in `errors[]`.

## UC-GRADEBOOK-PUBLISH-COMPLETE — Học bạ generation Outbox trigger

**Actor:** System (invoked by `SubjectGradeServiceImpl.publish`).

**Trigger:** Inside the publish transaction, after `Repository.save(grade)`
sets `status=PUBLISHED`.

**Main flow:**
1. Listener queries `countNotInStatusForStudentAndAcademicYear(studentId,
   academicYearId, PUBLISHED)`.
2. If count > 0 → returns silently (other subjects pending).
3. If count == 0 → constructs `SubjectGradeAllPublishedEvent(studentId,
   academicYearId, Instant.now())`.
4. Serialises payload via Jackson (JSR-310 module required for `Instant`).
5. Calls `OutboxEventWriter.enqueue(routingKey, "SubjectGradeBook",
   "<studentId>:<academicYearId>", payloadJson)` — committed atomically with
   the publish via `Propagation.MANDATORY`.

**Downstream consumers (planned):**
- GAP-055 MOET học bạ generator — materialises year-end transcript.
- GAP-059 conduct grade trigger — finalises hạnh kiểm column.

**Errors:**
- Jackson serialisation failure → logged, no event emitted, publish still
  succeeds (ý đồ "publish must not fail because the trigger failed").
- Caller forgets `@Transactional` → `OutboxEventWriter` (MANDATORY propagation)
  throws — caught at integration level.

## Out-of-scope (deferred to Wave 25 / future gaps)

| Use case | Notes |
|----------|-------|
| UC-GRADEBOOK-ENTER-DRAFT (GV bộ môn enters TX/GK/CK score) | Depends on RBAC. GAP-360 §360.2 |
| UC-GRADEBOOK-RENDER (4 view variants for Admin / Hiệu trưởng / GV / Tổ trưởng) | UI work ~10–15 days. GAP-360 §360.3 |
| UC-GRADEBOOK-NOTIFY-TO-TRUONG (notification on DRAFT submission) | Depends on GAP-063b notification engine + GAP-058 role hierarchy |
| UC-HOC-BA-GENERATE (downstream consumer of UC-GRADEBOOK-PUBLISH-COMPLETE) | GAP-055 MOET form generator |
