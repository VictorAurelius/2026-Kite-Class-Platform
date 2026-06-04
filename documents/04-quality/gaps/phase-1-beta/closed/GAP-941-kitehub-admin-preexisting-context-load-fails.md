# GAP-941: kitehub-admin preexisting Spring context-load test failures block strict-warnings CI

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (PR #2155 GAP-937 fix CI surfaced AFTER subscription fixes landed)
**Affects:** Every PR touching `kitehub/kitehub-admin/**` OR triggering Test KiteHub Admin Service workflow — CI job FAILs với 7 errors trong `AdminControllerTest` + 1 error trong `KiteHubAdminApplicationTest.contextLoads`.

## Problem

Sau khi GAP-937 (kitehub-subscription Mockito UnnecessaryStubbing) đóng qua PR #2155, "Test KiteHub Admin Service (strict-warnings — GAP-245)" CI job vẫn FAIL. Class lỗi khác hoàn toàn GAP-937 scope:

| Test class | Test count | Failure type |
|---|---|---|
| `com.kitehub.admin.controller.AdminControllerTest` | 7 errors (0 pass) | Spring context load fail tại constructor injection / bean wiring |
| `com.kitehub.admin.KiteHubAdminApplicationTest.contextLoads` | 1 error | Application context cannot bootstrap |

Hậu quả: mọi PR touching `kitehub-admin` HOẶC `kitehub` parent pom HOẶC liên quan đến SubscriptionService (transitive dependency) đều fail CI. PR #2150 (đã merged main) + PR #2151/#2152/#2153/#2155 đều cùng pattern. Cần `ADMIN_MERGE_OVERRIDE: GAP-941` trailer cho mọi unrelated PR tới khi fix.

Tiền lệ pattern: GAP-735 (kiteclass-core Spring context-load), GAP-937 (kitehub-subscription Mockito) — đây là sister class thứ 3 trong family preexisting-flaky-tests cluster.

## Root Cause

Cần investigation phase per `release-fix-retry-budget.md` §3.5 trước khi propose fix. Possible classes:

1. **Bean wiring break** — code change ở `kitehub-platform` hoặc shared module thay đổi bean dependency mà admin module's @SpringBootTest config chưa update
2. **@MockBean / @TestConfiguration drift** — test config khai báo mocks cho production class signature cũ
3. **Constructor injection mismatch** — `AdminControllerTest` instantiate controller với mock list không match new constructor signature (recall PR #2151 added new SUB-20 payment gate logic → SubscriptionService constructor có thể đã thay đổi)
4. **Missing migration** — V60+ migration không apply trong test profile → schema drift → context fail

Likely candidate: refactor cluster Wave flow-kh3 (PR #2150 + PR #2147 + commit `ac54a419`) đổi `SubscriptionService` signature hoặc inject thêm dependency mà `AdminControllerTest` mock setup không follow.

## Proposed Fix

Wave dedicated cho admin module test cleanup (~1-2h effort):
1. Run failing tests + đọc Spring stack trace:
   ```bash
   cd kitehub && ./mvnw -pl kitehub-admin test -Dtest='AdminControllerTest,KiteHubAdminApplicationTest' 2>&1 | tee /tmp/gap941-baseline.log
   ```
2. Identify bean wiring issue from "UnsatisfiedDependencyException" hoặc "NoSuchBeanDefinitionException" trace
3. Per error: (a) update test @MockBean signature, (b) add missing @TestConfiguration provider, hoặc (c) update `AdminControllerTest` constructor mock list to match production signature
4. Re-run cluster đến khi xanh
5. Verify full `mvn verify -pl kitehub-admin` clean

## Acceptance Criteria

- [ ] `./mvnw -pl kitehub-admin test` PASS 0 failures 0 errors trên `main` HEAD
- [ ] CI job "Test KiteHub Admin Service (strict-warnings — GAP-245)" xanh trên PR mới động `kitehub-admin` không cần `ADMIN_MERGE_OVERRIDE`
- [ ] Investigation finding documented per `release-fix-retry-budget.md` §3.5 trong fix PR body

## Related

- Sister gaps (same family — preexisting flaky test class):
  - GAP-735 (kiteclass-core Spring context-load) — CLOSED Wave meta-3
  - GAP-937 (kitehub-subscription Mockito UnnecessaryStubbing) — CLOSED PR #2155 same session
- Triggered từ: PR #2155 CI fail post-GAP-937 fix 2026-06-04
- Blocks: clean merge của PR #2151/#2152/#2153/#2155 không cần admin override
- Likely cause: Wave flow-kh3 SubscriptionService signature changes (ac54a419 + PR #2150 + PR #2147)
- Rule cite: `.claude/rules/admin-merge-discipline.md` v1.0.3 §4 ADMIN_MERGE_OVERRIDE pattern
- Rule cite: `.claude/rules/release-fix-retry-budget.md` §3.5 investigation phase mandate
