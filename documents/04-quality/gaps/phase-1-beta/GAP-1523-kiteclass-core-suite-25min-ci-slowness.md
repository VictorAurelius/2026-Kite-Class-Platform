# GAP-1523: kiteclass-core test-suite ~25min CI slowness under load (post-GAP-1393 residual)

**Status:** 🔵 OPEN
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

## Root Cause (hypotheses — cần investigate)

1. **8 `*AuthzTest` mới của GAP-1491 dùng `@SpringBootTest` full-context** thay vì `@WebMvcTest` slice → +8 Spring context load × DB conn → tăng context-cache pressure + thời gian. (CHECK: grep `@SpringBootTest` vs `@WebMvcTest` trong `*AuthzTest`.)
2. Suite có **69 `@SpringBootTest` + 11 `@DataJpaTest` context** — context-cache churn nặng dưới CI CPU/mem limit.
3. Runner load variance (GitHub-hosted runner shared).

## Proposed Fix (options)

- **(a)** Nếu `*AuthzTest` là `@SpringBootTest` → đổi sang `@WebMvcTest(controller) + @MockBean` slice (authz test không cần full context) → giảm 8 context.
- **(b)** Consolidate/giảm số `@SpringBootTest` distinct context (shared `@ContextConfiguration` / `@DirtiesContext` audit) → tăng context-cache hit.
- **(c)** Parallelize surefire (`forkCount` > 1 với `reuseForks`) — cẩn thận shared Testcontainer.
- **(d)** Band-aid: bump `forkedProcessTimeoutInSeconds` 1500→2400 + job `timeout-minutes` tương ứng (cho slow-but-passing runner pass) — KHÔNG fix root, chỉ nới.

## Acceptance Criteria

- [ ] Root cause xác định (grep `*AuthzTest` context type + đo thời gian suite local vs CI)
- [ ] "Test Core Service" pass reliably (<15min) trên ≥3 consecutive kiteclass-core PR CI runs (no trip-wire timeout)
- [ ] Không giảm test coverage thật (chỉ tăng tốc, không xóa test)

## Related

- **Parent:** GAP-1393 (DONE #2526) — fix hang root cause; gap này = residual slowness
- **Triggered by:** #2525 (GAP-1491 A01 fix) — admin-merged với `ADMIN_MERGE_OVERRIDE: GAP-1393`
- **Precedent:** GAP-735 (DONE) — kiteclass-core flaky-test class, admin-override precedent
- `admin-merge-discipline.md` §2/§4 — override mechanism dùng cho #2525
- `.github/workflows/core-ci.yml` + `kiteclass/kiteclass-core/pom.xml` (surefire/failsafe) + `TestContainersConfiguration.java`

## Log

- 2026-06-22 — Filed (loop round 6). Surfaced khi #2525/GAP-1491 hit 25min trip-wire timeout CẢ 2 LẦN (consistent, không phải runner one-off). GAP-1393 fix stopped 1h hang nhưng suite vẫn >25min dưới load. #2525 admin-merged per user AskUserQuestion choice. Filed as `ADMIN_MERGE_FOLLOWUP` của #2525.
