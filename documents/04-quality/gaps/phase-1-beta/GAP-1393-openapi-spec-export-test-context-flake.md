# GAP-1393: OpenApiSpecExportTest (kiteclass-core) intermittent "Failed to load ApplicationContext" trong CI full-reactor

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Test-infra
**Found:** 2026-06-14 (audit-fix campaign 2026-06-14 — red-flagged #2416/#2421/#2422)
**Affects:** `kiteclass/kiteclass-core/src/test/resources/application-test.yml` (root cause) + `kiteclass/kiteclass-core/pom.xml` (surefire insurance) + `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/openapi/OpenApiSpecExportTest.java` (victim)

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

## Root Cause (ĐÃ XÁC ĐỊNH empirical từ CI log — giả thuyết ban đầu SAI)

Điều tra CI log `gh run view 27509770548/27509543210 --log-failed` (cả 2 run đều FAIL **đúng 1 test = OpenApiSpecExportTest**, Errors: 1/1770). Bóc tách `Caused by` chôn dưới noise RabbitMQ `Connection refused`:

```
java.lang.IllegalStateException: Failed to load ApplicationContext ...
Caused by: ApplicationContextException: Unable to start web server
Caused by: WebServerException: Unable to start embedded Tomcat
Caused by: UnsatisfiedDependencyException: ... 'entityManagerFactory' ...
Caused by: BeanCreationException: entityManagerFactory ... Unable to create requested
           service [JdbcEnvironment] due to: Unable to determine Dialect without JDBC metadata
Caused by: HibernateException: Unable to determine Dialect without JDBC metadata
```

Đồng thời (cùng timestamp): `HikariPool-4 - Failed to validate connection PgConnection (This connection has been closed)` + lettuce Redis `Cannot reconnect to localhost:32770: Connection refused`.

**Cơ chế thật (KHÔNG phải "cached failure inherit từ sibling"):**

1. **OpenApiSpecExportTest là test `RANDOM_PORT` (Tomcat thật) DUY NHẤT** trong toàn bộ 68 `@SpringBootTest` của kiteclass-core (tất cả còn lại dùng `MOCK` web env). → context của nó có cache-key **duy nhất** → KHÔNG bao giờ reuse warm context, luôn **build mới**. Package `com.kiteclass.core.openapi` xếp **muộn** trong fork → build context mới ở cuối run (~sau 1766 test).

2. **`application-test.yml` đặt `hikari.minimum-idle: 10`** + `maximum-pool-size: 50`. Toàn suite share **1 static Testcontainers Postgres**, nhưng **mỗi cached context (`@SpringBootTest`/`@DataJpaTest`) mở pool Hikari RIÊNG**. Với Spring context-cache mặc định `maxSize = 32`, ở mức idle đã pin **32 × 10 = 320+ connection** vào 1 Postgres (`max_connections` mặc định ~100) → **cạn connection**.

3. Khi OpenApiSpecExportTest build context web mới ở cuối run (lúc connection đã cạn + connection cũ "has been closed"), Hibernate không mở được connection để đọc JDBC metadata → "Unable to determine Dialect" → EMF fail → web server fail → context load fail. `0.003s` = Spring **cache lại context-load FAILURE** (Spring 6.1+) nên test method ném ngay failure đã cache.

4. **PASS local** vì: run nhanh hơn, ít context cached đồng thời, Postgres/Docker local nhiều headroom hơn — không chạm trần connection. **FAIL CI** vì runner bị giới hạn + tích lũy qua 1766 test trước đó.

`minimum-idle: 10` từng được chọn cho `EnrollmentCapacityConcurrentTest` (20 thread × ≤3 TX) — nhưng nó áp cho MỌI context nên gây cạn connection toàn cục.

## Proposed Fix

Ổn định context kiteclass-core SpringBootTest (chọn 1 hoặc kết hợp, theo điều tra trước per `release-fix-retry-budget.md` §3.5):
1. **Điều tra sibling poison context trước** — tìm `@SpringBootTest` nào fail-load đầu tiên dưới CI pressure (đọc CI log full-reactor, không patch mù).
2. **Context-cache key alignment** — gom các `@SpringBootTest` về cùng cấu hình (cùng `properties`/`@Import`) để giảm số context riêng → giảm eviction churn; hoặc tăng `spring.test.context.cache.maxSize`.
3. **`@DirtiesContext` discipline** — chỉ đánh dấu context dirty ở test thực sự mutate bean, tránh evict lan man.
4. **Quarantine** — tách `OpenApiSpecExportTest` sang surefire fork riêng (`forkCount`/`reuseForks=false` hoặc execution riêng) để không chia sẻ cache với sibling.

## Acceptance Criteria

- [x] Xác định nguyên nhân thật làm fail context trong CI full-reactor (điều tra empirical, không patch mù) — **DONE**: connection exhaustion trên shared Testcontainers Postgres do `minimum-idle: 10` × nhiều cached context; OpenApiSpecExportTest (RANDOM_PORT duy nhất) build muộn là nạn nhân. Bằng chứng = CI log `Caused by: Unable to determine Dialect` + `connection has been closed`.
- [ ] `OpenApiSpecExportTest.exportSpec` PASS ổn định trong CI full-reactor ≥3 lần liên tiếp (không cần `ADMIN_MERGE_OVERRIDE`) — **PENDING CI**: flake không tái hiện local nên chỉ xác nhận được qua quan sát CI nhiều run. Fix root-cause đã land + insurance rerun.
- [x] Không tăng đáng kể tổng thời gian test kiteclass-core — **DONE**: `minimum-idle: 0` không tăng thời gian (chỉ release idle conn); surefire rerun chỉ kích hoạt khi test FAIL; không quarantine fork riêng.

## Resolution (2026-06-15 — PARTIAL, best-effort root-cause fix, CI-confirm pending)

**Fix 1 — root cause (`src/test/resources/application-test.yml`):** `hikari.minimum-idle` 10 → **0** (cached context không pin connection idle nữa) + thêm `idle-timeout: 10000` + `max-lifetime: 60000` + `keepalive-time: 30000` (release nhanh slot Postgres giữa các cached context + validate/replace dead connection — đúng cảnh báo CI "Failed to validate connection ... has been closed"). Giữ `maximum-pool-size: 50` cho burst của `EnrollmentCapacityConcurrentTest` (chỉ context active dùng tới, các context idle giờ giữ 0 conn).

**Fix 2 — insurance (`pom.xml` maven-surefire-plugin):** `rerunFailingTestsCount=2` + `systemPropertyVariables: spring.test.context.failure.threshold=5`. Spring 6.1+ cache context-load FAILURE và fail-fast sau `threshold` (mặc định 1) → rerun thường sẽ dính lại failure đã cache (chính là triệu chứng 0.003s); nâng threshold để rerun **build lại context thật** → retry mới có tác dụng cho transient context-load flake. KHÔNG đụng `argLine` → JaCoCo unit coverage giữ nguyên (verify: log build vẫn in `argLine set to -javaagent:...jacoco`).

**Local verify (foreground):**
- `./mvnw test -Dtest=OpenApiSpecExportTest -P strict-warnings` → `Tests run: 1, Failures: 0, Errors: 0` · BUILD SUCCESS · jacoco argLine intact.
- `./mvnw test -Dtest=EnrollmentCapacityConcurrentTest` (test mà pool 50/10 từng tune cho) → PASS, **không regression** với `minimum-idle: 0`.
- `./mvnw validate` → exit 0.

**Tại sao PARTIAL chứ không DONE:** flake KHÔNG tái hiện local (suite local xanh) nên AC #2 ("PASS ≥3x CI không cần override") chỉ chứng minh được bằng quan sát CI qua nhiều run. Root cause đã xác định deterministic + fix đúng hướng, nhưng theo `gap-done-discipline.md` không flip DONE khi AC còn chưa verify được. Đóng DONE khi 3 PR kiteclass-core kế tiếp qua "Test Core Service" mà không cần `ADMIN_MERGE_OVERRIDE`.

## Log

- **2026-06-15:** Điều tra CI log (#2416 run 27509770548, #2422 run 27509543210) → bác bỏ giả thuyết "sibling cache inherit"; root cause thật = connection exhaustion shared Postgres (`minimum-idle: 10` × cached contexts). Land Fix 1 (Hikari `minimum-idle: 0` + lifecycle bounds) + Fix 2 (surefire rerun + failure.threshold). Local verify PASS (isolation + concurrent test + validate). Status OPEN → PARTIAL (70%). CI-confirm AC #2 pending. PR `fix/open1393-2026-06-15`.

## Related

- Discovered in: audit-fix campaign 2026-06-14 (red-flag #2416 / #2421 / #2422 — tất cả merge qua `ADMIN_MERGE_OVERRIDE` sau local-verify clean)
- Rule: `.claude/rules/release-fix-retry-budget.md` (lớp "preexisting flaky kiteclass-core"; §3.5 investigation-first)
- Rule: `.claude/rules/admin-merge-discipline.md` (override tax cần đóng bằng fix gốc)
- Memory: `project_kiteclass_core_it_ddl_auto_masks_migration_drift.md` (context của kiteclass-core test-infra)
- Filed by: audit-fixG-quality wave (cùng PR Jacoco/bundle/triage)
