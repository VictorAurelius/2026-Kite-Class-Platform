# Trial Lifecycle — Use Cases

### UC-TR-01: Đăng ký Trial mới (Self-Service)
- **Actor:** Visitor (chưa có tài khoản)
- **Precondition:** Subdomain chưa tồn tại, email chưa đăng ký
- **Steps:**
  1. FE: hiển thị form đăng ký (orgName, subdomain, email, password)
  2. User: điền thông tin và submit
  3. System: validate subdomain không nằm trong reserved list (27 names)
  4. System: validate subdomain uniqueness (non-deleted instances)
  5. System: validate email uniqueness
  6. System: tạo instance (tier=FREE, status=TRIAL), start trial 14 ngày
  7. System: provision database async
  8. System: gửi welcome email
  9. System: trả về JWT access + refresh tokens
- **Postcondition:** Instance TRIAL, user có thể login ngay
- **Errors:**
  - 409: subdomain already taken → "Subdomain đã được sử dụng"
  - 409: email already exists → "Email đã đăng ký"
  - 400: subdomain reserved → "Subdomain không hợp lệ"
- **FE Behavior:** Redirect về dashboard sau đăng ký thành công

### UC-TR-02: Trial Expiration (Tự động)
- **Actor:** System (TrialExpirationChecker — daily 8 AM)
- **Precondition:** Instance đang TRIAL, trialExpiresAt đã qua
- **Steps:**
  1. System: scan instances TRIAL có trialExpiresAt <= now
  2. System: gửi warning emails tại ngày 11 (3 days left) và ngày 13 (1 day left)
  3. System: tại ngày 14 → set status = SUSPENDED
  4. System: gửi trial-expired email
  5. System: user không thể truy cập instance
- **Postcondition:** Instance SUSPENDED, data retention clock bắt đầu
- **FE Behavior:** Redirect về upgrade page khi detect instance SUSPENDED

### UC-TR-03: Nâng cấp trong Trial
- **Actor:** Owner (đang trong trial)
- **Precondition:** Instance status = TRIAL, chưa hết hạn
- **Steps:**
  1. FE: hiển thị upgrade prompt (trial countdown visible)
  2. User: chọn plan và thanh toán
  3. System: create subscription, set instance status = ACTIVE
  4. System: gửi subscription-created email
- **Postcondition:** Instance ACTIVE, trial kết thúc sớm (zero downtime)
- **Errors:**
  - 409: already has active subscription → conflict

### UC-TR-04: Admin Extend Trial
- **Actor:** Admin
- **Precondition:** Instance đang TRIAL, chưa bị xóa
- **Steps:**
  1. Admin: gọi POST /api/platform/instances/{id}/extend-trial?days=N
  2. System: validate days > 0, max total trial <= 90 ngày
  3. System: trialExpiresAt += N days
- **Postcondition:** Trial period được kéo dài
- **Errors:**
  - 400: days <= 0 → invalid
  - 400: exceeds max 90 days total

### UC-TR-05: Xem Trial Status
- **Actor:** Owner
- **Precondition:** Instance đang TRIAL
- **Steps:**
  1. FE: gọi GET /api/platform/instances/{id}/trial-status
  2. System: trả về daysLeft, trialExpiresAt, status
- **FE Behavior:** Hiển thị countdown banner trong dashboard
