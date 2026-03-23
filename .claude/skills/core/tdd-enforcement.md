# Skill: Test-Driven Development Enforcement

**Version:** 1.0 (Superpowers-inspired)
**Last Updated:** 2026-03-13
**Purpose:** Enforce RED-GREEN-REFACTOR cycle for higher quality, fewer bugs

---

## 📋 Overview

Replace "code first, test later (maybe)" with **mandatory test-first development**:
- 🔴 **RED:** Write failing test (defines expected behavior)
- 🟢 **GREEN:** Write minimal code to pass test
- ♻️ **REFACTOR:** Clean up while keeping tests green

**Target:** +10% test coverage (75% → 85%), -40% bug escape rate

---

## 🎯 When to Use This Skill

**Mandatory for:**
- ✅ New features (all new code)
- ✅ Bug fixes (write failing test reproducing bug first)
- ✅ API endpoints (test contract before implementation)
- ✅ Business logic (services, repositories)

**Exceptions (TDD not required):**
- ⏭️ Refactoring existing code (tests already exist)
- ⏭️ Typos in strings/comments
- ⏭️ Documentation updates
- ⏭️ Configuration files (application.yml, pom.xml)
- ⏭️ Simple getters/setters (Lombok generates)

**Default:** If in doubt, write test first. Cost is low, benefit is high.

---

## 🔄 RED-GREEN-REFACTOR Cycle

### 🔴 Phase 1: RED - Write Failing Test First (5-10 min)

**Goal:** Define expected behavior through test (test = specification)

#### Rules:

1. **Write test BEFORE any production code**
   ```java
   // ❌ WRONG Order:
   // 1. Write StudentService.createStudent()
   // 2. Write test

   // ✅ CORRECT Order:
   // 1. Write test
   // 2. Run test → MUST FAIL
   // 3. Write StudentService.createStudent()
   ```

2. **Test must FAIL when first run**
   ```java
   // If test passes without implementation, test is wrong!
   @Test
   void createStudent_ShouldReturnStudentWithId() {
       CreateStudentRequest request = new CreateStudentRequest("John", "john@test.com", "CS");

       // This WILL fail - method not implemented yet
       assertThatThrownBy(() -> studentService.createStudent(request))
           .isInstanceOf(UnsupportedOperationException.class);
   }
   ```

3. **Test should be specific and clear**
   ```java
   // ❌ WRONG: Vague test
   @Test
   void testStudent() { ... }

   // ✅ CORRECT: Clear intent
   @Test
   void createStudent_WithValidData_ShouldReturnStudentWithGeneratedId() {
       // Arrange
       CreateStudentRequest request = new CreateStudentRequest(
           "John Doe",
           "john@example.com",
           "Computer Science"
       );

       // Act
       StudentResponse response = studentService.createStudent(request);

       // Assert
       assertThat(response.getId()).isNotNull();
       assertThat(response.getName()).isEqualTo("John Doe");
       assertThat(response.getEmail()).isEqualTo("john@example.com");
       assertThat(response.getMajor()).isEqualTo("Computer Science");
   }
   ```

#### Examples:

**Java (Backend):**
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

**TypeScript (Frontend):**
```typescript
test('StudentList should display loading state initially', () => {
  // Arrange
  render(<StudentList />);

  // Assert: This WILL fail initially (component not implemented)
  expect(screen.getByText('Loading students...')).toBeInTheDocument();
});

test('StudentList should display students after load', async () => {
  // Arrange
  const mockStudents = [
    { id: '1', name: 'John Doe', email: 'john@test.com' },
    { id: '2', name: 'Jane Smith', email: 'jane@test.com' }
  ];

  server.use(
    http.get('/api/students', () => {
      return HttpResponse.json(mockStudents);
    })
  );

  // Act
  render(<StudentList />);

  // Assert: This WILL fail (component not implemented)
  await waitFor(() => {
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
  });
});
```

**Run Test:**
```bash
# Backend
./mvnw test -Dtest=StudentServiceTest#createStudent_WithValidData_ShouldReturnStudentWithGeneratedId

# Frontend
pnpm test StudentList.test.tsx

# Expected: ❌ FAIL (method/component not implemented)
```

---

### 🟢 Phase 2: GREEN - Minimal Code to Pass (10-20 min)

**Goal:** Make test pass with SIMPLEST possible implementation

#### Rules:

1. **Write just enough code to pass the test**
   ```java
   // ❌ WRONG: Over-engineering in GREEN phase
   public StudentResponse createStudent(CreateStudentRequest request) {
       validateRequest(request);
       checkDuplicateEmail(request.getEmail());
       Student student = mapRequestToEntity(request);
       enrichWithMetadata(student);
       Student saved = saveWithAudit(student);
       publishStudentCreatedEvent(saved);
       return mapEntityToResponse(saved);
   }

   // ✅ CORRECT: Minimal implementation to pass test
   public StudentResponse createStudent(CreateStudentRequest request) {
       Student student = new Student(
           request.getName(),
           request.getEmail(),
           request.getMajor()
       );
       Student saved = studentRepository.save(student);
       return new StudentResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getMajor());
   }
   ```

2. **No premature optimization**
   ```java
   // ❌ WRONG: Optimizing before needed
   // Caching, batch processing, async operations
   @Cacheable("students")
   public StudentResponse createStudent(...) { ... }

   // ✅ CORRECT: Simple synchronous save
   public StudentResponse createStudent(CreateStudentRequest request) {
       Student saved = studentRepository.save(toEntity(request));
       return toResponse(saved);
   }
   ```

3. **No extra features ("just make it work")**
   ```java
   // ❌ WRONG: Adding features not in test
   public StudentResponse createStudent(CreateStudentRequest request) {
       Student student = ...;
       studentRepository.save(student);

       // Not in test requirements!
       emailService.sendWelcomeEmail(student);
       auditService.logStudentCreation(student);

       return toResponse(student);
   }

   // ✅ CORRECT: Only what test requires
   public StudentResponse createStudent(CreateStudentRequest request) {
       Student saved = studentRepository.save(toEntity(request));
       return toResponse(saved);
   }
   ```

**Run Test:**
```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithValidData_ShouldReturnStudentWithGeneratedId

# Expected: ✅ PASS (GREEN!)
```

---

### ♻️ Phase 3: REFACTOR - Clean Up (5-15 min)

**Goal:** Improve code quality while keeping tests green

#### Rules:

1. **Remove duplication**
   ```java
   // Before refactor (duplication):
   @Override
   public StudentResponse createStudent(CreateStudentRequest request) {
       Student student = new Student(request.getName(), request.getEmail(), request.getMajor());
       Student saved = studentRepository.save(student);
       return new StudentResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getMajor());
   }

   @Override
   public TeacherResponse createTeacher(CreateTeacherRequest request) {
       Teacher teacher = new Teacher(request.getName(), request.getEmail(), request.getSubject());
       Teacher saved = teacherRepository.save(teacher);
       return new TeacherResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getSubject());
   }

   // After refactor (DRY):
   @Override
   public StudentResponse createStudent(CreateStudentRequest request) {
       Student saved = studentRepository.save(studentMapper.toEntity(request));
       return studentMapper.toResponse(saved);
   }

   @Override
   public TeacherResponse createTeacher(CreateTeacherRequest request) {
       Teacher saved = teacherRepository.save(teacherMapper.toEntity(request));
       return teacherMapper.toResponse(saved);
   }
   ```

2. **Improve naming**
   ```java
   // Before: Vague names
   Student s = studentRepository.save(toEntity(req));
   return toResponse(s);

   // After: Clear names
   Student savedStudent = studentRepository.save(toEntity(request));
   return toResponse(savedStudent);
   ```

3. **Extract methods if complex**
   ```java
   // Before: Long method
   public StudentResponse createStudent(CreateStudentRequest request) {
       if (studentRepository.findByEmailAndDeletedFalse(request.getEmail()).isPresent()) {
           throw new DuplicateResourceException("STUDENT_EMAIL_ALREADY_EXISTS", request.getEmail());
       }

       Student student = new Student();
       student.setName(request.getName());
       student.setEmail(request.getEmail());
       student.setMajor(request.getMajor());
       student.setCreatedAt(LocalDateTime.now());

       Student saved = studentRepository.save(student);

       return new StudentResponse(
           saved.getId(),
           saved.getName(),
           saved.getEmail(),
           saved.getMajor(),
           saved.getCreatedAt()
       );
   }

   // After: Extracted methods
   public StudentResponse createStudent(CreateStudentRequest request) {
       validateEmailUnique(request.getEmail());
       Student saved = studentRepository.save(studentMapper.toEntity(request));
       return studentMapper.toResponse(saved);
   }

   private void validateEmailUnique(String email) {
       if (studentRepository.findByEmailAndDeletedFalse(email).isPresent()) {
           throw new DuplicateResourceException("STUDENT_EMAIL_ALREADY_EXISTS", email);
       }
   }
   ```

**Run Tests Again:**
```bash
./mvnw test -Dtest=StudentServiceTest

# Expected: ✅ ALL TESTS STILL PASS (refactor successful!)
```

---

## 🛡️ Enforcement via Git Hook

### Pre-Commit Check (Automated)

**Location:** `.claude/scripts/pre-commit.sh`

**Week 1-4 (Warning Mode):**
```bash
#!/bin/bash
# TDD Enforcement Check - WARNING MODE

echo "🧪 Checking TDD compliance..."

# Get list of modified Java files (excluding tests)
MODIFIED_JAVA=$(git diff --cached --name-only --diff-filter=ACM | grep "src/main/.*\.java$")

if [ -n "$MODIFIED_JAVA" ]; then
  for java_file in $MODIFIED_JAVA; do
    # Find corresponding test file
    test_file=$(echo "$java_file" | sed 's/src\/main/src\/test/' | sed 's/\.java$/Test.java/')

    if [ -f "$test_file" ]; then
      # Compare modification times in this commit
      java_time=$(git log -1 --format=%ct -- "$java_file" 2>/dev/null || echo 0)
      test_time=$(git log -1 --format=%ct -- "$test_file" 2>/dev/null || echo 0)

      if [ "$java_time" -gt "$test_time" ] && [ "$test_time" -ne 0 ]; then
        echo "⚠️  TDD Warning: Code modified after test"
        echo "   File: $java_file"
        echo "   Test: $test_file"
        echo "   Consider: Did you write test first (RED-GREEN-REFACTOR)?"
        echo ""
      fi
    else
      echo "⚠️  Missing test file: $test_file"
      echo "   Consider: Add test for $java_file"
      echo ""
    fi
  done
fi

echo "✅ TDD check complete (warnings only)"
```

**Week 5+ (Blocking Mode):**
```bash
#!/bin/bash
# TDD Enforcement Check - BLOCKING MODE

echo "🧪 Checking TDD compliance..."

TDD_VIOLATIONS=0

# ... (same detection logic as above)

if [ "$java_time" -gt "$test_time" ] && [ "$test_time" -ne 0 ]; then
  echo "❌ ERROR: TDD violation - cannot commit"
  echo "   Code modified after test: $java_file"
  echo "   Fix: Write test first, then implement (RED-GREEN-REFACTOR)"
  TDD_VIOLATIONS=$((TDD_VIOLATIONS + 1))
fi

# ... (check all files)

if [ "$TDD_VIOLATIONS" -gt 0 ]; then
  echo ""
  echo "❌ $TDD_VIOLATIONS TDD violation(s) found - commit BLOCKED"
  echo ""
  echo "To fix:"
  echo "  1. Write/update test first"
  echo "  2. Run test (should fail)"
  echo "  3. Implement code to pass test"
  echo "  4. Commit test and code together"
  echo ""
  echo "To skip (NOT recommended): git commit --no-verify"
  exit 1
fi

echo "✅ TDD compliance verified"
```

---

## ⚠️ Git Hook Limitations (Discovered in Pilot Testing)

### Known Limitations

**1. Pre-commit Hook Cannot Access Current Commit Message**

**Problem:**
- Pre-commit runs BEFORE commit message is written
- Cannot check commit message for keywords like "ready for review" or "fix bug"
- Check #13 (Review Reminder) and #14 (Debug Reminder) only check:
  - Branch name (e.g., `feature/review-ready`)
  - PREVIOUS commit message (from `git log -1`)

**Impact:**
- ✅ Works if branch named with keywords (e.g., `fix/bug-123`)
- ⚠️ Doesn't detect keywords in CURRENT commit message
- ⚠️ May show reminder from previous commit context

**Workaround:**
- Use descriptive branch names: `feature/ready-for-review-*`
- Or accept that reminders based on previous commit
- Or move checks to `commit-msg` hook (has message access)

**Example:**
```bash
# Commit message: "fix: Add phone validation"
# Hook shows: "🔬 Debugging Reminder" (because previous commit had "fix")
# This is expected behavior in pre-commit hook
```

---

**2. Timestamp Check Uses Git History, Not Filesystem**

**Problem:**
- Hook checks `git log -1 --format=%ct` (commit timestamp)
- For NEW files (never committed), timestamp is 0
- May not catch test-after-code if both files are new in same commit

**Impact:**
- ✅ Works correctly for MODIFIED files
- ⚠️ May miss violation for NEWLY CREATED files committed together

**Workaround:**
- Commit test file first, then code file (separate commits)
- Or manually verify TDD workflow for greenfield code

**Example:**
```bash
# Both files new, committed together:
git add StudentServiceTest.java StudentServiceImpl.java
git commit -m "feat: Add student service"
# Hook may not detect order violation (both timestamp=0 initially)
```

---

**3. Only Checks Java Files (Backend)**

**Problem:**
- Current hook only checks `src/main/.*\.java$` pattern
- Frontend TDD (TypeScript/React) not checked

**Impact:**
- ✅ Backend TDD enforced
- ❌ Frontend TDD not enforced

**Future Enhancement:**
- Extend hook to check `.tsx` files
- Pattern: `src/components/*.tsx` → `src/__tests__/*.test.tsx`

---

**4. Warning Mode (Week 1-4) Not Blocking**

**Problem:**
- Warnings are advisory only (commit still succeeds)
- Developers might ignore warnings

**Impact:**
- ⚠️ TDD compliance not guaranteed during warning period
- ✅ Builds awareness before enforcement

**Mitigation:**
- Switch to BLOCKING mode Week 5+ (exit 1 on violation)
- Track compliance rate during warning period
- Address violations before blocking mode starts

---

### Acceptable Limitations

**These are by design:**

1. **Pre-commit vs Commit-msg Trade-off**
   - Pre-commit: Checks files before commit (good for code)
   - Commit-msg: Checks message content (good for review/debug reminders)
   - **Decision:** Keep in pre-commit for code checks, accept message limitation

2. **Git History Dependency**
   - Relies on git log for timestamps
   - **Decision:** Acceptable - encourages incremental commits (test → code)

3. **Backend-Only Initially**
   - **Decision:** Start with Java, add frontend later (Week 3-4)

---

## 🎯 KiteClass Examples

### Example 1: Student Service (Java)

**RED Phase:**
```java
@Test
void createStudent_WithValidData_ShouldReturnStudentWithGeneratedId() {
    // Arrange
    CreateStudentRequest request = new CreateStudentRequest(
        "John Doe",
        "john@example.com",
        "Computer Science"
    );

    // Act - This WILL fail (not implemented)
    StudentResponse response = studentService.createStudent(request);

    // Assert
    assertThat(response.getId()).isNotNull();
    assertThat(response.getName()).isEqualTo("John Doe");
    assertThat(response.getEmail()).isEqualTo("john@example.com");
}
```

```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithValidData_ShouldReturnStudentWithGeneratedId
# Result: ❌ FAIL - Method not implemented
```

**GREEN Phase:**
```java
@Override
public StudentResponse createStudent(CreateStudentRequest request) {
    Student student = new Student();
    student.setName(request.getName());
    student.setEmail(request.getEmail());
    student.setMajor(request.getMajor());

    Student saved = studentRepository.save(student);

    return new StudentResponse(
        saved.getId(),
        saved.getName(),
        saved.getEmail(),
        saved.getMajor()
    );
}
```

```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithValidData_ShouldReturnStudentWithGeneratedId
# Result: ✅ PASS - Green!
```

**REFACTOR Phase:**
```java
@Override
public StudentResponse createStudent(CreateStudentRequest request) {
    Student saved = studentRepository.save(studentMapper.toEntity(request));
    return studentMapper.toResponse(saved);
}
```

```bash
./mvnw test -Dtest=StudentServiceTest
# Result: ✅ ALL PASS - Refactor successful!
```

---

### Example 2: Student List Component (TypeScript)

**RED Phase:**
```typescript
// StudentList.test.tsx
test('should display loading state initially', () => {
  render(<StudentList />);

  // This WILL fail - component doesn't exist
  expect(screen.getByText('Loading...')).toBeInTheDocument();
});
```

```bash
pnpm test StudentList.test.tsx
# Result: ❌ FAIL - Component not found
```

**GREEN Phase:**
```typescript
// StudentList.tsx
export default function StudentList() {
  return <div>Loading...</div>;
}
```

```bash
pnpm test StudentList.test.tsx
# Result: ✅ PASS - Green!
```

**REFACTOR Phase:**
```typescript
// StudentList.tsx
export default function StudentList() {
  const [loading, setLoading] = useState(true);

  return (
    <div>
      {loading ? <LoadingSpinner text="Loading..." /> : <StudentTable />}
    </div>
  );
}
```

```bash
pnpm test StudentList.test.tsx
# Result: ✅ PASS - Refactor successful!
```

---

## 🔗 Integration with Existing Skills

**Use with:**
- `testing-guide.md` - Test structure patterns (Arrange-Act-Assert)
- `spring-boot-testing-quality.md` - Test quality checklist
- `code-style.md` - Code formatting after refactor
- `systematic-debugging.md` - If test fails unexpectedly

**Reference in:**
- `development-workflow.md` - Step 3 (TEST) now requires TDD
- `implementation-plan.md` - Task breakdown includes "write test" before "implement"

---

## 📏 Success Metrics

**Track per PR:**
- TDD compliance rate (% of files with test-first)
- Test coverage on new code (target: 85%+)
- Test-first violations caught by git hook

**Track overall:**
- Overall test coverage trend (target: 75% → 85%)
- Bug escape rate (target: -40%)
- Time spent debugging (should decrease)

---

## 🎯 Trigger Phrases

Auto-remind this skill when detecting:
- "implement [feature]"
- "add [functionality]"
- "create [class/method]"
- Any new .java or .tsx file created

**Git hook auto-activates** on commit with modified code files

---

## ✅ Quick Reference Checklist

Before committing, verify TDD was followed:

- [ ] **RED:** Did I write test FIRST? (test file modified before code file)
- [ ] **RED:** Did test FAIL initially? (proves test works)
- [ ] **GREEN:** Did I write minimal code? (no over-engineering)
- [ ] **GREEN:** Does test PASS now? (green bar)
- [ ] **REFACTOR:** Did I clean up code? (DRY, clear names)
- [ ] **REFACTOR:** Do all tests still PASS? (refactor safe)

**Git hook will verify:** Test file timestamp < Code file timestamp

---

**Last Updated:** 2026-03-13
**Author:** Claude Code (Superpowers-inspired)
**Status:** ✅ Active - Mandatory for all new code (enforced Week 5+)
