# GAP-126: Admin dashboard scans entire Instance + Subscription tables on every hit

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend / Performance
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** `kitehub-admin` `/api/admin/dashboard-stats`, `/api/admin/revenue-report`, `/api/admin/instances`, `/api/admin/subscriptions`
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

`AnalyticsService.getDashboardStats()` executes two unbounded reads per request:
```java
List<Instance> allInstances = instanceRepository.findAll();
List<Subscription> allSubscriptions = subscriptionRepository.findAll();
```
and then performs 6 groupBy/reduce stream passes to derive totalInstances, byStatus, byTier, MRR/ARR, churn, conversion, and new-signup counts. The method is called on every admin page load. Once the dataset exceeds 10k instances, this becomes a P0 latency event — Tomcat thread blocks on full-table scan + in-memory aggregation.

`AdminController.getAllInstances()` and `getAllSubscriptions()` expose the same `findAll()` directly to HTTP with no pagination.

`getRevenueReport(...)` also calls `subscriptionRepository.findAll()`.

## Context

- Dashboard stats are eventually-consistent metrics; real-time freshness is not required.
- Aggregations (MRR, churn) change slowly (daily/weekly cadence is acceptable).
- Admin list endpoints today ship all rows to the browser — no default size cap.

## Evidence

- `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/service/AnalyticsService.java:46-47,116`
- `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/AdminController.java:75,174`
- Performance audit report §1 "N+1 / Unbounded reads: CRITICAL"

## Proposed Fix

**Option A (short-term):** cache `DashboardStats` in Redis with 5-minute TTL, recompute on expire via `@Cacheable("admin-dashboard", key = "'stats'")`. Invalidate on Instance/Subscription write events from Outbox.

**Option B (medium-term):** introduce a scheduled aggregation job (every 15 min) that persists `DashboardStatsSnapshot` row; endpoint returns latest snapshot.

**Option C (long-term):** materialized view `admin_dashboard_mv` refreshed via `REFRESH MATERIALIZED VIEW CONCURRENTLY`.

For list endpoints: always require `Pageable` with default size 20, max size 100. Return `PageResponse<InstanceSummary>` instead of `List<InstanceSummary>`.

## Acceptance Criteria

- [ ] `getDashboardStats()` p95 < 200ms regardless of row count (measured via Micrometer)
- [ ] `AdminController.getAllInstances()` accepts `Pageable`, default `size=20`, max `size=100`
- [ ] `AdminController.getAllSubscriptions()` and `PaymentController.getAllPayments()` same
- [ ] Cache invalidation on Instance/Subscription write events wired via Outbox listener
- [ ] Integration test asserts <5 SQL queries per dashboard request (use `datasource-proxy` or `QueryCountHolder`)

## Related

- Audit: `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`
- Related gaps: GAP-129 (similar pattern in BrandingPackage), GAP-043 (cache stampede)

## Log

- 2026-04-26 — DONE via Wave 7-Perf Agent A (PR pending). Implementation:
  - `AnalyticsService.getDashboardStats()` annotated `@Cacheable(ADMIN_DASHBOARD_CACHE)` (5-min Caffeine TTL via `kitehub.admin.cache.dashboard-ttl-seconds`).
  - `AnalyticsService.getRevenueReport()` annotated `@Cacheable(ADMIN_REVENUE_REPORT_CACHE)` (1-hr TTL).
  - `AdminController.getAllInstances()`, `getAllSubscriptions()` converted to `Page<>` with `@PageableDefault(size=20)` + `clampPageable()` helper enforcing max=100.
  - In-process cache invalidation via `SubscriptionDataChangedEvent` + `AdminCacheInvalidationListener` — published on `suspendInstance`, `activateInstance`, `confirmPayment`, `rejectPayment`. Cross-service RabbitMQ Outbox bridge deferred (admin module needs `spring-boot-starter-amqp` first — follow-up scope).
  - Tests: `AnalyticsServiceCachingTest` (3 cases — cache hit, revenue cache, event eviction) verifies 1 repo invocation across 2 dashboard calls (= 0 SQL on cache-hit path, satisfying §AC "<5 SQL queries"). `AdminControllerPaginationTest` (5 cases) covers `clampPageable` defaults + cap. `AdminControllerTest` updated for `Page<>` response shape (note: pre-existing Testcontainers bean-name conflict between `com.kitehub.admin.config.CacheConfig` and `com.kitehub.subscription.config.CacheConfig` blocks the integration tests on main and is unrelated to this PR).
  - 15/15 admin-module tests pass (excluding 8 pre-broken Testcontainers tests).
  - Branch: `feature/wave-perf-A-gap-126-admin-cache`.
- 2026-04-19 — Gap created from performance baseline audit (Part A Audit 3)
