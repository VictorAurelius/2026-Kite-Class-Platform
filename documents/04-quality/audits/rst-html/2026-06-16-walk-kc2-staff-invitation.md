---
title: G2 Browser Walk — KC-2 Staff invitation → accept → RBAC (headless)
audience: dev
created: 2026-06-16
scope: Flow Verification Campaign KC-2 — headless Playwright browser-walk (platform-side, kitehub-frontend :3001 + kitehub-subscription)
flow: KC-2 staff-invitation
walker: headless chromium (Playwright 1.59.1) — agent-driven
references:
  - documents/04-quality/audits/persona-review/2026-06-16-pre-walk-kc2-staff-invitation.md
  - documents/05-guides/operations/2026-06-05-g2-recipe-kc2-staff-invitation.md
  - .claude/rules/g1-browser-walk-before-flip.md
---

# G2 Browser Walk — KC-2 Staff invitation + RBAC (2026-06-16)

## 0. Tóm tắt

**Verdict: ⚠️ MOSTLY PASS** — toàn bộ chuỗi KC-2 chạy thông qua **browser thật** (`:3001`):
Owner login → mời nhân viên (201) → email (FM-1 fix ✅ link `localhost:3001`) → accept + đặt password (200) →
STAFF login → RBAC enforce (bounce + 403). Không có blocker. Còn lại 5 gap cosmetic/UX nhỏ + 1 recipe drift.

**Stack (verified 2026-06-16):** kitehub-subscription (Up healthy), kitehub-frontend `:3001` (200), kite-gateway `:9000`,
kite-postgres, kite-mailhog `:8025`. FM-1 fix confirmed: `KITEHUB_STAFF_INVITATION_BASE_URL=http://localhost:3001`.

**Persona:** Owner `owner@skyedu.vn` (tenant `sky-education`, JWT `tenantId=e8ff87e1-...`, role OWNER) + invitee mới.

## 1. Browser-walk evidence per step (per g1-browser-walk-before-flip.md §3)

| Bước | Hành động (browser thật) | Kết quả | Verdict |
|---|---|---|---|
| **1. Owner login** | `:3001/login` → fill email/password → submit | `POST /api/auth/login` **200** → redirect `/dashboard` | ✅ PASS |
| **2. Mở trang mời** | `:3001/admin/staff/invite` | Form render; role **read-only** (0 dropdown — GAP-784 ✅); session giữ (không bounce) | ✅ PASS |
| **2b. Gửi lời mời** | fill email + fullName → submit | `POST /api/v1/staff-invitations` **201** | ✅ PASS |
| **3. Email** | MailHog `:8025` API | Email tới đúng địa chỉ; accept link = `http://localhost:3001/staff/accept-invite?token=...` | ✅ PASS (FM-1 fix ✅) |
| **4. Accept + password** | mở accept link → đặt password (≥12) → submit | `GET /by-token` **200**, `POST /accept` **200**; 2 password input render | ✅ PASS |
| **5. STAFF login** | `:3001/login` invitee + password | `POST /api/auth/login` **200** (role=STAFF) → `/dashboard` | ✅ PASS |
| **5b. RBAC verify** | STAFF → `:3001/admin/staff/invite` | Bounce về `/dashboard` (RoleGuard) | ✅ PASS |
| **5c. RBAC BE** | STAFF token → `POST /api/v1/staff-invitations` (curl) | **403 Forbidden** | ✅ PASS |

**Console:** owner happy-path clean trừ 1 broken docs link (xem BUG-3); STAFF dashboard 1×403 owner-scoped widget (FM-3).
**Network chính:** mọi request `:9000` happy-path = 2xx; FE tự inject `X-Instance-Subdomain`/JWT tenantId (không header tay).
**Screenshots:** `/tmp/kc2-step-{1..5}*.png` + `/tmp/kc2-staff-dashboard-final.png` (11 ảnh).

## 2. 9 Failure Mode verification (pre-walk)

| FM | Dự đoán | Kết quả walk | Verdict |
|---|---|---|---|
| **FM-1** | Email link prod domain | Link = `localhost:3001` (env override hiệu lực) | ✅ **FIXED** |
| **FM-2** | Re-invite PENDING → 201 (recipe ghi 409) | Re-invite 2nd lần → **201** (auto-revoke), KHÔNG 409 | ⚠️ **recipe drift** (code đúng) |
| **FM-3** | STAFF /dashboard widget owner-scoped 403/rỗng | `GET /api/platform/instances/owner/{id}` → **403** trên STAFF /dashboard | ⚠️ **CONFIRMED** (UX) |
| **FM-4** | Trap credential `owner.sky@test.vn` | Dùng đúng `owner@skyedu.vn` — không dính bẫy | ✅ tránh đúng |
| **FM-5** | Owner redirect /dashboard, dùng menu | Owner → /dashboard; điều hướng trực tiếp `/admin/staff/invite` OK | ✅ PASS |
| **FM-6** | Accept page role thô "STAFF" | Hiển thị "vai trò STAFF" (không nhãn tiếng Việt) | ⚠️ **CONFIRMED** (cosmetic) |
| **FM-7** | Email tên tổ chức generic | Subject = "Bạn được mời tham gia **Trung tâm KiteHub** trên KiteHub" (không "Sky Education") | ⚠️ **CONFIRMED** (cosmetic) |
| **FM-8** | trial gate chặn invite | Invite 201 bình thường — KHÔNG bị gate | ✅ không xảy ra |
| **FM-9** | STAFF RBAC defense-in-depth | 3 lớp PASS: RoleGuard bounce + BE 403 (POST invite) + 403 owner endpoint | ✅ **VERIFIED solid** |

Thêm sad-path: fullName 1 ký tự → **400 `INVALID_FULL_NAME`** ✅ (khớp recipe).

## 3. Bug catalog (5 gap — KHÔNG blocker)

| # | ID | Sev | Mô tả | Đề xuất |
|---|---|---|---|---|
| 1 | FM-3 | **MED** | STAFF login → `/dashboard` (owner-centric); widget gọi `GET /api/platform/instances/owner/{id}` → 403. Dashboard render OK nhưng có lỗi tải widget owner-scoped. STAFF chưa có home riêng đúng scope. | Phase 2 STAFF dashboard scope: ẩn/thay widget owner-only cho STAFF, hoặc route STAFF tới home khác. |
| 2 | FM-2 (recipe) | LOW | Recipe step 2 sad-path ghi "PENDING re-invite → 409" nhưng BE trả **201** (auto-revoke + token mới). Code đúng (idempotency), recipe sai. | Sửa recipe: "PENDING re-invite → 201, lời mời cũ tự thu hồi". (docs-only) |
| 3 | broken-doc-link | LOW | `GET /docs/data-reset-policy` → **404** trên CẢ owner + STAFF dashboard (footer/link tĩnh thiếu trang). | Tạo trang `/docs/data-reset-policy` hoặc bỏ link. |
| 4 | FM-6 | LOW | Accept page hiển thị role thô "STAFF" (không nhãn "Nhân viên"). | Map `STAFF → "Nhân viên"` ở `(public)/staff/accept-invite/page.tsx`. |
| 5 | FM-7 | LOW | Email tên tổ chức hardcode "Trung tâm KiteHub" (không phải "Sky Education") — `StaffInvitationController.resolveTenantName()` stub. | Phase 1.5 plumb tenant/branding service. |

## 4. Báo kết quả (recipe §5)

**⚠️ MOSTLY PASS** — happy-path chain hoàn toàn functional qua browser thật, RBAC enforce đúng 3 lớp,
FM-1 fix verified. 5 gap còn lại đều cosmetic/UX-nhỏ + 1 recipe drift — KHÔNG có blocking issue.
Theo `small-gap-inline-fix.md`: FM-6 + recipe FM-2 + broken-doc-link là fix-inline candidates (≤30p mỗi cái);
FM-3 (STAFF dashboard scope) defer Phase 2; FM-7 defer Phase 1.5 (cần plumb tenant service).
