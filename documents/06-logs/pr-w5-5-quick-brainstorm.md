# PR W5-5: Course Search by Level and Category - Quick Brainstorming

**Date:** 2026-03-13
**Estimated Time:** 45 minutes
**Complexity:** Low-Medium
**Skill Applied:** Quick Brainstorming (5 min version)

---

## Problem Definition

**What:** Add search endpoint to find courses by level and/or category
**Why:** Improve course discoverability for students/admins
**Who:** Students browsing courses, Admins managing curriculum

---

## Decision Points

### 1. Query Type
- ✅ **JPQL with optional parameters** - Type-safe, standard pattern
- ❌ Native SQL - Not needed for simple query
- ❌ QueryDSL - Overkill for this use case

**Chosen:** JPQL with `(:param IS NULL OR c.field = :param)` pattern

### 2. Optional Parameters Handling
- ✅ **NULL check in WHERE clause** - `(:level IS NULL OR c.level = :level)`
- ❌ Dynamic query builder - Too complex
- ❌ Multiple repository methods - Code duplication

**Chosen:** Single method with NULL checks (both params optional)

### 3. Repository Location
- ✅ **Add to existing CourseRepository** - Reuse pagination, consistent pattern
- ❌ Create new SearchRepository - Unnecessary abstraction

**Chosen:** Extend CourseRepository

---

## Implementation Plan (Inline Task Breakdown)

**Total: 45 minutes**

### Phase 1: TDD RED (15 min)
1. **Test 1:** Search by level only → filters courses with matching level
2. **Test 2:** Search by category only → filters courses with matching category
3. **Test 3:** Search by both → filters courses matching both criteria
4. **Test 4:** Search with neither (both null) → returns all courses

### Phase 2: TDD GREEN (25 min)
1. **Repository (5 min):** Add `findByLevelAndCategory()` JPQL query
2. **Service (10 min):** Add `searchCoursesByLevelAndCategory()` method
3. **Controller (5 min):** Add GET `/api/v1/courses/search` endpoint
4. **Run tests (5 min):** Verify all 4 tests pass

### Phase 3: Review (5 min)
- Spec compliance: All acceptance criteria met
- Code quality: No hardcoded values, proper error handling

---

## Acceptance Criteria

- [x] Endpoint: `GET /api/v1/courses/search?level={level}&category={category}`
- [x] Both parameters optional
- [x] Returns paginated CourseResponse list
- [x] Supports sorting (name, level, createdAt)
- [x] Tests cover all combinations (level only, category only, both, neither)

---

## Expected Challenges

**None identified** - This is a standard JPQL query pattern, already used in Teacher search.

---

**Decision Status:** ✅ Approved - Ready for implementation
**Time Spent on Brainstorming:** 5 minutes
**Next:** Inline task breakdown during implementation (TDD workflow)
