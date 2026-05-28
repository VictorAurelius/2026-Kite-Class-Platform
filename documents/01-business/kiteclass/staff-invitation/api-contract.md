---
audience: mixed
domain: kiteclass-core/staff-invitation
last-updated: 2026-05-28
version: 1.0 (Wave meta-6 Bucket A MVP — GAP-772)
---

# Staff Invitation — API Contract

**Domain:** KiteClass Core / Staff Invitation
**Version:** 1.0 (Wave meta-6 Bucket A MVP)
**Updated:** 2026-05-28
**Module:** `kiteclass-core` package `com.kiteclass.core.module.staff.controller`
**Source code:** [`StaffInvitationController.java`](../../../kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/controller/StaffInvitationController.java)
**Companion docs:** [`rules.md`](rules.md), [`use-cases.md`](use-cases.md)

---

## Authentication & Headers

| Header | Source | Required for |
|--------|--------|--------------|
| `Authorization: Bearer <JWT>` | Gateway (Owner JWT after `/api/v1/auth/login`) | Owner endpoints: invite + list + revoke |
| `X-User-Id` | Gateway (populate from JWT `userId` claim) | `POST /api/v1/staff-invitations` (UC-STAFF-INV-01) |
| `X-Tenant-Id` | Gateway (sub-domain → instanceId UUID) | **MANDATORY** mọi endpoint (Owner + public accept) |
| (none) | Token là auth | `POST /api/v1/staff-invitations/{token}/accept` (UC-STAFF-INV-02) |

---

## Endpoint Index

| Method | Path | Use Case | Auth | Role |
|--------|------|----------|------|------|
| `POST` | `/api/v1/staff-invitations` | UC-STAFF-INV-01 | JWT + `X-User-Id` + `X-Tenant-Id` | ADMIN / OWNER / PLATFORM_ADMIN |
| `GET` | `/api/v1/staff-invitations` | UC-STAFF-INV-03 | JWT + `X-Tenant-Id` | ADMIN / OWNER / PLATFORM_ADMIN |
| `DELETE` | `/api/v1/staff-invitations/{id}` | UC-STAFF-INV-04 | JWT + `X-Tenant-Id` | ADMIN / OWNER / PLATFORM_ADMIN |
| `POST` | `/api/v1/staff-invitations/{token}/accept` | UC-STAFF-INV-02 | Public (token = auth) + `X-Tenant-Id` | Public |

---

## StaffInvitationController — `/api/v1/staff-invitations`

### POST /api/v1/staff-invitations

**Use Case:** UC-STAFF-INV-01 (Owner mời nhân viên) | **Auth:** Bearer + `X-User-Id` + `X-Tenant-Id` | **Role:** ADMIN, OWNER, PLATFORM_ADMIN (enforce ở Controller `@PreAuthorize` line 70)

```json
// Request — InviteStaffRequest
// Source: kiteclass-core/.../module/staff/dto/InviteStaffRequest.java
{
  "email": "tam.nguyen@gmail.com",
  "role": "TEACHER"
}

// Response 201 — ApiResponse<StaffInvitationResponse>
{
  "success": true,
  "message": "Lời mời nhân viên đã được gửi",
  "data": {
    "id": 42,
    "email": "tam.nguyen@gmail.com",
    "role": "TEACHER",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PENDING",
    "expiresAt": "2026-06-04T10:00:00Z",
    "invitedByUserId": 7,
    "acceptedAt": null,
    "acceptedUserId": null,
    "createdAt": "2026-05-28T10:00:00Z"
  }
}
```

**Validation rules (Bean validation messages — vi):**

| Field | Constraint | Error message |
|---|---|---|
| `email` | `@NotBlank @Email @Size(max = 255)` | "Email là bắt buộc" / "Email không hợp lệ" / "Email không được vượt quá 255 ký tự" |
| `role` | `@NotBlank @Pattern(regexp = "^(STAFF|TEACHER|MANAGER)$")` | "role must be one of STAFF, TEACHER, MANAGER" (BR-STAFF-INVITE-003) |

**Error code matrix:**

| HTTP Status | Error Code | Condition | Mapping to BR |
|---|---|---|---|
| `401 Unauthorized` | `AUTH_REQUIRED` | `X-User-Id` thiếu (Gateway didn't populate) | BR-STAFF-INVITE-009 |
| `403 Forbidden` | (Spring Security default) | JWT role không phải ADMIN/OWNER/PLATFORM_ADMIN | BR-STAFF-AUTH-001 |
| `400 Bad Request` | `VALIDATION_ERROR` | email blank/sai format OR role không phải STAFF/TEACHER/MANAGER | BR-STAFF-INVITE-003 |

**Notes:**

- `token` chỉ trả về ở response create endpoint này. List endpoint (GET) **không bao giờ** trả token (BR-STAFF-INVITE-008) — Owner phải lưu link redemption ngay khi tạo.
- `expiresAt` = `now + 168h` (BR-STAFF-INVITE-002), configurable qua `kiteclass.staff-invite.invitation-ttl-hours`.
- Email được normalize lowercase + trim trước khi save (BR-STAFF-INVITE-006).

---

### GET /api/v1/staff-invitations

**Use Case:** UC-STAFF-INV-03 (Owner xem list invitations PENDING) | **Auth:** Bearer + `X-Tenant-Id` | **Role:** ADMIN, OWNER, PLATFORM_ADMIN

```json
// Request — không có body, không có query params (Wave meta-6 MVP scope: chỉ trả PENDING)

// Response 200 — ApiResponse<List<StaffInvitationResponse>>
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 42,
      "email": "tam.nguyen@gmail.com",
      "role": "TEACHER",
      "token": null,
      "status": "PENDING",
      "expiresAt": "2026-06-04T10:00:00Z",
      "invitedByUserId": 7,
      "acceptedAt": null,
      "acceptedUserId": null,
      "createdAt": "2026-05-28T10:00:00Z"
    },
    {
      "id": 43,
      "email": "mai.pham@skyedu.vn",
      "role": "STAFF",
      "token": null,
      "status": "PENDING",
      "expiresAt": "2026-06-04T11:30:00Z",
      "invitedByUserId": 7,
      "acceptedAt": null,
      "acceptedUserId": null,
      "createdAt": "2026-05-28T11:30:00Z"
    }
  ]
}
```

**Notes:**

- `token` luôn là `null` ở list response (BR-STAFF-INVITE-008 — chống leak qua admin UI logs)
- Wave meta-6 MVP scope chỉ trả status PENDING. Phase 1.5+ sẽ thêm query param `?status=ACCEPTED|EXPIRED|REVOKED|ALL` (UC-STAFF-INV-07 deferred)
- Sort: `createdAt DESC` (mới nhất trước)
- Tenant filter: defense-in-depth — Hibernate filter clamp + service-level `instanceId.equals(tenantId)` filter (BR-STAFF-INVITE-004)

**Error code matrix:**

| HTTP Status | Error Code | Condition |
|---|---|---|
| `401 Unauthorized` | `AUTH_REQUIRED` | JWT thiếu hoặc invalid |
| `403 Forbidden` | (Spring Security default) | Role không phải ADMIN/OWNER/PLATFORM_ADMIN |

---

### DELETE /api/v1/staff-invitations/{id}

**Use Case:** UC-STAFF-INV-04 (Owner hủy invitation) | **Auth:** Bearer + `X-Tenant-Id` | **Role:** ADMIN, OWNER, PLATFORM_ADMIN

```json
// Request — Path param {id} = invitation BIGSERIAL id
// Không có body

// Response 200 — ApiResponse<Void>
{
  "success": true,
  "message": "Lời mời đã bị hủy",
  "data": null
}
```

**Error code matrix:**

| HTTP Status | Error Code | Condition | Mapping to BR |
|---|---|---|---|
| `401 Unauthorized` | `AUTH_REQUIRED` | JWT thiếu | — |
| `403 Forbidden` | (Spring Security default) | Role không đủ quyền | BR-STAFF-AUTH-001 |
| `404 Not Found` | `STAFF_INVITATION_NOT_FOUND` | Invitation không tồn tại / soft-deleted / cross-tenant | BR-STAFF-INVITE-004 |
| `409 Conflict` | `STAFF_INVITATION_NOT_PENDING` | Status đã ACCEPTED/EXPIRED/REVOKED (idempotent guard) | BR-STAFF-INVITE-005 |

**Notes:**

- Idempotent guard — nếu Owner click "Hủy" lần thứ 2, server trả 409 để FE distinguish "đã hủy rồi" vs "hủy thành công vừa rồi"
- Cross-tenant revoke attempt: log warn server-side + trả 404 (KHÔNG trả 403 — chống enumeration)

---

### POST /api/v1/staff-invitations/{token}/accept

**Use Case:** UC-STAFF-INV-02 (Staff accept invitation) | **Auth:** Public (token = auth) + `X-Tenant-Id` | **Role:** Public — không cần JWT

```json
// Request — Path param {token} = UUID 128-bit redemption token
// Body — AcceptStaffInviteRequest
// Source: kiteclass-core/.../module/staff/dto/AcceptStaffInviteRequest.java
{
  "fullName": "Trần Văn Tâm",
  "password": "Tam2026abc"
}

// Response 200 — ApiResponse<AcceptStaffInviteResult>
// Source: kiteclass-core/.../module/staff/dto/AcceptStaffInviteResult.java
{
  "success": true,
  "message": "Kích hoạt tài khoản nhân viên thành công",
  "data": {
    "invitationId": 42,
    "tenantId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "tam.nguyen@gmail.com",
    "fullName": "Trần Văn Tâm",
    "role": "TEACHER",
    "acceptedAt": "2026-05-28T15:30:00Z"
  }
}
```

**Validation rules (Bean validation messages — vi):**

| Field | Constraint | Error message |
|---|---|---|
| `fullName` | `@NotBlank @Size(min = 2, max = 100)` | "Họ tên là bắt buộc" / "Họ tên phải từ 2-100 ký tự" |
| `password` | `@NotBlank @Size(min = 8, max = 128) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$")` | "Mật khẩu là bắt buộc" / "Mật khẩu phải từ 8-128 ký tự" / "Password must contain at least 1 letter + 1 digit" (BR-STAFF-PWD-002) |

**Error code matrix:**

| HTTP Status | Error Code | Condition | Mapping to BR |
|---|---|---|---|
| `404 Not Found` | `STAFF_INVITATION_NOT_FOUND` | Token sai / cross-tenant (defense in depth — chống enumeration) | BR-STAFF-ACC-001, BR-STAFF-ACC-006 |
| `400 Bad Request` | `STAFF_INVITATION_ALREADY_ACCEPTED` | Status ACCEPTED (idempotent guard — token đã được redeem trước) | BR-STAFF-ACC-002, BR-STAFF-ACC-003 |
| `400 Bad Request` | `STAFF_INVITATION_REVOKED` | Status REVOKED (Owner đã hủy) | BR-STAFF-ACC-002 |
| `400 Bad Request` | `STAFF_INVITATION_EXPIRED` | `expiresAt < now()` — Service tự flip PENDING → EXPIRED rồi trả lỗi | BR-STAFF-ACC-002 |
| `400 Bad Request` | `VALIDATION_ERROR` | password yếu / fullName invalid | BR-STAFF-PWD-001, BR-STAFF-PWD-002 |

**Notes:**

- Sau khi accept thành công, kiteclass-core trả `AcceptStaffInviteResult`. Gateway sử dụng payload này để:
  1. Hash password (kiteclass-core không touch password per BR-STAFF-PWD-003)
  2. Tạo `users` row với `userType=STAFF, referenceId=invitationId, role=<TEACHER|MANAGER|STAFF>`
  3. Mint JWT cho Staff
  4. Return JWT cho FE để auto-login
- `acceptedUserId` ở `staff_invitations` table còn null sau Wave meta-6 MVP (BR-STAFF-ACC-005); GAP-779 sẽ ship internal endpoint để Gateway write back.
- Status check order trong service: ACCEPTED → REVOKED → expired (`expiresAt`) — quan trọng cho error message clarity (BR-STAFF-ACC-002)

---

## Error Code Mapping → Business Rules

| Error Code | HTTP Status | Trigger | Mapped Business Rule | UC |
|---|---|---|---|---|
| `AUTH_REQUIRED` | 401 | `X-User-Id` thiếu (invite) hoặc JWT invalid | BR-STAFF-INVITE-009 | UC-STAFF-INV-01 |
| `VALIDATION_ERROR` | 400 | Bean validation fail (email/role/password/fullName) | BR-STAFF-INVITE-003, BR-STAFF-PWD-001/002 | UC-STAFF-INV-01, UC-STAFF-INV-02 |
| `STAFF_INVITATION_NOT_FOUND` | 404 | Invitation row missing OR cross-tenant access | BR-STAFF-INVITE-004, BR-STAFF-ACC-001, BR-STAFF-ACC-006 | UC-STAFF-INV-02, UC-STAFF-INV-04 |
| `STAFF_INVITATION_NOT_PENDING` | 409 | Revoke attempt status không phải PENDING (idempotent guard) | BR-STAFF-INVITE-005 | UC-STAFF-INV-04 |
| `STAFF_INVITATION_ALREADY_ACCEPTED` | 400 | Accept attempt status ACCEPTED | BR-STAFF-ACC-003, BR-STAFF-INVITE-005 | UC-STAFF-INV-02 |
| `STAFF_INVITATION_REVOKED` | 400 | Accept attempt status REVOKED | BR-STAFF-INVITE-005 | UC-STAFF-INV-02 |
| `STAFF_INVITATION_EXPIRED` | 400 | Accept attempt `expiresAt < now()` — service tự flip PENDING → EXPIRED | BR-STAFF-INVITE-002, BR-STAFF-INVITE-005 | UC-STAFF-INV-02 |

**Error envelope format:** `BusinessException` được handle qua global `ControllerAdvice` (kế thừa kiteclass-core common); response shape:

```json
{
  "success": false,
  "message": "Lời mời nhân viên không tồn tại",
  "errorCode": "STAFF_INVITATION_NOT_FOUND",
  "data": null
}
```

---

## DTO Schemas

### InviteStaffRequest

```java
// kiteclass-core/.../module/staff/dto/InviteStaffRequest.java
public record InviteStaffRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Pattern(regexp = "^(STAFF|TEACHER|MANAGER)$",
        message = "role must be one of STAFF, TEACHER, MANAGER") String role
) {}
```

| Field | Type | Required | Format | Description |
|---|---|---|---|---|
| `email` | string | yes | RFC 5322 email | Email nhân viên — trở thành login identifier sau khi accept |
| `role` | enum | yes | STAFF \| TEACHER \| MANAGER | Role provision khi accept (Owner role excluded) |

### AcceptStaffInviteRequest

```java
// kiteclass-core/.../module/staff/dto/AcceptStaffInviteRequest.java
public record AcceptStaffInviteRequest(
    @NotBlank @Size(min = 2, max = 100) String fullName,
    @NotBlank
    @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "Password must contain at least 1 letter + 1 digit")
    String password
) {}
```

| Field | Type | Required | Format | Description |
|---|---|---|---|---|
| `fullName` | string | yes | 2-100 ký tự, hỗ trợ VN diacritic | Họ tên hiển thị (vd: `Trần Văn Tâm`) — preserve VN diacritic per `vn-localization-audit-checklist.md` §5 |
| `password` | string | yes | 8-128 ký tự, ≥1 chữ + ≥1 số | Mật khẩu khởi tạo — strength tighten Phase 1.5 |

### StaffInvitationResponse

```java
// kiteclass-core/.../module/staff/dto/StaffInvitationResponse.java
public record StaffInvitationResponse(
    Long id,
    String email,
    String role,
    String token,                      // null trên list endpoints
    StaffInvitationStatus status,
    Instant expiresAt,
    Long invitedByUserId,
    Instant acceptedAt,
    Long acceptedUserId,                // null Wave meta-6 MVP (GAP-779 defer)
    Instant createdAt
) {}
```

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | long | no | BIGSERIAL invitation id |
| `email` | string | no | Email được mời (lowercase normalized) |
| `role` | string | no | STAFF \| TEACHER \| MANAGER |
| `token` | string | yes | 128-bit UUID — populate CHỈ ở create endpoint response; null ở list (BR-STAFF-INVITE-008) |
| `status` | enum | no | PENDING \| ACCEPTED \| EXPIRED \| REVOKED |
| `expiresAt` | Instant ISO-8601 | no | TTL boundary (UTC) |
| `invitedByUserId` | long | yes | Gateway user id của Owner đã mời |
| `acceptedAt` | Instant ISO-8601 | yes | Timestamp khi Staff redeem thành công |
| `acceptedUserId` | long | yes | Wave meta-6 MVP luôn null; GAP-779 sẽ patch sau khi Gateway tạo users row |
| `createdAt` | Instant ISO-8601 | no | BaseEntity audit field |

### AcceptStaffInviteResult

```java
// kiteclass-core/.../module/staff/dto/AcceptStaffInviteResult.java
public record AcceptStaffInviteResult(
    Long invitationId,
    UUID tenantId,
    String email,
    String fullName,
    String role,
    Instant acceptedAt
) {}
```

| Field | Type | Description |
|---|---|---|
| `invitationId` | long | Reference for Gateway to set `referenceId` trên users row |
| `tenantId` | UUID | Tenant scope cho Gateway provisioning |
| `email` | string | Login identifier cho users row |
| `fullName` | string | Display name cho users row |
| `role` | string | Role binding cho users row (TEACHER/MANAGER/STAFF) |
| `acceptedAt` | Instant ISO-8601 | Audit trail timestamp |

### StaffInvitationStatus (enum)

```java
// kiteclass-core/.../common/constant/StaffInvitationStatus.java
public enum StaffInvitationStatus {
    PENDING,    // Owner đã tạo invitation, Staff chưa accept
    ACCEPTED,   // Staff đã hoàn tất redeem flow (terminal state success)
    EXPIRED,    // TTL hết hạn — service tự flip khi accept attempt past TTL (terminal failure)
    REVOKED     // Owner đã hủy invitation (terminal failure)
}
```

**State transitions valid:** `PENDING → ACCEPTED` | `PENDING → EXPIRED` | `PENDING → REVOKED`. Không có transition nào khác.

---

## Integration Points

### Upstream (consumers of these endpoints)

1. **kiteclass-frontend** dashboard Staff page — calls Owner endpoints
2. **kiteclass-frontend** public `/staff/accept-invite/[token]` page — calls accept endpoint
3. **Gateway** `POST /api/v1/auth/register-staff/{token}` — wraps accept endpoint, then provisions Gateway users row

### Downstream (services consumed by this domain — Wave meta-6 MVP scope)

- **None.** Wave meta-6 MVP scope tự túc — Gateway tạo users row + mint JWT là responsibility riêng của Gateway. Email send chưa wire (defer).

### Future integration (Phase 1.5+)

- **kitehub-email** service — async send invitation email qua RabbitMQ outbox (per `design-patterns.md` §3.5.1 Outbox pattern)
- **Gateway internal endpoint** — write back `acceptedUserId` sau khi tạo users row (GAP-779)

---

## Smoke Test Examples

### Owner invite Staff (curl)

```bash
# Pre-condition: Owner đã login, có JWT
OWNER_JWT="eyJ..."
TENANT_ID="550e8400-e29b-41d4-a716-446655440000"

curl -X POST https://api.kitehub.me/api/v1/staff-invitations \
  -H "Authorization: Bearer $OWNER_JWT" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tam.nguyen@gmail.com",
    "role": "TEACHER"
  }'

# Expected: HTTP 201 + token field populated
```

### Staff accept invitation (curl)

```bash
TOKEN="<token from invite response>"
TENANT_ID="550e8400-e29b-41d4-a716-446655440000"

curl -X POST https://api.kitehub.me/api/v1/staff-invitations/$TOKEN/accept \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Trần Văn Tâm",
    "password": "Tam2026abc"
  }'

# Expected: HTTP 200 + AcceptStaffInviteResult với acceptedAt populated
```

---

## Compliance Check (per `business-logic-review.md` §2.4)

| Compliance area | Verdict | Rationale |
|---|---|---|
| **PDPL 2023** (Personal Data Protection Law) | **Considered — N/A specific obligation** | Staff identity = tenant employee record, không phải end-user PII consent flow. Email + fullName là minimum onboarding data; password hashed bởi Gateway. Không trigger Art 23 retention (Staff record stays cùng tenant lifecycle). |
| **Cybersecurity Law 2018 + Decree 53/2022** | **Considered — N/A specific obligation** | Token + password chuyển qua HTTPS TLS 1.2+ (Gateway terminate); không có log password plaintext (BR-STAFF-PWD-003). |
| **Education Law 2019** | **N/A** | Staff invitation flow không touch student data; TEACHER role grant scope không liên quan student-level access. |
| **Consumer Protection Law 2023** | **N/A** | Staff = employee, không phải consumer; refund / dispute scope không apply. |

**Reviewer:** @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev, 2026-05-28). Formal legal counsel review N/A — Staff onboarding không trigger regulated area specific. Re-review Phase 1.5 nếu staff scope mở rộng access financial reports / student grades (lúc đó touch Education Law + PDPL Art 16 children data).

**Review cadence:** Quarterly. **Next review:** 2026-08-28. Event triggers: regulator amend on employee data retention OR scope mở rộng (vd staff role có access student record).
