# PR W5-3: Soft Delete for Teacher - Full Socratic Brainstorming

**Date:** 2026-03-13
**Estimated Time:** 90 minutes
**Complexity:** Medium
**Skill Applied:** Full Socratic Brainstorming (20 min version)

---

## Step 1: Question Assumptions (7 min)

### Problem Definition

**Q: What problem are we solving?**
A: Hard delete loses teacher data permanently. Need audit trail (who deleted, when) and ability to restore teachers if needed.

**Q: Why is this important NOW?**
A: Compliance requirement (data retention for 1 year). Accidental deletes cause data loss. Teachers may be temporarily inactive but need history preserved.

**Q: Who is the primary user?**
A: Admin (deletes teachers), Auditor (reviews deletion history), Support (restores accidentally deleted teachers)

### Success Criteria

**Q: How do we know we succeeded?**
- Teacher marked as deleted (not physically removed) ✓
- Deletion audit trail recorded (deleted_by, deleted_at) ✓
- Queries automatically filter deleted=false ✓
- Can restore deleted teacher (future feature) ✓
- Existing teacher references preserved (course assignments, class history) ✓

**Q: What does "done" look like?**
- MVP: Soft delete with audit trail (no restore yet)
- Full feature: Restore capability (future PR if needed)

### Constraints

**Q: What are the constraints?**
- Performance: Queries must filter deleted=false (Hibernate @Where handles this)
- Data volume: All historical data retained (1 year minimum)
- Compliance: Audit trail immutable (no editing deleted_by/deleted_at)
- Relationships: Teacher may have course/class history that must be preserved

---

## Step 2: Explore Trade-offs (10 min)

### Option A: Add deleted flag + audit columns to Teacher table ⭐ RECOMMENDED

**Pros:**
- ✅ Simple implementation (use BaseEntity fields already defined)
- ✅ All data in one table (no joins)
- ✅ Standard soft delete pattern (consistent with Student entity)
- ✅ Hibernate @Where annotation already on BaseEntity
- ✅ Pattern already validated in Pilot PR 3

**Cons:**
- ⚠️ Table grows (includes deleted records)
- ⚠️ All queries must filter deleted=false (handled by Hibernate filter)

**Implementation:**
Teacher entity already extends BaseEntity which has:
```java
@Column(name = "deleted")
private Boolean deleted = false;

@Column(name = "deleted_by")
private UUID deletedBy;

@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

Just need to:
1. Use `markAsDeleted()` method from BaseEntity
2. Ensure Hibernate filter is active (tenant filter setup)
3. Update repository queries to use `...AndDeletedFalse` suffix

**Time:** ~90 minutes (tests + implementation + verification)

---

### Option B: Separate teacher_deletions audit table

**Pros:**
- ✅ Teacher table stays clean (only active teachers)
- ✅ Audit data isolated (easier to archive/query)
- ✅ Can track multiple deletion/restoration cycles

**Cons:**
- ❌ Requires JOIN to check if teacher deleted
- ❌ More complex queries
- ❌ Two-table transaction (deletion + audit insert)
- ❌ Inconsistent with Student entity pattern

**Time:** ~3 hours (new entity + repository + complex queries)

---

### Option C: Event sourcing (deletion events)

**Pros:**
- ✅ Full audit history (all state changes)
- ✅ Time-travel queries (state at any point)
- ✅ Immutable event log

**Cons:**
- ❌ Over-engineering for simple soft delete
- ❌ Complex infrastructure (event store, projections)
- ❌ High development time (8+ hours)
- ❌ Not needed for MVP

**Use Case:** Enterprise systems with complex compliance needs

---

## Trade-off Matrix

| Criterion | Weight | Option A (BaseEntity) | Option B (Audit Table) | Option C (Event Sourcing) |
|-----------|--------|----------------------|------------------------|---------------------------|
| **Simplicity** | 30% | 5 (150) | 3 (90) | 1 (30) |
| **Consistency** | 25% | 5 (125) | 2 (50) | 3 (75) |
| **Audit Richness** | 20% | 3 (60) | 4 (80) | 5 (100) |
| **Development Time** | 15% | 5 (75) | 3 (45) | 1 (15) |
| **Query Performance** | 10% | 4 (40) | 3 (30) | 2 (20) |
| **TOTAL** | 100% | **450** ⭐ | **295** | **240** |

**Decision:** Option A (use BaseEntity soft delete fields)

---

## Step 3: Document Decision (3 min)

### Chosen Approach: Soft delete using BaseEntity fields

**Summary:**
Use existing `deleted`, `deleted_by`, `deleted_at` fields from BaseEntity. Call `markAsDeleted()` method in service layer. Hibernate @Where filter automatically excludes deleted records from queries.

**Rationale:**
- BaseEntity already has all needed fields (consistent with Student)
- Hibernate filter already configured (multi-tenant setup)
- Simple implementation (just use existing infrastructure)
- Best query performance (single table, indexed)
- Sufficient audit trail for MVP (who deleted, when)
- Can extend to separate audit table later if needed

---

### Rejected Alternatives

**1. Separate teacher_deletions audit table**
- Why considered: Cleaner separation of active/deleted data
- Why rejected: Adds complexity (JOINs) without significant benefit for MVP, inconsistent with Student entity pattern

**2. Event sourcing**
- Why considered: Full audit history, immutable events
- Why rejected: Over-engineering for simple soft delete requirement

---

### Trade-offs Accepted

**What we're giving up:**
- Separate audit table (cleaner) → Using inline columns (simpler)
- Full event history → Only deletion event tracked (sufficient for compliance)

**What we're gaining:**
- Fast implementation (~90 min vs 3+ hours)
- Consistent pattern (same as Student entity)
- Simple queries (single table, auto-filtering)
- Standard pattern (familiar to developers)

---

### Success Criteria

**Must have:**
- [x] Teacher marked as deleted (deleted=true via markAsDeleted())
- [x] Audit trail (deleted_by, deleted_at) recorded
- [x] Queries filter deleted=false automatically (Hibernate @Where)
- [x] Repository methods use `...AndDeletedFalse` suffix
- [x] Tests verify soft delete behavior
- [x] Multi-tenant isolation maintained

**Nice to have (future):**
- [ ] Restore functionality (future PR if needed)
- [ ] Permanent delete after 1 year (scheduled job)

---

### Implementation Notes

**Affected Files:**
- Entity: Teacher.java (already has BaseEntity with soft delete fields) ✅
- Service: TeacherServiceImpl.java (update delete method to use markAsDeleted())
- Repository: TeacherRepository.java (verify all methods use ...AndDeletedFalse)
- Tests: TeacherIntegrationTest.java (add soft delete tests)
- Migration: NOT NEEDED (BaseEntity fields already in schema)

**Existing Implementation Check:**
- BaseEntity has: deleted, deleted_by, deleted_at ✅
- BaseEntity has: markAsDeleted() method ✅
- Hibernate @Where filter: Check if configured ✅
- Repository methods: Check if use ...AndDeletedFalse suffix

**Risks:**
- Existing queries without ...AndDeletedFalse may return deleted teachers
- **Mitigation:** Audit all TeacherRepository methods, ensure consistent suffix usage

---

### Review Date

**When to revisit:** After 1 year of data (2027-03-13)
**Why:** Evaluate table size, consider archiving strategy if needed

---

**Decision Status:** ✅ Approved - Ready for implementation
**Time Spent on Brainstorming:** 20 minutes
**Skills Applied:** Full Socratic Brainstorming ✅
**Comparison to Quick:** Quick would miss trade-off analysis, validation of existing BaseEntity pattern, repository audit needs
