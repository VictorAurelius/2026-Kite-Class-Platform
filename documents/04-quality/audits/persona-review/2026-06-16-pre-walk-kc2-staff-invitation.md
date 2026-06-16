---
title: Pre-Walk Persona Simulation — KC-2 Staff invitation → accept → RBAC
audience: dev
created: 2026-06-16
scope: Pre-walk failure-mode prediction for Flow Verification Campaign KC-2 (G2 browser walk)
flow: KC-2 staff-invitation (platform-side — kitehub-subscription BE + kitehub-frontend :3001)
references:
  - documents/03-planning/waves/wave-2026-06-05-flow-kc2-staff-invitation-rbac.md
  - documents/05-guides/operations/2026-06-05-g2-recipe-kc2-staff-invitation.md
  - .claude/rules/pre-walk-persona-simulation-mandate.md
  - .claude/rules/g1-browser-walk-before-flip.md
---

# Pre-Walk Persona Simulation — KC-2 Staff invitation + RBAC

## 0. Boundary note (đọc trước)

**KC-2 là flow PLATFORM-SIDE** per `kitehub-kiteclass-boundary.md` §4.1 exception — dù mang nhãn "KC-",
flow này chạy trên **kitehub-frontend `:3001`** (KH) + **kitehub-subscription** BE, KHÔNG phải
kiteclass-frontend `:3000`. Walker mở browser ở `http://localhost:3001` (đúng như G2 recipe).
Đừng nhầm sang `:3000`.

**Persona walked:** (a) Owner (`owner@skyedu.vn`, Chủ trung tâm Sky Education) mời nhân viên;
(b) Invitee mới (chưa có account) nhận email → đặt password → login STAFF lần đầu.

**Stack pre-walk state (verified 2026-06-16):** full Docker stack healthy (kitehub-subscription +
kitehub-frontend + gateway + kite-postgres + kite-mailhog đều Up). BE happy-path chain đã verify
walk-ready bằng pre-walk checks (xem §2).

## 1. BE happy-path — đã verify walk-ready (curl pre-walk)

| Bước | Pre-walk check kết quả | Verdict |
|---|---|---|
| Owner login | `POST :9000/api/auth/login {owner@skyedu.vn / SkyEdu@2026}` → **HTTP 200**, trả accessToken | ✅ credential hợp lệ |
| 2FA divert | Login trả token trực tiếp (KHÔNG `requires2fa`) → owner không bật 2FA | ✅ không bị 2FA chặn |
| JWT tenantId | Decode JWT claim → `tenantId=e8ff87e1-...` (= instance `sky-education`) | ✅ apiClient sẽ tự inject X-Tenant-Id |
| Instance active | `instances`: subdomain `sky-education`, tier PREMIUM, status ACTIVE, isActive=true | ✅ tenant active |
| Invite POST / accept / STAFF RBAC | G1 verdict 2026-06-05 đã PASS (xem wave plan §5.1) | ✅ BE chain confirmed |

→ Happy-path BE solid. Các failure mode dưới đây là **browser/FE/config-only** mà curl-walk
(header gắn tay) đã che mất — đúng class mà `g1-browser-walk-before-flip.md` cảnh báo.

## 2. Failure modes (9) — ưu tiên browser-only

| # | Title | Confidence | Severity |
|---|---|---|---|
| FM-1 | Email link trỏ prod domain `https://kitehub.me` (không local) | **HIGH** | MED (có workaround) |
| FM-2 | Re-invite email PENDING → 201 (auto-revoke), recipe ghi 409 — drift | **HIGH** | LOW (recipe sai, code đúng) |
| FM-3 | STAFF login → `/dashboard` render nhưng widget owner-scoped 403/rỗng | **MED-HIGH** | MED |
| FM-4 | Sai credential `owner.sky@test.vn` (no instance → no tenantId → 403) | **HIGH** (nếu chọn nhầm) | HIGH (walk chết bước 2) |
| FM-5 | Owner redirect `/dashboard` sau login — phải dùng menu sidebar tới `/admin/staff` | MED | LOW |
| FM-6 | accept page hiển thị role thô `STAFF` (không nhãn tiếng Việt) | MED | LOW (cosmetic) |
| FM-7 | Email subject/tên tổ chức = "Trung tâm KiteHub" generic, không phải "Sky Education" | HIGH | LOW (cosmetic) |
| FM-8 | trialDaysLeft=0 (trial hết hạn) — có thể gate 1 vài feature | LOW | LOW |
| FM-9 | STAFF role-authority mapping → owner-only 403 (defense-in-depth) | LOW | — (verify, không phải bug) |

---

### FM-1 — Email accept link trỏ prod domain `https://kitehub.me`

- **(a) Where:** `StaffInvitationController.java:102` `@Value("${kitehub.staff.invitation.base-url:https://kitehub.me}")` + `dispatchInviteEmail()` line 273 `inviteBaseUrl + "/staff/accept-invite?token="`. Property `kitehub.staff.invitation.base-url` **KHÔNG được override** (verified: không có trong `application*.yml`, không có env `KITEHUB_STAFF_INVITATION_BASE_URL` trên container đang chạy) → dùng default `https://kitehub.me`.
- **(b) Symptom (browser):** Bước 3 — user mở MailHog, click link accept → trình duyệt đi tới `https://kitehub.me/staff/accept-invite?token=...` (prod, không có trên local) → trang lạ / không tải. Phải thủ công sửa `https://kitehub.me` → `http://localhost:3001`, giữ `?token=`.
- **(c) Pre-walk check (đã chạy):**
  ```bash
  docker exec kitehub-subscription sh -c 'env | grep -iE "STAFF_INVITATION|INVITATION_BASE"'   # → rỗng (không override)
  grep -rn "staff" kitehub/kitehub-subscription/src/main/resources/application*.yml | grep base-url  # → rỗng
  ```
  Đối chiếu: beta-signup dùng `KITEHUB_BETA_SIGNUP_BASE_URL=http://localhost:3001` (đúng local) → staff-invitation lệch convention.
- **(d) Confidence HIGH / proposed fix:** Recipe step 3 ĐÃ document workaround (sửa domain tay) → walk vẫn chạy được. **Inline-fix candidate (~5 phút, small-gap-inline-fix):** thêm env `KITEHUB_STAFF_INVITATION_BASE_URL=http://localhost:3001` vào compose kitehub-subscription + restart → link local đúng, bỏ friction. Per `thesis-as-future-state-mandate` prod sẽ set kitehub.me thật, nên local-override là đúng hướng.

### FM-2 — Re-invite email PENDING trả 201 (auto-revoke), recipe ghi 409

- **(a) Where:** `StaffInvitationController.java:126` `revokePendingForEmail()` — re-invite cùng email PENDING → **auto-revoke old + create new → trả 201**. Recipe step 2 sad-path ghi "Email đã mời (PENDING) → 409 (idempotency)". FE `invite/page.tsx:65` có nhánh `INVITATION_ALREADY_PENDING → 409 message` nhưng BE KHÔNG bao giờ trả 409 cho re-invite (đã auto-revoke).
- **(b) Symptom (browser):** Owner mời lại email đang PENDING → thấy **toast thành công + redirect list (201)**, KHÔNG thấy lỗi 409 như recipe mô tả → walker tưởng "sad-path fail".
- **(c) Pre-walk check (code-read):** `revokePendingForEmail` filter PENDING + `email.toLowerCase().trim()` → `service.revoke` rồi `service.create` → 201. Không có path nào throw `INVITATION_ALREADY_PENDING` từ controller create.
- **(d) Confidence HIGH / proposed fix:** Code đúng (idempotency = silent replace là hành vi hợp lý). **Sửa RECIPE** step 2 sad-path: "Email PENDING re-invite → **201**, lời mời cũ tự thu hồi + token mới" (bỏ kỳ vọng 409). Nhỏ — fix recipe inline.

### FM-3 — STAFF `/dashboard` render nhưng widget owner-scoped 403/rỗng

- **(a) Where:** `login/page.tsx:131` `router.push(isPlatformAdmin(role) ? '/admin' : '/dashboard')` → STAFF về `/dashboard`. `(customer)/layout.tsx` "DashboardLayout chỉ check isAuthenticated (không role)" → STAFF qua được layout. Nhưng `/dashboard` widgets gọi endpoint owner-scoped (subscription/instance) → STAFF JWT có tenantId (GAP-981) nhưng không own instance → 1 số call có thể 403/rỗng.
- **(b) Symptom (browser):** Bước 5 — STAFF login OK → vào `/dashboard` nhưng dashboard có thể hiện lỗi tải / card rỗng / "không có dữ liệu" do widget owner-centric. Recipe step 5 kỳ vọng "STAFF vào được dashboard tenant" — render OK nhưng nội dung có thể không đầy đủ.
- **(c) Pre-walk check:** chưa verify rendering thật (cần browser). `grep -n RoleGuard kitehub/kitehub-frontend/src/app/(customer)/dashboard/page.tsx` → KHÔNG có role guard (STAFF không bị bounce khỏi /dashboard). Owner-scoped data behavior cần quan sát lúc walk.
- **(d) Confidence MED-HIGH / proposed fix:** Walk verify thật: STAFF login → quan sát /dashboard render + console/Network 403. Nếu chấp nhận được (render + thuộc tenant) → PASS step 5; nếu vỡ → catalog gap STAFF-home (Phase 2 STAFF dashboard scope). KHÔNG block RBAC verify (FM-9).

### FM-4 — Trap credential `owner.sky@test.vn` (no instance → 403)

- **(a) Where:** Có 2 owner Sky trong DB: `owner@skyedu.vn` (✅ own instance sky-education → JWT tenantId) và `owner.sky@test.vn` (❌ KHÔNG own instance → JWT KHÔNG có tenantId claim).
- **(b) Symptom (browser):** Nếu walker login nhầm `owner.sky@test.vn` → login 200 nhưng apiClient KHÔNG inject X-Tenant-Id (no claim) → `POST /staff-invitations` → **403 TENANT_CONTEXT_MISSING** → walk chết bước 2 + dễ chẩn đoán nhầm "tenant resolution vỡ".
- **(c) Pre-walk check (đã chạy):**
  ```bash
  docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT u.email, i.subdomain FROM users u LEFT JOIN instances i ON i.owner_id=u.id AND i.deleted=false WHERE u.email IN ('owner@skyedu.vn','owner.sky@test.vn');"
  # owner@skyedu.vn → sky-education ; owner.sky@test.vn → (null)
  ```
- **(d) Confidence HIGH (nếu chọn nhầm) / proposed fix:** Recipe credential ĐÚNG (`owner@skyedu.vn`). Pre-walk note cho walker: **bắt buộc dùng `owner@skyedu.vn`**, KHÔNG dùng `owner.sky@test.vn`. (Lưu ý chung: OWNER tenant resolve từ `instances.owner_id`, KHÔNG phải `users.tenant_id` — mọi owner đều có `users.tenant_id=NULL`, đó là bình thường, không phải bug.)

### FM-5 — Owner redirect `/dashboard`, phải dùng menu sidebar tới `/admin/staff`

- **(a) Where:** Owner login → `/dashboard` (không phải `/admin/staff`). Trang mời ở `/admin/staff/invite`. Sidebar `Sidebar.tsx:31` có link `{ href:'/admin/staff', label:'Nhân viên', requiresRole:['OWNER'] }`.
- **(b) Symptom (browser):** Owner phải click menu "Nhân viên" trên sidebar → `/admin/staff` → nút "Mời nhân viên" → `/admin/staff/invite`. Nếu sidebar không render link (bug) → walker phải gõ URL tay.
- **(c) Pre-walk check:** menu OWNER-only đã tồn tại (grep confirm). Walk verify sidebar render link "Nhân viên".
- **(d) Confidence MED / proposed fix:** Recipe step 1 đã ghi "vào /admin/staff/invite (hoặc menu 'Nhân viên' → 'Mời')". Verify menu hiển thị lúc walk; nếu thiếu → catalog nav gap.

### FM-6 — accept page hiển thị role thô `STAFF` (không nhãn tiếng Việt)

- **(a) Where:** `(public)/staff/accept-invite/page.tsx:197` `<strong>{preview?.role}</strong>` → render thẳng enum `STAFF` từ BE response. Trang invite (owner-side) dùng nhãn đẹp "Nhân viên trung tâm (STAFF)" nhưng accept page (invitee-side) chỉ "vai trò **STAFF**".
- **(b) Symptom (browser):** Bước 4 — invitee thấy "Bạn được mời ... với vai trò **STAFF**" (English enum) thay vì "Nhân viên". Per `vn-localization-audit-checklist.md` §2 — label tenant-facing nên tiếng Việt.
- **(c) Pre-walk check (code-read):** accept page không map role enum → VN label.
- **(d) Confidence MED / proposed fix:** cosmetic i18n — map `STAFF → "Nhân viên"` trong accept page. Small-gap-inline-fix candidate (~5 phút) hoặc defer cosmetic. KHÔNG block walk.

### FM-7 — Email subject/tên tổ chức generic "Trung tâm KiteHub"

- **(a) Where:** `StaffInvitationController.java:304` `resolveTenantName()` hardcode return `"Trung tâm KiteHub"` (Phase 1 BETA stub, không plumb tenant org name thật).
- **(b) Symptom (browser):** Bước 3 — email mời ghi "Trung tâm KiteHub" thay vì "Trung tâm Anh ngữ Sky Education" → invitee không nhận ra trung tâm mời mình.
- **(c) Pre-walk check (code-read):** confirmed hardcode + javadoc "Phase 1.5 will plumb branding/tenant service".
- **(d) Confidence HIGH / proposed fix:** đã document wave plan §5.1 minor defer P3. Cosmetic — defer Phase 1.5 (cần plumb tenant service). KHÔNG block walk; ghi nhận.

### FM-8 — trialDaysLeft=0 (trial hết hạn) có thể gate feature

- **(a) Where:** Login response: `trialExpiresAt 2026-06-11`, `trialDaysLeft 0`, `isOnTrial false`, nhưng `isActive true` + tier PREMIUM + status ACTIVE.
- **(b) Symptom (browser):** Nếu có gate "trial expired" chặn invite/feature → owner bị block. Nhưng isActive=true + PREMIUM nên nhiều khả năng không chặn.
- **(c) Pre-walk check (đã chạy):** login trả instance ACTIVE + isActive true → không có dấu hiệu hard-block. G1 invite PASS 2026-06-05 (lúc đó trial cũng đã/ sắp hết).
- **(d) Confidence LOW / proposed fix:** quan sát lúc walk; nếu invite bị 402/403 do trial → catalog. Khả năng thấp.

### FM-9 — STAFF role-authority → owner-only 403 (verify defense-in-depth)

- **(a) Where:** `StaffInvitationController` `OWNER_AUTHZ = hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')`. STAFF JWT role=STAFF. FE 3 lớp chặn: Sidebar `requiresRole:['OWNER']` (ẩn menu) + `(customer)/admin/staff/layout.tsx` `RoleGuard allowedRoles={['OWNER']}` (bounce STAFF về /dashboard) + BE `@PreAuthorize` 403.
- **(b) Symptom (browser):** Bước 5 verify — STAFF KHÔNG thấy menu "Nhân viên"; gõ URL `/admin/staff/invite` tay → RoleGuard bounce về `/dashboard`; nếu curl owner-only endpoint → BE 403.
- **(c) Pre-walk check:** G1 (curl) đã confirm STAFF→owner-only **403**. FE RoleGuard + Sidebar guard confirmed code-read.
- **(d) Confidence LOW (đây là VERIFY, không phải bug) / proposed fix:** Walk xác nhận cả 3 lớp; RBAC solid → step 5 PASS expected.

---

## 3. Recommended pre-walk batch fix (sort confidence × impact)

**FIX trước walk (inline, ~10 phút tổng):**
- **FM-1** (HIGH×MED): thêm env `KITEHUB_STAFF_INVITATION_BASE_URL=http://localhost:3001` vào compose kitehub-subscription + restart → bỏ friction sửa domain tay. (Nếu skip → recipe workaround vẫn chạy được.)
- **FM-2** (HIGH×LOW): sửa RECIPE step 2 sad-path 409 → 201 (auto-revoke). Docs-only, không rebuild.

**VERIFY-trong-walk (quan sát, không sửa trước):**
- **FM-3** (MED-HIGH): quan sát STAFF /dashboard render + console 403 → quyết PASS/catalog.
- **FM-4** (HIGH-nếu-nhầm): pre-walk note — bắt buộc `owner@skyedu.vn`, KHÔNG `owner.sky@test.vn`.
- **FM-5 / FM-9**: verify sidebar menu + 3-lớp RBAC lúc walk.

**DEFER cosmetic (catalog, không block):**
- **FM-6** (role label VN trên accept page), **FM-7** (tên tổ chức generic — Phase 1.5 plumb), **FM-8** (trial gate — khả năng thấp).

## 4. Verdict

BE happy-path chain **walk-ready** (login + JWT tenantId + invite + accept + RBAC đều verify qua
pre-walk/G1). 9 failure mode còn lại đều **browser/FE/config-only** — top-3 HIGH-confidence:
**FM-1 (email prod-domain), FM-2 (idempotency recipe drift 409→201), FM-4 (trap credential
owner.sky@test.vn)**. Không có blocker BE mới; G2 browser walk chủ yếu verify FE wiring + bắt 3 HIGH
mode trên. Khuyến nghị fix FM-1 + FM-2 inline trước khi hand cho human G2.
