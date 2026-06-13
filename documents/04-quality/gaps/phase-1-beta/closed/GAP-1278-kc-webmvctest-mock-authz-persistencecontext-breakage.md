# GAP-1278: `@WebMvcTest` mocking `AuthorizationBean` breaks — inherited `@PersistenceContext` injected in JPA-less slice

**Status:** 🟢 DONE (2026-06-14)
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

- [x] Affected `@WebMvcTest` authz-mocking test(s) load context + pass under `mvn verify`
- [x] Decision recorded on whether CI should run `*IT` (failsafe) to catch this class — see Resolution §Decision (decision recorded; CI-wiring deferred to GAP-1293)

## Resolution (2026-06-14)

### Fix — Proposed Fix option 2 (`@MockitoBean`)

Picked the least-invasive root-cause fix: replaced the
`@TestConfiguration @Bean("authz") @Primary AuthorizationBean authz()` Mockito mock
in `AttendanceClassBatchControllerIT` with a Spring Boot bean-override field
`@MockitoBean(name = "authz") private AuthorizationBean authz;`.

Why this fixes it: a `@Bean`-registered Mockito mock is a subclass of
`AuthorizationBean`, so it inherits the `@PersistenceContext EntityManager` field
(GAP-1165) and goes through the full `createBean → populateBean` lifecycle →
`PersistenceAnnotationBeanPostProcessor.postProcessProperties` fires → tries to
inject an `EntityManager` → `NoSuchBeanDefinitionException: EntityManagerFactory` →
context init fails. A `@MockitoBean` override is registered as a pre-built singleton
that **bypasses property post-processing**, so the persistence post-processor never
touches the mock. No production code changed; no `@SpringBootTest`/Testcontainers
rewrite; the existing `ArgumentCaptor` service-mock verifications are preserved.

(Note: the `@MockitoBean`-is-deprecated comments in `CourseControllerTest` /
`StudentControllerTest` are factually mistaken — `@MockBean`
`org.springframework.boot.test.mock.mockito.MockBean` is the deprecated one;
`@MockitoBean` `org.springframework.test.context.bean.override.mockito.MockitoBean`
is its non-deprecated replacement. Tracked separately as GAP-1294.)

### Verification

- RED baseline (pre-fix, `main`): `Tests run: 6, Failures: 0, Errors: 6`; root cause
  `Error creating bean with name 'authz': Injection of persistence dependencies failed`
  → `NoSuchBeanDefinitionException: ... EntityManagerFactory` via
  `PersistenceAnnotationBeanPostProcessor.postProcessProperties`.
- GREEN (post-fix): `Tests run: 6, Failures: 0, Errors: 0` — both under surefire
  (`-Dtest=` override) AND under failsafe (`failsafe:integration-test` +
  `failsafe:verify` = the `mvn verify` path the gap names) → BUILD SUCCESS.
- No regression: full module surefire suite `Tests run: 1718, Failures: 0, Errors: 0,
  Skipped: 55` → BUILD SUCCESS.

### Decision (AC #2) — CI failsafe coverage for `*IT`

CI (`core-ci.yml`) runs `./mvnw test -P strict-warnings` (surefire = `*Test` only),
so `*IT` failsafe regressions are invisible at PR time — this is exactly why this
bug stayed latent on `main`. **Decision:** CI SHOULD gain a targeted failsafe step
for the non-Docker `@WebMvcTest`-style `*IT` slices (no Testcontainers ⇒ no Docker
runner needed). Full failsafe (Testcontainers ITs) on every PR is rejected for now
(Docker-on-runner cost + queue time). Wiring this is a separate ops concern →
follow-up **GAP-1293**.

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

Bug-class signature: `@WebMvcTest` slice that mocks `AuthorizationBean` via a
`@Bean` (triggers `@PersistenceContext` injection on the inherited field).

```
grep -rln "AuthorizationBean\|@authz" kiteclass/kiteclass-core/src/test --include=*.java
```

| Test | Verdict | Reason |
|---|---|---|
| `AttendanceClassBatchControllerIT` | **FIX** | The affected slice — fixed this PR |
| `ClassControllerRescheduleIT` (`@WebMvcTest`) | EXEMPT | Does NOT enable method security + does NOT mock `AuthorizationBean` (`@PreAuthorize` not enforced in slice per its own javadoc); never resolves `@authz` → no authz bean needed |
| `CrossUserAuthzTest` / `StaffRolePreAuthorizeIT` / `AuthorizationBeanHasAccessToClassIT` | EXEMPT | `@SpringBootTest`/`@DataJpaTest` with real JPA-backed `AuthorizationBean` — no mock, no inherited-field injection problem |

Result: 1 FIX, 0 DEFER, 3 EXEMPT — `AttendanceClassBatchControllerIT` is the sole
affected slice (confirms the gap's "currently `AttendanceClassBatchControllerIT`").

## Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1293: CI thiếu failsafe gate cho `*IT` slices (P2, DevOps) — AC #2 follow-up
- GAP-1294: comment sai "deprecated @MockitoBean" trong `CourseControllerTest` / `StudentControllerTest` (P3, Backend test-doc)

## Related

- Discovered in: Wave rbac-lms-kc9-staff (GAP-1274 STAFF authz test work) — `StaffRolePreAuthorizeIT` uses the @SpringBootTest workaround (option 1) and passes 5/5
- Root-cause change: GAP-1165 `AuthorizationBean.hasAccessToSession` (added `@PersistenceContext`)
- GAP-1276 (recent main instability in same file)
- Fixed in: PR (this) — branch `fix/gap-1278-webmvctest-mock`

## Log

- **2026-06-14 (DONE):** Fixed via Proposed Fix option 2 — `@MockitoBean(name = "authz")` bean-override replaces the `@Bean` Mockito mock in `AttendanceClassBatchControllerIT`, bypassing `PersistenceAnnotationBeanPostProcessor`. RED (6 errors) → GREEN (6 pass) verified under both surefire + failsafe (`mvn verify` path); full module surefire suite 1718/0/0 no regression. Test-infra only, no production change. AC #2 decision recorded → GAP-1293 (CI failsafe gate). Discovery: GAP-1294 (mistaken "deprecated @MockitoBean" comments). Spring Boot 3.5.14 — `@MockitoBean` create-if-absent confirmed working.
