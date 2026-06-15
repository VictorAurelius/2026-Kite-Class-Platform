# GAP-1393: OpenApiSpecExportTest (kiteclass-core) intermittent "Failed to load ApplicationContext" trong CI full-reactor

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Test-infra
**Found:** 2026-06-14 (audit-fix campaign 2026-06-14 — red-flagged #2416/#2421/#2422)
**Affects:** `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/openapi/OpenApiSpecExportTest.java`

## Problem

`OpenApiSpecExportTest.exportSpec` (kiteclass-core) **chập chờn FAIL trong CI full-reactor** với `Failed to load ApplicationContext`, thời gian **0.003s** — dấu hiệu kinh điển của **cached context-load failure**: không phải test này tự load context fail, mà nó thừa kế một context đã fail từ test `@SpringBootTest` sibling (Spring TestContext cache trả lại exception đã cache cho cùng context key, hoặc context bị evict + reload fail dưới áp lực tài nguyên CI).

Test này dùng:
```java
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "springdoc.api-docs.path=/api-docs")
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
```
Thuộc tính `properties=` riêng → **context-cache key KHÁC** với 67 `@SpringBootTest` sibling còn lại trong kiteclass-core (tổng 68 class `@SpringBootTest`). Dưới áp lực tài nguyên CI + thứ tự nạp cache, một sibling-context load fail có thể cascade / context bị evict rồi reload fail.

Đặc điểm: **PASS ổn định ở local full-suite + chạy isolation**, chỉ FAIL trong CI full-reactor. Đây thuộc lớp "preexisting flaky kiteclass-core" mà `release-fix-retry-budget.md` đã ghi nhận.

## Bằng chứng — red-flag 3 PR campaign 2026-06-14

- PR #2416 (storage IDOR authz), #2421 (devops rollback + CW alarms), #2422 (perf bulkhead + pagination) — **cả 3 merge qua `ADMIN_MERGE_OVERRIDE`** sau khi local-verify clean, vì CI full-reactor đỏ ở chính `OpenApiSpecExportTest`. Đây là tax bypass lặp lại → cần ổn định context thay vì tiếp tục override.

## Root Cause (giả thuyết)

Spring TestContext shared-context cache trong reactor lớn (68 `@SpringBootTest`): khi 1 context fail load (resource pressure: Testcontainers Postgres + RANDOM_PORT servlet + springdoc scan), cache có thể trả failure cho test cùng key, hoặc eviction (`@DirtiesContext` ở sibling / cache size 32 mặc định bị vượt) buộc reload fail. 0.003s = không có init thực, lấy thẳng từ cache-failure.

## Proposed Fix

Ổn định context kiteclass-core SpringBootTest (chọn 1 hoặc kết hợp, theo điều tra trước per `release-fix-retry-budget.md` §3.5):
1. **Điều tra sibling poison context trước** — tìm `@SpringBootTest` nào fail-load đầu tiên dưới CI pressure (đọc CI log full-reactor, không patch mù).
2. **Context-cache key alignment** — gom các `@SpringBootTest` về cùng cấu hình (cùng `properties`/`@Import`) để giảm số context riêng → giảm eviction churn; hoặc tăng `spring.test.context.cache.maxSize`.
3. **`@DirtiesContext` discipline** — chỉ đánh dấu context dirty ở test thực sự mutate bean, tránh evict lan man.
4. **Quarantine** — tách `OpenApiSpecExportTest` sang surefire fork riêng (`forkCount`/`reuseForks=false` hoặc execution riêng) để không chia sẻ cache với sibling.

## Acceptance Criteria

- [ ] Xác định sibling-test (hoặc nguyên nhân) làm fail shared context đầu tiên trong CI full-reactor (điều tra empirical, không patch mù)
- [ ] `OpenApiSpecExportTest.exportSpec` PASS ổn định trong CI full-reactor ≥3 lần liên tiếp (không cần `ADMIN_MERGE_OVERRIDE`)
- [ ] Không tăng đáng kể tổng thời gian test kiteclass-core (quarantine fork phải cân nhắc cost)

## Related

- Discovered in: audit-fix campaign 2026-06-14 (red-flag #2416 / #2421 / #2422 — tất cả merge qua `ADMIN_MERGE_OVERRIDE` sau local-verify clean)
- Rule: `.claude/rules/release-fix-retry-budget.md` (lớp "preexisting flaky kiteclass-core"; §3.5 investigation-first)
- Rule: `.claude/rules/admin-merge-discipline.md` (override tax cần đóng bằng fix gốc)
- Memory: `project_kiteclass_core_it_ddl_auto_masks_migration_drift.md` (context của kiteclass-core test-infra)
- Filed by: audit-fixG-quality wave (cùng PR Jacoco/bundle/triage)
