# GAP-1278: `@WebMvcTest` mocking `AuthorizationBean` breaks — inherited `@PersistenceContext` injected in JPA-less slice

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (test infra)
**Found:** 2026-06-14 (Wave rbac-lms-kc9-staff — discovered while writing GAP-1274 STAFF authz tests)
**Affects:** `kiteclass/kiteclass-core/src/test/**` `@WebMvcTest` classes that mock `AuthorizationBean` (currently `AttendanceClassBatchControllerIT`)

## Problem

`AuthorizationBean` (`@Component("authz")`) gained a `@PersistenceContext EntityManager entityManager` field (Wave 105 / GAP-1165 `hasAccessToSession` + native-query helpers). A `@WebMvcTest` slice has **no JPA / no `EntityManagerFactory`**. When such a slice provides a mock authz bean via `@Bean("authz") AuthorizationBean authz() { return Mockito.mock(AuthorizationBean.class); }`, the Mockito mock is a **subclass of the real `AuthorizationBean`**, so it **inherits the `@PersistenceContext` field**. Spring's `PersistenceAnnotationBeanPostProcessor` runs against the mock and tries to inject an `EntityManager` → `NoSuchBeanDefinitionException: No qualifying bean of type 'jakarta.persistence.EntityManagerFactory'` → context init fails → **all tests in the class error**.

Confirmed pre-existing on `main` (HEAD with PR #2385) by baseline run (`git stash` + run on stashed code): `AttendanceClassBatchControllerIT` → `Tests run: 6, Errors: 6` with `Error creating bean with name 'authz': Injection of persistence dependencies failed`. NOT introduced by this wave (the wave's only change to that controller is a 1-line `@PreAuthorize` STAFF addition, which does not affect bean wiring).

**Not caught by CI:** `core-ci.yml` runs `./mvnw test -P strict-warnings` (surefire = `*Test` only). `*IT` classes (incl. `AttendanceClassBatchControllerIT`) run under failsafe (`mvn verify` / `integration-test`), which CI does not invoke — so this failure is invisible to PR CI and only surfaces in a local `mvn verify`.

## Proposed Fix

Pick one (test-infra, no production change):

1. Convert affected `@WebMvcTest` authz-mocking classes to `@SpringBootTest` + Testcontainers (real JPA-backed authz bean) — mirrors `CrossUserAuthzTest` / the new `StaffRolePreAuthorizeIT` (this wave's workaround). Downside: rewrites `ArgumentCaptor` service-mock verifications in `AttendanceClassBatchControllerIT`.
2. OR keep `@WebMvcTest` but stop the persistence post-processor touching the mock — e.g. `@MockitoBean AuthorizationBean authz` (Spring Boot mock beans are registered specially) OR register a stub `EntityManagerFactory`/`LocalContainerEntityManagerFactoryBean`-less shim.
3. OR add a CI/failsafe step so `*IT` regressions are caught at PR time (separate ops gap).

## Acceptance Criteria

- [ ] Affected `@WebMvcTest` authz-mocking test(s) load context + pass under `mvn verify`
- [ ] Decision recorded on whether CI should run `*IT` (failsafe) to catch this class

## Related

- Discovered in: Wave rbac-lms-kc9-staff (GAP-1274 STAFF authz test work) — `StaffRolePreAuthorizeIT` uses the @SpringBootTest workaround (option 1) and passes 5/5
- Root-cause change: GAP-1165 `AuthorizationBean.hasAccessToSession` (added `@PersistenceContext`)
- GAP-1276 (recent main instability in same file)
