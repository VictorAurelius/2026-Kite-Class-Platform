---
title: Multi-Subject Gradebook — API Contract (K-12 schools, TT 22/2021)
status: draft
created: 2026-05-05
updated: 2026-05-05
domain: kiteclass.multi-subject-gradebook
gaps: [GAP-323c]
---

# Multi-Subject Gradebook — API Contract

> **Phase 1C v1 status: PARTIAL — backend service shipped, no controller
> endpoints yet.** This file is mandatory per the 3-layer business-docs
> structure (CLAUDE.md §Business Logic Documents) but lists no endpoints in
> Phase 1C v1. Endpoints are defined when the Phase 1C remainder follow-up
> gap wires the gradebook UI.

## Phase 1C v1 — internal service surface (no HTTP endpoints)

The `GradeFormulaService` is a backend Strategy interface. Phase 1C v1
ships it for callers within `kiteclass-core` only:

```java
public interface GradeFormulaService {
    BigDecimal computeDTBmHK(Long studentId, Long subjectSectionId, Long semesterId);
    BigDecimal computeDTBmCN(Long studentId, Long subjectSectionId, Long academicYearId);
}
```

Both methods are **read-only** and return:
- `BigDecimal` with scale=1 (1 decimal, HALF_EVEN per BR-GRADEBOOK-005), or
- `null` when component data is missing per BR-GRADEBOOK-001/002 partial-state.

No transactions opened beyond JPA session read context; safe to call from
any service or scheduled task.

## Phase 1C remainder — planned endpoints (TBD)

The following are placeholders — defined when state-machine + UI lands in
the Phase 1C remainder follow-up gap. Each will land alongside its DTO,
controller, OpenAPI spec, and integration tests in that PR.

| Endpoint | Method | Purpose | Status |
|----------|--------|---------|--------|
| `/api/v1/gradebook/students/{studentId}/semesters/{semesterId}` | GET | Read all SubjectGrades for student in semester (rendered with computed ĐTBmHK per row). | Planned |
| `/api/v1/gradebook/classes/{homeroomClassId}/semesters/{semesterId}` | GET | Read full class gradebook (HS rows × môn cols × tabs per kỳ). | Planned |
| `/api/v1/gradebook/grades/{id}` | PATCH | GV bộ môn updates DRAFT score. RBAC: only assigned subject teacher. | Planned |
| `/api/v1/gradebook/grades/{id}/review` | POST | Tổ trưởng transitions DRAFT → REVIEWED. RBAC: only Tổ trưởng of subject. | Planned |
| `/api/v1/gradebook/grades/{id}/publish` | POST | Hiệu trưởng transitions REVIEWED → PUBLISHED. RBAC: Hiệu trưởng. | Planned |
| `/api/v1/gradebook/grades/bulk-publish` | POST | Bulk transition REVIEWED → PUBLISHED for class/subject. | Planned |

## Error code reservations (Phase 1C remainder)

Reserved for the upcoming endpoints; not in use until Phase 1C remainder PR
lands them:

| Code | Meaning |
|------|---------|
| `GRADE_INVALID_TRANSITION` | Status transition violates BR-GRADEBOOK-003 order. |
| `GRADE_NOT_ASSIGNED_TEACHER` | Caller is not the GV bộ môn assigned to subject section. |
| `GRADE_NOT_TO_TRUONG` | Caller is not Tổ trưởng for subject. |
| `GRADE_NOT_HIEU_TRUONG` | Caller is not Hiệu trưởng for tenant. |
| `GRADE_PUBLISHED_LOCKED` | Cannot edit a PUBLISHED grade (must un-publish first — Tổ trưởng+ only). |

## Verification chain

Per CLAUDE.md §Business Logic Documents 3-Layer:
- BR-GRADEBOOK-001 → UC-GRADE-COMPUTE-DTBmHK → `GradeFormulaService.computeDTBmHK` →
  `GradeFormulaServiceImpl#computeDTBmHK` → `GradeFormulaServiceImplTest`
- BR-GRADEBOOK-002 → UC-GRADE-COMPUTE-DTBmCN → `GradeFormulaService.computeDTBmCN` →
  `GradeFormulaServiceImpl#computeDTBmCN` → `GradeFormulaServiceImplTest`
- BR-GRADEBOOK-005 → `GradeFormulaServiceImpl.SCALE/ROUNDING` constants →
  boundary test in `GradeFormulaServiceImplTest`
