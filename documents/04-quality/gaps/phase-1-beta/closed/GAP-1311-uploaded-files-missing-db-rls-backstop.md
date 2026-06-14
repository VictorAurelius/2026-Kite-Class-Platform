# GAP-1311: uploaded_files + storage_quota thiếu DB-level RLS — chỉ dựa Hibernate @Filter (thiếu defense-in-depth)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (security full audit post wave-p0-closeout-1 — AUDIT-2026-06-14-security-full, F-004)
**Affects:** `kiteclass-core` storage tables (`uploaded_files`, `storage_quota`)

## Problem

Các bảng tenant-scoped của kiteclass-core có 2 lớp cô lập: (1) Hibernate `@Filter("tenantFilter", condition="instance_id = :tenantId")` (BaseEntity L43-44, bật per-request bởi TenantFilterInterceptor) + (2) Postgres RLS policy ở DB (migration sweep V58/V59/V78/V81/V83/V84).

`uploaded_files` và `storage_quota` **KHÔNG có trong bất kỳ migration enable-RLS nào** (grep V58/V78/V84 = rỗng). `UploadedFile extends BaseEntity` nên CÓ lớp Hibernate `@Filter`, nhưng **thiếu lớp RLS DB backstop** mà các bảng tenant-scoped peer đều có.

**Rủi ro:** nếu lớp Hibernate filter không bật cho 1 code path (native `@Query`, path filter-exempt, hoặc quên `enableFilter`), thì KHÔNG còn lớp DB nào chặn → cross-tenant read/write có thể xảy ra. Đây là single-layer isolation, lệch pattern dual-layer của phần còn lại của hệ.

## Proposed Fix

Thêm migration enable RLS + `CREATE POLICY` cho `uploaded_files` + `storage_quota` theo đúng pattern V58/V78/V84 (force-fail khi tenant GUC NULL, admin-bypass nếu áp dụng). Verify policy bằng IT chạy trên schema Flyway (không phải ddl-auto — per memory `kiteclass-core IT ddl-auto masks migration drift`).

## Acceptance Criteria

- [x] Migration mới enable ROW LEVEL SECURITY + policy cho `uploaded_files` + `storage_quotas`.
- [x] Test: query 2 bảng này với tenant GUC = tenant khác → 0 row (DB-level), độc lập với Hibernate filter.
- [x] Tenant GUC NULL → force-fail (không leak), khớp V59 pattern.

## Resolution (2026-06-15, audit-fixB PR)

Thêm migration **`V99__uploaded_files_storage_quotas_rls.sql`** (V98 là max trước đó):
`ENABLE` + `FORCE ROW LEVEL SECURITY` + policy `tenant_isolation` theo đúng shape V59-hardened
(admin-bypass `app.is_platform_admin` + NULL force-fail `NULLIF(...,'')::uuid`) cho cả 2 bảng.
Migration dùng `DO $$` block idempotent (`DROP POLICY IF EXISTS` + defensive `information_schema`
guard mirror V58/V84). Cả 2 bảng được tạo trong V79 (`uploaded_files`, `storage_quotas` — tên
thật số nhiều, gap title viết tắt `storage_quota`) nên `ALTER TABLE` an toàn trên schema Flyway.

**Test:** `UploadedFilesRlsIsolationTest` (CI-bound `*Test`, mirror `LmsRlsIsolationIT`) 3/3 PASS.
Vì test profile tắt Flyway + `ddl-auto: create-drop`, test áp cùng policy SQL programmatically +
provision role `NOSUPERUSER NOBYPASSRLS` để RLS thực sự kích hoạt (DB Testcontainers là superuser
bypass RLS). Cover: cross-tenant isolation cho từng bảng (tenant A không thấy row tenant B kể cả
lookup theo id) + NULL force-fail (GUC unset → 0 row).

## Related

- Discovered in: AUDIT-2026-06-14-security-full F-004 (EVIDENCE-2026-06-14-INFRA-006). Reserved gap-ID per `multi-session-concurrency-coordination.md`.
- GAP-825 (tenant-isolation hardening — OPEN) — cùng chủ đề defense-in-depth.
- Migration precedent: V58 (enable_rls_tenant_scoped_tables), V59 (null_force_fail), V78 (rls_sweep), V84 (denormalize_instance_id_rls).
