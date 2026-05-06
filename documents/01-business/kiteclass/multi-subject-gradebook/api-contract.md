---
title: Multi-Subject Gradebook — API Contract (K-12 schools, TT 22/2021)
status: draft
created: 2026-05-05
updated: 2026-05-06
domain: kiteclass.multi-subject-gradebook
gaps: [GAP-323c, GAP-360]
---

# Multi-Subject Gradebook — API Contract

> **Phase 1C v1.5 status:** Wave 24 Bucket B (GAP-360 §360.1 + §360.4 + §360.5 +
> §360.6) ships the lifecycle endpoints below. Read endpoints (gradebook
> projection per role) + UI variants are still deferred to Wave 25 (GAP-360 §360.3).

All endpoints are mounted under the {@code /api/v1} prefix and require the
Gateway-injected `X-User-Reference-Id` header (request returns HTTP 401
`AUTH_REQUIRED` when absent). Real role enforcement (Tổ trưởng / Hiệu trưởng)
depends on GAP-058 — enforcement is documented inline as TODO until that
lands.

## Internal service surface (no HTTP, used by other services)

```java
public interface GradeFormulaService {
    BigDecimal computeDTBmHK(Long studentId, Long subjectSectionId, Long semesterId);
    BigDecimal computeDTBmCN(Long studentId, Long subjectSectionId, Long academicYearId);
}

public interface SubjectGradeService {
    Long submitForReview(Long gradeId, Long submitterId);
    SubjectGrade review(Long gradeId, Long reviewerId);
    SubjectGrade publish(Long gradeId, Long publisherId);
    SubjectGrade revertToDraft(Long gradeId, Long reviewerId);
}
```

Both services run inside the JPA session of the caller; no transactions
opened beyond `@Transactional` propagation already declared per method.

## HTTP endpoints (Phase 1C v1.5 — shipped Wave 24 Bucket B)

### `POST /api/v1/grades/subjects/{id}/submit-for-review`

GV bộ môn flags a DRAFT grade as ready for Tổ trưởng. No status transition
yet (the marker exists for the api-contract; full notification flow is
GAP-360 §360.2, depends on GAP-063b).

| Aspect | Detail |
|--------|--------|
| Path param | `id` — `SubjectGrade.id` |
| Header | `X-User-Reference-Id` — submitter (GV bộ môn) user id |
| Body | (none) |
| Success | `200 OK` with `{success: true, data: <gradeId>, message: "Submitted for review"}` |
| Errors | `409 INVALID_GRADE_TRANSITION` if status is not DRAFT • `404 GRADE_NOT_FOUND` • `401 AUTH_REQUIRED` |
| RBAC | Out of scope §360.4; will be GV bộ môn (own subject only) under GAP-058 |
| Side-effects | None (no state change in §360.1) |

### `POST /api/v1/grades/subjects/{id}/review`

Tổ trưởng marks a DRAFT grade as REVIEWED. Per BR-GRADEBOOK-006 the only
allowed source state is DRAFT; calling on REVIEWED or PUBLISHED returns 409.

| Aspect | Detail |
|--------|--------|
| Path param | `id` — `SubjectGrade.id` |
| Header | `X-User-Reference-Id` — reviewer (Tổ trưởng) user id |
| Body | (none) |
| Success | `200 OK` with `{success: true, data: <SubjectGrade>}` (status=REVIEWED, reviewedBy populated) |
| Errors | `409 INVALID_GRADE_TRANSITION` (current ∈ {REVIEWED, PUBLISHED}) • `404 GRADE_NOT_FOUND` • `401 AUTH_REQUIRED` |
| RBAC | Out of scope §360.4; will be Tổ trưởng-of-subject under GAP-058 |
| Side-effects | `SubjectGrade.status = REVIEWED`, `reviewedBy = <reviewer>` |

### `POST /api/v1/grades/subjects/{id}/publish`

Hiệu trưởng marks a REVIEWED grade as PUBLISHED — terminal state. Triggers
`UC-GRADEBOOK-PUBLISH-COMPLETE` (Outbox event when this is the last grade
for the (student, academicYear)).

| Aspect | Detail |
|--------|--------|
| Path param | `id` — `SubjectGrade.id` |
| Header | `X-User-Reference-Id` — publisher (Hiệu trưởng) user id |
| Body | (none) |
| Success | `200 OK` with `{success: true, data: <SubjectGrade>}` (status=PUBLISHED, publishedAt populated) |
| Errors | `409 INVALID_GRADE_TRANSITION` (current ∈ {DRAFT, PUBLISHED}) • `404 GRADE_NOT_FOUND` • `401 AUTH_REQUIRED` |
| RBAC | Out of scope §360.4; will be Hiệu trưởng under GAP-058 |
| Side-effects | `SubjectGrade.status = PUBLISHED`, `publishedAt = now`. Outbox row written if all-published-for-year (see §360.5). |

### `POST /api/v1/grades/subjects/bulk-publish`

Hiệu trưởng "publish all REVIEWED in one click" mass action — best-effort
per BR-GRADEBOOK-007 (max 500 ids per request).

| Aspect | Detail |
|--------|--------|
| Header | `X-User-Reference-Id` — publisher (Hiệu trưởng) |
| Body | `{"gradeIds": [<Long>, ...]}` (1-500 entries, validated by `@Valid` + `@Size(max=500)`) |
| Success | `200 OK` with `{success: true, data: {publishedCount: <int>, skippedCount: <int>, errors: ["<id>: <code>", ...]}}` |
| Errors | `400` when body fails validation (`@NotEmpty` / `@Size`) • `401 AUTH_REQUIRED` |
| RBAC | Out of scope §360.4; will be Hiệu trưởng under GAP-058 |
| Side-effects | Per-grade publish runs through `SubjectGradeService.publish` — same audit + Outbox path as single publish. |

## Outbox event (§360.5)

When `publish` flips the LAST DRAFT/REVIEWED grade for `(studentId,
academicYearId)` to PUBLISHED, this event is written to the `outbox_events`
table and dispatched by the existing outbox worker.

| Aspect | Detail |
|--------|--------|
| Routing key | `kiteclass.k12.grades.all-published` |
| Aggregate type | `SubjectGradeBook` |
| Aggregate id | `<studentId>:<academicYearId>` (composite, used for idempotency) |
| Payload | `{"studentId": <Long>, "academicYearId": <Long>, "publishedAt": "<ISO-8601 Instant>"}` |
| Producer | `SubjectGradeAllPublishedListener#onPublish` (called inside publish txn) |
| Consumers (planned) | GAP-055 MOET học bạ generator, GAP-059 conduct grade trigger |
| Delivery guarantee | At-least-once (Outbox pattern per `design-patterns.md` §3.5) |

## Error code reference

| Code | HTTP | Meaning |
|------|------|---------|
| `INVALID_GRADE_TRANSITION` | 409 Conflict | Status transition violates BR-GRADEBOOK-006 (e.g. DRAFT → PUBLISHED skip, or any exit from terminal PUBLISHED). Args: `current`, `target`. |
| `GRADE_NOT_FOUND` | 404 Not Found | Grade id does not resolve to a non-deleted row. |
| `AUTH_REQUIRED` | 401 Unauthorized | `X-User-Reference-Id` header missing on a self-service endpoint. |
| `GRADE_NOT_ASSIGNED_TEACHER` | 403 Forbidden | (Reserved — RBAC layer GAP-058) Caller is not the GV bộ môn assigned to the subject section. |
| `GRADE_NOT_TO_TRUONG` | 403 Forbidden | (Reserved — GAP-058) Caller is not Tổ trưởng for subject. |
| `GRADE_NOT_HIEU_TRUONG` | 403 Forbidden | (Reserved — GAP-058) Caller is not Hiệu trưởng for tenant. |
| `GRADE_PUBLISHED_LOCKED` | 409 Conflict | (Reserved) Cannot edit a PUBLISHED grade. Phase 1C terminal-state — no un-publish path. |

## Read endpoints (deferred — Wave 25 / GAP-360 §360.3)

The following endpoints are planned for the gradebook UI variants but are
NOT shipped in Wave 24 Bucket B. They land alongside the FE work in Wave 25:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/grades/subjects?status=REVIEWED` | GET | Tổ trưởng review queue (filter by status) |
| `/api/v1/gradebook/students/{studentId}/semesters/{semesterId}` | GET | Student's per-semester gradebook view |
| `/api/v1/gradebook/classes/{homeroomClassId}/semesters/{semesterId}` | GET | Class-wide grid (HS rows × môn columns × kỳ tabs) |
| `/api/v1/gradebook/grades/{id}` | PATCH | GV bộ môn updates DRAFT score |

## Verification chain (BR → UC → endpoint → controller → test)

| Business Rule | Use Case | Endpoint | Controller method | Test |
|---------------|----------|----------|-------------------|------|
| BR-GRADEBOOK-001 | UC-GRADE-COMPUTE-DTBmHK | (internal service) | `GradeFormulaServiceImpl#computeDTBmHK` | `GradeFormulaServiceImplTest` |
| BR-GRADEBOOK-002 | UC-GRADE-COMPUTE-DTBmCN | (internal service) | `GradeFormulaServiceImpl#computeDTBmCN` | `GradeFormulaServiceImplTest` |
| BR-GRADEBOOK-003 + BR-GRADEBOOK-006 | UC-GRADEBOOK-REVIEW | `POST /api/v1/grades/subjects/{id}/review` | `SubjectGradeController#review` | `SubjectGradeControllerTest`, `SubjectGradeServiceImplTest` |
| BR-GRADEBOOK-006 | UC-GRADEBOOK-PUBLISH | `POST /api/v1/grades/subjects/{id}/publish` | `SubjectGradeController#publish` | `SubjectGradeServiceImplTest` |
| BR-GRADEBOOK-007 | UC-GRADEBOOK-BULK-PUBLISH | `POST /api/v1/grades/subjects/bulk-publish` | `SubjectGradeController#bulkPublish` | `SubjectGradeControllerTest#bulkPublish_skipsAlreadyPublished` |
| BR-GRADEBOOK-008 | UC-GRADEBOOK-PUBLISH-COMPLETE | (Outbox event `kiteclass.k12.grades.all-published`) | `SubjectGradeAllPublishedListener#onPublish` | `SubjectGradeAllPublishedListenerTest` |
