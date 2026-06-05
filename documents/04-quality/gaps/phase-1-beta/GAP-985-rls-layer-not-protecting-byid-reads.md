# GAP-985: RLS defense-in-depth không chặn cross-tenant by-id reads (V58 FORCE-RLS không reach read path)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (multi-tenant isolation — defense-in-depth)
**Found:** 2026-06-05 (Wave security-1 G2 live re-walk — discovery)
**Affects:** RLS policies `V58__enable_rls_tenant_scoped_tables.sql` trên `classes`/`courses`/`teachers`/`class_sessions` trong `kiteclass_shared`

## Problem

Wave security-1 fix GAP-983 đóng leak ở lớp ORM (`@Filter`). Nhưng phát hiện: production có FORCE-RLS (V58) trên các bảng tenant-scoped, GUC `app.current_tenant_id` set bởi `TenantAwareDataSourceInterceptor` — vậy mà by-id leak vẫn xảy ra HTTP 200 ở lớp DB (RLS đáng lẽ là lớp phòng thủ thứ 2 độc lập với @Filter). Tức RLS policy/GUC KHÔNG hiệu lực trên read path này. Cần điều tra: (a) RLS có thực sự enabled+forced trên các bảng trong `kiteclass_shared` không (GAP-983 §Problem ghi `relrowsecurity=f`), (b) GUC `set_config` có reach session của @Transactional read không, (c) policy USING clause đúng chưa. Tie với Bucket E (RLS FORCE defense-in-depth, defer Phase 1.5) của wave plan.

## Proposed Fix

Verify RLS state thực tế trên `kiteclass_shared` (`SELECT relname,relrowsecurity,relforcerowsecurity FROM pg_class`), reconcile migration vs runtime; nếu RLS off → bật + force + verify policy reads GUC; đảm bảo defense-in-depth độc lập với @Filter.

## Acceptance Criteria
- [ ] `pg_class.relforcerowsecurity = true` cho classes/courses/teachers/class_sessions trong kiteclass_shared
- [ ] Với @Filter tắt (mô phỏng), cross-tenant by-id vẫn 404 nhờ RLS (defense-in-depth proven)
- [ ] Flyway-running Testcontainers IT verify RLS layer (xem [[GAP-987]])

## Related
- Discovered in: Wave security-1 G2 re-walk 2026-06-05
- Parent: [[GAP-983]] (ORM-layer fix shipped); RLS-layer = lớp thứ 2 còn hở
