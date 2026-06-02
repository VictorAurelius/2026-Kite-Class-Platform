---
title: Database Schema Reference — KiteHub + KiteClass (per-table, per-field, dev-control-grade)
audience: mixed
status: active
created: 2026-06-02
last-reviewed: 2026-06-02
scope: Reference đầy đủ cấp bảng + cấp trường cho 2 database của dự án (KiteHub control-plane + KiteClass multi-tenant domain). Mục tiêu — dev kiểm soát hoàn toàn database. Bổ sung (KHÔNG trùng) database-architecture-map.md (bản đồ tổng quan high-level).
related:
  - documents/02-architecture/database-architecture-map.md
  - documents/02-architecture/multi-tenant-architecture.md
  - documents/02-architecture/adr/ADR-001-k12-data-model.md
---

# Database Schema Reference — KiteHub + KiteClass

**TL;DR:** Bộ tài liệu reference **cấp bảng + cấp trường** cho toàn bộ schema 2 database. Mỗi cluster file liệt kê **đầy đủ cột** (tên / kiểu / nullable / default / khóa / index) + **ý nghĩa từng trường** + **giải thích quan hệ** + ERD Mermaid cho cluster. Mục tiêu: một dev mới đọc xong **kiểm soát hoàn toàn database** — biết mỗi bảng lưu gì, mỗi cột nghĩa gì, bảng nối bảng ra sao.

Khác với [`database-architecture-map.md`](../database-architecture-map.md) (bản đồ **tổng quan**: catalog tên bảng + RLS coverage + FK graph sample 25 + per-service mapping + data-flow). Tài liệu này là **reference chi tiết exhaustive** mà map cố tình defer (map §7 → GAP-677). Nguồn chân lý: Flyway `CREATE TABLE` migrations + JPA `@Entity`.

**Audience:** Backend dev (tra cứu cột bảng X kiểu gì, FK đi đâu), DBA (review schema), người mới onboard (học database), thesis author (Chapter 2 nguồn dữ liệu).

---

## Hai database

Dự án có **đúng 2 database vật lý** (cùng 1 instance PostgreSQL `kite-postgres` nhưng 2 schema/DB logic tách biệt):

| Database | Service sở hữu migration | Vai trò | Số bảng | Multi-tenant |
|---|---|---|:---:|---|
| **`kitehub`** | `kitehub-subscription` (Flyway) | Control-plane: lifecycle tenant, subscription, billing, branding job, email, auth, admin, compliance | **33** | Một phần (RLS non-forced; nhiều bảng global/control-plane) |
| **`kiteclass`** | `kiteclass-core` (Flyway) | Multi-tenant domain: nghiệp vụ giáo dục per-tenant (học sinh, lớp, điểm danh, điểm, tài chính) | **~65** | Toàn bộ tenant-scoped (`instance_id` + RLS FORCED per V58) |

> Các service `kitehub-platform` / `kitehub-branding` / `kitehub-admin` **chia sẻ database `kitehub`** (entity của chúng persist vào schema do `kitehub-subscription` quản lý migration). `kitehub-email` không có bảng riêng (chỉ ghi `email_logs`/`email_sent_log` trong `kitehub`). `kitehub-gateway` + `kitehub-platform` (library) **không chạm DB**.

---

## Quy ước chung (đọc trước khi vào chi tiết)

| Quy ước | Ý nghĩa |
|---|---|
| **Primary key** | Hầu hết `UUID` (default `gen_random_uuid()`); một số bảng kh-sub dùng `BIGSERIAL`. Mỗi cluster file ghi rõ. |
| **`instance_id UUID`** | Cột tenant chính (KiteClass). FK ngầm tới `instances` (DB kitehub) — **không có FK vật lý cross-DB**, chỉ logic. RLS policy filter theo cột này. |
| **`tenant_id UUID`** | Alias semantic của `instance_id` (3 bảng kh-sub dùng tên này — drift lịch sử). |
| **RLS** | Row-Level Security. KiteClass: `ENABLE + FORCE` per V58 (NULL force-fail). KiteHub: `ENABLE` non-forced (control-plane không propagate `TenantContext`). Chi tiết: [`multi-tenant-architecture.md`](../multi-tenant-architecture.md). |
| **Soft delete** | Nhiều bảng có `deleted BOOLEAN DEFAULT false` (+ `deleted_at`). Query nghiệp vụ filter `deleted = false`. |
| **Audit cột** | `created_at` / `updated_at` `TIMESTAMP` + `created_by` / `updated_by` (UUID user). `version BIGINT` cho optimistic locking (JPA `@Version`). |
| **JSONB** | Cột structured (`payload_json`, `before_state`, `metadata`...) dùng `jsonb`. Cần Testcontainers IT (per `postgres-specific-type-testcontainers.md`). |
| **Outbox** | Bảng `*_outbox` / `outbox_events` = transactional outbox pattern (per `design-patterns.md` §3.5). |
| **Ngôn ngữ** | Narrative tiếng Việt; identifier (tên bảng/cột/enum/SQL) giữ English (per `dev-readable-doc-language.md`). |

---

## Bản đồ cluster (index điều hướng)

Schema chia thành **12 cluster** theo domain để ERD render được + dễ đọc (1 ERD 98 bảng sẽ overflow). Mỗi file = full per-table/per-field cho các bảng trong cluster.

### Database `kitehub` — [`kitehub/`](kitehub/README.md) (33 bảng, 4 cluster)

| File | Cluster | Bảng |
|---|---|---|
| [`01-auth-user-instance.md`](kitehub/01-auth-user-instance.md) | Auth / User / Instance | `users`, `instances`, `recovery_codes`, `oauth_attempts`, `login_audit_log`, `onboarding_progress`, `beta_access_request` (+ email_verification / custom_domain inline) |
| [`02-subscription-billing.md`](kitehub/02-subscription-billing.md) | Subscription / Billing | `subscriptions`, `payments`, `system_config` |
| [`03-branding.md`](kitehub/03-branding.md) | Branding / AI job / Outbox | `branding_jobs`, `branding_templates`, `branding_instance_state`, `branding_lifecycle_events`, `branding_regenerate_usage`, `branding_outbox`, `migration_outbox`, `migration_idempotency_key`, `backup_records`, `ai_usage_log` |
| [`04-email-compliance-admin.md`](kitehub/04-email-compliance-admin.md) | Email / Compliance / Admin / Staff | `email_logs`, `email_sent_log`, `notification_preferences`, `consent_record`, `consent_record_immutable`, `dsar_ticket`, `feedback_submissions`, `staff_invitations`, `staff_invitation_audit_log`, `admin_audit_log`, `admin_audit_logs`, `impersonation_audit_log`, `idempotency_keys` |

### Database `kiteclass` — [`kiteclass/`](kiteclass/README.md) (~65 bảng, 8 cluster)

| File | Cluster | Bảng |
|---|---|---|
| [`01-academic-structure.md`](kiteclass/01-academic-structure.md) | Cấu trúc học vụ | `academic_years`, `semesters`, `courses`, `curricula`, `classes`, `class_schedules`, `class_sessions`, `class_schedule_slots`, `course_prerequisites`, `subject_sections`, `homeroom_classes`, `holidays` |
| [`02-people-enrollment.md`](kiteclass/02-people-enrollment.md) | Con người / Ghi danh | `students`, `teachers`, `parents`, `parent_student_links`, `parent_invitations`, `enrollments`, `teacher_courses`, `student_bulk_import_jobs` |
| [`03-attendance-grading.md`](kiteclass/03-attendance-grading.md) | Điểm danh / Điểm số | `attendance`, `attendance_period`, `grades`, `subject_grades`, `grading_scales`, `assignments`, `submissions` |
| [`04-finance.md`](kiteclass/04-finance.md) | Tài chính / Lương | `invoices`, `invoice_items`, `payments`, `payment_records`, `payment_idempotency_keys`, `payroll_configs`, `payroll_periods` |
| [`05-rbac.md`](kiteclass/05-rbac.md) | Phân quyền (RBAC) | `roles`, `permissions`, `role_permissions`, `user_roles`, `vettings` |
| [`06-gamification.md`](kiteclass/06-gamification.md) | Gamification | `badges`, `student_badges`, `point_rules`, `student_points`, `rewards`, `reward_redemptions` |
| [`07-compliance-audit.md`](kiteclass/07-compliance-audit.md) | Compliance / Audit / Moderation | `audit_log`, `admin_audit_logs`, `child_protection_audit_log`, `parent_read_audit_log`, `parent_complaint_queue`, `incidents`, `moderation_queue`, `deletion_requests`, `dmca_takedown_requests`, `quality_reports`, `outbox_events`, `zalo_oa_notification_outbox`, `idempotency_keys` |
| [`08-branding-marketing.md`](kiteclass/08-branding-marketing.md) | Branding / Marketing / Infra | `branding`, `branding_resources`, `branding_versions`, `rebrand_approvals`, `frontend_instances`, `landing_pages` |

---

## Trạng thái build

Tài liệu này được build qua **wave-local-doable-13** (fan-out agent per cluster, worktree-isolated, Opus). Mỗi cluster file ship khi agent hoàn tất + verify. README này là index ổn định; cluster file điền dần.

**Log:**
- **2026-06-02:** Scaffold khởi tạo (folder + README + cluster map 12 cluster). Nguồn: Flyway migrations (57 kh-sub + 77 kc-core) + JPA entities. Fan-out agent điền cluster detail. Per `audit-to-gap-pipeline.md` §2.5 state-check: bổ sung database-architecture-map.md (high-level), không trùng.
