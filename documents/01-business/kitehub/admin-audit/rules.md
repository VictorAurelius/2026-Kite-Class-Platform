# Admin Audit — Business Rules

**Domain:** Admin audit log (GAP-640 — Wave 97 Bucket C 3-layer foundation)
**Source-of-truth controller:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/audit/`
**Last verified:** 2026-05-18 (Wave 97 Bucket C — GAP-640 admin-audit 3-layer docs META P1)

---

## Config keys

| Key | Default | Mô tả |
|-----|---------|-------|
| `kitehub.admin-audit.retention-years` | `7` | Thời gian lưu trữ audit log tối thiểu (năm) theo PDPL 2023 Art 11 |
| `kitehub.admin-audit.export-page-size` | `1000` | Số dòng tối đa mỗi trang khi compliance export |
| `kitehub.admin-audit.sensitive-actions` | `LOGIN,IMPERSONATE,DATA_EXPORT,INSTANCE_DELETE,TENANT_SUSPEND` | Danh sách action type được coi là nhạy cảm — bắt buộc ghi `before_state` + `after_state` |

---

## BR-ADMIN-AUDIT-001 — Bất biến audit log (Immutable Audit Mandate)

**Rule:** Mọi row trong `admin_audit_log` sau khi INSERT thì KHÔNG được UPDATE hoặc DELETE bởi bất kỳ user, admin, hay job tự động nào. Chỉ INSERT mới là hợp lệ. Retention tối thiểu 7 năm.

**Source:** PDPL 2023 Art 11 (nghĩa vụ chứng minh compliance), ISO27001 A.12.4 (audit log integrity), V54 migration (`admin_audit_log` bảng — Wave 92 Bucket A)

**Rationale:** Audit log có giá trị chứng minh pháp lý chỉ khi không thể bị sửa đổi. Nếu admin có thể xóa dòng log sau khi thực hiện hành động vi phạm, toàn bộ evidence trail vô nghĩa. PDPL Art 11 yêu cầu tổ chức "lưu giữ bằng chứng về việc thực hiện các nghĩa vụ bảo vệ dữ liệu cá nhân" — audit log là bằng chứng đó.

**Reviewer:** @nguyenvankiet (solo-dev Wave 97 Bucket C)

**Compliance check:** Kiểm tra 6 tháng/lần — verify không có `DELETE FROM admin_audit_log` hoặc `UPDATE admin_audit_log` trong codebase; verify PostgreSQL row-level security ngăn direct DB write ngoài service account `kitehub-admin`; verify V54 migration không có `ON DELETE CASCADE` từ bảng referenced.

**Review cadence:** Mỗi khi có thay đổi database schema liên quan đến `admin_audit_log`, hoặc khi có security audit, hoặc theo review PDPL định kỳ.

---

## BR-ADMIN-AUDIT-002 — Trường bắt buộc baseline (Required Fields — V36)

**Rule:** Mọi row INSERT vào `admin_audit_log` PHẢI có đủ 11 trường V36 baseline: `id`, `admin_user_id`, `action`, `target_entity_type`, `target_entity_id`, `request_ip`, `user_agent`, `payload_json`, `success`, `error_message` (nullable khi success=true), `created_at`. Không được INSERT row thiếu bất kỳ trường nào trong số này (ngoại trừ `error_message` nullable).

**Source:** V36 migration baseline (Wave 72a) — `kitehub/kitehub-admin/src/main/resources/db/migration/V36__create_admin_audit_log.sql`

**Rationale:** Mỗi trường baseline có vai trò cụ thể trong forensic investigation:
- `admin_user_id` — ai thực hiện hành động?
- `action` — hành động gì?
- `target_entity_type` + `target_entity_id` — tác động vào đối tượng nào?
- `request_ip` + `user_agent` — từ đâu? thiết bị gì?
- `payload_json` — chi tiết request như thế nào?
- `success` + `error_message` — kết quả ra sao?
- `created_at` — khi nào?

Thiếu bất kỳ trường nào làm suy yếu khả năng điều tra sự cố hoặc chứng minh compliance.

**Reviewer:** @nguyenvankiet (solo-dev Wave 97 Bucket C)

**Compliance check:** Code review mỗi PR chạm `AuditLogService` — verify tất cả 11 trường được set trước khi persist; Integration test verify không có `NOT NULL constraint violation` khi insert audit row cho các action type đã defined.

**Review cadence:** Mỗi khi thêm `action` type mới hoặc refactor `AuditLogService`; quarterly audit score review.

---

## BR-ADMIN-AUDIT-003 — JSONB state snapshot cho sensitive actions (V54 Enrichment)

**Rule:** Với các action type trong danh sách `kitehub.admin-audit.sensitive-actions`, PHẢI ghi thêm `before_state JSONB` (trạng thái entity TRƯỚC hành động) và `after_state JSONB` (trạng thái entity SAU hành động) vào audit row tương ứng. Với action type không trong danh sách sensitive, `before_state` và `after_state` ĐƯỢC PHÉP NULL.

**Source:** V54 migration (Wave 92 Bucket A) — 5 enrichment columns: `request_id`, `target_resource_type`, `target_resource_id`, `before_state JSONB`, `after_state JSONB` + composite index `idx_admin_audit_log_resource`

**Rationale:** JSONB state snapshot là yếu tố quyết định trong compliance audit và incident forensics. Ví dụ:
- Admin thay đổi subscription plan của tenant: `before_state={"plan":"FREE"}`, `after_state={"plan":"PAID_BASIC"}` → auditor biết chính xác điều gì đã thay đổi
- Admin xóa instance: `before_state={"status":"ACTIVE","tenant_id":...}` → có evidence về state trước khi xóa
Nếu chỉ có `action="INSTANCE_DELETE"` mà không có `before_state`, không thể chứng minh instance đó thuộc tenant nào vào thời điểm xóa.

**Reviewer:** @nguyenvankiet (solo-dev Wave 97 Bucket C)

**Compliance check:** Integration test verify mỗi sensitive action type đều có `before_state` + `after_state` non-null trong audit row; Kiểm tra format JSONB hợp lệ (không phải raw string); Verify composite index `idx_admin_audit_log_resource` tồn tại sau V54 migration.

**Review cadence:** Mỗi khi cập nhật `kitehub.admin-audit.sensitive-actions` config, hoặc thêm action type mới, hoặc quarterly audit score review.

---

## Related

- UC-ADMIN-AUDIT-001..005: `documents/01-business/kitehub/admin-audit/use-cases.md`
- API contract: `documents/01-business/kitehub/admin-audit/api-contract.md`
- V36 migration: `kitehub/kitehub-admin/src/main/resources/db/migration/V36__create_admin_audit_log.sql`
- V54 migration: `kitehub/kitehub-admin/src/main/resources/db/migration/V54__enrich_admin_audit_log.sql`
- Business logic correctness: `.claude/rules/business-logic-review.md`
