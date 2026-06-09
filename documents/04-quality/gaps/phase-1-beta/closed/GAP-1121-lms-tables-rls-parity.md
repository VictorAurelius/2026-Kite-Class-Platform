# GAP-1121: RLS-parity defense-in-depth cho 4 bảng LMS (course_modules / lessons / learning_resources / lesson_progress)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-09 (điều tra anomaly J — RLS sweep V78 chạy trước V79)
**Affects:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V79__entity_schema_sync.sql` (4 bảng LMS) + `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/lms/LmsRlsIsolationIT.java` (mới)

## Problem

Điều tra ban đầu (anomaly J) kết luận: 4 bảng LMS `course_modules`, `lessons`, `learning_resources`, `lesson_progress` (tạo ở V79) **thiếu RLS DB-level** — chỉ còn 1 lớp bảo vệ (Hibernate `@Filter tenantFilter` ở ORM) thay vì 2 lớp như chuẩn dự án V58 ("RLS FORCED": ORM `@Filter` + DB RLS). Lý do nghi ngờ: `V78__rls_sweep` chạy TRƯỚC V79 nên không phủ 4 bảng này.

**Verify-before-fix đã chỉnh lại tiền đề này — nó SAI:**

`V78__rls_sweep` thật sự không phủ 4 bảng (đúng), NHƯNG **V79 tự nó đã ship RLS** cho cả 4 bảng. Khối `DO $$` cuối V79 (dòng 577–613) lặp qua mảng `instance_id_tables` bao gồm chính xác `course_modules`, `lessons`, `learning_resources`, `lesson_progress` và áp dụng:

- `ALTER TABLE <t> ENABLE ROW LEVEL SECURITY`
- `ALTER TABLE <t> FORCE ROW LEVEL SECURITY`
- `CREATE POLICY tenant_isolation` theo **đúng pattern hardened V59** (admin-bypass qua `app.is_platform_admin` + NULL force-fail qua `NULLIF(current_setting('app.current_tenant_id', true), '')::uuid` ở cả `USING` và `WITH CHECK`).

Sweep toàn bộ migration xác nhận: chỉ V79 chạm 4 bảng này; **không có migration nào sau đó `DISABLE`/`NO FORCE`/`DROP POLICY` mà không tạo lại**. Vậy 2 lớp defense-in-depth ĐÃ đầy đủ ở production.

**Gap thực tế còn lại (không phải thiếu migration):** 4 bảng LMS này **không có integration test** chứng minh RLS isolation hoạt động. Test profile kc-core (`application-test.yml`) tắt Flyway + dùng `ddl-auto: create-drop`, nên RLS của V79 **không bao giờ chạy trong test** → backstop DB không có regression guard. Nếu một thay đổi tương lai (refactor V79, migration mới drop policy, đổi GUC) vô tình gỡ RLS, sẽ không test nào bắt được.

## Root Cause

Điều tra anomaly J chỉ đọc V78 (thấy 4 bảng không trong mảng sweep) rồi suy ra "thiếu RLS", mà bỏ sót khối RLS riêng ở cuối V79 (cùng migration tạo bảng). Tiền đề "thiếu migration" sai; tiền đề "thiếu regression test cho RLS 4 bảng" mới đúng.

## Fix (shipped session 2026-06-09)

**KHÔNG tạo migration V96** — sẽ là no-op trùng lặp (V79 đã ENABLE+FORCE+POLICY idempotent với `DROP POLICY IF EXISTS`), gây nhiễu lịch sử migration.

**Thay vào đó: thêm `LmsRlsIsolationIT`** (regression guard, đúng intent "khôi phục backstop DB defense-in-depth"). Test áp dụng pattern hardened V59 lập trình trong `@BeforeAll` (giống `RLSEnforcementIT` / `RLSHardeningIT` vì test profile tắt Flyway) + dùng role `NOSUPERUSER NOBYPASSRLS` (vì DB user Testcontainers là superuser, bypass RLS kể cả FORCE). 5 test:

- `course_modules` / `lessons` / `learning_resources` / `lesson_progress`: seed 1 row tenant A + 1 row tenant B, set `app.current_tenant_id = A` → query chỉ thấy row A (count = 1) + by-id lookup row B trả 0 (cross-tenant isolation).
- Tất cả 4 bảng: GUC unset (không tenant context) → 0 row (NULL force-fail / default-deny).

## Acceptance Criteria

- [x] Xác minh 4 bảng LMS đã có RLS DB-level (ENABLE + FORCE + `tenant_isolation` hardened) — confirmed V79:577–613.
- [x] Không có migration sau V79 gỡ RLS khỏi 4 bảng — confirmed (sweep toàn bộ migration).
- [x] Integration test verify cross-tenant isolation cho cả 4 bảng — `LmsRlsIsolationIT` 5 test.
- [x] Integration test verify NULL/default-deny — `allLmsTables_shouldRejectQueryWithoutTenantContext`.
- [x] `./mvnw -pl kiteclass-core test -Dtest=LmsRlsIsolationIT` PASS local — `Tests run: 5, Failures: 0, Errors: 0`.

## Verification

```
cd kiteclass/kiteclass-core && ./mvnw -o -q test -Dtest='LmsRlsIsolationIT'
→ Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 29.78 s
```

(Testcontainers Postgres 15.17. Surefire warning "kill self fork JVM" chỉ là timing shutdown do thread reconnect RabbitMQ sau `System.exit(0)`, không phải test fail — exit code 0.)

## Related

- Discovered in: phiên fix-agent Gap #2 (RLS-parity 4 bảng LMS) 2026-06-09
- Pattern nguồn: `V58__enable_rls_tenant_scoped_tables.sql` + `V59__rls_admin_bypass_and_null_force_fail.sql` (hardened)
- Test mẫu: `RLSEnforcementIT.java`, `RLSHardeningIT.java`
- Đã ship RLS: `V79__entity_schema_sync.sql` dòng 577–613
