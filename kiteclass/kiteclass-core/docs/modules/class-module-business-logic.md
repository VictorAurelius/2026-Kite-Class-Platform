# Class Module - Business Logic

**Service:** kiteclass-core
**Module:** Class Management
**Priority:** P0 (Required after Course Module)
**Status:** Design Phase
**Version:** 1.0.0
**Date:** 2026-01-28

---

## 📋 1. Tổng Quan Module

### Mục đích

Module Class quản lý lớp học cụ thể trong hệ thống KiteClass.

**Vai trò trong hệ thống:**
- Quản lý classes (instances cụ thể của courses)
- Quản lý class schedule và sessions
- Quản lý teacher-class assignments
- Quản lý student enrollments (qua Enrollment Module)
- Generate class codes cho student self-enrollment
- Track class lifecycle: UPCOMING → ONGOING → COMPLETED
- Foundation cho Attendance, Assignments, và Teaching activities

### Phạm vi (Scope)

**Trong phạm vi:**
- ✅ CRUD operations cho Class entity
- ✅ Class lifecycle management (UPCOMING → ONGOING → COMPLETED → CANCELLED)
- ✅ Class schedule và session management
- ✅ Class code generation và validation
- ✅ Teacher-Class assignment (qua TeacherClass)
- ✅ Enrollment management (integration với Enrollment Module)
- ✅ Class capacity tracking (max students, current count)
- ✅ Class location management (Room / Online)

**Ngoài phạm vi:**
- ❌ Attendance taking (Attendance Module)
- ❌ Assignment management (Assignment Module)
- ❌ Grade management (Grade Module)
- ❌ Class materials (Material Module)
- ❌ Video conferencing (Future integration)

### Business Context

**Real-World Scenario:**

**Scenario: Language Center tạo Classes cho Course**
```
Course: "English Intermediate B1" (12 weeks, max 20 students)

Class 1: "B1 - Evening Mon-Wed-Fri"
├── Schedule: Mon-Wed-Fri, 18:00-20:00
├── Location: Room 101
├── Teacher: Teacher A (MAIN_TEACHER)
├── Max students: 20
├── Current enrolled: 18
├── Status: ONGOING
├── Class code: ABC123 (students tự enroll)
└── Sessions: 36 sessions (12 weeks * 3 days)

Class 2: "B1 - Morning Tue-Thu"
├── Schedule: Tue-Thu, 09:00-11:00
├── Location: Room 102
├── Teacher: Teacher B (MAIN_TEACHER)
├── Max students: 20
├── Current enrolled: 15
├── Status: UPCOMING (chưa start)
├── Class code: XYZ789
└── Sessions: 24 sessions (12 weeks * 2 days)

Class 3: "B1 - Weekend"
├── Schedule: Sat-Sun, 09:00-12:00
├── Location: Online (Zoom)
├── Teachers:
│   ├── Teacher A (MAIN_TEACHER)
│   └── Teacher C (ASSISTANT)
├── Max students: 25 (online, larger capacity)
├── Current enrolled: 22
├── Status: ONGOING
├── Class code: WEB456
└── Sessions: 24 sessions (12 weeks * 2 days)

Flow:
1. ADMIN/Teacher tạo Course
2. ADMIN/Teacher tạo Classes trong Course
3. Assign teachers vào classes
4. Generate class codes
5. Students nhận codes → Self-enroll vào classes
6. Classes start → ONGOING
7. Teachers take attendance, create assignments
8. Classes end → COMPLETED
```

### Priority

- **Priority:** P0 (Critical)
- **Reason:**
  - Core business entity
  - Required cho Enrollment workflow
  - Foundation cho teaching operations
  - Required cho Attendance, Assignments

---

## 🏗️ 2. Thực Thể Nghiệp Vụ

### 2.1. Class Entity

**Table:** `classes`

**Mô tả:** Lớp học cụ thể (instance của course).

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| course_id | BIGINT | NO | FK to courses.id | Must exist |
| name | VARCHAR(200) | NO | Tên lớp | 5-200 chars |
| description | TEXT | YES | Mô tả lớp | Max 2000 chars |
| schedule | VARCHAR(200) | YES | Lịch học text (Mon-Wed-Fri 18:00-20:00) | Max 200 chars |
| location_type | VARCHAR(20) | NO | IN_PERSON / ONLINE | Enum |
| location_detail | VARCHAR(200) | YES | Room 101 hoặc Zoom link | Max 200 chars |
| start_date | DATE | YES | Ngày bắt đầu | >= today |
| end_date | DATE | YES | Ngày kết thúc | > start_date |
| max_students | INT | NO | Số học sinh tối đa | >= 1 |
| current_enrolled | INT | NO | Số học sinh hiện tại | >= 0, <= max_students |
| class_code | VARCHAR(20) | YES | Mã lớp để enroll (unique) | 6-20 chars, uppercase |
| code_expires_at | TIMESTAMP | YES | Class code hết hạn | Future timestamp |
| status | VARCHAR(20) | NO | UPCOMING, ONGOING, COMPLETED, CANCELLED | Enum |
| created_by | BIGINT | YES | User ID người tạo | - |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian cập nhật | Auto-update |
| started_at | TIMESTAMP | YES | Thời gian start | Set when ONGOING |
| completed_at | TIMESTAMP | YES | Thời gian complete | Set when COMPLETED |
| cancelled_at | TIMESTAMP | YES | Thời gian cancel | Set when CANCELLED |

**Indexes:**
```sql
CREATE INDEX idx_classes_course_id ON classes(course_id);
CREATE INDEX idx_classes_status ON classes(status);
CREATE INDEX idx_classes_class_code ON classes(class_code);
CREATE INDEX idx_classes_start_date ON classes(start_date);
CREATE UNIQUE INDEX idx_classes_class_code_unique ON classes(class_code) WHERE class_code IS NOT NULL;
```

**Status Values:**
- `UPCOMING`: Lớp sắp bắt đầu (chưa start)
- `ONGOING`: Đang diễn ra (started)
- `COMPLETED`: Đã kết thúc (finished)
- `CANCELLED`: Đã hủy (cancelled, students refunded)

**Location Type Values:**
- `IN_PERSON`: Học trực tiếp (cần room)
- `ONLINE`: Học online (Zoom, Google Meet, etc.)

### 2.2. TeacherClass Entity (Reference)

**Table:** `teacher_classes`

**Mô tả:** Assignment giữa teachers và classes. Chi tiết trong Teacher Module.

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| teacher_id | BIGINT | FK to teachers.id |
| class_id | BIGINT | FK to classes.id |
| role | VARCHAR(20) | MAIN_TEACHER, ASSISTANT |
| assigned_at | TIMESTAMP | Thời gian assign |
| assigned_by | VARCHAR(100) | Người assign |

**Relationship:**
```
Class 1 ──── * TeacherClass * ──── 1 Teacher

Roles:
- MAIN_TEACHER: Giáo viên chính, full control
- ASSISTANT: Giáo viên phụ, limited permissions
```

### 2.3. ClassSession Entity

**Table:** `class_sessions`

**Mô tả:** Các buổi học cụ thể của class.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| class_id | BIGINT | NO | FK to classes.id | Must exist |
| session_number | INT | NO | Số thứ tự buổi học (1, 2, 3, ...) | >= 1 |
| session_date | DATE | NO | Ngày học | - |
| start_time | TIME | NO | Giờ bắt đầu | - |
| end_time | TIME | NO | Giờ kết thúc | > start_time |
| location | VARCHAR(200) | YES | Location cụ thể (override class location) | Max 200 chars |
| topic | VARCHAR(200) | YES | Chủ đề buổi học | Max 200 chars |
| status | VARCHAR(20) | NO | SCHEDULED, COMPLETED, CANCELLED | Enum |
| attendance_taken | BOOLEAN | NO | Đã điểm danh chưa | Default false |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian update | Auto-update |

**Indexes:**
```sql
CREATE INDEX idx_class_sessions_class_id ON class_sessions(class_id);
CREATE INDEX idx_class_sessions_date ON class_sessions(session_date);
CREATE INDEX idx_class_sessions_status ON class_sessions(status);
CREATE UNIQUE INDEX idx_class_sessions_unique ON class_sessions(class_id, session_number);
```

**Relationship:**
```
Class 1 ──── * ClassSession

Logic:
- 1 Class có nhiều Sessions
- Sessions được generate khi create class schedule
- Example: Class 12 weeks, 3 days/week → 36 sessions
```

### 2.4. Enrollment Entity (Reference)

**Table:** `enrollments`

**Mô tả:** Students enrolled vào classes. Chi tiết trong Enrollment Module.

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| student_id | BIGINT | FK to students.id |
| class_id | BIGINT | FK to classes.id |
| enrolled_at | TIMESTAMP | Thời gian enroll |
| status | VARCHAR(20) | ACTIVE, COMPLETED, DROPPED, CANCELLED |

**Relationship:**
```
Class 1 ──── * Enrollment * ──── 1 Student

Logic:
- Students enroll vào Classes (not Courses)
- current_enrolled count = count(enrollments where status=ACTIVE)
```

---

## 📐 3. Quy Tắc Kinh Doanh

### BR-CLASS-001: Class Phải Thuộc 1 Course

**Mô tả:** Mỗi class phải có course_id valid.

**Lý do:** Classes là instances của courses, không thể tồn tại độc lập.

**Validation:**
```java
Course course = courseRepository.findById(courseId)
    .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

if (course.getStatus() == CourseStatus.ARCHIVED) {
    throw new BusinessException("Không thể tạo class cho ARCHIVED course");
}
```

---

### BR-CLASS-002: Class Phải Có Ít Nhất 1 MAIN_TEACHER

**Mô tả:** Class không thể hoạt động không có MAIN_TEACHER.

**Lý do:** Cần teacher chịu trách nhiệm chính.

**Implementation:**
- Khi create class: Phải assign ít nhất 1 MAIN_TEACHER ngay hoặc trong vòng 24h
- Khi remove teacher: Check nếu là MAIN_TEACHER cuối → Không cho remove

---

### BR-CLASS-003: Enrollment Không Được Vượt Quá Max Students

**Mô tả:** current_enrolled <= max_students

**Lý do:** Giới hạn capacity của class.

**Validation:**
```java
if (clazz.getCurrentEnrolled() >= clazz.getMaxStudents()) {
    throw new BusinessException("Class đã đầy. Max: " + clazz.getMaxStudents());
}
```

---

### BR-CLASS-004: Class Code Phải Unique

**Mô tả:** Class codes phải unique trong toàn hệ thống.

**Lý do:** Students enroll bằng code, phải identify đúng class.

**Implementation:**
```java
boolean exists = classRepository.existsByClassCode(classCode);
if (exists) {
    throw new DuplicateResourceException("Class code", classCode);
}
```

---

### BR-CLASS-005: End Date Phải Sau Start Date

**Mô tả:** end_date > start_date

**Lý do:** Logic cơ bản.

**Validation:**
```java
if (endDate != null && startDate != null && !endDate.isAfter(startDate)) {
    throw new ValidationException("End date phải sau start date");
}
```

---

### BR-CLASS-006: Chỉ UPCOMING Classes Mới Có Thể Edit Schedule

**Mô tả:** Classes đang ONGOING hoặc COMPLETED không thể thay đổi schedule.

**Lý do:** Đã có attendance records, assignments → Breaking change.

**Rules:**
```
UPCOMING:
✅ Can edit: schedule, start_date, end_date, location
✅ Can cancel: Yes

ONGOING:
⚠️ Can edit: location (emergency)
❌ Cannot edit: schedule, dates
✅ Can complete: Yes

COMPLETED:
❌ Cannot edit: Read-only
❌ Cannot cancel: Already done

CANCELLED:
❌ Cannot edit: Read-only
```

---

### BR-CLASS-007: Class Code Expiration

**Mô tả:** Class codes có thể có expiration date.

**Lý do:** Prevent enrollments sau khi class đã full hoặc started.

**Logic:**
```java
if (clazz.getCodeExpiresAt() != null &&
    LocalDateTime.now().isAfter(clazz.getCodeExpiresAt())) {
    throw new BusinessException("Class code đã hết hạn");
}
```

---

### BR-CLASS-008: Không Thể Enroll Vào COMPLETED/CANCELLED Classes

**Mô tả:** Chỉ UPCOMING và ONGOING classes nhận enrollments.

**Lý do:** COMPLETED/CANCELLED classes không còn active.

**Validation:**
```java
if (clazz.getStatus() == ClassStatus.COMPLETED ||
    clazz.getStatus() == ClassStatus.CANCELLED) {
    throw new BusinessException("Không thể enroll vào class này");
}
```

---

## 🎯 4. Use Cases

### Overview

Module Class hỗ trợ full lifecycle management của lớp học:

**Class Creation & Management:**
- UC-CLASS-001: Create Class (trong Course)
- UC-CLASS-002: Update Class
- UC-CLASS-003: Start Class (UPCOMING → ONGOING)
- UC-CLASS-004: Complete Class (ONGOING → COMPLETED)
- UC-CLASS-005: Cancel Class
- UC-CLASS-006: Delete Class (chỉ UPCOMING, chưa có students)

**Class Discovery:**
- UC-CLASS-007: Get Class Details
- UC-CLASS-008: List Classes (trong Course hoặc by Teacher)

**Class Enrollment:**
- UC-CLASS-009: Generate Class Code
- UC-CLASS-010: Enroll by Class Code (Reference UC-GAT-008)
- UC-CLASS-011: Validate Class Code

**Class Schedule:**
- UC-CLASS-012: Create Class Schedule & Sessions
- UC-CLASS-013: Update Session
- UC-CLASS-014: List Sessions

---

### UC-CLASS-001: Tạo Class (trong Course)

**Người thực hiện:** CREATOR (của course), INSTRUCTOR, ADMIN/OWNER

**Mục đích:** Tạo class mới trong course

**Điều kiện trước:**
- User có quyền create classes trong course
- Course tồn tại và status != ARCHIVED

**Luồng chính:**

1. User truy cập Course Detail → Classes tab
2. User click "Tạo lớp mới"
3. Frontend hiển thị form:
   - Class name (required)
   - Description
   - Schedule text (Mon-Wed-Fri 18:00-20:00)
   - Location type (IN_PERSON / ONLINE)
   - Location detail (Room / Zoom link)
   - Start date
   - End date
   - Max students (default: course.maxStudents)
4. User điền thông tin và submit
5. Frontend gửi POST `/api/v1/courses/{courseId}/classes`
   ```json
   {
     "name": "English B1 - Evening Class",
     "description": "Evening class for working professionals",
     "schedule": "Mon-Wed-Fri 18:00-20:00",
     "locationType": "IN_PERSON",
     "locationDetail": "Room 101",
     "startDate": "2026-02-10",
     "endDate": "2026-05-10",
     "maxStudents": 20
   }
   ```
6. Hệ thống validate:
   - **BR-CLASS-001:** Course tồn tại và không ARCHIVED
   - Name không rỗng
   - **BR-CLASS-005:** end_date > start_date nếu có
   - max_students >= 1
7. Hệ thống tạo Class:
   - course_id = courseId
   - status = UPCOMING
   - current_enrolled = 0
   - class_code = NULL (sẽ generate sau)
8. Hệ thống lưu database
9. Hệ thống trả về HTTP 201 Created với ClassResponse
10. Frontend redirect đến Class Detail page
11. User thấy: "Lớp học đã được tạo. Hãy assign teachers và generate class code."

**Luồng thay thế:**

**AF1 - Course ARCHIVED:**
- Tại bước 6, course status = ARCHIVED
- Trả về HTTP 400 Bad Request
- Message: "Không thể tạo class cho ARCHIVED course"

**AF2 - Invalid dates:**
- Tại bước 6, end_date <= start_date
- Trả về HTTP 400 Bad Request
- Message: "End date phải sau start date"

**Kết quả:**
- Class được tạo với status = UPCOMING
- Class xuất hiện trong course classes list
- Sẵn sàng assign teachers và students

**Events:**
- Event: `CLASS_CREATED` (classId, courseId, className)

---

### UC-CLASS-002: Update Class

**Người thực hiện:** MAIN_TEACHER, CREATOR (của course), ADMIN/OWNER

**Mục đích:** Cập nhật thông tin class

**Điều kiện trước:**
- User có quyền edit class
- Class tồn tại

**Luồng chính:**

1. User truy cập Class Detail page
2. User click "Chỉnh sửa"
3. Frontend hiển thị edit form với data hiện tại
4. User update fields (allowed dựa vào status):
   - Description (always)
   - Location (UPCOMING, ONGOING with warning)
   - Schedule (chỉ UPCOMING)
   - Dates (chỉ UPCOMING)
   - Max students (UPCOMING, ONGOING nếu > current_enrolled)
5. User submit
6. Frontend gửi PUT `/api/v1/classes/{classId}`
   ```json
   {
     "description": "Updated description",
     "locationDetail": "Room 102 (changed)",
     "maxStudents": 25
   }
   ```
7. Hệ thống validate:
   - User có quyền edit
   - **BR-CLASS-006:** Check allowed fields dựa vào status
   - Nếu ONGOING và change location: Warning about impact
   - Nếu change max_students: Check >= current_enrolled
8. Hệ thống update Class
9. Hệ thống trả về HTTP 200 OK
10. Frontend update UI
11. Nếu ONGOING và major change: Notify students

**Luồng thay thế:**

**AF1 - Change schedule của ONGOING class:**
- Tại bước 7, class ONGOING và user cố change schedule
- Trả về HTTP 400 Bad Request
- Message: "Không thể thay đổi schedule của ONGOING class"

**AF2 - Reduce max_students below current_enrolled:**
- Tại bước 7, new max_students < current_enrolled
- Trả về HTTP 400 Bad Request
- Message: "Max students không thể nhỏ hơn số học sinh hiện tại ({current})"

**Kết quả:**
- Class được update
- Changes reflected trong class detail
- Students được notify nếu cần

**Events:**
- Event: `CLASS_UPDATED` (classId, updatedFields, userId)

---

### UC-CLASS-003: Start Class (UPCOMING → ONGOING)

**Người thực hiện:** MAIN_TEACHER, ADMIN/OWNER

**Mục đích:** Start class khi đã đủ điều kiện

**Điều kiện trước:**
- Class status = UPCOMING
- Class có ít nhất 1 MAIN_TEACHER
- Class có ít nhất 1 student enrolled (recommended)

**Luồng chính:**

1. User truy cập Class Detail page
2. User click "Bắt đầu lớp học"
3. Frontend hiển thị confirmation:
   - "Bắt đầu lớp học này?"
   - "Sau khi bắt đầu, schedule không thể thay đổi"
   - Checklist: ✅ Teachers assigned, ✅ Students enrolled
4. User confirm
5. Frontend gửi POST `/api/v1/classes/{classId}/start`
6. Hệ thống validate:
   - Status = UPCOMING
   - **BR-CLASS-002:** Có ít nhất 1 MAIN_TEACHER
   - Warning nếu chưa có students
7. Hệ thống update:
   - status = UPCOMING → ONGOING
   - started_at = NOW()
8. Hệ thống trả về HTTP 200 OK
9. Frontend hiển thị: "Lớp học đã bắt đầu"
10. Students/Teachers nhận notification

**Luồng thay thế:**

**AF1 - Chưa có MAIN_TEACHER:**
- Tại bước 6, class chưa có MAIN_TEACHER
- Trả về HTTP 400 Bad Request
- Message: "Class phải có ít nhất 1 MAIN_TEACHER trước khi bắt đầu"

**AF2 - Already ONGOING:**
- Tại bước 6, class đã ONGOING
- Trả về HTTP 409 Conflict
- Message: "Class đã bắt đầu"

**Kết quả:**
- Class status = ONGOING
- Schedule locked (không thể edit)
- Teachers có thể take attendance, create assignments
- started_at timestamp được set

**Events:**
- Event: `CLASS_STARTED` (classId, className, startedBy)

---

### UC-CLASS-004: Complete Class (ONGOING → COMPLETED)

**Người thực hiện:** MAIN_TEACHER, ADMIN/OWNER

**Mục đích:** Mark class as completed khi kết thúc

**Điều kiện trước:**
- Class status = ONGOING
- Class đã past end_date hoặc teacher manually complete

**Luồng chính:**

1. User truy cập Class Detail page
2. User click "Hoàn thành lớp học"
3. Frontend hiển thị confirmation:
   - "Hoàn thành lớp học này?"
   - "Sau khi hoàn thành, không thể undo"
   - Summary: Total sessions, attendance rate, completed assignments
4. User confirm
5. Frontend gửi POST `/api/v1/classes/{classId}/complete`
6. Hệ thống validate:
   - Status = ONGOING
   - User có quyền complete
7. Hệ thống update:
   - status = ONGOING → COMPLETED
   - completed_at = NOW()
8. Hệ thống trigger final tasks:
   - Calculate final grades
   - Generate certificates (future)
   - Archive class materials
9. Hệ thống trả về HTTP 200 OK
10. Frontend hiển thị: "Lớp học đã hoàn thành"
11. Students/Teachers nhận notification

**Kết quả:**
- Class status = COMPLETED
- Read-only mode
- Final grades calculated
- Students receive completion notification

**Events:**
- Event: `CLASS_COMPLETED` (classId, completedBy, totalStudents)

---

### UC-CLASS-005: Cancel Class

**Người thực hiện:** ADMIN/OWNER

**Mục đích:** Cancel class (chưa bắt đầu hoặc đang diễn ra)

**Điều kiện trước:**
- Class status = UPCOMING hoặc ONGOING
- User có quyền cancel

**Luồng chính:**

1. User truy cập Class Detail page
2. User click "Hủy lớp học"
3. Frontend hiển thị confirmation (màu đỏ, nghiêm trọng):
   - "HỦY lớp học này?"
   - "Students sẽ được refund"
   - "Hành động này ảnh hưởng {count} students"
   - Required: Reason for cancellation
4. User nhập reason và confirm
5. Frontend gửi POST `/api/v1/classes/{classId}/cancel`
   ```json
   {
     "reason": "Không đủ students enrolled"
   }
   ```
6. Hệ thống validate:
   - Status = UPCOMING hoặc ONGOING
   - User có quyền cancel
7. Hệ thống update:
   - status → CANCELLED
   - cancelled_at = NOW()
8. Hệ thống trigger refund workflow:
   - Mark enrollments as CANCELLED
   - Initiate refunds cho students
9. Hệ thống trả về HTTP 200 OK
10. Frontend hiển thị: "Lớp học đã bị hủy"
11. Students nhận notification và refund info

**Luồng thay thế:**

**AF1 - COMPLETED class:**
- Tại bước 6, class đã COMPLETED
- Trả về HTTP 400 Bad Request
- Message: "Không thể cancel COMPLETED class"

**Kết quả:**
- Class status = CANCELLED
- Enrollments cancelled
- Students được refund
- Class không còn active

**Events:**
- Event: `CLASS_CANCELLED` (classId, reason, affectedStudents)

---

### UC-CLASS-006: Delete Class

**Người thực hiện:** CREATOR, ADMIN/OWNER

**Mục đích:** Xóa class chưa có students

**Điều kiện trước:**
- Class status = UPCOMING
- current_enrolled = 0 (chưa có students)

**Luồng chính:**

1. User truy cập Class Detail page
2. User click "Xóa lớp học"
3. Frontend hiển thị confirmation:
   - "XÓA VĨNH VIỄN lớp học này?"
   - "Hành động này KHÔNG THỂ hoàn tác"
4. User nhập "DELETE" để confirm
5. User click "Xác nhận xóa"
6. Frontend gửi DELETE `/api/v1/classes/{classId}`
7. Hệ thống validate:
   - Status = UPCOMING
   - current_enrolled = 0
   - User có quyền delete
8. Hệ thống xóa:
   - Delete TeacherClass records
   - Delete ClassSession records
   - Delete Class record
9. Hệ thống trả về HTTP 204 No Content
10. Frontend redirect về Course Classes list

**Luồng thay thế:**

**AF1 - Class có students:**
- Tại bước 7, current_enrolled > 0
- Trả về HTTP 409 Conflict
- Message: "Không thể xóa class có students. Hãy cancel thay vì xóa."

**AF2 - Class đã ONGOING/COMPLETED:**
- Tại bước 7, status != UPCOMING
- Trả về HTTP 400 Bad Request
- Message: "Chỉ có thể xóa UPCOMING classes chưa có students"

**Kết quả:**
- Class bị xóa vĩnh viễn
- Không thể phục hồi

**Events:**
- Event: `CLASS_DELETED` (classId, className, deletedBy)

---

### UC-CLASS-009: Generate Class Code

**Người thực hiện:** MAIN_TEACHER, CREATOR, ADMIN/OWNER

**Mục đích:** Tạo class code để students tự enroll

**Điều kiện trước:**
- Class tồn tại
- User có quyền manage class

**Luồng chính:**

1. User truy cập Class Detail page
2. User click "Tạo mã lớp"
3. Frontend hiển thị form:
   - Auto-generate code (random 6-8 chars)
   - Hoặc custom code
   - Expiration date (optional)
4. User chọn và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/generate-code`
   ```json
   {
     "customCode": null,
     "expiresAt": "2026-03-01T23:59:59Z"
   }
   ```
6. Hệ thống generate code:
   - Nếu custom: Validate unique
   - Nếu auto: Generate random uppercase alphanumeric
   - Loop until unique
7. Hệ thống validate:
   - **BR-CLASS-004:** Code unique
8. Hệ thống update Class:
   - class_code = newCode
   - code_expires_at = expiresAt
9. Hệ thống trả về HTTP 200 OK với code
10. Frontend hiển thị code (copy button)
11. Teacher chia sẻ code với students

**Response Example:**
```json
{
  "success": true,
  "data": {
    "classCode": "ABC123XY",
    "expiresAt": "2026-03-01T23:59:59Z",
    "shareLink": "https://kiteclass.com/enroll?code=ABC123XY"
  }
}
```

**Luồng thay thế:**

**AF1 - Custom code trùng:**
- Tại bước 7, custom code đã tồn tại
- Trả về HTTP 409 Conflict
- Message: "Class code này đã được sử dụng. Hãy chọn code khác."

**Kết quả:**
- Class có code để students enroll
- Teacher chia sẻ code qua email, Zalo, etc.
- Students dùng code → Self-enroll

**Events:**
- Event: `CLASS_CODE_GENERATED` (classId, classCode)

---

### UC-CLASS-012: Create Class Schedule & Sessions

**Người thực hiện:** MAIN_TEACHER, CREATOR, ADMIN/OWNER

**Mục đích:** Tạo lịch học chi tiết và generate sessions

**Điều kiện trước:**
- Class tồn tại
- Class có start_date và end_date
- User có quyền manage class

**Luồng chính:**

1. User truy cập Class Detail → Schedule tab
2. User click "Tạo lịch học"
3. Frontend hiển thị calendar form:
   - Days of week (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
   - Start time (e.g., 18:00)
   - End time (e.g., 20:00)
   - Recurrence rule (WEEKLY)
4. User configure và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/schedule`
   ```json
   {
     "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
     "startTime": "18:00",
     "endTime": "20:00",
     "recurrenceRule": "WEEKLY"
   }
   ```
6. Hệ thống validate:
   - end_time > start_time
   - Class có start_date và end_date
7. Hệ thống generate ClassSession records:
   - Parse days of week
   - Loop from start_date to end_date
   - Create session cho mỗi ngày match
   - session_number auto-increment
8. Hệ thống lưu sessions vào database
9. Hệ thống trả về HTTP 201 Created với sessions count
10. Frontend hiển thị calendar với all sessions

**Example:**
```
Class: 2026-02-10 to 2026-05-10 (12 weeks)
Days: Mon-Wed-Fri
Time: 18:00-20:00

Generated sessions:
- Session 1: 2026-02-10 (Mon) 18:00-20:00
- Session 2: 2026-02-12 (Wed) 18:00-20:00
- Session 3: 2026-02-14 (Fri) 18:00-20:00
- Session 4: 2026-02-17 (Mon) 18:00-20:00
...
Total: 36 sessions (12 weeks * 3 days)
```

**Kết quả:**
- ClassSession records created
- Students/Teachers biết lịch học cụ thể
- Sẵn sàng cho attendance tracking

**Events:**
- Event: `CLASS_SCHEDULE_CREATED` (classId, totalSessions)

---

## 🔐 5. Permission Model

### Class Permissions

**Roles:**
- **OWNER/ADMIN:** Full access all classes
- **CREATOR (của course):** Full control classes trong course của mình
- **INSTRUCTOR (của course):** Manage classes trong course, can teach
- **MAIN_TEACHER (của class):** Full control specific class
- **ASSISTANT (của class):** Limited permissions, support role
- **STUDENT:** Chỉ view enrolled classes

**Permission Matrix:**

| Operation | OWNER/ADMIN | CREATOR | INSTRUCTOR | MAIN_TEACHER | ASSISTANT | STUDENT |
|-----------|-------------|---------|------------|--------------|-----------|---------|
| Create Class | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| View Class | ✅ | ✅ | ✅ | ✅ | ✅ | Enrolled only |
| Edit Class | ✅ | ✅ | ✅ (limited) | ✅ | ❌ | ❌ |
| Start Class | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Complete Class | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Cancel Class | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Delete Class | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Generate Code | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Take Attendance | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Create Assignment | ✅ | ✅ | ✅ | ✅ | View only | Submit |

---

## 🔗 6. Integration với Other Modules

### 6.1. Course Module Integration

**Dependency:** Class depends on Course Module

**Integration points:**
- Class.course_id FK to courses.id
- Inherit course info (subject, level, etc.)

**APIs used:**
```java
// Get course info for class creation
GET /internal/courses/{courseId}
```

### 6.2. Teacher Module Integration

**Dependency:** Class uses TeacherClass from Teacher Module

**Integration points:**
- TeacherClass table managed by Teacher Module
- Class permission checks query teacher_classes

**APIs used:**
```java
// Check if teacher can access class
GET /internal/teachers/{teacherId}/classes
```

### 6.3. Enrollment Module Integration

**Dependency:** Class uses Enrollment Module

**Integration points:**
- Enrollments link students to classes
- current_enrolled count from enrollments

**APIs used:**
```java
// Enroll student
POST /api/v1/enrollments

// Get enrolled students
GET /api/v1/classes/{classId}/students
```

### 6.4. Attendance Module Integration

**Dependency:** Attendance uses Class Sessions

**Integration points:**
- Attendance records link to class_sessions
- attendance_taken flag trong sessions

---

## 📊 7. Summary

### Entities
- ✅ **Class:** Main entity
- ✅ **ClassSession:** Sessions trong class
- ✅ **TeacherClass:** Teacher assignments (reference Teacher Module)
- ✅ **Enrollment:** Student enrollments (reference Enrollment Module)

### Business Rules
- ✅ BR-CLASS-001: Class thuộc 1 Course
- ✅ BR-CLASS-002: Phải có ít nhất 1 MAIN_TEACHER
- ✅ BR-CLASS-003: Enrollment <= Max students
- ✅ BR-CLASS-004: Class code unique
- ✅ BR-CLASS-005: End date > Start date
- ✅ BR-CLASS-006: Chỉ UPCOMING edit schedule
- ✅ BR-CLASS-007: Class code expiration
- ✅ BR-CLASS-008: Không enroll vào COMPLETED/CANCELLED

### Use Cases

**Class Management:**
- ✅ UC-CLASS-001: Create Class
- ✅ UC-CLASS-002: Update Class
- ✅ UC-CLASS-003: Start Class
- ✅ UC-CLASS-004: Complete Class
- ✅ UC-CLASS-005: Cancel Class
- ✅ UC-CLASS-006: Delete Class

**Class Discovery:**
- ✅ UC-CLASS-007: Get Class Details
- ✅ UC-CLASS-008: List Classes

**Class Enrollment:**
- ✅ UC-CLASS-009: Generate Class Code
- ✅ UC-CLASS-010: Enroll by Code (Reference Gateway)
- ✅ UC-CLASS-011: Validate Code

**Class Schedule:**
- ✅ UC-CLASS-012: Create Schedule & Sessions
- ✅ UC-CLASS-013: Update Session
- ✅ UC-CLASS-014: List Sessions

**Total:** 14 use cases

### Lifecycle
```
UPCOMING → ONGOING → COMPLETED
             ↓
         CANCELLED

UPCOMING:
- Created, being prepared
- Can edit schedule
- Can assign teachers
- Can enroll students
- Can delete (nếu chưa có students)

ONGOING:
- Started, classes happening
- Schedule locked
- Can take attendance
- Can create assignments
- Can complete hoặc cancel

COMPLETED:
- Finished
- Read-only
- Final grades calculated

CANCELLED:
- Cancelled
- Students refunded
- Read-only
```

### Integration
- ✅ Course Module: Class thuộc Course
- ✅ Teacher Module: TeacherClass assignments
- ✅ Enrollment Module: Student enrollments
- ✅ Attendance Module: Attendance tracking
- ✅ Gateway: Class code enrollment (UC-GAT-008)

---

## 🚀 Next Steps

**Sau khi document này được approve:**

1. **Create PR 2.5: Class Module**
   - Implement Class entity
   - Implement ClassSession entity
   - Implement repositories
   - Implement services (CRUD + lifecycle)
   - Implement REST APIs
   - Implement class code generation
   - Write tests (unit + integration)

2. **Update Course Module**
   - Ensure Course-Class relationship works
   - List classes in course

3. **Proceed to Enrollment Module**
   - Design Enrollment Module business logic
   - Implement student enrollment workflow

---

**Author:** VictorAurelius + Claude Sonnet 4.5
**Created:** 2026-01-28
**Status:** Ready for Review
**Next:** Enrollment Module business logic
