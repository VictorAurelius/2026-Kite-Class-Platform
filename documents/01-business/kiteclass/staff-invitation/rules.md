---
audience: mixed
domain: kiteclass-core/staff-invitation
last-updated: 2026-05-28
version: 1.0 (Wave meta-6 Bucket A MVP — GAP-772 + GAP-782 retroactive 3-layer docs)
---

# Staff Invitation — Business Rules

**Domain:** KiteClass Core / Staff Invitation
**Version:** 1.0 (Wave meta-6 Bucket A MVP — GAP-772)
**Updated:** 2026-05-28
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/`
**Source migration:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V71__create_staff_invitations.sql`
**Sister domain:** `parent-portal` (mirror pattern, không có student linkage)

---

## 1. Scope

Wave meta-6 Bucket A MVP ship token-based staff onboarding flow:
- `StaffInvitation` entity (token, role, status, TTL, lifecycle)
- Owner-side endpoints: invite + list pending + revoke
- Public claim endpoint: accept invitation (consumed by Gateway during staff register flow)
- Tenant isolation qua Hibernate filter
- Role enum: STAFF / TEACHER / MANAGER (Owner role excluded — single-owner invariant)

**Out of scope (Wave meta-6 deferred):**
- Audit log cho invitation lifecycle (GAP-659 sister split) — khi ship, audit service MUST use `Propagation.REQUIRES_NEW` per `.claude/rules/audit-service-isolation.md`
- Resend email endpoint
- Bulk invite (>10 emails cùng lúc)
- Scheduled sweeper PENDING → EXPIRED job (mới có repository method, scheduler chưa wire)
- Gateway `acceptedUserId` write-back qua internal endpoint (paired GAP-779 KH auth `/me` endpoint)

---

## 2. Staff Invitation Account Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-STAFF-INVITE-001 | Token entropy 128-bit | `UUID.randomUUID().toString()` lưu ở `staff_invitations.token`, unique index `idx_staff_inv_token`. **Code reference:** `StaffInvitationServiceImpl.java:64`. |
| BR-STAFF-INVITE-002 | TTL mặc định 168 giờ (7 ngày) | Cấu hình qua `kiteclass.staff-invite.invitation-ttl-hours` (default 168). **Source:** Competitor analysis (Slack workspace invite 7d; Notion guest invite 14d) + informed gut (TTL ngắn quá → Owner phải resend; dài quá → token leak cửa sổ rộng). **Rationale:** 7 ngày = đủ thời gian Owner gửi link Zalo + Staff confirm + set password mà không cần resend; ngắn hơn 24h như parent-portal vì Staff onboarding ít gấp hơn child enrollment. **Code reference:** `StaffInvitationServiceImpl.java:50` `@Value("${kiteclass.staff-invite.invitation-ttl-hours:168}")`. **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-28). **Compliance:** N/A. **Review cadence:** Quarterly. **Next review:** 2026-08-28. |
| BR-STAFF-INVITE-003 | Role grant scope giới hạn 3 giá trị | Chỉ chấp nhận `STAFF` / `TEACHER` / `MANAGER`. Owner role bị loại trừ (single-owner-per-tenant invariant — chỉ một Owner mỗi trung tâm). PLATFORM_ADMIN cũng bị loại trừ (chỉ seeded ở Gateway). **Code reference:** `InviteStaffRequest.java:20` `@Pattern(regexp = "^(STAFF|TEACHER|MANAGER)$")` + DB constraint `chk_staff_invitation_role` (V71 line 37-38). |
| BR-STAFF-INVITE-004 | Tenant isolation — defense in depth | Mọi truy vấn lọc theo `instance_id` qua Hibernate `tenantFilter` + service-level check trong `revoke()` + `accept()` (so sánh `invitation.instanceId == tenantId`). Cross-tenant attempt trả `404 STAFF_INVITATION_NOT_FOUND` để chống enumeration. **Code reference:** `StaffInvitationServiceImpl.java:105-111` (revoke) + `StaffInvitationServiceImpl.java:134-139` (accept). |
| BR-STAFF-INVITE-005 | Status state machine — chỉ 4 transition hợp lệ | `PENDING → ACCEPTED` (Staff hoàn tất redeem) HOẶC `PENDING → EXPIRED` (TTL hết hạn — phát hiện trong `accept()` runtime check) HOẶC `PENDING → REVOKED` (Owner hủy). Không có transition nào khác. ACCEPTED là terminal state cho thành công; EXPIRED + REVOKED là terminal state cho thất bại. **Code reference:** `StaffInvitationStatus.java` enum 4 giá trị + service-level guards. |
| BR-STAFF-INVITE-006 | Email lowercase + trim trước khi save | Khi invite, email được normalize bằng `email.trim().toLowerCase()` để tránh duplicate `Hong@Test.vn` vs `hong@test.vn`. **Code reference:** `StaffInvitationServiceImpl.java:59`. |
| BR-STAFF-INVITE-007 | Email format VN-friendly | Sample data trong test fixtures + docs dùng email VN-friendly: `hong.tran@skyedu.vn`, `tam.nguyen@gmail.com`. **Code reference:** `AcceptStaffInviteRequest.java:24` `@Size(min = 2, max = 100)` + `@Pattern` regex hỗ trợ VN diacritic per `vn-localization-audit-checklist.md` §3. |
| BR-STAFF-INVITE-008 | Token chỉ trả về ở response create | `StaffInvitationResponse.token` chỉ populate ở response của `POST /api/v1/staff-invitations` (Owner issuing). List endpoints `GET /api/v1/staff-invitations` server-side filter token = null để giảm leak surface qua admin UI log scrapes. **Code reference:** `StaffInvitationServiceImpl.java:178-191` `toResponse(... includeToken)` flag. |
| BR-STAFF-INVITE-009 | Inviter id bắt buộc | `POST /api/v1/staff-invitations` yêu cầu `X-User-Id` header (Gateway populate sau khi verify JWT). Thiếu → `401 AUTH_REQUIRED`. Tránh un-attributed invitations cho audit trail tương lai. **Code reference:** `StaffInvitationController.java:76-79`. |
| BR-STAFF-INVITE-010 | Soft delete only | Entity kế thừa `BaseEntity` với cờ `deleted` (V71 line 32). Repository methods chỉ trả rows có `deletedFalse` qua `findByTokenAndDeletedFalse(...)` + `findByStatusAndDeletedFalseOrderByCreatedAtDesc(...)`. |

**Staff invitation statuses:** PENDING, ACCEPTED, EXPIRED, REVOKED
**Staff invitation roles (provisioned on accept):** STAFF, TEACHER, MANAGER

---

## 3. Password Policy (cho Staff Accept Flow)

Khi Staff submit `AcceptStaffInviteRequest.password`:

| ID | Rule | Detail |
|----|------|--------|
| BR-STAFF-PWD-001 | Length 8-128 ký tự | `@Size(min = 8, max = 128)`. **Code reference:** `AcceptStaffInviteRequest.java:26`. **Source:** OWASP Password Cheat Sheet (min 8 ký tự cho non-privileged role) + project convention. **Rationale:** Trial-test Wave meta-6 Bucket A scope là MVP — chỉ require letter + digit; complexity rule mạnh hơn (uppercase + special char) defer Phase 1.5 khi staff role có access nhạy cảm hơn (manager + financial reports). |
| BR-STAFF-PWD-002 | Phải có ít nhất 1 chữ + 1 số | Regex `^(?=.*[A-Za-z])(?=.*\d).+$`. **Code reference:** `AcceptStaffInviteRequest.java:27-28`. **Rationale:** Yếu hơn parent-portal (yêu cầu uppercase + lowercase + special) vì Staff onboarding flow MVP — sẽ tighten Phase 1.5 khi audit log ship. |
| BR-STAFF-PWD-003 | Plaintext password chỉ chuyển qua Gateway | kiteclass-core KHÔNG hash hoặc store password. Service `accept()` chỉ pass `request.fullName()` vào `AcceptStaffInviteResult` để Gateway xử lý hash + tạo User row + mint JWT. **Code reference:** `StaffInvitationServiceImpl.java:168-175` (KHÔNG access `request.password()`). |

---

## 4. Acceptance Flow Rules

Khi Staff click email link → POST với token:

| ID | Rule | Detail |
|----|------|--------|
| BR-STAFF-ACC-001 | Token lookup qua `findByTokenAndDeletedFalse` | Lookup không cộng tenant filter (token là 128-bit unique). Sau khi tìm thấy → check `instanceId` (BR-STAFF-INVITE-004). **Code reference:** `StaffInvitationServiceImpl.java:130-132`. |
| BR-STAFF-ACC-002 | Status check theo thứ tự ACCEPTED → REVOKED → expiresAt | Service kiểm tra: (1) status ACCEPTED → `400 STAFF_INVITATION_ALREADY_ACCEPTED`; (2) status REVOKED → `400 STAFF_INVITATION_REVOKED`; (3) `expiresAt < now()` → flip status PENDING → EXPIRED + save + `400 STAFF_INVITATION_EXPIRED`. Thứ tự này quan trọng để return error message rõ ràng nhất cho FE. **Code reference:** `StaffInvitationServiceImpl.java:141-154`. |
| BR-STAFF-ACC-003 | Accept idempotent: chỉ 1 lần thành công | Sau khi PENDING → ACCEPTED, retry cùng token trả `400 STAFF_INVITATION_ALREADY_ACCEPTED`. Không re-issue token mới — Owner phải tạo invitation mới qua endpoint invite. |
| BR-STAFF-ACC-004 | `acceptedAt` set khi accept thành công | `Instant.now()` snap timestamp. **Code reference:** `StaffInvitationServiceImpl.java:157-158`. |
| BR-STAFF-ACC-005 | `acceptedUserId` null trong MVP | Wave meta-6 Bucket A MVP scope chỉ track invitation lifecycle ở Core. Gateway tạo `users` row với `userType=STAFF, referenceId=invitation.id` SAU khi nhận `AcceptStaffInviteResult` qua sync return; ghi `acceptedUserId` back qua internal endpoint defer GAP-779. **Code reference:** `StaffInvitationServiceImpl.java:159-162` comment block + `StaffInvitation.java:106` field annotation. |
| BR-STAFF-ACC-006 | Cross-tenant token redemption từ chối với 404 | Nếu `invitation.instanceId != X-Tenant-Id`, return `404 STAFF_INVITATION_NOT_FOUND` (không 403 — chống enumeration cross-tenant). **Code reference:** `StaffInvitationServiceImpl.java:134-139`. |

---

## 5. Identity & Access Pattern

| ID | Rule | Detail |
|----|------|--------|
| BR-STAFF-AUTH-001 | Owner endpoint dùng `@PreAuthorize` | `POST` + `GET` + `DELETE` `/api/v1/staff-invitations[/{id}]` annotate `@PreAuthorize("hasAnyRole('ADMIN','OWNER','PLATFORM_ADMIN')")`. Defense in depth — Gateway path filter routes Owner request, controller layer enforce role một lần nữa. **Code reference:** `StaffInvitationController.java:70` + 93 + 106. |
| BR-STAFF-AUTH-002 | Public accept endpoint không cần JWT | `POST /api/v1/staff-invitations/{token}/accept` không có `@PreAuthorize` — token là authentication. Gateway populate `X-Tenant-Id` header từ sub-domain. **Code reference:** `StaffInvitationController.java:119-129`. |
| BR-STAFF-AUTH-003 | Tenant context bắt buộc qua `TenantContext.getCurrentTenant()` | Owner endpoints lấy `tenantId` từ `TenantContext` (set bởi Hibernate filter chain after Gateway header propagation). Accept endpoint parse trực tiếp từ `X-Tenant-Id` header vì context chưa được set (chưa qua JWT auth). **Code reference:** `StaffInvitationController.java:75` (Owner) + `126` (accept). |

---

## 6. Config Keys

| Key | Default | Env Override | Description |
|-----|---------|--------------|-------------|
| `kiteclass.staff-invite.invitation-ttl-hours` | `168` (7 ngày) | — | Token lifetime giờ (BR-STAFF-INVITE-002). **Code reference:** `StaffInvitationServiceImpl.java:50`. |

**Note Wave meta-6 MVP:** Chưa wire `enabled` feature flag như parent-portal vì staff invitation là core feature mọi tenant cần (không có persona gate); feature flag có thể được thêm Phase 1.5 nếu cần kill-switch.

---

## 7. Database Schema

### Table
- `staff_invitations` (V71__create_staff_invitations.sql)

### Columns
| Column | Type | Nullable | Default | Constraint |
|---|---|---|---|---|
| `id` | BIGSERIAL | NOT NULL | — | PRIMARY KEY |
| `instance_id` | UUID | NOT NULL | — | tenant scope |
| `email` | VARCHAR(255) | NOT NULL | — | lowercase per BR-STAFF-INVITE-006 |
| `role` | VARCHAR(32) | NOT NULL | `'STAFF'` | `chk_staff_invitation_role IN ('STAFF', 'TEACHER', 'MANAGER')` |
| `token` | VARCHAR(64) | NOT NULL | — | UNIQUE |
| `status` | VARCHAR(20) | NOT NULL | `'PENDING'` | `chk_staff_invitation_status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')` |
| `expires_at` | TIMESTAMP | NOT NULL | — | TTL boundary |
| `invited_by_user_id` | BIGINT | nullable | — | Gateway user id (Owner) |
| `accepted_at` | TIMESTAMP | nullable | — | set khi accept thành công |
| `accepted_user_id` | BIGINT | nullable | — | Wave meta-6 defer GAP-779 |
| BaseEntity audit cols | created_at / updated_at / created_by / updated_by / deleted / version | per `BaseEntity` |

### Indexes
- `idx_staff_inv_token` (unique) — primary lookup pattern (BR-STAFF-ACC-001)
- `idx_staff_inv_email` — duplicate detection (informational; không phải unique vì re-invite cùng email là valid use case)
- `idx_staff_inv_status` — Owner list endpoint filter
- `idx_staff_inv_instance` — tenant isolation queries
- `idx_staff_inv_expires_pending` (partial — `WHERE status = 'PENDING'`) — sweeper scan optimization

---

## 8. Email Integration (Wave meta-6 deferred)

Wave meta-6 Bucket A MVP scope KHÔNG ship email send — Owner copy link manually từ FE response (token field). Email integration scope:

| Trigger | Template | Recipient | Routing |
|---------|----------|-----------|---------|
| Invitation created | `staff-invitation` (Wave meta-6 defer) | `invitation.email` | RabbitMQ exchange `email.exchange`, routing key `email.send` |

**Variables (planned):** `staffEmail`, `staffRole`, `tenantName`, `inviterName`, `redeemUrl`, `expiresAt`, `tokenTtlHours`

**Subject (vi):** "Lời mời tham gia làm nhân viên — Trung tâm {tenantName}"

**Greeting (vi):** "Chào bạn," (per `vn-localization-audit-checklist.md` §2 Email tone matrix row 5 — Solo casual tone phù hợp Staff onboarding flow)

**Resilience (planned):** Publish best-effort qua `OutboxEventWriter` per `design-patterns.md` §3.5.1 — invitation row là source of truth; nếu RabbitMQ down, Owner có thể "resend" qua FE.

---

## 9. Sister-Domain References

- **`parent-portal`** — pattern mirror, không có student linkage. Staff identity scope = tenant-wide; parent identity scope = child-bound qua `ParentStudentLink`.
- **Gateway User provisioning** — kiteclass-core không tạo `users` row; Gateway xử lý hash password + mint JWT sau khi nhận `AcceptStaffInviteResult` sync return.

---

## 10. Future Work (Phase 1.5+ — out of scope MVP)

- Audit log cho mọi state transition (GAP-659 sister split) — service phải dùng `Propagation.REQUIRES_NEW` per `audit-service-isolation.md`
- Resend email endpoint (`POST /api/v1/staff-invitations/{id}/resend`)
- Bulk invite (CSV upload + batch processing)
- Sweeper scheduled job PENDING → EXPIRED (đã có repository method `findByStatusAndExpiresAtBeforeAndDeletedFalse`)
- Gateway internal endpoint write-back `acceptedUserId` (GAP-779)
- Owner-side filter list theo status (đang hardcode PENDING — extend cho ACCEPTED/EXPIRED/REVOKED visibility)
- Password complexity tighten Phase 1.5 (uppercase + special char) khi staff scope access financial reports

---

## 11. Log

- **2026-05-28 (v1.0):** Rule file shipped retroactive cho Wave meta-6 Bucket A (GAP-772 code shipped PR #1904 2026-05-27 không có 3-layer docs). Closes GAP-782 Bucket A item 3 cluster (3-layer business docs). Reviewer: @nguyenvankiet (acting Product Owner + solo-dev, 2026-05-28). Source code reference: 11 Java files trong `kiteclass-core/.../module/staff/` + V71 Flyway migration. Sister domain: `parent-portal` (pattern mirror). 11 BR rules + 6 password/access rules + 3-section state machine documented. Wave meta-6 Bucket A MVP scope; Phase 1.5+ deferred items §10. Compliance check: tất cả BR đánh giá N/A (Staff onboarding không trigger PDPL article specific — Staff identity là tenant-scoped employee record, không phải end-user PII consent flow; revisit Phase 2 nếu external regulator yêu cầu specific staff data retention).
