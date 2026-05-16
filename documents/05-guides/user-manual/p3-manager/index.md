---
persona: p3-center-manager
topic: index
last-updated: 2026-05-16
version: v0.9.0-beta
effort_minutes: 4
---

# Hướng dẫn Quản lý trung tâm — Tổng quan

> 📅 Cập nhật lần cuối: **2026-05-16** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **4 phút**

## TL;DR

Anh Tâm (32 tuổi) là Quản lý trung tâm Sky Education do chị Hồng mời. Vai trò STAFF — làm 80% việc hành chính nhưng KHÔNG quyền billing/branding/delete.

- 🎯 **Vai trò:** STAFF (Manager) — đứng giữa OWNER và TEACHER
- 📋 **Việc hàng ngày:** Xếp lịch · Chấm công · Báo cáo · Quản lý lớp
- ❌ **KHÔNG quyền:** Duyệt hoá đơn · Đổi branding · Xoá tenant · Mời OWNER
- ✅ **CÓ quyền:** Mời TEACHER · Tạo lớp · Sửa lịch · Xem báo cáo

---

## 1. Sau khi accept invite

<!-- Screenshot placeholder pending B+C merge: capture index-step-1.png — 1440×900 vi-VN — show first-login overlay tour với 4 step (Dashboard / Lớp học / Chấm công / Báo cáo) + nút "Tiếp tục" mũi tên đỏ -->

Lần đầu đăng nhập, anh Tâm thấy tour overlay 4 bước:

1. **Dashboard** — KPI tuần (số học sinh, doanh thu chị duyệt, ...)
2. **Lớp học** — Tất cả lớp đang dạy + thêm lớp mới
3. **Chấm công** — Mobile-friendly điểm danh
4. **Báo cáo** — Read-only access báo cáo cho chị Hằng

Skip tour → vào `/dashboard`.

---

## 2. Dashboard Manager (giới hạn so với OWNER)

<!-- Screenshot placeholder pending B+C merge: capture index-step-2.png — 1440×900 vi-VN — show /dashboard Manager view với 4 cards (Lớp tuần này 15 / Học sinh active 120 / Đơn xin nghỉ 2 / Chấm công 95%) + sidebar KHÔNG có Billing/Branding menu so với OWNER + mũi tên đỏ chỉ vào sidebar -->

URL: `/dashboard`

| Card | Hiện | Click → |
|---|---|---|
| 📅 **Lớp tuần này** | 15 lớp | `/schedule` |
| 👨‍🎓 **Học sinh active** | 120 hs | `/students` |
| 🏖️ **Đơn xin nghỉ chờ duyệt** | 2 đơn | `/leave-requests` |
| ✅ **Chấm công tuần** | 95% | `/attendance` |

Khác với OWNER:
- ❌ KHÔNG có card "Doanh thu tuần" (chỉ chị Hằng thấy)
- ❌ KHÔNG có "Hoá đơn pending duyệt"
- ❌ Sidebar KHÔNG có menu `Billing` / `Branding` / `Danger zone`

---

## 3. 4 module Manager dùng hàng ngày

### 3.1 Daily operations (chấm công + lớp)

URL: `/operations` — xem [Vận hành hàng ngày](daily-operations.md).

### 3.2 Reports (read-only)

URL: `/reports` — xem [Báo cáo](reports.md).

### 3.3 Permissions visibility

Biết rõ mình KHÔNG quyền gì → xem [Quyền hạn STAFF](permissions.md).

### 3.4 Accept invite (lần đầu)

Xem [Quy trình nhận invite từ Owner](accept-invite.md) hoặc [Hướng dẫn chi tiết invite-accept](invite-accept.md).

### 3.5 Daily ops workflow tóm tắt

Xem [Daily Operations workflow 1 ngày](daily-ops.md) cho tóm tắt 30-phút-mỗi-sáng.

---

## 4. Routine một ngày Manager

| Giờ | Việc | Module |
|---|---|---|
| 7h45 | Mở dashboard, check lịch sáng | `/dashboard` |
| 8h00 | Verify chấm công đầu giờ (phone hoặc tablet) | `/attendance` |
| 9h-11h | Duyệt đơn xin nghỉ → escalate chị Hằng nếu cần | `/leave-requests` |
| 11h-12h | Tạo lớp mới nếu có đăng ký, xếp giáo viên | `/classes/new` |
| Chiều | Theo dõi chấm công ca chiều, support GV | `/attendance` |
| 17h | Verify doanh thu ngày, gửi báo cáo chị Hằng | `/reports?period=today` |

---

## 5. Khi cần làm việc chị Hằng phải duyệt

Anh Tâm hành động 80%, escalate chị Hằng cho 20% còn lại:

| Việc | Anh Tâm | Chị Hằng |
|---|:---:|:---:|
| Duyệt hoá đơn học sinh | ❌ | ✅ |
| Tạo lớp mới | ✅ | — |
| Đổi học phí 1 lớp | ❌ | ✅ |
| Đuổi học sinh quá hạn | ❌ | ✅ |
| Mời giáo viên mới | ✅ | — |
| Sa thải giáo viên | ❌ | ✅ |
| Đổi branding | ❌ | ✅ |
| Xuất báo cáo tháng | ✅ | ✅ |

Khi anh Tâm click vào action restricted → UI hiển thị tooltip "Quyền này thuộc Chủ trung tâm. Vui lòng liên hệ Trần Thị Hồng" + button "Tạo yêu cầu duyệt".

---

## 6. Tiếp theo

- 📋 [Vận hành hàng ngày](daily-operations.md)
- 📊 [Báo cáo](reports.md)
- 🛡️ [Quyền hạn STAFF — tôi không làm được gì?](permissions.md)
- 📨 [Quy trình accept invite](accept-invite.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 👥 Hỏi chị Hằng (OWNER) trực tiếp qua Zalo nội bộ
- 📞 Hotline: 1900-xxxx (giờ hành chính)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
