# GAP-134: JOIN FETCH / @EntityGraph near-absent — N+1 on every collection access

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Database / Performance
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** All services iterating lazy collections in @Transactional scope
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

`grep 'JOIN FETCH|@EntityGraph' **/main/**/*.java` → **1 match** (`ParentStudentLinkRepository`). Meanwhile:
- **164 `@Index`** annotations on entities
- **231 `@Transactional`** methods across 50 files
- Dozens of `@OneToMany` / `@ManyToOne` relationships loaded lazily by default

Every service that iterates `student.getEnrollments()`, `class.getSessions()`, `invoice.getItems()`, `plan.getInstallments()` inside a transaction triggers 1 extra SELECT per parent row. Classic N+1. With 100 students × 5 enrollments = 100 + 500 = 601 queries instead of 2.

`BulkImportChunkExecutor` is at risk; `InstallmentPlanServiceImpl.recordInstallmentPayment` is a documented offender (GAP-128); `GradeServiceImpl` (17 `@Transactional` methods) likely affected; `InvoiceServiceImpl` (11 `@Transactional`) likely affected.

## Context

This gap is BROAD — not a single site, but a pattern. Fixing requires picking the top N hot paths.

## Evidence

- `grep 'JOIN FETCH|@EntityGraph' **/main/**/*.java` → 1 hit
- Performance audit §1

## Proposed Fix

**Phase 1: Triage (1 day)**
1. Enable Hibernate statistics in dev: `spring.jpa.properties.hibernate.generate_statistics: true`.
2. Run E2E test suite with `Statistics.getQueryExecutionCount()` logging.
3. Rank top-20 offenders by query count per operation.

**Phase 2: Fix top 10 (1 week)**
For each offender, choose:
- **@EntityGraph** on repository method (preferred for well-defined fetch strategy):
  ```java
  @EntityGraph(attributePaths = {"enrollments", "enrollments.course"})
  Optional<Student> findWithEnrollmentsById(Long id);
  ```
- **JOIN FETCH** in `@Query` (preferred for ad-hoc queries):
  ```java
  @Query("select s from Student s left join fetch s.enrollments e left join fetch e.course where s.id = :id")
  Optional<Student> findFullById(@Param("id") Long id);
  ```

**Phase 3: Prevention**
- ArchUnit rule: fail build if a `@OneToMany` entity has `fetch = LAZY` AND the service accesses the collection without `@EntityGraph`/JOIN FETCH in the query path.
- Add `datasource-proxy` to test profile, assert query count per integration test.

## Acceptance Criteria

- [ ] Hibernate statistics enabled in dev profile
- [ ] Top-20 N+1 offenders ranked + top-10 fixed with `@EntityGraph` or JOIN FETCH
- [ ] Per-operation SQL count tests added for the 10 fixed paths
- [ ] backend-standards.md has a "N+1 prevention" section

## Related

- Audit: performance-audit-2026-04-19.md §1
- GAP-128 (InstallmentPlan — one specific N+1 offender)
- GAP-126 (admin dashboard — related, but aggregation not collection iteration)

## Log

- 2026-04-19 — Gap created from performance baseline audit
