# Pilot PR 2: Student Search by Name - Quick Brainstorm

**Date:** 2026-03-13
**Estimated Time:** 25 minutes
**Complexity:** Low
**Skill Applied:** Quick Brainstorm Template (5 min version)

---

## Problem Statement (1 min)

**What:** Add search endpoint to find students by name (partial match)

**Why:** Teachers need to quickly find students in large classes (100+ students)

**Success:** Endpoint returns students matching name query, case-insensitive, supports partial match

---

## Option Comparison (2 min)

### Option A: JPQL LIKE query with pagination

**Pros:**
- ✅ Built-in JPA support (standard pattern)
- ✅ Pagination included (Pageable parameter)
- ✅ Simple implementation (~15 min)

**Cons:**
- ⚠️ Case-sensitivity depends on database collation
- ⚠️ No full-text search (basic LIKE only)

**Time:** 15 minutes

**Implementation:**
```java
@Query("SELECT s FROM Student s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
Page<Student> searchByName(@Param("name") String name, Pageable pageable);
```

---

### Option B: Native SQL with PostgreSQL full-text search

**Pros:**
- ✅ Better performance for large datasets
- ✅ Advanced features (ranking, stemming)
- ✅ Case-insensitive by default

**Cons:**
- ❌ Database-specific (not portable)
- ❌ More complex setup (tsvector, indexes)
- ❌ Overkill for simple name search

**Time:** 2 hours (setup + testing)

---

## Decision (2 min)

**Chosen:** Option A (JPQL LIKE query)

**Rationale:**
Simple name search doesn't need full-text search complexity.
JPQL LIKE sufficient for partial matching with case-insensitive LOWER().
Pagination built-in via Spring Data Pageable.

**Trade-off Accepted:**
No advanced search features (ranking, stemming) - acceptable for MVP.
Can upgrade to full-text search later if performance becomes issue (>10k students).

**Review Date:** If search performance degrades with >1000 students per tenant

---

## Quick Implementation Plan (bonus - not in template)

**Tasks (25 min total):**
1. Add repository method (3 min)
2. Add service method (3 min)
3. Add controller endpoint (4 min)
4. Add tests (10 min)
5. Verify (5 min)

**Endpoint Design:**
```
GET /api/v1/students/search?name={query}&page=0&size=20&sort=name,asc
Response: Page<StudentResponse>
```

---

**Time Spent on Brainstorming:** 5 minutes ✅

**Skills Applied:**
- ✅ Quick Brainstorm Template (vs 20 min Full Socratic)
- **Time Saved:** 15 minutes (5 min vs 20 min)

**Decision Quality:** Same outcome as full brainstorming (Option A clearly best for simple search)

**Validation:** Quick template works well for low-complexity features ✅
