# GAP-1527: kiteclass-core OWASP A01 residual — bulk-import / attendance / assignment / financial-IDOR / SVG-MIME

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-22 (security wave continuing GAP-1491 — method-level @PreAuthorize sweep, kiteclass-core)
**Affects:** `kiteclass-core` controllers — student/enrollment bulk-import, attendance, attendance-period, assignment submissions, financial reads (invoice/payment/installment/refund), payment-record audit actor, child-protection upload, settings/storage SVG allowlist

## Problem

GAP-1491 (#2525) added method-level `@PreAuthorize` to the financial + admin controllers it covered (Payment / RefundRequest / InstallmentPlan / Course / Instance / SubjectGrade / Student / Teacher). The cluster MISSED several OWASP A01 (Broken Access Control) surfaces in kiteclass-core that were still wide-open (no `@PreAuthorize`, relying solely on the URL-layer `anyRequest().permitAll()` + Hibernate tenant filter) or used an over-broad role list:

1. **Bulk-import endpoints fully unguarded (P0):** `POST /students/bulk-import/{preview,commit}` + `jobs/{id}/errors` and `POST /enrollments/bulk-import/{preview,commit}` had NO `@PreAuthorize` — any authenticated caller (incl. STUDENT / PARENT) could bulk-create students or bulk-enroll.
2. **Attendance reads + delete unguarded (P1):** `GET /attendance/{id}`, `GET /attendance/enrollment/{id}`, `DELETE /attendance/{id}` had no role guard. `GET /attendance/periods/{id}` + `/students/{id}` + `/subject-sections/{id}` likewise.
3. **Assignment submission/grade reads unguarded (P1):** 4 submission GET endpoints (`/submissions/{id}`, `/{assignmentId}/submissions`, `/{assignmentId}/submissions/student/{studentId}`, `/submissions/student/{studentId}`) exposed submissions + grades to any caller; `POST /assignments/submit` was not role-gated (a TEACHER/ADMIN could spoof a student submission).
4. **Financial read intra-tenant IDOR (P2):** invoice / payment / installment-plan / refund-request READ-by-id endpoints included `TEACHER` in the role list, so a teacher could read any other student's invoice / payment within the tenant (tenant-wide financial reads are not a teacher concern). Financial WRITE endpoints already exclude TEACHER.
5. **PaymentRecord audit-actor hardcoded (P2):** `PaymentRecordController.recordPayment` set `recordedByUserId = 1L` hardcoded, silently attributing (and potentially colliding with) a real teacher whose numeric reference id == 1 (OWASP A09 audit-integrity).
6. **Child-protection upload trusts client MIME (P2):** `VettingController.uploadDocument` stored the client-supplied `file.getContentType()` verbatim with no server-side content sniff — a script payload spoofed with an image/pdf header would pass.
7. **Generic storage allowlist accepts SVG + client MIME (P2 — GAP-1489):** `StorageServiceImpl.ALLOWED_MIME_TYPES` contained `image/svg+xml` (active-content stored-XSS) validated against the client-declared MIME only.

## Proposed Fix

Add method-level `@PreAuthorize` mirroring the GAP-1491 role-string convention (gateway `X-User-Roles` → Spring `ROLE_*` authorities via `GatewayHeaderAuthenticationFilter`; roles `OWNER/ADMIN/PRINCIPAL/STAFF/TEACHER/STUDENT/PLATFORM_ADMIN` — there is no ACCOUNTANT role in kiteclass-core). Drop TEACHER from financial reads. Resolve PaymentRecord actor from `UserContext`. Add server-side magic-byte sniff to VettingController. Remove `image/svg+xml` from the generic storage allowlist (closes GAP-1489).

## Acceptance Criteria

- [x] BulkImportController preview/commit/jobs-errors guarded `hasAnyRole('OWNER','ADMIN','PRINCIPAL','STAFF')`
- [x] EnrollmentBulkImportController preview/commit guarded `hasAnyRole('OWNER','ADMIN','PRINCIPAL','STAFF','TEACHER')`
- [x] AttendanceController GET/{id}, GET /enrollment/{id}, DELETE /{id} guarded `hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')`
- [x] AttendancePeriodController 3 GET reads guarded same role tier
- [x] AssignmentController 4 submission/grade GET guarded `hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')`; POST /submit guarded `hasRole('STUDENT')`
- [x] PaymentRecordController resolves actor from `UserContext.getCurrentReferenceId()` (0L sentinel for admin/owner) instead of hardcoded 1L
- [x] Financial READ endpoints drop TEACHER (Invoice/Payment/InstallmentPlan/RefundRequest), add PRINCIPAL where consistent
- [x] VettingController upload adds server-side magic-byte content sniff (PDF/JPG/PNG allowlist)
- [x] StorageServiceImpl removes `image/svg+xml` from allowlist (closes GAP-1489)
- [x] New `*AuthzTest` web-slice tests per newly-guarded controller (allow-right-role + deny STUDENT/PARENT → 403); financial TEACHER-removal asserts TEACHER → 403
- [x] `./mvnw compile -P strict-warnings` clean (no new strict-warnings)
- [x] New authz tests pass locally (`*AuthzTest,*PaymentRecord*Test`, 49 tests green)
- [ ] CI green on PR (full kiteclass-core suite) — PENDING
- [ ] G2 human walk verify each guarded flow on production-equivalent stack — DEFERRED to flow campaign

## Related

- Parent cluster: GAP-1491 (#2525) — method-level @PreAuthorize on financial+admin controllers (this gap = the kiteclass-core residual the cluster missed)
- Closes: GAP-1489 (StorageServiceImpl svg+xml + client-trusted MIME) — fixed by the SVG-drop in this gap; flipped DONE, file moved to closed/
- Sibling sweep precedent: GAP-1037 (branding SVG-XSS — raster-only allowlist + magic-byte sniff)
- Method-security test interplay: GAP-1524 (`CrossUserAuthzTest` regression from GAP-1491 — `@SpringBootTest` method security needs `.with(user(ROLE))`). Design-first investigation confirmed the affected `@SpringBootTest` integration tests (`CrossTenantAuthzTest`, `AttendanceIntegrationTest`, `AssignmentIntegrationTest`, `AttendancePeriodIntegrationTest`) do NOT enable `@EnableMethodSecurity` (that annotation lives on `SecurityConfig` which is `@Profile("!test")`), so the new `@PreAuthorize` annotations are inert there — no regression. Only web-slice `*AuthzTest` classes (their own nested `@EnableMethodSecurity`) enforce guards.
- Parallel agent: GAP-1526 (held by a parallel agent in the same security wave)
- MIME-validation class: `pre-handoff-self-test-completeness.md` §2.5 file-upload checklist
- Discovered in: branch `fix/kiteclass-core-a01-authz-2026-06-22`
