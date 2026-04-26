# Instance Provisioning — Use Cases

### UC-INS-01: Tạo Instance Trial (Admin)
- **Actor:** Admin
- **Precondition:** Admin authenticated
- **Steps:**
  1. Admin: POST /api/platform/instances với CreateInstanceRequest
  2. System: validate subdomain (INS-06, INS-07)
  3. System: tạo instance (tier=FREE, status=TRIAL), start trial
  4. System: provision DB async
- **Postcondition:** Instance TRIAL
- **Errors:**
  - 409: subdomain đã tồn tại
  - 400: subdomain reserved

### UC-INS-02: Self-Service Registration
- **Actor:** Visitor
- **Precondition:** N/A
- **Steps:**
  1. FE: form đăng ký (orgName, subdomain, email, password)
  2. System: validate subdomain uniqueness + reserved check (INS-06, INS-07)
  3. System: validate email uniqueness
  4. System: tạo owner UUID, tạo instance (FREE/TRIAL)
  5. System: set placeholder DB credentials ("pending")
  6. System: start trial (14 ngày)
  7. System: provision DB async (continue on failure)
  8. System: generate JWT tokens
  9. System: gửi welcome email (INS-16)
- **Postcondition:** Instance TRIAL, tokens returned
- **Errors:**
  - 409: subdomain/email đã tồn tại
  - 400: subdomain reserved (INS-05)

### UC-INS-03: Pending Instance + Email Verification Flow
- **Actor:** System + User
- **Steps:**
  1. System: createPendingInstance() → status=PENDING, chưa provision DB
  2. System: gửi email verification
  3. User: click link verify
  4. System: activatePendingInstance() → start trial, provision DB, gửi welcome email
- **Postcondition:** Instance TRIAL

### UC-INS-04: Xem danh sách Instances
- **Actor:** Admin
- **Steps:**
  1. Admin: GET /api/platform/instances
  2. System: trả về tất cả instances

### UC-INS-05: Cập nhật Instance
- **Actor:** Owner/Admin
- **Steps:**
  1. User: PUT/PATCH /api/platform/instances/{id}
  2. System: update orgName hoặc các fields được phép

### UC-INS-06: Xóa Instance (Soft Delete)
- **Actor:** Admin
- **Steps:**
  1. Admin: DELETE /api/platform/instances/{id}
  2. System: set deleted=true, status=DELETED, đóng DataSource pool (INS-15)
  3. System: nếu lifecycle enabled → DROP DATABASE, DROP USER
- **Errors:**
  - 404: instance not found

---

## v2 Use Cases (drift fix — GAP-229 Phase 3)

### UC-INS-07: Xem Trial Status (FE polling)
- **Actor:** Owner (FE banner / dashboard)
- **Steps:**
  1. FE: GET /api/platform/instances/{id}/trial-status
  2. System: trả về `TrialStatusResponse { trialActive, expiresAt, daysRemaining, status }`
- **FE Behavior:** Hiển thị banner cảnh báo khi `daysRemaining ≤ 3` (per INS-03)

### UC-INS-08: Admin Extend Trial
- **Actor:** Admin
- **Precondition:** Tenant đã yêu cầu hỗ trợ kéo dài; instance đang ở TRIAL
- **Steps:**
  1. Admin: POST /api/platform/instances/{id}/extend-trial?days=N
  2. System: gia hạn `trialExpiresAt` thêm N ngày
  3. Audit log: ghi nhận extension + admin user + reason (nếu có)
- **Postcondition:** Trial expiration đẩy lùi N ngày
- **Errors:** 404 instance not found, 400 invalid days

### UC-INS-09: Hard Purge (Admin, Destructive)
- **Actor:** Admin
- **Precondition:** Instance ở status `DELETED` (UC-INS-06 đã chạy); ít nhất 1 backup `COMPLETED` tồn tại
- **Steps:**
  1. Admin: DELETE /api/platform/instances/{id}/purge
  2. System (`InstancePurgeService.adminPurge`):
     - Verify backup tồn tại — không có → trả `SKIPPED_NO_BACKUP` + log warning + dừng (KHÔNG destructive)
     - Drop PostgreSQL database
     - Delete S3 backup files
     - Mark `BackupRecord` status DELETED
     - Delete `EmailSentLog` rows cho instance
     - Outbox-first publish event `instance.purge.requested` tới `subscription_outbox` (per GAP-222c Exception A) + best-effort fanout RabbitMQ → consumers (kitehub-branding, kiteclass-core) cleanup tài nguyên cross-service
     - Set instance status `PURGED`
  3. System trả `PurgeResult` với chi tiết từng bước
- **Postcondition:** Tất cả tài nguyên của instance bị xóa vĩnh viễn; cross-service cleanup events đã enqueued
- **Errors:**
  - 404: instance not found
  - Trả `FAILED` nếu instance không ở DELETED status
  - Trả `SKIPPED_NO_BACKUP` nếu chưa có verified backup (safety check)
- **Lưu ý ops:** Hard purge KHÔNG reversible — backup verification là safety guard cuối cùng

### UC-INS-03 update: Email Verification path
- **Endpoint thay đổi:** Trước doc ghi `POST /api/platform/instances/{id}/activate`; thực tế đã move sang `POST /api/platform/auth/verify-email?token=...` (AuthController consolidated auth domain).
- **Logic:** `AuthController.verifyEmail()` → `AuthService.verifyEmail(token)` → bên trong gọi tới instance lifecycle để start trial + provision DB.
- **Action:** Không có thay đổi behavior; chỉ là path correction.
