# GAP-1491: Financial + admin controllers missing method-level @PreAuthorize (OWASP A01 cluster)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-19 (Wave close-2 SEC — GAP-1005 cross-flow sweep, OWASP A01)
**Affects:** `kiteclass-core` multiple controllers — `PaymentController`, `RefundRequestController`, `InstallmentPlanController`, `CourseController`, `InstanceController`, `SubjectGradeController`, `StudentController`, `TeacherController`

## Problem

GAP-1005 (InvoiceController @PreAuthorize) cross-flow sweep phát hiện sister controllers cùng class OWASP A01 (broken function-level authorization): có endpoint mapping nhưng **0 / thiếu class+method-level `@PreAuthorize` role guard**. Số liệu sweep (mappings / có @PreAuthorize):

| Controller | mappings | @PreAuthorize | Mức rủi ro |
|---|---:|---:|---|
| `PaymentController` | 8 | 0 | 🔴 cao — financial mutation |
| `RefundRequestController` | 5 | 0 | 🔴 cao — financial |
| `InstallmentPlanController` | 5 | 0 | 🔴 cao — financial |
| `CourseController` | 11 | 0 | 🟠 |
| `InstanceController` | 8 | 0 | 🟠 |
| `SubjectGradeController` | 4 | 0 | 🟠 |
| `StudentController` | 6 | 1 | 🟡 phần lớn thiếu |
| `TeacherController` | 7 | 1 | 🟡 phần lớn thiếu |

Hệ quả: nếu SecurityConfig chỉ require `authenticated()` cho `/api/**` (không role-restrict), bất kỳ user đã đăng nhập (vd STUDENT/PARENT) có thể gọi financial mutation (PaymentController/RefundRequestController) → privilege escalation. Khác `@authz.hasAccessTo*` SpEL guard ở các controller đã fix (InvoiceController/GradeController/ReportController).

**Lưu ý:** cần verify baseline SecurityConfig — "0 @PreAuthorize" = thiếu method-level role restriction, KHÔNG nhất thiết = unauthenticated. Nhưng financial endpoint phải role-restricted (OWNER/ADMIN/STAFF), không chỉ authenticated.

## Proposed Fix

1. Audit `SecurityConfig` baseline cho `/api/**` (authenticated-only vs role-aware).
2. Per-endpoint role design: financial mutation → `hasAnyRole('OWNER','ADMIN','STAFF','PLATFORM_ADMIN')`; read → broader tier; mirror InvoiceController/GradeController pattern (`@authz.hasAccessTo*` SpEL where row-scope needed).
3. Add `@PreAuthorize` per endpoint + 403 IT test per controller (mirror `InvoiceControllerAuthzTest` / `ReportControllerAuthzTest`).
4. EXEMPT confirmed: `PaymentWebhookController` (signature-auth), `Internal*Controller` (gateway-blocked S2S), `AuthController` (login), `Public*Controller` / `LandingPageController` (public).

## Acceptance Criteria

- [ ] SecurityConfig baseline audited + documented (authenticated vs role-aware for /api/**)
- [ ] 3 financial controllers (Payment/RefundRequest/InstallmentPlan) role-guarded + 403 IT
- [ ] Remaining controllers (Course/Instance/SubjectGrade/Student/Teacher) role-guarded + 403 IT
- [ ] EXEMPT controllers documented (webhook/internal/public)

## Related

- Parent sweep: GAP-1005 (InvoiceController @PreAuthorize — PARTIAL Wave close-2 SEC)
- Same class: GAP-999 (hasAccessToGrade), GAP-1000 (finalize JWT-derived) — SpEL authz pattern reference
- OWASP A01 broken function-level authz; `pre-launch-auth-hardening-checklist`
