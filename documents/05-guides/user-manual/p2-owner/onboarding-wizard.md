---
persona: p2-center-owner
topic: onboarding-wizard
last-updated: 2026-05-16
version: v0.9.0-beta
effort_minutes: 6
---

# Onboarding Wizard — 4 bước cấu hình trung tâm

> 📅 Cập nhật lần cuối: **2026-05-16** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **6 phút**

## TL;DR

Sau khi kích hoạt tài khoản, chị Hằng vào Onboarding Wizard 4 bước (~10 phút). Wizard hỗ trợ **skip-and-resume** — đóng tab giữa chừng vẫn quay lại đúng bước đã lưu.

- 🎉 **Bước 1:** Chào mừng + xác nhận thông tin trung tâm
- 🗺️ **Bước 2:** Tour trang quản lý (sidebar 4 module)
- 🌐 **Bước 3:** Xem trước website trung tâm cho học sinh
- ✅ **Bước 4:** Checklist 4 task tiếp theo (AI Branding / khoá học / giáo viên / gói)

---

## 1. Bước 1 — Chào mừng

<!-- Screenshot placeholder: capture onboarding-wizard-step-1.png — 1440×900 vi-VN — show /dashboard modal wizard với title "Chúc mừng! Trung tâm 'Sky Education' đã sẵn sàng 🎉" + card hiển thị tên trung tâm + URL + gói + trial days + mũi tên đỏ chỉ vào nút "Tiếp theo". -->

Wizard mở tự động sau khi vào `/dashboard` lần đầu. Bước 1 hiển thị:

- 🎉 Tiêu đề: **"Chúc mừng! Trung tâm 'Trung tâm Anh ngữ Sky Education' đã sẵn sàng"**
- 📋 Card thông tin:
  - Tên trung tâm: Trung tâm Anh ngữ Sky Education
  - URL: `sky-education.kiteclass.com`
  - Gói: **Beta FREE** (6 tháng đầu)
  - Dùng thử: Còn **180 ngày** (đến `Thứ Sáu, 14/11/2026`)
- 📝 Mô tả: "Trang quản lý này cho phép chị tuỳ chỉnh branding, thanh toán, và cài đặt. Học viên + giáo viên truy cập trang web riêng để học."

Click **"Tiếp theo"** chuyển sang Bước 2.

---

## 2. Bước 2 — Trang quản lý

<!-- Screenshot placeholder: capture onboarding-wizard-step-2.png — 1440×900 vi-VN — show wizard với list 4 sidebar items (Dashboard / Thanh toán / Thương hiệu / Cài đặt) + viền vàng khoanh vùng "Thương hiệu" item + số bước "2" overlay. -->

Bước 2 giới thiệu 4 module trong sidebar bên trái:

| Module | Mô tả | URL |
|---|---|---|
| 📊 **Dashboard** | Tổng quan + truy cập nhanh + KPI cards | `/dashboard` |
| 💳 **Thanh toán** | Quản lý gói + hoá đơn + nâng cấp | `/billing` |
| 🎨 **Thương hiệu** | AI tạo logo + màu sắc + website | `/branding` |
| ⚙️ **Cài đặt** | Cấu hình trung tâm (tên, địa chỉ, SĐT, MST) | `/settings` |

Click **"Tiếp theo"** chuyển sang Bước 3.

---

## 3. Bước 3 — Website trung tâm

<!-- Screenshot placeholder: capture onboarding-wizard-step-3.png — 1440×900 vi-VN — show wizard với gradient card "Website trung tâm của bạn" + nút "Mở website trung tâm" (CTA primary) + mẹo "Sử dụng AI Branding để tạo logo + thiết kế trong 5 phút". -->

Bước 3 hiển thị link đến **website công khai** mà học sinh + phụ huynh sẽ thấy:

- 🌐 **URL public**: `https://sky-education.kiteclass.com` (mở tab mới)
- ✨ Mẹo: Sử dụng tính năng **AI Branding** để tạo logo + thiết kế website chuyên nghiệp trong 5 phút (xem [Branding](branding.md))

Click **"Mở website trung tâm"** → tab mới mở trang tenant. Quay lại tab gốc click **"Tiếp theo"**.

---

## 4. Bước 4 — Checklist tiếp theo

<!-- Screenshot placeholder: capture onboarding-wizard-step-4.png — 1440×900 vi-VN — show wizard với 4 numbered checklist items (1. AI Branding / 2. Thêm khoá học / 3. Mời giáo viên / 4. Nâng cấp gói) + nút "Hoàn thành" (CTA primary). -->

Bước 4 — bước cuối — hiển thị 4 task gợi ý ưu tiên:

| # | Task | Mô tả | Link |
|---|---|---|---|
| 1 | **Tạo thương hiệu AI** | Upload logo → AI tạo website | [/branding](branding.md) |
| 2 | **Thêm khoá học đầu tiên** | Cài đặt khoá học để học viên đăng ký | [Lớp đầu tiên](first-class.md) |
| 3 | **Mời giáo viên** | Thêm giáo viên + Quản lý vào hệ thống | [Mời nhân viên](invite-staff.md) |
| 4 | **Nâng cấp gói (tuỳ chọn)** | Không giới hạn, nhiều tính năng | [Bảng giá](pricing-billing.md) |

Click **"Hoàn thành"** → wizard đóng → chuyển về `/dashboard`. Banner thông báo: "✅ Onboarding hoàn tất! Có thể xem lại bất kỳ lúc nào từ Dashboard."

---

## 5. Skip-and-resume — đóng giữa chừng

Wizard hỗ trợ **skip-and-resume** nhờ FE state lưu trong localStorage + BE state qua API:

- 🔒 **Đóng tab giữa Bước 2** → mở lại `/dashboard` → wizard tự động restore Bước 2
- ⏭️ **Skip wizard** (click X góc trên phải) → có thể mở lại từ Dashboard → "Xem hướng dẫn onboarding"
- ⏮️ **Đi lùi** — click nút "Quay lại" trong wizard để chỉnh sửa bước trước
- 🎯 **Click trực tiếp step indicator** (4 dot dưới content) — nhảy tới bất kỳ bước nào

**Verify bằng dev tools** (nâng cao):
- LocalStorage key: `onboarding-wizard-step` (string `"0"` đến `"3"`)
- API endpoint: `GET /api/v1/instances/{id}/onboarding-state`

---

## 6. Khi nào skip onboarding hợp lý?

| Tình huống | Skip OK? | Lý do |
|---|---|---|
| Chị đã từng dùng KiteHub trial trước | ✅ | Quen rồi, vào thẳng `/dashboard` |
| Test thử 30 phút trước khi quyết định | ⚠️ | Skip OK, nhưng nên xem Bước 3 (website preview) |
| Tài khoản beta đầu tiên + chưa biết gì | ❌ | Đi đủ 4 bước để hiểu hệ thống |
| Anh Tâm (Manager) login lần đầu | ✅ | Manager có wizard riêng (xem [/help/p3-manager](../p3-manager/index.md)) |

---

## 7. Troubleshooting

| Vấn đề | Cách khắc phục |
|---|---|
| Wizard không hiển thị sau khi vào /dashboard | F5 refresh / clear localStorage `onboarding-wizard-*` |
| Bước 3 link "Mở website" không hoạt động | Kiểm tra DNS subdomain — đợi 5 phút sau khi tạo tenant |
| Nút "Tiếp theo" disabled | Field bắt buộc chưa điền — scroll up tìm error message |
| Modal đóng đột ngột | Kiểm tra console error — báo support kèm screenshot |

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 💬 Zalo OA: zalo.me/kitehub (đang triển khai — Phase 1.5)
- 📞 Hotline beta: 1900-xxxx (giờ hành chính)
- 🐛 Báo lỗi trang này: [mailto:support@kitehub.me?subject=Lỗi /help/p2-owner/onboarding-wizard](mailto:support@kitehub.me?subject=L%E1%BB%97i%20%2Fhelp%2Fp2-owner%2Fonboarding-wizard)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
