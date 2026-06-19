# GAP-944: Cross-module payment cache invalidation via RabbitMQ topic event

**Status:** 🔵 OPEN
**Priority:** P2
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 PR #2162 — duplicate-mapping CI fix dropped ApplicationEvent publishing)
**Affects:** Admin dashboard payment cache freshness

## Problem

PR #2162 (`fix(ci): remove duplicate payment endpoints from legacy AdminController`) deleted 3 legacy payment endpoints from `kitehub-admin/AdminController.java` to resolve Spring Ambiguous Mapping CI cluster. Canonical home is now `AdminPaymentController` in `kitehub-subscription` (introduced PR #2150).

**Side effect:** legacy controller previously published `SubscriptionDataChangedEvent("payment.confirmed"|"payment.rejected", paymentId)` via Spring `ApplicationEventPublisher` to invalidate dashboard cache (consumed by `AdminCacheInvalidationListener` in same module). New controller does NOT publish this event.

`kitehub-subscription` CANNOT import `SubscriptionDataChangedEvent` from `com.kitehub.admin.event` — `kitehub-admin` already depends on `kitehub-subscription` (per `kitehub-admin/pom.xml`); reversing would create circular dependency.

Net effect: admin dashboard cache may serve stale payment data after admin confirms/rejects a payment until natural TTL expiry.

## Proposed Fix

Wire cross-module cache invalidation via RabbitMQ topic event (per `design-patterns.md` §3.5 Outbox pattern):
1. `PaymentService.confirmPayment()` + `rejectPayment()` write outbox row with routing key `payment.confirmed` / `payment.rejected` (atomic with DB update)
2. Outbox dispatcher publishes to RabbitMQ topic exchange
3. `AdminCacheInvalidationListener` in kitehub-admin subscribes via `@RabbitListener` instead of `@EventListener`
4. Idempotency: dedupe on `paymentId` (same payment confirmed twice = single eviction)

## Acceptance Criteria

- [ ] Outbox row written within `PaymentService.confirmPayment` / `rejectPayment` `@Transactional` block
- [ ] `AdminCacheInvalidationListener` (or new `PaymentCacheInvalidationListener`) `@RabbitListener` subscribed to `payment.confirmed` / `payment.rejected` topic routes
- [ ] Integration test verifies cache eviction after admin confirm via cross-module event flow (Testcontainers RabbitMQ + Spring boot test)
- [ ] No regression on legacy `SubscriptionDataChangedEvent` flow for instance suspend/activate (those still in-module, unchanged)

## Related

- Discovered in: PR #2162 (`AUDIT_OVERRIDE: cross-module cache invalidation via RabbitMQ`)
- Parent fix: PR #2162 (CI cluster fix — duplicate Spring mapping)
- PR #2150 (canonical `AdminPaymentController` source)
- Design pattern: `.claude/rules/design-patterns.md` §3.5 Outbox
- Existing infrastructure: `kitehub-admin/src/main/java/com/kitehub/admin/event/AdminCacheInvalidationListener.java` (already exists, currently listens to in-module ApplicationEvent)
- Sister mechanism: `BrandingEventPublisher` (kiteclass-core) per ADR-021 per-module outbox
