---
persona: p2-center-owner
topic: invite-staff
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 5
---

# Mời Manager + Giáo viên — Hướng dẫn cho Chủ trung tâm

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **5 phút**

## TL;DR

Chị Hằng mời anh Tâm (Quản lý) + 5 giáo viên (Mai, Lan, ...) qua email. Mỗi người nhận invite link TTL 7 ngày, click link để tạo tài khoản với role phù hợp.

- 📧 **Cách mời:** Nhập email + chọn role → hệ thống gửi invite link
- ⏱️ **TTL invite link:** 7 ngày
- 🛡️ **3 role:** OWNER (chị) · STAFF (anh Tâm) · TEACHER (Mai, Lan, ...)
- 📊 **Quota PRO:** ≤10 giáo viên + 1 Manager

---

## 1. Mở trang Team

<!-- Screenshot placeholder pending B+C merge: capture invite-staff-step-1.png — 1440×900 vi-VN — show /team list với 4 rows (Trần Thị Hồng OWNER / Nguyễn Văn An STAFF / Phạm Thị Mai TEACHER / Lê Thị Lan TEACHER) + nút "Mời thành viên mới" mũi tên đỏ -->

URL: `/team`

Danh sách hiện tại:

| Tên | Role | Email | Trạng thái |
|---|---|---|---|
| Trần Thị Hồng (chị) | OWNER | hong@sky-edu.vn | ACTIVE |
| Nguyễn Văn An | STAFF | an@sky-edu.vn | ACTIVE |
| Phạm Thị Mai | TEACHER | mai@sky-edu.vn | ACTIVE |
| Lê Thị Lan | TEACHER | lan@sky-edu.vn | ACTIVE |

Click **+ Mời thành viên mới** → mở form invite.

---

## 2. Form invite

<!-- Screenshot placeholder pending B+C merge: capture invite-staff-step-2.png — 1440×900 vi-VN — show invite form với input "Email", dropdown "Role" (TEACHER), textarea "Lời mời cá nhân", checkbox "Gửi notification Zalo" + nút Gửi mũi tên đỏ -->

| Field | Bắt buộc | Mô tả |
|---|:---:|---|
| **Email** | ✅ | Email người nhận (verify format) |
| **Họ tên** | ✅ | Hiển thị trong app |
| **Role** | ✅ | STAFF (Manager) hoặc TEACHER |
| **Số điện thoại** | optional | Để gửi SMS reminder |
| **Lời mời cá nhân** | optional | Sẽ chèn vào email (tiếng Việt) |
| **Gửi notification Zalo** | optional | Cần Zalo OA active (Phase 1.5+) |

Click **Gửi lời mời** → hệ thống:
1. Tạo row `invitations` với token UUID + `expires_at=now()+7d`
2. Gửi email tới người nhận với invite link `https://app.kitehub.me/invite/{token}`
3. Hiển thị toast "Đã gửi lời mời tới mai@sky-edu.vn"

---

## 3. Người nhận accept invite

### 3.1 Click invite link trong email

<!-- Screenshot placeholder pending B+C merge: capture invite-staff-step-3.png — 1440×900 vi-VN — show /invite/{token} landing page "Bạn được Trần Thị Hồng mời tham gia Sky Education với vai trò Giáo viên" + form Tạo mật khẩu + nút Chấp nhận mũi tên đỏ -->

Email người nhận có nội dung:

> Xin chào Phạm Thị Mai,
>
> Trần Thị Hồng đã mời bạn tham gia **Sky Education** trên KiteHub với vai trò **Giáo viên**.
>
> [Chấp nhận lời mời và tạo tài khoản](https://app.kitehub.me/invite/abc-123)
>
> Lời mời hết hạn sau 7 ngày (22/05/2026).

Click link → mở trang `/invite/{token}` với form:
- Họ tên (pre-filled từ invite)
- Email (pre-filled, không edit được)
- Tạo mật khẩu (≥8 ký tự, chữ + số)
- Xác nhận mật khẩu

Click **Chấp nhận** → tài khoản tạo, auto đăng nhập vào `/dashboard`.

### 3.2 Lần đầu đăng nhập

Tour overlay hiển thị 4 step giới thiệu:
1. Đây là dashboard tổng quan
2. Lịch học của bạn (sidebar)
3. Điểm danh / Bài tập (top nav)
4. Cài đặt profile + đổi password

Skip tour → vào dashboard.

---

## 4. Quản lý team sau khi invite

### 4.1 Edit role

<!-- Screenshot placeholder pending B+C merge: capture invite-staff-step-4.png — 1440×900 vi-VN — show member detail "Phạm Thị Mai" với dropdown role + history "Joined 14/05/2026, Last active 15/05/2026" + nút Save mũi tên đỏ -->

Click row team member → detail view → có thể:
- Đổi role (TEACHER → STAFF nếu promote)
- Khoá tạm thời (suspend account, không xoá)
- Reset password (gửi link reset email)
- Xem audit log (mọi action của user này)

### 4.2 Xoá thành viên (offboarding)

Khi giáo viên nghỉ việc:

1. Detail view → **Action** → **Khoá tài khoản**
2. Tích checkbox: "Chuyển lớp đang dạy cho giáo viên khác" → chọn người thay
3. Confirm → tài khoản bị khoá đăng nhập, data history giữ lại 90 ngày

Sau 90 ngày, có thể hard-delete qua **Action** → **Xoá vĩnh viễn**.

---

## 5. Resend / Cancel invite

Tab **Pending invites** trong `/team`:

| Email | Role | Sent | Expires | Action |
|---|---|---|---|---|
| lan-new@gmail.com | TEACHER | 13/05/2026 | 20/05/2026 | [Resend] [Cancel] |

- **Resend:** Gửi lại email + reset TTL 7 ngày
- **Cancel:** Hủy invite, token invalidate ngay

---

## 6. Quota theo tier

| Tier | OWNER | STAFF | TEACHER | Tổng |
|---|:---:|:---:|:---:|:---:|
| FREE | 1 | 0 | 3 | 4 |
| PRO | 1 | 1 | 10 | 12 |
| PREMIUM | 1 | 3 | 30 | 34 |
| ENTERPRISE | 1 | ∞ | ∞ | ∞ |

Khi vượt quota → form invite báo "Bạn đã đạt giới hạn 10 giáo viên gói PRO. Nâng cấp PREMIUM để thêm." kèm link `/billing/plans`.

---

## 7. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| Người nhận không thấy email invite | Spam folder / email sai | Resend invite, kiểm tra `/admin/email-events` |
| Click invite link báo "Hết hạn" | TTL >7 ngày | Resend từ Pending invites tab |
| Tạo password fail "Mật khẩu yếu" | <8 ký tự hoặc thiếu chữ/số | Theo hint trong form |
| Accept xong nhưng dashboard trống | Cache | F5 hoặc logout + login lại |

---

## 8. Liên kết

- [Tổng quan Chủ trung tâm](index.md)
- [Bảng giá + Thanh toán](pricing-billing.md)
- [Quản lý role STAFF](../p3-manager/index.md) — góc nhìn anh Tâm sau khi nhận invite

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 📞 Hotline: 1900-xxxx (giờ hành chính)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
