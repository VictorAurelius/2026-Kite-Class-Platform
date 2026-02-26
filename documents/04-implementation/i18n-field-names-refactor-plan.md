# I18n Field Names Refactor Plan

**Created**: 2026-02-26
**Status**: Planning
**Priority**: High (Technical Debt)

## Problem Statement

Current implementation hardcodes Vietnamese field names directly in Java code, violating i18n best practices:

```java
// ❌ CURRENT (WRONG)
if (course.getDescription() == null) {
    missingFields.append("Mô tả, ");  // Hardcoded Vietnamese
}
```

**Issues**:
- Cannot support multiple languages (English, Vietnamese, Chinese, etc.)
- Field names are not centralized in messages.properties
- Violates DRY principle
- Not maintainable or scalable

## Best Practice Solution

Use message keys with MessageSource for field names:

```java
// ✅ CORRECT
private static final String FIELD_DESCRIPTION = "field.course.description";

if (course.getDescription() == null) {
    missingFieldKeys.add(FIELD_DESCRIPTION);
}

// Resolve to localized string
String missing = missingFieldKeys.stream()
    .map(key -> messageSource.getMessage(key, null, locale))
    .collect(Collectors.joining(", "));
```

**Benefits**:
- Support multi-language out of the box
- Centralized in messages.properties (EN) and messages_vi.properties (VI)
- Easy to add new languages (messages_zh.properties, etc.)
- Maintainable and testable

## Scope

Refactor all modules with validation that build error messages with field names:

### Modules to Refactor

1. **Student Module** (`StudentServiceImpl.java`)
   - Field names: name, email, phone, dateOfBirth, etc.
   - Used in validation errors

2. **Teacher Module** (`TeacherServiceImpl.java`)
   - Field names: name, email, phone, specialization, etc.
   - Used in validation errors

3. **Course Module** (`CourseServiceImpl.java`)
   - Field names: name, description, syllabus, objectives, durationWeeks
   - Used in: `validatePublishRequirements()`

4. **Class Module** (`ClassServiceImpl.java`)
   - Field names: name, code, startDate, endDate, maxStudents, etc.
   - Used in validation errors

5. **Attendance Module** (if exists)
   - Field names: date, status, etc.

## Implementation Steps

### Step 1: Define Field Name Message Keys

**File**: `messages.properties` (English)

```properties
# ============================================================================
# Field Names - English
# ============================================================================

# Student Fields
field.student.name=Name
field.student.email=Email
field.student.phone=Phone
field.student.dateOfBirth=Date of Birth
field.student.address=Address
field.student.emergencyContact=Emergency Contact

# Teacher Fields
field.teacher.name=Name
field.teacher.email=Email
field.teacher.phone=Phone
field.teacher.specialization=Specialization
field.teacher.bio=Biography
field.teacher.qualifications=Qualifications

# Course Fields
field.course.name=Name
field.course.code=Code
field.course.description=Description
field.course.syllabus=Syllabus
field.course.objectives=Objectives
field.course.prerequisites=Prerequisites
field.course.targetAudience=Target Audience
field.course.durationWeeks=Duration (weeks)
field.course.totalSessions=Total Sessions
field.course.price=Price
field.course.coverImageUrl=Cover Image URL

# Class Fields
field.class.name=Class Name
field.class.code=Class Code
field.class.startDate=Start Date
field.class.endDate=End Date
field.class.schedule=Schedule
field.class.maxStudents=Maximum Students
field.class.room=Room
```

**File**: `messages_vi.properties` (Vietnamese)

```properties
# ============================================================================
# Field Names - Vietnamese
# ============================================================================

# Student Fields
field.student.name=Tên học viên
field.student.email=Email
field.student.phone=Số điện thoại
field.student.dateOfBirth=Ngày sinh
field.student.address=Địa chỉ
field.student.emergencyContact=Liên hệ khẩn cấp

# Teacher Fields
field.teacher.name=Tên giảng viên
field.teacher.email=Email
field.teacher.phone=Số điện thoại
field.teacher.specialization=Chuyên môn
field.teacher.bio=Giới thiệu
field.teacher.qualifications=Bằng cấp

# Course Fields
field.course.name=Tên khóa học
field.course.code=Mã khóa học
field.course.description=Mô tả
field.course.syllabus=Giáo trình
field.course.objectives=Mục tiêu học tập
field.course.prerequisites=Yêu cầu đầu vào
field.course.targetAudience=Đối tượng học viên
field.course.durationWeeks=Thời lượng (tuần)
field.course.totalSessions=Tổng số buổi học
field.course.price=Học phí
field.course.coverImageUrl=Ảnh bìa (URL)

# Class Fields
field.class.name=Tên lớp học
field.class.code=Mã lớp học
field.class.startDate=Ngày bắt đầu
field.class.endDate=Ngày kết thúc
field.class.schedule=Lịch học
field.class.maxStudents=Số học viên tối đa
field.class.room=Phòng học
```

### Step 2: Add Field Name Constants to Each Service

**Pattern** (apply to all service implementations):

```java
public class CourseServiceImpl implements CourseService {

    private final MessageSource messageSource;

    // Field name message keys
    private static final String FIELD_NAME = "field.course.name";
    private static final String FIELD_DESCRIPTION = "field.course.description";
    private static final String FIELD_SYLLABUS = "field.course.syllabus";
    private static final String FIELD_OBJECTIVES = "field.course.objectives";
    private static final String FIELD_DURATION_WEEKS = "field.course.durationWeeks";

    // ... rest of class
}
```

### Step 3: Refactor Validation Methods

**Before** (hardcoded):
```java
private void validatePublishRequirements(Course course) {
    StringBuilder missingFields = new StringBuilder();

    if (course.getDescription() == null || course.getDescription().isBlank()) {
        missingFields.append("Mô tả, ");  // ❌ Hardcoded
    }
    // ...

    String missing = missingFields.substring(0, missingFields.length() - 2);
    throw new ValidationException("COURSE_MISSING_REQUIRED_FIELDS", (Object) missing);
}
```

**After** (i18n-ready):
```java
private void validatePublishRequirements(Course course) {
    List<String> missingFieldKeys = new ArrayList<>();

    if (course.getName() == null || course.getName().isBlank()) {
        missingFieldKeys.add(FIELD_NAME);
    }
    if (course.getDescription() == null || course.getDescription().isBlank()) {
        missingFieldKeys.add(FIELD_DESCRIPTION);
    }
    if (course.getSyllabus() == null || course.getSyllabus().isBlank()) {
        missingFieldKeys.add(FIELD_SYLLABUS);
    }
    if (course.getObjectives() == null || course.getObjectives().isBlank()) {
        missingFieldKeys.add(FIELD_OBJECTIVES);
    }
    if (course.getDurationWeeks() == null || course.getDurationWeeks() <= 0) {
        missingFieldKeys.add(FIELD_DURATION_WEEKS);
    }

    if (!missingFieldKeys.isEmpty()) {
        // Resolve field names to localized strings
        Locale locale = LocaleContextHolder.getLocale();
        String missing = missingFieldKeys.stream()
            .map(key -> messageSource.getMessage(key, null, locale))
            .collect(Collectors.joining(", "));

        throw new ValidationException("COURSE_MISSING_REQUIRED_FIELDS", (Object) missing);
    }
}
```

### Step 4: Update Tests

Ensure tests still pass after refactoring:

```java
@Test
void shouldThrowValidationExceptionWhenMissingRequiredFields() {
    // Given
    Course course = new Course();
    course.setName("Test");
    // Missing: description, syllabus, objectives, durationWeeks

    // When/Then
    assertThatThrownBy(() -> courseService.publish(course.getId()))
        .isInstanceOf(ValidationException.class)
        .satisfies(e -> {
            ValidationException ex = (ValidationException) e;
            assertThat(ex.getCode()).isEqualTo("COURSE_MISSING_REQUIRED_FIELDS");
            // Field names should be localized based on Accept-Language
            assertThat(ex.getMessage()).containsAnyOf("Mô tả", "Description");
        });
}
```

## File Changes Summary

### Modified Files

**Backend** (`kiteclass-core`):
- `src/main/resources/messages.properties` - Add field name keys (EN)
- `src/main/resources/messages_vi.properties` - Add field name keys (VI)
- `src/main/java/com/kiteclass/core/module/student/service/impl/StudentServiceImpl.java`
- `src/main/java/com/kiteclass/core/module/teacher/service/impl/TeacherServiceImpl.java`
- `src/main/java/com/kiteclass/core/module/course/service/impl/CourseServiceImpl.java`
- `src/main/java/com/kiteclass/core/module/class/service/impl/ClassServiceImpl.java`

**Tests**:
- Update tests to verify localized field names (if needed)

## Acceptance Criteria

- [ ] All field names moved from hardcoded strings to message keys
- [ ] Field name constants defined in each service implementation
- [ ] Both English and Vietnamese messages complete
- [ ] All validation methods refactored to use MessageSource
- [ ] All existing tests pass
- [ ] Manual test: Error messages show correct language based on Accept-Language header
- [ ] Code follows DRY principle (no duplicate field name strings)

## Testing Strategy

### 1. Unit Tests
- Verify validation methods throw correct exceptions
- Verify field names are localized based on locale

### 2. Integration Tests
- Test with `Accept-Language: en` → English field names
- Test with `Accept-Language: vi` → Vietnamese field names
- Test with missing language → Fallback to English

### 3. Manual Browser Test
- Set browser language to English → Error messages in English
- Set browser language to Vietnamese → Error messages in Vietnamese
- Verify field names in error toasts are correct

## Future Enhancements

After this refactor, adding a new language (e.g., Chinese) is trivial:

1. Create `messages_zh.properties`
2. Add Chinese translations for all field names
3. Done! No code changes needed.

## Rollback Plan

If issues arise:
1. Revert commits
2. Use previous hardcoded Vietnamese field names
3. File bug report with details

## Estimated Effort

- **Messages.properties updates**: 30 minutes
- **Service refactoring**: 1 hour (4 modules × 15 min each)
- **Testing**: 30 minutes
- **Total**: ~2 hours

## Success Metrics

- ✅ Zero hardcoded field names in Java code
- ✅ All field names centralized in messages.properties
- ✅ Support for English and Vietnamese
- ✅ Easy to add new languages
- ✅ All tests passing
