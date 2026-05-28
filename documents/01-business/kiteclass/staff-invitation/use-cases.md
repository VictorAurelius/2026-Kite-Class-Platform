---
audience: mixed
domain: kiteclass-core/staff-invitation
last-updated: 2026-05-28
version: 1.0 (Wave meta-6 Bucket A MVP — GAP-772)
---

# Staff Invitation — Use Cases

**Domain:** KiteClass Core / Staff Invitation
**Version:** 1.0 (Wave meta-6 Bucket A MVP)
**Updated:** 2026-05-28
**Companion docs:** [`rules.md`](rules.md), [`api-contract.md`](api-contract.md)

---

## Use Cases

### UC-STAFF-INV-01: Owner mời nhân viên mới

**Actor:** Owner / Admin của trung tâm (e.g., chị Hằng — Owner Trung tâm Anh ngữ Sky Education)
**Precondition:**
- Owner đã đăng nhập, JWT hợp lệ với role `OWNER` hoặc `ADMIN` hoặc `PLATFORM_ADMIN`
- Có quyền quản lý nhân viên trên tenant của mình
- Biết email + role muốn mời (STAFF/TEACHER/MANAGER)

**Steps:**

1. Owner đăng nhập KiteHub → vào dashboard quản lý trung tâm → mở trang "Nhân viên"
2. Owner click nút "Mời nhân viên mới" → form mở ra với 2 ô:
   - Email: `tam.nguyen@gmail.com`
   - Role dropdown: chọn "Giáo viên" (TEACHER)
3. Owner click "Gửi lời mời"
4. FE → Gateway: `POST /api/v1/staff-invitations` kèm body `{ email: "tam.nguyen@gmail.com", role: "TEACHER" }` + Bearer JWT
5. Gateway: verify JWT → forward đến kiteclass-core kèm `X-User-Id` (Owner id) + `X-Tenant-Id` (sub-domain → UUID)
6. Core `StaffInvitationController.invite()`:
   - Lấy `tenantId` từ `TenantContext.getCurrentTenant()`
   - Lấy `inviterId` từ `UserContext.getCurrentUser()` — thiếu → `401 AUTH_REQUIRED` (BR-STAFF-INVITE-009)
   - Service `invite(tenantId, email, role, inviterId)` — normalize email lowercase + trim (BR-STAFF-INVITE-006), tạo token UUID 128-bit (BR-STAFF-INVITE-001), set `expiresAt = now + 168h` (BR-STAFF-INVITE-002), status PENDING (BR-STAFF-INVITE-005), save entity
7. Core → FE: trả `201 Created` với `ApiResponse<StaffInvitationResponse>` chứa `token` (chỉ ở response create — BR-STAFF-INVITE-008)
8. FE: hiển thị toast "Lời mời nhân viên đã được gửi" + ô nhập "Sao chép link redemption" với `https://app.kitehub.me/staff/accept-invite/{token}` để Owner copy gửi qua Zalo / SMS (vì email send chưa wire Wave meta-6 MVP per `rules.md` §8)

**Postcondition:**
- Row `staff_invitations` mới với `status=PENDING`, `expires_at = now + 168h`, `invited_by_user_id = Owner id`
- Owner có link redemption để forward cho Staff

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 401 | JWT thiếu hoặc invalid | "AUTH_REQUIRED" |
| 403 | Role không phải ADMIN/OWNER/PLATFORM_ADMIN | (Spring Security default) |
| 400 | Email blank / sai format | "Email là bắt buộc" / "Email không hợp lệ" |
| 400 | Role không phải STAFF/TEACHER/MANAGER | "role must be one of STAFF, TEACHER, MANAGER" (BR-STAFF-INVITE-003) |

**FE behavior:**
- Toast success thông báo "Đã gửi lời mời nhân viên đến {email}"
- Hiển thị link redemption với nút "Sao chép" — Owner chủ động forward Zalo
- Reload list invitations PENDING để show row mới

---

### UC-STAFF-INV-02: Staff accept invitation token

**Actor:** Nhân viên được mời (e.g., thầy Tâm — sắp làm giáo viên cho Trung tâm Sky Education)
**Precondition:**
- Thầy Tâm nhận link redemption từ Owner (qua Zalo/email forward)
- Token còn hạn (status PENDING, `expiresAt > now()`)
- Có tài khoản email + password để set initial credential

**Steps:**

1. Thầy Tâm click link `https://app.kitehub.me/staff/accept-invite/{token}` trong tin nhắn Zalo từ chị Hằng
2. FE route `/staff/accept-invite/[token]` mở form:
   - Họ tên: `Trần Văn Tâm`
   - Mật khẩu: `Tam2026abc` (≥8 ký tự + 1 chữ + 1 số per BR-STAFF-PWD-002)
3. Thầy Tâm submit form
4. FE → Gateway: `POST /api/v1/auth/register-staff/{token}` với body `AcceptStaffInviteRequest`
5. Gateway: forward → Core `POST /api/v1/staff-invitations/{token}/accept` kèm `X-Tenant-Id`
6. Core `StaffInvitationController.accept()`:
   - Parse `tenantId` từ `X-Tenant-Id` header (BR-STAFF-AUTH-003)
   - Service `accept(tenantId, token, request)`:
     - Lookup `findByTokenAndDeletedFalse(token)` — thiếu → `404 STAFF_INVITATION_NOT_FOUND` (BR-STAFF-ACC-001)
     - Check `invitation.instanceId == tenantId` — không khớp → `404` (BR-STAFF-ACC-006, defense in depth chống enumeration)
     - Check status ACCEPTED → `400 STAFF_INVITATION_ALREADY_ACCEPTED` (BR-STAFF-ACC-002, BR-STAFF-ACC-003)
     - Check status REVOKED → `400 STAFF_INVITATION_REVOKED`
     - Check `expiresAt < now()` — quá hạn → set status EXPIRED, save, `400 STAFF_INVITATION_EXPIRED`
     - Flip status PENDING → ACCEPTED + set `acceptedAt = now()` (BR-STAFF-ACC-004)
   - Return `AcceptStaffInviteResult { invitationId, tenantId, email, fullName, role, acceptedAt }`
7. Core → Gateway: trả `200 OK` với `ApiResponse<AcceptStaffInviteResult>`
8. Gateway: nhận result → hash password (BR-STAFF-PWD-003) → tạo `users` row với `userType=STAFF, referenceId=invitationId, role=TEACHER` → mint JWT cho thầy Tâm
9. FE: nhận JWT → auto-login → redirect dashboard `/teacher` (cho TEACHER role)

**Postcondition:**
- `staff_invitations.status = ACCEPTED`, `acceptedAt` set
- Gateway `users` row tồn tại với role TEACHER
- Thầy Tâm có JWT, đã đăng nhập, redirect vào dashboard teacher
- (Wave meta-6 defer) `acceptedUserId` chưa được write back vào `staff_invitations` — paired GAP-779 endpoint sẽ patch sau

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 404 | Token sai / cross-tenant | "STAFF_INVITATION_NOT_FOUND" |
| 400 | Status ACCEPTED (idempotent guard) | "STAFF_INVITATION_ALREADY_ACCEPTED" |
| 400 | Status REVOKED (Owner đã hủy) | "STAFF_INVITATION_REVOKED" |
| 400 | Token quá hạn TTL | "STAFF_INVITATION_EXPIRED" |
| 400 | Password yếu | "Password must contain at least 1 letter + 1 digit" |
| 400 | Họ tên < 2 hoặc > 100 ký tự | "Họ tên phải từ 2-100 ký tự" (Bean validation message default) |

**FE behavior:**
- Token đã dùng → hiển thị CTA "Tài khoản đã kích hoạt, mời bạn đăng nhập" + link `/login`
- Token quá hạn → hiển thị CTA "Lời mời đã hết hạn, mời liên hệ chủ trung tâm để xin link mới"
- Token bị hủy → hiển thị CTA "Lời mời không còn hợp lệ, mời liên hệ chủ trung tâm"
- Form validation inline: password yếu hiển thị "Mật khẩu phải có ít nhất 8 ký tự, gồm 1 chữ + 1 số"

---

### UC-STAFF-INV-03: Owner xem list invitations PENDING

**Actor:** Owner / Admin trung tâm
**Precondition:** JWT hợp lệ với role ADMIN/OWNER/PLATFORM_ADMIN, đã có ≥1 invitation đã gửi trong tenant

**Steps:**

1. Owner mở trang "Nhân viên" → tab "Lời mời đang chờ"
2. FE → Gateway: `GET /api/v1/staff-invitations` + Bearer JWT
3. Gateway: forward đến Core kèm `X-Tenant-Id`
4. Core `StaffInvitationController.list()`:
   - Lấy `tenantId` từ `TenantContext`
   - Service `listForTenant(tenantId)`:
     - `findByStatusAndDeletedFalseOrderByCreatedAtDesc(PENDING)`
     - Filter rows có `instanceId.equals(tenantId)` (defense in depth, dù Hibernate filter đã clamp)
     - Map sang `StaffInvitationResponse` với `includeToken=false` (BR-STAFF-INVITE-008 — không bao giờ trả token ở list)
5. Core → FE: `200 OK` với `ApiResponse<List<StaffInvitationResponse>>`
6. FE: render table với cột Email / Role / Ngày tạo / Ngày hết hạn / Trạng thái + nút "Hủy lời mời" cho mỗi row

**Postcondition:** List rendered, Owner thấy được tất cả invitations PENDING của tenant

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 401 | JWT thiếu | "AUTH_REQUIRED" |
| 403 | Role không đủ quyền | (Spring Security default) |

**FE behavior:**
- Empty state khi list trống: "Chưa có lời mời nào đang chờ. Mời nhân viên ngay!" + CTA "Mời nhân viên mới"
- Token field hiển thị "—" (không bao giờ leak token qua list endpoint)
- Sort default mới nhất trước (DESC theo `createdAt`)

---

### UC-STAFF-INV-04: Owner hủy invitation PENDING

**Actor:** Owner / Admin trung tâm
**Precondition:**
- JWT hợp lệ với role ADMIN/OWNER/PLATFORM_ADMIN
- Invitation tồn tại, status PENDING (chưa ACCEPTED/EXPIRED/REVOKED)

**Steps:**

1. Owner trong tab "Lời mời đang chờ" → click nút "Hủy lời mời" trên row của thầy Tâm
2. FE hiển thị confirm modal: "Bạn có chắc muốn hủy lời mời gửi đến tam.nguyen@gmail.com? Hành động này không thể hoàn tác."
3. Owner click "Xác nhận hủy"
4. FE → Gateway: `DELETE /api/v1/staff-invitations/{id}` + Bearer JWT
5. Gateway: forward đến Core kèm `X-Tenant-Id`
6. Core `StaffInvitationController.revoke()`:
   - Lấy `tenantId` từ `TenantContext`
   - Service `revoke(tenantId, invitationId)`:
     - `findById(invitationId)` filter `!deleted` — thiếu → `404 STAFF_INVITATION_NOT_FOUND`
     - Check `invitation.instanceId == tenantId` — không khớp → `404` (BR-STAFF-INVITE-004, log warn)
     - Check status PENDING — không phải → `409 STAFF_INVITATION_NOT_PENDING` (idempotent guard: đã ACCEPTED/EXPIRED/REVOKED không cho hủy)
     - Flip status PENDING → REVOKED + save (BR-STAFF-INVITE-005)
7. Core → FE: `200 OK` với `ApiResponse<Void>` message "Lời mời đã bị hủy"

**Postcondition:**
- `staff_invitations.status = REVOKED`
- Future accept attempts với token này → `400 STAFF_INVITATION_REVOKED`
- Owner UI reload list, row biến mất khỏi tab PENDING (hoặc move sang tab "Đã hủy" nếu FE có tab đó)

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 401 | JWT thiếu | "AUTH_REQUIRED" |
| 403 | Role không đủ quyền | (Spring Security default) |
| 404 | Invitation không tồn tại / cross-tenant / soft-deleted | "STAFF_INVITATION_NOT_FOUND" |
| 409 | Status đã không phải PENDING (idempotent guard) | "STAFF_INVITATION_NOT_PENDING" |

**FE behavior:**
- Toast success: "Đã hủy lời mời gửi đến tam.nguyen@gmail.com"
- Reload list — row biến mất khỏi tab PENDING
- Confirm modal có warning "Hành động này không thể hoàn tác. Để mời lại, bạn cần tạo lời mời mới."

---

## Cross-Use-Case Notes

### Persona tone (per `vn-localization-audit-checklist.md` §2 email tone matrix)

| Persona | Greeting trong email/UI | Sample |
|---|---|---|
| **P2 Center Owner** (chị Hằng) | `Em chào chị,` — formal-respectful | Subject: `Lời mời tham gia làm nhân viên — Trung tâm Anh ngữ Sky Education` |
| **Staff (TEACHER)** (thầy Tâm) | `Chào bạn,` — casual friendly | Subject: `Lời mời tham gia Trung tâm Anh ngữ Sky Education` |

### VN sample data convention (per `vn-localization-audit-checklist.md` §3)

- Owner: chị Hằng (Owner Trung tâm Anh ngữ Sky Education)
- Staff TEACHER: thầy Tâm (`tam.nguyen@gmail.com`)
- Staff MANAGER: anh Quang (`quang.le@skyedu.vn`)
- Staff STAFF: cô Mai (`mai.pham@skyedu.vn`)
- Tenant: Trung tâm Anh ngữ Sky Education
- Email format: `<name>@skyedu.vn` (tenant email) hoặc `<name>@gmail.com` (personal)

### Future scope (Wave 1.5+)

- UC-STAFF-INV-05: Resend invitation (Wave meta-6 defer, paired GAP cluster)
- UC-STAFF-INV-06: Bulk invite từ CSV upload
- UC-STAFF-INV-07: Owner xem list invitations ALL statuses (current MVP chỉ PENDING)
- UC-STAFF-INV-08: Sweeper scheduled job tự động flip PENDING → EXPIRED (scheduler chưa wire MVP)
