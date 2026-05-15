---
persona: platform-admin
topic: impersonation
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 5
---

# Impersonation tool — Đăng nhập tạm với TTL 30 giây

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **5 phút**

## TL;DR

Khi tenant phàn nàn "Tôi không thấy nút X" — em Mai có thể impersonate vào tenant TRONG 30 GIÂY để debug, KHÔNG xem được password. Mọi action ghi audit log.

- 🎯 **Mục đích:** Debug UI/permission issue của tenant mà không lộ password
- ⏱️ **TTL:** 30 giây — short-lived để giảm rủi ro
- 🛡️ **Bảo mật:** Audit log auto-record entry/exit + mọi click trong session
- 📋 **Yêu cầu:** Tenant phải đồng ý trước qua email/Zalo trước khi impersonate (consent log)

---

## 1. Khi nào dùng Impersonation?

✅ **Đúng case:**
- Tenant báo "nút Approve không click được" → impersonate xem permission/RBAC
- Tenant báo "không thấy hoá đơn tháng 5" → impersonate verify data isolation
- Test sau cấu hình branding cho tenant để confirm theme apply đúng

❌ **KHÔNG dùng:**
- Để xem dữ liệu tenant mà không có consent → vi phạm PDPL
- Để thực hiện action thay tenant (vd duyệt invite, đổi billing) → tenant phải tự làm
- Để "test feature" → dùng test tenant dev environment

---

## 2. Quy trình impersonation

### 2.1 Lấy consent từ tenant

<!-- Screenshot placeholder: capture impersonation-step-1.png — 1440×900 vi-VN — show email template "Yêu cầu hỗ trợ kỹ thuật" gửi chị Hằng với consent button + 30s TTL note -->

Trước khi impersonate, gửi email/Zalo cho tenant owner:

> "Chị Hằng, để debug lỗi 'nút duyệt không click được', em cần đăng nhập tạm vào tài khoản chị trong 30 giây. Audit log sẽ ghi nhận. Chị đồng ý cho phép không?"

Lưu phản hồi (email screenshot hoặc Zalo screenshot) vào support ticket — chứng cứ consent cho PDPL audit.

### 2.2 Mở impersonation tool

URL: `/admin/impersonation`

<!-- Screenshot placeholder: capture impersonation-step-2.png — 1440×900 vi-VN — show /admin/impersonation form với input "Tenant search" + "User email" + textarea "Lý do impersonation" + nút "Bắt đầu impersonate" mũi tên đỏ -->

Form yêu cầu nhập:

| Field | Bắt buộc | Mô tả |
|---|:---:|---|
| Tenant search | ✅ | Search by `center_name` hoặc `tenant_id` (UUID) |
| User email | ✅ | Email user trong tenant đó (dropdown autocomplete) |
| Lý do | ✅ | ≥20 ký tự — sẽ ghi vào audit log |
| Consent evidence | ✅ | Link/screenshot URL từ support ticket |

### 2.3 Click "Bắt đầu impersonate"

Hệ thống:
1. Verify `PLATFORM_ADMIN` role + Tenant + User tồn tại
2. Tạo short-lived JWT `tenantId={target}` + `actorId={admin}` + `exp=now()+30s` + `impersonating=true`
3. Write audit log `impersonation.enter` với consent_evidence link
4. Redirect tới `/dashboard?impersonating=true` của tenant với banner đỏ trên đầu trang

<!-- Screenshot placeholder: capture impersonation-step-3.png — 1440×900 vi-VN — show /dashboard view với banner đỏ "🔴 Bạn đang impersonate vào Sky Education (chị Hằng) — TTL còn 27s" + countdown timer mũi tên đỏ -->

### 2.4 Debug trong 30 giây

Banner đỏ trên cùng hiển thị:
- Tên tenant + user đang impersonate
- Countdown timer realtime (28s → 27s → ...)
- Nút "Thoát ngay" để kết thúc sớm

Thao tác:
- ✅ Click qua menu, mở trang để xem UI render đúng không
- ✅ Mở DevTools (Network/Console) xem error
- ❌ KHÔNG submit form thay tenant (POST/PUT bị block với 403)
- ❌ KHÔNG export data (action audit ghi nhận + alert)

### 2.5 Kết thúc impersonation

3 cách kết thúc:
1. **Auto expire:** JWT exp=30s → API trả 401 → frontend redirect `/admin`
2. **Manual exit:** Click "Thoát ngay" trong banner đỏ
3. **Force exit:** Browser close tab — JWT vẫn expire backend-side đúng 30s

Hệ thống tự động:
- Write audit log `impersonation.exit` với `duration_seconds`, `actions_count`
- Banner đỏ biến mất, redirect `/admin/impersonation` với toast "Phiên impersonation kết thúc"

---

## 3. Audit log impersonation

URL: `/admin/audit-log?action=impersonation.*`

Mỗi entry chứa:

| Field | Ví dụ |
|---|---|
| `timestamp` | `2026-05-15T10:32:15Z` |
| `actor_id` | `admin@kitehub.me` |
| `action` | `impersonation.enter` / `impersonation.exit` |
| `target_tenant_id` | UUID Sky Education |
| `target_user_id` | UUID chị Hằng |
| `reason` | "Debug lỗi nút duyệt invite không click được" |
| `consent_evidence` | Link tới support ticket #1234 |
| `duration_seconds` | 27 (đóng banner ở giây thứ 27) |
| `actions_count` | 5 (5 click trong session) |

Lưu trữ 7 năm theo PDPL data retention policy.

---

## 4. Khi tenant SAU đó hỏi về impersonation

Nếu tenant follow-up "tôi thấy notification 'Quản trị viên đã đăng nhập tạm thời'" — em Mai mở audit log, screenshot 2 entry (enter + exit) + reason + consent evidence gửi tenant qua email.

Tenant có quyền yêu cầu báo cáo impersonation định kỳ (vd: hàng tháng) theo PDPL — gửi qua [`/admin/audit-log?export=tenant&id={tenant_id}`].

---

## 5. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| Click "Bắt đầu" báo 403 | Tenant đang trong `suspended` state | Activate tenant trước, hoặc dùng read-only mode (riêng) |
| Banner đỏ không hiện | Frontend cache | F5, clear localStorage |
| TTL expired nhưng còn xem được data | Frontend stale | API call tiếp theo sẽ 401 → auto logout |
| Tenant phàn nàn impersonation không có consent | Audit log không tìm thấy consent_evidence link | File P0 gap — cần verify quy trình tay đã skip |

---

## 6. Liên kết

- [Tổng quan Platform Admin](index.md)
- [Quản lý tenant bulk operations](tenant-management.md) — non-destructive ops không cần impersonate
- Wave 79 Bucket F-bis: implementation runbook `documents/05-guides/operations/` (impersonation 30s TTL)

---

## 🆘 Cần hỗ trợ?

- 📧 Email nội bộ: [admin-support@kitehub.me](mailto:admin-support@kitehub.me)
- 🐛 Báo lỗi: file gap P0 nếu liên quan PDPL/security
- 📊 Trạng thái beta: [/beta-status](/beta-status)
