# Parent Portal — Use Cases

**Domain:** KiteClass Core / Parent Portal
**Version:** 1.0 (Wave 2 MVP)
**Updated:** 2026-04-19

---

## Use Cases

### UC-PARENT-01: Admin/Teacher Invite Parent

**Actor:** Admin / Teacher (role check ở Gateway)
**Precondition:**
- User authenticated, có quyền trên tenant
- Feature flag `kiteclass.parent-portal.enabled = true` (BR-PARENT-004)
- Student tồn tại và chưa soft-deleted trong tenant

**Steps:**
1. FE (admin): Mở student detail → click "Mời phụ huynh" → input email
2. FE → Gateway: `POST /api/v1/parent-invitations` với `{studentId, parentEmail}`, kèm Bearer token
3. Gateway: Forward request kèm `X-User-Id` (inviter), `X-Tenant-Id`
4. Core: Validate `enabled=true` (BR-PARENT-004), nếu false → `503 PARENT_PORTAL_DISABLED`
5. Core: Load student bằng `findByIdAndDeletedFalse` — thiếu → `404 STUDENT_NOT_FOUND`
6. Core: Check `parentRepository.existsByEmailAndInstanceIdAndDeletedFalse(email, tenantId)` — đã có Parent ACTIVE → `409 PARENT_EMAIL_EXISTS` (BR-PARENT-INV-002)
7. Core: Tạo `ParentInvitation` (token UUID 128-bit, status PENDING, expiresAt = now + 24h, invitedByUserId)
8. Core: Publish `ParentInvitationEmailEvent` lên RabbitMQ best-effort (BR-PARENT-INV-006); failure log warn nhưng vẫn return 201
9. Core → FE: Trả `ParentInvitationResponse` kèm `token` (chỉ ở response create — BR-PARENT-INV-008)
10. FE: Toast "Lời mời đã được gửi", hiển thị link copy (cho trường hợp email worker down)

**Postcondition:**
- Row `parent_invitations` mới với status=PENDING
- Email enqueued (best-effort)
- Admin UI hiển thị invitation status trong list

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 401 | `X-User-Id` thiếu | "AUTH_REQUIRED" |
| 503 | Feature flag tắt | "PARENT_PORTAL_DISABLED" |
| 404 | Student không tồn tại | "STUDENT_NOT_FOUND" |
| 409 | Parent ACTIVE đã có cho email | "PARENT_EMAIL_EXISTS" |
| 400 | Email/studentId validation | "Email là bắt buộc" / "studentId là bắt buộc" |

---

### UC-PARENT-02: Parent Redeem Invitation Token

**Actor:** Phụ huynh (chưa có tài khoản)
**Precondition:**
- Phụ huynh nhận email với link `https://{tenant}.kitehub.me/parent-invite/{token}`
- Token còn hạn (status PENDING, `expiresAt > now`)

**Steps:**
1. Phụ huynh: Click link trong email → FE route `/parent-invite/[token]`
2. FE: Hiển thị form (password, fullName, phoneNumber optional, relationship)
3. Phụ huynh: Submit form
4. FE → Gateway: `POST /api/v1/auth/register-parent/{token}` với body `RedeemInvitationRequest`
5. Gateway: Forward → Core `POST /api/v1/parent-invitations/redeem/{token}` kèm `X-Tenant-Id`
6. Core: Load invitation bằng `findByTokenAndDeletedFalse` — thiếu → `404 PARENT_INVITATION_NOT_FOUND`
7. Core: Check `invitation.instanceId == tenantId` — không khớp → `404` (BR-PARENT-INV-010, defense in depth)
8. Core: Check status — không phải PENDING → `400 PARENT_INVITATION_ALREADY_USED`
9. Core: Check `expiresAt > now` — quá hạn → set status EXPIRED, save, `400 PARENT_INVITATION_EXPIRED`
10. Core: Lookup Parent bằng email + tenantId
    - **Nếu chưa có:** `createParent(...)` với status ACTIVE
    - **Nếu PENDING:** promote ACTIVE + ghi đè profile từ request (BR-PARENT-INV-005)
    - **Nếu ACTIVE:** giữ nguyên profile (BR-PARENT-INV-004 — child #2 cùng parent)
11. Core: Tạo `ParentStudentLink` nếu chưa có cho `(parent, student)` với linkType=PRIMARY (idempotent, BR-PARENT-LINK-002)
12. Core: Set invitation status REDEEMED, redeemedAt, redeemedParentId
13. Core → Gateway: Trả `RedeemInvitationResult { parentId, email, fullName, phoneNumber, relationship, linkedStudentIds }`
14. Core (Wave auth-1, Option B): `AuthCredentialProvisioningService.provisionParent` tạo `auth_credentials` row (entity_type=PARENT, entity_id=parentId, email=invitation.email, password BCrypt) idempotent-on-email, atomic trong redeem txn. **Superseded Option A:** Gateway tạo `users` row + mint JWT. Giờ parent login qua `POST /api/v1/tenant-auth/login` (xem `tenant-auth/use-cases.md` UC-AUTH-01/03).
15. FE: Redirect trang login portal phụ huynh; parent đăng nhập bằng email + password vừa đặt

**Postcondition:**
- Parent ACTIVE trong tenant
- Có `ParentStudentLink` cho student tương ứng
- Invitation REDEEMED
- (Wave 5) Gateway User tồn tại, JWT issued

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 503 | Feature flag tắt | "PARENT_PORTAL_DISABLED" |
| 404 | Token không tồn tại / cross-tenant | "PARENT_INVITATION_NOT_FOUND" |
| 400 | Token đã dùng (REDEEMED/EXPIRED/REVOKED) | "PARENT_INVITATION_ALREADY_USED" |
| 400 | Token quá hạn | "PARENT_INVITATION_EXPIRED" |
| 400 | Password yếu | "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt" |
| 400 | Tên < 2 hoặc > 100 ký tự | "Tên phải từ 2-100 ký tự" |
| 400 | Phone sai format | "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)" |
| 400 | Relationship không hợp lệ | "Quan hệ phải là FATHER, MOTHER hoặc GUARDIAN" |

**FE behavior:**
- Side-effect khi token đã dùng: hiển thị CTA "Quay lại trang đăng nhập" thay vì retry
- Side-effect khi expired: hiển thị CTA "Liên hệ giáo viên để xin link mới"

---

### UC-PARENT-03: Parent Xem Profile Của Mình

**Actor:** Parent đã ACTIVE và đăng nhập
**Precondition:** KC-native JWT hợp lệ (role=PARENT, claim `referenceId`), Gateway re-inject `X-User-Reference-Id` từ verified claim (Option B)

**Steps:**
1. FE (parent dashboard): Sau login, fetch `GET /api/v1/parent/me`
2. Gateway: Strip client `X-User-Reference-Id` → re-inject `= referenceId claim` (= `auth_credentials.entity_id`)
3. Core: `parentService.getParentById(parentId)` (read-only txn)
4. Core: Load Parent bằng `findByIdAndDeletedFalse` — thiếu → `404 PARENT_NOT_FOUND`
5. Core: Map sang `ParentResponse` (id, fullName, email, phoneNumber, relationship, status)
6. FE: Render profile card

**Postcondition:** Profile hiển thị

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 401 | `X-User-Reference-Id` thiếu | "AUTH_REQUIRED" |
| 404 | Parent không tồn tại / cross-tenant / soft-deleted | "PARENT_NOT_FOUND" |

---

### UC-PARENT-04: Parent Xem Danh Sách Children

**Actor:** Parent đã ACTIVE
**Precondition:** Parent có ≥1 `ParentStudentLink` trong tenant hiện tại

**Steps:**
1. FE: Fetch `GET /api/v1/parent/me/children`
2. Gateway: Forward kèm `X-User-Reference-Id`
3. Core: Load Parent (assert tenant scope, BR-PARENT-AUTH-004) — thiếu → `404 PARENT_NOT_FOUND`
4. Core: Query `linkRepository.findByParentIdWithStudent(parentId)` (JOIN FETCH student để tránh N+1)
5. Core: Map sang `List<ChildSummaryResponse>` — `className` + `grade` để null (Wave 5 sẽ enrich)
6. FE: Render list children, mỗi card có name + linkType badge (PRIMARY/SECONDARY)

**Postcondition:** List children hiển thị (có thể rỗng nếu chưa link)

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 401 | Header thiếu | "AUTH_REQUIRED" |
| 404 | Parent không thuộc tenant | "PARENT_NOT_FOUND" |

**FE behavior:**
- Empty state: "Chưa có học sinh nào được liên kết. Liên hệ giáo viên/admin để được mời."
- `className/grade = null`: hiển thị placeholder "—" (Wave 5 sẽ populate)

---

### UC-PARENT-05: Hourly Sweep Expire Pending Invitations

**Actor:** System scheduled job (`scheduledExpireStale`)
**Precondition:** Feature flag `enabled=true` (BR-PARENT-004); else early return

**Steps:**
1. Trigger: `@Scheduled(fixedRateString = "${kiteclass.parent-portal.expire-sweep-ms:3600000}")` mỗi giờ
2. Service: Check `properties.enabled()` — false → return (BR-PARENT-INV-007)
3. Service: Query `findByStatusAndExpiresAtBeforeAndDeletedFalse(PENDING, now)`
4. Service: Cho mỗi row → set status EXPIRED
5. Service: `saveAll(stale)`, log info `"Expired N stale parent invitations"`
6. Service: Catch + log error nếu exception (sweeper không được fail silent)

**Postcondition:** Tất cả PENDING invitations quá hạn chuyển EXPIRED

**Errors:** Internal — không trả ra API caller. Errors log ở mức ERROR.

**Note:** Single-instance MVP. Wave 5 sẽ wire scheduler-lock cho multi-instance deployment.

---

### UC-PARENT-06: Parent JWT Enrichment (internal)

> **Note (Wave 96, ADR-032):** `kiteclass-gateway` đã removed; auth + JWT issuance chuyển vào `kiteclass-core` trực tiếp. HMAC internal-request layer cũ (gateway↔core) loại bỏ. Use case này giờ là internal-call trong `kiteclass-core` auth module.

**Actor:** `kiteclass-core` auth module (internal)
**Precondition:** Parent đã authenticate bằng password trong login flow

**Steps:**
1. Login flow: Core auth module authenticate parent bằng password
2. Auth module: Cần `linked_student_ids` cho JWT claim + display profile
3. Auth module → Parent service (in-process): Load Parent → load `linkRepository.findStudentIdsByParentId(parentId)`
4. Parent service trả `ParentInternalResponse { id, email, fullName, phoneNumber, relationship, status, linkedStudentIds }`
5. Auth module: Mint JWT với `linked_student_ids`, populate `LoginResponse.profile = ParentProfileResponse`

**Postcondition:** JWT issued cho parent kèm linked_student_ids claim

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Parent không tồn tại | "PARENT_NOT_FOUND" |
| 500 | Load failure | "INTERNAL_ERROR" |

**Hidden từ public Swagger** (`@Hidden`).

---

## Cross-cutting Behaviors

### Tenant Filter
Tất cả read transaction (`@Transactional(readOnly = true)`) tự động kích hoạt Hibernate `tenantFilter` (BR-PARENT-005). Caller không cần truyền `instance_id` — interceptor lấy từ `TenantContext`.

### Soft Delete
Repository methods chỉ trả `deletedFalse = true`. Không có hard delete trong domain này.

### Audit Fields
`BaseEntity` cung cấp `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`, `instanceId` cho mọi entity.

---

## K-12 LEGAL Phase 1A Use Cases (Wave 18b1 Bucket D — GAP-321)

Phase 1A use cases extend the Wave 2 GAP-052a flows (above) with the **transcript read-only** facet for K-12 parent portal compliance with Luật Giáo dục 2019 Đ.83 K2 + PDPL Decree 13/2023 Art 16.

### UC-PARENT-PORTAL-01: Phụ huynh xem học bạ con

**Actor:** Phụ huynh (PARENT user, đã được mời + redeem qua ParentInvitation)
**Pre-condition:**
- Parent có `ParentStudentLink` với child (`linkType = PRIMARY` hoặc `SECONDARY`, `deleted = false`)
- Core đã mint KC-native JWT (Option B) với claim `referenceId = auth_credentials.entity_id = parents.id`; Gateway re-inject `X-User-Reference-Id` từ claim
- Frontend nhận access token + lưu vào localStorage

**Trigger:** Phụ huynh click "Học bạ" trên dashboard `/parent` cho 1 con.

**Main flow:**
1. FE redirect tới `/parent/transcript/[childId]`
2. FE gọi `GET /api/v1/parent/children/{childId}/transcript` qua `apiClient` — Gateway đính `X-User-Reference-Id: <parentId>` vào header xuôi xuống Core.
3. Core `ParentTranscriptController.getChildTranscript()` nhận request: `requireParentId(parentId)` — null → 401 AUTH_REQUIRED.
4. Service `ParentTranscriptService.getTranscriptsForChild(parentId, childId)`:
   - Validate args (null check) — null → 400 BAD_REQUEST.
   - **Scope guard (BR-PARENT-PORTAL-001):** `linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parentId, childId)` — false → 403 PARENT_NOT_LINKED. Service short-circuits (does NOT touch transcripts table).
5. Service queries `transcriptRepository.findByStudentIdAndDeletedFalseOrderBySemesterDesc(childId)` — returns list newest-first.
6. Service maps `Transcript → TranscriptResponse` (minimal projection per BR-PARENT-PORTAL-006).
7. Controller wraps in `ApiResponse.success(list)` → 200 OK.
8. FE `useChildTranscript()` hook receives data, renders `TranscriptView` component (each semester = 1 card với GPA + courses summary).

**Post-condition:** Phụ huynh thấy danh sách học bạ theo học kỳ, mới nhất trên cùng. Empty list nếu chưa có học bạ nào.

**Errors / FE behavior:**

| Error code | HTTP | FE behavior |
|------------|------|-------------|
| `AUTH_REQUIRED` | 401 | Redirect `/login` (Gateway interceptor đã handle) |
| `BAD_REQUEST` | 400 | Toast "Yêu cầu không hợp lệ" + back to `/parent` |
| `PARENT_NOT_LINKED` | 403 | Toast "Bạn không có quyền xem học bạ này" + back to `/parent`. Log client-side — chỉ ra IDOR probing. |
| Network error | — | Retry banner, React Query retry 3× backoff |
| 5xx | 500 | Toast "Lỗi hệ thống, thử lại sau" + retry button |

### UC-PARENT-PORTAL-02: Phụ huynh xem danh sách con (reuses Wave 2 endpoint)

**Actor:** Phụ huynh
**Pre-condition:** Parent đã đăng nhập; có ≥1 ParentStudentLink active.
**Trigger:** Phụ huynh truy cập `/parent` (landing page sau login).

**Main flow:** Reuses Wave 2 `GET /api/v1/parent/me/children` (existing, không phải scope Phase 1A). Phase 1B sẽ enrich `className` + `grade` (GAP-321b).

### UC-PARENT-PORTAL-03 (negative): Phụ huynh probe con của người khác

**Actor:** Phụ huynh A (linked với child 100 only)
**Trigger:** A enumerate id, gọi `GET /api/v1/parent/children/999/transcript` (child 999 thuộc parent B).

**Main flow:**
1. FE gọi endpoint với `childId = 999`.
2. Gateway xuôi xuống Core với `X-User-Reference-Id: 10` (id parent A).
3. Service `existsByParentIdAndStudentIdAndDeletedFalse(10, 999)` → `false`.
4. Service throws `BusinessException("PARENT_NOT_LINKED", FORBIDDEN)` — **NEVER touches `transcripts` table** (BR-PARENT-PORTAL-001 short-circuit).
5. GlobalExceptionHandler returns 403 + error code.
6. FE toast "Bạn không có quyền xem học bạ này".

**Post-condition:** A không leak được existence/non-existence của child 999. Server log warns probing attempt cho audit.

**Verification test:** `ParentTranscriptServiceTest#scopeGuard_unlinkedParent_throws403` — asserts `verify(transcriptRepository, never()).findByStudentIdAndDeletedFalseOrderBySemesterDesc(...)` — leak-free guarantee.

### Phase 1A out of scope

| UC | Where |
|----|-------|
| Phụ huynh xem điểm danh tháng | GAP-321b |
| Phụ huynh xem học phí pending + paid | GAP-321b |
| Phụ huynh xem hạnh kiểm HK | GAP-321b |
| Phụ huynh nhận notification | GAP-321b (depends GAP-063) |
| Phụ huynh xem kỷ luật history | GAP-321b |
| Phụ huynh đăng nhập qua Zalo OTP | GAP-321b |
| Phụ huynh nộp đơn khiếu nại | GAP-321c (depends GAP-339) |
| Phụ huynh confirm họp PHHS | GAP-321c (depends GAP-338) |
| Phụ huynh xin phép vắng mặt | GAP-321c |

---

## K-12 LEGAL Phase 1B foundation Use Cases (Wave 18b2 Bucket C — GAP-321b)

Phase 1B foundation extends the §K-12 LEGAL Phase 1A use cases with four sibling read-only facets + a per-read audit invariant. The flows mirror UC-PARENT-PORTAL-01 verbatim — only the data source + facet enum value change.

### UC-PARENT-FACET-ATTENDANCE-001: Phụ huynh xem điểm danh tháng

**Actor:** Phụ huynh (PARENT user with active `ParentStudentLink`)
**Pre-condition:** Same as UC-PARENT-PORTAL-01.
**Trigger:** Phụ huynh click "Điểm danh" on `/parent` dashboard for one child.

**Main flow:**
1. FE → `GET /api/v1/parent/children/{childId}/attendance?from=&to=&page=&size=` (Gateway injects `X-User-Reference-Id`).
2. `ParentAttendanceFacetController.getChildAttendance()` → `requireParentId()` (401 if header missing).
3. `ParentAttendanceFacetService.getAttendanceForChild(parentId, childId, from, to, pageable)`:
   - Validate args (null / inverted range → 400 BAD_REQUEST).
   - **Scope guard (BR-PARENT-FACET-ATT-001):** `linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parentId, childId)` — false → 403 `PARENT_FACET_FORBIDDEN`. Service short-circuits (does NOT touch `attendance_period`).
   - Query `attendancePeriodRepository.findByStudentIdAndDateBetweenAndDeletedFalse(...)`.
   - **Audit row (BR-PARENT-AUDIT-001):** `auditLogService.logRead(parentId, childId, ParentFacet.ATTENDANCE)` — best-effort.
4. Map to `AttendancePeriodResponse` page; controller wraps in `ApiResponse.success(...)`.
5. FE renders calendar/heatmap of period attendance.

**Post-condition:** Parent sees per-tiết attendance; one `parent_read_audit_log` row written.

**Errors / FE behavior (mirrors UC-PARENT-PORTAL-01):**

| Error code | HTTP | FE behavior |
|------------|------|-------------|
| `AUTH_REQUIRED` | 401 | Redirect `/login` |
| `BAD_REQUEST` | 400 | Toast "Khoảng thời gian không hợp lệ" |
| `PARENT_FACET_FORBIDDEN` | 403 | Toast "Bạn không có quyền xem điểm danh của con này" + back to `/parent` |
| 5xx | 500 | Retry banner |

### UC-PARENT-FACET-FEES-001: Phụ huynh xem học phí (v1 stub)

**Actor + pre-condition:** Same.
**Trigger:** Phụ huynh click "Học phí" for one child.

**Main flow:** identical to UC-PARENT-FACET-ATTENDANCE-001 except step 3 queries `invoiceRepository.findByStudentIdAndDeletedFalse(...)` and emits `ParentFacet.FEES`. v1 does NOT narrow by issue date — date-range narrowing deferred to GAP-321b.1; the api-contract.md flags this stub explicitly.

**Post-condition:** Parent sees invoices for child; audit row written.

### UC-PARENT-FACET-CONDUCT-001: Phụ huynh xem hạnh kiểm (v1 stub)

**Actor + pre-condition:** Same.
**Trigger:** Phụ huynh click "Hạnh kiểm" for one child.

**Main flow:** identical scope-guard pattern. v1 returns an empty list — backing data source for digital hạnh kiểm rating not yet present. Audit row still written so the parent's read intent is traceable. Concrete data source lands in GAP-321b.1.

**Post-condition:** FE shows "Chưa có dữ liệu hạnh kiểm" message; audit row written.

### UC-PARENT-FACET-NOTIFY-001: Phụ huynh xem thông báo (v1 stub)

**Actor + pre-condition:** Same.
**Trigger:** Phụ huynh click "Thông báo" for one child.

**Main flow:** identical scope-guard pattern. v1 returns an empty page — the cross-cutting notification engine ships in Wave 18a Bucket B (GAP-063b). Audit row still written.

**Post-condition:** FE shows empty notification drawer; audit row written.

### UC-PARENT-AUDIT-001: Per-read audit row invariant (cross-cutting)

**Actor:** System (no human trigger).
**Pre-condition:** Any parent-side facet endpoint returned 200.
**Trigger:** Successful 200 from any of: transcript (Phase 1A) / attendance / fees / conduct / notifications.

**Main flow:**
1. Facet service short-circuits if scope guard fails (BR-PARENT-FACET-*-001) — NO audit row in that case (denied reads must not be silently attributed to the requested child).
2. After scope guard passes, before returning data: `auditLogService.logRead(parentId, childId, facet)` writes one row in `parent_read_audit_log` (parent_id, child_id, facet, read_at, instance_id) inside a `REQUIRES_NEW` transaction.
3. Audit-store outage → write logged warn, propagation suppressed (best-effort; per BR-PARENT-AUDIT-001).

**Post-condition:** Append-only row exists. Admin/safeguarding query surface lands in GAP-321b.4.

**Verification test:** `ParentReadAuditLogIntegrationTest` — for each facet asserts (a) linked-parent invocation writes exactly one audit row with the correct `ParentFacet` value, and (b) unlinked-parent invocation throws 403 BEFORE any audit row is written.

### Out of Phase 1B foundation scope

| UC | Where |
|----|-------|
| Phụ huynh xem kỷ luật history | GAP-321c |
| Phụ huynh đăng nhập qua Zalo OTP | GAP-321b.2 |
| Phụ huynh nộp đơn / xin phép | GAP-321c |

---

## K-12 LEGAL Phase 1C v1 Use Cases (Wave 19 Bucket C — GAP-321c)

Phase 1C v1 ships PDPL granular consent management + 1 write action (complaint). Remaining write actions (conduct-confirm, meeting RSVP, absence-excuse) deferred to GAP-321c follow-up.

### UC-PARENT-CONSENT-MANAGE: Phụ huynh xem + cập nhật quyền xem dữ liệu con

**Actor:** Phụ huynh (PARENT user with active `ParentStudentLink`)
**Pre-condition:** Same as UC-PARENT-PORTAL-01.
**Trigger:** Phụ huynh mở trang "Quyền riêng tư của con" trên `/parent` (FE settings page chưa ship — endpoints sẵn cho consumer).

**Main flow (read):**
1. FE → `GET /api/v1/parent/consent?childId={id}` (Gateway injects `X-User-Reference-Id`).
2. `ParentConsentController.getConsent()` → `requireParentId()` (401 if header missing).
3. `ConsentService.getConsent(parentId, childId)`:
   - Validate args (null → 400 BAD_REQUEST).
   - Tìm `ParentStudentLink` qua `findByParentIdWithStudent` rồi filter theo `childId`.
   - Không có link → return `ParentalConsent.defaultValue()` (informational, không 404 — settings page hiển thị default sang option toggle).
4. Controller trả `{fields, version, updatedAt}` qua `ApiResponse.success(...)`.

**Main flow (write):**
1. FE → `PUT /api/v1/parent/consent?childId={id}` body `{"updates": {"fees": true, "conduct": false}}`.
2. `ParentConsentController.updateConsent()` → 401 nếu thiếu header; 400 nếu body null/empty.
3. `ConsentService.bumpConsent(parentId, childId, updates)`:
   - Tìm link; không có → 404 `PARENT_CONSENT_LINK_NOT_FOUND` (vs 403 — không phải lỗi authorization, là lỗi sai child id).
   - Merge updates lên `existing.fields` (sparse).
   - `next.version = existing.version + 1`, `next.updatedAt = now()`.
   - `link.setParentalConsent(next)` — JPA dirty checking flush ở commit.
5. Controller trả consent payload vừa bump.

**Post-condition:** JSONB column `parental_consent` cập nhật; subsequent facet calls re-evaluate `checkConsent`.

**Errors / FE behavior:**

| Error code | HTTP | FE behavior |
|------------|------|-------------|
| `AUTH_REQUIRED` | 401 | Redirect `/login` |
| `BAD_REQUEST` | 400 | Toast "Yêu cầu không hợp lệ" |
| `PARENT_CONSENT_LINK_NOT_FOUND` | 404 | Toast "Không tìm thấy liên kết phụ huynh-học sinh" + back to dashboard |

### UC-PARENT-COMPLAINT-FILE: Phụ huynh nộp khiếu nại (v1)

**Actor:** Phụ huynh (PARENT user with active `ParentStudentLink`)
**Pre-condition:** Same. Có ít nhất 1 child linked.
**Trigger:** Phụ huynh click "Khiếu nại" trên `/parent` cho 1 con cụ thể.

**Main flow:**
1. FE → `POST /api/v1/parent/complaints` body `{"studentId": 100, "complaintText": "..."}` (Gateway injects `X-User-Reference-Id`).
2. `ParentComplaintController.fileComplaint()` → `requireParentId()` (401 nếu thiếu header).
3. Bean validation: `complaintText` 10–2000 chars (nếu fail → 400).
4. `ParentComplaintService.fileComplaint(parentId, request)`:
   - Validate args (null → 400 BAD_REQUEST).
   - **Scope guard (BR-PARENT-PORTAL-013):** `linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parentId, studentId)` — false → 403 `PARENT_FACET_FORBIDDEN`.
   - Persist `ParentComplaint` row với status=PENDING.
5. Controller trả 201 + `{id, studentId, status, createdAt}`.

**Post-condition:** Row tồn tại trong `parent_complaint_queue`. Workflow xử lý (4-level escalation, attachments, resolver UI) ship trong GAP-339 follow-up.

**Errors / FE behavior:**

| Error code | HTTP | FE behavior |
|------------|------|-------------|
| `AUTH_REQUIRED` | 401 | Redirect `/login` |
| `BAD_REQUEST` | 400 | Toast "Nội dung khiếu nại 10–2000 ký tự" |
| `PARENT_FACET_FORBIDDEN` | 403 | Toast "Bạn không có quyền nộp khiếu nại cho học sinh này" |
| 5xx | 500 | Retry banner |

### Out of Phase 1C v1 scope

| UC | Where |
|----|-------|
| Phụ huynh confirm họp PHHS | GAP-321c follow-up (depends GAP-338) |
| Phụ huynh xác nhận đã đọc báo cáo hạnh kiểm tháng | GAP-321c follow-up |
| Phụ huynh xin phép vắng mặt + upload chứng cứ | GAP-321c follow-up (depends MinIO encrypted bucket pattern) |
| Re-consent prompt khi policy version bump | GAP-321c follow-up |
| Settings page UI | GAP-321c follow-up (FE) |
| i18n EN + zh-CN | GAP-321c follow-up |

## Log

- **2026-05-05** Phase 1C v1 use cases added — Wave 19 Bucket C (GAP-321c v1). 2 new UCs (UC-PARENT-CONSENT-MANAGE, UC-PARENT-COMPLAINT-FILE). Phase 1C v1 wires fees facet end-to-end with the new consent gate; remaining facets + 3 write actions + i18n + settings UI deferred via GAP-321c follow-up filed in this PR.
- **2026-05-04** Phase 1B foundation use cases added — Wave 18b2 Bucket C (GAP-321b foundation). 4 new facet UCs + 1 cross-cutting audit invariant UC. Conduct + notifications shipped as v1 stubs returning empty data; the scope guard + audit row are the foundation contract.
