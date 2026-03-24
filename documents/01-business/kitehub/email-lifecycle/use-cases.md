# Email Lifecycle — Use Cases

### UC-EML-01: Gửi email theo trigger sự kiện
- **Actor:** System (event-driven)
- **Precondition:** Instance tồn tại, email address hợp lệ
- **Steps:**
  1. System: nhận trigger (instance activated / sub created)
  2. System: build EmailRequest (to, subject, templateName, variables)
  3. System: POST /api/platform/emails/send tới kitehub-email service
  4. System: ghi EmailSentLog (nếu có idempotency guard)
- **Postcondition:** Email đã được gửi và logged
- **Errors:**
  - Network error → log + continue (không throw lên user)

### UC-EML-02: Gửi email theo scheduler
- **Actor:** System (scheduled jobs)
- **Precondition:** Conditions met (trial day, sub expiry, etc.)
- **Steps:**
  1. System: scheduler chạy theo cron
  2. System: query instances cần gửi email
  3. System: check `alreadySentToday(instanceId, emailType, recipient)` (EML-01)
  4. System: nếu đã gửi hôm nay → skip
  5. System: gửi email + ghi EmailSentLog
- **Postcondition:** Email gửi đúng timing, không duplicate
- **FE Behavior:** N/A (background job)

### UC-EML-03: Email xác nhận tài khoản
- **Actor:** Visitor mới đăng ký (pending instance flow)
- **Steps:**
  1. System: gửi email-verification.html với token
  2. User: click link xác nhận
  3. System: POST /api/auth/verify-email?token=xxx
  4. System: kích hoạt instance (PENDING → TRIAL)
  5. System: gửi welcome email
- **Postcondition:** Instance TRIAL, user nhận welcome email
- **Errors:**
  - 400: token expired/invalid

### UC-EML-04: Resend verification email
- **Actor:** User (chưa verify)
- **Steps:**
  1. FE: hiển thị "Resend email" button
  2. User: click
  3. System: POST /api/auth/resend-verification với email
  4. System: gửi lại email-verification.html với token mới
- **Errors:**
  - 404: email not found
  - 400: already verified
