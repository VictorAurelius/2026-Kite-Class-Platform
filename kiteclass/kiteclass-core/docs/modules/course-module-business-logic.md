# Course Module - Business Logic

**Service:** kiteclass-core
**Module:** Course Management
**Priority:** P0 (Required after Teacher Module)
**Status:** Design Phase
**Version:** 1.0.0
**Date:** 2026-01-28

---

## 📋 1. Tổng Quan Module

### Mục đích

Module Course quản lý thông tin khóa học trong hệ thống KiteClass.

**Vai trò trong hệ thống:**
- Lưu trữ thông tin khóa học (course metadata, syllabus, objectives)
- Quản lý course lifecycle: DRAFT → PUBLISHED → ARCHIVED
- Quản lý course-teacher relationship (qua TeacherCourse)
- Quản lý course-class relationship (1 course → nhiều classes)
- Hỗ trợ course categorization và filtering
- Foundation cho Class, Enrollment, và Teaching workflow

### Phạm vi (Scope)

**Trong phạm vi:**
- ✅ CRUD operations cho Course entity
- ✅ Course lifecycle management (draft, publish, archive)
- ✅ Course categorization (level, subject, tags)
- ✅ Course-Teacher assignment (CREATOR, INSTRUCTOR, ASSISTANT)
- ✅ Course-Class relationship (1-to-many)
- ✅ Course syllabus và learning objectives
- ✅ Course pricing và duration
- ✅ Internal APIs cho other modules

**Ngoài phạm vi:**
- ❌ Course content management (videos, lessons) - Future module
- ❌ Course reviews và ratings - Future feature
- ❌ Course certificates - Future feature
- ❌ Course bundles và promotions - Future feature

### Business Context

**Real-World Scenarios:**

**Scenario 1: Language Center tạo Course Structure**
```
Course: "English for Business Communication"
├── Level: Intermediate (B1-B2)
├── Duration: 12 weeks
├── Price: 5,000,000 VND
├── Teachers:
│   ├── Teacher A (CREATOR)
│   └── Teacher B (INSTRUCTOR)
├── Classes:
│   ├── Class A (Mon-Wed-Fri 18:00-20:00)
│   ├── Class B (Tue-Thu 19:00-21:00)
│   └── Class C (Weekend 09:00-12:00)
└── Students: Enroll vào 1 trong 3 classes

Flow:
1. ADMIN/Teacher tạo course → status = DRAFT
2. Add syllabus, objectives, materials
3. Assign teachers (CREATOR, INSTRUCTOR)
4. Publish course → status = PUBLISHED
5. Create classes cho course
6. Students enroll vào classes
```

**Scenario 2: Independent Teacher tạo Course**
```
Teacher: Cô Mai (OWNER + TEACHER)
Course: "TOEIC 600+ Preparation"
├── Status: DRAFT
├── Teacher: Cô Mai (CREATOR - auto assigned)
├── Classes: Chưa có (sẽ tạo sau)

Flow:
1. Cô Mai login và tạo course
2. Cô Mai tự động là CREATOR
3. Cô Mai design syllabus
4. Cô Mai publish course
5. Cô Mai tạo classes
6. Students tự enroll qua class code
```

### Priority

- **Priority:** P0 (Critical)
- **Reason:**
  - Foundation cho Class Module
  - Required cho Enrollment workflow
  - Core business entity
  - Blocking nhiều features

---

## 🏗️ 2. Thực Thể Nghiệp Vụ

### 2.1. Course Entity

**Table:** `courses`

**Mô tả:** Lưu trữ thông tin khóa học.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| name | VARCHAR(200) | NO | Tên khóa học | 5-200 chars |
| description | TEXT | YES | Mô tả khóa học | Max 5000 chars |
| subject | VARCHAR(100) | YES | Môn học (English, Math, Physics, etc.) | Max 100 chars |
| level | VARCHAR(50) | NO | Level (BEGINNER, INTERMEDIATE, ADVANCED) | Enum |
| duration_weeks | INT | YES | Thời lượng khóa học (weeks) | >= 1 |
| max_students | INT | YES | Số học sinh tối đa per class | >= 1 |
| price | DECIMAL(15,2) | YES | Giá khóa học (VND) | >= 0 |
| syllabus | TEXT | YES | Nội dung syllabus | Max 10000 chars |
| objectives | TEXT | YES | Learning objectives | Max 5000 chars |
| prerequisites | TEXT | YES | Điều kiện tiên quyết | Max 2000 chars |
| thumbnail_url | VARCHAR(500) | YES | URL ảnh thumbnail | Valid URL |
| status | VARCHAR(20) | NO | Status (DRAFT, PUBLISHED, ARCHIVED) | Enum |
| created_by | BIGINT | YES | Teacher ID người tạo | FK to teachers.id |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian cập nhật | Auto-update |
| published_at | TIMESTAMP | YES | Thời gian publish | Set khi publish |
| archived_at | TIMESTAMP | YES | Thời gian archive | Set khi archive |

**Indexes:**
```sql
CREATE INDEX idx_courses_status ON courses(status);
CREATE INDEX idx_courses_subject ON courses(subject);
CREATE INDEX idx_courses_level ON courses(level);
CREATE INDEX idx_courses_created_by ON courses(created_by);
CREATE INDEX idx_courses_name ON courses(name);
```

**Status Values:**
- `DRAFT`: Đang soạn thảo, chưa public
- `PUBLISHED`: Đã public, students có thể enroll
- `ARCHIVED`: Đã archive, không nhận students mới

**Level Values:**
- `BEGINNER`: Cơ bản
- `ELEMENTARY`: Sơ cấp
- `INTERMEDIATE`: Trung cấp
- `UPPER_INTERMEDIATE`: Trung cấp cao
- `ADVANCED`: Nâng cao
- `PROFICIENCY`: Thành thạo

### 2.2. TeacherCourse Entity (Reference)

**Table:** `teacher_courses`

**Mô tả:** Quản lý assignment giữa teachers và courses. Chi tiết trong Teacher Module.

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| teacher_id | BIGINT | FK to teachers.id |
| course_id | BIGINT | FK to courses.id |
| role | VARCHAR(20) | CREATOR, INSTRUCTOR, ASSISTANT |
| assigned_at | TIMESTAMP | Thời gian assign |
| assigned_by | VARCHAR(100) | Người assign (NULL nếu self-created) |

**Relationship:**
```
Course 1 ──── * TeacherCourse * ──── 1 Teacher

Roles:
- CREATOR: Teacher tạo course, full control
- INSTRUCTOR: Teacher được assign dạy course
- ASSISTANT: Teacher phụ, support role
```

### 2.3. Class Entity (Reference)

**Table:** `classes`

**Mô tả:** Lớp học cụ thể trong course. Chi tiết trong Class Module.

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| course_id | BIGINT | FK to courses.id |
| name | VARCHAR(200) | Tên lớp |
| schedule | VARCHAR(100) | Lịch học (Mon-Wed-Fri 18:00-20:00) |
| max_students | INT | Max students cho class này |
| status | VARCHAR(20) | UPCOMING, ONGOING, COMPLETED, CANCELLED |

**Relationship:**
```
Course 1 ──── * Class

Logic:
- 1 Course có nhiều Classes
- Classes là instances cụ thể của Course với schedule riêng
- Example: "English B1" course có 3 classes với 3 schedules khác nhau
```

---

## 📐 3. Quy Tắc Kinh Doanh

### BR-COURSE-001: Course Name Phải Duy Nhất Cho Mỗi Teacher

**Mô tả:** Một teacher không thể tạo 2 courses cùng tên (case-insensitive).

**Lý do:** Tránh confusion khi teacher quản lý nhiều courses.

**Validation:**
```java
boolean exists = courseRepository
    .existsByNameIgnoreCaseAndCreatedBy(name, teacherId);
if (exists) {
    throw new DuplicateResourceException(
        "Course với tên này đã tồn tại cho teacher này"
    );
}
```

**Note:** Teachers khác nhau có thể tạo courses cùng tên.

---

### BR-COURSE-002: Course Phải Có Ít Nhất 1 Teacher (CREATOR)

**Mô tả:** Mỗi course phải có ít nhất 1 teacher với role = CREATOR.

**Lý do:** Course không thể tồn tại không có teacher ownership.

**Implementation:**
- Khi tạo course → Auto tạo TeacherCourse (CREATOR)
- Không cho remove CREATOR cuối cùng

---

### BR-COURSE-003: Chỉ DRAFT Courses Mới Có Thể Edit Freely

**Mô tả:** Courses ở status DRAFT có thể edit tự do. PUBLISHED courses giới hạn edits.

**Lý do:** PUBLISHED courses đã có students enrolled → Changes impact students.

**Rules:**
```
DRAFT status:
✅ Can edit: name, description, syllabus, objectives, price, duration
✅ Can delete: Yes (nếu chưa có classes)

PUBLISHED status:
✅ Can edit: description, syllabus, objectives (minor updates)
⚠️ Can edit with warning: price (affect new enrollments)
❌ Cannot edit: name, duration (breaking changes)
❌ Cannot delete: No (phải archive)

ARCHIVED status:
❌ Cannot edit: Read-only
❌ Cannot delete: No
```

---

### BR-COURSE-004: Không Thể Publish Course Rỗng

**Mô tả:** Course phải có đủ thông tin trước khi publish.

**Lý do:** Students cần thông tin đầy đủ để quyết định enroll.

**Required fields để publish:**
```java
boolean canPublish = course.getName() != null &&
                     course.getDescription() != null &&
                     course.getLevel() != null &&
                     course.getDurationWeeks() != null &&
                     course.getDurationWeeks() > 0 &&
                     course.getSyllabus() != null &&
                     course.getObjectives() != null;
```

---

### BR-COURSE-005: ARCHIVED Courses Không Nhận Students Mới

**Mô tả:** Courses ở status ARCHIVED không thể tạo classes mới hoặc nhận enrollments mới.

**Lý do:** Archived courses không còn active.

**Note:** Students đã enroll trước khi archive vẫn có thể học tiếp.

---

### BR-COURSE-006: Price Phải >= 0

**Mô tả:** Giá khóa học phải >= 0 (free hoặc paid).

**Lý do:** Không có giá âm.

**Implementation:**
```java
if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
    throw new ValidationException("Price không thể âm");
}
```

**Note:** Price = 0 hoặc NULL = Free course

---

### BR-COURSE-007: Duration Weeks Phải > 0

**Mô tả:** Thời lượng khóa học phải > 0 nếu có.

**Lý do:** Không có course 0 tuần.

**Validation:**
```java
if (durationWeeks != null && durationWeeks <= 0) {
    throw new ValidationException("Duration phải > 0");
}
```

---

## 🎯 4. Use Cases

### Overview

Module Course hỗ trợ full lifecycle management của khóa học:

**Course Creation & Management:**
- UC-COURSE-001: Create Course (Draft)
- UC-COURSE-002: Update Course
- UC-COURSE-003: Publish Course
- UC-COURSE-004: Archive Course
- UC-COURSE-005: Delete Course

**Course Discovery:**
- UC-COURSE-006: Get Course Details
- UC-COURSE-007: List/Search Courses
- UC-COURSE-008: Filter Courses by Subject/Level

**Course-Teacher Management:**
- UC-COURSE-009: Add Teacher to Course (Reference UC-TEACHER-003)
- UC-COURSE-010: Remove Teacher from Course

**Course-Class Management:**
- UC-COURSE-011: Create Class in Course (Reference UC-CLASS-001)
- UC-COURSE-012: List Classes in Course

---

### UC-COURSE-001: Tạo Course (Draft)

**Người thực hiện:** TEACHER (nếu có quyền), ADMIN/OWNER

**Mục đích:** Tạo course mới ở status DRAFT

**Điều kiện trước:**
- User có quyền create courses
- Teacher có status = ACTIVE (nếu teacher tạo)

**Luồng chính:**

1. User truy cập Course Management
2. User click "Tạo khóa học mới"
3. Frontend hiển thị form:
   - Course name (required)
   - Subject (English, Math, etc.)
   - Level (BEGINNER, INTERMEDIATE, ADVANCED)
   - Description
   - Duration (weeks)
   - Max students per class
   - Price
4. User điền thông tin cơ bản và submit
5. Frontend gửi POST `/api/v1/courses`
   ```json
   {
     "name": "English for Business Communication",
     "subject": "English",
     "level": "INTERMEDIATE",
     "description": "Learn professional English for business contexts",
     "durationWeeks": 12,
     "maxStudents": 20,
     "price": 5000000
   }
   ```
6. Hệ thống validate:
   - **BR-COURSE-001:** Name unique cho teacher này
   - Name không rỗng
   - Level valid
   - **BR-COURSE-006:** Price >= 0 nếu có
   - **BR-COURSE-007:** Duration > 0 nếu có
7. Hệ thống tạo Course:
   - status = DRAFT
   - created_by = userId (nếu teacher)
8. Hệ thống tạo TeacherCourse:
   - teacher_id = userId (nếu teacher)
   - course_id = newCourseId
   - role = CREATOR
   - assigned_by = NULL (self-created)
9. Hệ thống lưu database
10. Hệ thống trả về HTTP 201 Created với CourseResponse
11. Frontend redirect đến Course Detail page (edit mode)
12. User thấy: "Khóa học đã được tạo. Bạn có thể tiếp tục chỉnh sửa."

**Luồng thay thế:**

**AF1 - Name trùng:**
- Tại bước 6, course name đã tồn tại cho teacher này
- Trả về HTTP 409 Conflict
- Message: "Bạn đã có khóa học với tên này"

**AF2 - Validation failed:**
- Tại bước 6, price < 0 hoặc duration <= 0
- Trả về HTTP 400 Bad Request
- Message chi tiết lỗi validation

**Kết quả:**
- Course được tạo với status = DRAFT
- TeacherCourse (CREATOR) được tạo
- User có full control để edit course
- Course chưa visible cho students (DRAFT)

**Events:**
- Event: `COURSE_CREATED` (courseId, teacherId, courseName, status=DRAFT)

---

### UC-COURSE-002: Update Course

**Người thực hiện:** CREATOR, ADMIN/OWNER

**Mục đích:** Cập nhật thông tin course

**Điều kiện trước:**
- User có quyền edit course (CREATOR hoặc ADMIN)
- Course tồn tại

**Luồng chính:**

1. User truy cập Course Detail page
2. User click "Chỉnh sửa"
3. Frontend hiển thị edit form với data hiện tại
4. User update các fields:
   - Description
   - Syllabus
   - Objectives
   - Prerequisites
   - Thumbnail
   - Price (nếu DRAFT)
   - Duration (nếu DRAFT)
5. User submit
6. Frontend gửi PUT `/api/v1/courses/{courseId}`
   ```json
   {
     "description": "Updated description",
     "syllabus": "Week 1: Introduction\nWeek 2: ...",
     "objectives": "Students will be able to...",
     "price": 5500000
   }
   ```
7. Hệ thống validate:
   - User có quyền edit
   - **BR-COURSE-003:** Check status và allowed fields
   - Nếu PUBLISHED: Chỉ cho edit certain fields
8. Hệ thống update Course
9. Hệ thống trả về HTTP 200 OK
10. Frontend update UI
11. Nếu PUBLISHED và price changed: Notify students

**Luồng thay thế:**

**AF1 - Không có quyền:**
- Tại bước 7, user không phải CREATOR và không phải ADMIN
- Trả về HTTP 403 Forbidden
- Message: "Bạn không có quyền chỉnh sửa course này"

**AF2 - PUBLISHED course breaking changes:**
- Tại bước 7, user cố update name/duration của PUBLISHED course
- Trả về HTTP 400 Bad Request
- Message: "Không thể thay đổi {field} của PUBLISHED course"

**AF3 - ARCHIVED course:**
- Tại bước 7, course status = ARCHIVED
- Trả về HTTP 400 Bad Request
- Message: "Không thể chỉnh sửa ARCHIVED course"

**Kết quả:**
- Course được update
- Changes reflected trong course detail
- Nếu PUBLISHED: Students được notify về major changes

**Events:**
- Event: `COURSE_UPDATED` (courseId, updatedFields, userId)

---

### UC-COURSE-003: Publish Course

**Người thực hiện:** CREATOR, ADMIN/OWNER

**Mục đích:** Publish course từ DRAFT sang PUBLISHED để students có thể enroll

**Điều kiện trước:**
- User có quyền publish (CREATOR hoặc ADMIN)
- Course status = DRAFT
- Course có đủ thông tin required

**Luồng chính:**

1. User truy cập DRAFT Course Detail page
2. User click "Publish khóa học"
3. Frontend hiển thị confirmation dialog:
   - "Bạn có chắc muốn publish course này?"
   - "Sau khi publish, một số thông tin không thể thay đổi"
   - Checklist: ✅ Name, ✅ Syllabus, ✅ Objectives, etc.
4. User confirm
5. Frontend gửi POST `/api/v1/courses/{courseId}/publish`
6. Hệ thống validate:
   - **BR-COURSE-004:** Course có đủ thông tin
   - User có quyền publish
   - Status hiện tại = DRAFT
7. Hệ thống update:
   - status = DRAFT → PUBLISHED
   - published_at = NOW()
8. Hệ thống trả về HTTP 200 OK
9. Frontend hiển thị: "Khóa học đã được publish"
10. Course xuất hiện trong course catalog
11. Students có thể browse và enroll

**Luồng thay thế:**

**AF1 - Thiếu thông tin required:**
- Tại bước 6, course thiếu syllabus hoặc objectives
- Trả về HTTP 400 Bad Request
- Message: "Không thể publish. Thiếu: syllabus, objectives"

**AF2 - Already PUBLISHED:**
- Tại bước 6, course đã PUBLISHED
- Trả về HTTP 409 Conflict
- Message: "Course đã được publish"

**Kết quả:**
- Course status = PUBLISHED
- Course visible trong catalog
- Students có thể enroll vào classes
- published_at timestamp được set

**Events:**
- Event: `COURSE_PUBLISHED` (courseId, courseName, publishedBy)

---

### UC-COURSE-004: Archive Course

**Người thực hiện:** CREATOR, ADMIN/OWNER

**Mục đích:** Archive course không còn active

**Điều kiện trước:**
- User có quyền archive
- Course status = PUBLISHED

**Luồng chính:**

1. User truy cập Course Detail page
2. User click "Archive khóa học"
3. Frontend hiển thị confirmation:
   - "Archive course này?"
   - "Students đã enroll vẫn có thể học tiếp"
   - "Course sẽ không nhận enrollments mới"
4. User confirm
5. Frontend gửi POST `/api/v1/courses/{courseId}/archive`
6. Hệ thống validate:
   - User có quyền archive
   - Course status = PUBLISHED
7. Hệ thống update:
   - status = PUBLISHED → ARCHIVED
   - archived_at = NOW()
8. Hệ thống trả về HTTP 200 OK
9. Frontend hiển thị: "Course đã được archive"
10. Course biến mất khỏi catalog
11. Existing students vẫn access được classes

**Luồng thay thế:**

**AF1 - Already ARCHIVED:**
- Tại bước 6, course đã ARCHIVED
- Trả về HTTP 409 Conflict
- Message: "Course đã được archive"

**AF2 - Course is DRAFT:**
- Tại bước 6, course status = DRAFT
- Suggestion: Delete thay vì archive

**Kết quả:**
- Course status = ARCHIVED
- Không visible trong catalog
- Không nhận students mới
- Existing students/classes không bị ảnh hưởng

**Events:**
- Event: `COURSE_ARCHIVED` (courseId, archivedBy)

---

### UC-COURSE-005: Delete Course

**Người thực hiện:** CREATOR, ADMIN/OWNER

**Mục đích:** Xóa DRAFT course chưa có classes/students

**Điều kiện trước:**
- User có quyền delete
- Course status = DRAFT
- Course chưa có classes

**Luồng chính:**

1. User truy cập DRAFT Course Detail page
2. User click "Xóa khóa học"
3. Frontend hiển thị confirmation (màu đỏ, nghiêm trọng):
   - "XÓA VĨNH VIỄN course này?"
   - "Hành động này KHÔNG THỂ hoàn tác"
4. User nhập "DELETE" để confirm
5. User click "Xác nhận xóa"
6. Frontend gửi DELETE `/api/v1/courses/{courseId}`
7. Hệ thống validate:
   - User có quyền delete
   - Course status = DRAFT
   - Course không có classes (count = 0)
8. Hệ thống xóa:
   - Delete TeacherCourse records
   - Delete Course record
9. Hệ thống trả về HTTP 204 No Content
10. Frontend redirect về Course List
11. Course biến mất hoàn toàn

**Luồng thay thế:**

**AF1 - Course có classes:**
- Tại bước 7, course có classes (count > 0)
- Trả về HTTP 409 Conflict
- Message: "Không thể xóa course có classes. Hãy archive thay vì xóa."

**AF2 - Course PUBLISHED:**
- Tại bước 7, course status = PUBLISHED
- Trả về HTTP 400 Bad Request
- Message: "Không thể xóa PUBLISHED course. Hãy archive."

**Kết quả:**
- Course bị xóa vĩnh viễn
- TeacherCourse assignments bị xóa
- Không thể phục hồi

**Events:**
- Event: `COURSE_DELETED` (courseId, courseName, deletedBy)

---

### UC-COURSE-006: Get Course Details

**Người thực hiện:** Any authenticated user

**Mục đích:** Xem chi tiết một course

**Điều kiện trước:**
- User đã login (hoặc public nếu PUBLISHED)
- Course tồn tại

**Luồng chính:**

1. User truy cập Course Detail page hoặc click vào course
2. Frontend gửi GET `/api/v1/courses/{courseId}`
3. Hệ thống validate:
   - Course tồn tại
   - Permission check:
     - Nếu DRAFT: Chỉ CREATOR/INSTRUCTOR/ADMIN
     - Nếu PUBLISHED/ARCHIVED: Anyone
4. Hệ thống query:
   - Course data
   - Teachers (qua teacher_courses)
   - Classes count
   - Enrolled students count
5. Hệ thống trả về HTTP 200 OK với CourseDetailResponse
6. Frontend hiển thị:
   - Course info
   - Syllabus
   - Teachers
   - Classes list
   - Enrollment button (nếu PUBLISHED)

**Response Example:**
```json
{
  "success": true,
  "data": {
    "id": 5,
    "name": "English for Business Communication",
    "description": "Learn professional English...",
    "subject": "English",
    "level": "INTERMEDIATE",
    "durationWeeks": 12,
    "maxStudents": 20,
    "price": 5000000,
    "syllabus": "Week 1: ...\nWeek 2: ...",
    "objectives": "Students will be able to...",
    "status": "PUBLISHED",
    "teachers": [
      {
        "id": 10,
        "name": "Teacher A",
        "role": "CREATOR",
        "specialization": "Business English"
      },
      {
        "id": 12,
        "name": "Teacher B",
        "role": "INSTRUCTOR"
      }
    ],
    "classesCount": 3,
    "enrolledStudentsCount": 45,
    "createdAt": "2026-01-15T10:00:00Z",
    "publishedAt": "2026-01-20T14:00:00Z"
  }
}
```

**Luồng thay thế:**

**AF1 - DRAFT course, không có quyền:**
- Tại bước 3, course DRAFT và user không phải CREATOR/ADMIN
- Trả về HTTP 403 Forbidden
- Message: "Course này chưa được publish"

**AF2 - Course không tồn tại:**
- Tại bước 3, courseId không có trong DB
- Trả về HTTP 404 Not Found
- Message: "Course không tồn tại"

**Kết quả:**
- User xem được đầy đủ thông tin course
- Có thể enroll nếu PUBLISHED

---

### UC-COURSE-007: List/Search Courses

**Người thực hiện:** Any authenticated user

**Mục đích:** Browse danh sách courses với search và filter

**Điều kiện trước:**
- User đã login (hoặc public)

**Luồng chính:**

1. User truy cập Course Catalog page
2. Frontend gửi GET `/api/v1/courses` với query params:
   ```
   ?status=PUBLISHED
   &subject=English
   &level=INTERMEDIATE
   &search=business
   &page=0
   &size=20
   &sort=createdAt,desc
   ```
3. Hệ thống build query:
   - Filter by status (default: PUBLISHED nếu không phải ADMIN)
   - Filter by subject nếu có
   - Filter by level nếu có
   - Search trong name/description nếu có
   - Pagination
   - Sort
4. Hệ thống query courses
5. Hệ thống trả về HTTP 200 OK với paged result
6. Frontend hiển thị course grid:
   - Course cards
   - Thumbnail
   - Name, level, price
   - Teachers
   - Classes count
   - "Enroll" button

**Response Example:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 5,
        "name": "English for Business Communication",
        "subject": "English",
        "level": "INTERMEDIATE",
        "price": 5000000,
        "durationWeeks": 12,
        "thumbnailUrl": "https://...",
        "teachersCount": 2,
        "classesCount": 3,
        "enrolledStudentsCount": 45,
        "status": "PUBLISHED"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

**Kết quả:**
- User browse courses
- Apply filters để tìm course phù hợp
- Click vào course → Course Detail

---

### UC-COURSE-008: Filter Courses by Subject/Level

**Người thực hiện:** Any user

**Mục đích:** Filter courses theo môn học hoặc level

**Điều kiện trước:**
- None

**Luồng chính:**

1. User ở Course Catalog page
2. Frontend hiển thị filter sidebar:
   - Subject (checkboxes: English, Math, Physics, etc.)
   - Level (checkboxes: Beginner, Intermediate, Advanced)
   - Price range (slider)
   - Duration (dropdown: 4-8 weeks, 8-12 weeks, 12+ weeks)
3. User chọn filters
4. Frontend gửi GET `/api/v1/courses` với filters
5. Backend query với WHERE clauses
6. Return filtered results
7. Frontend update course grid

**Kết quả:**
- User tìm courses phù hợp
- Narrow down options

---

## 🔐 5. Permission Model

### Course Permissions

**Roles:**
- **OWNER/ADMIN:** Full access all courses
- **CREATOR:** Teacher tạo course, full control
- **INSTRUCTOR:** Teacher được assign, có thể teach và manage classes
- **ASSISTANT:** Teacher phụ, view only
- **STUDENT:** Chỉ view PUBLISHED courses, enroll vào classes

**Permission Matrix:**

| Operation | OWNER/ADMIN | CREATOR | INSTRUCTOR | ASSISTANT | STUDENT |
|-----------|-------------|---------|------------|-----------|---------|
| Create Course | ✅ | ✅ | ❌ | ❌ | ❌ |
| View DRAFT | ✅ | ✅ | ❌ | ❌ | ❌ |
| View PUBLISHED | ✅ | ✅ | ✅ | ✅ | ✅ |
| Edit DRAFT | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit PUBLISHED | ✅ | ✅ (limited) | ❌ | ❌ | ❌ |
| Publish Course | ✅ | ✅ | ❌ | ❌ | ❌ |
| Archive Course | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete Course | ✅ | ✅ | ❌ | ❌ | ❌ |
| Add Teachers | ✅ | ✅ | ❌ | ❌ | ❌ |
| Create Classes | ✅ | ✅ | ✅ | ❌ | ❌ |
| Teach Classes | ✅ | ✅ | ✅ | View only | Attend |

---

## 🔗 6. Integration với Other Modules

### 6.1. Teacher Module Integration

**Dependency:** Course depends on Teacher Module

**Integration points:**
- TeacherCourse table (managed by Teacher Module)
- Course created_by FK to teachers.id
- Permission checks query teacher_courses

**APIs used:**
```java
// Check if teacher can create course
GET /internal/teachers/{teacherId}/permissions

// Get teacher info for course detail
GET /internal/teachers/{teacherId}
```

### 6.2. Class Module Integration

**Dependency:** Class depends on Course Module

**Integration points:**
- Class.course_id FK to courses.id
- 1 Course → Many Classes relationship
- Classes inherit course info (level, subject, etc.)

**APIs provided:**
```java
// Get course info for class creation
GET /internal/courses/{courseId}

// List classes in course
GET /api/v1/courses/{courseId}/classes
```

### 6.3. Enrollment Module Integration

**Dependency:** Enrollment uses Course data

**Integration points:**
- Students enroll vào Classes (not directly into Courses)
- Course info displayed during enrollment
- Course price used for payment

---

## 📊 7. Summary

### Entities
- ✅ **Course:** Main entity cho khóa học
- ✅ **TeacherCourse:** Course-Teacher relationship (reference Teacher Module)
- ✅ **Class:** Course-Class relationship (reference Class Module)

### Business Rules
- ✅ BR-COURSE-001: Name unique per teacher
- ✅ BR-COURSE-002: Phải có ít nhất 1 CREATOR
- ✅ BR-COURSE-003: DRAFT edit freely, PUBLISHED limited
- ✅ BR-COURSE-004: Không thể publish course rỗng
- ✅ BR-COURSE-005: ARCHIVED không nhận students mới
- ✅ BR-COURSE-006: Price >= 0
- ✅ BR-COURSE-007: Duration > 0

### Use Cases

**Course Management:**
- ✅ UC-COURSE-001: Create Course (Draft)
- ✅ UC-COURSE-002: Update Course
- ✅ UC-COURSE-003: Publish Course
- ✅ UC-COURSE-004: Archive Course
- ✅ UC-COURSE-005: Delete Course

**Course Discovery:**
- ✅ UC-COURSE-006: Get Course Details
- ✅ UC-COURSE-007: List/Search Courses
- ✅ UC-COURSE-008: Filter Courses

**Total:** 8 use cases + references to Teacher/Class modules

### Lifecycle
```
DRAFT → PUBLISHED → ARCHIVED

DRAFT:
- Created, being edited
- Not visible to students
- Can edit freely
- Can delete

PUBLISHED:
- Public, students can enroll
- Limited edits
- Cannot delete
- Can archive

ARCHIVED:
- No new enrollments
- Read-only
- Existing students continue
```

### Integration
- ✅ Teacher Module: TeacherCourse relationship
- ✅ Class Module: Course-Class 1-to-many
- ✅ Enrollment Module: Course info for enrollment
- ✅ Gateway: Internal APIs for cross-service calls

---

## 🚀 Next Steps

**Sau khi document này được approve:**

1. **Create PR 2.4: Course Module**
   - Implement Course entity
   - Implement repositories
   - Implement services (CRUD + lifecycle)
   - Implement REST APIs
   - Write tests (unit + integration)

2. **Update Teacher Module**
   - Ensure TeacherCourse integration works
   - Test teacher creating courses

3. **Proceed to Class Module**
   - Design Class Module business logic
   - Implement Course-Class relationship

---

**Author:** VictorAurelius + Claude Sonnet 4.5
**Created:** 2026-01-28
**Status:** Ready for Review
**Next:** Class Module business logic
