# Pilot PR 1: Add Phone Number to Student - Task Breakdown

**Date:** 2026-03-13
**Feature:** Add phone_number field to Student
**Estimated Total:** 35 minutes (9 tasks)
**Skill Applied:** Task Breakdown Formula

---

## Task List (Bottom-Up Approach)

### Task 1: Create database migration

**File:** `kiteclass/kiteclass-core/src/main/resources/db/migration/V15__add_student_phone_number.sql`

**Change:** Add phone_number column to students table

**Code:**
```sql
-- V15: Add phone number field to students
ALTER TABLE students
ADD COLUMN phone_number VARCHAR(20);

COMMENT ON COLUMN students.phone_number IS 'Student contact phone number (E.164 format, optional)';
```

**Verification:**
```bash
# Migration will run on next startup
# Or test with: ./mvnw flyway:migrate
```

**Time:** 2 minutes

---

### Task 2: Add phoneNumber field to Student entity

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/entity/Student.java`

**Change:** Add phoneNumber field with validation annotation

**Code:**
```java
@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "STUDENT_PHONE_INVALID")
@Column(name = "phone_number", length = 20)
private String phoneNumber;
```

**Verification:**
```bash
# Compile check
./mvnw compile
# Expected: ✅ Compiles without errors
```

**Time:** 2 minutes

---

### Task 3: Add phone to CreateStudentRequest DTO

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/dto/CreateStudentRequest.java`

**Change:** Add phoneNumber field with same validation

**Code:**
```java
@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "STUDENT_PHONE_INVALID")
private String phoneNumber;
```

**Verification:**
```bash
./mvnw compile
# Expected: ✅ DTO compiles
```

**Time:** 2 minutes

---

### Task 4: Add phone to UpdateStudentRequest DTO

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/dto/UpdateStudentRequest.java`

**Change:** Add phoneNumber field (optional for update)

**Code:**
```java
@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "STUDENT_PHONE_INVALID")
@Size(max = 20, message = "STUDENT_PHONE_TOO_LONG")
private String phoneNumber;
```

**Verification:**
```bash
./mvnw compile
```

**Time:** 2 minutes

---

### Task 5: Add phone to StudentResponse DTO

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/dto/StudentResponse.java`

**Change:** Add phoneNumber to record

**Code:**
```java
public record StudentResponse(
    UUID id,
    String name,
    String email,
    String major,
    String phoneNumber,  // Add this
    LocalDate enrollmentDate,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

**Verification:**
```bash
./mvnw compile
```

**Time:** 2 minutes

---

### Task 6: Update StudentMapper to include phoneNumber

**File:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/mapper/StudentMapper.java`

**Change:** Ensure MapStruct maps phoneNumber field

**Code:**
```java
// MapStruct auto-maps matching field names
// Just verify @Mapping annotations if needed
// No explicit mapping needed for phoneNumber (same name in all)
```

**Verification:**
```bash
./mvnw clean compile
# MapStruct will generate mapping code
# Check: target/generated-sources/annotations/.../StudentMapperImpl.java
```

**Time:** 2 minutes

---

### Task 7: Write test for valid phone number (TDD - RED)

**File:** `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/student/service/StudentServiceTest.java`

**Change:** Add test for creating student with valid phone

**Code:**
```java
@Test
void createStudent_WithValidPhoneNumber_ShouldSucceed() {
    // Arrange
    CreateStudentRequest request = new CreateStudentRequest(
        "John Doe",
        "john@test.com",
        "Computer Science",
        "+84123456789",  // Valid E.164 format
        LocalDate.now()
    );

    // Act
    StudentResponse response = studentService.createStudent(request);

    // Assert
    assertThat(response.phoneNumber()).isEqualTo("+84123456789");
}
```

**Verification:**
```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithValidPhoneNumber_ShouldSucceed
# Expected: ✅ PASS (if Student already supports phone field)
```

**Time:** 4 minutes

---

### Task 8: Write test for invalid phone number (TDD - RED)

**File:** Same as Task 7

**Change:** Add test for validation failure

**Code:**
```java
@Test
void createStudent_WithInvalidPhoneNumber_ShouldThrowValidationException() {
    // Arrange
    CreateStudentRequest request = new CreateStudentRequest(
        "John Doe",
        "john@test.com",
        "Computer Science",
        "invalid-phone",  // Invalid format
        LocalDate.now()
    );

    // Act & Assert
    assertThatThrownBy(() -> studentService.createStudent(request))
        .isInstanceOf(ConstraintViolationException.class)
        .satisfies(e -> assertThat(e.getMessage())
            .containsIgnoringCase("STUDENT_PHONE_INVALID"));
}
```

**Verification:**
```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithInvalidPhoneNumber_ShouldThrowValidationException
# Expected: ✅ PASS (validation kicks in)
```

**Time:** 4 minutes

---

### Task 9: Write test for null phone number (edge case)

**File:** Same as Task 7

**Change:** Add test for optional field (null allowed)

**Code:**
```java
@Test
void createStudent_WithNullPhoneNumber_ShouldSucceed() {
    // Arrange
    CreateStudentRequest request = new CreateStudentRequest(
        "John Doe",
        "john@test.com",
        "Computer Science",
        null,  // Null phone - should be allowed
        LocalDate.now()
    );

    // Act
    StudentResponse response = studentService.createStudent(request);

    // Assert
    assertThat(response.phoneNumber()).isNull();
}
```

**Verification:**
```bash
./mvnw test -Dtest=StudentServiceTest#createStudent_WithNullPhoneNumber_ShouldSucceed
# Expected: ✅ PASS
```

**Time:** 3 minutes

---

### Task 10: Add phone to error messages

**File:** `kiteclass/kiteclass-core/src/main/resources/messages.properties`

**Change:** Add i18n message for phone validation

**Code:**
```properties
# Student phone validation
STUDENT_PHONE_INVALID=Phone number must be in E.164 format (e.g., +84123456789)
STUDENT_PHONE_TOO_LONG=Phone number must not exceed 20 characters
```

**Verification:**
```bash
# Messages will be loaded by MessageSource
# Verify in test run
```

**Time:** 2 minutes

---

### Task 11: Run all tests and verify

**File:** N/A (verification task)

**Change:** Run full test suite

**Code:** N/A

**Verification:**
```bash
cd kiteclass/kiteclass-core
./mvnw clean test
# Expected: ✅ All tests pass
# Coverage check: Should maintain >=80%
```

**Time:** 5 minutes

---

### Task 12: Update API documentation (if needed)

**File:** N/A (documentation task)

**Change:** Verify Swagger/OpenAPI includes phone field

**Verification:**
```bash
# Start app and check Swagger UI
# http://localhost:8081/swagger-ui.html
# Verify StudentResponse shows phoneNumber field
```

**Time:** 3 minutes

---

## Summary

**Total Tasks:** 12
**Total Estimated Time:** 35 minutes
**Average Time per Task:** 2.9 minutes ✅

**Task Ordering:** Bottom-Up (Database → Entity → DTOs → Tests)

**Skills Applied:**
- ✅ Task Breakdown Formula
- ✅ 2-5 minute task sizing (range: 2-5 min, average 2.9 min)
- ✅ Exact file paths provided
- ✅ Code samples copy-paste ready
- ✅ Verification commands included

**Next Step:** Implement using TDD (RED-GREEN-REFACTOR)
