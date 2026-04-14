# ADR-002: Academic Year + Semester Structure

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Architect
**Related Gap:** GAP-053

## Context

Schools + universities vận hành theo năm học (Sep-Jun) với 2 semesters (HK1, HK2). Platform hiện chỉ có Class với startDate/endDate flat — không map được year/semester concept.

Missing capabilities:
- Organize classes by academic year
- Semester-level reports
- Year-end rollover (promotion/retention)
- VN national holidays calendar

## Decision

**Introduce AcademicYear + Semester + Holiday entities as top-level organizing structure.**

```
AcademicYear
├── name: "2026-2027"
├── startDate, endDate
├── status: UPCOMING | CURRENT | COMPLETED
├── semesters: [HK1, HK2, SUMMER]
└── holidays: [VN national + school-specific]

Class / HomeroomClass references academicYear.
```

Pre-populate VN national holidays (1/1, Tết, 30/4, 1/5, 2/9, etc.) via migration seed.

## Consequences

### Positive
- ✅ Semester-aware features unlocked (report cards, semester grades)
- ✅ Year-end rollover support (promotion logic GAP-061)
- ✅ VN holidays built-in
- ✅ Academic calendar UI

### Negative
- ❌ Classes must be academic-year-scoped (data migration)
- ❌ Cross-year data queries more complex

## Alternatives Considered

### Alternative A: Use Class.startDate/endDate only
Pros: Simple
Cons: Can't aggregate "all classes in 2026-2027", no semester concept

**Rejected:** insufficient for K-12

### Alternative B: Separate AcademicCalendar service
Pros: Reusable
Cons: Over-engineering for MVP

**Rejected:** in-service is enough

## Implementation Notes

Migration V28:
```sql
CREATE TABLE academic_years (
  id BIGSERIAL PRIMARY KEY,
  tenant_id UUID NOT NULL,
  name VARCHAR(20),           -- "2026-2027"
  start_date DATE,
  end_date DATE,
  status VARCHAR(20),
  UNIQUE (tenant_id, name)
);

CREATE TABLE semesters (
  id BIGSERIAL PRIMARY KEY,
  academic_year_id BIGINT,
  type VARCHAR(20),           -- HK1, HK2, SUMMER
  name VARCHAR(50),
  start_date DATE,
  end_date DATE,
  exam_start_date DATE,
  exam_end_date DATE
);

CREATE TABLE holidays (
  id BIGSERIAL PRIMARY KEY,
  academic_year_id BIGINT,
  name VARCHAR(100),
  start_date DATE,
  end_date DATE,
  type VARCHAR(20)            -- NATIONAL, SCHOOL
);

-- Seed VN national holidays template
```

Existing classes: nullable academic_year_id, backfill later.

## References

- GAP-053
- Design pattern: Aggregate Root (AcademicYear)

## Log
- 2026-04-14 — Accepted
