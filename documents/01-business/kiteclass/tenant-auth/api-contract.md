# Tenant Auth (KC-native login) — API Contract

**Domain:** KiteClass Core / Tenant Auth
**Version:** 1.0 (Wave auth-1 — Option B, GAP-725/798)
**Updated:** 2026-06-06
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/auth/`

---

## 1. AuthController — `/api/v1/tenant-auth`

KC-native login cho role tenant-scoped (PARENT/TEACHER/STUDENT). Route `/api/v1/tenant-auth/**` là **public** (gateway forward không qua JWT auth filter, không TenantResolver). Tách hoàn toàn khỏi `/api/v1/auth/**` (KiteHub subscription, OWNER/STAFF) — không collision.

### POST /api/v1/tenant-auth/login
**Use Case:** UC-AUTH-01  |  **Auth:** Public (no token)  |  **Role:** —  |  **Rate-limit:** 3/5 IP-keyed (GAP-1012)

```json
// Request — LoginRequest
{ "email": "string (required, @Email)", "password": "string (required)" }
// Response 200 — ApiResponse<LoginResponse>
{
  "success": true,
  "data": {
    "accessToken": "string (HS512 JWT)",
    "tokenType": "Bearer",
    "expiresInSeconds": 43200,
    "role": "PARENT | TEACHER | STUDENT",
    "referenceId": "long (= auth_credentials.entity_id)",
    "tenantId": "string (UUID = instance_id)"
  }
}
```

**JWT claims (trong `accessToken`):** `sub` = user_uuid, `role` = entity_type, `email`, `tenantId` = instance_id, `referenceId` = entity_id, `type` = "access", `iat`, `exp` (+12h). Ký HS512 bằng shared `JWT_SECRET` (BR-AUTH-JWT-001/002/003).

| Status | Code | Message | Nguyên nhân |
|--------|------|---------|-------------|
| 401 | INVALID_CREDENTIALS | "INVALID_CREDENTIALS" | Email không tồn tại / credential disabled / sai password — **uniform, no user-enumeration** (BR-AUTH-LOGIN-001) |
| 400 | VALIDATION_ERROR | "Email is required" / "Email format invalid" / "Password is required" | Bean-validation `LoginRequest` |
| 429 | — | Too Many Requests | Vượt rate-limit gateway (BR-AUTH-LOGIN-004) |

**Ghi chú HTTP status:** trả `200` (không `201`) — login không tạo resource. Error envelope dùng `ErrorResponse{code, message, path}` qua `GlobalExceptionHandler` (chuẩn nội bộ kiteclass-core, KHÔNG phải RFC7807).

---

## 2. TeacherController — credential provisioning

Bổ sung Wave auth-1 (Hướng B). Endpoint đầy đủ teacher xem `teacher/api-contract.md`; phần này chỉ ghi endpoint credential mới.

### POST /api/v1/teachers/{id}/credentials
**Use Case:** UC-AUTH-02  |  **Auth:** Bearer token  |  **Role:** OWNER, ADMIN, PRINCIPAL

```json
// Request — SetPasswordRequest
{ "password": "string (required, 8-100 chars, regex: letter + digit + special)" }
// Response 200 — ApiResponse<Void>
{ "success": true, "data": null, "message": "Đặt mật khẩu giáo viên thành công" }
```

Provision/UPSERT credential `auth_credentials` (entity_type=TEACHER, entity_id=teacher.id, email=teacher.email, instance_id=tenant). Idempotent set-password → rotate nếu đã tồn tại (BR-AUTH-PROV-003). Email + role lấy từ teacher entity (request chỉ mang password).

| Status | Code | Message | Nguyên nhân |
|--------|------|---------|-------------|
| 200 | — | "Đặt mật khẩu giáo viên thành công" | Thành công (set hoặc reset) |
| 403 | — | Forbidden | Caller không phải OWNER/ADMIN/PRINCIPAL (`@PreAuthorize`) |
| 400 | VALIDATION_ERROR | "Mật khẩu phải từ 8-100 ký tự" / "Mật khẩu phải có chữ, số và ký tự đặc biệt" | Bean-validation `SetPasswordRequest` (BR-AUTH-PROV-005) |
| 404 | TEACHER_NOT_FOUND | "Teacher not found" | Teacher id không tồn tại trong tenant |

**Ghi chú HTTP status:** trả `200` (không `201`) — UPSERT idempotent, không trả resource URI.

---

## 2b. StudentController — credential provisioning (KC-9, GAP-1277)

Bổ sung KC-9 student-auth (Hướng B, mirror teacher). Endpoint đầy đủ student xem `student/api-contract.md`; phần này chỉ ghi endpoint credential mới. KHÔNG dùng Zalo/SMS OTP — password-based như parent/teacher.

### POST /api/v1/students/{id}/credentials
**Use Case:** UC-AUTH-02 (student variant)  |  **Auth:** Bearer token  |  **Role:** OWNER, ADMIN, PRINCIPAL, TEACHER

```json
// Request — SetPasswordRequest
{ "password": "string (required, 8-100 chars, regex: letter + digit + special)" }
// Response 200 — ApiResponse<Void>
{ "success": true, "data": null, "message": "Đặt mật khẩu học sinh thành công" }
```

Provision/UPSERT credential `auth_credentials` (entity_type=STUDENT, entity_id=student.id, email=student.email, instance_id=tenant). Idempotent set-password → rotate nếu đã tồn tại (BR-AUTH-PROV-003). Email + role lấy từ student entity (request chỉ mang password). Owner/teacher provision (teacher được phép vì quản học sinh của lớp mình).

| Status | Code | Message | Nguyên nhân |
|--------|------|---------|-------------|
| 200 | — | "Đặt mật khẩu học sinh thành công" | Thành công (set hoặc reset) |
| 403 | — | Forbidden | Caller không phải OWNER/ADMIN/PRINCIPAL/TEACHER (`@PreAuthorize`) |
| 400 | VALIDATION_ERROR | "Mật khẩu phải từ 8-100 ký tự" / "Mật khẩu phải có chữ, số và ký tự đặc biệt" | Bean-validation `SetPasswordRequest` (BR-AUTH-PROV-005) |
| 400 | STUDENT_EMAIL_REQUIRED | — | Student không có email → không thể provision login email-keyed |
| 404 | STUDENT_NOT_FOUND | "Student not found" | Student id không tồn tại trong tenant |

**Soft-delete:** xóa student → `disableCredential(STUDENT, id)` revoke login (parity teacher GAP-1013b).

**Login:** student dùng chung `POST /api/v1/tenant-auth/login` (entity_type=STUDENT đã hợp lệ trong V89 CHECK + AuthService/AuthTokenService role-agnostic) → JWT `role=STUDENT`.

---

## 2c. Auto-provision login at create / bulk-import (Wave flow-kc3, GAP-1124 / GAP-1277)

Bổ sung **opt-in auto-provision**: thay vì 2 bước (tạo entity → gọi `POST .../credentials`), caller có thể cấp login NGAY lúc tạo bằng cách kèm `initialPassword`. KHÔNG đổi design Hướng B (entity tách credential) — chỉ AUTO-gọi `AuthCredentialProvisioningService.setPassword(...)` trong cùng transaction tạo entity. Vắng `initialPassword` → hành vi cũ y nguyên (KHÔNG tạo credential).

### POST /api/v1/teachers — create teacher (+ optional initialPassword)
Field optional `initialPassword` trong `CreateTeacherRequest`. Khi present → provision `auth_credentials` (entity_type=TEACHER, entity_id=teacher.id, email=teacher.email) cùng transaction. Validate theo `AuthPasswordPolicy` (8-100 ký tự + chữ hoa/thường/số/đặc biệt) — chỉ khi non-null (Bean Validation bỏ qua null).

### POST /api/v1/students — create student (+ optional initialPassword)
Field optional `initialPassword` trong `CreateStudentRequest`. Khi present → provision (entity_type=STUDENT). Login email-keyed → student KHÔNG có email + có `initialPassword` ⇒ HTTP 400 `STUDENT_EMAIL_REQUIRED` (fail loud, KHÔNG nuốt).

### POST /api/v1/students/bulk-import/commit — bulk import (+ optional initialPassword)
Form field optional `initialPassword` (batch-level, KHÔNG phải cột trong xlsx). Khi present + hợp lệ → mỗi học sinh tạo thành công CÓ email được provision login cùng batch. Response `BulkImportResult.credentialsProvisioned` = số credential đã cấp (≤ `successCount`; 0 cho preview + commit không kèm password). Validate password 1 lần ở batch-level → invalid ⇒ HTTP 400 `BULK_IMPORT_INVALID_PASSWORD` (trước mọi DB write). Provision-fail 1 dòng (vd email cross-tenant) → ghi row error, KHÔNG hủy create + KHÔNG abort chunk.

| Status | Code | Nguyên nhân |
|--------|------|-------------|
| 400 | VALIDATION_ERROR | `initialPassword` không đạt `AuthPasswordPolicy` (create teacher/student — bean-validation) |
| 400 | STUDENT_EMAIL_REQUIRED | create student có `initialPassword` nhưng student không email |
| 400 | BULK_IMPORT_INVALID_PASSWORD | bulk-import batch `initialPassword` không đạt `AuthPasswordPolicy` |

**Phase-2 enhancement (NGOÀI scope hiện tại):** random-per-student password + force-reset-on-first-login + teacher email-invite self-serve full-flow (GAP-1124) + FE student-shell (GAP-1277).

---

## 3. Anti-Spoof Header Contract (Gateway)

`X-User-Reference-Id` là header **gateway-only-trusted** (giống `X-User-Id`):

| Bước | Hành vi | Code |
|------|---------|------|
| 1 | Gateway **strip** mọi `X-User-Reference-Id` client gửi lên | `application.yml` `default-filters: RemoveRequestHeader=X-User-Reference-Id` (BR-AUTH-HDR-001) |
| 2 | Gateway **re-inject** `X-User-Reference-Id` từ claim `referenceId` của JWT đã verify (chỉ khi `!isChallenge` + claim non-null) | `JwtAuthenticationGatewayFilter.java` (BR-AUTH-HDR-002) |
| 3 | Core đọc header như identity verified — client KHÔNG set được trực tiếp | reference-id authz `@authz.hasAccessToChild` (GAP-798) |

**Note cho consumer:** Token OWNER/STAFF không mang `referenceId` → header vắng. Token KC-native (PARENT/TEACHER/STUDENT) luôn mang → header có. Client-supplied value LUÔN bị bỏ.

---

## 4. Endpoint Index

| Method | Path | Use Case | Auth | Rate-limit | Visibility |
|--------|------|----------|------|-----------|------------|
| POST | `/api/v1/tenant-auth/login` | UC-AUTH-01 | Public | 3/5 IP-keyed | Public (Tag: Tenant Auth) |
| POST | `/api/v1/teachers/{id}/credentials` | UC-AUTH-02 | OWNER/ADMIN/PRINCIPAL | (teacher route) | Public Swagger (Tag: Teacher) |
| POST | `/api/v1/students/{id}/credentials` | UC-AUTH-02 | OWNER/ADMIN/PRINCIPAL/TEACHER | (student route) | Public Swagger (Tag: Student) |

---

## 4.5 Error envelope (GAP-1337 cross-service)

kiteclass-core trả lỗi theo DTO custom `ErrorResponse` (`application/json`) — **khác** kitehub-subscription dùng RFC 7807 `ProblemDetail` (`application/problem+json`). FE phải parse 2 shape tùy service được gọi.

**kiteclass `ErrorResponse` shape** (`GlobalExceptionHandler` → `ErrorResponse.of(code, message, path)`):
```json
{ "success": false, "code": "VALIDATION_ERROR", "message": "...",
  "path": "/api/v1/...", "timestamp": "2026-06-14T08:00:00Z",
  "fieldErrors": { "email": ["must be a valid email"] } }
```

`code` ổn định cho FE branch (vd `TENANT_NOT_SET`, `ACCESS_DENIED`, `RESOURCE_NOT_FOUND`, `METHOD_NOT_ALLOWED`, `VALIDATION_ERROR`). `fieldErrors` chỉ có ở validation 400.

Quyết định canonical + lộ trình hợp nhất (target RFC 7807, migration deferred Phase 1): xem `kitehub/subscription-billing/api-contract.md` §"Error envelope (cross-service contract)" + GAP-1337 (PARTIAL).

---

## 5. Related

- `tenant-auth/rules.md` — BR-AUTH-* (credential / JWT / login / provisioning / anti-spoof).
- `tenant-auth/use-cases.md` — UC-AUTH-01/02/03.
- `teacher/api-contract.md` — full teacher endpoints.
- `parent-portal/api-contract.md` + `student-portal/api-contract.md` — reference-id source (Option B).
- GAP-725, GAP-798/798b, GAP-705, GAP-711, GAP-1012, GAP-1009.
- Audit: `documents/04-quality/audits/api-contract/2026-06-06-wave-auth-1-api-contract.md`.
