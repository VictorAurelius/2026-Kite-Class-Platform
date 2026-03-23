# Skill: Two-Stage Code Review

**Version:** 1.0 (Superpowers-inspired)
**Last Updated:** 2026-03-13
**Purpose:** Systematic code review with spec compliance before code quality

---

## 📋 Overview

Replace single-stage "check everything at once" review with **2-stage systematic process**:
1. **Stage 1: Specification Compliance** - Does it do what was asked? (BLOCKING)
2. **Stage 2: Code Quality** - Is the code clean & maintainable? (GRADED)

**Target:** -25% code review iterations (2.5 → 2 per PR), catch spec issues earlier

---

## 🎯 When to Use This Skill

**Mandatory for:**
- ✅ All PRs before merging to main
- ✅ Feature implementations (any size)
- ✅ Bug fixes with code changes
- ✅ Refactoring PRs

**Skip for:**
- ⏭️ Documentation-only PRs (no code review needed)
- ⏭️ Configuration-only changes (quick sanity check)

**Default:** Every PR gets 2-stage review (Stage 1 → PASS → Stage 2)

---

## 🔄 Two-Stage Process

### Stage 1: Specification Compliance (15-20 min) 🔴 BLOCKING

**Goal:** Verify PR matches requirements EXACTLY before looking at code quality

**Question:** Does this PR do what was asked?

#### Checklist:

**1. Requirements Match**
```markdown
- [ ] Matches PR description exactly
- [ ] Implements all acceptance criteria from plan
- [ ] No missing features (incomplete implementation)
- [ ] No extra features (scope creep)
```

**Example - PR 2.14 Invoice Payment:**
```markdown
## Stage 1: Specification Compliance

**Requirements from implementation-plan.md:**
1. Filter unpaid invoices
2. Filter overdue invoices
3. Mark invoice as paid

**Verification:**
- [x] Requirement 1: `findUnpaidInvoices()` exists ✅
- [x] Requirement 2: `findOverdueInvoices()` exists ✅
- [ ] Requirement 3: `markAsPaid()` exists ❌ MISSING

**Outcome:** ❌ FAIL Stage 1 - Missing requirement 3
**Action:** BLOCK - Return to developer for implementation
```

**2. Edge Cases Coverage**
```markdown
- [ ] Handles null/empty inputs
- [ ] Handles invalid data (validation)
- [ ] Handles errors gracefully
- [ ] Multi-tenant isolation (if applicable)
```

**Example:**
```java
// ✅ PASS: Edge cases covered
@Test
void createStudent_WithNullName_ShouldThrowValidationException() { ... }

@Test
void createStudent_WithEmptyEmail_ShouldThrowValidationException() { ... }

@Test
void createStudent_FromDifferentTenant_ShouldNotSeeStudent() { ... }

// ❌ FAIL: Missing edge case
// No test for duplicate email!
```

**3. File Paths Match Plan**
```markdown
- [ ] Code files in correct locations (per implementation-plan.md)
- [ ] Test files in corresponding test directories
- [ ] No unexpected file changes
```

**Example:**
```markdown
**Expected from plan:**
- `kiteclass-core/src/main/java/.../StudentService.java` ✅
- `kiteclass-core/src/test/java/.../StudentServiceTest.java` ✅

**Actual:**
- `kiteclass-core/src/main/java/.../StudentService.java` ✅
- `kiteclass-gateway/src/test/...` ❌ WRONG LOCATION

**Outcome:** ❌ FAIL - Test in wrong service
```

**4. API Contracts Match Design**
```markdown
- [ ] Request/Response DTOs match api-design.md
- [ ] HTTP status codes correct (200, 201, 400, 404, etc.)
- [ ] Endpoint paths follow conventions
```

**Example:**
```java
// ✅ PASS: Matches api-design.md
@PostMapping("/api/v1/students")
@ResponseStatus(HttpStatus.CREATED)
public StudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) { ... }

// ❌ FAIL: Wrong status code
@PostMapping("/api/v1/students")
@ResponseStatus(HttpStatus.OK) // Should be 201 CREATED!
```

**5. Tests Prove Requirements Met**
```markdown
- [ ] Every acceptance criterion has corresponding test
- [ ] Tests actually verify the requirement (not just code coverage)
- [ ] Tests pass (green)
```

**Example:**
```markdown
**Acceptance Criteria:** Student email must be unique per tenant

**Test verification:**
@Test
void createStudent_WithDuplicateEmail_InSameTenant_ShouldThrow() {
    // Create first student in tenant A
    studentService.createStudent(...);

    // Try to create second student with same email in tenant A
    assertThatThrownBy(() -> studentService.createStudent(...))
        .isInstanceOf(DuplicateResourceException.class);
}

✅ PASS - Test proves requirement met
```

#### Outcome:

- ✅ **PASS Stage 1** → Proceed to Stage 2
- ❌ **FAIL Stage 1** → BLOCK PR, return to developer with specific issues

**CRITICAL:** Do NOT review code quality if Stage 1 fails. Fix spec issues first.

---

### Stage 2: Code Quality (20-30 min) 🟠🟡 GRADED

**Goal:** Evaluate code maintainability, performance, security

**Question:** Is this code production-ready?

#### Review by Severity:

### 🔴 CRITICAL Issues (Must Fix - BLOCKING)

**Security Vulnerabilities:**
```java
// ❌ CRITICAL: SQL Injection
String sql = "SELECT * FROM students WHERE name = '" + name + "'";
jdbcTemplate.query(sql, ...);

// ✅ Fix: Parameterized query
@Query("SELECT s FROM Student s WHERE s.name = :name")
List<Student> findByName(@Param("name") String name);
```

**Data Loss Risks:**
```java
// ❌ CRITICAL: Missing transaction (data inconsistency)
public void transferStudent(UUID studentId, UUID newClassId) {
    classService.removeStudent(studentId); // If this fails halfway...
    classService.addStudent(studentId, newClassId); // ...student is lost!
}

// ✅ Fix: Transaction boundary
@Transactional
public void transferStudent(UUID studentId, UUID newClassId) { ... }
```

**Breaking Changes:**
```java
// ❌ CRITICAL: Breaking API change (removes field)
public record StudentResponse(UUID id, String name) {
    // REMOVED: String email - breaks clients!
}

// ✅ Fix: Deprecate first, remove later
public record StudentResponse(
    UUID id,
    String name,
    @Deprecated String email // Mark deprecated, remove in v2.0
) { }
```

**Authentication/Authorization Bypasses:**
```java
// ❌ CRITICAL: No auth check
@GetMapping("/api/v1/students/{id}")
public StudentResponse getStudent(@PathVariable UUID id) {
    return studentService.findById(id); // Any user can access!
}

// ✅ Fix: Add security
@GetMapping("/api/v1/students/{id}")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public StudentResponse getStudent(@PathVariable UUID id) { ... }
```

**Outcome:** 🔴 Any CRITICAL issue → BLOCK PR

---

### 🟠 MAJOR Issues (Should Fix - Strong Recommendation)

**Performance Problems:**
```java
// 🟠 MAJOR: N+1 query
students.forEach(student -> {
    student.getCourses().size(); // Lazy load in loop! (N+1)
});

// ✅ Fix: Eager fetch
@EntityGraph(attributePaths = {"courses"})
List<Student> findAllWithCourses();
```

**Test Coverage Gaps:**
```java
// 🟠 MAJOR: New service method with 0 tests
public StudentResponse updateStudent(UUID id, UpdateStudentRequest request) {
    // 50 lines of logic, NO TESTS!
}

// ✅ Fix: Add tests (target 80%+ coverage on new code)
@Test
void updateStudent_WithValidData_ShouldUpdateAndReturnStudent() { ... }
```

**Missing Error Handling:**
```java
// 🟠 MAJOR: Uncaught exception
public Student findById(UUID id) {
    return studentRepository.findById(id).get(); // NoSuchElementException if not found!
}

// ✅ Fix: Proper error handling
public Student findById(UUID id) {
    return studentRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("STUDENT_NOT_FOUND", id));
}
```

**Maintainability Concerns:**
```java
// 🟠 MAJOR: 500+ line class (too complex)
public class StudentService {
    // 500+ lines of methods (hard to maintain)
}

// ✅ Fix: Extract to smaller classes
public class StudentRegistrationService { ... }
public class StudentGradeService { ... }
```

**Outcome:** 🟠 MAJOR issues → Approve with strong recommendation to fix

---

### 🟡 MINOR Issues (Nice to Have - Non-Blocking)

**Naming Improvements:**
```java
// 🟡 MINOR: Vague naming
List<Student> list = studentRepository.findAll();
Student s = list.get(0);

// ✅ Better: Descriptive names
List<Student> enrolledStudents = studentRepository.findAll();
Student firstStudent = enrolledStudents.get(0);
```

**Code Duplication:**
```java
// 🟡 MINOR: Duplicate mapping logic
StudentResponse response1 = new StudentResponse(student1.getId(), student1.getName(), ...);
StudentResponse response2 = new StudentResponse(student2.getId(), student2.getName(), ...);

// ✅ Better: Extract to mapper
StudentResponse response1 = studentMapper.toResponse(student1);
StudentResponse response2 = studentMapper.toResponse(student2);
```

**Missing JavaDoc:**
```java
// 🟡 MINOR: No JavaDoc on public method
public StudentResponse createStudent(CreateStudentRequest request) { ... }

// ✅ Better: Add JavaDoc
/**
 * Creates a new student in the system.
 *
 * @param request student creation details
 * @return created student with generated ID
 * @throws DuplicateResourceException if email already exists
 */
public StudentResponse createStudent(CreateStudentRequest request) { ... }
```

**Style Inconsistencies:**
```java
// 🟡 MINOR: Inconsistent spacing/formatting
public void method1(){...}
public void method2() { ... }

// ✅ Fix: Consistent style (Checkstyle auto-formats)
public void method1() { ... }
public void method2() { ... }
```

**Outcome:** 🟡 MINOR issues only → Approve (optional follow-up issue)

---

## 📝 Review Template

### For Reviewers:

```markdown
## Code Review: PR #XX - [Feature Name]

**Reviewer:** [Your Name]
**Date:** YYYY-MM-DD

---

### Stage 1: Specification Compliance ✅/❌

**Requirements Verification:**
1. [Requirement 1]: ✅/❌ [Brief note]
2. [Requirement 2]: ✅/❌ [Brief note]
3. [Requirement 3]: ✅/❌ [Brief note]

**Edge Cases:**
- Null inputs: ✅/❌
- Invalid data: ✅/❌
- Error handling: ✅/❌
- Multi-tenant: ✅/❌

**File Locations:**
- Code files: ✅/❌
- Test files: ✅/❌

**API Contracts:**
- DTOs match design: ✅/❌
- Status codes correct: ✅/❌

**Test Coverage:**
- All criteria tested: ✅/❌
- Tests pass: ✅/❌

**Stage 1 Outcome:** ✅ PASS / ❌ FAIL

**If FAIL, stop here. List issues:**
- [Issue 1]
- [Issue 2]

---

### Stage 2: Code Quality (Only if Stage 1 PASS)

**🔴 Critical Issues (BLOCKING):**
- [ ] No security vulnerabilities
- [ ] No data loss risks
- [ ] No breaking changes
- [ ] No auth bypasses

**Issues Found:**
- [None / List issues]

---

**🟠 Major Issues (RECOMMENDED):**
- [ ] No N+1 queries or performance issues
- [ ] Test coverage ≥ 80% on new code
- [ ] Error handling present
- [ ] Class size reasonable (<300 lines)

**Issues Found:**
1. [Issue with location/line number]
2. [Issue with location/line number]

---

**🟡 Minor Issues (OPTIONAL):**
- [ ] Naming is clear and descriptive
- [ ] No code duplication
- [ ] JavaDoc on public methods
- [ ] Style consistent

**Issues Found:**
1. [Issue with location/line number]
2. [Issue with location/line number]

---

### Stage 2 Outcome:

- [ ] ✅ APPROVE (no critical/major issues)
- [ ] 🟠 APPROVE with recommendations (major issues noted, not blocking)
- [ ] 🔴 BLOCK (critical issues must be fixed)

**Summary:**
[Brief summary of review - what's good, what needs work]

**Next Steps:**
- [Action 1]
- [Action 2]
```

---

## 🎯 KiteClass Example

### Example PR: Student CRUD Implementation

```markdown
## Code Review: PR 2.15 - Student CRUD Endpoints

**Reviewer:** Claude Code
**Date:** 2026-03-13

---

### Stage 1: Specification Compliance ✅

**Requirements from implementation-plan.md (PR 2.15):**
1. POST /api/students (create): ✅ PASS
2. GET /api/students/{id} (read): ✅ PASS
3. PUT /api/students/{id} (update): ✅ PASS
4. DELETE /api/students/{id} (soft delete): ✅ PASS

**Edge Cases:**
- Null inputs: ✅ Validation present (@NotBlank, @Email)
- Invalid data: ✅ Tests for invalid email format
- Error handling: ✅ EntityNotFoundException for missing student
- Multi-tenant: ✅ Tests verify tenant isolation

**File Locations:**
- Code: `kiteclass-core/src/main/java/.../StudentService.java` ✅
- Tests: `kiteclass-core/src/test/java/.../StudentServiceTest.java` ✅

**API Contracts:**
- DTOs: ✅ Match api-design.md (CreateStudentRequest, StudentResponse)
- Status: ✅ 201 CREATED, 200 OK, 404 NOT_FOUND

**Test Coverage:**
- All CRUD tested: ✅ 18 tests cover all methods
- Tests pass: ✅ All green

**Stage 1 Outcome:** ✅ PASS - Proceed to Stage 2

---

### Stage 2: Code Quality

**🔴 Critical Issues:**
- [x] No security vulnerabilities
- [x] No data loss risks
- [x] No breaking changes
- [x] No auth bypasses

**Issues Found:** None ✅

---

**🟠 Major Issues:**
- [x] No N+1 queries
- [ ] Test coverage 75% (target 80%) ⚠️
- [x] Error handling present
- [x] Class size OK (180 lines)

**Issues Found:**
1. StudentService test coverage is 75% (missing tests for edge case: update with unchanged data)
   - **Recommendation:** Add test `updateStudent_WithSameData_ShouldNotUpdateTimestamp()`
   - **Priority:** Medium (can be follow-up issue)

---

**🟡 Minor Issues:**
- [x] Naming clear
- [x] No duplication
- [ ] Missing JavaDoc on `updateStudent()` ⚠️
- [x] Style consistent

**Issues Found:**
1. Line 45: `updateStudent()` missing JavaDoc
   - **Recommendation:** Add JavaDoc with @param, @return, @throws
   - **Priority:** Low (nice to have)

---

### Stage 2 Outcome: 🟠 APPROVE with Recommendations

**Summary:**
Solid implementation! All requirements met, code is clean and well-tested. Two minor improvements suggested:
1. Increase test coverage to 80% (add edge case test)
2. Add JavaDoc to updateStudent() method

Neither issue is blocking - can be addressed in follow-up PR or quick fix.

**Next Steps:**
- ✅ APPROVED for merge
- [ ] Create follow-up issue: "Increase StudentService test coverage to 80%"
- [ ] Optional: Add JavaDoc in next PR touching this file
```

---

## 🔗 Integration with Existing Skills

**Stage 1 uses:**
- `implementation-plan.md` - Acceptance criteria
- `api-design.md` - Endpoint contracts
- `brainstorming-methodology.md` - Design decisions

**Stage 2 uses:**
- `code-style.md` - Naming conventions
- `testing-guide.md` - Coverage requirements
- `spring-boot-testing-quality.md` - Test quality
- `security-testing-standards.md` - Security checks

**After Review:**
- Update PR with feedback
- Re-review if BLOCKED
- Merge if APPROVED

---

## 📏 Success Metrics

**Track per review:**
- Stage 1 pass rate (target: >80% first time)
- Issues caught in Stage 1 vs Stage 2
- Time spent on review (target: <45 min total)

**Track overall:**
- Review iterations per PR (target: ≤2, down from 2.5)
- Critical issues caught (should be rare with TDD)
- PR merge time (faster with clear 2-stage process)

---

## 🎯 Trigger Phrases

Auto-activate this skill when:
- "review this PR"
- "ready for code review"
- "please review"
- PR marked as "Ready for Review" on GitHub

---

## ✅ Quick Reference Checklist

**Stage 1 (Spec Compliance) - MUST PASS:**
- [ ] All requirements implemented
- [ ] Edge cases covered
- [ ] Files in correct locations
- [ ] API contracts match design
- [ ] Tests prove requirements met

**Stage 2 (Code Quality) - GRADED:**
- [ ] 🔴 No CRITICAL issues (security, data loss, breaking changes)
- [ ] 🟠 Minimal MAJOR issues (performance, test coverage, error handling)
- [ ] 🟡 MINOR issues noted (naming, duplication, docs)

**Decision:**
- ✅ APPROVE (no critical/major) or 🟠 APPROVE with recommendations (major noted) or 🔴 BLOCK (critical found)

---

**Last Updated:** 2026-03-13
**Author:** Claude Code (Superpowers-inspired)
**Status:** ✅ Active - Mandatory for all PRs
