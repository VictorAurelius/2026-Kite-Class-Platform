# Task Breakdown Formula

**Quick Reference** - 2-5 Minute Task Anatomy

---

## The Formula

```
TASK = FILE_PATH + CHANGE + CODE_SAMPLE + VERIFICATION + TIME

Where:
- FILE_PATH: Exact location (absolute path)
- CHANGE: What to do (specific action)
- CODE_SAMPLE: Exact code to write (copy-paste ready)
- VERIFICATION: How to test (command or check)
- TIME: Estimated duration (2-5 min)
```

---

## Anatomy of a Perfect Task

### ✅ GOOD Task Example

```markdown
**Task 2.3:** Add email validation to CreateStudentRequest

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/dto/CreateStudentRequest.java`

**Change:** Add `@Email` annotation to email field

**Code:**
```java
@Email(message = "STUDENT_EMAIL_INVALID")
private String email;
```

**Verification:**
```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithInvalidEmail_ShouldThrow
# Expected: ✅ PASS - Validation exception thrown
```

**Time:** 2 minutes
```

### ❌ BAD Task Example

```markdown
**Task X:** Add validation

File: StudentService  ❌ Not specific enough
Change: Validate inputs  ❌ What inputs? How?
Code: (none provided)  ❌ Developer must guess implementation
Verification: Make sure it works  ❌ Not testable
Time: (none)  ❌ Can't estimate effort
```

---

## Task Sizing Guide

### 2-Minute Tasks
- Add single annotation
- Update single field
- Change single value
- Import single class

**Example:**
```markdown
Task: Add @NotNull to studentId parameter
File: StudentService.java:45
Code: public Student findById(@NotNull UUID studentId)
Time: 2 min
```

### 3-Minute Tasks
- Add single method
- Create single test
- Update single configuration

**Example:**
```markdown
Task: Add validation test
File: StudentServiceTest.java
Code: @Test void createStudent_WithNullName_ShouldThrow() { ... }
Time: 3 min
```

### 5-Minute Tasks
- Implement small method with logic
- Create DTO class
- Update multiple related fields

**Example:**
```markdown
Task: Implement StudentMapper.toEntity()
File: StudentMapper.java
Code: public Student toEntity(CreateStudentRequest request) { ... }
Time: 5 min
```

### ❌ TOO LARGE (>5 min)
- Implement entire service class → BREAK DOWN
- Create multiple endpoints → BREAK DOWN
- Write 10+ tests → BREAK DOWN

---

## Breakdown Strategies

### Strategy 1: Bottom-Up (Recommended)

Build from foundation upward:

```
1. Entity (2 min) →
2. Repository (2 min) →
3. Mapper (3 min) →
4. Service (5 min) →
5. Controller (5 min)

Total: 17 min / 5 tasks = 3.4 min average ✅
```

### Strategy 2: Test-First (TDD)

Alternate test and implementation:

```
1. Write test (3 min) →
2. Implement minimal code (4 min) →
3. Refactor (3 min) →
4. Next test (3 min)...

Total: 10 min / 3 tasks = 3.3 min average ✅
```

### Strategy 3: Vertical Slice

Complete one feature end-to-end:

```
1. Entity + Repository (4 min) →
2. Service method (5 min) →
3. Controller endpoint (5 min) →
4. Test (4 min)

Total: 18 min / 4 tasks = 4.5 min average ✅
```

---

## Task Ordering Rules

### ✅ DO: Dependencies First

```
1. Create StudentEntity  ✅ (needed by repository)
2. Create StudentRepository  ✅ (needed by service)
3. Create StudentService  ✅ (needed by controller)
4. Create StudentController  ✅
```

### ❌ DON'T: Random Order

```
1. Create StudentController  ❌ (depends on service)
2. Create StudentEntity  ❌ (should be first)
3. Create StudentService  ❌ (depends on repository)
```

---

## Verification Examples

### Backend Tests
```bash
# Single test
./mvnw test -Dtest=StudentServiceTest#createStudent

# Test class
./mvnw test -Dtest=StudentServiceTest

# All tests
./mvnw test
```

### Frontend Tests
```bash
# Single test file
pnpm test StudentList.test.tsx

# Watch mode
pnpm test:watch

# Coverage
pnpm test:coverage
```

### Manual Verification
```bash
# Start dev environment
docker-compose -f docker-compose.dev.yml up

# Test endpoint
curl http://localhost:8081/api/students

# Check logs
docker logs kiteclass-core -f
```

---

## Complete Task Breakdown Example

### Feature: Student CRUD (9 tasks)

```markdown
**Task 1:** Create Student entity
File: kiteclass-core/src/main/java/com/kiteclass/entity/Student.java
Code: [Full entity class with annotations]
Verify: Compiles without errors
Time: 3 min

**Task 2:** Create StudentRepository
File: kiteclass-core/src/main/java/com/kiteclass/repository/StudentRepository.java
Code: public interface StudentRepository extends JpaRepository<Student, UUID> { }
Verify: Compiles without errors
Time: 2 min

**Task 3:** Create CreateStudentRequest DTO
File: kiteclass-core/src/main/java/com/kiteclass/dto/CreateStudentRequest.java
Code: public record CreateStudentRequest(@NotBlank String name, @Email String email) { }
Verify: Compiles with validation annotations
Time: 2 min

**Task 4:** Create StudentResponse DTO
File: kiteclass-core/src/main/java/com/kiteclass/dto/StudentResponse.java
Code: public record StudentResponse(UUID id, String name, String email) { }
Verify: Compiles without errors
Time: 2 min

**Task 5:** Create StudentMapper
File: kiteclass-core/src/main/java/com/kiteclass/mapper/StudentMapper.java
Code: [toEntity() and toResponse() methods]
Verify: ./mvnw test -Dtest=StudentMapperTest
Time: 4 min

**Task 6:** Create StudentService interface
File: kiteclass-core/src/main/java/com/kiteclass/service/StudentService.java
Code: public interface StudentService { StudentResponse createStudent(...); }
Verify: Compiles without errors
Time: 2 min

**Task 7:** Implement StudentServiceImpl
File: kiteclass-core/src/main/java/com/kiteclass/service/impl/StudentServiceImpl.java
Code: [createStudent implementation with repository call]
Verify: ./mvnw test -Dtest=StudentServiceTest#createStudent
Time: 5 min

**Task 8:** Create StudentController
File: kiteclass-core/src/main/java/com/kiteclass/controller/StudentController.java
Code: [POST /api/students endpoint]
Verify: ./mvnw test -Dtest=StudentControllerTest
Time: 4 min

**Task 9:** Add integration test
File: kiteclass-core/src/test/java/com/kiteclass/StudentIntegrationTest.java
Code: [Full integration test with database]
Verify: ./mvnw test -Dtest=StudentIntegrationTest
Time: 5 min

---
Total: 29 minutes / 9 tasks = 3.2 min average ✅
```

---

## Common Mistakes

### ❌ Tasks Too Large
```
Task: Implement entire Student module
Time: 2 hours  ❌ Should be 20-30 tasks of 2-5 min each
```

### ❌ Missing Exact File Path
```
File: StudentService  ❌ Which directory? Main or test?
Should: kiteclass-core/src/main/java/com/kiteclass/service/StudentService.java
```

### ❌ Vague Change Description
```
Change: Update the code  ❌ What exactly?
Should: Add @NotNull annotation to studentId parameter
```

### ❌ No Code Sample
```
Code: (implement the method)  ❌ Developer must guess
Should: [Exact code to copy-paste]
```

### ❌ Unverifiable
```
Verify: Make sure it works  ❌ How?
Should: ./mvnw test -Dtest=StudentServiceTest#createStudent
```

---

## Success Criteria

- ✅ All tasks are 2-5 minutes (no >5 min tasks)
- ✅ Every task has exact file path
- ✅ Every task has code sample (copy-paste ready)
- ✅ Every task has verification command
- ✅ Tasks ordered by dependencies
- ✅ Total time = sum of task times (predictable)

---

## Benefits

**Before (ad-hoc):**
- "Implement Student CRUD" → 2 hours, actual 4 hours (100% overrun)
- No clear progress tracking
- Hard to resume after interruption

**After (task breakdown):**
- 9 tasks × 3.2 min = 29 min estimated, actual 35 min (20% overrun) ✅
- Clear progress: 5/9 tasks done (56%)
- Easy to resume: Start at task 6

---

**Reference:** `.claude/skills/task-breakdown-guide.md`
**Target:** 80%+ planning accuracy (up from 60% baseline)
**Target:** 2-5 min per task (no >5 min tasks)
