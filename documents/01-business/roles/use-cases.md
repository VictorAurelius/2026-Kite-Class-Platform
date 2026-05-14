# RBAC Roles — Use Cases

**Domain:** Tenant role separation (OWNER + STAFF) + migration from existing PLATFORM_ADMIN
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)

> **Wave 79 Bucket 0 status:** 4 use cases dưới describe target Bucket B implementation. Live shipment Wave 79 Bucket B (GAP-562).

---

## UC-ROLE-OWNER-SCOPED-DASHBOARD — Owner user logged in, sees full Customer scope

**Actor:** User với role `OWNER` (sau Wave 79 migration; trước đó users với `PLATFORM_ADMIN` BE seed + `ADMIN` FE guard).
**Trigger:** Login thành công (password + 2FA nếu enable) → JWT issued với claim `role=OWNER`.
**Rules:** BR-ROLE-001 (2-role scope), BR-ROLE-002 (alias 30 ngày), BR-ROLE-003 (STAFF restriction inverse).

### Happy path

1. User nhập credentials trên `/login` → `POST /api/auth/login` returns access JWT với `role=OWNER` claim.
2. FE store JWT, decode claim, set role context global.
3. FE redirect `/dashboard` (Owner home).
4. Sidebar nav hiển thị FULL menu:
   - Dashboard
   - Học sinh / Lớp / Lịch học (KiteClass core)
   - Cài đặt → Billing / Subscription
   - Cài đặt → Branding / AI Branding
   - Cài đặt → Staff (invite/revoke)
   - Cài đặt → Domain / DNS
5. Mọi click vào privileged route (vd `/dashboard/billing`) load page hợp lệ (BE return 200 vì `@PreAuthorize("hasRole('OWNER')")` pass).
6. FE display banner welcome (one-time per session).

### Migration case (existing PLATFORM_ADMIN/ADMIN users)

Within 30-ngày window (until 2026-06-14):
1. User login với existing credentials.
2. BE issue JWT với `role=PLATFORM_ADMIN` (original claim) — Wave 79 Bucket B Flyway V46 migrate `users.role='PLATFORM_ADMIN'` → `OWNER` for tenant scope users; cross-tenant superuser giữ `PLATFORM_ADMIN`.
3. FE `RoleGuard` accept cả `PLATFORM_ADMIN` / `ADMIN` / `OWNER` cho cùng Owner UI (per BR-ROLE-002 alias).
4. Banner top dashboard: "Hệ thống đã cập nhật role lên 'OWNER'. Vui lòng logout + login lại để áp dụng" (one-time after V46 migration).
5. Sau 90 ngày: alias removed; users với stale JWT `PLATFORM_ADMIN` cho tenant scope → 401 force re-login (rotate refresh token earlier).

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 1 | Password sai | 401 | `INVALID_CREDENTIALS` | Inline error |
| 1 | Account locked | 401 | `ACCOUNT_LOCKED` | Banner + CTA support |
| 5 | BE 403 trên privileged route (role mismatch) | 403 | `FORBIDDEN` | Redirect `/dashboard` + toast "Bạn không có quyền truy cập trang này" |

---

## UC-ROLE-STAFF-SCOPED-DASHBOARD — Staff user logged in, sees restricted scope

**Actor:** User với role `STAFF` (created via invite-staff flow Wave 79 Bucket B).
**Trigger:** Login thành công → JWT issued với `role=STAFF`.
**Rules:** BR-ROLE-003 (STAFF scope restrictions), BR-ROLE-006 (matrix BE+FE parity).

### Happy path

1. User nhập credentials → `POST /api/auth/login` returns JWT với `role=STAFF`.
2. FE redirect `/dashboard` (same path; nav hidden differs).
3. Sidebar nav hiển thị RESTRICTED menu:
   - Dashboard ✅
   - Học sinh / Lớp / Lịch học ✅
   - Cài đặt → Profile (own profile only)
   - Cài đặt → 2FA (own)
   - (NO Billing, NO Branding, NO Staff management, NO Domain)
4. Direct URL access `/dashboard/billing` → FE `RoleGuard` block + redirect `/dashboard` + toast "Trang này chỉ dành cho Chủ tenant".
5. Even nếu user paste URL từ Owner session, BE returns 403 (defense in depth — gateway path filter + `@PreAuthorize("hasRole('OWNER')")` annotation).

### Data isolation check

STAFF login as `tenant-A` user:
- GET `/api/v1/students` → only `tenant-A` student rows returned (per existing `@TenantSecurity` interceptor).
- Switch to `tenant-B` is NOT possible cho STAFF (Phase 1: each user single tenant; multi-tenant for staff defer Wave 80+).

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 4 | FE RoleGuard catch privileged URL | (FE-only) | — | Redirect + toast |
| 5 | BE 403 confirms FE guard | 403 | `FORBIDDEN` | Same redirect (defense in depth) |
| (multi) | STAFF attempt cross-tenant via tampered JWT | 401 | `INVALID_TOKEN` | Force logout (per `pre-launch-auth-hardening-checklist.md` §2.8 refresh blacklist) |

---

## UC-ROLE-STAFF-INVITE — Owner invite new staff member

**Actor:** OWNER (cannot be PLATFORM_ADMIN cross-tenant; cannot be STAFF — per BR-ROLE-004).
**Trigger:** Settings → Staff → "Mời nhân viên" → submit email + name.
**Endpoint:** `POST /api/v1/staff-invitations` (Wave 79 Bucket B target)
**Rules:** BR-ROLE-004 (Owner-only invite), BR-ROLE-002 (role assigned `STAFF`).

### Happy path

1. Owner fill modal: `{ email: "staff@trungtam.edu.vn", fullName: "Nguyễn Văn A" }`.
2. FE POST `/api/v1/staff-invitations` với Owner access token.
3. BE verify `@PreAuthorize("hasRole('OWNER')")`.
4. BE validate:
   - Email unique trong tenant scope (no duplicate active invite + no existing user).
   - Tenant STAFF count < `kitehub.rbac.staff-max-per-tenant` (50 default).
5. BE insert `staff_invitations` row với `(id, tenant_id, email, full_name, token, expires_at, status=PENDING, invited_by)`.
6. BE generate signed JWT invite token (TTL 7 ngày per BR-ROLE-004), embed `invitation_id` + `tenant_id` claims.
7. BE emit `staff.invitation.created` outbox event → kitehub-email subscriber sends template `invite-staff.ftl` đến `staff@trungtam.edu.vn` với link `https://kitehub.me/staff/accept?token=<jwt>`.
8. BE return 201 với invitation summary (NO token in response — only email link).
9. FE refresh staff list (status=Đang chờ chấp nhận).

### Staff accepts invitation

10. Recipient mở email → click link → FE `/staff/accept?token=...` page.
11. FE call `GET /api/v1/staff-invitations/{token}` để fetch invitation details (tenant name, inviter, role).
12. Recipient set password + confirm name → POST `/api/v1/staff-invitations/{token}/accept` với `{ password, fullName }`.
13. BE validate token (signature + exp), validate password complexity (per `pre-launch-auth-hardening-checklist.md` §2.3).
14. BE create User row `(email, password_hash, role=STAFF, tenant_id, full_name, status=ACTIVE)`, update invitation status=ACCEPTED.
15. BE issue access + refresh token; auto-login.
16. FE redirect `/dashboard` (STAFF scope).

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 3 | Caller không phải OWNER | 403 | `FORBIDDEN` | Toast "Chỉ Chủ tenant có thể mời nhân viên" |
| 4 | Email đã active trong tenant | 409 | `EMAIL_ALREADY_INVITED` | Inline error "Email này đã được mời / đã là staff" |
| 4 | Tenant đạt STAFF cap (50) | 422 | `STAFF_CAP_REACHED` | Toast "Đã đạt giới hạn 50 nhân viên. Liên hệ hỗ trợ" |
| 11 | Token expired (>7 ngày) | 410 | `INVITATION_EXPIRED` | Page hiển thị "Lời mời đã hết hạn. Liên hệ Chủ tenant để được mời lại" |
| 11 | Token invalid (tampered/wrong signature) | 401 | `INVALID_TOKEN` | Page hiển thị "Link không hợp lệ" |
| 12 | Invitation đã ACCEPTED (replay) | 409 | `INVITATION_ALREADY_USED` | "Lời mời đã được sử dụng" + CTA "Quay lại đăng nhập" |
| 13 | Password không đủ phức tạp | 400 | `WEAK_PASSWORD` | Inline error với hint requirements |

### FE behavior notes

- Staff invite modal có dropdown role: hiện ẨN (chỉ STAFF available Phase 1 per BR-ROLE-005). Phase 2+ unlock Manager/Teacher.
- Staff list page hiển thị: Email | Tên | Vai trò | Trạng thái (Active / Đang chờ chấp nhận / Hết hạn) | Mời lại (button) | Thu hồi (button).
- Resend invite: `POST /api/v1/staff-invitations/{id}/resend` — same email, new token, reset TTL 7 ngày.
- Revoke invite/staff: `DELETE /api/v1/staff-invitations/{id}` — invalidate token (if PENDING) hoặc disable user (if ACTIVE → status=DISABLED).

---

## UC-ROLE-MIGRATION — Existing PLATFORM_ADMIN/ADMIN users migrate sang OWNER

**Actor:** System (Flyway migration V46 + alias fallback runtime); affected users là beta tenant với existing role.
**Trigger:** Wave 79 Bucket B deploy → V46 runs.
**Rules:** BR-ROLE-002 (backward-compat alias 30 ngày).

### Migration flow (one-time at V46 apply)

1. V46 migration SQL:
   - Add new `users.role` enum values `OWNER`, `STAFF` (keep existing `PLATFORM_ADMIN` for cross-tenant superuser).
   - UPDATE `users SET role = 'OWNER' WHERE role = 'PLATFORM_ADMIN' AND tenant_id IS NOT NULL` (tenant-scoped users).
   - PLATFORM_ADMIN giữ nguyên cho `tenant_id IS NULL` (cross-tenant superuser).
2. BE deploy: `RoleHierarchy` Spring Security bean wired với alias mapping `PLATFORM_ADMIN > OWNER > STAFF`.
3. FE deploy: `RoleGuard` accept `['PLATFORM_ADMIN', 'ADMIN', 'OWNER']` cho cùng Owner UI surface.

### Runtime alias window (30 ngày — until 2026-06-14)

Active sessions (refresh token chưa expire) với JWT claim `role=PLATFORM_ADMIN` (issued trước V46):
- BE accept cho `@PreAuthorize("hasRole('OWNER')")` checks (alias hierarchy).
- FE `RoleGuard` accept cả 3 strings.
- Banner one-time: "Role đã cập nhật. Logout + login để dùng tên role mới ('OWNER')."

After 30 ngày (post 2026-06-14):
- BE log WARN cho mọi JWT có `role=PLATFORM_ADMIN` + `tenant_id IS NOT NULL` (stale claim).
- FE alias removed; users với stale JWT redirected `/login` với toast "Vui lòng đăng nhập lại để cập nhật".

After 90 ngày (2026-08-14):
- Alias removed entirely; stale JWT users force-logout 401.

### Error branches / edge cases

| Case | Behavior |
|------|----------|
| Tenant user logged in suốt 30 ngày không re-login | Sau 90 ngày: refresh token attempt → 401 force re-login → re-issue JWT với `role=OWNER` |
| User active across multiple devices | Each device's JWT refresh independent; eventual consistency |
| PLATFORM_ADMIN cross-tenant superuser | KHÔNG migrate (giữ `PLATFORM_ADMIN` role); FE `/admin/*` routes vẫn check `hasRole('PLATFORM_ADMIN')` exclusively |

### FE behavior notes

- Migration banner ship 2026-05-14 → 2026-06-14 (cố định top dashboard cho Owner-aliased users).
- Documentation banner link → `/help/anonymous/role-migration` MDX page (Wave 79 Bucket F1 cluster — anonymous persona user manual hoặc Wave 80+ proper Owner manual).
