# Teacher Module - Business Logic

**Service:** kiteclass-core
**Module:** Teacher Management
**Priority:** P0 (BLOCKING - Required for PR 1.8 completion)
**Status:** Design Phase
**Version:** 1.0.0
**Date:** 2026-01-28

---

## 📋 1. Tổng Quan Module

### Mục đích

Module Teacher quản lý thông tin giáo viên và phân quyền truy cập khóa học, lớp học trong hệ thống KiteClass.

**Vai trò trong hệ thống:**
- Lưu trữ profile và thông tin nghiệp vụ của giáo viên
- Quản lý quyền truy cập **COURSE** (khóa học) - Teacher có thể tạo và dạy courses
- Quản lý quyền truy cập **CLASS** (lớp học) - Teacher dạy các classes cụ thể trong courses
- Quản lý teaching workflow: Attendance, Assignments, Grades, Materials
- Hỗ trợ 2 use cases chính:
  1. **Language Center:** Nhiều teachers, resource-level permissions
  2. **Independent Teacher:** 1 person vừa OWNER vừa TEACHER, full access

### Phạm vi (Scope)

**Trong phạm vi:**
- ✅ CRUD operations cho Teacher entity
- ✅ **Teacher-Course relationship:** Create courses, assign teachers to courses
- ✅ **Teacher-Class relationship:** Assign teachers to classes
- ✅ **Permission model:** Course-level và Class-level access control
- ✅ **Teaching operations:** Attendance, assignments, grades, materials
- ✅ Internal APIs cho Gateway (profile fetching)
- ✅ Teacher specialization, qualification, schedule tracking
- ✅ Teacher dashboard và analytics

**Ngoài phạm vi:**
- ❌ Payroll và salary management (sẽ có module riêng sau)
- ❌ Performance evaluation (future module)
- ❌ Teacher training và development (future)
- ❌ Video conferencing integration (future)

### Business Context

**Use Case 1: Trung Tâm Tiếng Anh (Language Center)**
```
Structure:
- 1 OWNER/ADMIN: Full access 30 classes
- 5 TEACHERS:
  - Teacher A manages: Class 1, 2, 3 (assigned)
  - Teacher B manages: Class 4, 5, 6 (assigned)
  - Teacher C manages: Class 7, 8, 9 (assigned)
  - Teacher D manages: Class 10, 11, 12 (assigned)
  - Teacher E manages: Class 13, 14, 15 (assigned)

Permissions:
- Teacher A CAN: View/Edit Class 1, 2, 3 ONLY
- Teacher A CANNOT: Access Class 4+ (not assigned)
- OWNER/ADMIN: Full access all 30 classes

Implementation:
- teacher_classes table controls which classes teacher can access
- Permission check on every class operation
```

**Use Case 2: Giáo Viên Độc Lập (Independent Teacher)**
```
Structure:
- 1 person: Vừa OWNER vừa TEACHER
- Self-manage 3-5 classes
- Không cần complex permissions

Permissions:
- User has roles: [OWNER, TEACHER]
- OWNER role → Bypass all resource-level checks
- Full access to all classes

Implementation:
- Check roles in Gateway
- If OWNER → Skip teacher_classes check
- Simple and flexible
```

### Priority

- **Priority:** P0 (Critical)
- **Reason:**
  - BLOCKING PR 1.8 (Teacher profile fetching)
  - Required before Class Module (Class entity references teacher_id)
  - Core business entity

---

## 🏗️ 2. Thực Thể Nghiệp Vụ

### 2.1. Teacher Entity

**Table:** `teachers`

**Mô tả:** Lưu trữ thông tin profile và nghiệp vụ của giáo viên.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| name | VARCHAR(100) | NO | Tên giáo viên | 2-100 chars |
| email | VARCHAR(255) | NO | Email (unique) | Valid email format |
| phone_number | VARCHAR(20) | YES | Số điện thoại | 10-15 digits |
| specialization | VARCHAR(100) | YES | Chuyên môn (English, Math, etc.) | Max 100 chars |
| bio | TEXT | YES | Giới thiệu bản thân | Max 2000 chars |
| qualification | VARCHAR(200) | YES | Trình độ (Bachelor, Master, etc.) | Max 200 chars |
| experience_years | INT | YES | Số năm kinh nghiệm | >= 0 |
| avatar_url | VARCHAR(500) | YES | URL ảnh đại diện | Valid URL |
| status | VARCHAR(20) | NO | Trạng thái (ACTIVE, INACTIVE, ON_LEAVE) | Enum |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian cập nhật | Auto-update |
| created_by | VARCHAR(100) | YES | Người tạo | - |
| updated_by | VARCHAR(100) | YES | Người cập nhật | - |

**Indexes:**
```sql
CREATE INDEX idx_teachers_email ON teachers(email);
CREATE INDEX idx_teachers_status ON teachers(status);
CREATE INDEX idx_teachers_specialization ON teachers(specialization);
```

**Status Values:**
- `ACTIVE`: Đang hoạt động, có thể assign classes
- `INACTIVE`: Tạm ngưng, không assign classes mới
- `ON_LEAVE`: Nghỉ phép, classes hiện tại vẫn giữ

### 2.2. TeacherClass Entity (Assignment)

**Table:** `teacher_classes`

**Mô tả:** Quản lý assignment giữa giáo viên và lớp học. Table này **CONTROLS** permissions - teacher chỉ có thể access classes có trong table này.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| teacher_id | BIGINT | NO | FK to teachers.id | Must exist |
| class_id | BIGINT | NO | FK to classes.id | Must exist |
| role | VARCHAR(20) | NO | Role trong class (MAIN_TEACHER, ASSISTANT) | Enum |
| assigned_at | TIMESTAMP | NO | Thời gian assign | Auto-set |
| assigned_by | VARCHAR(100) | YES | Người assign | - |

**Constraints:**
```sql
UNIQUE (teacher_id, class_id) -- One teacher can't be assigned to same class twice
FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
```

**Indexes:**
```sql
CREATE INDEX idx_teacher_classes_teacher_id ON teacher_classes(teacher_id);
CREATE INDEX idx_teacher_classes_class_id ON teacher_classes(class_id);
```

**Role Values:**
- `MAIN_TEACHER`: Giáo viên chính, full control class
- `ASSISTANT`: Giáo viên phụ, limited permissions

### 2.3. TeacherCourse Entity (Course Assignment)

**Table:** `teacher_courses`

**Mô tả:** Quản lý assignment giữa giáo viên và khóa học (Course). Teacher có thể **TẠO** course (creator) hoặc được **ASSIGN** vào course để dạy (instructor).

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| teacher_id | BIGINT | NO | FK to teachers.id | Must exist |
| course_id | BIGINT | NO | FK to courses.id | Must exist |
| role | VARCHAR(20) | NO | Role (CREATOR, INSTRUCTOR, ASSISTANT) | Enum |
| assigned_at | TIMESTAMP | NO | Thời gian assign | Auto-set |
| assigned_by | VARCHAR(100) | YES | Người assign (NULL nếu self-created) | - |

**Constraints:**
```sql
UNIQUE (teacher_id, course_id) -- One teacher can't have multiple roles in same course
FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
FOREIGN KEY (course_id) REFERENCES courses.id) ON DELETE CASCADE
```

**Indexes:**
```sql
CREATE INDEX idx_teacher_courses_teacher_id ON teacher_courses(teacher_id);
CREATE INDEX idx_teacher_courses_course_id ON teacher_courses(course_id);
CREATE INDEX idx_teacher_courses_role ON teacher_courses(role);
```

**Role Values:**
- `CREATOR`: Teacher tạo course này, full control (edit, delete, manage classes)
- `INSTRUCTOR`: Teacher được assign vào course, có thể dạy và manage classes
- `ASSISTANT`: Teacher phụ, limited permissions (view only, support)

**Relationship với TeacherClass:**
```
Teacher → TeacherCourse (CREATOR/INSTRUCTOR) → Course
                                                  ↓
                                              Classes
                                                  ↓
Teacher → TeacherClass (MAIN_TEACHER) → Specific Class

Logic:
1. Teacher creates Course → TeacherCourse (CREATOR)
2. Course has multiple Classes
3. Teacher gets assigned to specific Classes → TeacherClass (MAIN_TEACHER)
4. Or teacher được assign vào Course → TeacherCourse (INSTRUCTOR)
   → Auto có quyền với tất cả classes trong course đó
```

**Permission Hierarchy:**
```
Level 1: Course-level permissions (TeacherCourse)
- CREATOR: Full control course + all classes
- INSTRUCTOR: Teach course + manage assigned classes
- ASSISTANT: View only

Level 2: Class-level permissions (TeacherClass)
- MAIN_TEACHER: Full control specific class
- ASSISTANT: Limited class permissions

Priority: Course-level > Class-level
Nếu teacher là CREATOR của course → Auto có quyền với tất cả classes
```

### 2.4. Course Entity Reference

**Note:** Course entity sẽ được design chi tiết trong Course Module. Ở đây chỉ liệt kê fields liên quan đến Teacher.

**Relevant fields:**
```sql
courses table:
- id (PK)
- created_by (teacher_id) -- Teacher tạo course này
- name
- description
- status (DRAFT, PUBLISHED, ARCHIVED)
```

**Relationship:**
- 1 Teacher có thể create nhiều Courses (created_by)
- 1 Teacher có thể được assign vào nhiều Courses (teacher_courses)
- 1 Course có nhiều Classes
- 1 Teacher có thể dạy nhiều Classes trong 1 hoặc nhiều Courses

---

## 📐 3. Quy Tắc Kinh Doanh

### BR-TEACHER-001: Email Phải Duy Nhất

**Mô tả:** Mỗi giáo viên phải có email duy nhất trong hệ thống.

**Lý do:** Email dùng để liên lạc và liên kết với Gateway User.

**Validation:**
```java
boolean exists = teacherRepository.existsByEmail(email);
if (exists) {
    throw new DuplicateResourceException("email", email);
}
```

---

### BR-TEACHER-002: Teacher Phải Có Specialization

**Mô tả:** Giáo viên nên có specialization (chuyên môn) để dễ search và assign.

**Lý do:** Giúp admin/owner tìm đúng teacher cho từng môn học.

**Note:** Không bắt buộc (nullable) nhưng recommended.

---

### BR-TEACHER-003: Teacher Có Thể Được Assign Nhiều Classes

**Mô tả:** Một teacher có thể dạy nhiều classes đồng thời.

**Lý do:** Real-world scenario - teacher thường dạy nhiều lớp.

**Implementation:** Multiple records trong teacher_classes table.

---

### BR-TEACHER-004: Class Phải Có Ít Nhất 1 Main Teacher

**Mô tả:** Mỗi class phải có ít nhất 1 teacher với role = MAIN_TEACHER.

**Lý do:** Class không thể hoạt động không có giáo viên chính.

**Validation:**
- Khi remove teacher: Check nếu là MAIN_TEACHER cuối cùng → Không cho remove
- Khi create class: Phải assign ít nhất 1 MAIN_TEACHER

---

### BR-TEACHER-005: Chỉ ACTIVE Teachers Mới Được Assign Classes/Courses Mới

**Mô tả:** Chỉ teachers có status = ACTIVE mới có thể được assign vào classes/courses mới.

**Lý do:** INACTIVE hoặc ON_LEAVE teachers không nên nhận thêm công việc.

**Note:** Teachers đã assign từ trước vẫn giữ assignments khi chuyển sang INACTIVE/ON_LEAVE.

---

### BR-TEACHER-006: Course Creator Có Full Control

**Mô tả:** Teacher tạo course (CREATOR role) tự động có full control với course và tất cả classes trong course.

**Lý do:** Creator ownership - teacher tạo course thì tự nhiên có quyền quản lý.

**Implementation:**
- Check `teacher_courses.role = CREATOR` → Auto grant permissions
- Không cần assign vào từng class riêng

---

### BR-TEACHER-007: Teacher Có Thể Vừa Là CREATOR Vừa INSTRUCTOR

**Mô tả:** Một teacher có thể:
- Tạo Course A (CREATOR)
- Được assign dạy Course B (INSTRUCTOR)
- Được assign dạy Class X trong Course C (Class-level only)

**Lý do:** Real-world scenario - teacher vừa tạo courses riêng, vừa support courses khác.

---

### BR-TEACHER-008: Attendance Chỉ MAIN_TEACHER Mới Được Điểm Danh

**Mô tả:** Chỉ teachers có role MAIN_TEACHER trong class mới có quyền take attendance.

**Lý do:** ASSISTANT chỉ support, không nên có quyền điểm danh chính thức.

**Exception:** CREATOR của course có thể điểm danh tất cả classes.

---

## 🎯 4. Use Cases

### Overview

Module Teacher hỗ trợ đầy đủ teaching workflow từ tạo course đến quản lý classes:

**Course Management:**
- UC-TEACHER-001: Create Teacher Profile
- UC-TEACHER-002: Create Course (Teacher as Creator)
- UC-TEACHER-003: Assign Teacher to Course

**Class Management:**
- UC-TEACHER-004: Assign Teacher to Class
- UC-TEACHER-005: Remove Teacher from Class
- UC-TEACHER-006: Get Teacher Classes/Courses (Permission Check)

**Teaching Operations:**
- UC-TEACHER-007: Take Attendance
- UC-TEACHER-008: Create/Grade Assignment
- UC-TEACHER-009: Upload Course Material
- UC-TEACHER-010: View Student Progress
- UC-TEACHER-011: Manage Class Schedule
- UC-TEACHER-012: View Teacher Dashboard & Analytics

### UC-TEACHER-001: Tạo Teacher Profile

**Người thực hiện:** ADMIN/OWNER

**Điều kiện trước:**
- User đã đăng nhập với role ADMIN/OWNER

**Luồng chính:**

1. Admin truy cập Teacher Management
2. Admin click "Thêm giáo viên"
3. Frontend hiển thị form:
   - Name (required)
   - Email (required)
   - Phone number
   - Specialization (e.g., "English", "Math")
   - Qualification (e.g., "Bachelor's in Education")
   - Experience years
   - Bio
4. Admin điền thông tin và submit
5. Frontend gửi POST `/api/v1/teachers`
6. Hệ thống validate:
   - **BR-TEACHER-001:** Email unique
   - Name không rỗng
   - Email valid format
7. Hệ thống tạo Teacher:
   - status = ACTIVE (mặc định)
8. Hệ thống lưu vào database
9. Hệ thống trả về HTTP 201 Created với TeacherResponse
10. Frontend hiển thị: "Thêm giáo viên thành công"

**Luồng thay thế:**

**AF1 - Email trùng:**
- Tại bước 6, email đã tồn tại
- Trả về HTTP 409 Conflict
- Message: "Email '{email}' đã được sử dụng"

**Kết quả:**
- Teacher profile được tạo
- Status = ACTIVE
- Sẵn sàng được assign vào classes

---

### UC-TEACHER-002: Tạo Course (Teacher as Creator)

**Người thực hiện:** TEACHER, ADMIN/OWNER

**Mục đích:** Teacher tạo course mới và tự động trở thành CREATOR với full control

**Điều kiện trước:**
- Teacher đã login
- Teacher có status = ACTIVE

**Luồng chính:**

1. Teacher truy cập Course Management
2. Teacher click "Tạo khóa học mới"
3. Frontend hiển thị form:
   - Course name (required)
   - Course description
   - Level (Beginner, Intermediate, Advanced)
   - Duration (weeks)
   - Max students
   - Price
4. Teacher điền thông tin và submit
5. Frontend gửi POST `/api/v1/courses`
   ```json
   {
     "name": "English for Business - Advanced",
     "description": "Advanced business English course",
     "level": "ADVANCED",
     "durationWeeks": 12,
     "maxStudents": 20,
     "price": 5000000
   }
   ```
6. Hệ thống validate:
   - Name không rỗng
   - Duration > 0
   - Max students > 0
7. Hệ thống tạo Course:
   - created_by = teacherId
   - status = DRAFT
8. Hệ thống tạo TeacherCourse record:
   - teacher_id = teacherId
   - course_id = newCourseId
   - role = CREATOR
   - assigned_by = NULL (self-created)
9. Hệ thống lưu database
10. Hệ thống trả về HTTP 201 Created
11. Frontend redirect đến Course Detail page
12. Teacher thấy: "Khóa học đã được tạo. Bạn có thể bắt đầu thêm classes."

**Luồng thay thế:**

**AF1 - Course name trùng:**
- Tại bước 6, course name đã tồn tại cho teacher này
- Warning (không block): "Bạn đã có course tên này. Có chắc muốn tạo?"
- Teacher có thể proceed hoặc change name

**Kết quả:**
- Course được tạo với status = DRAFT
- TeacherCourse (CREATOR) được tạo
- Teacher có full control course này
- Teacher có quyền với tất cả classes sẽ tạo trong course

**Events:**
- Event: `COURSE_CREATED` (courseId, teacherId, courseName)

**Permission sau khi tạo:**
- Teacher (CREATOR): Full control course + all classes
- Có thể edit course, delete course, create classes, assign other teachers

---

### UC-TEACHER-003: Assign Teacher Vào Course

**Người thực hiện:** CREATOR (teacher owner course) hoặc ADMIN/OWNER

**Mục đích:** Assign thêm teachers vào course để hỗ trợ teaching

**Điều kiện trước:**
- Course tồn tại
- Teacher được assign có status = ACTIVE
- User là CREATOR của course hoặc ADMIN/OWNER

**Luồng chính:**

1. User truy cập Course Detail page
2. User click tab "Teachers"
3. User click "Assign Teacher"
4. Frontend hiển thị form:
   - Select Teacher (dropdown ACTIVE teachers)
   - Select Role (INSTRUCTOR / ASSISTANT)
5. User chọn và submit
6. Frontend gửi POST `/api/v1/courses/{courseId}/teachers`
   ```json
   {
     "teacherId": 8,
     "role": "INSTRUCTOR"
   }
   ```
7. Hệ thống validate:
   - Course tồn tại
   - Teacher tồn tại và ACTIVE
   - User có quyền assign (CREATOR hoặc ADMIN)
   - Chưa có assignment này (unique constraint)
8. Hệ thống tạo TeacherCourse record:
   - role = INSTRUCTOR (hoặc ASSISTANT)
   - assigned_by = currentUserId
9. Hệ thống trả về HTTP 201 Created
10. Frontend update teacher list
11. Assigned teacher nhận notification

**Luồng thay thế:**

**AF1 - Teacher đã được assign:**
- Tại bước 7, teacher đã có role trong course
- Trả về HTTP 409 Conflict
- Message: "Teacher đã được assign vào course này với role: {role}"

**AF2 - Không có permission:**
- Tại bước 7, user không phải CREATOR và không phải ADMIN
- Trả về HTTP 403 Forbidden
- Message: "Chỉ CREATOR hoặc ADMIN mới có thể assign teachers"

**Kết quả:**
- Teacher được assign vào course
- INSTRUCTOR: Có quyền dạy tất cả classes trong course
- ASSISTANT: View only, support role
- Teacher xuất hiện trong course teachers list

**Events:**
- Event: `TEACHER_ASSIGNED_TO_COURSE` (courseId, teacherId, role)

**Permission sau khi assign:**
- INSTRUCTOR: Access all classes in course, manage students, grades
- ASSISTANT: View only access

---

### UC-TEACHER-004: Assign Teacher Vào Class

**Người thực hiện:** ADMIN/OWNER

**Mục đích:** Phân công giáo viên vào lớp học cụ thể

**Điều kiện trước:**
- Teacher tồn tại và status = ACTIVE
- Class tồn tại

**Luồng chính:**

1. Admin truy cập Class Detail page
2. Admin click "Assign Teacher"
3. Frontend hiển thị form:
   - Select Teacher (dropdown list ACTIVE teachers)
   - Select Role (MAIN_TEACHER / ASSISTANT)
4. Admin chọn và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/teachers`
   ```json
   {
     "teacherId": 5,
     "role": "MAIN_TEACHER"
   }
   ```
6. Hệ thống validate:
   - Teacher tồn tại và ACTIVE (BR-TEACHER-005)
   - Class tồn tại
   - Chưa có assignment này (unique constraint)
7. Hệ thống tạo TeacherClass record
8. Hệ thống trả về HTTP 201 Created
9. Frontend hiển thị: "Assign teacher thành công"
10. Teacher xuất hiện trong class roster

**Luồng thay thế:**

**AF1 - Teacher đã được assign:**
- Tại bước 6, already exists trong teacher_classes
- Trả về HTTP 409 Conflict
- Message: "Teacher đã được assign vào class này"

**AF2 - Teacher không ACTIVE:**
- Tại bước 6, teacher.status != ACTIVE
- Trả về HTTP 400 Bad Request
- Message: "Chỉ ACTIVE teachers mới có thể assign vào class"

**Kết quả:**
- Teacher được assign vào class
- Teacher có quyền access class này
- Teacher xuất hiện trong class detail

---

### UC-TEACHER-005: Remove Teacher Khỏi Class

**Người thực hiện:** ADMIN/OWNER

**Điều kiện trước:**
- Teacher đã được assign vào class

**Luồng chính:**

1. Admin truy cập Class Detail → Teacher list
2. Admin click "Remove" bên teacher cần remove
3. Frontend hiển thị confirmation dialog
4. Admin confirm
5. Frontend gửi DELETE `/api/v1/classes/{classId}/teachers/{teacherId}`
6. Hệ thống validate:
   - **BR-TEACHER-004:** Nếu là MAIN_TEACHER cuối → Không cho remove
7. Hệ thống xóa TeacherClass record
8. Hệ thống trả về HTTP 204 No Content
9. Frontend update UI: Teacher biến mất khỏi roster

**Luồng thay thế:**

**AF1 - MAIN_TEACHER cuối cùng:**
- Tại bước 6, teacher là MAIN_TEACHER duy nhất
- Trả về HTTP 409 Conflict
- Message: "Không thể remove MAIN_TEACHER cuối cùng. Phải có ít nhất 1 MAIN_TEACHER."

**Kết quả:**
- Teacher bị remove khỏi class
- Teacher KHÔNG CÒN quyền access class này
- Class roster updated

---

### UC-TEACHER-006: Get Teacher Classes/Courses (For Permission Check)

**Người thực hiện:** System (Internal API)

**Mục đích:** Lấy danh sách courses và classes mà teacher được assign (cho permission check)

**Điều kiện trước:**
- Teacher ID hợp lệ

**Luồng chính:**

1. Hệ thống nhận request GET `/api/v1/teachers/{teacherId}/permissions`
2. Hệ thống query 2 tables:

   **Query 1 - Courses:**
   ```sql
   SELECT course_id, role
   FROM teacher_courses
   WHERE teacher_id = :teacherId
   ```

   **Query 2 - Classes:**
   ```sql
   SELECT class_id, role
   FROM teacher_classes
   WHERE teacher_id = :teacherId
   ```

3. Hệ thống trả về combined permissions
4. System sử dụng list này để check permissions

**Response Example:**
```json
{
  "success": true,
  "data": {
    "courses": [
      {
        "courseId": 5,
        "courseName": "English Business Advanced",
        "role": "CREATOR",
        "assignedAt": "2026-01-10T09:00:00Z"
      },
      {
        "courseId": 8,
        "courseName": "TOEIC Preparation",
        "role": "INSTRUCTOR",
        "assignedAt": "2026-01-18T14:00:00Z"
      }
    ],
    "classes": [
      {
        "classId": 1,
        "className": "English Beginner A1",
        "courseId": 3,
        "role": "MAIN_TEACHER",
        "assignedAt": "2026-01-15T10:00:00Z"
      },
      {
        "classId": 2,
        "className": "English Intermediate B1",
        "courseId": 3,
        "role": "MAIN_TEACHER",
        "assignedAt": "2026-01-20T14:30:00Z"
      }
    ]
  }
}
```

**Sử dụng trong Permission Check:**
```java
// Use Case 1: Language Center
public boolean canAccessClass(User user, Long classId) {
    if (user.hasRole("OWNER") || user.hasRole("ADMIN")) {
        return true; // Full access
    }

    if (user.getUserType() == UserType.TEACHER) {
        Long teacherId = user.getReferenceId();

        // Check 1: Course-level access (CREATOR/INSTRUCTOR)
        Long courseId = classService.getCourseIdByClassId(classId);
        if (courseId != null) {
            boolean hasCourseAccess = teacherCourseRepository
                .existsByTeacherIdAndCourseIdAndRoleIn(
                    teacherId, courseId,
                    Arrays.asList(TeacherCourseRole.CREATOR, TeacherCourseRole.INSTRUCTOR)
                );
            if (hasCourseAccess) {
                return true; // Has course-level access
            }
        }

        // Check 2: Class-level access (direct assignment)
        boolean hasClassAccess = teacherClassRepository
            .existsByTeacherIdAndClassId(teacherId, classId);

        return hasClassAccess;
    }

    return false;
}
```

**Permission Priority Logic:**
```
1. Check if OWNER/ADMIN → Full access
2. Check if CREATOR of course → Full access to all classes
3. Check if INSTRUCTOR of course → Access to all classes
4. Check if assigned to specific class → Access to that class only
5. Otherwise → Access denied
```

---

### UC-TEACHER-007: Take Attendance (Điểm Danh)

**Người thực hiện:** MAIN_TEACHER hoặc CREATOR

**Mục đích:** Teacher điểm danh học sinh trong buổi học

**Điều kiện trước:**
- Teacher có quyền điểm danh class (MAIN_TEACHER hoặc CREATOR của course)
- Class session tồn tại
- Class có students enrolled

**Luồng chính:**

1. Teacher truy cập Class Detail → Schedule tab
2. Teacher chọn session hôm nay hoặc một session cụ thể
3. Teacher click "Điểm danh"
4. Frontend hiển thị danh sách students:
   - Student name
   - Attendance status (PRESENT / ABSENT / LATE / EXCUSED)
   - Note field
5. Teacher đánh dấu từng student
6. Teacher click "Lưu điểm danh"
7. Frontend gửi POST `/api/v1/classes/{classId}/sessions/{sessionId}/attendance`
   ```json
   {
     "sessionId": 45,
     "attendanceRecords": [
       {
         "studentId": 10,
         "status": "PRESENT",
         "note": null
       },
       {
         "studentId": 11,
         "status": "LATE",
         "note": "Đến muộn 15 phút"
       },
       {
         "studentId": 12,
         "status": "ABSENT",
         "note": "Xin phép nghỉ ốm"
       }
     ]
   }
   ```
8. Hệ thống validate:
   - **BR-TEACHER-008:** Teacher phải là MAIN_TEACHER hoặc CREATOR
   - Session tồn tại và thuộc class này
   - Students enrolled trong class
9. Hệ thống lưu attendance records
10. Hệ thống trả về HTTP 200 OK
11. Frontend hiển thị: "Điểm danh thành công"
12. Students/Parents nhận notification về attendance

**Luồng thay thế:**

**AF1 - Không có quyền điểm danh:**
- Tại bước 8, teacher là ASSISTANT (không phải MAIN_TEACHER)
- Trả về HTTP 403 Forbidden
- Message: "Chỉ MAIN_TEACHER mới có thể điểm danh"

**AF2 - Session đã có attendance:**
- Tại bước 8, session đã được điểm danh
- Hiển thị warning: "Session này đã có điểm danh. Có muốn cập nhật?"
- Teacher có thể update hoặc cancel

**Kết quả:**
- Attendance records được lưu cho session
- Students/Parents được thông báo
- Attendance data dùng cho báo cáo và analytics

**Events:**
- Event: `ATTENDANCE_TAKEN` (classId, sessionId, teacherId, presentCount, absentCount)

---

### UC-TEACHER-008: Create/Grade Assignment (Tạo và Chấm Bài Tập)

**Người thực hiện:** MAIN_TEACHER, INSTRUCTOR, CREATOR

**Mục đích:** Teacher tạo bài tập và chấm điểm cho students

**Điều kiện trước:**
- Teacher có quyền manage class assignments
- Class tồn tại và có students

**Luồng chính (Create Assignment):**

1. Teacher truy cập Class Detail → Assignments tab
2. Teacher click "Tạo bài tập mới"
3. Frontend hiển thị form:
   - Assignment title
   - Description
   - Due date
   - Max score (points)
   - Attachment (files)
4. Teacher điền thông tin và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/assignments`
   ```json
   {
     "title": "Unit 3 - Grammar Exercise",
     "description": "Complete exercises on page 45-48",
     "dueDate": "2026-02-05T23:59:59Z",
     "maxScore": 100,
     "attachmentUrls": ["https://storage.../exercise.pdf"]
   }
   ```
6. Hệ thống validate:
   - Teacher có quyền create assignments
   - Due date trong tương lai
   - Max score > 0
7. Hệ thống tạo Assignment
8. Hệ thống trả về HTTP 201 Created
9. Frontend hiển thị: "Bài tập đã được tạo"
10. Students nhận notification về assignment mới

**Luồng chính (Grade Assignment):**

1. Teacher truy cập Assignment Detail → Submissions tab
2. Teacher thấy list student submissions
3. Teacher click vào một submission
4. Teacher xem student work (files, text)
5. Teacher nhập điểm và feedback
6. Teacher click "Lưu điểm"
7. Frontend gửi PUT `/api/v1/assignments/{assignmentId}/submissions/{submissionId}/grade`
   ```json
   {
     "score": 85,
     "feedback": "Good work! Cần cải thiện phần grammar.",
     "gradedAt": "2026-02-06T10:30:00Z"
   }
   ```
8. Hệ thống validate:
   - Score <= maxScore
   - Submission tồn tại
9. Hệ thống lưu grade
10. Hệ thống trả về HTTP 200 OK
11. Student nhận notification về điểm số

**Luồng thay thế:**

**AF1 - Late submission:**
- Student submit sau due date
- System đánh dấu "Late submission"
- Teacher quyết định có chấm hay không

**Kết quả:**
- Assignment được tạo và assigned cho students
- Submissions được graded
- Students nhận feedback
- Grades contribute to final course grade

**Events:**
- Event: `ASSIGNMENT_CREATED` (classId, assignmentId, dueDate)
- Event: `ASSIGNMENT_GRADED` (assignmentId, studentId, score)

---

### UC-TEACHER-009: Upload Course Material (Upload Tài Liệu)

**Người thực hiện:** CREATOR, INSTRUCTOR, MAIN_TEACHER

**Mục đích:** Teacher upload tài liệu học tập cho course hoặc class

**Điều kiện trước:**
- Teacher có quyền manage course/class materials

**Luồng chính:**

1. Teacher truy cập Course/Class Detail → Materials tab
2. Teacher click "Upload tài liệu"
3. Frontend hiển thị upload form:
   - Material title
   - Description
   - Category (Lecture Notes, Slides, Exercises, Reference)
   - Files (PDF, DOCX, PPTX, videos)
   - Visibility (All students / Specific class)
4. Teacher chọn files và điền thông tin
5. Frontend upload files lên storage (S3/Cloud Storage)
6. Frontend gửi POST `/api/v1/courses/{courseId}/materials`
   ```json
   {
     "title": "Unit 3 - Present Perfect Tense",
     "description": "Lecture notes và exercises",
     "category": "LECTURE_NOTES",
     "fileUrls": [
       "https://storage.../unit3-notes.pdf",
       "https://storage.../unit3-exercises.pdf"
     ],
     "visibility": "ALL_STUDENTS",
     "classId": null
   }
   ```
7. Hệ thống validate:
   - Teacher có quyền upload materials
   - Files valid (virus scan passed)
   - File size trong limit
8. Hệ thống tạo Material records
9. Hệ thống trả về HTTP 201 Created
10. Frontend hiển thị: "Tài liệu đã được upload"
11. Students có thể download materials

**Luồng thay thế:**

**AF1 - File quá lớn:**
- Tại bước 7, file size > 50MB
- Trả về HTTP 413 Payload Too Large
- Message: "File quá lớn. Max 50MB per file."

**AF2 - Virus detected:**
- Tại bước 7, virus scan failed
- Trả về HTTP 400 Bad Request
- Message: "File không an toàn. Không thể upload."

**Kết quả:**
- Materials được upload và available cho students
- Students có thể view/download
- Materials organized by category

**Events:**
- Event: `MATERIAL_UPLOADED` (courseId, materialId, teacherId)

---

### UC-TEACHER-010: View Student Progress (Xem Tiến Độ Học Sinh)

**Người thực hiện:** MAIN_TEACHER, INSTRUCTOR, CREATOR

**Mục đích:** Teacher theo dõi tiến độ học tập của students

**Điều kiện trước:**
- Teacher có quyền view class/course data
- Class có students và activities

**Luồng chính:**

1. Teacher truy cập Class Detail → Progress tab
2. Frontend gửi GET `/api/v1/classes/{classId}/progress`
3. Hệ thống query data:
   - Attendance rate per student
   - Assignment scores per student
   - Test scores per student
   - Overall progress percentage
4. Hệ thống calculate metrics:
   ```java
   studentProgress = {
       attendanceRate: (presentCount / totalSessions) * 100,
       avgAssignmentScore: sum(assignmentScores) / assignmentCount,
       avgTestScore: sum(testScores) / testCount,
       overallProgress: (completedLessons / totalLessons) * 100
   }
   ```
5. Hệ thống trả về progress data
6. Frontend hiển thị table:
   - Student name
   - Attendance rate (%)
   - Avg assignment score
   - Avg test score
   - Overall progress (%)
   - Status (On track / At risk / Excellent)
7. Teacher có thể:
   - Sort by metrics
   - Filter by status
   - Click vào student → Chi tiết progress
   - Export report (PDF/Excel)

**Response Example:**
```json
{
  "success": true,
  "data": {
    "classId": 5,
    "className": "English Intermediate B1",
    "totalStudents": 25,
    "studentProgress": [
      {
        "studentId": 10,
        "studentName": "Nguyen Van A",
        "attendanceRate": 95.0,
        "avgAssignmentScore": 88.5,
        "avgTestScore": 85.0,
        "overallProgress": 75.0,
        "status": "EXCELLENT"
      },
      {
        "studentId": 11,
        "studentName": "Tran Thi B",
        "attendanceRate": 60.0,
        "avgAssignmentScore": 55.0,
        "avgTestScore": 50.0,
        "overallProgress": 40.0,
        "status": "AT_RISK"
      }
    ]
  }
}
```

**Kết quả:**
- Teacher có overview về class performance
- Identify at-risk students sớm
- Data-driven decisions về teaching adjustments

**Events:**
- Event: `PROGRESS_VIEWED` (classId, teacherId)

---

### UC-TEACHER-011: Manage Class Schedule (Quản Lý Lịch Học)

**Người thực hiện:** MAIN_TEACHER, CREATOR

**Mục đích:** Teacher tạo và quản lý lịch học của class

**Điều kiện trước:**
- Teacher có quyền manage class schedule
- Class tồn tại

**Luồng chính:**

1. Teacher truy cập Class Detail → Schedule tab
2. Teacher click "Tạo lịch học"
3. Frontend hiển thị calendar form:
   - Start date
   - End date
   - Days of week (Mon, Wed, Fri)
   - Time (e.g., 18:00 - 20:00)
   - Location (Room 101 hoặc Online)
4. Teacher configure và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/schedule`
   ```json
   {
     "startDate": "2026-02-01",
     "endDate": "2026-05-01",
     "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
     "startTime": "18:00",
     "endTime": "20:00",
     "location": "Room 101",
     "recurrenceRule": "WEEKLY"
   }
   ```
6. Hệ thống validate:
   - End date > Start date
   - Time valid
   - No schedule conflicts (nếu in-person)
7. Hệ thống generate class sessions:
   - Parse recurrence rule
   - Create session records cho mỗi ngày
   - Total sessions = (weeks * daysPerWeek)
8. Hệ thống lưu schedule và sessions
9. Hệ thống trả về HTTP 201 Created
10. Frontend hiển thị calendar với all sessions
11. Students nhận notification về schedule

**Luồng thay thế:**

**AF1 - Schedule conflict:**
- Tại bước 6, phòng học đã có lớp khác cùng thời gian
- Warning: "Room 101 has conflict on Mon 18:00-20:00"
- Teacher chọn phòng khác hoặc thời gian khác

**AF2 - Update existing schedule:**
- Teacher muốn change schedule
- Hiển thị warning về impact lên students
- Teacher confirm → Update sessions
- Students nhận notification về schedule change

**Kết quả:**
- Class schedule được tạo
- Sessions được generate tự động
- Students biết thời gian học
- Calendar integration (Google Calendar, iCal)

**Events:**
- Event: `SCHEDULE_CREATED` (classId, totalSessions, startDate, endDate)
- Event: `SCHEDULE_UPDATED` (classId, changes)

---

### UC-TEACHER-012: View Teacher Dashboard & Analytics

**Người thực hiện:** TEACHER

**Mục đích:** Teacher xem overview về courses, classes, và teaching performance

**Điều kiện trước:**
- Teacher đã login

**Luồng chính:**

1. Teacher login và truy cập Dashboard
2. Frontend gửi GET `/api/v1/teachers/{teacherId}/dashboard`
3. Hệ thống collect data:
   - Total courses (CREATOR, INSTRUCTOR)
   - Total classes (MAIN_TEACHER)
   - Total students across all classes
   - Upcoming sessions (next 7 days)
   - Pending assignments to grade
   - Recent attendance records
   - Class performance metrics
4. Hệ thống calculate analytics:
   ```java
   dashboard = {
       totalCourses: count(teacher_courses where teacher_id = X),
       totalClasses: count(teacher_classes where teacher_id = X),
       totalStudents: count(distinct student_id from enrollments),
       upcomingSessions: sessions where date in [today, +7 days],
       pendingGrading: count(submissions where graded = false),
       avgClassAttendance: avg(attendance_rate) across classes,
       avgClassScore: avg(assignment_scores) across classes
   }
   ```
5. Hệ thống trả về dashboard data
6. Frontend hiển thị widgets:
   - **Summary Cards:** Courses, Classes, Students, Upcoming sessions
   - **Quick Actions:** Take attendance, Grade assignments, Upload materials
   - **Recent Activity:** Latest attendance, graded assignments
   - **Performance Charts:** Attendance trends, Score distributions
   - **Upcoming Schedule:** Next 7 days sessions với locations
   - **Alerts:** At-risk students, Missing grades, Schedule conflicts
7. Teacher có thể:
   - Click vào widget → Drill down details
   - Quick action buttons → Nhanh chóng điểm danh, chấm bài
   - View detailed reports

**Response Example:**
```json
{
  "success": true,
  "data": {
    "summary": {
      "totalCourses": 3,
      "totalClasses": 8,
      "totalStudents": 156,
      "upcomingSessions": 12
    },
    "pendingTasks": {
      "assignmentsToGrade": 45,
      "attendanceToTake": 3
    },
    "performance": {
      "avgAttendanceRate": 88.5,
      "avgAssignmentScore": 78.3,
      "avgTestScore": 82.1
    },
    "upcomingSessions": [
      {
        "sessionId": 450,
        "classId": 5,
        "className": "English Intermediate B1",
        "date": "2026-01-29",
        "startTime": "18:00",
        "endTime": "20:00",
        "location": "Room 101"
      }
    ],
    "alerts": [
      {
        "type": "AT_RISK_STUDENT",
        "message": "3 students có attendance < 70% trong Class B1"
      },
      {
        "type": "PENDING_GRADES",
        "message": "15 bài tập chưa chấm điểm (quá 3 ngày)"
      }
    ]
  }
}
```

**Kết quả:**
- Teacher có overview về tất cả teaching activities
- Quick access đến các tasks cần làm
- Data-driven insights về teaching performance
- Alerts về issues cần attention

**Events:**
- Event: `DASHBOARD_VIEWED` (teacherId)

---

## 🔐 5. Permission Model

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                Gateway (Authentication)                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  users table:                                                │
│  - user_type: TEACHER                                       │
│  - reference_id: 5 (→ teachers.id in Core)                 │
│  - roles: [OWNER, TEACHER] or [TEACHER]                    │
│                                                              │
└───────────────────────────┬──────────────────────────────────┘
                            │
                            │ Permission Check
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  Core (Business Logic)                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  teachers table:                                             │
│  - id: 5                                                    │
│  - name, email, specialization                              │
│                                                              │
│  teacher_courses table (COURSE-LEVEL PERMISSIONS):          │
│  - teacher_id: 5, course_id: 10, role: CREATOR             │
│  - teacher_id: 5, course_id: 12, role: INSTRUCTOR          │
│                                                              │
│  teacher_classes table (CLASS-LEVEL PERMISSIONS):           │
│  - teacher_id: 5, class_id: 1, role: MAIN_TEACHER          │
│  - teacher_id: 5, class_id: 2, role: MAIN_TEACHER          │
│  - teacher_id: 5, class_id: 3, role: MAIN_TEACHER          │
│                                                              │
│  Permission Hierarchy:                                       │
│  1. OWNER/ADMIN role → Full access ALL                      │
│  2. CREATOR of course → Access ALL classes in course        │
│  3. INSTRUCTOR of course → Access ALL classes in course     │
│  4. MAIN_TEACHER of class → Access specific class only      │
│  5. Otherwise → Access denied                                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Use Case 1: Language Center (Multiple Teachers)

**Scenario:**
- Center có 5 teachers
- Mỗi teacher có course-level hoặc class-level permissions
- Hierarchy: Course-level > Class-level

**Permission Implementation:**

```java
@Service
public class PermissionService {

    public boolean canAccessClass(User user, Long classId) {
        // Step 1: Check if user is OWNER/ADMIN
        if (user.hasRole("OWNER") || user.hasRole("ADMIN")) {
            return true; // Full access
        }

        // Step 2: Check if user is TEACHER
        if (user.getUserType() == UserType.TEACHER) {
            Long teacherId = user.getReferenceId();

            // Step 3: Check Course-level permissions (higher priority)
            Long courseId = classService.getCourseIdByClassId(classId);
            if (courseId != null) {
                // Check if teacher is CREATOR or INSTRUCTOR of course
                boolean hasCourseAccess = teacherCourseRepository
                    .existsByTeacherIdAndCourseIdAndRoleIn(
                        teacherId,
                        courseId,
                        Arrays.asList(TeacherCourseRole.CREATOR, TeacherCourseRole.INSTRUCTOR)
                    );

                if (hasCourseAccess) {
                    return true; // Has course-level access → All classes in course
                }
            }

            // Step 4: Check Class-level permissions
            boolean hasClassAccess = teacherClassRepository
                .existsByTeacherIdAndClassId(teacherId, classId);

            return hasClassAccess; // Direct class assignment
        }

        return false;
    }

    public boolean canModifyClass(User user, Long classId) {
        // Same as canAccessClass but also check role
        if (!canAccessClass(user, classId)) {
            return false;
        }

        // Check if can modify (not just view)
        if (user.getUserType() == UserType.TEACHER) {
            Long teacherId = user.getReferenceId();

            // Check 1: CREATOR → Full modify rights
            Long courseId = classService.getCourseIdByClassId(classId);
            if (courseId != null) {
                boolean isCreator = teacherCourseRepository
                    .existsByTeacherIdAndCourseIdAndRole(
                        teacherId, courseId, TeacherCourseRole.CREATOR
                    );
                if (isCreator) {
                    return true; // CREATOR can modify
                }

                // Check 2: INSTRUCTOR → Can modify
                boolean isInstructor = teacherCourseRepository
                    .existsByTeacherIdAndCourseIdAndRole(
                        teacherId, courseId, TeacherCourseRole.INSTRUCTOR
                    );
                if (isInstructor) {
                    return true;
                }
            }

            // Check 3: MAIN_TEACHER → Can modify
            Optional<TeacherClass> assignment = teacherClassRepository
                .findByTeacherIdAndClassId(teacherId, classId);

            if (assignment.isPresent()) {
                return assignment.get().getRole() == TeacherRole.MAIN_TEACHER;
            }
        }

        return true; // OWNER/ADMIN can modify
    }

    public boolean canTakeAttendance(User user, Long classId) {
        // BR-TEACHER-008: Only MAIN_TEACHER or CREATOR
        if (user.hasRole("OWNER") || user.hasRole("ADMIN")) {
            return true;
        }

        if (user.getUserType() == UserType.TEACHER) {
            Long teacherId = user.getReferenceId();

            // Check 1: CREATOR of course
            Long courseId = classService.getCourseIdByClassId(classId);
            if (courseId != null) {
                boolean isCreator = teacherCourseRepository
                    .existsByTeacherIdAndCourseIdAndRole(
                        teacherId, courseId, TeacherCourseRole.CREATOR
                    );
                if (isCreator) {
                    return true;
                }
            }

            // Check 2: MAIN_TEACHER of class
            Optional<TeacherClass> assignment = teacherClassRepository
                .findByTeacherIdAndClassId(teacherId, classId);

            return assignment.isPresent() &&
                   assignment.get().getRole() == TeacherRole.MAIN_TEACHER;
        }

        return false;
    }
}
```

**Example Scenario 1 - Course-level Access:**
```java
// Teacher A là CREATOR của Course 10 (có 5 classes)
User teacherA = getCurrentUser();
// teacherA.userType = TEACHER
// teacherA.referenceId = 5
// teacherA.roles = [TEACHER]

// teacher_courses: (teacher_id=5, course_id=10, role=CREATOR)
// Course 10 có classes: [1, 2, 3, 4, 5]

// Teacher A tries to access Class 1 (thuộc Course 10)
permissionService.canAccessClass(teacherA, 1L);
→ Step 1: Not OWNER/ADMIN
→ Step 2: Is TEACHER
→ Step 3: Get courseId = 10
→ Step 3: Check teacher_courses: (teacher_id=5, course_id=10, role=CREATOR)
→ Return true ✅ (Course-level access)

// Teacher A can access ALL classes in Course 10
permissionService.canAccessClass(teacherA, 2L); // ✅
permissionService.canAccessClass(teacherA, 3L); // ✅
permissionService.canAccessClass(teacherA, 4L); // ✅
permissionService.canAccessClass(teacherA, 5L); // ✅

// But NOT classes in other courses
permissionService.canAccessClass(teacherA, 20L); // ❌ (belongs to Course 15)
```

**Example Scenario 2 - Class-level Access Only:**
```java
// Teacher B chỉ được assign vào Class 8, 9 (không có course-level permissions)
User teacherB = getCurrentUser();
// teacherB.userType = TEACHER
// teacherB.referenceId = 8
// teacherB.roles = [TEACHER]

// teacher_classes:
//   (teacher_id=8, class_id=8, role=MAIN_TEACHER)
//   (teacher_id=8, class_id=9, role=MAIN_TEACHER)

// Teacher B tries to access Class 8
permissionService.canAccessClass(teacherB, 8L);
→ Step 1: Not OWNER/ADMIN
→ Step 2: Is TEACHER
→ Step 3: Get courseId (e.g., Course 12)
→ Step 3: Check teacher_courses: (teacher_id=8, course_id=12) NOT FOUND
→ Step 4: Check teacher_classes: (teacher_id=8, class_id=8) EXISTS
→ Return true ✅ (Class-level access)

// Can access assigned classes only
permissionService.canAccessClass(teacherB, 8L); // ✅
permissionService.canAccessClass(teacherB, 9L); // ✅

// Cannot access other classes in same course
permissionService.canAccessClass(teacherB, 10L); // ❌ (cùng Course 12 nhưng not assigned)
```

**Example Scenario 3 - Mixed Permissions:**
```java
// Teacher C là:
// - CREATOR of Course 5
// - INSTRUCTOR of Course 8
// - MAIN_TEACHER of Class 15 (trong Course 10 - không có course-level)

User teacherC = getCurrentUser();
// teacherC.userType = TEACHER
// teacherC.referenceId = 10
// teacherC.roles = [TEACHER]

// teacher_courses:
//   (teacher_id=10, course_id=5, role=CREATOR)
//   (teacher_id=10, course_id=8, role=INSTRUCTOR)
// teacher_classes:
//   (teacher_id=10, class_id=15, role=MAIN_TEACHER)

// Access all classes in Course 5 (CREATOR)
permissionService.canAccessClass(teacherC, 1L); // ✅ (Course 5)
permissionService.canAccessClass(teacherC, 2L); // ✅ (Course 5)

// Access all classes in Course 8 (INSTRUCTOR)
permissionService.canAccessClass(teacherC, 10L); // ✅ (Course 8)
permissionService.canAccessClass(teacherC, 11L); // ✅ (Course 8)

// Access specific Class 15 only (Class-level)
permissionService.canAccessClass(teacherC, 15L); // ✅ (Direct assignment)
permissionService.canAccessClass(teacherC, 16L); // ❌ (Cùng Course 10 nhưng not assigned)
```

### Use Case 2: Independent Teacher (OWNER + TEACHER)

**Scenario:**
- 1 person vừa là OWNER vừa là TEACHER
- Full access tất cả classes

**Permission Implementation:**

```java
User independentTeacher = getCurrentUser();
// independentTeacher.userType = TEACHER
// independentTeacher.referenceId = 1
// independentTeacher.roles = [OWNER, TEACHER] ← Has OWNER role

// Independent teacher tries to access ANY class
permissionService.canAccessClass(independentTeacher, anyClassId);
→ Check roles: Has OWNER? YES
→ Return true immediately ✅ (Bypass teacher_classes check)
→ Full access to all classes
```

**Benefits:**
- ✅ Simple: Just add OWNER role to user
- ✅ No need to create teacher_classes records for every class
- ✅ Scales: Works for 1 class or 100 classes
- ✅ Flexible: Can switch between modes by adding/removing OWNER role

---

## 🔗 6. Integration với Gateway

### Gateway → Core Communication

**Scenario:** User login, Gateway fetch teacher profile

**Flow:**

```
1. User login ở Gateway
2. Gateway validates credentials
3. Gateway checks: user.userType == TEACHER
4. Gateway gets: user.referenceId = 5
5. Gateway calls Core:
   GET /internal/teachers/5
   Header: X-Internal-Request: true
6. Core returns TeacherProfileResponse
7. Gateway includes profile trong LoginResponse
```

**Core Internal API:**

```java
@RestController
@RequestMapping("/internal/teachers")
public class InternalTeacherController {

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherProfileResponse>>
            getTeacher(@PathVariable Long id) {

        Teacher teacher = teacherService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));

        TeacherProfileResponse response = new TeacherProfileResponse(
            teacher.getId(),
            teacher.getName(),
            teacher.getEmail(),
            teacher.getPhoneNumber(),
            teacher.getSpecialization(),
            teacher.getAvatarUrl(),
            teacher.getStatus().name()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

**Gateway DTO:**

```java
public record TeacherProfileResponse(
    Long id,
    String name,
    String email,
    String phoneNumber,
    String specialization,
    String avatarUrl,
    String status
) {}
```

---

## 📊 7. Summary

### Entities
- ✅ **Teacher:** Profile và thông tin nghiệp vụ
- ✅ **TeacherCourse:** Course-level assignment và permissions (CREATOR, INSTRUCTOR, ASSISTANT)
- ✅ **TeacherClass:** Class-level assignment và permissions (MAIN_TEACHER, ASSISTANT)
- ✅ **Course:** Reference entity (designed in Course Module)

### Business Rules
- ✅ BR-TEACHER-001: Email unique
- ✅ BR-TEACHER-002: Should have specialization
- ✅ BR-TEACHER-003: Can assign nhiều classes
- ✅ BR-TEACHER-004: Class phải có ít nhất 1 MAIN_TEACHER
- ✅ BR-TEACHER-005: Chỉ ACTIVE teachers assign được
- ✅ BR-TEACHER-006: Course CREATOR có full control
- ✅ BR-TEACHER-007: Teacher có thể vừa CREATOR vừa INSTRUCTOR
- ✅ BR-TEACHER-008: Attendance chỉ MAIN_TEACHER hoặc CREATOR

### Use Cases

**Course Management:**
- ✅ UC-TEACHER-001: Create Teacher Profile
- ✅ UC-TEACHER-002: Create Course (Teacher as Creator)
- ✅ UC-TEACHER-003: Assign Teacher to Course

**Class Management:**
- ✅ UC-TEACHER-004: Assign Teacher to Class
- ✅ UC-TEACHER-005: Remove Teacher from Class
- ✅ UC-TEACHER-006: Get Teacher Classes/Courses (Permission Check)

**Teaching Operations:**
- ✅ UC-TEACHER-007: Take Attendance
- ✅ UC-TEACHER-008: Create/Grade Assignment
- ✅ UC-TEACHER-009: Upload Course Material
- ✅ UC-TEACHER-010: View Student Progress
- ✅ UC-TEACHER-011: Manage Class Schedule
- ✅ UC-TEACHER-012: View Teacher Dashboard & Analytics

**Total:** 12 comprehensive use cases covering full teaching workflow

### Permission Model
- ✅ **Two-level hierarchy:** Course-level > Class-level
- ✅ **Course-level roles:** CREATOR (full control), INSTRUCTOR (teaching), ASSISTANT (view only)
- ✅ **Class-level roles:** MAIN_TEACHER (full control), ASSISTANT (limited)
- ✅ Support Use Case 1: Language Center (resource-level permissions)
- ✅ Support Use Case 2: Independent Teacher (OWNER bypass)
- ✅ Flexible permission cascade: Course permissions → Apply to all classes
- ✅ Scalable và flexible

### Integration
- ✅ Internal API cho Gateway profile fetching
- ✅ Clear separation: Gateway (auth) vs Core (business)
- ✅ Cross-module integration: Course, Class, Student, Enrollment, Attendance modules

---

## 🚀 Next Steps

**Sau khi document này được approve:**

1. **Create PR 2.3.1: Teacher Module**
   - Implement Teacher entity
   - Implement TeacherClass entity
   - Implement repositories
   - Implement services
   - Implement internal API
   - Write tests (unit + integration)

2. **Update PR 1.8 (Gateway)**
   - Uncomment `ProfileFetcher.fetchTeacherProfile()` lines 136-137
   - Test teacher login with profile
   - Add integration tests

3. **Update Class Module**
   - Add teacher_id FK to classes table
   - Update Class entity với Teacher relationship

---

**Author:** VictorAurelius + Claude Sonnet 4.5
**Created:** 2026-01-28
**Status:** Ready for Review
**Next:** Course Module business logic
