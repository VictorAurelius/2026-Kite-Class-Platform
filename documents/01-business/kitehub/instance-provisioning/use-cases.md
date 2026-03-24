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
