# admin-audit — Kiểm toán hành động quản trị (Admin Audit Log)

**Last updated:** 2026-05-18
**Domain:** Immutable audit trail cho mọi hành động admin trên KiteHub platform
**Source-of-truth controller:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/`

---

## Mục đích

Domain `admin-audit` quản lý việc ghi nhận, lưu trữ và truy xuất log kiểm toán cho mọi hành động quản trị trên KiteHub platform. Mỗi hành động của `PLATFORM_ADMIN` — từ phê duyệt beta request, thay đổi cấu hình instance, đến truy cập dữ liệu nhạy cảm — đều phải được ghi nhận vào `admin_audit_log` theo chuẩn bất biến (immutable).

Tuân thủ:
- **PDPL 2023 Art 11** — yêu cầu evidence trail cho mọi xử lý dữ liệu cá nhân; giữ tối thiểu 7 năm
- **ISO27001 A.12.4** — audit logging và monitoring chuẩn quốc tế

---

## Cấu trúc thư mục

| File | Mục đích |
|------|----------|
| `README.md` | Tổng quan domain (file này) |
| `rules.md` | Business rules (BR-ADMIN-AUDIT-001..003) — constraints + config keys |
| `use-cases.md` | Use cases (UC-ADMIN-AUDIT-001..005) — actor, steps, errors, FE behavior |
| `api-contract.md` | API contract — write path conventions + read endpoints |

---

## Phạm vi

- **Thuộc domain này:** Ghi nhận audit log (write), truy vấn audit log (read), xuất báo cáo audit (compliance export)
- **Không thuộc domain này:** Auth logic (xem `auth/`), business actions được audit (xem domain gốc vd `beta-access/`, `instance-provisioning/`), log application (xem `logs-format-standard.md`)

---

## Database schema

**V36 migration (baseline — Wave 72a):** bảng `admin_audit_log` cột cơ bản:
- `id`, `admin_user_id`, `action`, `target_entity_type`, `target_entity_id`
- `request_ip`, `user_agent`, `payload_json`, `success`, `error_message`, `created_at`

**V54 migration (enrichment — Wave 92 Bucket A):** 5 cột bổ sung:
- `request_id`, `target_resource_type`, `target_resource_id`
- `before_state JSONB`, `after_state JSONB`
- Composite index `idx_admin_audit_log_resource`

---

## Archive policy

Log kiểm toán giữ lại tối thiểu **7 năm** theo PDPL 2023 Art 11. Không được xóa hoặc chỉnh sửa sau khi ghi. Tài liệu domain này cập nhật khi có thay đổi business rule hoặc schema migration liên quan.
