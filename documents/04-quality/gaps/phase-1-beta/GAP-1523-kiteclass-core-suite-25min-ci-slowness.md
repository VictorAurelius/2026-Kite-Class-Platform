# GAP-1523: kiteclass-core test-suite ~25min CI slowness under load (post-GAP-1393 residual)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Test-infra
**Found:** 2026-06-22 (loop round 6 — surfaced khi land #2525/GAP-1491)
**Affects:** `kiteclass/kiteclass-core` test suite (`Test Core Service` CI job) + mọi PR đụng kiteclass-core

## Problem

GAP-1393 (DONE #2526) đã fix root cause treo CI kiteclass-core: shared Testcontainers Postgres `max_connections=100` cạn → context-load fail → fork treo ~1h. Fix: `max_connections=500` + surefire `forkedProcessTimeoutInSeconds=1500` (25min) + core-ci.yml `timeout-minutes` per job. **CI giờ không treo >30min nữa** (trip-wire fail-fast).

**Residual (gap này):** suite vẫn **quá chậm dưới CI load**. Bằng chứng từ #2525 (GAP-1491):
- #2526 (chỉ fix GAP-1393): "Test Core Service" pass **6m20s**.
- #2525 (fix + GAP-1491's 8 @PreAuthorize + 26 authz test): hit **25min fork trip-wire timeout CẢ 2 LẦN** (build đạt `Tests run: 1393, Failures: 0` rồi bị kill → 11 lỗi `AssignmentIntegrationTest` "Could not open JPA EntityManager" = collateral của timeout-kill, KHÔNG phải test fail thật).

→ Suite chạy biến thiên lớn (6min ↔ 25min+) tùy runner load. Mọi PR kiteclass-core có nguy cơ hit trip-wire timeout (fail) trừ khi gặp fast runner. #2525 phải `--admin` merge (`ADMIN_MERGE_OVERRIDE: GAP-1393`, user-approved) vì lý do này.

## Root Cause (XÁC ĐỊNH 2026-06-22 — static analysis)

**Hypothesis #1 SAI:** chỉ **3/17** `*AuthzTest` dùng `@SpringBootTest` (`CrossTenantAuthzTest` / `CrossUserAuthzTest` / `LmsAuthoringAuthzTest`) — 13 đã là `@WebMvcTest` slice nhẹ + 1 service test. GAP-1491 KHÔNG thêm 8 full-context.

**Hypothesis #2 ĐÚNG — Spring TestContext context-cache thrash.** Suite có ~50 context PHÂN BIỆT vs cache default `maxSize=32` → LRU eviction → heavy full-context phải reload nhiều lần.

Số liệu distinct-context (grep, no `| head`):
- **35 `@WebMvcTest(controller)`** — mỗi controller = 1 slice context riêng (nhẹ, ~1s build).
- **64 `@SpringBootTest`** NHƯNG chỉ **~6 context phân biệt**: chỉ 1 class có `@MockBean`, chỉ 2 có `properties=`/`@DirtiesContext`, còn lại cluster vào ~5 combo `@Import` → Spring **share** cached context cho config giống nhau (37×`{TestContainers,TestSecurity,TestTenant}` + 17×`{TestContainers}` + 7×no-import + 7×`{TestContainers,TestSecurity}`). Đây là context NẶNG (Hibernate + Testcontainers conn + full bean graph, ~5-15s build/runner ràng buộc).
- **~11 `@DataJpaTest`** — context nặng riêng.

→ ~35 + ~6 + ~11 = **~50 distinct > 32 cache** → eviction bắt buộc. 35 WebMvc slice nhẹ cycle qua cache đẩy (LRU) các full-context nặng ra → reload đắt. Local (RAM/CPU dư) reload nhanh → ~6min; CI runner ràng buộc → reload chậm → 25min+. GAP-1393 fix *connection exhaustion* (minimum-idle 10→0, max_conn→500) nhưng KHÔNG fix *cache churn* = residual này. Quan trọng: sau GAP-1393 `minimum-idle:0` → cached-idle context pin ~0 conn → lý do cũ giữ cache nhỏ (320 pinned conn) KHÔNG còn → nâng maxSize an toàn về connection.

## Proposed Fix — ÁP DỤNG (root-cause, low-risk, reversible)

Nâng `spring.test.context.cache.maxSize` 32 → **64** (trên ~50 distinct → 0 eviction → mỗi context build đúng 1 lần) + `-Xmx4g` heap headroom cho cache lớn hơn (runner public-repo = 16GB RAM). Set qua `pom.xml` surefire + failsafe `<systemPropertyVariables>` (maxSize đọc từ `SpringProperties`, KHÔNG phải application.yml) — co-located với GAP-1393 config.

Options KHÔNG chọn: (a) đổi 3 AuthzTest `@SpringBootTest`→slice = impact thấp + chúng cần full cross-tenant context; (c) `forkCount>1` = risky shared Testcontainer; (d) bump timeout = band-aid không fix root.

## Acceptance Criteria

- [x] Root cause xác định (static analysis: ~50 distinct context vs cache 32 → thrash; hypothesis #1 SAI bác bỏ)
- [ ] "Test Core Service" pass reliably (<15min) trên ≥3 consecutive kiteclass-core PR CI runs (no trip-wire timeout) — *PR này = run #1; cần 2 run nữa*
- [x] Không giảm test coverage thật (chỉ config maxSize + heap, 0 test xóa)

## Related

- **Parent:** GAP-1393 (DONE #2526) — fix hang root cause; gap này = residual slowness
- **Triggered by:** #2525 (GAP-1491 A01 fix) — admin-merged với `ADMIN_MERGE_OVERRIDE: GAP-1393`
- **Precedent:** GAP-735 (DONE) — kiteclass-core flaky-test class, admin-override precedent
- `admin-merge-discipline.md` §2/§4 — override mechanism dùng cho #2525
- `.github/workflows/core-ci.yml` + `kiteclass/kiteclass-core/pom.xml` (surefire/failsafe) + `TestContainersConfiguration.java`

## Log

- 2026-06-22 — **Root cause xác định + fix shipped (→ PARTIAL).** Static analysis bác bỏ hypothesis #1 (chỉ 3/17 AuthzTest là `@SpringBootTest`, không phải 8). Xác nhận hypothesis #2: ~50 distinct Spring context (35 `@WebMvcTest` slice + ~6 `@SpringBootTest` config phân biệt + ~11 `@DataJpaTest`) > cache default `maxSize=32` → LRU thrash → heavy-context reload. Fix: `spring.test.context.cache.maxSize` 32→64 + `-Xmx4g` qua pom.xml surefire+failsafe `<systemPropertyVariables>`/`argLine`. Local verify: suite pass + context-cache stats `missCount == size` (0 reload) + no OOM @4g. Stays PARTIAL — AC#2 cần `<15min` trên ≥3 consecutive CI run (PR này = run #1).
- 2026-06-22 — Filed (loop round 6). Surfaced khi #2525/GAP-1491 hit 25min trip-wire timeout CẢ 2 LẦN (consistent, không phải runner one-off). GAP-1393 fix stopped 1h hang nhưng suite vẫn >25min dưới load. #2525 admin-merged per user AskUserQuestion choice. Filed as `ADMIN_MERGE_FOLLOWUP` của #2525.
