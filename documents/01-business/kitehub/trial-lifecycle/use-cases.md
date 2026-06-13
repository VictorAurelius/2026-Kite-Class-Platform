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
  2. System: gửi warning emails theo cadence widened (TR-03/TR-08, GAP-1270) — `warning-days: [10,5,3,1]` = trial day 4/9/11/13 (4 touch-points thay vì 2, tăng conversion). Email midpoint ngày 7 (`midpoint-day`).
  3. System: tại ngày 14 → nếu `auto-extend-on-expiry: true` (TR-08) auto-grant 1 extension `extension-days: 7` (rescue) thay vì suspend; mặc định `false` → set status = SUSPENDED qua `instance.setStatus(SUSPENDED)` (stamp `suspended_at` — SUB-25)
  4. System: gửi trial-expired email
  5. System: user không thể truy cập instance (trừ khi được rescue-extend)
- **Postcondition:** Instance SUSPENDED (stamp `suspended_at` → retention clock bắt đầu deterministic) HOẶC trial extended thêm 7 ngày nếu auto-extend bật
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

### UC-TR-06: Trial extension / rescue cadence (TR-08, GAP-1270)
- **Actor:** System (TrialExpirationChecker auto-extend) HOẶC Admin (manual, UC-TR-04)
- **Precondition:** Instance TRIAL sắp/đã hết hạn; chưa từng được extend quá max-total 90 ngày
- **Steps:**
  1. **Auto-extend (rescue, default off):** khi `auto-extend-on-expiry: true` và trial chạm ngày 14 chưa convert → System auto-grant 1 lần extension `extension-days: 7` thay vì suspend ngay (giảm involuntary trial-churn). Áp dụng tối đa 1 lần rescue/instance.
  2. **Admin manual (UC-TR-04):** Admin POST `/api/platform/instances/{id}/extend-trial?days=N` — validate `days>0`, tổng trial ≤ 90 ngày.
  3. System: `trialExpiresAt += extension/N days`; gửi email thông báo gia hạn; cadence warning (TR-03) tiếp tục cho window mới.
- **Postcondition:** Trial kéo dài; owner có thêm touch-point convert (TR-08 rationale: ngành dùng 5-7 email cadence vs 3 trước đây).
- **Errors:** 400 days ≤ 0; 400 vượt max 90 ngày total; auto-extend skip nếu đã rescue 1 lần
- **Note:** Auto-extend mặc định `false` Phase 1 BETA — bật khi A/B test cho thấy rescue tăng conversion. Business-value (extension length / max-total / auto-extend policy) queued GAP-156 per `business-logic-review.md`.
