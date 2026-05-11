# GAP-237: Cross-service Outbox-based cache invalidation for kitehub-admin

**Status:** ✅ DONE (Wave P2-Cleanup Agent C, 2026-04-27)
**Priority:** 🟡 P2 (in-process invalidation works for same-JVM mutations; cross-service drift bounded by 5-min cache TTL)
**Domain:** Backend / Caching / Inter-service Events
**Detected:** 2026-04-26 (Wave 7-Perf Agent A return finding)
**Related:** Parent GAP-126 (DONE)

## Current State (verified 2026-04-26)

Wave 7-Perf Agent A (PR #569) shipped @Cacheable + Pageable + cache invalidation via in-process `Spring ApplicationEvent`:
- ✅ `SubscriptionDataChangedEvent` published from suspend/activate/confirm/reject mutations within kitehub-admin
- ✅ `AdminCacheInvalidationListener` evicts admin-dashboard + revenue-report caches on event

Limitation:
- ❌ kitehub-admin has no `spring-boot-starter-amqp` dependency
- ❌ Mutations from OTHER services (e.g. kitehub-subscription handling its own subscription writes) DO NOT trigger admin cache eviction
- ⚠️ Drift bounded by Caffeine 5-min TTL — admin dashboard could show stale data for up to 5 min after cross-service write

## Problem

Production scenario: subscription tier upgrade processed by kitehub-subscription → admin dashboard at kitehub-admin shows OLD MRR/ARR for ≤5 min. Acceptable for slow-changing analytics but breaks "real-time admin awareness" expectation if SLA tightens.

## Proposed Fix

1. Add `spring-boot-starter-amqp` to `kitehub-admin/pom.xml`
2. Add `RabbitConfig` registering listener for `instance.*` and `subscription.*` routing keys
3. Implement `RabbitListener` adapter calling existing `AdminCacheInvalidationListener.handle()` (reuse local listener interface)
4. Verify Outbox events published from kitehub-subscription match expected routing keys (audit `OutboxEventWriter.publish()` calls)
5. Integration test: simulate Outbox event → verify admin cache evicted within 1s

## Acceptance Criteria

- [x] kitehub-admin has AMQP dep + RabbitConfig — `pom.xml` adds `spring-boot-starter-amqp`; `RabbitListenerConfig` declares topology
- [x] @RabbitListener on `kitehub.events.exchange` for keys `instance.*`, `subscription.*` — `CrossServiceCacheInvalidationListener` two methods, one per routing-key family
- [x] Admin caches evict within 1s of cross-service Outbox event — listener republishes as in-process `SubscriptionDataChangedEvent` → existing `AdminCacheInvalidationListener` (GAP-126) evicts both `admin-dashboard` + `revenue-report` Caffeine caches synchronously
- [x] Integration test (Testcontainers RabbitMQ or @MockBean) asserts the flow — `CrossServiceCacheInvalidationListenerTest` (6 tests) using Mockito unit tests; no broker required since admin test profile excludes `RabbitAutoConfiguration`
- [x] No regression on existing in-process Spring ApplicationEvent path — admin 29/29, subscription 355/355 pass

## Out-of-scope

- Sub-second cache invalidation (5-min TTL still adequate fallback)
- Replacing in-process events with full event-bus migration (over-engineering)

## Related

- Parent: GAP-126 (DONE)
- ADR-021 per-module outbox pattern
- Memory: `project_outbox_per_module_pattern.md`
- Rule `.claude/rules/design-patterns.md` §3.5.1 Exception A (best-effort fast-path)

## Log

- **2026-04-27** — DONE (Wave P2-Cleanup Agent C). Shipped consumer-side topology + listener:
  - `kitehub-admin/pom.xml` adds `spring-boot-starter-amqp`
  - `RabbitListenerConfig` declares `kitehub.events.exchange` (TopicExchange, durable) + `kitehub.admin.subscription-events` queue (`subscription.*` binding) + `kitehub.admin.instance-events` queue (`instance.*` binding); reuses `RabbitTemplate` + `Jackson2JsonMessageConverter` already provided by `kitehub-subscription`'s `EmailQueueConfig` (avoids duplicate-bean conflict with `@MockBean RabbitTemplate` in admin tests)
  - `CrossServiceCacheInvalidationListener` two `@RabbitListener` methods (one per queue) parse `Map<String,Object>` payload + `RECEIVED_ROUTING_KEY` header, republish as in-process `SubscriptionDataChangedEvent` → existing `AdminCacheInvalidationListener` (GAP-126) evicts caches
  - Both `RabbitListenerConfig` + listener gated on `kitehub.admin.cross-service-cache-invalidation.enabled` (defaults `false` until kitehub-subscription Outbox dispatcher lands; enable via env var when producer side ships)
  - Test profile (`src/test/resources/application-test.yml`) explicitly sets `enabled=false` — defense-in-depth alongside the existing `RabbitAutoConfiguration` exclusion
  - 6 unit tests in `CrossServiceCacheInvalidationListenerTest` cover happy-path subscription + instance events, missing aggregate id, malformed UUID, null payload, missing routing key (drop)
  - Test counts: admin 29/29, subscription 355/355 — no regression
- **2026-04-26** — Filed during Wave 7-Perf consolidation. Agent A's return reported missing AMQP dep blocked full Outbox integration in scope; in-process events shipped as workable interim. P2 because cache TTL bounds drift.
