# Tenant Auth (KC-native login) — Use Cases

**Domain:** KiteClass Core / Tenant Auth
**Version:** 1.0 (Wave auth-1 — Option B, GAP-725/798)
**Updated:** 2026-06-06
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/auth/`

---

## UC-AUTH-01 — Parent/Teacher/Student login (KC-native)

**Actor:** PARENT / TEACHER / STUDENT (đã có credential trong `auth_credentials`)
**Precondition:** Credential đã được provision (parent qua redeem, teacher/student qua admin set-password); `enabled = true`.
**Trigger:** User submit email + password trên trang login portal.

**Main flow:**
1. FE → `POST /api/v1/tenant-auth/login` body `{ "email": "...", "password": "..." }` qua gateway `:9000`.
2. Gateway: route `kc-tenant-auth` là public — KHÔNG chạy JWT auth filter, KHÔNG TenantResolver (login pre-auth, chưa có tenant context). Áp rate-limit IP-keyed (`replenishRate=3 / burst=5`, BR-AUTH-LOGIN-004).
3. Core `AuthService.login`: trim email → `findByEmailIgnoreCase` → filter `enabled` → filter `BCrypt.matches(password, hash)`.
4. Khớp → `AuthTokenService.mintAccessToken`: HS512 JWT với claims `sub/role/email/tenantId/referenceId/type/iat/exp` (BR-AUTH-JWT-003), TTL 12h.
5. Core trả `200` `ApiResponse<LoginResponse{accessToken, tokenType="Bearer", expiresInSeconds=43200, role, referenceId, tenantId}>`.
6. FE lưu token, dùng cho các request portal tiếp theo (`Authorization: Bearer <token>`).

**Downstream (request portal có token):**
7. FE → `GET /api/v1/parent/me/children` (hoặc `/students/me/...`) kèm `Authorization: Bearer <token>`.
8. Gateway: validate JWT (HS512, shared key) → strip client `X-User-Reference-Id` (BR-AUTH-HDR-001) → re-inject `X-User-Reference-Id = referenceId claim` (BR-AUTH-HDR-002) + `X-User-Id`/`X-User-Roles`/`X-User-Email` + resolved `X-Tenant-Id`.
9. Core: đọc `X-User-Reference-Id` như identity verified → reference-id authz (`@authz.hasAccessToChild`, GAP-798).

**Error flow:**
| Tình huống | Kết quả |
|---|---|
| Email không tồn tại | `401 INVALID_CREDENTIALS` (uniform — không enumeration, BR-AUTH-LOGIN-001) |
| Credential `enabled=false` | `401 INVALID_CREDENTIALS` |
| Sai password | `401 INVALID_CREDENTIALS` |
| Email/password thiếu hoặc email sai format | `400` bean-validation error |
| Vượt rate-limit | `429 Too Many Requests` (gateway) |

**FE behavior:** Hiển thị 1 message lỗi chung "Email hoặc mật khẩu không đúng" cho mọi 401 (không phân biệt nguyên nhân, đồng bộ no-enumeration). Token hết hạn (12h) → redirect login.

**Walk-verified (KC-8 G3, gateway `:9000`, real JWT only):** login 200 (HS512 + claims) → `/parent/me/children` 200 → fees facet 200 → IDOR child2 403 → anti-spoof forged ref-id stripped.

---

## UC-AUTH-02 — Admin set/reset teacher login password (Hướng B)

**Actor:** OWNER / ADMIN / PRINCIPAL (đã đăng nhập, tenant-scoped)
**Precondition:** Teacher đã tồn tại trong tenant (`teachers.id`); teacher có `email`.
**Trigger:** Admin đặt hoặc reset mật khẩu đăng nhập cho giáo viên.

**Main flow:**
1. Admin FE → `POST /api/v1/teachers/{id}/credentials` body `{ "password": "..." }` kèm `Authorization: Bearer <admin token>`.
2. Gateway: validate admin JWT → forward (route tenant-scoped, có TenantResolver → `X-Tenant-Id`).
3. Core `TeacherController.setTeacherCredential`: `@PreAuthorize("hasAnyRole('OWNER','ADMIN','PRINCIPAL')")` gate.
4. `TeacherServiceImpl.provisionCredential`: load teacher theo id (404 nếu không thấy) → lấy `teacher.email` + `tenantId` từ `TenantContext`.
5. `AuthCredentialProvisioningService.setPassword(ROLE_TEACHER, teacher.id, teacher.email, tenantId, rawPassword)`: UPSERT credential (rotate password nếu đã tồn tại, BR-AUTH-PROV-003).
6. Core trả `200` `ApiResponse<Void>` message "Đặt mật khẩu giáo viên thành công".
7. Teacher giờ login được qua UC-AUTH-01 với email + password vừa đặt.

**Error flow:**
| Tình huống | Kết quả |
|---|---|
| Caller không phải OWNER/ADMIN/PRINCIPAL | `403` (PreAuthorize) |
| Password yếu (< 8 ký tự / thiếu chữ/số/ký tự đặc biệt) | `400` bean-validation (BR-AUTH-PROV-005) |
| Teacher id không tồn tại | `404` teacher not found |

**FE behavior:** Form đặt mật khẩu trong trang quản lý giáo viên (admin). Sau 200 → toast thành công + hint giáo viên dùng email + password mới để đăng nhập.

---

## UC-AUTH-03 — Parent credential provisioning qua redeem invitation

**Actor:** PARENT (người được mời, chưa có tài khoản)
**Precondition:** `ParentInvitation` `PENDING` còn hạn (BR-PARENT-003); `PARENT_PORTAL_ENABLED = true` (BR-PARENT-004).
**Trigger:** Parent mở link mời + đặt mật khẩu lần đầu.

**Main flow:**
1. FE → `POST /api/v1/parent-invitations/redeem/{token}` body chứa `fullName` + `password` (+ profile fields). Endpoint contract không đổi — xem `parent-portal/api-contract.md`.
2. Core `ParentInvitationServiceImpl.redeem` (1 transaction):
   - Validate token (còn hạn, PENDING, đúng tenant).
   - Tạo/activate `Parent` row (`PENDING → ACTIVE`) + `ParentStudentLink`.
   - **Side-effect mới (Wave auth-1):** `AuthCredentialProvisioningService.provisionParent(parentId, invitation.email, instanceId, password)` — provision credential idempotent-on-email (BR-AUTH-PROV-002), atomic cùng parent+link (BR-AUTH-PROV-001).
3. Core trả thành công (response shape redeem không đổi).
4. Parent login được ngay qua UC-AUTH-01 với `invitation.email` + password vừa đặt.

**Lưu ý idempotent:** Parent redeem invite của con thứ 2 (cùng email) → credential cũ giữ nguyên (password gốc thắng, không rotate). Đảm bảo 1 parent = 1 credential.

**Error flow:** Theo `parent-portal/use-cases.md` UC-PARENT-02 (token expired/invalid → 404/410; portal disabled → 503 `PARENT_PORTAL_DISABLED`).

**FE behavior:** Trang redeem (đặt mật khẩu lần đầu) → sau thành công → redirect trang login portal phụ huynh.

---

## Related

- `parent-portal/use-cases.md` — UC-PARENT-02 (redeem chi tiết), UC-PARENT-03/04 (self-service portal sau login).
- `student-portal/use-cases.md` — UC student portal đọc dữ liệu sau login.
- `tenant-auth/rules.md` — BR-AUTH-* (credential / JWT / provisioning / anti-spoof).
- `tenant-auth/api-contract.md` — endpoint shapes + error tables.
- GAP-725, GAP-798/798b, GAP-1012, GAP-1009.
