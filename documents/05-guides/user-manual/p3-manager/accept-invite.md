---
persona: p3-center-manager
topic: accept-invite
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 3
---

# Accept invite — Quy trình lần đầu tham gia

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **3 phút**

## TL;DR

Anh Tâm nhận email mời từ chị Hằng → click link → tạo password → vào dashboard. Toàn bộ ~2 phút.

- 📧 **Nguồn:** Email tiếng Việt từ `noreply@kitehub.me`
- ⏱️ **TTL link:** 7 ngày (sau đó cần resend)
- 🔐 **Bảo mật:** Password ≥8 ký tự, chữ + số
- 📱 **Mobile:** Có thể accept trên điện thoại

---

## 1. Email invite từ chị Hằng

<!-- Screenshot placeholder pending B+C merge: capture accept-invite-step-1.png — 375×812 mobile vi-VN — show Gmail mobile inbox với email "Trần Thị Hồng mời bạn tham gia Sky Education" subject + preview "Bạn được mời với vai trò Quản lý" + mũi tên đỏ chỉ vào subject -->

Email từ `noreply@kitehub.me` với subject:

> **Trần Thị Hồng mời bạn tham gia Sky Education trên KiteHub**

Nội dung tiếng Việt:

> Xin chào Anh Nguyễn Văn An,
>
> **Trần Thị Hồng** (chủ trung tâm Anh ngữ Sky Education) đã mời bạn tham gia với vai trò **Quản lý**.
>
> Bạn sẽ phụ trách:
> - Quản lý lịch lớp + chấm công
> - Mời và quản lý giáo viên
> - Báo cáo cho chủ trung tâm
>
> [**Chấp nhận lời mời**](https://app.kitehub.me/invite/abc123...) (link hết hạn sau 7 ngày)
>
> Trân trọng,
> KiteHub Team

---

## 2. Click invite link

<!-- Screenshot placeholder pending B+C merge: capture accept-invite-step-2.png — 1440×900 vi-VN — show /invite/{token} landing page "Bạn được mời tham gia Sky Education" với pre-filled form (Email an@sky-edu.vn / Role STAFF) + form tạo password + checkbox điều khoản + nút Chấp nhận mũi tên đỏ -->

Click link → mở `/invite/{token}` page với:

### 2.1 Thông tin invite (pre-filled, không edit được)

| Field | Giá trị |
|---|---|
| Trung tâm | Sky Education |
| Người mời | Trần Thị Hồng (Owner) |
| Email của bạn | an@sky-edu.vn |
| Vai trò | STAFF (Quản lý) |
| Hết hạn | 22/05/2026 (7 ngày) |

### 2.2 Form tạo tài khoản

| Field | Yêu cầu |
|---|---|
| Họ tên hiển thị | Nguyễn Văn An (pre-filled, có thể sửa) |
| Số điện thoại | optional (cho SMS reminder) |
| Tạo mật khẩu | ≥8 ký tự, có chữ + số |
| Xác nhận mật khẩu | Phải trùng |
| Đọc điều khoản | ☐ Tôi đã đọc [Điều khoản dịch vụ](/terms) |

Click **Chấp nhận và tạo tài khoản** → hệ thống:
1. Verify token còn hợp lệ (`expires_at > now()`)
2. Tạo user trong DB với password hash (BCrypt)
3. Tự động đăng nhập + redirect `/dashboard`
4. Invite token marked `accepted_at=now()`, không reuse được

---

## 3. Lần đầu đăng nhập

<!-- Screenshot placeholder pending B+C merge: capture accept-invite-step-3.png — 1440×900 vi-VN — show first-login overlay tour với 4 step (Dashboard / Lớp học / Chấm công / Báo cáo) + nút "Tiếp tục" hoặc "Bỏ qua tour" mũi tên đỏ -->

Tour overlay 4 bước:

1. **Dashboard** — KPI tuần
2. **Lớp học** — kéo-thả lịch
3. **Chấm công** — tap-check mobile
4. **Báo cáo** — read-only access

Skip → vào `/dashboard`.

Modal welcome cũng hiển thị link tới [Hướng dẫn Manager](index.md) để anh Tâm đọc kỹ trong 30 phút đầu.

---

## 4. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| Không nhận email invite | Spam folder / sai email chị Hằng nhập | Search "kitehub" trong Gmail; báo chị Hằng resend |
| Link báo "Đã hết hạn" | >7 ngày từ khi gửi | Báo chị Hằng resend invite |
| Link báo "Token không hợp lệ" | Đã accept rồi, hoặc bị invalidate | Login bằng email cũ + password đã tạo |
| Password tạo fail "Quá yếu" | <8 ký tự hoặc thiếu chữ/số | Theo hint hiển thị trong form |
| Sau accept thấy "403 Forbidden" trên một số page | Đang là STAFF, không phải OWNER | Đây là đúng (xem [Quyền hạn STAFF](permissions.md)) |

---

## 5. Sau accept

Anh Tâm sẵn sàng dùng KiteHub. Đọc các trang khác:

- [Tổng quan Manager](index.md) — 4 phút
- [Vận hành hàng ngày](daily-operations.md) — 6 phút
- [Báo cáo](reports.md) — 4 phút
- [Quyền hạn STAFF](permissions.md) — 4 phút

Tổng ~18 phút đọc → sẵn sàng làm việc.

---

## 6. Đổi mật khẩu sau

URL: `/settings/security`

- Đổi password
- Bật 2FA (TOTP với Google Authenticator)
- Xem session đang active, có thể revoke

---

## 7. Liên kết

- [Tổng quan Manager](index.md)
- [Vận hành hàng ngày](daily-operations.md)
- [Quyền hạn STAFF](permissions.md)
- Wave 80 Bucket B: InvitationController + accept-invite route

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 👥 Hỏi chị Hằng (người mời) trực tiếp
- 📞 Hotline: 1900-xxxx
- 📊 Trạng thái beta: [/beta-status](/beta-status)
