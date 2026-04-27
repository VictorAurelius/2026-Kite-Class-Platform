# GAP-243: AdminControllerTest needs S3 mock + RabbitMQ mock for full @SpringBootTest

**Status:** 🟢 DONE 2026-04-27 — @DynamicPropertySource extended with S3 mock-mode + region; @MockBean RabbitTemplate added; AdminControllerTest 7/7 + admin full suite 23/23 pass
**Priority:** 🟡 P2 (test-only failure; production unaffected; admin module's other tests + subscription full suite green)
**Domain:** Backend / Tests / Test Infrastructure
**Detected:** 2026-04-27 (GAP-242 fix verification surface)

## Current State (verified 2026-04-27)

After GAP-242 fix (V11 SQL valid Postgres) + GAP-240 (JPA scan) + GAP-238 (bean collision), AdminControllerTest still fails at deeper test-infra layers:

```
Caused by: BeanCreationException: 's3Client' ... region must not be null.
```

After overriding for S3:

```
Caused by: NoSuchBeanDefinitionException: No qualifying bean of type 'RabbitTemplate' available
... required by EmailServiceClient
```

`AdminControllerTest`'s `@DynamicPropertySource` registers Postgres datasource + Flyway + JWT + encryption — but NOT:
- `storage.s3.mock-mode=true` (gates S3Client off via @ConditionalOnProperty)
- `storage.s3.region=ap-southeast-1` (S3Client factory needs)
- A way to provide RabbitTemplate mock OR exclude the AMQP autoconfiguration without breaking EmailServiceClient injection

`KiteHubAdminApplicationTest` works because it activates `@ActiveProfiles("test")` → `application-test.yml` provides S3 mock + Rabbit exclusion. But that profile uses H2 driver/dialect which conflicts with AdminControllerTest's Testcontainers Postgres.

## Problem

AdminControllerTest needs a hybrid config: real Postgres (Testcontainers) + S3 mock (from test profile) + RabbitTemplate handling (mock or test profile-style exclusion). Each fix path has its own conflict.

## Proposed Fix Options

### Option A — Extend `@DynamicPropertySource` (least invasive)
Add to AdminControllerTest:
```java
registry.add("storage.s3.mock-mode", () -> "true");
registry.add("storage.s3.region", () -> "ap-southeast-1");
registry.add("storage.s3.bucket", () -> "test-bucket");
registry.add("storage.s3.access-key", () -> "test");
registry.add("storage.s3.secret-key", () -> "test");
```
For RabbitTemplate: add `@MockBean RabbitTemplate rabbitTemplate` field, OR exclude `RabbitAutoConfiguration` via `@TestPropertySource(properties = "spring.autoconfigure.exclude=...RabbitAutoConfiguration")` and accept that EmailServiceClient autowire might require @Lazy.

### Option B — Separate test profile `admin-controller`
Create `application-admin-controller.yml` with: Postgres driver/dialect + S3 mock + Rabbit exclusion. Use `@ActiveProfiles("admin-controller")` on AdminControllerTest. Override datasource URL/user/pass via @DynamicPropertySource for Testcontainers.

### Option C — Refactor admin controller's transitive deps
Identify whether AdminControllerTest actually needs the full dependency chain (InstancePurgeService → BackupStorageService → S3Client; SubscriptionService → EmailServiceClient → RabbitTemplate). If admin's test only exercises GET endpoints + dashboard stats, narrow `@SpringBootTest(classes={...})` to required slice. Most pragmatic for fast feedback loop.

## Acceptance Criteria

- [ ] All 7 `AdminControllerTest` tests pass with full @SpringBootTest context
- [ ] No regression in admin unit tests (15/15) or subscription full suite (355/355)
- [ ] CI workflow updated: `kitehub-ci.yml` admin job removes `-Dtest=` exclusion → flips GAP-241 to DONE
- [ ] Test-infra approach documented in `documents/05-guides/` or admin module README

## Out-of-scope

- Migrating other admin tests (none exist that exercise full controller stack)
- Standardizing test infrastructure across all kitehub modules (broader concern)

## Related

- Parent: GAP-242 (PARTIAL — V11 SQL fixed; this gap continues the test-infra cleanup)
- Sibling: GAP-241 (admin CI coverage; PARTIAL until this closes)
- Memory: `feedback_jpa_jsonb_jdbctypecode.md` (real-Postgres needs Testcontainers per kiteclass pattern)

## Log

- **2026-04-27 (DONE same day):** Status 🔵 OPEN → 🟢 DONE. **Option A** (extend @DynamicPropertySource) shipped — minimal invasive. Added S3 mock properties (`storage.s3.mock-mode=true`, `region`, `bucket`, `access-key`, `secret-key`) + webhook + backup config. Added `@MockBean RabbitTemplate` field — Mockito proxy resolves EmailServiceClient autowire without real broker. Verification: AdminControllerTest 7/7 ✅, admin full suite 23/23 ✅, subscription 355/355 ✅ (no regression). `kitehub-ci.yml` admin job updated — removed `-Dtest=` exclusion, now runs full admin suite. **GAP-241 + GAP-242 also flip to DONE** (their PARTIAL status was waiting on this gap). Note: `@MockBean` deprecated since Spring Boot 3.4.0 — minor warning, replace with `@MockitoBean` in future cleanup PR.
- **2026-04-27** — Filed during GAP-242 fix verification. V11 SQL bug closed; AdminControllerTest then surfaces S3 + RabbitMQ test-infra gaps. P2 because production unaffected — these are test-only setup deficiencies in `AdminControllerTest` that mask real test coverage.
