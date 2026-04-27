# GAP-242: AdminControllerTest fails Flyway V11 migration in test DB

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (test-only failure; production unaffected; admin module's other tests + subscription full suite green)
**Domain:** Backend / Tests / Database Migration
**Detected:** 2026-04-27 (GAP-240 fix verification surface)

## Current State (verified 2026-04-27)

After GAP-238 + GAP-240 fixes, `KiteHubAdminApplicationTest.contextLoads` passes ✅. But `AdminControllerTest` (7 tests) still fails:

```
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean
with name 'entityManagerFactory' ... Failed to initialize dependency 'flywayInitializer'
of LoadTimeWeaverAware bean 'entityManagerFactory': Error creating bean with name
'flywayInitializer' ... Script V11__create_email_sent_log.sql failed
SQL State  : 42601
```

SQL state 42601 = syntax error. Test embedded DB (likely H2 or in-memory Postgres) doesn't accept some Postgres-specific syntax in V11.

`AdminControllerTest` uses `@SpringBootTest` (full context boot) — triggers Flyway. Subscription module's tests all pass (355/355) because subscription tests configure DB differently (Testcontainers vs embedded H2 likely).

## Problem

Admin's full-context `@SpringBootTest` doesn't have a working test DB strategy:
- Either uses H2 which can't parse Postgres-specific DDL
- Or uses a different config than subscription's tests (which work)
- Or admin module is missing a Testcontainers/test-profile setup

## Proposed Fix

Investigate one of:
1. Adopt subscription module's test DB strategy (likely Testcontainers + Postgres image) — copy the `application-test.yml` + Testcontainers setup
2. Add `@AutoConfigureTestDatabase(replace = NONE)` to use external dev DB
3. Skip Flyway in admin's `@SpringBootTest` via `spring.flyway.enabled=false` if migrations not needed for these tests
4. Use `@DynamicPropertySource` with Postgres Testcontainers per subscription's pattern

Per memory `feedback_jpa_jsonb_jdbctypecode.md` and `feedback_thymeleaf_ognl_pin.md` — admin tests likely need same Postgres-real Testcontainers as subscription.

## Acceptance Criteria

- [ ] All 7 `AdminControllerTest` tests pass with full SpringBootTest context
- [ ] No regression in admin unit tests (15/15) or subscription full suite (355/355)
- [ ] Test DB strategy documented in admin module README (or shared kitehub testing rule)

## Out-of-scope

- Migrating Flyway scripts to be H2-compatible (multi-dialect SQL support not project standard)
- Standardizing test DB strategy across all kitehub modules (broader concern)

## Related

- Parent: GAP-240 (DONE — KiteHubAdminApplicationTest now passes; this is the next layer of issue)
- Sibling: GAP-241 (admin CI coverage — should not enable admin CI tests until this is fixed, else CI fails immediately)
- Migration script: `kitehub/kitehub-subscription/src/main/resources/db/migration/V11__create_email_sent_log.sql`

## Log

- **2026-04-27** — Filed during GAP-240 fix verification. GAP-240 closed JPA scan; AdminControllerTest then surfaced this Flyway V11 issue. Pre-existing test infrastructure gap, not GAP-240 scope.
