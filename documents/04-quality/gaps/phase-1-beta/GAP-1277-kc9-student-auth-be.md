# GAP-1277: KC-9 student-auth (BE) — STUDENT KC-native login provisioning + login

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (Wave rbac-lms-kc9-staff — KC-9 student-auth, pulls forward the STUDENT auth path)
**Affects:** `kiteclass/kiteclass-core` — `module/student` + `module/auth` (login already role-agnostic)

## Problem

KC-9 student-auth was the last role gated in the 5-template RBAC shell (GAP-1119): parent + teacher KC-native login shipped Wave auth-1 (Option B, PR #2186) but STUDENT login was never wired, blocking the student-shell. The auth infra already fully *supports* STUDENT — `V89 auth_credentials` CHECK allows `entity_type IN ('PARENT','TEACHER','STUDENT')` (V89:22), `AuthCredentialProvisioningService` has `ROLE_STUDENT`, and `AuthService`/`AuthTokenService` are role-agnostic — but there was no **entry point** to provision a STUDENT credential, and no test proving the STUDENT login round-trips.

Per the locked decision (memory `project_parent_student_portal_phase2_gated`): student auth is password-based KC-native (Option B), **NO Zalo/SMS OTP** — built exactly like the shipped parent/teacher path.

## Resolution (BE — this PR)

Mirrors the teacher provisioning path 1:1:

- **Provisioning:** `StudentService.provisionCredential(studentId, rawPassword)` + `StudentServiceImpl` — finds the student tenant-scoped, guards null/blank email (`STUDENT_EMAIL_REQUIRED` 400 — login is email-keyed), calls `credentialProvisioning.setPassword(ROLE_STUDENT, student.id, student.email, tenant, rawPassword)` (UPSERT, idempotent rotate).
- **Endpoint:** `POST /api/v1/students/{id}/credentials` (`@PreAuthorize("hasAnyRole('OWNER','ADMIN','PRINCIPAL','TEACHER')")`) — owner/teacher provisions; mirrors `POST /api/v1/teachers/{id}/credentials`.
- **Soft-delete parity (GAP-1013b):** `deleteStudent` → `credentialProvisioning.disableCredential(ROLE_STUDENT, id)` revokes login.
- **Login:** unchanged — STUDENT uses the shared `POST /api/v1/tenant-auth/login` (entity_type=STUDENT valid in V89 CHECK; `AuthService`/`AuthTokenService` role-agnostic) → JWT `role=STUDENT` + `referenceId` + `tenantId`.
- **RLS preserved:** all student lookups go through tenant-scoped repository methods + Hibernate `@Filter`.
- **Doc:** `documents/01-business/kiteclass/tenant-auth/api-contract.md` §2b + endpoint index updated (same PR).

### Tests (Testcontainers real Postgres + Mockito)

- `StudentAuthFlowIT` (Testcontainers, real Flyway V89) — provision STUDENT → login happy (JWT `role=STUDENT`, referenceId, tenant) → wrong password 401 → STUDENT entity_type round-trips the real V89 CHECK. **3/3 PASS.**
- `StudentServiceTest` (Mockito) — provisionCredential (sets STUDENT password) + not-found (404) + no-email (400 `STUDENT_EMAIL_REQUIRED`) + delete→disableCredential. **13/13 PASS.**

## Acceptance Criteria

- [x] Owner/teacher can provision a STUDENT credential (entity_type=STUDENT, entity_id=students.id) + set-password — `POST /api/v1/students/{id}/credentials`
- [x] KC-native login accepts STUDENT entity_type → JWT with `role=STUDENT` + correct tenantId (shared `/api/v1/tenant-auth/login`)
- [x] Tests (Testcontainers): provision → login happy → JWT STUDENT role; wrong password → 401; STUDENT entity_type round-trip
- [x] 3-layer doc updated (`tenant-auth/api-contract.md`) same PR
- [x] **(Wave flow-kc3)** Bulk auto-provision — `POST /api/v1/students` + `initialPassword` (per-create) AND `POST /api/v1/students/bulk-import/commit` + batch `initialPassword` (every created student with email) auto-provision login; `BulkImportResult.credentialsProvisioned` count; invalid batch password → 400 `BULK_IMPORT_INVALID_PASSWORD`
- [ ] **FE student-shell** (login→role-redirect, student dashboard, lesson player) — later FE batch (GAP-1119 (a)/(b) student-shell scaffold; LMS surfaces GAP-1113). **This is why the gap stays PARTIAL** (BE done; FE student-shell not in this wave).

## Related

- Wave: `wave/rbac-lms-kc9-staff` (this PR)
- GAP-1119 (RBAC shell — student gated KC-9) — this closes the BE half of the student gate
- GAP-1113 (FE LMS headless — student lesson player cắm lên student-shell)
- Wave auth-1 PR #2186 (parent/teacher KC-native login — pattern mirrored here)
- Memory `project_parent_student_portal_phase2_gated` (student auth Option B, no OTP)
- V89 `auth_credentials` (entity_type CHECK already allows STUDENT)

## G1 runtime walk (2026-06-14) — gateway BE-contract: ✅ PASS (status giữ PARTIAL)

Per `documents/04-quality/audits/rst-html/2026-06-14-g1-runtime-walk-rbac-lms.md`. STUDENT token (role=STUDENT, referenceId=students.id) qua gateway `:9000` HS512 chain: `/enrollments/me` 200 (self-scoped); LMS student-area (sau fix GAP-1297) lesson-player + progress 200; cross-role 403 (TEACHER→/me). Cross-student isolation: student 4 (class 14) vs student 5 (class 6) = tập enrollment disjoint. **Còn lại G2★ human:** browser-walk student-shell login + `/student/*` trên FE `:3000` (KC-native login + student-shell FE). KHÔNG flip DONE.

## Wave flow-kc3 — bulk auto-provision-on-create/import (2026-06-17)

Extends the per-entity `POST .../credentials` provisioning (this gap's original BE) with **opt-in auto-provision lúc tạo / import** — owner/teacher không phải gọi 2 lần (create → set-password):

- **Per-create:** `CreateStudentRequest.initialPassword` (optional) → `StudentServiceImpl.createStudent` provision `auth_credentials` (entity_type=STUDENT) cùng transaction khi present. No-email + password → 400 `STUDENT_EMAIL_REQUIRED` (fail loud). Convenience ctor giữ ~22 call-site cũ compile (initialPassword=null → no-provision, design preserved).
- **Bulk-import:** `POST /api/v1/students/bulk-import/commit` + batch form field `initialPassword` → mỗi học sinh tạo thành công CÓ email được auto-provision. `BulkImportResult.credentialsProvisioned` = count (≤ successCount). Validate batch password 1 lần (`AuthPasswordPolicy`) → invalid ⇒ 400 `BULK_IMPORT_INVALID_PASSWORD` trước mọi DB write. Provision-fail 1 dòng → row error field `credential`, KHÔNG hủy create + KHÔNG abort chunk.
- **Tests:** `StudentServiceTest` 16/16 (provision + no-provision + no-email-throws), `StudentBulkImportServiceTest` 12/12 (bulk provision N + invalid-password-400 + no-password-0), `RowValidatorTest` 13/13 (7-arg ctor parity), `InternalStudentControllerTest` 5/5 (Jackson canonical-ctor body-binding intact). 3-layer doc: `tenant-auth/api-contract.md` §2c + `student-enrollment/api-contract.md` create + bulk-import commit.
- **Status:** giữ 🟡 PARTIAL — FE student-shell vẫn defer (như AC cuối). **Phase-2 enhancement** (NGOÀI scope): random-per-student password + force-reset-on-first-login.

## G1-FE browser walk note (2026-06-14)

G1-FE PASS: STUDENT login `/api/v1/tenant-auth/login` → JWT role=STUDENT → redirect `/student/today` (student-shell render). Provision student credential (entity_id=164 skytest) verified. — verified qua Playwright headless trên FE thật `skytest.127.0.0.1.nip.io:3000` (rebuild kiteclass-frontend). Evidence: `documents/04-quality/audits/rst-html/2026-06-14-g1-fe-browser-walk.md`. **Giữ PARTIAL** — human G2★ vẫn bắt buộc (mutation deep-interaction chưa walk).
