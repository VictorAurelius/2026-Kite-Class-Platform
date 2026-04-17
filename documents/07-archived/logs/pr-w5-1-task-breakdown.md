# PR W5-1: Add Specialization to Teacher - Task Breakdown

**Feature:** Add specialization field to Teacher entity
**Total Time:** 30 minutes
**Documentation Level:** Inline (per decision matrix: <30 min → inline)

---

## Tasks (6 tasks, 30 min)

### Task 1: Create migration script (3 min)

**File:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V20__add_specialization_to_teachers.sql`

```sql
-- Add specialization field to teachers table
ALTER TABLE teachers
ADD COLUMN specialization VARCHAR(50);

CREATE INDEX idx_teachers_specialization ON teachers(specialization);
```

**Verify:** Check SQL syntax, naming convention

---

### Task 2: Update Teacher entity (3 min)

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/teacher/entity/Teacher.java`

```java
@NotBlank(message = "TEACHER_SPECIALIZATION_REQUIRED")
@Size(max = 50, message = "TEACHER_SPECIALIZATION_TOO_LONG")
@Column(name = "specialization", length = 50)
private String specialization;

// Getter + Setter
public String getSpecialization() {
    return specialization;
}

public void setSpecialization(String specialization) {
    this.specialization = specialization;
}
```

**Verify:** `./mvnw compile`

---

### Task 3: Update DTOs (5 min)

**Files:**
- `CreateTeacherRequest.java`
- `UpdateTeacherRequest.java`
- `TeacherResponse.java`

**CreateTeacherRequest:**
```java
@NotBlank(message = "TEACHER_SPECIALIZATION_REQUIRED")
@Size(max = 50, message = "TEACHER_SPECIALIZATION_TOO_LONG")
String specialization
```

**UpdateTeacherRequest** (optional for updates):
```java
@Size(max = 50, message = "TEACHER_SPECIALIZATION_TOO_LONG")
String specialization  // No @NotBlank - optional in updates
```

**TeacherResponse:**
```java
String specialization
```

**Verify:** `./mvnw compile`

---

### Task 4: Update TeacherMapper (2 min)

**File:** `TeacherMapper.java`

```java
// toEntity method - add mapping
teacher.setSpecialization(request.specialization());

// toResponse method - add mapping
specialization = teacher.getSpecialization()
```

**Verify:** `./mvnw compile`

---

### Task 5: Add i18n messages (2 min)

**File:** `kiteclass/kiteclass-core/src/main/resources/messages.properties`

```properties
# Teacher Specialization
TEACHER_SPECIALIZATION_REQUIRED=Teacher specialization is required
TEACHER_SPECIALIZATION_TOO_LONG=Teacher specialization must not exceed 50 characters
```

**File:** `messages_vi.properties`

```properties
# Teacher Specialization
TEACHER_SPECIALIZATION_REQUIRED=Chuyên môn giáo viên là bắt buộc
TEACHER_SPECIALIZATION_TOO_LONG=Chuyên môn giáo viên không được vượt quá 50 ký tự
```

**Verify:** Formatting, no typos

---

### Task 6: Add tests (15 min)

**File:** `TeacherServiceTest.java`

**Test 1: Create teacher with valid specialization**
```java
@Test
void createTeacher_WithValidSpecialization_ShouldSucceed() {
    // Arrange
    CreateTeacherRequest request = new CreateTeacherRequest(
        "John Doe",
        "john@test.com",
        "+84987654321",
        "Mathematics"  // Valid specialization
    );

    // Act
    TeacherResponse response = teacherService.create(request);

    // Assert
    assertThat(response.specialization()).isEqualTo("Mathematics");
}
```

**Test 2: Create teacher without specialization (validation)**
```java
@Test
void createTeacher_WithoutSpecialization_ShouldFail() {
    // Arrange
    CreateTeacherRequest request = new CreateTeacherRequest(
        "John Doe",
        "john@test.com",
        "+84987654321",
        ""  // Empty specialization
    );

    // Act & Assert
    assertThatThrownBy(() -> teacherService.create(request))
        .isInstanceOf(ValidationException.class)
        .satisfies(e -> assertThat(e.getMessage())
            .containsIgnoringCase("TEACHER_SPECIALIZATION_REQUIRED"));
}
```

**Test 3: Create teacher with too long specialization**
```java
@Test
void createTeacher_WithTooLongSpecialization_ShouldFail() {
    // Arrange
    String tooLong = "A".repeat(51);  // 51 chars
    CreateTeacherRequest request = new CreateTeacherRequest(
        "John Doe",
        "john@test.com",
        "+84987654321",
        tooLong
    );

    // Act & Assert
    assertThatThrownBy(() -> teacherService.create(request))
        .isInstanceOf(ValidationException.class)
        .satisfies(e -> assertThat(e.getMessage())
            .containsIgnoringCase("TEACHER_SPECIALIZATION_TOO_LONG"));
}
```

**Test 4: Update teacher specialization**
```java
@Test
void updateTeacher_WithNewSpecialization_ShouldSucceed() {
    // Arrange
    TeacherResponse created = createTeacher("John Doe", "john@test.com", "+84987654321", "Mathematics");
    UpdateTeacherRequest updateRequest = new UpdateTeacherRequest(
        "John Doe",
        "john@test.com",
        "+84987654321",
        "Physics"  // Change specialization
    );

    // Act
    TeacherResponse updated = teacherService.update(created.id(), updateRequest);

    // Assert
    assertThat(updated.specialization()).isEqualTo("Physics");
}
```

**Verify:** `./mvnw test -Dtest=TeacherServiceTest`

---

## Summary

**Total Tasks:** 6
**Total Time:** 30 minutes
**Average per Task:** 5 minutes

**Breakdown Approach:** Inline (no separate .md file overhead)

**Skills Applied:**
- ✅ Quick Brainstorm (5 min)
- ✅ Task Breakdown (inline, per refinement)
- ✅ TDD workflow (tests in Task 6)

---

**Time Spent on Breakdown:** 0 minutes (inline documentation)
**Next:** Implementation with TDD workflow
