# Skill: Systematic Debugging

**Version:** 1.0 (Superpowers-inspired)
**Last Updated:** 2026-03-13
**Purpose:** 4-phase root cause analysis process for faster, more effective debugging

---

## 📋 Overview

Replace ad-hoc trial-and-error debugging with **systematic 4-phase process** inspired by Superpowers framework:
1. **Reproduce** - Create failing test case
2. **Trace** - Follow execution flow
3. **Root Cause** - Identify underlying issue (not symptom)
4. **Defensive Fix** - Prevent recurrence

**Target:** -50% debugging time (3 hrs → 1.5 hrs average)

---

## 🎯 When to Use This Skill

**Use for:**
- ✅ Bug investigations (any severity)
- ✅ Unexpected behavior in tests
- ✅ Production issues (after reproducing)
- ✅ Integration failures
- ✅ Performance issues

**Skip for:**
- ⏭️ Typos/syntax errors (obvious fix)
- ⏭️ Compiler errors (clear error message)
- ⏭️ Known issues (check troubleshooting.md first)

---

## 🔄 4-Phase Process

### Phase 1: Reproduce (15-30 min)

**Goal:** Consistently trigger the bug in a controlled environment

#### Steps:

1. **Create Failing Test Case**
   ```java
   @Test
   void testBugReproduction() {
       // Arrange - Setup exact conditions

       // Act - Trigger the bug

       // Assert - Verify unexpected behavior
       assertThat(actualResult).isNotEqualTo(expectedResult);
   }
   ```

2. **Document Exact Steps**
   - User actions taken
   - System state (logged in user, data present, etc.)
   - Environment (dev/staging/prod, browser, OS)

3. **Verify Consistency**
   - Run test 3+ times
   - Should fail consistently (not flaky)
   - If flaky → timing/concurrency issue (different approach)

4. **Record Environment Details**
   - Spring Boot version
   - Java version
   - Database state (schema version, data present)
   - Cache state (Redis keys)
   - External services (up/down)

**Output:**
```markdown
## Bug Reproduction

**Test:** StudentServiceTest#testMultiTenantFilterBug
**Consistent:** Yes (fails 5/5 runs)
**Environment:**
- Spring Boot 3.5.11
- PostgreSQL 15
- Tenant ID: 12345
- Redis: Cleared before test

**Steps:**
1. Create student in tenant A
2. Switch to tenant B
3. Call findById() with student ID from tenant A
4. Expected: Empty result
5. Actual: Returns student from tenant A (WRONG!)
```

---

### Phase 2: Trace (30-60 min)

**Goal:** Follow code execution to identify where behavior diverges from expected

#### Tools:

**1. IntelliJ Debugger (Recommended)**
```
- Set breakpoint at entry point
- Step through code (F8 = step over, F7 = step into)
- Watch variables (Add to Watches)
- Evaluate expressions (Alt+F8)
```

**2. Debug Logging**
```java
// Add at key decision points
log.debug("Entering findById - studentId: {}, tenantId: {}",
    studentId, TenantContext.getTenantId());

log.debug("After repository query - result: {}, tenant filter enabled: {}",
    student, hibernateFilterEnabled);
```

**3. Stack Trace Analysis**
```
Read stack trace BOTTOM-UP (oldest → newest)
Identify last known-good method
Find first method where behavior diverges
```

#### Trace Execution Flow:

```
Request Entry → Controller → Service → Repository → Database → Response

Mark each step:
✅ Controller receives correct tenantId (12345)
✅ Service extracts tenantId from context (12345)
❌ Repository query ignores tenantId filter <-- DIVERGENCE POINT
```

**Output:**
```markdown
## Execution Trace

**Flow:**
1. POST /api/students → StudentController.create()
   - Tenant header: 12345 ✅
2. TenantFilter.doFilter()
   - Sets TenantContext: 12345 ✅
3. StudentServiceImpl.findById()
   - Calls repository.findById() ✅
4. StudentRepository.findById()
   - Uses JPA findById() method
   - ❌ ISSUE: Hibernate filter NOT applied to findById()
   - Executes: SELECT * FROM students WHERE id = ?
   - Missing: AND instance_id = '12345'

**Divergence Point:** JPA findById() bypasses Hibernate filters
```

---

### Phase 3: Root Cause (30-45 min)

**Goal:** Distinguish symptom from underlying cause using 5 Whys technique

#### 5 Whys Method:

```
Symptom: Wrong tenant data returned

Why 1: Why was wrong tenant data returned?
→ Because Hibernate filter was not applied

Why 2: Why was Hibernate filter not applied?
→ Because JPA findById() method was used

Why 3: Why does findById() bypass Hibernate filters?
→ Because findById() uses EntityManager.find() which bypasses query interceptors

Why 4: Why didn't we use a custom query method?
→ Because Spring Data JPA findById() was assumed to respect filters

Why 5: Why was this assumption made?
→ Because Hibernate filter documentation doesn't explicitly warn about findById()

ROOT CAUSE: Spring Data JPA findById() bypasses Hibernate filters by design
```

#### Validation:

**Check for Similar Issues:**
```bash
# Search troubleshooting.md
grep -i "hibernate filter" .claude/skills/troubleshooting.md

# Search past issues
git log --grep="filter" --grep="findById"

# Check MEMORY.md
grep -i "findById bypasses" ~/.claude/projects/.../memory/MEMORY.md
```

**Review Recent Changes:**
```bash
# What changed recently in this area?
git log --oneline --since="1 week ago" -- src/main/java/.../*Repository.java

# Any related PRs?
gh pr list --search "multi-tenant filter"
```

**Output:**
```markdown
## Root Cause Analysis

**Symptom:** Multi-tenant filter returns wrong tenant data

**5 Whys Chain:**
1. Filter not applied
2. findById() used
3. EntityManager.find() bypasses interceptors
4. Wrong method chosen
5. Documentation gap

**ROOT CAUSE:**
JPA findById() bypasses Hibernate filters (by design, not a bug)

**Similar Known Issues:**
- MEMORY.md: "Spring Data JPA findById() bypasses Hibernate filters"
- Recommendation: Use custom query methods like findByEmailAndDeletedFalse()

**Recent Changes:** None (existing code, latent bug)

**Why Now:** Bug discovered during PR 2.14 multi-tenant testing
```

---

### Phase 4: Defensive Fix (1-2 hours)

**Goal:** Fix root cause AND prevent similar issues in the future

#### Fix Implementation:

**1. Fix the Root Cause (Not Symptom)**

```java
// ❌ WRONG: Fix symptom (manually enable filter)
public Student findById(UUID id) {
    entityManager.enableFilter("tenantFilter");  // Band-aid!
    return repository.findById(id).orElse(null);
}

// ✅ CORRECT: Fix root cause (use query method that respects filters)
public interface StudentRepository extends JpaRepository<Student, UUID> {
    // Custom query method - Hibernate filters WILL apply
    @Query("SELECT s FROM Student s WHERE s.id = :id AND s.deleted = false")
    Optional<Student> findByIdAndDeletedFalse(@Param("id") UUID id);
}

// Service uses new method
public Student findById(UUID id) {
    return repository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("STUDENT_NOT_FOUND", id));
}
```

**2. Add Regression Test**

```java
@Test
void findById_ShouldRespectMultiTenantFilter() {
    // Arrange: Create student in tenant A
    UUID tenantA = UUID.randomUUID();
    TenantContext.setTenantId(tenantA);
    Student studentA = repository.save(new Student("John", "john@test.com"));
    entityManager.flush();
    entityManager.clear();

    // Act: Switch to tenant B and try to find student A
    UUID tenantB = UUID.randomUUID();
    TenantContext.setTenantId(tenantB);

    // Assert: Should NOT find student from different tenant
    Optional<Student> result = repository.findByIdAndDeletedFalse(studentA.getId());
    assertThat(result).isEmpty();  // ✅ Tenant isolation verified
}
```

**3. Consider Related Scenarios**

```java
// Also fix findAllById() if it exists
@Query("SELECT s FROM Student s WHERE s.id IN :ids AND s.deleted = false")
List<Student> findAllByIdInAndDeletedFalse(@Param("ids") List<UUID> ids);

// Add to other repositories with same issue
public interface TeacherRepository {
    @Query("SELECT t FROM Teacher t WHERE t.id = :id AND t.deleted = false")
    Optional<Teacher> findByIdAndDeletedFalse(@Param("id") UUID id);
}
```

**4. Update Documentation**

```markdown
<!-- Update troubleshooting.md -->

## Multi-tenant Filter Issues

**Problem:** findById() returns wrong tenant data

**Root Cause:** JPA findById() bypasses Hibernate filters

**Solution:** Use custom query methods instead
- Replace: repository.findById(id)
- With: repository.findByIdAndDeletedFalse(id)

**Prevention:** Always use ...AndDeletedFalse suffix for query methods
```

```markdown
<!-- Update MEMORY.md if not already there -->

## Known Issues
- **Spring Data JPA findById() bypasses Hibernate filters**: Use custom query methods
```

**Output:**
```markdown
## Fix Implementation

**Changes:**
1. ✅ Created findByIdAndDeletedFalse() method
2. ✅ Updated service to use new method
3. ✅ Added regression test (testMultiTenantIsolation)
4. ✅ Applied same fix to TeacherRepository
5. ✅ Updated troubleshooting.md

**Regression Test:** StudentServiceTest#findById_ShouldRespectMultiTenantFilter
- Verifies tenant isolation
- Runs in CI (prevents recurrence)

**Related Fixes:** TeacherRepository, CourseRepository (same pattern)

**Documentation:**
- troubleshooting.md: Added "Multi-tenant Filter Issues" section
- MEMORY.md: Already documented (verified)
```

---

## 📊 KiteClass Examples

### Example 1: Multi-Tenant Filter Not Working

**Symptom:** GET /api/students/{id} returns student from different tenant

**Phase 1 - Reproduce:**
```java
@Test
void testBugReproduction() {
    UUID tenantA = UUID.randomUUID();
    TenantContext.setTenantId(tenantA);
    Student student = studentService.create(new CreateStudentRequest("John"));

    TenantContext.setTenantId(UUID.randomUUID()); // Different tenant

    // Bug: This should throw EntityNotFoundException, but returns student
    assertThatThrownBy(() -> studentService.findById(student.getId()))
        .isInstanceOf(EntityNotFoundException.class); // FAILS!
}
```

**Phase 2 - Trace:**
- Debugger shows TenantContext has correct different tenantId
- But query executes without tenant filter: `SELECT * FROM students WHERE id = ?`
- Missing: `AND instance_id = ?`

**Phase 3 - Root Cause:**
- Why? Hibernate filter not applied
- Why? JPA findById() used
- Why? EntityManager.find() bypasses interceptors
- **Root Cause:** Wrong method chosen (Spring Data JPA limitation)

**Phase 4 - Fix:**
```java
// Custom query method
@Query("SELECT s FROM Student s WHERE s.id = :id AND s.deleted = false")
Optional<Student> findByIdAndDeletedFalse(@Param("id") UUID id);

// Regression test
@Test
void findById_ShouldRespectTenantFilter() { ... }

// Update troubleshooting.md
```

**Time Saved:** 3 hours (trial-and-error) → 1.5 hours (systematic)

---

### Example 2: Redis Serialization Error

**Symptom:** GET request returns 500 after code change (StudentService)

**Phase 1 - Reproduce:**
```bash
# Consistent failure
curl http://localhost:8081/api/students/123
# Response: 500 Internal Server Error
# Log: SerializationException: @class property is required
```

**Phase 2 - Trace:**
```
Request → Controller → Service → Cache Hit
                                   ↓
                            Redis.get("student:123")
                                   ↓
                            Jackson.deserialize()
                                   ↓
                         ❌ ERROR: @class missing
```

**Phase 3 - Root Cause:**
- Why? @class property missing from cached JSON
- Why? Jackson config not applied to DTOs
- Why? Recent code change removed StudentResponse from cache
- Why? Developer didn't know Redis serialization requirement
- **Root Cause:** Missing @JsonTypeInfo on cached DTO + Redis not cleared after rebuild

**Phase 4 - Fix:**
```java
// Add to StudentResponse
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public record StudentResponse(UUID id, String name, String email) {}

// Update dev-rebuild.sh to auto-clear Redis
echo "Clearing Redis cache..."
docker exec kiteclass-redis redis-cli FLUSHALL

// Regression test
@Test
void cacheSerializationShouldWork() {
    // Cache student
    studentService.create(...);
    // Clear context
    redisTemplate.delete(...);
    // Retrieve from cache (should deserialize)
    studentService.findById(...); // Should work
}

// Update troubleshooting.md
```

**Time Saved:** 2 hours (guessing) → 45 min (systematic)

---

## 🔗 Integration with Existing Skills

**Before Systematic Debugging:**
1. Check `troubleshooting.md` for known issues
2. Search MEMORY.md for similar problems
3. Review `error-logging.md` for logging best practices

**During Systematic Debugging:**
- **Phase 2 (Trace):** Use `error-logging.md` patterns for debug logs
- **Phase 3 (Root Cause):** Reference `architecture-overview.md` for system design
- **Phase 4 (Fix):** Follow `code-style.md`, `testing-guide.md` for implementation

**After Systematic Debugging:**
- Update `troubleshooting.md` with new known issue (if common)
- Add to MEMORY.md if pattern likely to recur
- Share learnings in weekly retrospective

---

## 📏 Success Metrics

**Track for each debugging session:**
- Time to reproduce (target: <30 min)
- Time to identify root cause (target: <1 hour)
- Time to implement fix (target: <1 hour)
- Total time (target: <2 hours, down from 3 hours baseline)

**Measure overall:**
- % of bugs using systematic process (target: 90%+)
- Average debugging time trend (should decrease over time)
- Recurrence rate (target: <5%, regression tests prevent)

---

## 🎯 Trigger Phrases

Auto-activate this skill when detecting:
- "debug this issue"
- "investigate bug"
- "root cause analysis"
- "systematic debugging"
- "bug in production"
- "unexpected behavior"
- "failing test"

---

## 📚 Additional Resources

**Internal:**
- `.claude/skills/troubleshooting.md` - Known issues & quick fixes
- `.claude/skills/error-logging.md` - Logging patterns
- `MEMORY.md` - Common pitfalls documented

**External:**
- [5 Whys Technique](https://en.wikipedia.org/wiki/Five_whys)
- [Rubber Duck Debugging](https://en.wikipedia.org/wiki/Rubber_duck_debugging)
- IntelliJ IDEA Debugger docs

---

## ✅ Quick Reference Checklist

Before starting debugging, verify:

- [ ] **Phase 1:** Can I consistently reproduce the bug? (test case exists)
- [ ] **Phase 2:** Have I traced execution flow? (debugger or logs)
- [ ] **Phase 3:** Did I identify root cause? (5 Whys applied)
- [ ] **Phase 4:** Did I add regression test? (prevents recurrence)
- [ ] **Phase 4:** Did I update documentation? (troubleshooting.md or MEMORY.md)

**If stuck:** Pair debug with another developer or AI agent (explain out loud)

---

**Last Updated:** 2026-03-13
**Author:** Claude Code (Superpowers-inspired)
**Status:** ✅ Active - Use for all bug investigations
