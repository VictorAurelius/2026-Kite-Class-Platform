# ADR-001: K-12 Multi-Subject Data Model

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Architect
**Related Gap:** GAP-054

## Context

Platform hiện có data model `Student → Enrollment → Class → Course (1 môn)` — phù hợp cho trung tâm (P2, P3) nơi 1 học sinh chỉ học 1-2 môn trong 1 lớp.

**K-12 School (P5) realistic:**
- 1 học sinh trong homeroom class "10A1" (30 students)
- Học **đồng thời 12+ môn** (Toán, Văn, Anh, Lý, Hóa...)
- Mỗi môn có teacher riêng, schedule riêng, grade riêng
- Attendance chung cho homeroom + per-subject

Model hiện tại **không support** được K-12 (P5 coverage 30%). Cần refactor nhưng:
- 🔴 **Không được break existing center tenants** (P2, P3)
- 🔴 Production data migration risk

## Decision

**Split Class entity thành HomeroomClass + SubjectSection với Strangler Fig pattern.**

### New Model

```
HomeroomClass (Lớp chính)
├── students[] (30 học sinh)
├── homeroomTeacher (GVCN)
└── academicYear

SubjectSection (Lớp bộ môn)
├── homeroomClass (FK)
├── course/subject
├── teacher
├── schedule
└── grades[]

Student enrollment:
- Student → HomeroomClass (1-1 per academic year)
- Student auto-enrolled in all SubjectSections của that class
```

### Coexistence (Strangler Fig)

Feature flag `TENANT_MODEL` per tenant:
- `CENTER_MODEL`: existing `Enrollment → Class → Course` (no changes)
- `K12_MODEL`: new `Enrollment → HomeroomClass → SubjectSections`

Code paths dual-mode. Over time:
- Phase 1 (now): Add new entities, K-12 tenants opt-in
- Phase 2 (+6 months): Default K-12 for new tenants
- Phase 3 (+12 months): Migrate existing centers (optional)
- Phase 4 (+18 months): Deprecate old model

### Backward Compatibility

- Old tenants: zero changes to code path
- New entities additive (no existing table modifications)
- Feature flag defaults to CENTER_MODEL until explicit opt-in

## Consequences

### Positive
- ✅ K-12 market unlocked (P5 largest persona)
- ✅ Correct domain modeling (DDD Aggregate)
- ✅ No breaking changes to existing tenants
- ✅ Per-subject grading enabled (report card GAP-055)
- ✅ Timetable generation supported

### Negative
- ❌ Dual-mode code complexity (maintenance burden)
- ❌ Testing matrix doubles (center + K12 scenarios)
- ❌ Feature flag infrastructure dependency (GAP-044)
- ❌ Migration plan for existing centers wanting K-12 later

### Neutral
- New migration V29
- 3 new entities (HomeroomClass, SubjectSection, Curriculum, SubjectGrade)
- Admin UI needs mode-aware rendering

## Alternatives Considered

### Alternative A: Extend Class với type field
`Class.type = CENTER | HOMEROOM | SUBJECT_SECTION`

Pros: minimal schema change
Cons: Single table, confused semantics, nullable fields proliferate, hard to query

**Rejected:** anti-pattern (Primitive Obsession at entity level)

### Alternative B: Separate K-12 product entirely
Build kiteschool as separate service

Pros: Clean separation
Cons: Code duplication, separate deployment, business model confused

**Rejected:** over-engineering; SaaS "sân chơi chung" principle (per user)

### Alternative C: Multi-inheritance via @Inheritance
Class superclass with HomeroomClass + SubjectSection subclasses

Pros: OOP-correct
Cons: Hibernate joined-table performance, complex queries, over-abstraction

**Rejected:** simpler composition preferred

## Implementation Notes

### Migration V29

```sql
CREATE TABLE homeroom_classes (
  id BIGSERIAL PRIMARY KEY,
  instance_id UUID NOT NULL,
  academic_year_id BIGINT REFERENCES academic_years(id),
  grade VARCHAR(10) NOT NULL,    -- "10"
  section VARCHAR(10) NOT NULL,  -- "A1"
  homeroom_teacher_id BIGINT REFERENCES teachers(id),
  capacity INT DEFAULT 30,
  ...
);

CREATE TABLE subject_sections (
  id BIGSERIAL PRIMARY KEY,
  homeroom_class_id BIGINT REFERENCES homeroom_classes(id),
  course_id BIGINT REFERENCES courses(id),
  teacher_id BIGINT REFERENCES teachers(id),
  schedule TEXT,
  ...
);

CREATE TABLE curricula (
  id BIGSERIAL PRIMARY KEY,
  grade VARCHAR(10) NOT NULL UNIQUE,
  subjects JSONB  -- list of course + weekly hours + weight
);

CREATE TABLE subject_grades (
  id BIGSERIAL PRIMARY KEY,
  student_id BIGINT,
  subject_section_id BIGINT,
  semester_id BIGINT,
  midterm_score DECIMAL(4,2),
  final_score DECIMAL(4,2),
  regular_score DECIMAL(4,2),
  average DECIMAL(4,2),
  letter_grade VARCHAR(2),
  ...
);
```

### Rollback

If K-12 tenants hit issues:
- Feature flag off → revert to CENTER_MODEL
- Data preserved (additive schema)
- No loss of existing center tenant data (never touched)

### Testing

- Integration test: K-12 flow (30 students × 12 subjects)
- Integration test: Center flow (backward compat)
- Migration test: V29 idempotent, rollback-able

## References

- Design pattern: Strangler Fig (per `ai-branding-design-patterns.md` §2.17)
- Related ADRs: ADR-002 (Academic Year), ADR-003 (Role Hierarchy)
- GAP-054: implementation gap
- Persona: P5 K-12 School (`00-brd/personas-catalog.md`)

## Log

- 2026-04-14 — Proposed + accepted
