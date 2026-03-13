# PR W5-2: Teacher Search by Specialization - Task Breakdown

**Feature:** Teacher search endpoint by specialization
**Total Time:** 25 minutes
**Documentation Level:** Inline (per decision matrix: <30 min → inline)

---

## Tasks (5 tasks, 25 min)

### Task 1: Add repository search method (3 min)

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/teacher/repository/TeacherRepository.java`

```java
@Query("SELECT t FROM Teacher t WHERE LOWER(t.specialization) LIKE LOWER(CONCAT('%', :specialization, '%')) AND t.deleted = false")
Page<Teacher> searchBySpecialization(@Param("specialization") String specialization, Pageable pageable);
```

**Verify:** Compilation check (no syntax errors)

---

### Task 2: Add service interface method (2 min)

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/teacher/service/TeacherService.java`

```java
/**
 * Search teachers by specialization (partial match, case-insensitive).
 *
 * @param specialization Search query (partial match)
 * @param page Page number (0-indexed)
 * @param size Page size
 * @param sortBy Sort field (e.g., "name", "specialization")
 * @param direction Sort direction ("ASC" or "DESC")
 * @return Paginated teacher results
 */
PageResponse<TeacherResponse> searchBySpecialization(
    String specialization,
    int page,
    int size,
    String sortBy,
    String direction
);
```

**Verify:** Interface compiles

---

### Task 3: Implement service method (4 min)

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/teacher/service/impl/TeacherServiceImpl.java`

```java
@Override
public PageResponse<TeacherResponse> searchBySpecialization(
        String specialization,
        int page,
        int size,
        String sortBy,
        String direction) {

    Sort.Direction sortDirection = Sort.Direction.fromString(direction);
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    Page<Teacher> teachers = teacherRepository.searchBySpecialization(specialization, pageable);

    List<TeacherResponse> content = teachers.getContent().stream()
            .map(teacherMapper::toResponse)
            .toList();

    return new PageResponse<>(
            content,
            teachers.getNumber(),
            teachers.getSize(),
            teachers.getTotalElements(),
            teachers.getTotalPages(),
            teachers.isLast()
    );
}
```

**Verify:** `./mvnw compile`

---

### Task 4: Add controller endpoint (4 min)

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/teacher/controller/TeacherController.java`

```java
@GetMapping("/search")
public ResponseEntity<PageResponse<TeacherResponse>> searchTeachers(
        @RequestParam String specialization,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "ASC") String direction) {

    PageResponse<TeacherResponse> results = teacherService.searchBySpecialization(
            specialization, page, size, sortBy, direction);

    return ResponseEntity.ok(results);
}
```

**Verify:** `./mvnw compile`

---

### Task 5: Add integration tests (12 min)

**File:** `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/teacher/TeacherIntegrationTest.java`

**Test 1: Exact match**
```java
@Test
@DisplayName("GET /api/v1/teachers/search - Should find teacher by exact specialization")
void shouldFindTeacherByExactSpecialization() throws Exception {
    // Given: Create teacher with "Mathematics" specialization
    CreateTeacherRequest request = new CreateTeacherRequest(
        "Math Teacher",
        "math@test.com",
        "0901234567",
        "Mathematics",
        "Bio",
        "Qualification",
        5
    );

    mockMvc.perform(post("/api/v1/teachers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-Id", tenantId.toString())
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // When: Search for "Mathematics"
    mockMvc.perform(get("/api/v1/teachers/search")
            .param("specialization", "Mathematics")
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].specialization").value("Mathematics"));
}
```

**Test 2: Partial match**
```java
@Test
@DisplayName("GET /api/v1/teachers/search - Should find teacher by partial specialization")
void shouldFindTeacherByPartialSpecialization() throws Exception {
    // Given: Create teachers
    createTeacher("Math Teacher 1", "math1@test.com", "Mathematics");
    createTeacher("Physics Teacher", "physics@test.com", "Physics");
    createTeacher("Math Teacher 2", "math2@test.com", "Advanced Mathematics");

    // When: Search for "Math" (should match 2 teachers)
    mockMvc.perform(get("/api/v1/teachers/search")
            .param("specialization", "Math")
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content[*].specialization",
            hasItem(containsString("Math"))));
}
```

**Test 3: Case-insensitive**
```java
@Test
@DisplayName("GET /api/v1/teachers/search - Should be case-insensitive")
void shouldSearchCaseInsensitive() throws Exception {
    // Given: Create teacher
    createTeacher("Teacher", "teacher@test.com", "English Literature");

    // When: Search with different cases
    mockMvc.perform(get("/api/v1/teachers/search")
            .param("specialization", "english")
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].specialization").value("English Literature"));

    mockMvc.perform(get("/api/v1/teachers/search")
            .param("specialization", "ENGLISH")
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].specialization").value("English Literature"));
}
```

**Test 4: Pagination & sorting**
```java
@Test
@DisplayName("GET /api/v1/teachers/search - Should support pagination and sorting")
void shouldSupportPaginationAndSorting() throws Exception {
    // Given: Create 3 teachers
    createTeacher("Alice Math", "alice@test.com", "Mathematics");
    createTeacher("Bob Math", "bob@test.com", "Mathematics");
    createTeacher("Charlie Math", "charlie@test.com", "Mathematics");

    // When: Search with pagination (page 0, size 2, sort by name ASC)
    mockMvc.perform(get("/api/v1/teachers/search")
            .param("specialization", "Math")
            .param("page", "0")
            .param("size", "2")
            .param("sortBy", "name")
            .param("direction", "ASC")
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.totalElements").value(3))
        .andExpect(jsonPath("$.data.content[0].name").value("Alice Math"));
}
```

**Helper method:**
```java
private void createTeacher(String name, String email, String specialization) throws Exception {
    CreateTeacherRequest request = new CreateTeacherRequest(
        name, email, "0901234567", specialization, "Bio", "Qualification", 5
    );
    mockMvc.perform(post("/api/v1/teachers")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-Id", tenantId.toString())
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
}
```

**Verify:** `./mvnw test -Dtest=TeacherIntegrationTest#shouldFind*`

---

## Summary

**Total Tasks:** 5
**Total Time:** 25 minutes
**Average per Task:** 5 minutes

**Breakdown Approach:** Inline (no separate .md file overhead)

**Skills Applied:**
- ✅ Quick Brainstorm (5 min)
- ✅ Task Breakdown (inline, per decision matrix)
- ✅ TDD workflow (tests in Task 5)

---

**Time Spent on Breakdown:** 0 minutes (inline documentation)
**Next:** Implementation with TDD workflow (tests FIRST)
