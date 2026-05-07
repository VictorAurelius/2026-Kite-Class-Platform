# GAP-432: 3 service `findAll()` không bounded — performance cliff post-launch

**Status:** 🟡 PARTIAL 2026-05-07 — code refactor + tests DONE; JMH benchmark + Performance audit re-score deferred to Wave 41 closure audit
**Priority:** 🟠 P1 (Phase 1 BETA — cold-cache full-table scan; risk leo thang khi tenant count tăng)
**Domain:** Backend / Performance
**Found:** 2026-05-08 Wave 40 audit milestone (Bucket D Performance, PR #972)
**Affects:** 3 service classes trong `kitehub-admin` + `kitehub-subscription`

## Problem

Wave 40 Performance audit phát hiện 3 callsite `findAll()` không có Pageable hay filter, risk full-table scan post-launch:

| # | File | Line | Pattern |
|---|------|------|---------|
| 1 | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/service/AnalyticsService.java` | 57, 58, 129 | 3× unbounded — Instance + Subscription full-table scan; Caffeine 5-min TTL chỉ cushion warm-cache; cold-start = performance cliff |
| 2 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/PaymentService.java` | 121 | Unbounded payment scan, không Pageable, không status filter — payments table grow unbounded với usage |
| 3 | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/InstanceService.java` | 337 | Unbounded instance scan — high risk khi instance count grows post-launch |

## Root Cause

Code legacy từ Wave 1-4 prototype (DB nhỏ, không cần pagination). Audit tự động phát hiện nhưng pre-Wave-40 không có flag.

## Proposed Fix

**AnalyticsService (3 callsites):** thay `findAll()` bằng DB-side aggregation:
```java
// Before
List<Instance> all = instanceRepo.findAll();
long active = all.stream().filter(i -> i.getStatus() == ACTIVE).count();

// After
@Query("SELECT new DashboardStats(COUNT(i)) FROM Instance i WHERE i.status = 'ACTIVE'")
DashboardStats getActiveStats();
```

**PaymentService:121:** scope theo instance hoặc paginate:
```java
// Before
List<Payment> all = paymentRepo.findAll();

// After
Page<Payment> findByStatusAndCreatedAfter(Status status, Instant since, Pageable pageable);
```

**InstanceService:337:** xác định calling context, add `ownerIdAndDeletedFalse` filter hoặc pagination.

## Acceptance Criteria

- [x] AnalyticsService 3 callsites refactored sang DB-side aggregation queries (`countByDeletedFalse`, `countInstancesByStatus`, `countSubscriptionsByTier`, `sumActiveMrr`, `sumActiveRevenueByTier`, `sumCancelledRevenue`, `findActiveInPeriod` — all WHERE-clause bounded)
- [x] PaymentService:121 + InstanceService:337 thêm Pageable + filter (`findAllNotDeleted(Pageable)`, `findByStatusNotDeleted`, `findByDeletedFalse(Pageable)`); controllers expose `page`/`size` query params (default 50, capped at 200)
- [x] Unit tests cover new query methods (existing test coverage maintain) — `AnalyticsServiceTest` rewritten với GAP-432 invariant `verify(repo, never()).findAll()`; new `PaymentServiceBoundedQueryTest` (3 tests) + `InstanceServiceBoundedListTest` (2 tests); `AnalyticsServiceCachingTest` updated to bounded path; `InstanceApiContractTest` updated to Page envelope. 29/29 tests pass với `mvn verify -P strict-warnings`
- [ ] Performance test (JMH hoặc inline benchmark): cold-cache scenario <100ms cho dashboard stats với 1k instance fixture — **deferred Phase 2** (requires JMH harness + 1k row fixture; tracked as follow-up)
- [ ] Audit re-score Performance /100 ≥80 (current 75 +5) — pending Wave 41 closure audit cycle

## Related

- Wave 40 Bucket D Performance audit (PR #972) — báo cáo gốc
- `documents/04-quality/audits/performance/2026-05-08-wave-40-milestone.md` §Findings P1
- GAP-392 (DONE Wave 35) — sister N+1 fix in `SlugAvailabilityService.findAll()` — pattern reference
- Wave 36 GAP-393 Caffeine cache — chỉ cushion warm-cache, không thay refactor cold path

## Estimated effort

~3-4h (3 callsites × ~1h refactor + test + benchmark). 1 ngăn wave-pack — disjoint per-file (Analytics ↔ Payment ↔ Instance khác file).

## Log

- **2026-05-07** Wave 41 Bucket C shipped (this PR): all 5 unbounded `findAll()` callsites refactored to bounded DB-side queries. 3 callsites trong `AnalyticsService` (lines 57/58/129) → `countByDeletedFalse` + `countInstancesByStatus` + `countSubscriptionsByTier` + `sumActiveMrr` + `sumActiveRevenueByTier` + `findActiveInPeriod`; `PaymentService:121` + `InstanceService:337` → `Pageable`-driven paged queries (default size 50, hard cap 200); controllers exposed `page`/`size` query params. Tests pass: `mvn -pl kitehub-subscription,kitehub-admin verify -P strict-warnings` 29/29 green; new tests + caching test updated to assert `verify(repo, never()).findAll()` invariant. Status PARTIAL not DONE: AC items 4 (JMH benchmark) + 5 (audit re-score) deferred to Wave 41 closure audit cycle per `gap-done-discipline.md` §3. **API contract change:** `GET /api/platform/instances` and `GET /api/platform/payments` now return `Page<...>` envelope (`{content, totalElements, ...}`) instead of bare array — admin FE may need adapter.
- **2026-05-08** Filed during Wave 40 closure handoff. Audit Bucket D Performance bundle 3 P1 callsite vào 1 gap để Wave 41 fix-cluster có 1 ngăn refactor.
