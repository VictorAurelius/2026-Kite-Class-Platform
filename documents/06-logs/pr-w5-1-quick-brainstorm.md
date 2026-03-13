# PR W5-1: Add Specialization to Teacher - Quick Brainstorm

**Date:** 2026-03-13
**Estimated Time:** 30 minutes
**Complexity:** Low
**Skill Applied:** Quick Brainstorm Template (5 min version)

---

## Problem Statement (1 min)

**What:** Add `specialization` field to Teacher entity to track subject expertise

**Why:** Teachers need to specify their teaching subjects (e.g., "Mathematics", "Physics") for proper course assignment and filtering

**Success:** Teacher entity has validated specialization field, API returns it in responses

---

## Option Comparison (2 min)

### Option A: Single String field (max 50 chars) ⭐

**Pros:**
- ✅ Simple implementation (~20 min)
- ✅ Flexible (any subject name)
- ✅ MVP-ready (validate later if needed)
- ✅ Standard validation (@NotBlank, @Size)

**Cons:**
- ⚠️ No type safety (can enter typos)
- ⚠️ Hard to aggregate by subject (inconsistent naming)

**Time:** 20 minutes (entity + DTO + validation + tests)

---

### Option B: Enum of predefined subjects

**Pros:**
- ✅ Type-safe (only valid subjects)
- ✅ Easy to aggregate
- ✅ Consistent naming

**Cons:**
- ❌ Inflexible (can't add new subjects without code change)
- ❌ Requires predefined list (what if school teaches unique subjects?)
- ❌ More complex (enum migration + handling)

**Time:** 40 minutes (enum definition + migration + tests)

---

### Option C: ManyToMany with Subject entity

**Pros:**
- ✅ Supports multiple specializations per teacher
- ✅ Centralized subject management
- ✅ Queryable relationships

**Cons:**
- ❌ Over-engineering for MVP (YAGNI)
- ❌ Requires Subject entity creation
- ❌ Complex migration + join table

**Time:** 2+ hours (entity + relationship + CRUD + tests)

---

## Decision (2 min)

**Chosen:** Option A (Single String field)

**Rationale:**
MVP needs simple subject tracking. String field sufficient for initial release. Can upgrade to enum/entity later when we have clear subject list from real schools.

**Trade-off Accepted:**
- No type safety → Acceptable for MVP, can add validation later
- Single specialization → Most teachers have primary subject, can extend to array/ManyToMany in future PR if needed

**Review Date:** After 3 months of usage, evaluate if enum/entity needed based on subject name inconsistencies

---

## Implementation Notes

**Field specs:**
- Name: `specialization`
- Type: String
- Max length: 50 chars
- Validation: @NotBlank, @Size(max = 50)
- Example values: "Mathematics", "Physics", "Chemistry", "English Literature"

**Migration:**
```sql
ALTER TABLE teachers
ADD COLUMN specialization VARCHAR(50);
```

**No default value** - existing teachers will have NULL, update manually or via API

---

**Time Spent on Brainstorming:** 5 minutes ✅

**Skills Applied:**
- ✅ Quick Brainstorm Template (vs 20 min Full Socratic)
- **Time Saved:** 15 minutes

**Next:** Task Breakdown (inline, <30 min feature)
