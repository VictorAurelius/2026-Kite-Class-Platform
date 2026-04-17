# PR W5-2: Teacher Search by Specialization - Quick Brainstorm

**Date:** 2026-03-13
**Estimated Time:** 25 minutes
**Complexity:** Low
**Skill Applied:** Quick Brainstorm Template (5 min version)

---

## Problem Statement (1 min)

**What:** Add search endpoint to find teachers by specialization (partial match)

**Why:** Admins/students need to quickly find teachers by subject area (e.g., "Math" finds "Mathematics" teachers)

**Success:** GET `/api/v1/teachers/search?specialization={query}` returns paginated results with partial match, case-insensitive

---

## Option Comparison (2 min)

### Option A: JPQL LIKE query with pagination ⭐ RECOMMENDED

**Pros:**
- ✅ Built-in JPA support (standard pattern)
- ✅ Pagination included (Pageable parameter)
- ✅ Simple implementation (~15 min)
- ✅ Already implemented for Student search (proven pattern)

**Cons:**
- ⚠️ Basic LIKE only (no ranking/relevance)
- ⚠️ May be slow with >10k teachers (unlikely for single tenant)

**Time:** 15 minutes

**Implementation:**
```java
@Query("SELECT t FROM Teacher t WHERE LOWER(t.specialization) LIKE LOWER(CONCAT('%', :specialization, '%')) AND t.deleted = false")
Page<Teacher> searchBySpecialization(@Param("specialization") String specialization, Pageable pageable);
```

---

### Option B: PostgreSQL full-text search (tsvector + GIN index)

**Pros:**
- ✅ Better performance for large datasets
- ✅ Advanced features (ranking, stemming, typo tolerance)
- ✅ Case-insensitive by default

**Cons:**
- ❌ Database-specific (not portable to MySQL/H2)
- ❌ More complex setup (alter table, create index, trigger)
- ❌ Overkill for simple specialization search
- ❌ Native SQL (no JPA benefits)

**Time:** 2+ hours (migration + native query + testing)

---

## Decision (2 min)

**Chosen:** Option A (JPQL LIKE query)

**Rationale:**
Simple subject search doesn't need full-text search complexity. JPQL LIKE with LOWER() sufficient for case-insensitive partial matching. Pagination built-in via Spring Data Pageable. Same proven pattern as Student search (PR pilot 2).

**Trade-off Accepted:**
- No advanced search features (ranking, typo tolerance) → Acceptable for MVP
- May need full-text search if >10k teachers or complex search requirements
- Can upgrade later without breaking API contract

**Review Date:** If search performance degrades with >1000 teachers per tenant (unlikely)

---

## Implementation Notes

**Endpoint Design:**
```
GET /api/v1/teachers/search?specialization={query}&page=0&size=20&sort=name,asc
Response: PageResponse<TeacherResponse>
```

**Query Logic:**
- Partial match: "Math" matches "Mathematics", "Math Teacher", etc.
- Case-insensitive: "math" = "MATH" = "Math"
- Deleted teachers filtered automatically (Hibernate @Where clause)
- Multi-tenant isolation automatic (TenantContext filter)

**Edge Cases:**
- Empty/null query → Return empty page (or all teachers if null)
- Special characters in query → LIKE handles naturally
- No matches → Return empty page (not error)

---

**Time Spent on Brainstorming:** 5 minutes ✅

**Skills Applied:**
- ✅ Quick Brainstorm Template (vs 20 min Full Socratic)
- **Time Saved:** 15 minutes (5 min vs 20 min)

**Decision Quality:** Same outcome as pilot PR 2 (JPQL clearly best for simple search) ✅

**Next:** Task Breakdown (inline)
