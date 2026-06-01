# GAP-791 — Course list endpoint native query bypasses Hibernate tenant filter (cross-tenant leak)

**Status:** 🔴 OPEN
**Priority:** P0
**Owner:** Backend
**Phase:** phase-1-beta
**Progress:** 0%
**Filed:** 2026-05-28
**Last Updated:** 2026-05-28
**Surfaced by:** Wave A Bucket D RST test walkthrough — `CourseClassCrudOwnerIT.crossTenantIsolation_course` test failed empirically on Postgres Testcontainer.

---

## Problem

`CourseRepository.findBySearchCriteria(...)` uses `nativeQuery = true`. Hibernate `@FilterDef` / `@Filter("tenantFilter")` annotations on `BaseEntity` only apply to **JPQL queries**, NOT native SQL queries. The native query has no `instance_id = :tenantId` predicate.

**Consequence:** any tenant calling `GET /api/v1/courses` sees courses across all tenants. Direct-fetch endpoints (`GET /api/v1/courses/{id}` using `findByIdAndDeletedFalse`) DO respect tenant filter because they use JPQL-derived methods.

This is a **silent cross-tenant data leak** — P0 OWASP A01 Broken Access Control per `pre-launch-owasp-rest-hardening-checklist.md` §2.1.

## Reproduction (deterministic — Testcontainer Postgres)

Bucket D `CourseClassCrudOwnerIT.crossTenantIsolation_course` (this PR's test):

1. Create tenant A course id=4
2. Create tenant B course id=5
3. `GET /api/v1/courses?page=0&size=100` with `X-Tenant-Id: <tenantA>`
4. Response `data.content` array contains BOTH `id=4` (tenant A) AND `id=5` (tenant B)
5. Assertion fires: `"Cross-tenant LEAK detected: tenant A list contains tenant B's courseId=5"`

Counterfactual: direct `GET /api/v1/courses/5` with `X-Tenant-Id: <tenantA>` returns 404 — confirming Hibernate filter is active for JPA-method finders but absent for native query.

## Code locations

- Bug: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/repository/CourseRepository.java:91-113` (`@Query(nativeQuery = true)`)
- Service caller: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/service/impl/CourseServiceImpl.java:159-182` (`getCourses(criteria)`)
- Tenant filter spec: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/entity/BaseEntity.java:43-44` (`@FilterDef` / `@Filter("tenantFilter")` — JPQL-only)

## Cross-flow sweep (per `cross-flow-bug-class-sweep.md` §3)

**Bug class signature:** Repository method declared with `@Query(nativeQuery = true)` that omits `instance_id = :tenantId` predicate → bypasses Hibernate `@Filter` tenant scope.

```bash
grep -rln "nativeQuery = true" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/*/repository/
```

| # | File:line | Verdict | Reason |
|---|---|---|---|
| 1 | `CourseRepository.java:91-113` `findBySearchCriteria` | **FIX (this gap)** | Confirmed leak in test |
| 2 | (other native-query repositories — sweep deferred to fix wave) | **DEFER** | Sweep all `nativeQuery=true` users in same wave with this fix; each needs `AND c.instance_id = :tenantId` predicate OR migration back to JPQL |

Sweep across kiteclass-core repository module deferred to fix wave for completeness (Wave will spawn cross-flow audit + batch fix all sites).

## Proposed Fix

Two viable approaches:

**Option A — Add explicit tenantId predicate to native query (preferred, minimal blast radius)**

```java
@Query(value = """
        SELECT * FROM courses c
        WHERE c.deleted = false
        AND c.instance_id = CAST(:tenantId AS uuid)
        AND (CAST(:search AS text) IS NULL OR ...)
        ...
        """, nativeQuery = true)
Page<Course> findBySearchCriteria(
        @Param("tenantId") UUID tenantId,
        @Param("search") String search,
        ...);
```

Service layer reads from `TenantContext.getCurrentTenantId()` and passes through.

**Option B — Migrate query to JPQL (Spring Data derived OR `@Query` JPQL)**

Slower for complex search but Hibernate filter applies automatically. Trade-off: native query was chosen for performance (Wave 9.5-D `GAP-043` cache stampede protection comment), so migration may regress.

**Recommendation:** Option A.

## Acceptance Criteria

- [ ] `CourseRepository.findBySearchCriteria` native query includes `c.instance_id = :tenantId` predicate
- [ ] Service layer extracts `tenantId` from `TenantContext` and passes through
- [ ] `CourseClassCrudOwnerIT.crossTenantIsolation_course` test PASSES (no `@Disabled`)
- [ ] Cross-flow sweep: all other `nativeQuery = true` repositories in kiteclass-core audited; each either has tenant predicate OR documented exemption
- [ ] Post-fix re-walk per `pre-handoff-self-test-completeness.md` §3 — verify direct GET + list both honor tenant scope

## Future scope

- Add ArchUnit test: every `@Query(nativeQuery = true)` on a tenant-scoped entity (extends BaseEntity with @FilterDef) MUST contain `instance_id` predicate. Detector deferred per `incident-to-rule-pipeline.md` §3.1 premature-rule guard until ≥2 recurrences post-fix.
- Wave-level audit of ALL native queries in repo (kitehub + kiteclass) for tenant predicate completeness.

## References

- Surfaced: Bucket D PR (Wave A) `CourseClassCrudOwnerIT.crossTenantIsolation_course`
- Rule: `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.1 (A01 Broken Access Control)
- Rule: `.claude/rules/cross-flow-bug-class-sweep.md` (sweep methodology applied)
- Sister mechanism: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/entity/BaseEntity.java` (Hibernate `@FilterDef`)
