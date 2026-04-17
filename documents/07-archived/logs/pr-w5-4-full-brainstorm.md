# PR W5-4: Course Prerequisites - Full Socratic Brainstorming

**Date:** 2026-03-13
**Estimated Time:** 120 minutes
**Complexity:** Medium
**Skill Applied:** Full Socratic Brainstorming (20 min version)

---

## Step 1: Question Assumptions (7 min)

### Problem Definition

**Q: What problem are we solving?**
A: Courses need prerequisites (e.g., "Algebra 2" requires "Algebra 1"). Students shouldn't enroll in advanced courses without completing foundations.

**Q: Why is this important NOW?**
A: Academic integrity - prevents students from taking courses they're not prepared for. Business logic requirement for enrollment validation (future PR).

**Q: Who is the primary user?**
A: Admin (sets prerequisites), Student (sees prerequisites before enrolling), System (validates enrollment eligibility)

### Success Criteria

**Q: How do we know we succeeded?**
- Course entity has prerequisites relationship ✓
- Can add/remove prerequisites via API ✓
- Prevents circular dependencies (A → B → A) ✓
- DTO includes prerequisite list (IDs + names) ✓
- Tests cover circular dependency detection ✓

**Q: What does "done" look like?**
- MVP: Add/remove prerequisites, prevent circular dependencies
- Future: Enrollment validation (checks prerequisite completion)

### Constraints

**Q: What are the constraints?**
- Performance: Graph traversal for cycle detection (DFS algorithm)
- Data integrity: Prerequisites must exist and not be deleted
- Complexity: Self-referential many-to-many relationship
- Database: Need join table for course_prerequisites

---

## Step 2: Explore Trade-offs (10 min)

### Option A: @ManyToMany self-referential with join table ⭐ RECOMMENDED

**Pros:**
- ✅ Standard JPA pattern for many-to-many
- ✅ Queryable (can find all prerequisites or dependents)
- ✅ Flexible (course can have multiple prerequisites)
- ✅ Database normalized (no redundancy)

**Cons:**
- ⚠️ Self-referential relationship (slightly complex)
- ⚠️ Need custom validation for circular dependencies
- ⚠️ Graph traversal algorithm required (DFS)

**Implementation:**
```java
@Entity
public class Course extends BaseEntity {
    // Existing fields...

    @ManyToMany
    @JoinTable(
        name = "course_prerequisites",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    private Set<Course> prerequisites = new HashSet<>();

    @ManyToMany(mappedBy = "prerequisites")
    private Set<Course> dependentCourses = new HashSet<>();
}
```

**Circular Dependency Detection:**
- Use DFS (Depth-First Search) to traverse prerequisite graph
- Detect cycles before adding new prerequisite
- Complexity: O(V + E) where V = courses, E = prerequisite relationships

**Time:** ~120 minutes (entity + validation + tests)

---

### Option B: JSON array of prerequisite IDs

**Pros:**
- ✅ Simple implementation (single column)
- ✅ No join table needed
- ✅ Fast to add/remove (array manipulation)

**Cons:**
- ❌ Not queryable (can't find "which courses require this course?")
- ❌ No referential integrity (IDs may point to deleted courses)
- ❌ Difficult to validate circular dependencies (need to fetch all courses)
- ❌ Not relational (violates normalization)

**Time:** ~60 minutes (simpler but limited)

---

### Option C: Separate PrerequisiteRelationship entity

**Pros:**
- ✅ Can add metadata (e.g., required_score, completion_date)
- ✅ Audit trail (who added prerequisite, when)
- ✅ Flexible for future requirements

**Cons:**
- ❌ Over-engineering for MVP (no metadata needed yet)
- ❌ More complex queries (extra entity layer)
- ❌ Higher development time

**Time:** ~180 minutes (separate entity + CRUD + tests)

---

## Trade-off Matrix

| Criterion | Weight | Option A (ManyToMany) | Option B (JSON Array) | Option C (Separate Entity) |
|-----------|--------|----------------------|----------------------|----------------------------|
| **Queryability** | 30% | 5 (150) | 1 (30) | 5 (150) |
| **Simplicity** | 25% | 4 (100) | 5 (125) | 2 (50) |
| **Data Integrity** | 20% | 5 (100) | 2 (40) | 5 (100) |
| **Development Time** | 15% | 4 (60) | 5 (75) | 2 (30) |
| **Future Extensibility** | 10% | 3 (30) | 1 (10) | 5 (50) |
| **TOTAL** | 100% | **440** ⭐ | **280** | **380** |

**Decision:** Option A (@ManyToMany self-referential)

---

## Step 3: Document Decision (3 min)

### Chosen Approach: @ManyToMany self-referential relationship

**Summary:**
Use JPA @ManyToMany with self-referential relationship. Create join table `course_prerequisites`. Implement DFS algorithm to detect circular dependencies before adding prerequisites.

**Rationale:**
- Standard JPA pattern (familiar to developers)
- Queryable in both directions (prerequisites and dependent courses)
- Database enforces referential integrity
- Sufficient for MVP, extensible for future needs
- Graph traversal algorithm is straightforward (DFS)

---

### Rejected Alternatives

**1. JSON array of prerequisite IDs**
- Why considered: Simpler implementation
- Why rejected: Not queryable, no referential integrity, violates normalization

**2. Separate PrerequisiteRelationship entity**
- Why considered: Flexible for future metadata
- Why rejected: Over-engineering for MVP, no metadata requirements yet

---

### Trade-offs Accepted

**What we're giving up:**
- Metadata on prerequisites (e.g., required score) → Can add later if needed
- Simplicity of JSON array → Gaining queryability and integrity

**What we're gaining:**
- Queryable relationships (find all courses requiring X)
- Referential integrity (prerequisites must exist)
- Standard pattern (maintainable, testable)
- Circular dependency prevention (DFS validation)

---

### Success Criteria

**Must have:**
- [x] Course entity has prerequisites Set<Course>
- [x] Join table course_prerequisites (course_id, prerequisite_id)
- [x] Add prerequisite endpoint (with circular dependency check)
- [x] Remove prerequisite endpoint
- [x] CourseResponse includes prerequisite list (IDs + names)
- [x] Tests verify circular dependency detection
- [x] Tests verify add/remove prerequisites

**Nice to have (future):**
- [ ] Enrollment validation (check prerequisite completion)
- [ ] Prerequisite tree visualization
- [ ] Bulk add/remove prerequisites

---

### Implementation Notes

**Affected Files:**
- Entity: `Course.java` (+2 fields: prerequisites, dependentCourses)
- Migration: `V11__add_course_prerequisites.sql` (create join table)
- DTO: `CourseResponse.java` (+1 field: prerequisites)
- Service: `CourseService.java` (+2 methods: addPrerequisite, removePrerequisite)
- Validation: New class `PrerequisiteValidator.java` (DFS cycle detection)
- Tests: `CourseIntegrationTest.java` (prerequisite tests)

**Circular Dependency Detection (DFS):**
```java
public boolean hasCycle(Long courseId, Long prerequisiteId) {
    Set<Long> visited = new HashSet<>();
    return dfs(prerequisiteId, courseId, visited);
}

private boolean dfs(Long current, Long target, Set<Long> visited) {
    if (current.equals(target)) return true; // Cycle found
    if (visited.contains(current)) return false; // Already visited

    visited.add(current);
    Course course = courseRepository.findById(current).orElse(null);
    if (course == null) return false;

    for (Course prereq : course.getPrerequisites()) {
        if (dfs(prereq.getId(), target, visited)) return true;
    }

    return false;
}
```

**Risks:**
- DFS performance on deep prerequisite chains (unlikely, courses typically 1-3 levels)
- **Mitigation:** Limit max prerequisite depth (e.g., 5 levels)

**Edge Cases:**
- Course cannot be its own prerequisite (A → A)
- Circular chain (A → B → C → A)
- Deleted course as prerequisite (filter with deleted=false)

---

### Review Date

**When to revisit:** After 100 courses with prerequisites
**Why:** Evaluate DFS performance, consider caching if needed

---

**Decision Status:** ✅ Approved - Ready for implementation
**Time Spent on Brainstorming:** 20 minutes
**Skills Applied:** Full Socratic Brainstorming ✅
**Comparison to Quick:** Quick would miss trade-off analysis, DFS algorithm design, edge case identification
