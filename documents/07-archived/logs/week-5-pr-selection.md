# Week 5 - Production PRs Selection

**Date:** 2026-03-13
**Phase:** Rollout Week 5
**Status:** 🎯 SELECTED

---

## Selection Criteria

✅ Build on existing foundation (Student, Teacher, Course entities)
✅ Various complexity levels (Low, Medium)
✅ Demonstrate all 5 Superpowers skills
✅ Low risk (no breaking changes)
✅ Clear acceptance criteria
✅ Real business value (not just pilot exercises)

---

## Week 5 PRs (5 Total)

### PR W5-1: Add Specialization to Teacher Entity ⭐ LOW

**Complexity:** Low
**Estimated Time:** 30 minutes
**Skills Applied:** Quick Brainstorm, Task Breakdown, TDD, Two-Stage Review

**Description:**
Add `specialization` field to Teacher entity to track teacher's subject expertise (e.g., "Mathematics", "Physics", "Chemistry", "English").

**Acceptance Criteria:**
- Teacher entity has `specialization` String field (max 50 chars)
- Validation: non-empty, alphanumeric + spaces only
- Migration script to add column
- API returns specialization in TeacherResponse
- Tests cover validation rules

**Similar to:** Pilot PR 1 (Phone Number) - adding single field

**Brainstorming Decision:**
- Option A: Single String field (simple, MVP) ✅
- Option B: Enum of predefined subjects (restrictive)
- Option C: Many-to-many with Subject entity (over-engineering for MVP)

**Why Low:** Single field, standard validation, no complex logic

---

### PR W5-2: Teacher Search by Specialization ⭐ LOW

**Complexity:** Low
**Estimated Time:** 25 minutes
**Skills Applied:** Quick Brainstorm, Task Breakdown (inline), TDD, Two-Stage Review

**Description:**
Add search endpoint to find teachers by specialization (partial match, case-insensitive).

**Acceptance Criteria:**
- GET `/api/v1/teachers/search?specialization={query}`
- Returns paginated list of teachers
- Case-insensitive, partial match (e.g., "Math" matches "Mathematics")
- Supports sorting and pagination
- Tests cover exact match, partial match, case-insensitive

**Similar to:** Pilot PR 2 (Student Search) - JPQL LIKE query

**Brainstorming Decision:**
- Option A: JPQL LIKE query (simple, standard) ✅
- Option B: PostgreSQL full-text search (overkill for simple search)

**Why Low:** Standard search pattern, already implemented for Student

---

### PR W5-3: Apply Soft Delete to Teacher Entity ⭐⭐ MEDIUM

**Complexity:** Medium
**Estimated Time:** 90 minutes
**Skills Applied:** Full Socratic Brainstorm, Task Breakdown (light doc), TDD, Two-Stage Review

**Description:**
Implement soft delete for Teacher entity with audit trail (deleted_by, deleted_at). Prevent hard deletes, filter deleted teachers from queries automatically.

**Acceptance Criteria:**
- Teacher entity has `deleted`, `deleted_by`, `deleted_at` fields
- `@Where(clause = "deleted = false")` annotation on entity
- DELETE endpoint marks teacher as deleted (not physical delete)
- Audit fields populated (deleted_by from JWT, deleted_at = now)
- Index on `deleted` column for performance
- All repository queries filter deleted=false automatically
- Tests verify soft delete behavior, multi-tenant isolation

**Similar to:** Pilot PR 3 (Soft Delete for Student) - same pattern, different entity

**Brainstorming Decision:**
- Option A: deleted flag + audit columns (simple, standard) ✅
- Option B: Separate deletion_audit table (complex, unnecessary)
- Option C: Event sourcing (over-engineering)

**Why Medium:** Multiple fields, Hibernate filter setup, migration affects existing queries

---

### PR W5-4: Add Prerequisites to Course Entity ⭐⭐ MEDIUM

**Complexity:** Medium
**Estimated Time:** 120 minutes
**Skills Applied:** Full Socratic Brainstorm, Task Breakdown (full doc), TDD, Two-Stage Review

**Description:**
Add course prerequisites feature - courses can require completion of other courses before enrollment. Self-referential many-to-many relationship.

**Acceptance Criteria:**
- Course entity has `@ManyToMany` relationship to itself (prerequisites)
- Join table: `course_prerequisites` (course_id, prerequisite_id)
- API endpoint to add/remove prerequisites
- Validation: prevent circular dependencies (A → B → A)
- Validation: prerequisite must exist and not be deleted
- DTO includes prerequisite list (course IDs + names)
- Tests cover: add prerequisite, circular dependency detection, deleted prerequisite handling

**Brainstorming Decisions:**
- **Storage:** ManyToMany join table vs JSON array?
  - Option A: `@ManyToMany` (relational, queryable) ✅
  - Option B: JSON array of IDs (simple but not queryable)

- **Validation:** How to prevent circular dependencies?
  - Option A: DFS graph traversal (robust) ✅
  - Option B: Max depth limit (simple but may miss cycles)
  - Option C: No validation (risky)

**Why Medium:** Self-referential relationship, circular dependency validation, graph traversal logic

---

### PR W5-5: Course Search by Level and Category ⭐ LOW-MEDIUM

**Complexity:** Low-Medium
**Estimated Time:** 45 minutes
**Skills Applied:** Quick Brainstorm, Task Breakdown (inline), TDD, Two-Stage Review

**Description:**
Add search endpoint to find courses by level (e.g., "Beginner", "Intermediate", "Advanced") and/or category (e.g., "Science", "Math", "Language").

**Acceptance Criteria:**
- GET `/api/v1/courses/search?level={level}&category={category}`
- Both parameters optional (can search by one, both, or neither)
- Returns paginated list of courses
- Supports sorting (by name, level, created date)
- Tests cover: search by level only, by category only, by both, by neither

**Brainstorming Decisions:**
- **Query:** Native SQL vs JPQL?
  - Option A: JPQL with optional parameters ✅
  - Option B: Native SQL (not needed for simple query)

- **Filtering:** How to handle optional parameters?
  - Option A: `@Query` with `(:level IS NULL OR c.level = :level)` ✅
  - Option B: Dynamic query builder (overkill)

**Why Low-Medium:** Multiple optional parameters, but standard JPQL pattern

---

## Implementation Order (Recommended)

**Day 1 (Low complexity warmup):**
1. PR W5-1: Add Specialization to Teacher (~30 min) - Practice Quick Brainstorm
2. PR W5-2: Teacher Search by Specialization (~25 min) - Practice TDD

**Day 2 (Medium complexity):**
3. PR W5-3: Soft Delete for Teacher (~90 min) - Practice Full Brainstorm, already know pattern

**Day 3 (Medium complexity + polish):**
4. PR W5-4: Course Prerequisites (~120 min) - Most complex, full skill demonstration
5. PR W5-5: Course Search (~45 min) - Cool down with familiar pattern

**Total estimated:** ~310 min (~5.2 hours)

---

## Skills Coverage Matrix

| PR | Debugging | Brainstorm | TDD | Review | Task Breakdown |
|----|-----------|------------|-----|--------|----------------|
| W5-1 | - | Quick ✅ | ✅ | ✅ | Inline ✅ |
| W5-2 | - | Quick ✅ | ✅ | ✅ | Inline ✅ |
| W5-3 | - | Full ✅ | ✅ | ✅ | Light ✅ |
| W5-4 | - | Full ✅ | ✅ | ✅ | Full ✅ |
| W5-5 | - | Quick ✅ | ✅ | ✅ | Inline ✅ |

**Note:** Debugging skill not applicable (no bugs in greenfield features)
**Coverage:** 4/5 skills demonstrated across all PRs ✅

---

## Risk Assessment

| PR | Risk Level | Mitigation |
|----|------------|------------|
| W5-1 | LOW | Single field, standard pattern |
| W5-2 | LOW | Already implemented for Student |
| W5-3 | LOW-MEDIUM | Pattern validated in pilot, careful with Hibernate filter |
| W5-4 | MEDIUM | Circular dependency detection critical - thorough testing |
| W5-5 | LOW | Standard query pattern |

**Overall Risk:** LOW ✅ (4 low, 1 medium)

---

## Success Metrics (Week 5 Target)

**Planning Accuracy:**
- Target: ≥85%
- Calculation: `(Estimated Time / Actual Time) * 100`
- Expected: ~88% based on pilot (92%) with adjustment for production reality

**ROI:**
- Target: ≥2.2:1
- Planning investment: ~55 min total (brainstorming + breakdown)
- Expected rework saved: ~120+ min (wrong approaches, review iterations)

**TDD Compliance:**
- Target: ≥70%
- Expected: 100% (all 5 PRs have clear business logic for testing)

**Review Iterations:**
- Target: ≤2 iterations per PR
- Expected: 1-2 (self-review should catch most issues)

---

## Preparation Checklist

Before starting PR W5-1:
- [x] ✅ Week 5 rollout guide reviewed
- [x] ✅ Quick reference cards accessible
- [x] ✅ Metrics tracking template ready
- [x] ✅ PRs selected and prioritized
- [ ] ⏳ Feature branch naming convention confirmed
- [ ] ⏳ TDD hook tested with sample commit

---

## Next Steps

1. **Immediate:** Review this PR selection with team (if applicable)
2. **Today:** Start PR W5-1 (simplest, warmup)
3. **Track:** Document actual vs estimated time for each task
4. **Update:** Metrics tracking file after each PR
5. **Friday:** Calculate Week 5 metrics, create completion report

---

**Status:** ✅ PRs selected and ready for implementation
**Last Updated:** 2026-03-13
**Next:** Implement PR W5-1 (Task #4)
