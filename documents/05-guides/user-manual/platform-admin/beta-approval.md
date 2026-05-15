---
persona: platform-admin
topic: beta-approval
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 5
---

# Duyệt Beta tenant — Quy trình chính thức

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **5 phút**

## TL;DR

Khi Anonymous Prospect (chị Hằng, em Vy) gửi form `/beta-access` → request vào hàng đợi `/admin/beta-requests`. Em Mai duyệt theo 4 tiêu chí trong 24h.

- 📥 **Nguồn:** Form submit từ trang `/beta-access` của tenant prospect
- ⏱️ **SLA duyệt:** 24h trong giờ hành chính
- ✅ **4 tiêu chí:** Quy mô phù hợp (50-500 hs) · Khu vực VN · Email công ty hợp lệ · Không trùng tenant
- 🚀 **Sau duyệt:** Tự động tạo tenant + gửi invite link + 6 tháng FREE

---

## 1. Mở danh sách yêu cầu

<!-- Screenshot placeholder: capture beta-approval-step-1.png — 1440×900 vi-VN — show /admin/beta-requests list view với 3 mock rows (Sky Education / Quang Minh / Phương Đông) status=PENDING + nút "Xem chi tiết" mũi tên đỏ -->

URL: `/admin/beta-requests`

Sidebar Admin → mục **Beta Requests** → danh sách hiển thị:

| Cột | Ý nghĩa |
|---|---|
| Center Name | Tên trung tâm chị Hằng nhập |
| Owner | Họ tên + email người gửi |
| Submitted | Thời gian submit, format `Thứ Hai, 14/05/2026 14:30` |
| Status | `PENDING` / `APPROVED` / `REJECTED` |
| Action | Nút "Xem chi tiết" |

Lọc nhanh: `Status=PENDING` mặc định hiển thị đầu trang.

---

## 2. Đánh giá yêu cầu (4 tiêu chí)

Click "Xem chi tiết" → mở trang `/admin/beta-requests/{id}` với form đầy đủ:

<!-- Screenshot placeholder: capture beta-approval-step-2.png — 1440×900 vi-VN — show beta request detail view với form data (Sky Education / Trần Thị Hồng / hong@sky-edu.vn / 120 hs / Q.1 HCM) + 4 checkbox tiêu chí + nút Approve / Reject -->

### 2.1 Quy mô phù hợp

- ✅ Trung tâm khai 50-500 học sinh hiện tại → phù hợp với target Beta
- ❌ <50 hs → quá nhỏ, suggest FREE tier (tự đăng ký không cần Beta)
- ❌ >500 hs → quá lớn cho Phase 1 BETA, đề xuất ENTERPRISE callback

### 2.2 Khu vực Việt Nam

- ✅ Địa chỉ trung tâm trong VN (TP.HCM, Hà Nội, Đà Nẵng, tỉnh khác)
- ❌ Nước ngoài → reject + suggest version multi-region (chưa launch)

### 2.3 Email công ty hợp lệ

- ✅ Email domain match tên trung tâm (vd `hong@sky-edu.vn` cho Sky Education)
- ⚠️ Gmail/Yahoo cá nhân → flag để verify thêm qua điện thoại
- ❌ Email tạm bợ (mailinator/10minute) → reject

### 2.4 Không trùng tenant

Search DB: `SELECT * FROM tenants WHERE owner_email = '{email}' OR center_name ILIKE '%{name}%'`.

- ✅ Không có row → tenant mới, OK duyệt
- ⚠️ Có row STATUS=`TRIAL_EXPIRED` → liên hệ owner xem có muốn tái kích hoạt
- ❌ Có row STATUS=`ACTIVE` → reject, đề nghị họ login bằng tài khoản cũ

---

## 3. Action — Approve hoặc Reject

### 3.1 Approve

<!-- Screenshot placeholder: capture beta-approval-step-3.png — 1440×900 vi-VN — show approve confirmation modal "Xác nhận duyệt Sky Education? Hành động này sẽ tạo tenant + gửi invite link" + nút Đồng ý mũi tên đỏ -->

Click nút **Approve** xanh → confirm modal hiện ra → click **Đồng ý**.

Hệ thống tự động:
1. Tạo row `tenants` mới với `subscription_tier=BETA_FREE_6M` (6 tháng FREE)
2. Tạo row `users` PRIMARY_OWNER với email chị Hằng
3. Tạo invite link có TTL 7 ngày: `https://app.kitehub.me/invite/{token}`
4. Gửi email transactional (Resend) tới `hong@sky-edu.vn` với invite link + welcome content
5. Update row `beta_access_requests` STATUS=`APPROVED`, `approved_at=now()`, `approved_by=admin@kitehub.me`
6. Write audit log entry `beta.approve` với `request_id` + `tenant_id` + `approved_by`

### 3.2 Reject

Click nút **Reject** đỏ → modal yêu cầu nhập lý do (≥10 ký tự) → click **Xác nhận từ chối**.

Hệ thống:
1. Update row STATUS=`REJECTED` + `rejection_reason`
2. Gửi email tới prospect: "Cảm ơn quan tâm, hiện tại KiteHub chưa phù hợp do {reason}. Sẽ liên hệ lại khi có gói phù hợp."
3. Write audit log `beta.reject`

---

## 4. Sau approve — verify tenant đã hoạt động

T+5 phút: kiểm tra audit log `tenant.created` với đúng `tenant_id`.

T+1h: kiểm tra email đã giao thành công qua `/admin/email-events` (Resend status = `delivered`).

T+24h: nếu chị Hằng chưa accept invite → trigger reminder email manual qua nút "Resend invite" trong detail view.

T+7 ngày: nếu invite link expired chưa accept → flag tenant `INVITE_EXPIRED`, gửi reminder phone call.

---

## 5. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| Click Approve nhưng status không đổi | Network error, transactional rollback | F5 trang, retry. Nếu vẫn lỗi → file P1 gap |
| Email không tới prospect | Resend bounce / domain block | Mở `/admin/email-events` lọc theo email → xem `bounce_reason` |
| Invite link bị 404 khi click | TTL expired hoặc token typo | Trigger "Resend invite" trong detail view → token mới TTL 7 ngày |
| Trùng tenant nhưng tenant cũ TRIAL_EXPIRED | Owner cũ có thể đã quên | Reject, liên hệ phone tay để hỏi |

---

## 6. Liên kết

- [Tổng quan Platform Admin](index.md)
- [Impersonation 30s TTL](impersonation.md) — debug tenant nếu phàn nàn sau accept
- Operations runbook: [`documents/05-guides/operations/admin-beta-approval-runbook.md`](../../operations/) (Wave 79 GAP-480)

---

## 🆘 Cần hỗ trợ?

- 📧 Email nội bộ: [admin-support@kitehub.me](mailto:admin-support@kitehub.me)
- 🐛 Báo lỗi quy trình: file gap P1 trong [`documents/04-quality/gaps/`](../../../04-quality/gaps/)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
