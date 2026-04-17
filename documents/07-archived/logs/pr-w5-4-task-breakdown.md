# PR W5-4: Course Prerequisites - Task Breakdown

**Feature:** Course prerequisites with circular dependency detection
**Total Time:** 120 minutes
**Documentation Level:** Full doc (per decision matrix: >60 min, complex logic → full documentation)

---

## Tasks (12 tasks, 120 min)

### Task 1: Create migration for join table (10 min)

**File:** `V11__add_course_prerequisites.sql`

```sql
-- Add course prerequisites join table
CREATE TABLE course_prerequisites (
    course_id BIGINT NOT NULL,
    prerequisite_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, prerequisite_id),
    CONSTRAINT fk_course_prerequisites_course
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_prerequisites_prerequisite
        FOREIGN KEY (prerequisite_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_prerequisite CHECK (course_id != prerequisite_id)
);

-- Indexes for performance
CREATE INDEX idx_course_prerequisites_course ON course_prerequisites(course_id);
CREATE INDEX idx_course_prerequisites_prerequisite ON course_prerequisites(prerequisite_id);

-- Comments
COMMENT ON TABLE course_prerequisites IS 'Many-to-many relationship for course prerequisites';
COMMENT ON CONSTRAINT chk_no_self_prerequisite ON course_prerequisites IS 'Prevents course from being its own prerequisite';
```

**Verify:** SQL syntax, naming conventions

---

### Task 2: Update Course entity with @ManyToMany (10 min)

**File:** `Course.java`

```java
@ManyToMany
@JoinTable(
    name = "course_prerequisites",
    joinColumns = @JoinColumn(name = "course_id"),
    inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
)
private Set<Course> prerequisites = new HashSet<>();

@ManyToMany(mappedBy = "prerequisites")
private Set<Course> dependentCourses = new HashSet<>();

// Helper methods
public void addPrerequisite(Course prerequisite) {
    this.prerequisites.add(prerequisite);
    prerequisite.getDependentCourses().add(this);
}

public void removePrerequisite(Course prerequisite) {
    this.prerequisites.remove(prerequisite);
    prerequisite.getDependentCourses().remove(this);
}
```

**Verify:** `./mvnw compile`

---

### Task 3: Update CourseResponse DTO (5 min)

**File:** `CourseResponse.java`

```java
public record CourseResponse(
    Long id,
    String name,
    String code,
    // ... existing fields ...
    List<PrerequisiteCourseDTO> prerequisites  // NEW
) {
}

// New DTO for prerequisite info
public record PrerequisiteCourseDTO(
    Long id,
    String name,
    String code
) {
}
```

**Verify:** Compilation

---

### Task 4: Update CourseMapper (10 min)

**File:** `CourseMapper.java`

```java
default CourseResponse toResponse(Course course) {
    // ... existing mapping ...

    List<PrerequisiteCourseDTO> prerequisites = course.getPrerequisites().stream()
        .map(prereq -> new PrerequisiteCourseDTO(
            prereq.getId(),
            prereq.getName(),
            prereq.getCode()
        ))
        .sorted(Comparator.comparing(PrerequisiteCourseDTO::name))
        .toList();

    return new CourseResponse(
        // ... existing fields ...
        prerequisites
    );
}
```

**Verify:** `./mvnw compile`

---

### Task 5: Create PrerequisiteValidator with DFS (20 min)

**File:** `PrerequisiteValidator.java` (new)

```java
package com.kiteclass.core.module.course.validator;

import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Validator for course prerequisite relationships.
 * Detects circular dependencies using DFS algorithm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrerequisiteValidator {

    private final CourseRepository courseRepository;

    /**
     * Checks if adding a prerequisite would create a circular dependency.
     *
     * @param courseId ID of course to add prerequisite to
     * @param prerequisiteId ID of prerequisite to add
     * @return true if circular dependency detected, false otherwise
     */
    public boolean wouldCreateCycle(Long courseId, Long prerequisiteId) {
        if (courseId.equals(prerequisiteId)) {
            log.warn("Course {} cannot be its own prerequisite", courseId);
            return true; // Self-prerequisite
        }

        Set<Long> visited = new HashSet<>();
        boolean hasCycle = dfs(prerequisiteId, courseId, visited);

        if (hasCycle) {
            log.warn("Adding prerequisite {} to course {} would create circular dependency",
                prerequisiteId, courseId);
        }

        return hasCycle;
    }

    /**
     * DFS traversal to detect cycles in prerequisite graph.
     *
     * @param current Current node being visited
     * @param target Target node to search for
     * @param visited Set of visited nodes
     * @return true if target found (cycle detected), false otherwise
     */
    private boolean dfs(Long current, Long target, Set<Long> visited) {
        if (current.equals(target)) {
            return true; // Cycle found
        }

        if (visited.contains(current)) {
            return false; // Already visited this path
        }

        visited.add(current);

        Course currentCourse = courseRepository.findByIdAndDeletedFalse(current).orElse(null);
        if (currentCourse == null) {
            return false; // Course not found or deleted
        }

        // Recursively check all prerequisites
        for (Course prereq : currentCourse.getPrerequisites()) {
            if (dfs(prereq.getId(), target, visited)) {
                return true;
            }
        }

        return false;
    }
}
```

**Verify:** `./mvnw compile`
**Test:** Unit test for DFS algorithm

---

### Task 6: Add service methods for prerequisites (15 min)

**File:** `CourseService.java` (interface)

```java
/**
 * Adds a prerequisite to a course.
 *
 * @param courseId ID of course
 * @param prerequisiteId ID of prerequisite to add
 * @throws EntityNotFoundException if course or prerequisite not found
 * @throws ValidationException if adding would create circular dependency
 */
void addPrerequisite(Long courseId, Long prerequisiteId);

/**
 * Removes a prerequisite from a course.
 *
 * @param courseId ID of course
 * @param prerequisiteId ID of prerequisite to remove
 * @throws EntityNotFoundException if course not found
 */
void removePrerequisite(Long courseId, Long prerequisiteId);
```

**File:** `CourseServiceImpl.java`

```java
@Override
public void addPrerequisite(Long courseId, Long prerequisiteId) {
    log.info("Adding prerequisite {} to course {}", prerequisiteId, courseId);

    // Check for circular dependency
    if (prerequisiteValidator.wouldCreateCycle(courseId, prerequisiteId)) {
        throw new ValidationException("COURSE_CIRCULAR_PREREQUISITE",
            courseId, prerequisiteId);
    }

    Course course = courseRepository.findByIdAndDeletedFalse(courseId)
        .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) courseId));

    Course prerequisite = courseRepository.findByIdAndDeletedFalse(prerequisiteId)
        .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) prerequisiteId));

    course.addPrerequisite(prerequisite);
    courseRepository.save(course);

    log.info("Added prerequisite {} to course {}", prerequisiteId, courseId);
}

@Override
public void removePrerequisite(Long courseId, Long prerequisiteId) {
    log.info("Removing prerequisite {} from course {}", prerequisiteId, courseId);

    Course course = courseRepository.findByIdAndDeletedFalse(courseId)
        .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) courseId));

    Course prerequisite = courseRepository.findByIdAndDeletedFalse(prerequisiteId)
        .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) prerequisiteId));

    course.removePrerequisite(prerequisite);
    courseRepository.save(course);

    log.info("Removed prerequisite {} from course {}", prerequisiteId, courseId);
}
```

**Verify:** `./mvnw compile`

---

### Task 7: Add controller endpoints (10 min)

**File:** `CourseController.java`

```java
@PostMapping("/{id}/prerequisites/{prerequisiteId}")
@Operation(summary = "Add prerequisite to course")
public ResponseEntity<ApiResponse<Void>> addPrerequisite(
        @PathVariable Long id,
        @PathVariable Long prerequisiteId) {

    log.info("REST request to add prerequisite {} to course {}", prerequisiteId, id);
    courseService.addPrerequisite(id, prerequisiteId);
    return ResponseEntity.ok(ApiResponse.success(null, "Prerequisite added successfully"));
}

@DeleteMapping("/{id}/prerequisites/{prerequisiteId}")
@Operation(summary = "Remove prerequisite from course")
public ResponseEntity<ApiResponse<Void>> removePrerequisite(
        @PathVariable Long id,
        @PathVariable Long prerequisiteId) {

    log.info("REST request to remove prerequisite {} from course {}", prerequisiteId, id);
    courseService.removePrerequisite(id, prerequisiteId);
    return ResponseEntity.ok(ApiResponse.success(null, "Prerequisite removed successfully"));
}
```

**Verify:** `./mvnw compile`

---

### Task 8: Add i18n error messages (5 min)

**File:** `messages.properties`

```properties
# Course Prerequisites
COURSE_CIRCULAR_PREREQUISITE=Cannot add prerequisite: would create circular dependency (Course {0} -> {1})
```

**File:** `messages_vi.properties`

```properties
# Course Prerequisites
COURSE_CIRCULAR_PREREQUISITE=Không thể thêm điều kiện tiên quyết: tạo vòng lặp phụ thuộc (Khóa học {0} -> {1})
```

**Verify:** Formatting, placeholders

---

### Task 9: Add integration tests (40 min)

**File:** `CourseIntegrationTest.java`

**Test 1: Add prerequisite successfully (8 min)**
**Test 2: Get course includes prerequisites (8 min)**
**Test 3: Prevent self-prerequisite (8 min)**
**Test 4: Prevent circular dependency (A→B, B→A) (8 min)**
**Test 5: Prevent transitive circular dependency (A→B→C, C→A) (8 min)**

(See full test code below)

---

### Task 10: Run full test suite (5 min)

```bash
cd kiteclass/kiteclass-core
./mvnw clean test
```

**Expected:** All tests pass

---

### Task 11: Two-Stage self-review (10 min)

**Stage 1: Spec Compliance**
- Prerequisites relationship works
- Circular dependency detection works
- API endpoints functional

**Stage 2: Code Quality**
- DFS algorithm correct
- Test coverage adequate
- Error handling proper

---

### Task 12: Documentation review (5 min)

- JavaDoc complete
- Migration script documented
- i18n messages added

---

## Full Integration Test Code (Task 9)

```java
// Test 1: Add prerequisite successfully
@Test
@DisplayName("POST /{id}/prerequisites/{prerequisiteId} - Should add prerequisite")
void shouldAddPrerequisite() throws Exception {
    // Given: Create 2 courses
    Long algebra1 = createCourse("Algebra 1", "ALG1");
    Long algebra2 = createCourse("Algebra 2", "ALG2");

    // When: Add Algebra 1 as prerequisite to Algebra 2
    mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", algebra2, algebra1)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk());

    // Then: Algebra 2 response includes Algebra 1 as prerequisite
    mockMvc.perform(get("/api/v1/courses/{id}", algebra2)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.prerequisites[0].id").value(algebra1))
        .andExpect(jsonPath("$.data.prerequisites[0].name").value("Algebra 1"));
}

// Test 3: Prevent self-prerequisite
@Test
@DisplayName("POST /{id}/prerequisites/{id} - Should prevent self-prerequisite")
void shouldPreventSelfPrerequisite() throws Exception {
    // Given: Create course
    Long courseId = createCourse("Math 101", "MATH101");

    // When: Try to add course as its own prerequisite
    // Then: Should return 400 Bad Request
    mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseId, courseId)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COURSE_CIRCULAR_PREREQUISITE"));
}

// Test 4: Prevent circular dependency (A→B, B→A)
@Test
@DisplayName("POST - Should prevent direct circular dependency")
void shouldPreventDirectCircularDependency() throws Exception {
    // Given: Create A→B
    Long courseA = createCourse("Course A", "A");
    Long courseB = createCourse("Course B", "B");

    mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseB, courseA)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk());

    // When: Try to add B→A (would create cycle)
    // Then: Should return 400 Bad Request
    mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseA, courseB)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COURSE_CIRCULAR_PREREQUISITE"));
}

// Test 5: Prevent transitive circular dependency (A→B→C, C→A)
@Test
@DisplayName("POST - Should prevent transitive circular dependency")
void shouldPreventTransitiveCircularDependency() throws Exception {
    // Given: Create A→B→C chain
    Long courseA = createCourse("Course A", "A");
    Long courseB = createCourse("Course B", "B");
    Long courseC = createCourse("Course C", "C");

    mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseB, courseA)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseC, courseB)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isOk());

    // When: Try to add C→A (would create transitive cycle: A→B→C→A)
    // Then: Should return 400 Bad Request
    mockMvc.perform(post("/api/v1/courses/{id}/prerequisites/{prereqId}", courseA, courseC)
            .header("X-Tenant-Id", tenantId.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COURSE_CIRCULAR_PREREQUISITE"));
}

// Helper method
private Long createCourse(String name, String code) throws Exception {
    CreateCourseRequest request = new CreateCourseRequest(
        name, code, "Description", "Beginner", 100
    );

    String response = mockMvc.perform(post("/api/v1/courses")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Tenant-Id", tenantId.toString())
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    return objectMapper.readTree(response).get("data").get("id").asLong();
}
```

---

## Summary

**Total Tasks:** 12
**Total Time:** 120 minutes
**Average per Task:** 10 minutes

**Breakdown Approach:** Full documentation (complex logic, DFS algorithm, multiple files)

**Skills Applied:**
- ✅ Full Socratic Brainstorming (20 min) - 3 options, DFS design
- ✅ Task Breakdown (full doc with code samples)
- ✅ TDD workflow (tests in Task 9)

---

**Time Spent on Breakdown:** 15 minutes
**Next:** Implementation with TDD workflow (tests FIRST, then DFS algorithm + endpoints)
