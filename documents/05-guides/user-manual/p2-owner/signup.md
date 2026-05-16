---
persona: p2-center-owner
topic: signup
last-updated: 2026-05-16
version: v0.9.0-beta
effort_minutes: 5
---

# Đăng ký tài khoản Chủ trung tâm (P2 Owner)

> 📅 Cập nhật lần cuối: **2026-05-16** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **5 phút**

## TL;DR

Trang này hướng dẫn chị Hằng (Chủ trung tâm Anh ngữ Sky Education) đăng ký tài khoản KiteHub Beta trong 5 phút.

- 🔗 **Bước 1:** Truy cập trang chủ → click **"Yêu cầu truy cập Beta"**
- 📧 **Bước 2:** Điền form (họ tên + email + tên trung tâm + số học sinh)
- ✉️ **Bước 3:** Xác nhận email (link gửi qua Resend trong 2-3 phút)
- 🎯 **Bước 4:** Đợi admin duyệt (tối đa 24 giờ) → email kích hoạt
- 🚀 **Bước 5:** Click link kích hoạt → vào onboarding wizard

---

## 1. Truy cập trang chủ KiteHub

<!-- Screenshot placeholder: capture signup-step-1.png — 1440×900 vi-VN — show https://kitehub.me/ landing với hero "Quản lý trung tâm giáo dục dễ dàng" + nút CTA màu primary "Yêu cầu truy cập Beta" — mũi tên đỏ #dc2626 chỉ vào nút CTA + số bước "1" overlay góc trên trái. Capture deferred per BE stopped — track GAP-537c-followup-screenshot-capture. -->

Mở browser, truy cập `https://kitehub.me/`. Trang chủ KiteHub hiển thị:

- **Hero section** giới thiệu sản phẩm với tone Việt Nam giáo dục
- **Nút CTA** chính: "Yêu cầu truy cập Beta" (màu primary)
- **Footer** chứa link [Bảng giá](/pricing) + [Điều khoản](/legal/terms) + [Hỗ trợ](mailto:support@kitehub.me)

Click nút **"Yêu cầu truy cập Beta"**.

---

## 2. Điền form đăng ký

<!-- Screenshot placeholder: capture signup-step-2.png — 1440×900 vi-VN — show form /signup với 4 input fields (họ tên / email / tên trung tâm / số học sinh dropdown) + viền vàng #facc15 khoanh vùng email field + số bước "2" overlay. Sample data: "Trần Thị Hằng" / "hang.tran@sky-edu.vn" / "Trung tâm Anh ngữ Sky Education" / "100-200 học sinh". -->

Form đăng ký yêu cầu 4 thông tin:

| Trường | Ví dụ | Ghi chú |
|---|---|---|
| Họ tên đầy đủ | Trần Thị Hằng | Tên trên giấy tờ kinh doanh |
| Email công việc | hang.tran@sky-edu.vn | Email kích hoạt sẽ gửi tới đây |
| Tên trung tâm | Trung tâm Anh ngữ Sky Education | Tên đầy đủ trên giấy phép |
| Số học sinh | 100-200 | Dropdown: dưới 50 / 50-100 / 100-200 / 200+ |

Click **"Gửi yêu cầu"**. Hệ thống lưu request với status `PENDING` → email xác nhận tự động.

---

## 3. Xác nhận email

<!-- Screenshot placeholder: capture signup-step-3.png — 1440×900 vi-VN — show inbox Gmail với email từ Resend (sender: noreply@kitehub.me, subject: "Xác nhận đăng ký KiteHub Beta — Trung tâm Anh ngữ Sky Education") + mũi tên đỏ chỉ vào link "Xác nhận email" trong body. -->

Kiểm tra inbox email **hang.tran@sky-edu.vn**:

- Sender: `noreply@kitehub.me`
- Subject: "Xác nhận đăng ký KiteHub Beta — Trung tâm Anh ngữ Sky Education"
- Nếu không thấy: kiểm tra **Spam/Promotions** + đợi 5 phút trước khi resend

Click link **"Xác nhận email"** trong body. Trang `/signup/confirm?token=<uuid>` mở:

- ✅ "Cảm ơn chị Hằng! Yêu cầu của chị đang được Platform Admin xem xét."
- 📅 "Thời gian phản hồi tối đa: 24 giờ làm việc"

---

## 4. Đợi admin duyệt + nhận email kích hoạt

Platform Admin (anh Mai bên KiteHub) sẽ review yêu cầu:

- ✅ Verify thông tin trung tâm hợp lệ (có giấy phép)
- ✅ Đảm bảo đủ capacity beta cohort (Phase 1: 5 tenant đầu tiên)
- 📧 Approve → email kích hoạt gửi ngay; reject → email giải thích + invite resubmit sau

<!-- Screenshot placeholder: capture signup-step-4.png — 1440×900 vi-VN — show email approval từ admin với subject "Tài khoản KiteHub Beta đã sẵn sàng — Sky Education" + nút "Kích hoạt tài khoản" (CTA màu primary). -->

Email kích hoạt chứa:

- 🔐 **Link kích hoạt** (one-time, hết hạn sau 7 ngày): `https://kitehub.me/signup/activate?token=<uuid>`
- 📋 **Hướng dẫn nhanh** 3 bước đầu tiên
- 📞 **Hotline support beta**: 1900-xxxx (giờ hành chính)

---

## 5. Kích hoạt + vào onboarding wizard

Click link kích hoạt → chuyển sang **Onboarding Wizard 4 bước**:

1. **Chào mừng** — xác nhận tên trung tâm + URL (`sky-education.kiteclass.com`)
2. **Trang quản lý** — giới thiệu sidebar (Dashboard / Thanh toán / Thương hiệu / Cài đặt)
3. **Website trung tâm** — preview trang công khai cho học sinh + phụ huynh
4. **Checklist tiếp theo** — 4 task gợi ý (AI Branding / Thêm khoá học / Mời giáo viên / Nâng cấp gói)

Chi tiết wizard: xem [Onboarding Wizard](onboarding-wizard.md).

---

## 6. Troubleshooting

| Vấn đề | Cách khắc phục |
|---|---|
| Không nhận được email xác nhận | Kiểm tra Spam / đợi 10 phút / gửi support@kitehub.me |
| Link kích hoạt hết hạn | Email request lại; admin sẽ gửi link mới trong 24h |
| Token không hợp lệ | URL có thể bị truncate khi forward email; copy nguyên link |
| Admin không phản hồi trong 24h | Hotline 1900-xxxx hoặc Zalo OA (Phase 1.5+) |
| Tên trung tâm trùng | Hệ thống tự thêm hậu tố `-2` / `-3` (vd `sky-education-2.kiteclass.com`) |

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 💬 Zalo OA: zalo.me/kitehub (đang triển khai — Phase 1.5)
- 📞 Hotline beta: 1900-xxxx (giờ hành chính)
- 🐛 Báo lỗi trang này: [mailto:support@kitehub.me?subject=Lỗi /help/p2-owner/signup](mailto:support@kitehub.me?subject=L%E1%BB%97i%20%2Fhelp%2Fp2-owner%2Fsignup)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
