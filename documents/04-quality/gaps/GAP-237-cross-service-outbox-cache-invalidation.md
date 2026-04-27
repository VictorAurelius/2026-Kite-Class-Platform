# GAP-237: Cross-service Outbox-based cache invalidation for kitehub-admin

**Status:** 🔵 OPEN
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

- [ ] kitehub-admin has AMQP dep + RabbitConfig
- [ ] @RabbitListener on `kitehub.events.exchange` for keys `instance.*`, `subscription.*`
- [ ] Admin caches evict within 1s of cross-service Outbox event
- [ ] Integration test (Testcontainers RabbitMQ or @MockBean) asserts the flow
- [ ] No regression on existing in-process Spring ApplicationEvent path

## Out-of-scope

- Sub-second cache invalidation (5-min TTL still adequate fallback)
- Replacing in-process events with full event-bus migration (over-engineering)

## Related

- Parent: GAP-126 (DONE)
- ADR-021 per-module outbox pattern
- Memory: `project_outbox_per_module_pattern.md`
- Rule `.claude/rules/design-patterns.md` §3.5.1 Exception A (best-effort fast-path)

## Log

- **2026-04-26** — Filed during Wave 7-Perf consolidation. Agent A's return reported missing AMQP dep blocked full Outbox integration in scope; in-process events shipped as workable interim. P2 because cache TTL bounds drift.
