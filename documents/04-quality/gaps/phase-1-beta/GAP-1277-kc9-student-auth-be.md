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
- [ ] **FE student-shell** (login→role-redirect, student dashboard, lesson player) — later FE batch (GAP-1119 (a)/(b) student-shell scaffold; LMS surfaces GAP-1113). **This is why the gap stays PARTIAL** (BE done; FE student-shell not in this wave).

## Related

- Wave: `wave/rbac-lms-kc9-staff` (this PR)
- GAP-1119 (RBAC shell — student gated KC-9) — this closes the BE half of the student gate
- GAP-1113 (FE LMS headless — student lesson player cắm lên student-shell)
- Wave auth-1 PR #2186 (parent/teacher KC-native login — pattern mirrored here)
- Memory `project_parent_student_portal_phase2_gated` (student auth Option B, no OTP)
- V89 `auth_credentials` (entity_type CHECK already allows STUDENT)
