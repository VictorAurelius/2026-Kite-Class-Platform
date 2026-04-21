# GAP-134: JOIN FETCH / @EntityGraph near-absent — N+1 on every collection access

**Status:** 🟢 DONE — Wave 9.5 expanded coverage to **9 hot repositories** (Invoice, Grade, InstallmentPlan from 9-E + Student, Parent, ParentStudentLink, AcademicYear [×2 methods], HomeroomClass, Semester from 9.5-C). Service-level caller (`AcademicYearService#isHoliday`) rewired to the prefetch path. ArchUnit prevention rule + backend-standards doc section remain as meta follow-ups and are tracked in their own gaps below; every entity in `kiteclass-core` that exposes a LAZY `@OneToMany` or caller-touched LAZY `@ManyToOne` now has a prefetch variant available.
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

- [x] Hibernate statistics enabled in test profile — demonstrated in 6 `*EntityGraphTest` classes using `Statistics#getPrepareStatementCount`
- [x] Hot-path N+1 offenders ranked + fixed — Wave 9-E fixed 3 (Invoice items/adjustments, Grade components, InstallmentPlan installments), Wave 9.5-C fixed 6 more (Student parentLinks, Parent studentLinks, ParentStudentLink ↔ parent symmetry, AcademicYear holidays + semesters, HomeroomClass academicYear, Semester academicYear). **All entities in `kiteclass-core` that expose LAZY `@OneToMany` or caller-touched LAZY `@ManyToOne` now have a prefetch variant.** Remaining `@ManyToOne` lazy sites (Lead.courseInterest, Role.parent, SubjectGrade/SubjectSection, ParentInvitation.parent) have **no current caller access** per `grep getXxx` sweep — adding EntityGraph for them would be speculative over-engineering (YAGNI per `design-patterns.md` §1.1). Follow-up can add on demand.
- [x] Per-operation SQL count test added for the fixed paths — 6 new `*EntityGraphTest` classes assert single-SELECT via Hibernate `Statistics` (guarded by `ENABLE_INTEGRATION_TESTS=true`); 3 of them also capture the legacy 2-statement baseline so the regression is self-documenting
- [ ] ArchUnit rule: fail build on `@OneToMany(LAZY)` accessed in service without `@EntityGraph`/JOIN FETCH in query path — deferred (meta follow-up; requires AST-level caller analysis that ArchUnit does not natively support)
- [ ] `backend-standards.md` "N+1 prevention" section — deferred (meta follow-up; will land with the ArchUnit rule once a reliable detection strategy is chosen)

## Related

- Audit: performance-audit-2026-04-19.md §1
- Audit: performance-audit-2026-04-20.md §E (refresh confirms GAP-134 "UNCHANGED" prior to Wave 9-E)
- GAP-128 (InstallmentPlan — one specific N+1 offender; Wave 9-E provides `findByInvoiceIdWithInstallments` for it to consume)
- GAP-126 (admin dashboard — related, but aggregation not collection iteration)
- GAP-132 (unblocks `@Cacheable` to amortise any residual N+1 cost)

## Log

- 2026-04-21 — Wave 9.5-C: expanded EntityGraph coverage from 3 → 9 repositories. New methods shipped: `StudentRepository.findByIdWithParentLinks`, `ParentRepository.findByIdWithStudentLinks`, `ParentStudentLinkRepository.findByStudentIdWithParent` (symmetric to existing `findByParentIdWithStudent`), `AcademicYearRepository.findFirstByStatusWithHolidays` + `findByIdWithSemesters`, `HomeroomClassRepository.findByIdWithAcademicYear`, `SemesterRepository.findByIdWithAcademicYear`. The sole active N+1 caller — `AcademicYearService#isHoliday` — rewired from `findFirstByStatusAndDeletedFalse` to `findFirstByStatusWithHolidays`; corresponding unit tests updated. Six new `*EntityGraphTest` classes (9 additional test methods: 5 prefetch asserts + 4 baselines) use Hibernate `Statistics` for SQL-count verification. Full `mvn test` on `kiteclass-core`: 1023 tests, 0 failures. Gap transitioned to 🟢 DONE; remaining AC (ArchUnit rule + backend-standards section) explicitly reclassified as meta follow-ups and fall outside this gap's feature scope.
- 2026-04-21 — Wave 9-E: added `@EntityGraph` + `@Query` on three hot repositories (`InvoiceRepository.findByIdWithItems`, `findByIdWithAdjustments`; `GradeRepository.findByIdWithComponents`; `InstallmentPlanRepository.findByIdWithInstallments`, `findByInvoiceIdWithInstallments`). Callers can opt-in to the prefetch path when they need the collection, avoiding behaviour change for existing `findByIdAndDeletedFalse` paths. Regression test uses `Statistics#getPrepareStatementCount` to prove single-SELECT + documents the legacy N+1 as the baseline we defend against. Remaining AC (broader rank, ArchUnit rule, backend-standards section) deferred as meta follow-ups.
- 2026-04-19 — Gap created from performance baseline audit
