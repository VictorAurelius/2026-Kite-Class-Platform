# Admin Audit — Use Cases

**Domain:** Admin audit log (GAP-640 — Wave 97 Bucket C 3-layer foundation)
**Source-of-truth controller:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/`
**Last verified:** 2026-05-18 (Wave 97 Bucket C — GAP-640 admin-audit 3-layer docs META P1)

---

## UC-ADMIN-AUDIT-001 — Ghi nhận audit log đăng nhập admin

**Use case:** Admin đăng nhập vào hệ thống — hệ thống tự động ghi audit log

**Actor:** `PLATFORM_ADMIN` (trigger thông qua `AuthService.adminLogin`)

**Trigger:** Admin gửi request đăng nhập thành công hoặc thất bại tới `/api/v1/auth/login`

**Business rule:** BR-ADMIN-AUDIT-001, BR-ADMIN-AUDIT-002

**Happy path:**
1. Admin gửi `POST /api/v1/auth/login` với credentials hợp lệ
2. `AuthService` xác thực credentials, tạo JWT
3. `AuditLogService.record()` được gọi trong cùng transaction
4. Row audit được INSERT với `action=LOGIN`, `success=true`, `admin_user_id`, `request_ip`, `user_agent`, `created_at`
5. JWT trả về cho admin

**Error branches:**
- Đăng nhập thất bại (sai password): vẫn ghi audit row với `success=false`, `error_message="INVALID_CREDENTIALS"`, `admin_user_id=null` (vì chưa xác định được user)
- Rate limit exceeded: ghi audit row với `success=false`, `error_message="RATE_LIMITED"`

**FE behavior:** FE không hiển thị audit log khi đăng nhập; log chỉ visible cho compliance reader ở `/admin/audit-logs`

**Acceptance criteria:**
- [ ] Mọi lần đăng nhập (thành công hay thất bại) đều tạo audit row
- [ ] `request_ip` lấy từ `X-Forwarded-For` header (qua gateway) hoặc `REMOTE_ADDR`
- [ ] `user_agent` lấy từ `User-Agent` header
- [ ] Audit row được INSERT trong cùng transaction với JWT issuance (atomic)

**Code reference:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/AuditLogService.java`

---

## UC-ADMIN-AUDIT-002 — Ghi nhận audit log thao tác impersonation

**Use case:** Admin impersonate một tenant user để debug — hệ thống ghi audit log đầy đủ

**Actor:** `PLATFORM_ADMIN` (qua `/api/v1/admin/impersonate/{tenantUserId}`)

**Trigger:** Admin gửi request impersonation tới endpoint impersonate

**Business rule:** BR-ADMIN-AUDIT-001, BR-ADMIN-AUDIT-002, BR-ADMIN-AUDIT-003 (IMPERSONATE là sensitive action)

**Happy path:**
1. Admin gửi `POST /api/v1/admin/impersonate/{tenantUserId}` với JWT hợp lệ
2. `ImpersonationService` xác thực quyền, chuẩn bị impersonation token
3. `AuditLogService.recordSensitive()` được gọi — lấy `before_state` (tenant user state hiện tại)
4. Impersonation token được tạo
5. `AuditLogService.finalizeSensitive()` — ghi `after_state` (impersonation session details)
6. Audit row INSERT với `action=IMPERSONATE`, `before_state={tenantUser}`, `after_state={session}`

**Error branches:**
- TenantUser không tồn tại: ghi audit row với `success=false`, `error_message="TENANT_USER_NOT_FOUND"`
- Admin không đủ quyền: ghi `success=false`, `error_message="ACCESS_DENIED"`

**FE behavior:** Admin xem log impersonation trong `/admin/audit-logs?action=IMPERSONATE`; có filter theo `admin_user_id` và date range

**Acceptance criteria:**
- [ ] Mọi attempt impersonation (thành công hay thất bại) đều có audit row
- [ ] `before_state` chứa thông tin tenant user (không chứa password/secret)
- [ ] `after_state` chứa impersonation session ID và expiry
- [ ] Action type `IMPERSONATE` nằm trong `kitehub.admin-audit.sensitive-actions`

**Code reference:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/impersonation/ImpersonationService.java`

---

## UC-ADMIN-AUDIT-003 — Ghi nhận audit log thao tác vòng đời instance

**Use case:** Admin thực hiện thay đổi lifecycle instance (approve, suspend, delete) — hệ thống ghi audit log với before/after state

**Actor:** `PLATFORM_ADMIN` (qua `/api/v1/admin/instances/{id}/...`)

**Trigger:** Admin gửi request thay đổi trạng thái instance

**Business rule:** BR-ADMIN-AUDIT-001, BR-ADMIN-AUDIT-002, BR-ADMIN-AUDIT-003 (INSTANCE_DELETE là sensitive action)

**Happy path (ví dụ INSTANCE_DELETE):**
1. Admin gửi `DELETE /api/v1/admin/instances/{id}` với confirmation token
2. `InstanceService` đọc instance hiện tại — lấy `before_state`
3. `AuditLogService.recordSensitive()` capture `before_state={status, tenant_id, plan, created_at}`
4. Instance được xóa (soft delete hoặc hard delete tùy config)
5. `AuditLogService.finalizeSensitive()` — ghi `after_state={status:"DELETED", deleted_at}`
6. Audit row INSERT với `action=INSTANCE_DELETE`, `target_entity_type=INSTANCE`, `target_entity_id={id}`

**Error branches:**
- Instance không tồn tại: ghi `success=false`, `error_message="INSTANCE_NOT_FOUND"`
- Instance đang trong trạng thái không cho phép xóa: ghi `success=false`, `error_message="INVALID_STATE_TRANSITION"`

**FE behavior:** Trang `/admin/instances/{id}` hiển thị lịch sử thay đổi từ audit log; filter theo `target_entity_id`

**Acceptance criteria:**
- [ ] Mọi thay đổi lifecycle instance đều có audit row
- [ ] `before_state` và `after_state` non-null khi action là `INSTANCE_DELETE` hoặc `INSTANCE_SUSPEND`
- [ ] `target_resource_type=INSTANCE`, `target_resource_id={id}` (V54 enrichment columns)
- [ ] Composite index `idx_admin_audit_log_resource` phục vụ query by resource type + id

**Code reference:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/instance/InstanceAdminService.java`

---

## UC-ADMIN-AUDIT-004 — Ghi nhận audit log truy cập dữ liệu nhạy cảm

**Use case:** Admin truy cập hoặc xuất dữ liệu nhạy cảm (tenant PII, payment data, raw logs) — hệ thống ghi audit trail đầy đủ

**Actor:** `PLATFORM_ADMIN` (qua các endpoint `/api/v1/admin/data-export/*` hoặc raw data endpoints)

**Trigger:** Admin request xem hoặc xuất dữ liệu được phân loại là nhạy cảm

**Business rule:** BR-ADMIN-AUDIT-001, BR-ADMIN-AUDIT-002, BR-ADMIN-AUDIT-003 (DATA_EXPORT là sensitive action)

**Happy path:**
1. Admin gửi request đến endpoint dữ liệu nhạy cảm (vd `GET /api/v1/admin/tenants/{id}/pii`)
2. `DataAccessAuditAspect` (Spring AOP aspect) intercept request trước khi controller method chạy
3. `before_state` = metadata về data scope (tenant_id, data_type, row_count estimate)
4. Controller thực thi — data được serve
5. `after_state` = thực tế data đã serve (không chứa raw PII — chỉ metadata: row_count, fields_included, format)
6. Audit row INSERT với `action=DATA_EXPORT`, `success=true`

**Error branches:**
- Unauthorized access: ghi `success=false`, `error_message="FORBIDDEN"` — ngay cả failed attempt cũng được log

**FE behavior:** Audit log truy cập dữ liệu nhạy cảm được highlight với màu cảnh báo trong `/admin/audit-logs`; compliance reader filter `action=DATA_EXPORT`

**Acceptance criteria:**
- [ ] Mọi access vào endpoint dữ liệu nhạy cảm đều có audit row (kể cả failed attempt)
- [ ] `before_state` và `after_state` không chứa raw PII — chỉ metadata
- [ ] `DataAccessAuditAspect` apply tự động — dev không cần manual gọi `AuditLogService` cho mỗi endpoint
- [ ] Audit row tạo ra trước khi data được serve (pre-access logging)

**Code reference:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/DataAccessAuditAspect.java`

---

## UC-ADMIN-AUDIT-005 — Compliance reader xuất báo cáo audit

**Use case:** Compliance reader hoặc PLATFORM_ADMIN xuất báo cáo audit log theo khoảng thời gian và filter điều kiện, dùng cho mục đích kiểm toán nội bộ hoặc báo cáo PDPL

**Actor:** `PLATFORM_ADMIN` (qua `/api/v1/admin/audit-logs/export`)

**Trigger:** Admin hoặc compliance reader request xuất audit log

**Business rule:** BR-ADMIN-AUDIT-001 (dữ liệu xuất không được phép bị thay đổi), BR-ADMIN-AUDIT-002

**Happy path:**
1. Admin gửi `GET /api/v1/admin/audit-logs/export` với params: `from=YYYY-MM-DD`, `to=YYYY-MM-DD`, `action` (optional), `admin_user_id` (optional)
2. `AuditLogExportService` query `admin_audit_log` với page size = `kitehub.admin-audit.export-page-size`
3. Kết quả trả về dưới dạng JSON array (hoặc CSV nếu `?format=csv`)
4. Hành động xuất chính nó cũng được ghi vào audit log với `action=DATA_EXPORT`, `before_state={query_params}`

**Error branches:**
- Date range quá lớn (> 1 năm): trả lỗi `400 AUDIT_EXPORT_RANGE_TOO_LARGE`
- Không đủ quyền: `403 FORBIDDEN`

**FE behavior:** Trang `/admin/audit-logs` có nút "Xuất CSV" với date picker; tiến trình xuất hiển thị spinner

**Acceptance criteria:**
- [ ] Export endpoint chỉ cho phép `PLATFORM_ADMIN`
- [ ] Hành động export được ghi vào audit log (tránh chicken-and-egg — log export action TRƯỚC khi query data export)
- [ ] Date range tối đa 1 năm mỗi request (nhiều hơn → phân trang theo range)
- [ ] CSV export đúng format UTF-8 BOM cho Excel compatibility (theo `test-artifact-format-standard.md`)
- [ ] Không thể sửa đổi dữ liệu audit thông qua export endpoint

**Code reference:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/AuditLogExportService.java`

---

## Related

- BR-ADMIN-AUDIT-001..003: `documents/01-business/kitehub/admin-audit/rules.md`
- API contract: `documents/01-business/kitehub/admin-audit/api-contract.md`
- V36 migration: `kitehub/kitehub-admin/src/main/resources/db/migration/V36__create_admin_audit_log.sql`
- V54 migration: `kitehub/kitehub-admin/src/main/resources/db/migration/V54__enrich_admin_audit_log.sql`
