# Child Protection — Use Cases

**Domain:** KiteClass Core / Compliance / Safeguarding
**Version:** 0.2 (Phase 1A + Phase 1B foundation)
**Created:** 2026-05-04
**Last-Reviewed:** 2026-05-04

> Phase 1A documents skeleton use cases (UC-CHILD-PROT-001..004 — Incident CRUD foundation). Use cases UC-CHILD-PROT-005..010 (state machine, RBAC-gated decryption, mandatory reporting, audit log, retention) are placeholder-defined here so Phase 1B/1C have a target to implement against.

---

## UC-CHILD-PROT-001 — Reporter submits incident

**Actor:** PH / HS / GV / staff (any tenant user with `INCIDENT_REPORT` permission). Phase 1A grants broadly; Phase 1B refines.

**Trigger:** Reporter observes / experiences a child-protection concern (bullying, abuse, grooming, CSAM, other).

**Preconditions:**
- Reporter authenticated; `tenant.vertical_type` is set
- (Phase 1B) Reporter has been shown anonymous-reporting opt-in choice

**Main flow:**

1. Reporter opens the "Báo cáo sự cố" form (Phase 1B FE; Phase 1A: API only)
2. Reporter enters:
   - Title (≤200 chars; coached to NOT include names)
   - Severity (`LOW` / `MEDIUM` / `HIGH` / `CRITICAL`)
   - Category (`BULLYING` / `ABUSE` / `GROOMING` / `CSAM` / `OTHER`)
   - Description (sensitive narrative, free text)
   - Optional: subject student id (autocomplete from class roster)
   - Optional: evidence file uploads (Phase 1B; encrypted MinIO)
3. Reporter submits.
4. Service `IncidentService.create(...)` validates + persists with `status=REPORTED`. Sensitive fields encrypted via `AesGcmAttributeConverter`.
5. (Phase 1B) System notifies on-duty `SAFEGUARDING_OFFICER` via in-app + email.
6. (Phase 1C) If `severity=CRITICAL` + `category ∈ {ABUSE, GROOMING, CSAM}`, system shows banner: "Luật Trẻ em 2016 Đ.51 — báo cáo Tổng đài 111 + công an địa phương ≤24h".

**Postconditions:**
- Row exists in `incidents` table with `status=REPORTED`, `description` + `evidence_paths` columns BYTEA-encrypted
- Audit log entry written (Phase 1C)

**Errors:**
- `VALIDATION_TITLE_REQUIRED` — title null or blank
- `VALIDATION_TITLE_TOO_LONG` — title >200 chars
- `VALIDATION_SEVERITY_REQUIRED` — severity null
- `VALIDATION_CATEGORY_REQUIRED` — category null
- `VALIDATION_REPORTER_REQUIRED` — reporter user id null

**Business rules invoked:** BR-CHILD-PROT-001, -002, -005, -006, -008.

---

## UC-CHILD-PROT-002 — Officer views incident list

**Actor:** `SAFEGUARDING_OFFICER` (Phase 1B+); Phase 1A: any tenant user.

**Trigger:** Officer opens dashboard or filters by severity/category/status.

**Main flow:**

1. Officer requests incident list, optionally filtered.
2. Service `IncidentService.findAll(severity, category, status, pageable)` returns paged results.
3. Each row's encrypted columns are decrypted by `AesGcmAttributeConverter` on read (Phase 1A: any caller can decrypt; Phase 1B: RBAC-gated).
4. UI renders list view with title (plaintext), severity badge, category, status, reporter id, timestamp.

**Postconditions:** No state change.

**Errors:** Tenant-filter mismatch returns empty list (silent — by design, no information leakage).

**Business rules invoked:** BR-CHILD-PROT-001, -002, -005, -009 (Phase 1B).

---

## UC-CHILD-PROT-003 — Officer reads single incident detail

**Actor:** `SAFEGUARDING_OFFICER` + `PRINCIPAL` + `COUNSELOR` (Phase 1B+); Phase 1A: any tenant user.

**Trigger:** Officer clicks an incident in the list.

**Main flow:**

1. Officer requests `GET /api/v1/incidents/{id}`.
2. Service `IncidentService.findById(id)` looks up + (Phase 1B) verifies caller has `INCIDENT_READ_DECRYPTED` permission.
3. Encrypted columns decrypted by `AesGcmAttributeConverter`.
4. Service returns full record including plaintext `description` + `evidence_paths`.
5. (Phase 1C) Audit log entry: `INCIDENT_READ_DECRYPTED` by `userId` at timestamp.

**Postconditions:** Audit log entry (Phase 1C).

**Errors:**
- `INCIDENT_NOT_FOUND` — id missing or soft-deleted
- `FORBIDDEN_DECRYPT` (Phase 1B) — caller lacks permission

**Business rules invoked:** BR-CHILD-PROT-001, -002, -003 (tamper rejection), -009 (Phase 1B), -011 (Phase 1C).

---

## UC-CHILD-PROT-004 — Officer assigns themselves / advances status

**Actor:** `SAFEGUARDING_OFFICER`.

**Trigger:** Officer takes ownership or transitions status.

**Main flow:**

1. Officer calls `PUT /api/v1/incidents/{id}/officer { officerUserId }` to assign.
2. Officer calls `PUT /api/v1/incidents/{id}/status { newStatus }` to transition.
3. Phase 1A: any non-null status accepted.
4. Phase 1B: state-machine validation per BR-CHILD-PROT-006:
   - `REPORTED → INVESTIGATING`
   - `INVESTIGATING → ESCALATED | RESOLVED`
   - `ESCALATED → RESOLVED`
   - `RESOLVED → CLOSED`

**Postconditions:**
- Row updated, audit log entry written (Phase 1C)
- If new status is `ESCALATED` (Phase 1C), Đ.51 banner refreshed for officer

**Errors:**
- `INVALID_STATUS_TRANSITION` (Phase 1B) — illegal transition
- `INCIDENT_NOT_FOUND`

**Business rules invoked:** BR-CHILD-PROT-006, -011 (Phase 1C).

---

## UC-CHILD-PROT-005 — State machine enforcement (PHASE 1B placeholder)

**Phase 1B (GAP-322b) implements full state machine.** Phase 1A allows free transitions to enable skeleton-testing.

---

## UC-CHILD-PROT-006 — Vetting evidence upload + verify (PHASE 1B placeholder)

**Phase 1B (GAP-322b) implements:**
- HR uploads xlsx + zip per teacher (CCCD scan + bằng tốt nghiệp + LLTP số 2 ≤6 tháng + ảnh 3×4)
- Encrypted MinIO bucket `staff-vetting-evidence/{tenantId}/{userId}/`
- Admin-Kite verify queue: `pending → verified | rejected`
- RBAC-gate teacher access to student PII until `verified=true`
- Annual reminder + 2-year LLTP refresh

---

## UC-CHILD-PROT-007 — Mandatory reporting banner (PHASE 1C placeholder)

**Phase 1C (GAP-322c) implements:**
- When `severity=CRITICAL` + `category ∈ {ABUSE, GROOMING, CSAM}`, banner shown: "Luật Trẻ em 2016 Đ.51 — báo cáo Tổng đài 111 + công an địa phương ≤24h"
- Banner persists until officer marks `escalation_acknowledged=true`
- (Stage 2 Q4 2026) PDF export + Tổng đài 111 webhook

---

## UC-CHILD-PROT-008 — Hash-chained audit log (PHASE 1C placeholder)

**Phase 1C (GAP-322c) implements:**
- Every CRUD on Incident emits an immutable audit log entry
- Each entry's `hash = SHA-256(prev_entry_hash || entry_payload)`
- Admin CANNOT delete or modify audit entries (DB trigger + RBAC)

---

## UC-CHILD-PROT-009 — 7-year retention enforcement (PHASE 1C placeholder)

**Phase 1C (GAP-322c) implements:**
- `status=CLOSED` incidents retained 7 years from `closed_at` timestamp
- Delete-protection at service layer (`softDelete()` rejects if `status=CLOSED` and age < 7y)
- DB trigger as belt-and-suspenders enforcement
- After 7y: archived to cold storage, master row remains in DB with redacted ciphertext

---

## UC-CHILD-PROT-010 — Critical-CSAM no-delete (PHASE 1C placeholder)

**Phase 1C (GAP-322c) implements:** `severity=CRITICAL` + `category=CSAM` rows can NEVER be soft-deleted; service rejects with `FORBIDDEN_DELETE_CSAM`.

---

## Vetting workflow use cases (Phase 1B foundation — GAP-322b, Wave 18b2 Bucket B)

> Phase 1B foundation ships service + endpoints + state machine + storage contract + RBAC gate. UI (LLTP upload form, verify queue table) is deferred to Phase 1B follow-up. UCs below describe the actor flows the foundation enables.

---

## UC-VETTING-001 — Officer creates a vetting record (PENDING)

**Actor:** `SAFEGUARDING_OFFICER` (BR-VETTING-003).

**Trigger:** Onboarding a new teacher; HR/officer initiates the vetting workflow.

**Preconditions:**
- Caller authenticated; `X-User-Roles` header includes `SAFEGUARDING_OFFICER`.
- Teacher record exists in `users` (FK to `teacher_id`).

**Main flow:**
1. Officer calls `POST /api/v1/vettings` with `{ teacherId, lltpNumber?, policeCheckDetails?, expiresAt? }`.
2. Service `VettingService.create(...)` validates `teacherId`; encrypted fields persisted via `AesGcmAttributeConverter` (BR-VETTING-002).
3. Record persisted with `status=PENDING` (default per BR-VETTING-001).
4. Response 201 with `VettingResponse` body.

**FE behaviour (Phase 1B follow-up):** "Add new vetting" form pre-fills `teacherId` from picker; `lltpNumber` / `policeCheckDetails` optional at this stage (uploaded later by HR); after success, navigates to detail view in PENDING state.

**Errors:** `400 VETTING_TEACHER_ID_REQUIRED`, `403 VETTING_RBAC_DENIED`.

**Business rules invoked:** BR-VETTING-001, -002, -003, -005.

---

## UC-VETTING-002 — Submit documents (PENDING → SUBMITTED)

**Actor:** `SAFEGUARDING_OFFICER`.

**Trigger:** HR uploads LLTP số 2 + bằng tốt nghiệp + CCCD scan + ảnh 3×4; officer reviews completeness and confirms submission.

**Main flow:**
1. (Phase 1B follow-up) HR/officer uploads documents via storage endpoint (deferred — uses `VettingDocumentStorage` stub today).
2. Officer calls `PATCH /api/v1/vettings/{id}/transition` with `{ targetStatus: "SUBMITTED" }`.
3. Service validates current state is `PENDING`; sets `submittedAt = now()`.
4. Response 200 with updated record.

**FE behaviour (Phase 1B follow-up):** "Submit for review" button visible only when status is PENDING; disabled until at least LLTP file present.

**Errors:** `400 VETTING_INVALID_TRANSITION`, `404 VETTING_NOT_FOUND`, `403 VETTING_RBAC_DENIED`.

**Business rules invoked:** BR-VETTING-001, -003, -004 (storage stub).

---

## UC-VETTING-003 — Mark interview done (SUBMITTED → INTERVIEW_DONE)

**Actor:** `SAFEGUARDING_OFFICER`.

**Trigger:** Officer has conducted the in-person interview and is ready to record outcome.

**Main flow:**
1. Officer calls `PATCH /api/v1/vettings/{id}/transition` with `{ targetStatus: "INTERVIEW_DONE" }`.
2. Service validates current state is `SUBMITTED`; sets `interviewedAt = now()`.
3. Response 200 with updated record.

**FE behaviour (Phase 1B follow-up):** "Mark interview done" button visible only when status is SUBMITTED; opens an interview-notes modal that PATCHes both notes (encrypted) and target status in one call (Phase 1B follow-up — current foundation only stamps the timestamp).

**Errors:** `400 VETTING_INVALID_TRANSITION`, `404 VETTING_NOT_FOUND`, `403 VETTING_RBAC_DENIED`.

---

## UC-VETTING-004 — Approve or Reject (INTERVIEW_DONE → APPROVED | REJECTED)

**Actor:** `SAFEGUARDING_OFFICER`.

**Trigger:** Officer has reviewed interview + documents and is making a final decision.

**Main flow:**
1. Officer calls `PATCH /api/v1/vettings/{id}/transition` with `{ targetStatus: "APPROVED" }` or `"REJECTED"`.
2. Service validates current state is `INTERVIEW_DONE`; records `decidedAt = now()` and `decidedByUserId` from the `X-User-Reference-Id` header.
3. Response 200 with updated record.

**FE behaviour (Phase 1B follow-up):** Approve / Reject buttons visible only on INTERVIEW_DONE rows; both prompt for confirmation; Reject prompts for required reason (encrypted into `policeCheckDetails`).

**Postconditions:** Teacher's RBAC filter (Phase 1B follow-up) consults the `findLatestForTeacher` result on each student-PII request — APPROVED unblocks access; REJECTED keeps it blocked. EXPIRED arrives via cron (Phase 1B follow-up) when `now() > expires_at`.

**Errors:** `400 VETTING_INVALID_TRANSITION`, `404 VETTING_NOT_FOUND`, `403 VETTING_RBAC_DENIED`.

---

## UC-VETTING-005 — Expire an approved record (APPROVED → EXPIRED)

**Actor:** System cron (Phase 1B follow-up); manual override available to `SAFEGUARDING_OFFICER` for tests.

**Trigger:** `now() > vettings.expires_at` while status is APPROVED. Per Decree 56/2017 procedural standard, LLTP must be refreshed ≤2 years.

**Main flow:**
1. Cron iterates `findByFilters(status=APPROVED)` filtering by `expiresAt < now()` (Phase 1B follow-up).
2. For each, calls `transition(id, EXPIRED, null)`.
3. Teacher's RBAC filter (Phase 1B follow-up) re-blocks student-PII access on next request.

**FE behaviour:** No direct UI; the verify queue (Phase 1B follow-up) shows EXPIRED records with a "Re-vet" CTA that creates a new PENDING record for the same teacher.

---

## Log

- **2026-05-04** (v0.2): Phase 1B foundation — vetting workflow UCs UC-VETTING-001..005 added (sister of Phase 1A UC-CHILD-PROT-001..010). Wave 18b2 Bucket B (GAP-322b). UI (upload form, verify queue, RBAC filter) deferred to Phase 1B follow-up; foundation ships service + endpoints + state machine + storage contract.
- **2026-05-04** (v0.1): Phase 1A skeleton use cases UC-CHILD-PROT-001..004 + 005..010 placeholders. Wave 18b1 Bucket E.
