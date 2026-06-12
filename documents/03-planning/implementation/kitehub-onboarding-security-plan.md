# Plan: Onboarding & Security PRs

**Ngày tạo:** 2026-03-19
**Dựa trên:** [kitehub-onboarding-security-gaps.md](kitehub-onboarding-security-gaps.md)
**Trạng thái:** Chờ leader duyệt

---

## Phase 1: Chống spam + Email verification (🔴 Trước khi launch)

### PR-SEC-1: Email verification + Defer DB provisioning
**Priority:** 🔴 P0
**Estimate:** 2 ngày
**Scope:**

Backend:
- [ ] Thêm `EmailVerificationToken` entity (token, userId, expiresAt)
- [ ] Sửa `AuthService.register()`: tạo User (status=UNVERIFIED) + Instance (status=PENDING), KHÔNG provision DB
- [ ] Endpoint `POST /api/auth/verify-email?token={token}`: verify → provision DB → Instance status=TRIAL
- [ ] Endpoint `POST /api/auth/resend-verification`: gửi lại email
- [ ] Token hết hạn sau 24h, auto-cleanup instances PENDING quá 48h
- [ ] Gửi email verification qua kitehub-email service (hoặc mock ở local)

Frontend:
- [ ] Sau register → redirect trang "Kiểm tra email để xác nhận"
- [ ] Trang verify-email: nhận token từ URL → gọi API → redirect dashboard
- [ ] Trang resend: cho phép gửi lại

**Kết quả:** Email fake → không provision DB → không tốn resource

---

### PR-SEC-2: Rate limit + Instance limit
**Priority:** 🔴 P0
**Estimate:** 0.5 ngày
**Scope:**

- [ ] Gateway rate limit: max 3 POST /api/auth/register per IP per giờ
- [ ] InstanceService: max 2 instances per account (FREE tier)
- [ ] Trả lỗi rõ ràng: "Bạn đã đạt giới hạn số lượng trung tâm miễn phí"

**Kết quả:** Bot/spam bị chặn ở tầng gateway, user thường không ảnh hưởng

---

## Phase 2: Onboarding (🔴 Trước khi launch)

### PR-ONBOARD-1: Welcome wizard sau đăng ký
**Priority:** 🔴 P0
**Estimate:** 1.5 ngày
**Scope:**

- [ ] Component `OnboardingWizard` (modal/dialog, 3-4 bước)
- [ ] Bước 1: "Chúc mừng! Trung tâm {name} đã sẵn sàng"
  - Hiển thị tên + URL + status TRIAL (14 ngày)
- [ ] Bước 2: "Trang quản lý của bạn"
  - Giới thiệu sidebar: Dashboard, Thanh toán, Thương hiệu, Cài đặt
- [ ] Bước 3: "Truy cập trang web trung tâm"
  - Button mở KiteClass instance URL
  - Giải thích: đây là website mà học viên/phụ huynh sẽ thấy
- [ ] Bước 4: "Bước tiếp theo" (optional)
  - Checklist: Tạo AI Branding, Thêm khóa học, Mời giáo viên
- [ ] Lưu flag `onboardingCompleted` vào localStorage
- [ ] Chỉ hiển thị 1 lần (lần đầu vào dashboard)
- [ ] Button "Xem lại hướng dẫn" ở dashboard để mở lại

**Kết quả:** User hiểu flow, biết làm gì tiếp theo, giảm churn

---

### PR-ONBOARD-2: Dashboard improvements
**Priority:** 🟠 P1
**Estimate:** 1 ngày
**Scope:**

- [ ] Quick setup checklist trên dashboard:
  ```
  ✅ Đăng ký tài khoản
  ✅ Xác nhận email
  ⬜ Tạo thương hiệu AI
  ⬜ Thêm khóa học đầu tiên
  ⬜ Mời giáo viên
  ```
- [ ] Tooltip trên instance card giải thích status (TRIAL = dùng thử 14 ngày miễn phí)
- [ ] Banner nhắc nhở khi trial còn ≤3 ngày

**Kết quả:** User luôn biết mình đang ở đâu trong flow

---

## Phase 3: Local tenant URL (🟠 Sprint sau)

### PR-LOCAL-1: Tenant URL strategy
**Priority:** 🟠 P1
**Estimate:** 1 ngày
**Scope:**

- [ ] Env variable `TENANT_URL_PATTERN`:
  - Local: `http://localhost:3000?tenant={subdomain}`
  - Production: `https://{subdomain}.kitehub.me`
- [ ] Dashboard + Instance detail: link "Truy cập KiteClass" dùng pattern trên
- [ ] KiteClass Frontend: đọc `?tenant=` query param → set header `X-Tenant-Id`
- [ ] Gateway TenantResolver: hỗ trợ query param ngoài subdomain + header (đã có)
- [ ] Docs: hướng dẫn config cho local vs production

**Kết quả:** Dev bấm link trên dashboard → mở KiteClass instance ở local

---

## Phase 4: Defense in depth (🟡 Sau launch)

### PR-SEC-3: Captcha
**Priority:** 🟡 P2
**Estimate:** 0.5 ngày
**Scope:**

- [ ] hCaptcha (hoặc reCAPTCHA) trên form đăng ký
- [ ] Backend verify captcha token trước khi xử lý register
- [ ] Bypass ở local dev (config flag)

---

### PR-SEC-4: Phone OTP (optional)
**Priority:** 🟡 P3
**Estimate:** 2-3 ngày
**Scope:**

- [ ] Tích hợp SMS gateway (Twilio / SpeedSMS / Zalo OTP)
- [ ] Verify SĐT trước khi tạo instance
- [ ] Chi phí: ~500đ/SMS

---

## Execution Order

```
PR-SEC-1 (Email verify) ──→ PR-SEC-2 (Rate limit)
                                    ↓
                             PR-ONBOARD-1 (Wizard)
                                    ↓
                             PR-ONBOARD-2 (Dashboard)
                                    ↓
                             PR-LOCAL-1 (Tenant URL)
                                    ↓
                             PR-SEC-3 (Captcha)
                                    ↓
                             PR-SEC-4 (Phone OTP)
```

**Total estimate:** ~7-8 ngày cho Phase 1+2, ~1.5 ngày Phase 3, ~2.5 ngày Phase 4

---

## Completion Status

| PR | Status | GitHub |
|----|--------|--------|
| PR-SEC-1 Email verification | ✅ DONE | #153 |
| PR-SEC-2 Rate limit | ✅ DONE | #150 |
| PR-ONBOARD-1 Welcome wizard | ✅ DONE | #155 |
| PR-ONBOARD-2 Dashboard | ✅ DONE | #152 |
| PR-LOCAL-1 Tenant URL | ✅ DONE | #151 |
| PR-SEC-3 Captcha | ✅ DONE | #157 |
| PR-SEC-4 Phone OTP | ⬜ Chờ duyệt | - |
