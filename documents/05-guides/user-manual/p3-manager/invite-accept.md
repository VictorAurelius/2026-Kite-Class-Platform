---
persona: p3-center-manager
topic: invite-accept
last-updated: 2026-05-16
version: v0.9.0-beta
effort_minutes: 4
---

# Chấp nhận lời mời làm Quản lý (P3 Manager)

> 📅 Cập nhật lần cuối: **2026-05-16** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **4 phút**

## TL;DR

Anh Tâm được chị Hằng (Chủ trung tâm Sky Education) mời làm Quản lý qua email. Trang này hướng dẫn anh Tâm chấp nhận lời mời trong 3-5 phút.

- 📧 **Bước 1:** Mở email mời từ noreply@kitehub.me
- 🔗 **Bước 2:** Click link "Chấp nhận lời mời" (one-time, hết hạn 7 ngày)
- 🔐 **Bước 3:** Đặt mật khẩu mới + xác nhận
- 🎉 **Bước 4:** Đăng nhập → first-login tour hiển thị permission matrix
- 📊 **Bước 5:** Vào dashboard Manager (`/dashboard`)

---

## 1. Email mời từ chị Hằng

<!-- Screenshot placeholder: capture invite-accept-step-1.png — 1440×900 vi-VN — show inbox Gmail với email từ Resend (sender: noreply@kitehub.me, subject: "Trần Thị Hằng mời anh làm Quản lý — Trung tâm Anh ngữ Sky Education") + body có nút "Chấp nhận lời mời" + mũi tên đỏ chỉ vào nút. -->

Anh Tâm nhận email tại địa chỉ chị Hằng nhập khi mời:

- **Sender:** `noreply@kitehub.me`
- **Subject:** "Trần Thị Hằng mời anh làm Quản lý — Trung tâm Anh ngữ Sky Education"
- **Body:**
  > Xin chào anh Tâm,
  >
  > Chị Trần Thị Hằng (Chủ trung tâm) mời anh làm **Quản lý** tại Trung tâm Anh ngữ Sky Education.
  >
  > Quyền Manager bao gồm:
  > - ✅ Quản lý lớp + chấm công + báo cáo
  > - ✅ Mời thêm giáo viên + học sinh
  > - ❌ Không có quyền billing / branding / sa thải nhân viên
  >
  > [Chấp nhận lời mời] (nút CTA màu primary)
  >
  > Link hết hạn sau **7 ngày**.

Nếu không thấy email: kiểm tra **Spam/Promotions**.

---

## 2. Click link chấp nhận

<!-- Screenshot placeholder: capture invite-accept-step-2.png — 1440×900 vi-VN — show URL bar có /accept-invite?token=<uuid> + page hiển thị thông tin trung tâm (Sky Education / 120 học sinh / Hải Phòng) + nút "Tôi đồng ý làm Quản lý" + viền vàng khoanh nút. -->

Click link **"Chấp nhận lời mời"** → mở browser tab với URL:

```
https://kitehub.me/accept-invite?token=<uuid-one-time>
```

Trang `/accept-invite` hiển thị **xác nhận thông tin**:

- 🏫 Tên trung tâm: Trung tâm Anh ngữ Sky Education
- 📍 Địa chỉ: 123 Lê Lợi, Hải Phòng
- 👥 Quy mô: 120 học sinh · 8 nhân viên
- 👑 Chủ trung tâm: Trần Thị Hằng
- 🎯 Vai trò anh: **Quản lý (STAFF / MANAGER)**

Click **"Tôi đồng ý làm Quản lý"** → chuyển sang Bước 3.

---

## 3. Đặt mật khẩu mới

<!-- Screenshot placeholder: capture invite-accept-step-3.png — 1440×900 vi-VN — show form 2 password fields (Mật khẩu / Xác nhận mật khẩu) + password strength meter (Mạnh — màu xanh) + checkbox "Đồng ý điều khoản" + sample data hint. -->

Form đặt password mới:

| Trường | Yêu cầu |
|---|---|
| **Mật khẩu** | Tối thiểu 12 ký tự, có chữ hoa + chữ thường + số + ký tự đặc biệt |
| **Xác nhận mật khẩu** | Phải khớp |
| **Đồng ý** | ☑ Tôi đồng ý [Điều khoản dịch vụ](/legal/terms) + [Chính sách riêng tư](/legal/privacy) |

**Password strength meter** hiển thị:
- ❌ Yếu (đỏ): <8 ký tự
- ⚠️ Trung bình (vàng): 8-11 ký tự
- ✅ Mạnh (xanh lá): ≥12 ký tự + variety

Click **"Hoàn tất"** → BE tạo account → JWT issued → redirect `/dashboard`.

---

## 4. First-login tour — Permission matrix

<!-- Screenshot placeholder: capture invite-accept-step-4.png — 1440×900 vi-VN — show /dashboard với overlay tour modal title "Chào mừng anh Tâm 🎉" + permission matrix table (CÓ quyền / KHÔNG quyền với 5 row mỗi cột) + nút "Bắt đầu". -->

Lần đầu đăng nhập, Manager thấy **overlay tour** giới thiệu quyền hạn:

### CÓ quyền (5 mục)

- ✅ **Quản lý lớp** — Tạo / sửa / xoá lớp, gán giáo viên
- ✅ **Chấm công** — Điểm danh học sinh hàng buổi
- ✅ **Báo cáo + bảng điểm** — Xuất Excel, gửi phụ huynh
- ✅ **Mời nhân viên** — Mời giáo viên + trợ giảng + học sinh
- ✅ **Quản lý học sinh** — CRUD học sinh + phụ huynh

### KHÔNG quyền (5 mục)

- ❌ **Billing + thanh toán** — Chỉ chị Hằng (OWNER) duyệt hoá đơn
- ❌ **Branding + logo** — Chỉ OWNER đổi
- ❌ **Suspend / xoá tenant** — Chỉ Platform Admin
- ❌ **Sa thải nhân viên** — Chỉ OWNER
- ❌ **Xoá data permanent** — Soft delete OK, hard delete chỉ OWNER

Chi tiết: xem [Permissions](permissions.md).

Click **"Bắt đầu"** → tour đóng → vào Dashboard Manager.

---

## 5. Dashboard Manager

<!-- Screenshot placeholder: capture invite-accept-step-5.png — 1440×900 vi-VN — show /dashboard với 4 KPI cards (Hoá đơn pending / Đơn xin nghỉ / Chấm công tuần / Học sinh mới) + sidebar Manager scope. -->

Dashboard Manager (`/dashboard`) khác Dashboard OWNER:

- ❌ **KHÔNG có:** Doanh thu tổng (chỉ OWNER thấy)
- ✅ **CÓ:**
  - 📋 Hoá đơn pending: **8** (chuyển chị Hằng duyệt)
  - 🏖️ Đơn xin nghỉ: **3** (giáo viên xin nghỉ)
  - ✅ Tỷ lệ chấm công: **95%** (học sinh tới lớp tuần này)
  - 👥 Học sinh mới tuần: **5** (đăng ký mới)

Sidebar có: Dashboard / Lớp học / Học sinh / Chấm công / Báo cáo / Cài đặt cá nhân.

---

## 6. Troubleshooting

| Vấn đề | Cách khắc phục |
|---|---|
| Không nhận được email mời | Chị Hằng resend từ `/team` → "Gửi lại lời mời" |
| Link hết hạn (>7 ngày) | Chị Hằng tạo invite mới |
| Token invalid | Copy nguyên link, không forward email |
| Password yếu | Tăng độ dài ≥12 ký tự + ký tự đặc biệt |
| Vào /admin/* → 403 Forbidden | Đúng — Manager không có quyền admin (xem [permissions](permissions.md)) |
| Không thấy tour first-login | LocalStorage có thể đã lưu → mở `/help/p3-manager` để xem lại |

---

## 7. Bước tiếp theo

- 🛡️ [Permissions — Quyền hạn chi tiết](permissions.md)
- 📅 [Daily Operations — Việc hàng ngày](daily-ops.md)
- 📊 [Báo cáo + Bảng điểm](reports.md)
- 👥 [Mời giáo viên + học sinh](../p2-owner/invite-staff.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 💬 Zalo OA: zalo.me/kitehub (đang triển khai — Phase 1.5)
- 📞 Hotline beta: 1900-xxxx (giờ hành chính)
- 🐛 Báo lỗi trang này: [mailto:support@kitehub.me?subject=Lỗi /help/p3-manager/invite-accept](mailto:support@kitehub.me?subject=L%E1%BB%97i%20%2Fhelp%2Fp3-manager%2Finvite-accept)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
