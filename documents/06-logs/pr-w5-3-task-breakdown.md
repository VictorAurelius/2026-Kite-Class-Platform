# PR W5-3: Soft Delete for Teacher - Task Breakdown

**Feature:** Soft delete with audit trail for Teacher entity
**Total Time:** 90 minutes
**Documentation Level:** Light doc (per decision matrix: >60 min → light/full doc)

---

## Tasks (10 tasks, 90 min)

### Task 1: Audit existing repository methods (10 min)

**File:** `TeacherRepository.java`

**Action:** Check all query methods use `...AndDeletedFalse` suffix

**Current methods to verify:**
- `findByIdAndDeletedFalse` ✅
- `findByEmailAndDeletedFalse` ✅
- `existsByEmailAndDeletedFalse` ✅
- `findByStatusAndDeletedFalse` ✅
- `countByStatusAndDeletedFalse` ✅
- `findBySearchCriteria` - Check WHERE clause has `deleted = false` ✅

**Expected:** All methods already filter deleted records (from previous implementation)

**Verify:** Code review, no changes needed if all have filter

---

### Task 2: Check BaseEntity soft delete setup (5 min)

**File:** `BaseEntity.java`

**Verify:**
- Has `deleted` Boolean field ✅
- Has `deleted_by` UUID field ✅
- Has `deleted_at` LocalDateTime field ✅
- Has `markAsDeleted()` method ✅

**Expected:** All fields already present (Student uses same pattern)

---

### Task 3: Update TeacherServiceImpl delete method (10 min)

**File:** `TeacherServiceImpl.java`

**Current:**
```java
public void deleteTeacher(Long id) {
    Teacher teacher = teacherRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("TEACHER_NOT_FOUND", (Object) id));

    teacher.markAsDeleted();
    teacherRepository.save(teacher);

    log.info("Deleted teacher with ID: {}", id);
}
```

**Check:** Method already uses `markAsDeleted()` (from previous implementation)

**Expected:** No changes needed if already implemented

---

### Task 4: Add soft delete integration tests (40 min)

**File:** `TeacherIntegrationTest.java`

**Test 1: Soft delete marks teacher as deleted (10 min)**
```java
@Test
@DisplayName("DELETE /api/v1/teachers/{id} - Should soft delete teacher")
void shouldSoftDeleteTeacher() throws Exception {
    // Given: Create teacher
    CreateTeacherRequest request = createTeacherRequest("Delete Test", "delete@test.com");
    String createResponse = mockMvc.perform(post("/api/v1/teachers")...)
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long teacherId = extractId(createResponse);

    // When: Delete teacher
    mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isNoContent());

    // Then: Teacher not found in normal queries
    mockMvc.perform(get("/api/v1/teachers/{id}", teacherId)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isNotFound());
}
```

**Test 2: Soft delete not in list queries (10 min)**
```java
@Test
@DisplayName("GET /api/v1/teachers - Should not include soft deleted teachers")
void shouldNotIncludeSoftDeletedInList() throws Exception {
    // Given: Create 2 teachers
    CreateTeacherRequest req1 = createTeacherRequest("Active", "active@test.com");
    CreateTeacherRequest req2 = createTeacherRequest("ToDelete", "todelete@test.com");

    mockMvc.perform(post("/api/v1/teachers")...)
        .andExpect(status().isCreated());

    String deleteResponse = mockMvc.perform(post("/api/v1/teachers")...)
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long deleteId = extractId(deleteResponse);

    // When: Delete second teacher
    mockMvc.perform(delete("/api/v1/teachers/{id}", deleteId)...);

    // Then: List shows only active teacher
    mockMvc.perform(get("/api/v1/teachers")
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].email").value("active@test.com"));
}
```

**Test 3: Soft delete not in search results (10 min)**
```java
@Test
@DisplayName("GET /api/v1/teachers/search - Should not include soft deleted teachers")
void shouldNotIncludeSoftDeletedInSearch() throws Exception {
    // Given: Create teacher with specialization
    CreateTeacherRequest request = createTeacherRequest("Math Teacher", "mathdelete@test.com", "Mathematics");

    String createResponse = mockMvc.perform(post("/api/v1/teachers")...)
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long teacherId = extractId(createResponse);

    // When: Delete teacher
    mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)...);

    // Then: Search doesn't find deleted teacher
    mockMvc.perform(get("/api/v1/teachers/search")
            .param("specialization", "Mathematics")
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
}
```

**Test 4: Cannot delete already deleted teacher (10 min)**
```java
@Test
@DisplayName("DELETE /api/v1/teachers/{id} - Should return 404 for already deleted teacher")
void shouldReturn404ForAlreadyDeletedTeacher() throws Exception {
    // Given: Create and delete teacher
    CreateTeacherRequest request = createTeacherRequest("Delete Twice", "twice@test.com");

    String createResponse = mockMvc.perform(post("/api/v1/teachers")...)
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long teacherId = extractId(createResponse);

    mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)...)
        .andExpect(status().isNoContent());

    // When: Try to delete again
    // Then: Should return 404
    mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TEACHER_NOT_FOUND"));
}
```

**Helper methods:**
```java
private CreateTeacherRequest createTeacherRequest(String name, String email) {
    return new CreateTeacherRequest(
        name, email, "0901234567", "Specialization", "Bio", "Qualification", 5
    );
}

private CreateTeacherRequest createTeacherRequest(String name, String email, String specialization) {
    return new CreateTeacherRequest(
        name, email, "0901234567", specialization, "Bio", "Qualification", 5
    );
}

private Long extractId(String jsonResponse) throws Exception {
    return objectMapper.readTree(jsonResponse).get("data").get("id").asLong();
}
```

**Verify:** `./mvnw test -Dtest=TeacherIntegrationTest#should*Delete*`

---

### Task 5: Verify Hibernate @Where filter (10 min)

**File:** `Teacher.java`

**Check:** Entity has `@Where(clause = "deleted = false")` annotation

**Expected:** May need to add if not present (Student entity has it)

**If not present, add:**
```java
@Entity
@Table(name = "teachers")
@Where(clause = "deleted = false")
public class Teacher extends BaseEntity {
    // ...
}
```

**Verify:** Integration tests pass (filter automatically applies)

---

### Task 6: Test multi-tenant isolation (10 min)

**File:** `TeacherIntegrationTest.java`

**Test: Soft delete respects tenant boundaries**
```java
@Test
@DisplayName("DELETE - Should only delete teacher in current tenant")
void shouldRespectTenantBoundariesOnDelete() throws Exception {
    UUID tenant1 = UUID.randomUUID();
    UUID tenant2 = UUID.randomUUID();

    // Given: Create teacher in tenant1
    CreateTeacherRequest request = createTeacherRequest("Tenant Test", "tenant@test.com");

    String createResponse = mockMvc.perform(post("/api/v1/teachers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-Id", tenant1.toString())
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    Long teacherId = extractId(createResponse);

    // When: Try to delete from tenant2
    // Then: Should return 404 (not found in tenant2)
    mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)
            .header("X-Tenant-Id", tenant2.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TEACHER_NOT_FOUND"));

    // And: Teacher still exists in tenant1
    mockMvc.perform(get("/api/v1/teachers/{id}", teacherId)
            .header("X-Tenant-Id", tenant1.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(teacherId));
}
```

---

### Task 7: Run full test suite (5 min)

**Command:**
```bash
cd kiteclass/kiteclass-core
./mvnw clean test
```

**Expected:** All tests pass (including new soft delete tests)

**If failures:** Debug and fix before proceeding

---

### Task 8: Manual verification (optional, 5 min)

**Steps:**
1. Start dev environment: `docker-compose -f docker-compose.dev.yml up -d`
2. Create teacher via Postman/curl
3. Delete teacher via DELETE endpoint
4. Verify not in GET /teachers list
5. Verify 404 on GET /teachers/{id}

**Expected:** Soft delete works as expected

---

### Task 9: Code review self-check (10 min)

**Checklist:**
- [ ] All repository methods filter deleted=false
- [ ] Service uses markAsDeleted() method
- [ ] Tests cover all scenarios (delete, list, search, multi-tenant)
- [ ] No hardcoded values in tests
- [ ] Proper error handling (404 for deleted teachers)
- [ ] Audit trail fields populated (deleted_by, deleted_at)

---

### Task 10: Two-Stage self-review (10 min)

**Stage 1: Spec Compliance (5 min)**
- Requirements match (soft delete, audit trail)
- Edge cases covered (already deleted, multi-tenant)

**Stage 2: Code Quality (5 min)**
- No critical issues (security, data loss)
- Test coverage adequate
- Code style consistent

---

## Summary

**Total Tasks:** 10
**Total Time:** 90 minutes
**Average per Task:** 9 minutes

**Breakdown Approach:** Light doc (task list + time estimates, minimal code samples)

**Skills Applied:**
- ✅ Full Socratic Brainstorming (20 min) - 3 options analyzed
- ✅ Task Breakdown (light doc, per decision matrix)
- ✅ TDD workflow (tests in Task 4, 6)

---

**Time Spent on Breakdown:** 10 minutes
**Next:** Implementation with TDD workflow (tests FIRST)
