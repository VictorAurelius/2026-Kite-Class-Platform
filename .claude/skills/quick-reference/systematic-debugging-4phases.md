# Systematic Debugging — 4 Phases Detail

## Phase 1: Reproduce (15-30 min)

**Goal:** Consistently trigger the bug in a controlled environment.

```java
@Test
void testBugReproduction() {
    // Arrange - Setup EXACT conditions
    // Act - Trigger the bug
    // Assert - Verify unexpected behavior
    assertThat(actualResult).isNotEqualTo(expectedResult);
}
```

**Checklist:**
- Run test 3+ times — must fail consistently (flaky = concurrency/timing issue)
- Document: exact steps, system state, environment (Spring Boot ver, DB state, Redis keys)

**Output template:**
```markdown
## Bug Reproduction
**Test:** ServiceTest#testBug  **Consistent:** Yes (5/5 runs)
**Environment:** Spring Boot 3.5.13, PostgreSQL 15, Tenant: 12345
**Steps:** 1) ... 2) ... 3) Expected: X  Actual: Y (WRONG)
```

---

## Phase 2: Trace (30-60 min)

**Goal:** Follow code execution to find divergence point.

**IntelliJ Debugger:** F8 = step over, F7 = step into, Alt+F8 = evaluate expression

**Debug logging:**
```java
log.debug("Entering findById - studentId: {}, tenantId: {}", studentId, TenantContext.getTenantId());
log.debug("After repo query - result: {}, filter enabled: {}", student, filterEnabled);
```

**Stack trace:** Read BOTTOM-UP (oldest → newest), find last known-good method.

**Trace flow:**
```
Request → Controller → Service → Repository → Database
✅ Controller receives correct tenantId
✅ Service extracts tenantId from context
❌ Repository query ignores tenantId filter  ← DIVERGENCE POINT
```

---

## Phase 3: Root Cause (30-45 min)

**Goal:** 5 Whys — distinguish symptom from underlying cause.

```
Symptom: Wrong tenant data returned
Why 1: Hibernate filter not applied
Why 2: JPA findById() used
Why 3: EntityManager.find() bypasses query interceptors
Why 4: Wrong method chosen — assumed findById() respects filters
Why 5: Documentation gap in Spring Data JPA
ROOT CAUSE: findById() bypasses Hibernate filters by design
```

**Validate:** Search `troubleshooting.md`, `MEMORY.md`, `git log --grep="filter"` for similar issues.

---

## Phase 4: Defensive Fix (1-2 hrs)

**Goal:** Fix root cause AND prevent recurrence.

```java
// Fix root cause (not symptom)
// ❌ Band-aid: manually enable filter
// ✅ Correct: use custom query that respects filters
@Query("SELECT s FROM Student s WHERE s.id = :id AND s.deleted = false")
Optional<Student> findByIdAndDeletedFalse(@Param("id") UUID id);
```

**Regression test:**
```java
@Test
void findById_ShouldRespectMultiTenantFilter() {
    UUID tenantA = UUID.randomUUID();
    TenantContext.setTenantId(tenantA);
    Student studentA = repository.save(new Student(...));
    entityManager.flush(); entityManager.clear();

    TenantContext.setTenantId(UUID.randomUUID()); // different tenant
    assertThat(repository.findByIdAndDeletedFalse(studentA.getId())).isEmpty();
}
```

**Update docs:** `troubleshooting.md` → new known issue section. `MEMORY.md` if pattern likely to recur.

---

## KiteClass Examples

### Example 1: Multi-Tenant Filter Bug

**Symptom:** GET /api/students/{id} returns student from wrong tenant

**Phase 1:** Test with two tenants A and B, find student A visible from tenant B context
**Phase 2:** Debug shows TenantContext has correct tenantId, but SQL query missing `AND instance_id = ?`
**Phase 3:** 5 Whys → `findById()` uses EntityManager.find() bypassing Hibernate filters
**Phase 4:** Replace with `findByIdAndDeletedFalse()`, add regression test
**Time saved:** 3h (trial-and-error) → 1.5h (systematic)

### Example 2: Redis Serialization Error

**Symptom:** GET request returns 500 after code change — `SerializationException: @class property required`

**Phase 1:** Consistent failure on cached endpoint
**Phase 2:** Jackson.deserialize() fails — `@class` missing from cached JSON
**Phase 3:** Recent change removed `@JsonTypeInfo` from DTO + Redis not cleared
**Phase 4:** Add `@JsonTypeInfo(use = CLASS, include = PROPERTY)` to cached DTO; update dev-rebuild.sh to auto-clear Redis
**Time saved:** 2h → 45min
