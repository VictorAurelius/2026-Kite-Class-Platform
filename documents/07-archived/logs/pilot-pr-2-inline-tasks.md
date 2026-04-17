# Pilot PR 2: Student Search - Inline Task Breakdown

**Feature:** Student search by name endpoint
**Total Time:** 25 minutes
**Documentation Level:** Inline (per decision matrix: 10-30 min → inline in PR description)

---

## Tasks (5 tasks, 25 min)

### Task 1: Add repository search method (3 min)
**File:** `StudentRepository.java`
```java
@Query("SELECT s FROM Student s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) AND s.deleted = false")
Page<Student> searchByName(@Param("name") String name, Pageable pageable);
```
**Verify:** `./mvnw compile`

---

### Task 2: Add service search method (3 min)
**File:** `StudentServiceImpl.java`
```java
@Override
public Page<StudentResponse> searchByName(String name, int page, int size, String sortBy, String direction) {
    Sort.Direction sortDirection = Sort.Direction.fromString(direction);
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    Page<Student> students = studentRepository.searchByName(name, pageable);
    return students.map(studentMapper::toResponse);
}
```
**Verify:** `./mvnw compile`

---

### Task 3: Add controller endpoint (4 min)
**File:** `StudentController.java`
```java
@GetMapping("/search")
public ResponseEntity<Page<StudentResponse>> searchStudents(
    @RequestParam String name,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "name") String sortBy,
    @RequestParam(defaultValue = "ASC") String direction
) {
    Page<StudentResponse> results = studentService.searchByName(name, page, size, sortBy, direction);
    return ResponseEntity.ok(results);
}
```
**Verify:** `./mvnw compile`

---

### Task 4: Add tests (10 min)
**File:** `StudentServiceTest.java`

**Test 1: Exact match**
```java
@Test
void searchByName_ExactMatch_ShouldReturnStudent() {
    // Arrange
    createStudent("John Doe", "john@test.com");

    // Act
    Page<StudentResponse> results = studentService.searchByName("John Doe", 0, 20, "name", "ASC");

    // Assert
    assertThat(results.getTotalElements()).isEqualTo(1);
    assertThat(results.getContent().get(0).name()).isEqualTo("John Doe");
}
```

**Test 2: Partial match**
```java
@Test
void searchByName_PartialMatch_ShouldReturnMatching() {
    // Arrange
    createStudent("John Doe", "john@test.com");
    createStudent("Jane Doe", "jane@test.com");
    createStudent("Bob Smith", "bob@test.com");

    // Act
    Page<StudentResponse> results = studentService.searchByName("Doe", 0, 20, "name", "ASC");

    // Assert
    assertThat(results.getTotalElements()).isEqualTo(2);
}
```

**Test 3: Case-insensitive**
```java
@Test
void searchByName_CaseInsensitive_ShouldReturnMatching() {
    // Arrange
    createStudent("John Doe", "john@test.com");

    // Act
    Page<StudentResponse> results = studentService.searchByName("john doe", 0, 20, "name", "ASC");

    // Assert
    assertThat(results.getTotalElements()).isEqualTo(1);
}
```

**Verify:** `./mvnw test -Dtest=StudentServiceTest#searchByName*`

---

### Task 5: Integration test & verify (5 min)
**File:** `StudentControllerTest.java`
```java
@Test
void searchStudents_ShouldReturnPaginatedResults() throws Exception {
    // Arrange
    createStudent("John Doe", "john@test.com");

    // Act & Assert
    mockMvc.perform(get("/api/v1/students/search")
            .param("name", "John")
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("John Doe"))
        .andExpect(jsonPath("$.totalElements").value(1));
}
```

**Full Verification:**
```bash
./mvnw test
# Expected: All tests pass ✅
```

---

## Summary

**Total Tasks:** 5
**Total Time:** 25 minutes
**Average per Task:** 5 minutes

**Breakdown Approach:** Inline (no separate .md file needed)
**Justification:** Feature <30 min, low complexity, 5 tasks only

**Skills Applied:**
- ✅ Task Breakdown Formula (mentally)
- ✅ Inline documentation (per refinement: 10-30 min → inline)

**Validation:** Inline breakdown sufficient for small features ✅
- Still has exact file paths
- Still has code samples
- Still has verification steps
- Just no separate .md file (less overhead)

---

**Time Spent on Breakdown:** 0 minutes (inline in PR description)
**Time Saved vs Full Doc:** 10 minutes ✅
