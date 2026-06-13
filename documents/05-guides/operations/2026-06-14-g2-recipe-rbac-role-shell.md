---
title: G2 Human Test Recipe — RBAC role-shell (role-redirect + RoleGuard + assign-UI + STAFF scope)
audience: dev
created: 2026-06-14
scope: Flow Verification Campaign G2 handoff — RBAC foundation (GAP-1119 role routing + Bucket D assign-UI, GAP-1277 KC-9 student-auth, GAP-1274 STAFF @PreAuthorize coverage)
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1119-kc-role-based-login-routing-rbac-shell.md
  - documents/04-quality/gaps/phase-1-beta/GAP-1277-kc9-student-auth-be.md
  - documents/04-quality/gaps/phase-1-beta/closed/GAP-1274-kc-staff-role-no-preauthorize-coverage.md
  - .claude/rules/g2-handoff-md-mandate.md
  - .claude/rules/kitehub-kiteclass-boundary.md
---

# G2 Recipe — RBAC role-shell (KiteClass `:3000`)

> **Đây là KiteClass (KC) — FE port `:3000`** (`kiteclass-frontend`), per `kitehub-kiteclass-boundary.md` §2. KHÔNG nhầm với KiteHub `:3001`.

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Bạn tự test tầng nền RBAC của KiteClass:
- (a) Login mỗi role → redirect đúng **role-home** (OWNER/ADMIN/STAFF → `/dashboard`, TEACHER → `/teacher`, PARENT → `/parent`, STUDENT → `/student`).
- (b) **RoleGuard** chặn route ngoài quyền (teacher mở `/admin/roles` → bị chặn, không vào được).
- (c) Owner mở `/admin/roles` → liệt kê 5 role template + **gán/thu hồi** role cho user.
- (d) **STAFF scope** (GAP-1274 đã DONE — walk để yên tâm): STAFF vào được enrollment/attendance/invoice nhưng **KHÔNG** vào payroll/branding (403).

**Prereq:**
- Stack UP (rebuild `kiteclass-core` + `kiteclass-frontend`).
- Tenant `sky-education`, Owner đã seed: `owner@skyedu.vn` / `SkyEdu@2026`.
- TEACHER / STUDENT credential **provision trong Setup §2.2** (chưa seed sẵn cho non-owner role).

**Thời lượng:** ~15-18 phút.

## 2. Setup

### 2.1 Stack up + verify healthy

```bash
cd /home/kitedev/projects/2026-Kite-Class-Platform
bash kitehub/scripts/up.sh                 # rebuild kc-core + kc-frontend + gateway + infra
bash kitehub/scripts/status.sh             # tất cả Up + healthy
```

- Browser + DevTools → tab Network (filter `roles` / `tenant-auth`) + tab Console.
- URL KiteClass dashboard: **http://localhost:3000**

### 2.2 Provision login credential cho TEACHER + STUDENT (BE-only setup — KHÔNG phải bước G2 walk)

> Non-owner role (teacher/student) chưa seed sẵn login account → cần provision trước. Đây là **Setup BE-only** (provisioning API không có FE owner-facing surface trong scope này) — bước WALK thật là **login qua browser** ở §3.

**Bước Setup-A — lấy `studentId` + `teacherId` thật của tenant** (qua psql; điều chỉnh tên bảng/cột nếu schema khác):

```bash
docker exec kite-postgres psql -U kite -d kiteclass_shared -c \
"SET app.current_tenant='<tenant-uuid-sky-education>';
 SELECT id, full_name FROM students ORDER BY id LIMIT 3;
 SELECT id, full_name FROM teachers ORDER BY id LIMIT 3;"
```

**Bước Setup-B — owner provision credential** (owner JWT cần cho header; lấy token bằng cách login owner ở §3 Bước 1 rồi copy `Authorization` từ DevTools, hoặc curl login):

```bash
# Student credential (GAP-1277 — POST /api/v1/students/{id}/credentials)
curl -s -X POST http://localhost:9000/api/v1/students/<studentId>/credentials \
  -H "Authorization: Bearer <owner-jwt>" -H "Content-Type: application/json" \
  -d '{"email":"hocsinh1@skyedu.vn","password":"Student@2026"}'

# Teacher credential (POST /api/v1/teachers/{id}/credentials)
curl -s -X POST http://localhost:9000/api/v1/teachers/<teacherId>/credentials \
  -H "Authorization: Bearer <owner-jwt>" -H "Content-Type: application/json" \
  -d '{"email":"giaovien1@skyedu.vn","password":"Teacher@2026"}'
```

- **✅ Kỳ vọng:** HTTP 200/201, credential tạo trong `auth_credentials` (`entity_type=STUDENT` / `TEACHER`).
- **⚠️ Nếu seed đã có sẵn account teacher/student:** bỏ qua bước này, dùng account có sẵn.
- **🔍 Verify:** `docker exec kite-postgres psql -U kite -d kiteclass_shared -c "SELECT email, entity_type FROM auth_credentials ORDER BY created_at DESC LIMIT 3;"`

> PARENT credential dùng flow redeem-invitation (KC-8) — ngoài scope recipe này; nếu muốn test PARENT redirect, dùng account parent đã có từ KC-8 walk.

## 3. Các bước (browser-walk qua FE `:3000`)

### Bước 1 — Owner login → role-home `/dashboard`
- **Hành động:** Mở browser `http://localhost:3000/login` → đăng nhập `owner@skyedu.vn` / `SkyEdu@2026`.
- **✅ Kỳ vọng (PASS):** `POST /api/v1/tenant-auth/login` (hoặc owner login path) → 200 + JWT; FE redirect tới **`/dashboard`** (role-home OWNER per `roles.ts` `ROLE_HOME`). Header/sidebar hiện tên trung tâm.
- **⚠️ Sad path:** Sai mật khẩu → 401 + thông báo lỗi rõ ràng trên form (KHÔNG trắng trang). Trắng trang/400 mọi call → tenant chưa resolve (xem §5).
- **🔍 Verify:** DevTools → Application → Local Storage `localhost:3000` → có key `kc:<tenantId>:accessToken`; JWT decode có `role` = OWNER.

### Bước 2 — TEACHER login → role-home `/teacher`
- **Hành động:** Logout (hoặc tab ẩn danh) → `http://localhost:3000/login` → login `giaovien1@skyedu.vn` / `Teacher@2026`.
- **✅ Kỳ vọng:** Login 200 (`role=TEACHER`); FE redirect tới **`/teacher`** (teacher-shell), KHÔNG phải `/dashboard`.
- **⚠️ Sad path:** Redirect về `/dashboard` thay vì `/teacher` → role-home routing sai (báo FAIL). Login 401 → credential chưa provision (làm lại §2.2).
- **🔍 Verify:** URL bar = `/teacher`; teacher nav riêng hiển thị.

### Bước 3 — STUDENT login → role-home `/student` (KC-9, GAP-1277)
- **Hành động:** Logout → `http://localhost:3000/login` → login `hocsinh1@skyedu.vn` / `Student@2026`.
- **✅ Kỳ vọng:** Login 200 (`role=STUDENT`); FE redirect tới **`/student`** (student-shell mobile).
- **⚠️ Sad path:** `/student` 404 / redirect loop → student-shell chưa build đúng (báo FAIL). Login 401 → credential chưa provision.
- **🔍 Verify:** URL = `/student`; student PWA shell render.

### Bước 4 — RoleGuard chặn cross-role route
- **Hành động:** Trong khi đang login **TEACHER** (Bước 2), gõ thẳng URL `http://localhost:3000/admin/roles`.
- **✅ Kỳ vọng:** RoleGuard chặn — KHÔNG render trang assign-role; redirect về role-home `/teacher` HOẶC hiện màn "không có quyền". (Owner-only route per `(dashboard)/admin/layout.tsx` RoleGuard.)
- **⚠️ Sad path:** Teacher VÀO được `/admin/roles` + thấy nút gán role → **RoleGuard leak — báo BLOCKING**.
- **🔍 Verify:** Console không có data role-template load cho teacher; Network không có `GET /api/v1/roles` thành công 200 với teacher token (hoặc 403).

### Bước 5 — Owner mở `/admin/roles` → gán/thu hồi role (GAP-1119 Bucket D)
- **Hành động:** Login lại OWNER → mở `http://localhost:3000/admin/roles`.
- **✅ Kỳ vọng:**
  - Trang render 5 template: **Chủ trung tâm (OWNER) / Nhân viên (STAFF) / Giáo viên (TEACHER) / Phụ huynh (PARENT) / Học sinh (STUDENT)**. Nếu chưa seed → có nút seed templates.
  - Danh sách user + role hiện tại.
  - **KHÔNG** có UI chỉnh-sửa-permission (fixed-curated per GAP-1119 decision 1).
- **Hành động (gán):** Nhập `user_id` (numeric reference id) + chọn 1 template → **Gán**.
- **✅ Kỳ vọng:** Network `POST /api/v1/roles/.../assign` (hoặc tương đương) → 200; toast thành công; danh sách cập nhật role mới.
- **Hành động (thu hồi):** Click **Thu hồi (Trash)** trên 1 assignment → confirm.
- **✅ Kỳ vọng:** `DELETE`/revoke → 200; role biến mất khỏi danh sách.
- **⚠️ Sad path:** Gán role không tồn tại / user_id sai → 400/404 + thông báo lỗi. Thấy UI sửa permission → spec drift (báo).
- **🔍 Verify:** `docker exec kite-postgres psql ... -c "SELECT user_id, role_id FROM user_roles ORDER BY id DESC LIMIT 5;"` phản ánh gán/thu hồi.

### Bước 6 — STAFF scope (GAP-1274 DONE — walk yên tâm)
> STAFF = role platform-side (vào KC qua SSO owner/staff hoặc account STAFF có sẵn). Nếu chưa có session STAFF, dùng recipe SSO (`2026-06-14-g2-recipe-sso-kh-kc.md`) login KH staff → SSO sang KC.
- **Hành động:** Với session **STAFF**, mở các trang nghiệp vụ: enrollment (ghi danh), attendance (điểm danh), invoice (hóa đơn học phí).
- **✅ Kỳ vọng:** STAFF **truy cập được** (200) — `@PreAuthorize("hasAnyRole('STAFF') ...")` cho phép.
- **Hành động:** STAFF mở payroll (`/payroll` hoặc API `/api/v1/payroll/...`) + branding (KC-10 per-tenant branding).
- **✅ Kỳ vọng:** **403 Forbidden** — payroll = `hasAnyRole('ADMIN','OWNER')`, branding = OWNER-only. STAFF bị chặn.
- **⚠️ Sad path:** STAFF VÀO được payroll/branding → authz leak (báo BLOCKING). STAFF bị chặn enrollment/attendance/invoice → over-restrict (báo).
- **🔍 Verify:** Network status: enrollment/attendance/invoice = 2xx; payroll/branding = 403.

## 4. Sad path quick checks (tổng hợp)
- Login sai mật khẩu → 401 + thông báo rõ (không trắng trang).
- Login với role chưa provision credential → 401 (làm §2.2).
- Token hết hạn → API 401 → FE prompt re-login.
- Cross-role direct URL (teacher → `/admin/roles`, student → `/teacher`) → RoleGuard chặn, KHÔNG leak.
- `user_id` không tồn tại khi gán role → 404/400 + lỗi rõ ràng.

## 5. Báo kết quả
Khi G2 xong, báo lại 1 trong 4:
- ✅ **FULL PASS** → Claude flip rows liên quan (GAP-1119/1277) → chờ G3.
- ⚠️ **MOSTLY PASS** (cosmetic: label, toast text) → catalog gap polish, fix inline nếu nhỏ (per `small-gap-inline-fix.md`).
- 🔴 **BLOCKING** (role-home sai / RoleGuard leak / STAFF authz leak / assign fail) → catalog blocker + fix loop + re-walk.
- ❓ **UNCLEAR** → ping kèm screenshot + Network/Console error.

Format gọn: `Bước 1: ✅ | Bước 4: ⚠️ (teacher vào được /admin/roles)` + screenshot nếu fail.

## 6. Troubleshooting + G3 preview

| Triệu chứng | Quick fix |
|---|---|
| Login trắng trang / 400 mọi call | Tenant chưa resolve — owner phải là `instances.owner_id` (GAP-1068 class); dùng đúng creds §2.1 |
| `:3000` ERR_EMPTY_RESPONSE | Stale FE docker-proxy (GAP-1067 class) → restart container `kiteclass-frontend` |
| Teacher/student login 401 | Credential chưa provision → làm §2.2; verify `auth_credentials` row |
| Redirect sai role-home | `roles.ts` `ROLE_HOME` / `normalizeRole` — báo blocker kèm JWT `role` claim observed |
| STAFF không có session | Vào KC qua SSO (recipe `2026-06-14-g2-recipe-sso-kh-kc.md`) hoặc account STAFF sẵn có |

**G3 preview (production-parity, AWS-gated GAP-612):** mọi role login qua gateway `:9000` JWT→header chain trên RDS + Flyway thật; cross-tenant RoleGuard isolation; production access-mode (subdomain) per `g1-browser-walk-before-flip.md` §3.2. G3-infra (TLS/wildcard-cert/real-DNS) không block THÔNG-local.
