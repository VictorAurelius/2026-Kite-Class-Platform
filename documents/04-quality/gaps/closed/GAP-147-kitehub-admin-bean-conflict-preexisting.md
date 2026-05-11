---
name: GAP-147 — KiteHub Admin OpenAPI bean conflict (pre-existing)
description: Pre-existing test failure in kitehub-admin discovered during GAP-131/133 fix work — bean conflict openApiConfig subscription vs admin — closed Wave 9-F
type: gap
---

# GAP-147: KiteHub Admin OpenAPI Bean Conflict (Pre-existing)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend (kitehub-admin)
**Found:** 2026-04-20 (discovered during Part B perf batch PR #375; verified pre-existing on main)
**Closed:** 2026-04-21 (Wave 9-F)
**Affects:** `kitehub-admin/KiteHubAdminApplicationTest.contextLoads`

## Problem

`kitehub-admin/src/test/java/.../KiteHubAdminApplicationTest#contextLoads` fails with:

```
ConflictingBeanDefinitionException: Annotation-specified bean name 'openApiConfig'
  for bean class [com.kitehub.subscription.config.OpenApiConfig] conflicts with
  existing, non-compatible bean definition of same name and class
  [com.kitehub.admin.config.OpenApiConfig]
```

Two `OpenApiConfig` classes live in overlapping Spring component-scan paths. Both are annotated `@Configuration` with bean name `openApiConfig` (default — class name lowercased). Context startup fails.

## Root Cause

`KiteHubAdminApplication` explicitly scans `com.kitehub.subscription`:

```java
@SpringBootApplication(scanBasePackages = {
    "com.kitehub.admin",
    "com.kitehub.platform",
    "com.kitehub.subscription"
})
```

Both packages contain an `OpenApiConfig`, both annotated `@Configuration` with the default bean name `openApiConfig`. Spring's `ClassPathBeanDefinitionScanner.checkCandidate()` refuses the second registration (not a same-class override — different FQNs) and throws `ConflictingBeanDefinitionException`.

## Verification

Reproduced via `git stash` of Wave 9-F changes on main before fix — same failure on clean main. Confirmed unrelated to later changes.

## Fix Applied (Wave 9-F)

**Option 1 chosen:** explicit bean names on every `OpenApiConfig` across the KiteHub multi-module build.

| File | Change |
|------|--------|
| `kitehub-admin/.../config/OpenApiConfig.java` | `@Configuration("adminOpenApiConfig")` |
| `kitehub-subscription/.../config/OpenApiConfig.java` | `@Configuration("subscriptionOpenApiConfig")` |
| `kitehub-email/.../config/OpenApiConfig.java` | `@Configuration("emailOpenApiConfig")` |
| `kitehub-branding/.../config/OpenApiConfig.java` | `@Configuration("brandingOpenApiConfig")` |

Minimal (annotation-only), no refactor into shared module. `/v3/api-docs` endpoints unchanged — bean name doesn't affect springdoc generation, only DI registration.

**Secondary fixes required** (same root symptom — bringing admin context up):
- `kitehub-admin/src/test/resources/application-test.yml` — added `storage.s3.mock-mode: true`, `webhook.payment.secret`, `encryption.algorithm`, `backup.*` config, and `spring.autoconfigure.exclude: RabbitAutoConfiguration` so the subscription module's transitively-scanned beans all resolve under H2+no-broker.
- `KiteHubAdminApplicationTest` — added `@MockitoBean RabbitTemplate` so `EmailServiceClient` (which unconditionally requires the template via constructor injection, regardless of `use-queue=false`) can wire up without a live broker.

## Acceptance Criteria

- [x] `KiteHubAdminApplicationTest.contextLoads` passes
- [x] No regression in subscription module OpenAPI docs — bean name change is DI-internal, springdoc scanning unaffected
- [x] CI runs this test (Maven default surefire inclusion — verified green locally)

## Out of scope (pre-existing, unrelated)

- `AdminControllerTest` (Testcontainers) — fails with Flyway `V11__create_email_sent_log.sql` on PostgreSQL 15 Alpine. Pre-existing on main (reproduced via stash). Not caused by and not fixed by this PR. Needs separate gap (likely SQL compatibility issue with migration ordering / schema on throwaway container).

## Related

- Surfaced during: PR #375 (Part B perf batch)
- Paired with: GAP-146 (Wave 9-F hotfix cluster)
- Follow-up (out of scope): AdminControllerTest Flyway migration failure on Testcontainers PG15 — needs new gap

## Log

- 2026-04-20 — Gap filed after Wave 8b surfaced the failure during context-load diagnostics.
- 2026-04-21 — **Closed (Wave 9-F).** Fix: explicit bean names on all 4 `OpenApiConfig` classes + admin test profile fleshed out (S3 mock, webhook secret, Rabbit auto-config excluded, `@MockitoBean RabbitTemplate`). `KiteHubAdminApplicationTest.contextLoads` now green. Paired with GAP-146 HTTP-timeout remainder in same PR.
