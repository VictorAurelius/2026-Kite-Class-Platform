---
persona: p3-center-manager
topic: permissions
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 4
---

# Quyền hạn STAFF — Tôi không làm được gì?

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **4 phút**

## TL;DR

Anh Tâm có role STAFF — làm được 80% việc hành chính nhưng có 20% phải chị Hằng (OWNER) duyệt. Trang này liệt kê rõ để anh Tâm khỏi nhầm + biết khi nào escalate.

- ✅ **CÓ quyền:** Quản lý lớp · Chấm công · Mời GV · Báo cáo · Học sinh
- ❌ **KHÔNG quyền:** Billing · Branding · Suspend tenant · Sa thải GV · Xoá data
- 🛡️ **RBAC enforced:** Backend chặn API call sai role, UI cũng ẩn nút bằng RoleGuard
- 📞 **Khi cần làm action restricted:** Tạo yêu cầu chuyển chị Hằng duyệt

---

## 1. Matrix quyền hạn đầy đủ

<!-- Screenshot placeholder pending B+C merge: capture permissions-step-1.png — 1440×900 vi-VN — show /settings/my-permissions read-only view với danh sách 30 action × Allow/Deny check column theo STAFF role + tooltip giải thích cho mỗi Deny -->

URL: `/settings/my-permissions`

### 1.1 Quản lý lớp

| Action | STAFF | OWNER |
|---|:---:|:---:|
| Xem lịch lớp | ✅ | ✅ |
| Tạo lớp mới | ✅ | ✅ |
| Sửa thông tin lớp | ✅ | ✅ |
| Xoá lớp (không có hs đăng ký) | ✅ | ✅ |
| Xoá lớp (đã có hs) | ❌ | ✅ |
| Đổi học phí lớp | ❌ | ✅ |
| Đổi giáo viên lớp | ✅ | ✅ |

### 1.2 Chấm công

| Action | STAFF | OWNER |
|---|:---:|:---:|
| Điểm danh học sinh | ✅ | ✅ |
| Sửa điểm danh (≤24h) | ✅ | ✅ |
| Sửa điểm danh (>24h) | ❌ | ✅ |
| Xem báo cáo chấm công | ✅ | ✅ |

### 1.3 Học sinh

| Action | STAFF | OWNER |
|---|:---:|:---:|
| Thêm học sinh mới | ✅ | ✅ |
| Sửa thông tin học sinh | ✅ | ✅ |
| Đổi lớp học sinh | ✅ | ✅ |
| Hoàn học phí học sinh nghỉ | ❌ | ✅ |
| Hard delete học sinh | ❌ | ✅ |

### 1.4 Giáo viên

| Action | STAFF | OWNER |
|---|:---:|:---:|
| Mời TEACHER mới | ✅ | ✅ |
| Mời STAFF mới (Manager) | ❌ | ✅ |
| Mời OWNER khác | ❌ | ✅ |
| Sửa profile GV | ✅ | ✅ |
| Đổi lương GV | ❌ | ✅ |
| Suspend GV (≤1 tuần) | ✅ | ✅ |
| Sa thải GV | ❌ | ✅ |

### 1.5 Đơn xin nghỉ

| Action | STAFF | OWNER |
|---|:---:|:---:|
| Duyệt nghỉ ≤3 ngày | ✅ | ✅ |
| Duyệt nghỉ >3 ngày | ❌ | ✅ |
| Duyệt nghỉ phép năm | ❌ | ✅ |
| Duyệt nghỉ không lương | ❌ | ✅ |

### 1.6 Billing + Tài chính

| Action | STAFF | OWNER |
|---|:---:|:---:|
| Xem hoá đơn học sinh | ✅ | ✅ |
| Duyệt hoá đơn học sinh | ❌ | ✅ |
| Xem invoice nội bộ (lương GV) | ❌ | ✅ |
| Duyệt invoice nội bộ | ❌ | ✅ |
| Upgrade/downgrade tier | ❌ | ✅ |
| Đổi phương thức thanh toán | ❌ | ✅ |

### 1.7 Branding + Settings

| Action | STAFF | OWNER |
|---|:---:|:---:|
| Xem branding hiện tại | ✅ | ✅ |
| Đổi logo | ❌ | ✅ |
| Đổi màu chủ đạo | ❌ | ✅ |
| Đổi email signature | ❌ | ✅ |
| Đổi tên trung tâm | ❌ | ✅ |
| Đổi domain slug | ❌ | ✅ |

### 1.8 Danger zone

| Action | STAFF | OWNER |
|---|:---:|:---:|
| Xuất data PDPL | ❌ | ✅ |
| Suspend tenant | ❌ | ✅ |
| Xoá tenant vĩnh viễn | ❌ | ✅ |

---

## 2. Khi UI ẩn nút

<!-- Screenshot placeholder pending B+C merge: capture permissions-step-2.png — 1440×900 vi-VN — show /billing page Manager view với nút "Duyệt hoá đơn" GREYED OUT + tooltip "Quyền này thuộc Chủ trung tâm. Click để tạo yêu cầu duyệt" + mũi tên đỏ chỉ vào tooltip -->

Khi anh Tâm vào trang chứa action không có quyền (vd `/billing`), nút action sẽ:
- **Greyed out** (disabled state, opacity 50%)
- **Tooltip** khi hover: "Quyền này thuộc Chủ trung tâm. Vui lòng liên hệ Trần Thị Hồng."
- **Nút "Tạo yêu cầu duyệt"** thay thế

Click "Tạo yêu cầu duyệt" → form gửi chị Hằng:
- Action gì
- Lý do
- Mức độ ưu tiên (P0/P1/P2)
- Deadline

Chị Hằng nhận notification → vào `/owner/pending-requests` để duyệt.

---

## 3. Backend bảo vệ (defense-in-depth)

Ngay cả nếu anh Tâm bypass UI (vd manipulate JavaScript), backend vẫn từ chối:

- `@PreAuthorize("hasRole('OWNER')")` annotation trên Spring Boot controller
- JWT chứa `role=STAFF` → API trả `HTTP 403 Forbidden`
- Audit log ghi nhận attempt: `permission.denied`

Bypass không xảy ra trên thực tế, nhưng kiến trúc này đảm bảo PDPL compliance.

---

## 4. Khi tự dưng cảm thấy "có quyền" mà không click được

<!-- Screenshot placeholder pending B+C merge: capture permissions-step-3.png — 1440×900 vi-VN — show /settings/my-permissions search box "duyệt hoá đơn" → result hiển thị "Action này thuộc OWNER role. Bạn đang là STAFF" + giải thích chi tiết -->

Search action trong `/settings/my-permissions` → trả kết quả rõ ràng:
- Action `X` thuộc role nào
- Bạn đang là role gì
- Nếu cần upgrade → liên hệ chị Hằng

Tránh confusion + giảm hỗ trợ ticket.

---

## 5. Khi role được nâng STAFF → OWNER

Hiếm, nhưng nếu chị Hằng quyết định cho anh Tâm thành đồng-chủ:

1. Chị Hằng vào `/team/{anh-tâm-id}` → dropdown role → chọn `OWNER`
2. Confirm với 2FA (vì action sensitive)
3. Anh Tâm logout/login lại → JWT mới có role `OWNER`
4. Audit log: `role.upgrade` với reason

Sau đó anh Tâm thấy đủ menu trong sidebar (Billing, Branding, Danger zone).

---

## 6. Liên kết

- [Tổng quan Manager](index.md)
- [Vận hành hàng ngày](daily-operations.md)
- [Accept invite từ Owner](accept-invite.md)
- Wave 80 Bucket C: RBAC defense-in-depth (`@PreAuthorize` + RoleGuard)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 👥 Hỏi chị Hằng trực tiếp khi cần upgrade quyền
- 📊 Trạng thái beta: [/beta-status](/beta-status)
