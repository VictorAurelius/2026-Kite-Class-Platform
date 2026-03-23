# Skill: 2-5 Minute Task Breakdown

**Version:** 1.0 (Superpowers-inspired)
**Last Updated:** 2026-03-13
**Purpose:** Break work into bite-sized tasks for faster feedback, clearer progress, simpler reviews

---

## 📋 Overview

Replace vague, multi-hour tasks with **2-5 minute focused tasks**:
- Each task has exact file path, code sample, verification step
- Easy to estimate, track, and review
- Faster feedback loops (test after each task)
- Clear progress indicators (completed X/Y tasks)

**Target:** +30% planning accuracy (estimated vs actual time)

---

## 🎯 When to Use This Skill

**Use for:**
- ✅ Planning new features (all PRs in implementation-plan.md)
- ✅ Breaking down complex PRs (10+ steps)
- ✅ Clarifying implementation approach
- ✅ Setting expectations for time estimates

**Skip for:**
- ⏭️ Simple bug fixes (1-2 steps obvious)
- ⏭️ Typo corrections (no planning needed)
- ⏭️ Documentation updates (straightforward)

**Default:** Any PR marked Medium+ complexity gets task breakdown

---

## 📝 When to Document Task Breakdown

**Based on Pilot Testing Feedback:**

### Document Full Breakdown (in .md file)

**When:**
- ✅ Features >30 minutes total
- ✅ Multiple files affected (>3 files)
- ✅ Team collaboration (others need to understand plan)
- ✅ Complexity Medium or High
- ✅ New developers on project (learning reference)

**Benefits:**
- Clear roadmap for implementation
- Easy to resume after interruption
- Reviewers understand intent
- Reusable pattern for similar features

**Example:** Pilot PR 1 (35 min, 12 tasks) → Document in `pilot-pr-1-task-breakdown.md`

---

### Verbal/Mental Breakdown Only

**When:**
- ⏭️ Features <10 minutes total
- ⏭️ Single file change (obvious steps)
- ⏭️ Solo work (no collaboration needed)
- ⏭️ Complexity Low (routine task)

**Benefits:**
- No documentation overhead
- Faster for trivial features
- Still use formula mentally (file + change + verify)

**Example:**
```markdown
# Mental breakdown (no doc needed):
1. Add @NotNull to parameter (2 min)
2. Update test (3 min)
3. Run tests (2 min)
Total: 7 min

✅ No need to create task-breakdown.md for this
```

---

### Middle Ground: Inline Breakdown

**When:**
- Features 10-30 minutes
- Modest complexity (3-5 tasks)
- Quick reference needed (not formal doc)

**How:** List tasks in commit message or PR description

**Example:**
```markdown
## Tasks (20 min total)
1. Add validation annotation (3 min)
2. Update DTO (2 min)
3. Add test cases (10 min)
4. Run test suite (5 min)
```

✅ Lightweight documentation, sufficient for small PRs

---

## Decision Matrix: To Document or Not?

| Feature Size | Files Changed | Complexity | Documentation Level |
|--------------|---------------|------------|---------------------|
| <10 min | 1 file | Low | ⏭️ **None** (mental only) |
| 10-30 min | 2-3 files | Low-Medium | 📝 **Inline** (PR description) |
| 30-60 min | 3-5 files | Medium | 📄 **Light Doc** (task list + time) |
| >60 min | 5+ files | Medium-High | 📚 **Full Doc** (with code samples) |

**Rule of Thumb:** If you'd forget the plan after lunch break → Document it

---

## 🎯 Why Break Down Tasks?

### Benefits:

**1. Easier to Estimate** (-50% estimation error)
```
❌ Bad: "Implement student CRUD" → Estimate: "2 hours?" (very uncertain)
✅ Good: 5 tasks × 10 min each → Estimate: "50 min" (high confidence)
```

**2. Faster Feedback Loops** (test after each task)
```
❌ Bad: Code for 2 hours → Test → 50 bugs found → Fix for 1 hour
✅ Good: Code 10 min → Test → 2 bugs found → Fix 2 min → Next task
```

**3. Simpler Code Reviews** (small, focused changes)
```
❌ Bad: 500-line PR with 10 features → Reviewer overwhelmed
✅ Good: 10 PRs × 50 lines each → Easy to review
```

**4. Clear Progress Tracking** (motivation + transparency)
```
❌ Bad: "Working on student feature" (no progress visible)
✅ Good: "Completed 7/10 tasks (70% done)" (clear status)
```

**5. Reduced Context Switching** (one thing at a time)
```
❌ Bad: Jump between entity, service, controller, tests (mental overhead)
✅ Good: Finish entity → Test → Next task (focused)
```

---

## 📐 Task Anatomy (5 Required Elements)

### 1. Exact File Path ✅

**Purpose:** No ambiguity about WHERE to make changes

```markdown
❌ BAD (Vague):
- "Update Student entity"

✅ GOOD (Exact):
- File: `kiteclass-core/src/main/java/com/kiteclass/core/domain/Student.java`
```

### 2. Specific Change Description ✅

**Purpose:** Clear WHAT to change (not just "add feature")

```markdown
❌ BAD (Vague):
- "Add validation"

✅ GOOD (Specific):
- Add @NotBlank validation on name field
- Add @Email validation on email field
- Add @Size(min=2, max=100) on major field
```

### 3. Code Sample (Copy-Paste Ready) ✅

**Purpose:** Show exactly HOW to implement (reduces thinking time)

```java
// ✅ GOOD: Exact code to add

@Entity
@Table(name = "students")
public class Student {
    @NotBlank(message = "STUDENT_NAME_REQUIRED")
    @Size(min = 2, max = 100, message = "STUDENT_NAME_SIZE")
    private String name;

    @Email(message = "STUDENT_EMAIL_INVALID")
    @NotBlank(message = "STUDENT_EMAIL_REQUIRED")
    private String email;

    @Size(min = 2, max = 50, message = "STUDENT_MAJOR_SIZE")
    private String major;
}
```

### 4. Verification Step (How to Test) ✅

**Purpose:** Proof task is done (not just "looks right")

```markdown
✅ GOOD Verification:

**How to verify:**
1. Run: `./mvnw compile`
   - Expected: Compilation successful
2. Check: IntelliJ shows no red underlines
3. Verify: Annotations present in compiled class
   - Open: `target/classes/com/kiteclass/core/domain/Student.class`
   - Confirm: @NotBlank, @Email visible

**Time:** 1 minute
```

### 5. Time Estimate ✅

**Purpose:** Set expectations, track accuracy

```markdown
⏱️ Estimated: 3 minutes
⏱️ Actual: 4 minutes (track for future estimation improvement)
```

---

## 📚 Task Breakdown Examples

### ❌ EXAMPLE: Bad Task Breakdown

```markdown
## PR 2.15: Implement Student CRUD

**Task 1:** Implement student CRUD endpoints
- File: StudentController.java
- Time: 2 hours

**Problems:**
1. ❌ Not broken down (2 hours >> 5 minutes)
2. ❌ No code samples (what does "CRUD" mean exactly?)
3. ❌ No verification steps (how to test?)
4. ❌ Unclear scope (which operations? POST/GET/PUT/DELETE/PATCH?)
5. ❌ No dependencies (entity first? or controller first?)
```

---

### ✅ EXAMPLE: Good Task Breakdown

```markdown
## PR 2.15: Implement Student CRUD

**Total Estimated Time:** 45 minutes (9 tasks)

---

### Task 1: Create Student Entity (3 min)

**File:** `kiteclass-core/src/main/java/com/kiteclass/core/domain/Student.java`

**Change:** Create JPA entity with fields (id, name, email, major, createdAt, deleted, instanceId)

**Code:**
```java
package com.kiteclass.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "students")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "instance_id = :tenantId AND deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "STUDENT_NAME_REQUIRED")
    @Size(min = 2, max = 100, message = "STUDENT_NAME_SIZE")
    private String name;

    @Email(message = "STUDENT_EMAIL_INVALID")
    @NotBlank(message = "STUDENT_EMAIL_REQUIRED")
    private String email;

    @Size(min = 2, max = 50, message = "STUDENT_MAJOR_SIZE")
    private String major;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId; // Multi-tenant
}
```

**Verification:**
```bash
./mvnw compile
# Expected: ✅ Compilation successful

# Check IntelliJ: No red underlines
```

⏱️ **Estimated:** 3 min | **Actual:** ___ min

---

### Task 2: Create StudentRepository (2 min)

**File:** `kiteclass-core/src/main/java/com/kiteclass/core/repository/StudentRepository.java`

**Change:** Extend JpaRepository with custom query methods for multi-tenant support

**Code:**
```java
package com.kiteclass.core.repository;

import com.kiteclass.core.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    // Custom query to respect Hibernate filters
    @Query("SELECT s FROM Student s WHERE s.id = :id AND s.deleted = false")
    Optional<Student> findByIdAndDeletedFalse(@Param("id") UUID id);

    // Check duplicate email
    @Query("SELECT s FROM Student s WHERE s.email = :email AND s.deleted = false")
    Optional<Student> findByEmailAndDeletedFalse(@Param("email") String email);
}
```

**Verification:**
```bash
./mvnw compile
# Expected: ✅ Compilation successful

# IntelliJ auto-complete test:
# Type: studentRepository.findBy...
# Expected: Auto-complete shows findByIdAndDeletedFalse, findByEmailAndDeletedFalse
```

⏱️ **Estimated:** 2 min | **Actual:** ___ min

---

### Task 3: Create StudentService Interface (2 min)

**File:** `kiteclass-core/src/main/java/com/kiteclass/core/service/StudentService.java`

**Change:** Define service method signatures (CRUD operations)

**Code:**
```java
package com.kiteclass.core.service;

import com.kiteclass.core.dto.request.CreateStudentRequest;
import com.kiteclass.core.dto.request.UpdateStudentRequest;
import com.kiteclass.core.dto.response.StudentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StudentService {

    StudentResponse createStudent(@Valid CreateStudentRequest request);

    StudentResponse getStudent(UUID id);

    Page<StudentResponse> getAllStudents(Pageable pageable);

    StudentResponse updateStudent(UUID id, @Valid UpdateStudentRequest request);

    void deleteStudent(UUID id);
}
```

**Verification:**
```bash
./mvnw compile
# Expected: ✅ Interface compiles, no errors
```

⏱️ **Estimated:** 2 min | **Actual:** ___ min

---

### Task 4: Implement createStudent() (5 min)

**File:** `kiteclass-core/src/main/java/com/kiteclass/core/service/impl/StudentServiceImpl.java`

**Change:** Implement create logic with email uniqueness check

**Code:**
```java
@Override
public StudentResponse createStudent(CreateStudentRequest request) {
    // Validate email uniqueness
    if (studentRepository.findByEmailAndDeletedFalse(request.getEmail()).isPresent()) {
        throw new DuplicateResourceException("STUDENT_EMAIL_ALREADY_EXISTS", request.getEmail());
    }

    // Map request to entity
    Student student = Student.builder()
        .name(request.getName())
        .email(request.getEmail())
        .major(request.getMajor())
        .createdAt(LocalDateTime.now())
        .instanceId(TenantContext.getTenantId())
        .build();

    // Save and return
    Student saved = studentRepository.save(student);
    return studentMapper.toResponse(saved);
}
```

**Verification:**
```bash
./mvnw compile
# Expected: ✅ Compiles without errors

# IntelliJ inspection: Check for warnings
# Expected: No yellow/red highlights
```

⏱️ **Estimated:** 5 min | **Actual:** ___ min

---

### Task 5: Write Test for createStudent() (4 min)

**File:** `kiteclass-core/src/test/java/com/kiteclass/core/service/StudentServiceTest.java`

**Change:** Add test for happy path (valid student creation)

**Code:**
```java
@Test
void createStudent_WithValidData_ShouldReturnStudentWithId() {
    // Arrange
    CreateStudentRequest request = new CreateStudentRequest(
        "John Doe",
        "john@example.com",
        "Computer Science"
    );

    // Act
    StudentResponse response = studentService.createStudent(request);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getId()).isNotNull();
    assertThat(response.getName()).isEqualTo("John Doe");
    assertThat(response.getEmail()).isEqualTo("john@example.com");
    assertThat(response.getMajor()).isEqualTo("Computer Science");
}
```

**Verification:**
```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithValidData_ShouldReturnStudentWithId
# Expected: ✅ Test passes (1/1 run, 0 failures)
```

⏱️ **Estimated:** 4 min | **Actual:** ___ min

---

### Task 6: Write Test for Duplicate Email (3 min)

**File:** `kiteclass-core/src/test/java/com/kiteclass/core/service/StudentServiceTest.java`

**Change:** Add test for duplicate email rejection

**Code:**
```java
@Test
void createStudent_WithDuplicateEmail_ShouldThrowException() {
    // Arrange: Create first student
    studentService.createStudent(new CreateStudentRequest("John", "john@test.com", "CS"));

    // Act & Assert: Second student with same email should fail
    CreateStudentRequest duplicate = new CreateStudentRequest("Jane", "john@test.com", "Math");

    assertThatThrownBy(() -> studentService.createStudent(duplicate))
        .isInstanceOf(DuplicateResourceException.class)
        .satisfies(e -> assertThat(e.getMessage())
            .containsIgnoringCase("STUDENT_EMAIL_ALREADY_EXISTS"));
}
```

**Verification:**
```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithDuplicateEmail_ShouldThrowException
# Expected: ✅ Test passes
```

⏱️ **Estimated:** 3 min | **Actual:** ___ min

---

### Task 7: Implement getStudent() (3 min)

... (continue for GET, PUT, DELETE)

---

### Task 8: Write Test for getStudent() (3 min)

...

---

### Task 9: Create Controller Endpoint (5 min)

**File:** `kiteclass-core/src/main/java/com/kiteclass/core/controller/StudentController.java`

**Change:** Add POST /api/v1/students endpoint

**Code:**
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public StudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
    return studentService.createStudent(request);
}
```

**Verification:**
```bash
# Start app
./mvnw spring-boot:run

# Test endpoint
curl -X POST http://localhost:8081/api/v1/students \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $(uuidgen)" \
  -d '{"name":"John","email":"john@test.com","major":"CS"}'

# Expected: 201 CREATED with StudentResponse JSON
```

⏱️ **Estimated:** 5 min | **Actual:** ___ min

---

**Total Actual Time:** ___ min (compare to 45 min estimate)
**Accuracy:** ___% (actual / estimated × 100)
```

---

## 🎯 Task Ordering Principles

### 1. Bottom-Up (Recommended for New Features)

```
Entity → Repository → Service → Controller → Tests
```

**Rationale:**
- Can test each layer immediately
- No "red compilation errors" (dependencies exist)
- Clear progression

**Example:**
```markdown
1. Create Student entity (can compile)
2. Create StudentRepository (entity exists, can compile)
3. Create StudentService (repository exists, can compile)
4. Create StudentController (service exists, can compile)
5. Add tests (everything exists, can test)
```

### 2. Test-First (For TDD Compliance)

```
Test → Entity → Test → Repository → Test → Service → Test → Controller
```

**Rationale:**
- Follows RED-GREEN-REFACTOR
- Each task includes test

**Example:**
```markdown
1. Write failing test for Student entity
2. Create Student entity to pass test
3. Write failing test for StudentRepository
4. Implement StudentRepository to pass test
... (continue)
```

### 3. By Risk (For Bug Fixes)

```
Reproduce Bug → Fix Root Cause → Add Regression Test → Update Docs
```

**Rationale:**
- Prove bug exists first
- Fix immediately
- Prevent recurrence

---

## 📏 Task Granularity Guidelines

### Too Small (<2 min) ❌

```markdown
Task 1: Add import statement (30 sec)
Task 2: Add class annotation (30 sec)
Task 3: Add field (30 sec)
```

**Problem:** Too granular, overhead exceeds value

**Fix:** Combine into "Create entity class (3 min)"

---

### Just Right (2-5 min) ✅

```markdown
Task 1: Create Student entity (3 min)
Task 2: Create StudentRepository (2 min)
Task 3: Implement createStudent() (5 min)
```

**Sweet spot:** Focused, testable, clear progress

---

### Too Large (>10 min) ❌

```markdown
Task 1: Implement full CRUD with tests (2 hours)
```

**Problem:** Too vague, hard to estimate, slow feedback

**Fix:** Break into 10-15 smaller tasks (5 min each)

---

## 📋 Checklist: Before Starting Implementation

Verify your task breakdown has:

- [ ] **Every task is 2-5 min** (max 10 for complex tasks)
- [ ] **Exact file paths** (no ambiguity)
- [ ] **Code samples** (copy-paste ready)
- [ ] **Verification steps** (how to test/verify)
- [ ] **Time estimates** (realistic, not guesses)
- [ ] **Logical order** (dependencies resolved, bottom-up or test-first)
- [ ] **Total time reasonable** (10-20 tasks for Medium PR, 20-40 for High)

**If unclear:** Spend 10 min with `brainstorming-methodology.md` first

---

## 🔗 Integration with Existing Skills

**Use before task breakdown:**
- `brainstorming-methodology.md` - Clarify approach first
- `architecture-overview.md` - Understand file structure
- `api-design.md` - Know endpoint contracts

**Use during task breakdown:**
- `code-style.md` - Reference for code samples
- `testing-guide.md` - Reference for test structure

**Use after task breakdown:**
- `tdd-enforcement.md` - Order tasks for RED-GREEN-REFACTOR
- `implementation-plan.md` - Record task list in PR section

---

## 📏 Success Metrics

**Track per PR:**
- Tasks planned vs tasks actually needed (should be close)
- Estimated time vs actual time per task (improve over time)
- Tasks completed in sequence (no jumping around)

**Track overall:**
- Planning accuracy trend (target: 80%+ by Week 8)
- Task size distribution (most should be 2-5 min)
- Rework rate (target: <10% tasks need redo)

---

## 🎯 Trigger Phrases

Auto-activate this skill when:
- "create implementation plan"
- "break down this PR"
- "how long will this take?"
- "estimate this feature"
- PR marked "Complexity: Medium/High"

---

## ✅ Quick Reference

**Task Anatomy (5 elements):**
1. ✅ Exact file path
2. ✅ Specific change description
3. ✅ Code sample (copy-paste ready)
4. ✅ Verification step (how to test)
5. ✅ Time estimate (2-5 min)

**Task Ordering:**
- Bottom-up: Entity → Repository → Service → Controller
- Test-first: Test → Code → Test → Code (TDD)
- By risk: Reproduce → Fix → Test → Document (bugs)

**Granularity:**
- Too small: <2 min (combine tasks)
- Just right: 2-5 min (sweet spot)
- Too large: >10 min (break down further)

---

**Last Updated:** 2026-03-13
**Author:** Claude Code (Superpowers-inspired)
**Status:** ✅ Active - Use for all Medium+ complexity PRs
