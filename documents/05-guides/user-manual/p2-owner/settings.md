---
persona: p2-center-owner
topic: settings
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 5
---

# Cài đặt chung + Vùng nguy hiểm — Hướng dẫn

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **5 phút**

## TL;DR

`/settings` là nơi chị Hằng cấu hình thông tin trung tâm, giờ hoạt động, ngôn ngữ, notification. Vùng nguy hiểm cuối trang chứa tuỳ chọn xoá tenant (PDPL right to erasure).

- 🏢 **Thông tin trung tâm:** Tên, địa chỉ, SĐT, MST, website
- 🕐 **Giờ hoạt động:** Mở/đóng theo ngày, ảnh hưởng lịch học gợi ý
- 🌐 **Ngôn ngữ:** Tiếng Việt (default) · Tiếng Anh (Phase 2)
- 🔔 **Notification:** Email, SMS, Zalo OA (Phase 1.5+)
- ⚠️ **Vùng nguy hiểm:** Đổi domain, xuất data PDPL, xoá tenant

---

## 1. Thông tin trung tâm

<!-- Screenshot placeholder pending B+C merge: capture settings-step-1.png — 1440×900 vi-VN — show /settings/general form (Tên trung tâm Sky Education / Địa chỉ / SĐT / MST / Website) + nút Lưu mũi tên đỏ -->

URL: `/settings/general`

| Field | Bắt buộc | Mô tả |
|---|:---:|---|
| Tên trung tâm | ✅ | Hiển thị trong dashboard + email + hoá đơn |
| Slug URL | ✅ | URL `sky-education.kitehub.me`, không dấu, lowercase |
| Địa chỉ đầy đủ | ✅ | Số nhà, đường, quận, thành phố |
| Số điện thoại | ✅ | Format VN `0901-234-567` hoặc `+84-901-234-567` |
| Mã số thuế (MST) | optional | Cho hoá đơn VAT |
| Website | optional | URL ngoài để hiển thị trong email signature |
| Mô tả ngắn | optional | 1-2 câu giới thiệu trung tâm |

Click **Lưu** → cập nhật ngay, không cần restart.

---

## 2. Giờ hoạt động

<!-- Screenshot placeholder pending B+C merge: capture settings-step-2.png — 1440×900 vi-VN — show working hours config với 7 row (Thứ 2-CN) cho phép set open/close per ngày + checkbox "Đóng cửa" cho Chủ nhật + mũi tên đỏ chỉ vào nút Lưu -->

URL: `/settings/hours`

7 row cho 7 ngày trong tuần:

| Ngày | Mở | Đóng | Đóng cửa? |
|---|---|---|:---:|
| Thứ Hai | 08:00 | 21:00 | ☐ |
| Thứ Ba | 08:00 | 21:00 | ☐ |
| ... | | | |
| Chủ Nhật | — | — | ☑ Đóng |

Ảnh hưởng:
- Lịch học gợi ý slot chỉ trong giờ mở
- Phụ huynh đăng ký lớp được suggest slot phù hợp
- Báo cáo doanh thu theo ngày tuần

---

## 3. Ngôn ngữ + định dạng

URL: `/settings/locale`

| Field | Mặc định Sky Education |
|---|---|
| Ngôn ngữ | Tiếng Việt (vi) |
| Múi giờ | Asia/Ho_Chi_Minh (UTC+7) |
| Định dạng ngày | `DD/MM/YYYY` |
| Định dạng tiền | `1.500.000đ` |
| Tuần bắt đầu từ | Thứ Hai |

Phase 2 sẽ hỗ trợ tiếng Anh — preview ngay trong dropdown.

---

## 4. Notification preferences

<!-- Screenshot placeholder pending B+C merge: capture settings-step-3.png — 1440×900 vi-VN — show notification table với 6 row events (Hoá đơn mới / Đơn xin nghỉ / Học sinh mới ...) × 3 column (Email / SMS / Zalo) checkbox + nút Lưu mũi tên đỏ -->

URL: `/settings/notifications`

Cho mỗi event, chọn channel:

| Event | Email | SMS | Zalo OA |
|---|:---:|:---:|:---:|
| Hoá đơn mới chờ duyệt | ✅ | ☐ | ☐ |
| Giáo viên xin nghỉ | ✅ | ✅ | ☐ |
| Học sinh mới đăng ký | ☐ | ☐ | ☐ |
| Phụ huynh thanh toán | ✅ | ☐ | ☐ |
| Lương GV cuối tháng | ✅ | ☐ | ☐ |
| SLO breach (Critical) | ✅ | ✅ | ☐ |

Zalo OA require Phase 1.5+ setup riêng.

---

## 5. Bảo mật + 2FA

URL: `/settings/security`

<!-- Screenshot placeholder pending B+C merge: capture settings-step-4.png — 1440×900 vi-VN — show security tab với 2FA toggle ON + recovery codes hiển thị + mũi tên đỏ chỉ vào nút "Tạo lại mã" -->

| Setting | Khuyến nghị |
|---|---|
| 2FA (TOTP) | ✅ Bật cho OWNER role |
| Recovery codes | Tạo 10 mã, in giấy cất tủ |
| Session timeout | 24h (mặc định) |
| Active sessions | Xem device đang đăng nhập, có thể revoke |

Bật 2FA:
1. Click toggle → mở modal QR code
2. Scan bằng Google Authenticator hoặc Authy
3. Nhập 6 số code → confirm
4. Hệ thống cấp 10 recovery codes — **in ra giấy cất tủ**

---

## 6. Vùng nguy hiểm

<!-- Screenshot placeholder pending B+C merge: capture settings-step-5.png — 1440×900 vi-VN — show /settings/danger section với 3 nút đỏ "Đổi domain slug" "Xuất toàn bộ data" "Xoá tenant vĩnh viễn" + warning text + mũi tên đỏ chỉ vào nút Xoá -->

URL: `/settings/danger`

### 6.1 Đổi domain slug

`sky-education` → `sky-edu-hcm` → URL thay đổi `sky-edu-hcm.kitehub.me`. **Cảnh báo:** link cũ sẽ broken trong 30 ngày, sau đó xoá hẳn.

### 6.2 Xuất toàn bộ data (PDPL)

Chị có quyền xuất toàn bộ data tenant theo PDPL 2023:
1. Click **Yêu cầu xuất data**
2. Chọn module (students, teachers, classes, payments, ...)
3. Hệ thống tạo ZIP encrypted trong 5-15 phút
4. Nhận link download qua email + password qua SMS

Format: CSV UTF-8 BOM + JSON metadata.

### 6.3 Xoá tenant vĩnh viễn

⚠️ **KHÔNG THỂ UNDO**.

Quy trình:
1. Click **Yêu cầu xoá tenant**
2. Modal yêu cầu nhập:
   - Lý do (≥20 ký tự)
   - Gõ verbatim `XOA-{tên-trung-tâm}` để confirm
   - Tick "Tôi đã xuất data của tôi"
3. Click **Xác nhận** → tạo request `tenant.delete.request`
4. Platform Admin (em Mai) review trong 48h
5. Sau approve, hệ thống hard-delete + audit log retain 7 năm

---

## 7. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| Đổi tên trung tâm nhưng dashboard hiện tên cũ | Cache | F5 hoặc logout/login lại |
| 2FA mất phone, không login được | Cần recovery codes | Dùng 1 trong 10 recovery codes → reset 2FA |
| Yêu cầu xoá tenant nhưng Admin không respond | SLA review 48h | Email support@kitehub.me follow up |
| Không thấy notification email | Filter spam / unsub | Check spam folder + add `noreply@kitehub.me` whitelist |

---

## 8. Liên kết

- [Tổng quan Chủ trung tâm](index.md)
- [Bảng giá + Thanh toán](pricing-billing.md)
- [Mời nhân viên](invite-staff.md)
- [Tuỳ chỉnh logo + màu](branding.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 📞 Hotline: 1900-xxxx (giờ hành chính)
- ⚖️ PDPL inquiry: [dpo@kitehub.me](mailto:dpo@kitehub.me)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
