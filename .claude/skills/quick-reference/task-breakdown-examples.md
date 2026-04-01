# Task Breakdown — Bad vs Good Examples

## ❌ BAD Task Breakdown

```markdown
## PR: Implement Student CRUD

Task 1: Implement student CRUD endpoints
- File: StudentController.java
- Time: 2 hours

Problems:
❌ Not broken down (2 hours >> 5 minutes)
❌ No code samples
❌ No verification steps
❌ Unclear scope (which operations?)
❌ No dependencies listed
```

---

## ✅ GOOD Task Breakdown — Student CRUD (9 tasks, 45 min)

### Task 1: Create Student Entity (3 min)

**File:** `kiteclass-core/src/main/java/com/kiteclass/core/domain/Student.java`

**Code:**
```java
@Entity @Table(name = "students")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "instance_id = :tenantId AND deleted = false")
@Getter @Setter @NoArgsConstructor @Builder
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @NotBlank(message = "STUDENT_NAME_REQUIRED") @Size(min = 2, max = 100)
    private String name;
    @Email(message = "STUDENT_EMAIL_INVALID") @NotBlank
    private String email;
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId; // Multi-tenant
    private boolean deleted = false;
}
```

**Verify:** `./mvnw compile` → ✅ Compilation successful. ⏱️ 3 min

---

### Task 2: Create StudentRepository (2 min)

**File:** `...repository/StudentRepository.java`

**Code:**
```java
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    @Query("SELECT s FROM Student s WHERE s.id = :id AND s.deleted = false")
    Optional<Student> findByIdAndDeletedFalse(@Param("id") UUID id);

    @Query("SELECT s FROM Student s WHERE s.email = :email AND s.deleted = false")
    Optional<Student> findByEmailAndDeletedFalse(@Param("email") String email);
}
```

**Verify:** `./mvnw compile` → ✅ No errors. ⏱️ 2 min

---

### Task 3: Create StudentService Interface (2 min)

**File:** `...service/StudentService.java`

```java
public interface StudentService {
    StudentResponse createStudent(@Valid CreateStudentRequest request);
    StudentResponse getStudent(UUID id);
    Page<StudentResponse> getAllStudents(Pageable pageable);
    StudentResponse updateStudent(UUID id, @Valid UpdateStudentRequest request);
    void deleteStudent(UUID id);
}
```

**Verify:** `./mvnw compile`. ⏱️ 2 min

---

### Task 4: Implement createStudent() (5 min)

**File:** `...service/impl/StudentServiceImpl.java`

```java
@Override
public StudentResponse createStudent(CreateStudentRequest request) {
    if (studentRepository.findByEmailAndDeletedFalse(request.getEmail()).isPresent()) {
        throw new DuplicateResourceException("STUDENT_EMAIL_ALREADY_EXISTS", request.getEmail());
    }
    Student saved = studentRepository.save(studentMapper.toEntity(request));
    return studentMapper.toResponse(saved);
}
```

**Verify:** `./mvnw compile`. ⏱️ 5 min

---

### Task 5: Write Test for createStudent() — happy path (4 min)

**File:** `...test/.../StudentServiceTest.java`

```java
@Test
void createStudent_WithValidData_ShouldReturnStudentWithId() {
    CreateStudentRequest req = new CreateStudentRequest("John", "john@test.com", "CS");
    StudentResponse res = studentService.createStudent(req);
    assertThat(res.getId()).isNotNull();
    assertThat(res.getName()).isEqualTo("John");
}
```

**Verify:** `./mvnw test -Dtest=StudentServiceTest#createStudent_...` → ✅ PASS. ⏱️ 4 min

---

### Task 6: Write Test for duplicate email (3 min)

```java
@Test
void createStudent_WithDuplicateEmail_ShouldThrowException() {
    studentService.createStudent(new CreateStudentRequest("John", "john@test.com", "CS"));
    assertThatThrownBy(() ->
        studentService.createStudent(new CreateStudentRequest("Jane", "john@test.com", "Math")))
        .isInstanceOf(DuplicateResourceException.class);
}
```

**Verify:** test pass. ⏱️ 3 min

---

### Tasks 7-8: getStudent() + test, updateStudent() + test (~6 min each)

Follow same pattern: implement → verify compile → write test → verify test passes.

---

### Task 9: Create Controller Endpoint (5 min)

**File:** `...controller/StudentController.java`

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public StudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
    return studentService.createStudent(request);
}
```

**Verify:**
```bash
curl -X POST http://localhost:8081/api/v1/students \
  -H "Content-Type: application/json" -H "X-Tenant-Id: $(uuidgen)" \
  -d '{"name":"John","email":"john@test.com","major":"CS"}'
# Expected: 201 CREATED with StudentResponse JSON
```
⏱️ 5 min

---

## Task Granularity Guidelines

| Size | Example | Action |
|------|---------|--------|
| Too small (<2 min) | "Add import" + "Add annotation" (30s each) | Combine → "Create entity class (3 min)" |
| Just right (2-5 min) | "Create Entity", "Implement createStudent()" | ✅ Sweet spot |
| Too large (>10 min) | "Implement full CRUD with tests (2 hours)" | Break into 10-15 smaller tasks |
