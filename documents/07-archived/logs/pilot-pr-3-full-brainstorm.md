# Pilot PR 3: Soft Delete with Audit Trail - Full Brainstorm

**Date:** 2026-03-13
**Estimated Time:** 90 minutes
**Complexity:** Medium
**Skill Applied:** Full Socratic Brainstorming (20 min version)

---

## Step 1: Question Assumptions (7 min)

### Problem Definition

**Q: What problem are we solving?**
A: Hard delete loses student data permanently. Need audit trail (who deleted, when) and ability to restore students.

**Q: Why is this important NOW?**
A: Compliance requirement (data retention for 1 year). Accidental deletes cause data loss.

**Q: Who is the primary user?**
A: Admin (deletes students), Auditor (reviews deletion history), Support (restores accidentally deleted)

### Success Criteria

**Q: How do we know we succeeded?**
- Student marked as deleted (not physically removed) ✓
- Deletion audit trail recorded (deleted_by, deleted_at) ✓
- Queries automatically filter deleted=false ✓
- Can restore deleted student (future feature) ✓

**Q: What does "done" look like?**
- MVP: Soft delete with audit trail (no restore yet)
- Full feature: Restore capability (Week 4)

### Constraints

**Q: What are the constraints?**
- Performance: Queries must filter deleted=false (index needed)
- Data volume: All historical data retained (1 year)
- Compliance: Audit trail immutable (no editing deleted_by/deleted_at)

---

## Step 2: Explore Trade-offs (10 min)

### Option A: Add deleted flag + audit columns to Student table ⭐ RECOMMENDED

**Pros:**
- ✅ Simple implementation (3 new columns)
- ✅ All data in one table (no joins)
- ✅ Standard soft delete pattern
- ✅ Hibernate @Where annotation for auto-filtering

**Cons:**
- ⚠️ Table grows (includes deleted records)
- ⚠️ All queries must filter deleted=false (if not using @Where)

**Implementation:**
```java
@Entity
@Where(clause = "deleted = false")
public class Student {
    // Existing fields...

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

**Migration:**
```sql
ALTER TABLE students
ADD COLUMN deleted BOOLEAN DEFAULT false,
ADD COLUMN deleted_by UUID,
ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_students_deleted ON students(deleted);
```

---

### Option B: Separate deletion_audit table

**Pros:**
- ✅ Student table stays clean (only active students)
- ✅ Audit data isolated (easier to archive/query)
- ✅ Can track multiple deletion attempts

**Cons:**
- ❌ Requires JOIN to check if student deleted
- ❌ More complex queries
- ❌ Two-table transaction (deletion + audit insert)

**Implementation:**
```java
@Entity
public class StudentDeletionAudit {
    @Id
    private UUID id;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt; // For future restore feature
}
```

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

**Use Case:** Enterprise systems with complex compliance needs

---

## Trade-off Matrix

| Criterion | Weight | Option A (Flag) | Option B (Audit Table) | Option C (Event Sourcing) |
|-----------|--------|-----------------|------------------------|---------------------------|
| **Simplicity** | 30% | 5 (150) | 3 (90) | 1 (30) |
| **Query Performance** | 25% | 5 (125) | 3 (75) | 2 (50) |
| **Audit Richness** | 20% | 3 (60) | 4 (80) | 5 (100) |
| **Development Time** | 15% | 5 (75) | 3 (45) | 1 (15) |
| **Scalability** | 10% | 3 (30) | 4 (40) | 5 (50) |
| **TOTAL** | 100% | **440** ⭐ | **330** | **245** |

**Decision:** Option A (deleted flag + audit columns)

---

## Step 3: Document Decision (3 min)

### Chosen Approach: Soft delete with deleted flag + audit columns

**Summary:**
Add `deleted BOOLEAN`, `deleted_by UUID`, `deleted_at TIMESTAMP` to students table.
Use Hibernate `@Where(clause = "deleted = false")` for automatic filtering.

**Rationale:**
- Simplest implementation (3 columns, standard pattern)
- Best query performance (single table, indexed)
- Sufficient audit trail for MVP (who deleted, when)
- Can extend to separate audit table later if needed

---

### Rejected Alternatives

**1. Separate deletion_audit table**
- Why considered: Cleaner separation of active/deleted data
- Why rejected: Adds complexity (JOINs) without significant benefit for MVP

**2. Event sourcing**
- Why considered: Full audit history, immutable events
- Why rejected: Over-engineering for simple soft delete requirement

---

### Trade-offs Accepted

**What we're giving up:**
- Separate audit table (cleaner) → Using inline columns (simpler)
- Full event history → Only deletion event tracked (sufficient for compliance)

**What we're gaining:**
- Fast implementation (~90 min vs 8+ hours)
- Simple queries (single table, auto-filtering)
- Standard pattern (familiar to developers)

---

### Success Criteria

**Must have:**
- [x] Student marked as deleted (deleted=true)
- [x] Audit trail (deleted_by, deleted_at) recorded
- [x] Queries filter deleted=false automatically (@Where)
- [x] Index on deleted column (performance)
- [x] Tests verify soft delete behavior

**Nice to have (future):**
- [ ] Restore functionality (Week 4)
- [ ] Permanent delete after 1 year (scheduled job)

---

### Implementation Notes

**Affected Files:**
- Entity: `Student.java` (+3 fields, @Where annotation)
- Service: `StudentServiceImpl.java` (update delete method)
- Migration: `V16__add_soft_delete_to_students.sql`
- Tests: Update all repository tests (verify deleted=false filter)

**Risks:**
- Existing queries without @Where may return deleted students
- **Mitigation:** Use @Where on entity (applies globally)

---

### Review Date

**When to revisit:** After 1 year of data (2027-03-13)
**Why:** Evaluate table size, consider archiving strategy if needed

---

**Decision Status:** ✅ Approved - Ready for implementation
**Time Spent on Brainstorming:** 20 minutes
**Skills Applied:** Full Socratic Brainstorming ✅
**Comparison to Quick:** Would have missed trade-off analysis, picked simpler solution without validation
