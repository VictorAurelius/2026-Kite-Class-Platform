# TDD Workflow Diagram

**Quick Reference** - RED-GREEN-REFACTOR Cycle

---

## The Cycle

```
┌─────────────────────────────────────────────────────────┐
│                    TDD WORKFLOW                         │
└─────────────────────────────────────────────────────────┘

    🔴 RED                  🟢 GREEN              ♻️ REFACTOR

┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│ Write Test   │──────│ Write Code   │──────│ Clean Up     │
│ (Fails)      │      │ (Passes)     │      │ (Still Pass) │
└──────────────┘      └──────────────┘      └──────────────┘
       │                     │                     │
       │                     │                     │
       ▼                     ▼                     ▼

┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│ - Test file  │      │ - Minimal    │      │ - Remove     │
│   created    │      │   impl       │      │   duplication│
│ - Must FAIL  │      │ - No extra   │      │ - Better     │
│   first run  │      │   features   │      │   naming     │
│ - Specific   │      │ - Just make  │      │ - Extract    │
│   scenario   │      │   test pass  │      │   methods    │
└──────────────┘      └──────────────┘      └──────────────┘

       │                     │                     │
       └─────────────────────┴─────────────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  Next Test   │
                     │  (New Cycle) │
                     └──────────────┘
```

---

## Phase Details

### 🔴 RED (5-10 min)

**Steps:**
1. Write test FIRST (before any code)
2. Run test → MUST FAIL
3. Verify failure message is correct

**Example:**
```java
@Test
void createStudent_WithValidData_ShouldReturnStudentWithId() {
    CreateStudentRequest request = new CreateStudentRequest("John", "john@test.com");

    // This WILL fail - method not implemented
    StudentResponse response = studentService.createStudent(request);

    assertThat(response.getId()).isNotNull();
}
```

**Run:** `./mvnw test -Dtest=StudentServiceTest`
**Expected:** ❌ FAIL (method doesn't exist)

---

### 🟢 GREEN (10-20 min)

**Steps:**
1. Write MINIMAL code to pass test
2. No optimization, no extra features
3. Run test → MUST PASS

**Example:**
```java
public StudentResponse createStudent(CreateStudentRequest request) {
    Student student = new Student();
    student.setName(request.getName());
    student.setEmail(request.getEmail());

    Student saved = studentRepository.save(student);
    return new StudentResponse(saved.getId(), saved.getName(), saved.getEmail());
}
```

**Run:** `./mvnw test -Dtest=StudentServiceTest`
**Expected:** ✅ PASS (GREEN!)

---

### ♻️ REFACTOR (5-15 min)

**Steps:**
1. Improve code quality
2. Remove duplication
3. Better naming
4. Run tests → STILL PASS

**Example:**
```java
public StudentResponse createStudent(CreateStudentRequest request) {
    Student saved = studentRepository.save(studentMapper.toEntity(request));
    return studentMapper.toResponse(saved);
}
```

**Run:** `./mvnw test`
**Expected:** ✅ ALL PASS (refactor successful!)

---

## Git Hook Check

Pre-commit hook checks:
- ✅ Test file modified BEFORE code file
- ✅ Test file timestamp < Code file timestamp
- ⚠️ Warning if code modified without test

**Week 1-4:** WARNING mode (advisory)
**Week 5+:** BLOCKING mode (enforced)

---

## Rules to Follow

### ✅ DO
- ✅ Write test FIRST (before any code)
- ✅ Run test to see it FAIL (proves test works)
- ✅ Write minimal code to pass (no over-engineering)
- ✅ Refactor while tests are GREEN
- ✅ Run tests after refactor (ensure still green)

### ❌ DON'T
- ❌ Write code before test
- ❌ Skip RED phase (test must fail first)
- ❌ Add features not in test
- ❌ Optimize prematurely
- ❌ Refactor with failing tests

---

## Example Session

```bash
# 1. 🔴 RED - Write failing test
vim StudentServiceTest.java
# Write: testCreateStudent_ShouldReturnId()
./mvnw test -Dtest=StudentServiceTest
# Output: ❌ FAIL - Method not found

# 2. 🟢 GREEN - Minimal implementation
vim StudentServiceImpl.java
# Write: public StudentResponse createStudent(...)
./mvnw test -Dtest=StudentServiceTest
# Output: ✅ PASS

# 3. ♻️ REFACTOR - Clean up
vim StudentServiceImpl.java
# Extract to: studentMapper.toEntity()
./mvnw test
# Output: ✅ ALL PASS

# 4. Commit (test + code together)
git add StudentServiceTest.java StudentServiceImpl.java
git commit -m "feat(student): Add create student method"
# Pre-commit hook: ✅ TDD compliance OK
```

---

## Common Mistakes

### ❌ Writing Code First
```java
// WRONG ORDER:
// 1. Write StudentService.createStudent()  ❌
// 2. Write test later (maybe)               ❌
```

### ❌ Test Passes Immediately
```java
@Test
void test() {
    assertTrue(true);  // ❌ Test passes without implementation
}
```

### ❌ Over-Engineering in GREEN
```java
// ❌ WRONG: Too complex for GREEN phase
public StudentResponse createStudent(CreateStudentRequest request) {
    validateRequest(request);
    checkDuplicateEmail(request.getEmail());
    Student student = mapRequestToEntity(request);
    enrichWithMetadata(student);
    Student saved = saveWithAudit(student);
    publishStudentCreatedEvent(saved);
    return mapEntityToResponse(saved);
}

// ✅ CORRECT: Minimal for GREEN phase
public StudentResponse createStudent(CreateStudentRequest request) {
    Student saved = studentRepository.save(toEntity(request));
    return toResponse(saved);
}
// Add complexity in REFACTOR if needed
```

---

## Success Criteria

- ✅ Test written BEFORE code (verified by git timestamps)
- ✅ Test fails initially (RED phase completed)
- ✅ Test passes after implementation (GREEN phase completed)
- ✅ Code refactored with tests still green (REFACTOR completed)
- ✅ Commit includes both test AND code

---

**Reference:** `.claude/skills/tdd-enforcement.md`
**Enforcement:** Git hook checks timestamp order
**Target:** 85%+ test coverage (up from 75% baseline)
