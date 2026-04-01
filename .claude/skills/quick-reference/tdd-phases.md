# TDD Phases — RED GREEN REFACTOR Detail

## 🔴 Phase 1: RED — Write Failing Test First (5-10 min)

**Rules:**
1. Write test BEFORE production code
2. Test MUST fail when first run (if it passes → test is wrong!)
3. Test should be specific: `createStudent_WithValidData_ShouldReturnStudentWithId`

**Java example:**
```java
@Test
void createStudent_WithValidData_ShouldReturnStudentWithGeneratedId() {
    CreateStudentRequest request = new CreateStudentRequest("John Doe", "john@example.com", "CS");
    // This WILL fail — method not implemented yet
    StudentResponse response = studentService.createStudent(request);
    assertThat(response.getId()).isNotNull();
    assertThat(response.getName()).isEqualTo("John Doe");
}
```

**TypeScript example:**
```typescript
test('StudentList should display loading state initially', () => {
  render(<StudentList />);
  expect(screen.getByText('Loading students...')).toBeInTheDocument(); // WILL fail
});
```

**Run:**
```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_...   # Backend
pnpm test StudentList.test.tsx                             # Frontend
# Expected: ❌ FAIL (not implemented)
```

---

## 🟢 Phase 2: GREEN — Minimal Code to Pass (10-20 min)

**Rules:**
1. Write JUST ENOUGH code to pass the test
2. No premature optimization
3. No extra features not in test

```java
// ❌ WRONG: Over-engineering
public StudentResponse createStudent(CreateStudentRequest request) {
    validateRequest(request);
    checkDuplicateEmail(request.getEmail());
    enrichWithMetadata(student);
    publishStudentCreatedEvent(saved);  // Not in test!
    return mapEntityToResponse(saved);
}

// ✅ CORRECT: Minimal
public StudentResponse createStudent(CreateStudentRequest request) {
    Student saved = studentRepository.save(studentMapper.toEntity(request));
    return studentMapper.toResponse(saved);
}
```

**Run:** Same test → ✅ PASS (GREEN!)

---

## ♻️ Phase 3: REFACTOR — Clean Up (5-15 min)

**Rules:** Improve code quality while keeping ALL tests green.

**Remove duplication:**
```java
// Before (duplication):
Student student = new Student(request.getName(), request.getEmail(), request.getMajor());
return new StudentResponse(saved.getId(), saved.getName(), saved.getEmail());

// After (mapper):
Student saved = studentRepository.save(studentMapper.toEntity(request));
return studentMapper.toResponse(saved);
```

**Improve naming:**
```java
Student s = repo.save(toEntity(req));  // ❌ vague
Student savedStudent = studentRepository.save(studentMapper.toEntity(request));  // ✅ clear
```

**Extract methods (if complex):**
```java
// Before: long method
// After: extracted validateEmailUnique(), enrichStudent()
public StudentResponse createStudent(CreateStudentRequest request) {
    validateEmailUnique(request.getEmail());
    Student saved = studentRepository.save(studentMapper.toEntity(request));
    return studentMapper.toResponse(saved);
}
```

**Run ALL tests:** `./mvnw test` → ✅ ALL PASS (refactor safe!)

---

## KiteClass TDD Examples

### Student Service (Java) — Full Cycle

**RED:** Write test `createStudent_WithValidData_ShouldReturnStudentWithGeneratedId` → fails
**GREEN:** Implement minimal `createStudent()` → test passes
**REFACTOR:** Extract mapper, add `validateEmailUnique()` → all tests still pass

### Student List Component (TypeScript) — Full Cycle

**RED:** `test('should display loading state initially')` → fails (component doesn't exist)
**GREEN:** `export default function StudentList() { return <div>Loading...</div>; }` → passes
**REFACTOR:** Add `useState(true)`, `<LoadingSpinner>` component → still passes
