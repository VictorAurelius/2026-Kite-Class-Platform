# GAP-1491: Financial + admin controllers missing method-level @PreAuthorize (OWASP A01 cluster)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-19 (Wave close-2 SEC — GAP-1005 cross-flow sweep, OWASP A01)
**Closed:** 2026-06-21 (PR loop/gap1491-preauthorize-a01 — 8/8 controllers guarded + 26 web-slice 403 IT green)
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

- [x] SecurityConfig baseline audited + documented (authenticated vs role-aware for /api/**)
- [x] 3 financial controllers (Payment/RefundRequest/InstallmentPlan) role-guarded + 403 IT
- [x] Remaining controllers (Course/Instance/SubjectGrade/Student/Teacher) role-guarded + 403 IT
- [x] EXEMPT controllers documented (webhook/internal/public)

## Resolution (2026-06-21)

**SecurityConfig baseline audited:** `kiteclass-core/config/SecurityConfig.java` uses
`.anyRequest().permitAll()` at the URL layer — ALL authorization is delegated to the method
layer via `@PreAuthorize` (`@EnableMethodSecurity(prePostEnabled=true)` already on, `@Profile("!test")`).
So a controller with "0 @PreAuthorize" was effectively **wide open** to any role that cleared the
gateway → privilege escalation confirmed. Fix = add method-level role guards (defense-in-depth + the
actual gate, given the permitAll URL layer). Role names mirror the existing `InvoiceController`
(GAP-1005) convention — `hasAnyRole('ADMIN','OWNER','PLATFORM_ADMIN','STAFF')` etc — bridged from the
gateway `X-User-Roles` header to `ROLE_*` authorities by `GatewayHeaderAuthenticationFilter`.

**8/8 controllers guarded (38 mappings now role-restricted):**

| Controller | Mutation tier | Read tier |
|---|---|---|
| `PaymentController` (8) | `ADMIN,OWNER,PLATFORM_ADMIN,STAFF` | + `TEACHER` |
| `RefundRequestController` (5) | `ADMIN,OWNER,PLATFORM_ADMIN,STAFF` | + `TEACHER` |
| `InstallmentPlanController` (5) | `ADMIN,OWNER,PLATFORM_ADMIN,STAFF` | + `TEACHER` |
| `CourseController` (11) | `TEACHER,ADMIN,OWNER,PLATFORM_ADMIN,STAFF` | `permitAll()` — reads are the **public catalog** (anonymous browse + sitemap via `kiteclass-frontend/src/lib/api/public.ts`); annotated explicitly so it is not silently unguarded |
| `InstanceController` (8) | `PLATFORM_ADMIN,ADMIN,OWNER` (class-level) | `list()` tightened to `PLATFORM_ADMIN,ADMIN` (cross-tenant table) |
| `SubjectGradeController` (4) | submit/review = teacher tier; publish/bulk = principal tier | n/a |
| `StudentController` (5 new) | `OWNER,ADMIN,PRINCIPAL,TEACHER,STAFF,PLATFORM_ADMIN` | same |
| `TeacherController` (6 new) | HR mutations = `OWNER,ADMIN,PRINCIPAL,PLATFORM_ADMIN` (excludes TEACHER) | reads add `TEACHER,STAFF` |

**Conservative/notable role decisions** (per "guard conservatively when uncertain"):
- `RefundRequest`/`InstallmentPlan` `create`/`request` → staff tier (no parent-facing FE flow exists; a
  customer-request endpoint, if needed later, would be separate).
- `Course` reads kept `permitAll()` (would otherwise break anonymous catalog/landing/sitemap).
- `Instance` lifecycle → platform-admin + owner (AI-branding is mock in Phase 1, no real S2S caller);
  cross-tenant `list()` is platform-admin only.
- `Teacher` mutations exclude `TEACHER` (no lateral self-management).

**EXEMPT (unchanged):** `PaymentWebhookController` (signature auth), `Internal*Controller` (gateway-blocked
S2S), `AuthController` (login), `Public*Controller`/`LandingPageController`, and the `ParentPayment`/parent
controllers (already use `@authz.hasAccessTo*` per-resource SpEL).

**Tests:** 8 new `*AuthzTest` web-slice classes (`@WebMvcTest` + in-slice `@EnableMethodSecurity`, mirroring
`InvoiceControllerAuthzTest`) — 26 tests, allow-right-role + deny-low-priv (STUDENT/PARENT) per controller.
All green. No regression to existing tests: the app's `test`-profile `TestSecurityConfig` does **not** enable
method security, so `@PreAuthorize` is a no-op there; production (`!test`) enforces it.

**Local verify:** `cd kiteclass/kiteclass-core && ./mvnw -o surefire:test -Dtest='*AuthzTest'` → 26/26 PASS;
`./mvnw -o compile -P strict-warnings` → BUILD SUCCESS; `./mvnw -o test-compile` → SUCCESS.

## Related

- Parent sweep: GAP-1005 (InvoiceController @PreAuthorize — PARTIAL Wave close-2 SEC)
- Same class: GAP-999 (hasAccessToGrade), GAP-1000 (finalize JWT-derived) — SpEL authz pattern reference
- OWASP A01 broken function-level authz; `pre-launch-auth-hardening-checklist`
- Follow-up (not blocking): fine-grained per-resource ownership on `SubjectGrade` (Tổ trưởng-per-subject vs
  Hiệu trưởng) remains GAP-058/360.2 scope; `Instance` owner-self-service vs platform split could be revisited
  if a real branding S2S caller lands.
