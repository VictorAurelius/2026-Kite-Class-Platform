# GAP-240: kitehub-admin JPA repository scan misses subscription module repositories

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (test-time failure; production unaffected because admin runs separately, but tests block development)
**Domain:** Backend / Spring Configuration / Tests
**Detected:** 2026-04-27 (GAP-238 fix verification)
**Related:** GAP-238 (DONE), GAP-241 (admin CI coverage)

## Current State (verified 2026-04-27)

After GAP-238 fix removed `BeanDefinitionOverrideException`, kitehub-admin tests reveal next pre-existing failure:

```
UnsatisfiedDependencyException: Error creating bean with name 'subscriptionEventEmitter'...
constructor parameter 0: No qualifying bean of type
'com.kitehub.subscription.outbox.SubscriptionOutboxRepository' available
```

Affected tests: `AdminControllerTest` (7) + `KiteHubAdminApplicationTest` (1) = **8 tests** in kitehub-admin.

Root cause: kitehub-admin's `@SpringBootApplication` (or test config) doesn't include `com.kitehub.subscription.outbox` in its `@EnableJpaRepositories(basePackages=...)` scan. When admin context loads `subscriptionEventEmitter` (transitively via `EmailServiceClient` → `SubscriptionService` → `PaymentService`), it can't autowire `SubscriptionOutboxRepository`.

## Problem

kitehub-admin depends on kitehub-subscription. Many subscription components autowire `SubscriptionOutboxRepository` (per ADR-021 per-module outbox). When loaded into admin context for testing, repository must be discoverable.

## Proposed Fix

Add subscription's repository package to admin's JPA scan:

```java
// kitehub-admin/.../KiteHubAdminApplication.java OR a test config
@EnableJpaRepositories(basePackages = {
    "com.kitehub.admin.repository",
    "com.kitehub.subscription.repository",
    "com.kitehub.subscription.outbox"  // NEW
})
@EntityScan(basePackages = {
    "com.kitehub.admin.entity",
    "com.kitehub.subscription.entity",
    "com.kitehub.subscription.outbox"  // NEW (entity classes)
})
```

Verify by running `mvnw -pl kitehub-admin test` — all `@SpringBootTest` should load context cleanly.

## Acceptance Criteria

- [ ] `KiteHubAdminApplicationTest.contextLoads` passes
- [ ] All 7 `AdminControllerTest` tests pass
- [ ] No regression in subscription module tests (355/355)
- [ ] No regression in admin unit tests (15/15)

## Out-of-scope

- Production deployment: admin and subscription run as separate microservices in production; this is a test-time issue only
- Refactoring admin to NOT depend on subscription module (separate architectural concern)

## Related

- Parent: GAP-238 (DONE — bean collision fix surfaced this)
- Sibling: GAP-241 (admin CI coverage — would have caught this earlier)

## Log

- **2026-04-27** — Filed during GAP-238 fix verification. Pre-existing on main; surfaced because GAP-238 fix moved the failure from BeanDefinitionOverrideException to next layer (JPA scan).
