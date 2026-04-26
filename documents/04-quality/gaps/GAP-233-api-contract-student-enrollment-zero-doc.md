# GAP-233: API Contract Drift — student-enrollment domain

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Business-Logic — wrong API contract = wrong product per `meta-gap-priority.md` §3)
**Domain:** Backend / API contract documentation
**Found:** 2026-04-26 (post-wave-7 API contract audit, score 42/100 F)
**Affects:** Lifecycle-critical — admissions staff (enroll), homeroom (class-roster), finance (invoice-on-enroll trigger), bulk-import flow, frontend enrollment + student-management screens

## Problem

Post-wave-7 API audit flagged **student-enrollment** as third-highest by endpoint count (lifecycle-critical: enroll/withdraw triggers invoice + attendance + parent-portal record creation). Verified counts via grep on `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/enrollment/`:

| Controller | Method-level mappings | Class base path |
|---|---:|---|
| `EnrollmentController` | 6 | `/api/v1/enrollments` |
| **Total** | **6** | — |

(Audit count of 6 endpoints CONFIRMED. Note: audit named domain `student-enrollment` but kiteclass-core has the controller in `module/enrollment/` only — Student CRUD lives in `module/student/StudentController`, which the doc folder also bundles in. State-check shows the doc covers BOTH student CRUD + enrollment lifecycle.)

Sample endpoints verified in code:
- `POST /api/v1/enrollments` (`EnrollmentController:58`) — enroll student in class
- `GET /api/v1/enrollments/{id}` (`EnrollmentController:77`)
- `GET /api/v1/enrollments/student/{studentId}` (`EnrollmentController:94`)
- `GET /api/v1/enrollments/class/{classId}` (`EnrollmentController:116`)
- `PUT /api/v1/enrollments/{id}/status` (`EnrollmentController:145`)
- `PUT /api/v1/enrollments/{id}/withdraw` (`EnrollmentController:163`)

## Current State (verified 2026-04-26)

`documents/01-business/kiteclass/student-enrollment/api-contract.md` — **EXISTS**, 3.8K, contains both student CRUD endpoints (POST/GET/PUT/DELETE `/api/v1/students/...`) AND enrollment endpoints. Stub quality:

- ✅ All 6 enrollment endpoints have `### {VERB} /api/v1/enrollments/...` headings
- ✅ Student CRUD endpoints (5+ endpoints from `StudentController`) also covered in same file
- ❌ Auth header requirements not specified per endpoint
- ❌ Error code matrices thin or missing
- ❌ UC references (`UC-ENR-XX`, `UC-STU-XX`) absent — `use-cases.md` (5.1K) defines UCs but contract doesn't cite them
- ❌ Side-effect documentation missing — enrollment creation triggers invoice generation, attendance enrollment row, parent-portal access; withdraw triggers refund-request workflow. None of these cross-domain effects documented in api-contract.md
- ❌ Status-transition rules for `/{id}/status` underspecified (allowed transitions: ACTIVE → WITHDRAWN, ACTIVE → COMPLETED, etc.)
- ❌ Request DTO validation (required fields, FK constraints) inconsistent

(Audit reported "0 documented" — INCORRECT. State-check shows enrollment + student CRUD share one api-contract.md file with all endpoints listed; drift is depth + cross-domain side-effect documentation, not absence.)

`rules.md` (3.3K) — defines enrollment eligibility, withdrawal grace window, refund policy on withdraw; api-contract.md should cite these rules per relevant endpoint.

## Root Cause

Enrollment is a long-standing domain (Wave 1–2). Original api-contract.md was generated as inventory before standardization of auth/error conventions. Cross-domain side-effects (invoice trigger, attendance enrollment row, refund-on-withdraw) were not documented because each downstream domain shipped later and never circled back to update the originating contract. No automated check enforces "trigger X when endpoint Y called" cross-references.

## Proposed Fix

1. For each of the 6 enrollment endpoints + student CRUD endpoints, add to `documents/01-business/kiteclass/student-enrollment/api-contract.md`:
   - **Auth block**: `Bearer JWT (role: ADMIN|ADMISSIONS|HOMEROOM)`, `X-Tenant-Id: {slug}`
   - **Error codes**: `400 VALIDATION_ERROR`, `401`, `403`, `404 STUDENT_NOT_FOUND|CLASS_NOT_FOUND`, `409 ALREADY_ENROLLED|CAPACITY_EXCEEDED|WITHDRAWAL_WINDOW_CLOSED`, `422 ENROLLMENT_PERIOD_CLOSED`
   - **UC reference**: `UC-ENR-XX` / `UC-STU-XX` per endpoint
2. Document state-transition table for enrollment status: `ACTIVE → WITHDRAWN | COMPLETED | TRANSFERRED | SUSPENDED`, with allowed transitions and required fields per transition.
3. Document **cross-domain side-effects** explicitly per endpoint:
   - `POST /api/v1/enrollments` → emits `EnrollmentCreated` event → invoice-domain creates invoice (link to GAP-231 contract)
   - `PUT /api/v1/enrollments/{id}/withdraw` → emits `EnrollmentWithdrawn` → refund-request domain (link to GAP-231)
   - Attendance enrollment row auto-created (link to GAP-232)
4. Document withdraw vs status-update distinction (when to call which).
5. Decide whether to split file: keep combined OR split into `student/api-contract.md` + `enrollment/api-contract.md`. Recommend keeping combined for now (cross-cutting domain) but rename folder if appropriate.
6. Run `/api-contract-audit` skill — target ≥85/100 for this domain post-fix.

## Acceptance Criteria

- [ ] All 6 enrollment endpoints + all student CRUD endpoints have explicit auth block
- [ ] All endpoints have explicit error code matrix (≥3 standard + domain-specific)
- [ ] All endpoints reference UC IDs from `use-cases.md`
- [ ] State-transition table for enrollment status documented and matches real Java enum + service-layer guards
- [ ] Cross-domain side-effects (invoice, attendance, parent, refund) documented with event names + links to sibling api-contract.md files
- [ ] Withdraw vs status-update semantics documented
- [ ] File-split decision recorded (combined vs split) with rationale
- [ ] `/api-contract-audit` re-run scores ≥85 for this domain

## Related

- Audit: `documents/04-quality/audits/api/api-contract-audit-2026-04-26-post-wave7.md`
- Rule: `.claude/rules/audit-to-gap-pipeline.md` Step 3 + Step 2.5 state-check
- Rule: `.claude/rules/output-review-mandate.md` §3 matrix (API contracts row)
- Living Documents: `CLAUDE.md` §"CRITICAL: Living Documents"
- Sibling gaps: GAP-231 (payment-invoice — invoice-on-enroll trigger), GAP-232 (attendance — enrollment-attendance row)

## Log

- 2026-04-26 — Filed during post-wave-7 audit retrospective. Source: API contract audit. Endpoint count of 6 CONFIRMED via grep on `module/enrollment/`. State-check found api-contract.md exists with student CRUD + 6 enrollment endpoints listed; drift is depth + cross-domain side-effect documentation, not absence. Recognized doc folder bundles student + enrollment under one name (`student-enrollment/`); kept that grouping for the gap. Scope narrowed per `feedback_gap_state_check_required.md`.
