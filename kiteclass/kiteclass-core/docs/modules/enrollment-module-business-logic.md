# Enrollment Module - Business Logic

**Service:** kiteclass-core
**Module:** Enrollment Management
**Priority:** P0 (Required after Class Module)
**Status:** Design Phase
**Version:** 1.0.0
**Date:** 2026-01-28

---

## 📋 1. Tổng Quan Module

### Mục đích

Module Enrollment quản lý việc students đăng ký vào classes trong hệ thống KiteClass.

**Vai trò trong hệ thống:**
- Quản lý student enrollment vào classes
- Xử lý enrollment bằng class code (self-enrollment)
- Quản lý enrollment lifecycle: PENDING → ACTIVE → COMPLETED/DROPPED
- Track enrollment history và status
- Integration với Payment Module (future) cho paid courses
- Handle refunds khi classes cancelled
- Foundation cho Attendance và Grade tracking

### Phạm vi (Scope)

**Trong phạm vi:**
- ✅ Student enrollment vào classes
- ✅ Enrollment bằng class code (UC-GAT-008 integration)
- ✅ Admin-initiated enrollment (batch operations)
- ✅ Enrollment status management
- ✅ Class capacity tracking
- ✅ Enrollment validation (class full, duplicates, prerequisites)
- ✅ Drop/Withdraw from class
- ✅ Enrollment history tracking

**Ngoài phạm vi:**
- ❌ Payment processing (Payment Module - future)
- ❌ Course prerequisites validation (Future feature)
- ❌ Waitlist management (Future feature)
- ❌ Certificate generation (Future feature)

### Business Context

**Real-World Scenarios:**

**Scenario 1: Student Self-Enrollment bằng Class Code**
```
Flow:
1. Teacher tạo Class "English B1 - Evening" (max 20 students)
2. Teacher generate class code: "ABC123"
3. Teacher chia sẻ code với students qua email/Zalo
4. Student A login → Nhập code "ABC123"
5. Hệ thống validate:
   ✅ Code valid và chưa expired
   ✅ Class chưa full (18/20)
   ✅ Student chưa enrolled
6. Enrollment created → Status ACTIVE
7. Student A xuất hiện trong class roster
8. current_enrolled: 18 → 19
9. Student A có thể attend classes, submit assignments
```

**Scenario 2: Admin Batch Enrollment**
```
Flow:
1. Trung tâm có 30 học sinh đăng ký offline
2. Admin import danh sách students
3. Admin select Class "TOEIC 600+"
4. Admin batch enroll 30 students
5. Hệ thống validate từng student:
   ✅ Student exists
   ✅ Class có capacity
   ✅ Chưa enrolled
6. Create 30 enrollment records
7. Students nhận email welcome + class info
8. Class roster updated
```

**Scenario 3: Drop từ Class**
```
Flow:
1. Student B enrolled vào "English B1"
2. Student B muốn drop (lý do: schedule conflict)
3. Student B click "Rút khỏi lớp"
4. Hệ thống check:
   ✅ Enrollment ACTIVE
   ✅ Chưa quá drop deadline (nếu có)
5. Update enrollment: ACTIVE → DROPPED
6. current_enrolled: 19 → 18
7. Refund nếu chưa quá refund deadline
8. Student B không còn access class materials
```

**Scenario 4: Class Cancelled - Auto Refund**
```
Flow:
1. Class "Math Advanced" có 15 students enrolled
2. Admin cancel class (lý do: không đủ students)
3. Hệ thống auto process:
   - Update all 15 enrollments: ACTIVE → CANCELLED
   - Trigger refund workflow cho 15 students
   - Send notifications
4. Students nhận refund
5. Enrollments marked CANCELLED
```

### Priority

- **Priority:** P0 (Critical)
- **Reason:**
  - Core business process
  - Required cho teaching workflow
  - Blocking Attendance, Assignments
  - Direct revenue impact

---

## 🏗️ 2. Thực Thể Nghiệp Vụ

### 2.1. Enrollment Entity

**Table:** `enrollments`

**Mô tả:** Students enrolled vào classes.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| student_id | BIGINT | NO | FK to students.id | Must exist |
| class_id | BIGINT | NO | FK to classes.id | Must exist |
| status | VARCHAR(20) | NO | PENDING, ACTIVE, COMPLETED, DROPPED, CANCELLED | Enum |
| enrolled_at | TIMESTAMP | NO | Thời gian enroll | Auto-set |
| enrolled_by | VARCHAR(100) | YES | Ai enroll (student_id hoặc admin) | - |
| enrollment_method | VARCHAR(20) | NO | CLASS_CODE, ADMIN, DIRECT | Enum |
| payment_status | VARCHAR(20) | YES | PENDING, PAID, REFUNDED (future) | Enum |
| payment_amount | DECIMAL(15,2) | YES | Số tiền đã trả | >= 0 |
| dropped_at | TIMESTAMP | YES | Thời gian drop | Set when DROPPED |
| drop_reason | VARCHAR(500) | YES | Lý do drop | Max 500 chars |
| cancelled_at | TIMESTAMP | YES | Thời gian cancel | Set when CANCELLED |
| completed_at | TIMESTAMP | YES | Thời gian complete | Set when COMPLETED |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian update | Auto-update |

**Indexes:**
```sql
CREATE INDEX idx_enrollments_student_id ON enrollments(student_id);
CREATE INDEX idx_enrollments_class_id ON enrollments(class_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
CREATE UNIQUE INDEX idx_enrollments_unique ON enrollments(student_id, class_id);
```

**Status Values:**
- `PENDING`: Đang chờ xử lý (payment pending)
- `ACTIVE`: Đang học
- `COMPLETED`: Đã hoàn thành class
- `DROPPED`: Student tự rút khỏi class
- `CANCELLED`: Class bị cancel → Auto cancelled

**Enrollment Method Values:**
- `CLASS_CODE`: Student tự enroll bằng class code
- `ADMIN`: Admin enroll student
- `DIRECT`: Direct enrollment (API call)

**Payment Status Values (Future):**
- `PENDING`: Chưa thanh toán
- `PAID`: Đã thanh toán
- `REFUNDED`: Đã refund

### 2.2. Student Entity (Reference)

**Table:** `students`

**Mô tả:** Students trong hệ thống. Chi tiết trong Student Module.

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| name | VARCHAR(100) | Tên student |
| email | VARCHAR(255) | Email |
| status | VARCHAR(20) | ACTIVE, INACTIVE |

**Relationship:**
```
Student 1 ──── * Enrollment * ──── 1 Class

Logic:
- 1 Student có thể enroll nhiều Classes
- 1 Class có nhiều Students enrolled
- Many-to-many qua Enrollment table
```

### 2.3. Class Entity (Reference)

**Table:** `classes`

**Mô tả:** Classes. Chi tiết trong Class Module.

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| course_id | BIGINT | FK to courses.id |
| name | VARCHAR(200) | Tên class |
| max_students | INT | Max students |
| current_enrolled | INT | Số students hiện tại |
| class_code | VARCHAR(20) | Mã enroll |
| status | VARCHAR(20) | UPCOMING, ONGOING, COMPLETED, CANCELLED |

**Relationship:**
- Enrollment updates class.current_enrolled
- Enrollment validates class.max_students

---

## 📐 3. Quy Tắc Kinh Doanh

### BR-ENROLLMENT-001: Student Không Thể Enroll 2 Lần Vào Cùng Class

**Mô tả:** Mỗi student chỉ có thể có 1 enrollment record ACTIVE cho 1 class.

**Lý do:** Prevent duplicates.

**Validation:**
```java
boolean exists = enrollmentRepository
    .existsByStudentIdAndClassIdAndStatus(
        studentId, classId, EnrollmentStatus.ACTIVE
    );
if (exists) {
    throw new BusinessException("Student đã enrolled vào class này");
}
```

**Note:** Nếu student đã DROPPED, có thể re-enroll.

---

### BR-ENROLLMENT-002: Class Không Được Vượt Quá Max Students

**Mô tả:** Không thể enroll khi class.current_enrolled >= class.max_students

**Lý do:** Class capacity limit.

**Validation:**
```java
if (clazz.getCurrentEnrolled() >= clazz.getMaxStudents()) {
    throw new BusinessException(
        "Class đã đầy. Max: " + clazz.getMaxStudents()
    );
}
```

---

### BR-ENROLLMENT-003: Chỉ Enroll Vào UPCOMING/ONGOING Classes

**Mô tả:** Không thể enroll vào COMPLETED hoặc CANCELLED classes.

**Lý do:** Classes không còn active.

**Validation:**
```java
if (clazz.getStatus() == ClassStatus.COMPLETED ||
    clazz.getStatus() == ClassStatus.CANCELLED) {
    throw new BusinessException(
        "Không thể enroll vào class này (status: " + clazz.getStatus() + ")"
    );
}
```

---

### BR-ENROLLMENT-004: Class Code Phải Valid và Chưa Expired

**Mô tả:** Khi enroll bằng code, code phải match và chưa expired.

**Lý do:** Security và control enrollment window.

**Validation:**
```java
// Check code match
if (!clazz.getClassCode().equals(code)) {
    throw new BusinessException("Class code không đúng");
}

// Check expiration
if (clazz.getCodeExpiresAt() != null &&
    LocalDateTime.now().isAfter(clazz.getCodeExpiresAt())) {
    throw new BusinessException("Class code đã hết hạn");
}
```

---

### BR-ENROLLMENT-005: Student Phải ACTIVE Để Enroll

**Mô tả:** Chỉ ACTIVE students mới có thể enroll.

**Lý do:** INACTIVE students không nên học.

**Validation:**
```java
Student student = studentRepository.findById(studentId)
    .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

if (student.getStatus() != StudentStatus.ACTIVE) {
    throw new BusinessException("Student account không active");
}
```

---

### BR-ENROLLMENT-006: Auto Update current_enrolled Count

**Mô tả:** Khi create/drop enrollment, auto update class.current_enrolled.

**Lý do:** Maintain accurate count.

**Implementation:**
```java
// On create enrollment (ACTIVE)
clazz.setCurrentEnrolled(clazz.getCurrentEnrolled() + 1);
classRepository.save(clazz);

// On drop enrollment (ACTIVE → DROPPED)
clazz.setCurrentEnrolled(clazz.getCurrentEnrolled() - 1);
classRepository.save(clazz);
```

---

### BR-ENROLLMENT-007: Không Thể Drop Sau Refund Deadline

**Mô tả:** Nếu có refund deadline, không thể drop sau deadline để nhận refund.

**Lý do:** Business policy.

**Implementation:**
```java
// Example: Refund deadline = 7 days trước class start date
LocalDate refundDeadline = clazz.getStartDate().minusDays(7);
if (LocalDate.now().isAfter(refundDeadline)) {
    // Có thể drop nhưng không refund
    return false; // No refund
}
return true; // Can refund
```

---

## 🎯 4. Use Cases

### Overview

Module Enrollment hỗ trợ full student enrollment workflow:

**Enrollment Creation:**
- UC-ENROLLMENT-001: Enroll by Class Code (Self-enrollment)
- UC-ENROLLMENT-002: Admin Enroll Student
- UC-ENROLLMENT-003: Batch Enroll Students

**Enrollment Management:**
- UC-ENROLLMENT-004: Get Enrollment Details
- UC-ENROLLMENT-005: List Student Enrollments
- UC-ENROLLMENT-006: List Class Enrollments (Roster)

**Enrollment Lifecycle:**
- UC-ENROLLMENT-007: Drop from Class (Student-initiated)
- UC-ENROLLMENT-008: Withdraw Student (Admin-initiated)
- UC-ENROLLMENT-009: Complete Enrollment (Auto when class COMPLETED)
- UC-ENROLLMENT-010: Cancel Enrollment (Auto when class CANCELLED)

**Validation:**
- UC-ENROLLMENT-011: Validate Class Code
- UC-ENROLLMENT-012: Check Enrollment Eligibility

---

### UC-ENROLLMENT-001: Enroll by Class Code (Self-Enrollment)

**Người thực hiện:** STUDENT

**Mục đích:** Student tự enroll vào class bằng class code

**Điều kiện trước:**
- Student đã login
- Student có class code từ teacher
- Student status = ACTIVE

**Luồng chính:**

1. Student truy cập Enrollment page hoặc Course Catalog
2. Student click "Tham gia lớp bằng mã"
3. Frontend hiển thị form:
   - Input: Class code (6-20 chars)
   - Submit button
4. Student nhập code (e.g., "ABC123")
5. Student click "Tham gia"
6. Frontend gửi POST `/api/v1/enrollments/join-by-code`
   ```json
   {
     "classCode": "ABC123"
   }
   ```
7. Hệ thống validate:
   - Code exists trong database
   - **BR-ENROLLMENT-004:** Code valid và chưa expired
   - **BR-ENROLLMENT-003:** Class status UPCOMING hoặc ONGOING
   - **BR-ENROLLMENT-002:** Class chưa full
   - **BR-ENROLLMENT-001:** Student chưa enrolled
   - **BR-ENROLLMENT-005:** Student ACTIVE
8. Hệ thống tạo Enrollment:
   - student_id = currentUserId
   - class_id = foundClassId
   - status = ACTIVE
   - enrollment_method = CLASS_CODE
   - enrolled_by = studentId (self)
9. Hệ thống update Class:
   - current_enrolled += 1
10. Hệ thống trả về HTTP 201 Created
11. Frontend redirect đến Class Detail page
12. Student thấy: "Bạn đã tham gia lớp học thành công!"
13. Student nhận welcome email với class info

**Luồng thay thế:**

**AF1 - Code không đúng:**
- Tại bước 7, code không tồn tại
- Trả về HTTP 404 Not Found
- Message: "Mã lớp không đúng. Vui lòng kiểm tra lại."

**AF2 - Code đã hết hạn:**
- Tại bước 7, code.expiresAt < NOW()
- Trả về HTTP 400 Bad Request
- Message: "Mã lớp đã hết hạn. Vui lòng liên hệ giáo viên."

**AF3 - Class đã full:**
- Tại bước 7, current_enrolled >= max_students
- Trả về HTTP 409 Conflict
- Message: "Lớp học đã đầy ({current}/{max})"

**AF4 - Already enrolled:**
- Tại bước 7, student đã có enrollment ACTIVE
- Trả về HTTP 409 Conflict
- Message: "Bạn đã tham gia lớp này rồi"

**Kết quả:**
- Enrollment created với status ACTIVE
- Student xuất hiện trong class roster
- Student có thể attend classes, view materials, submit assignments
- current_enrolled increased

**Events:**
- Event: `STUDENT_ENROLLED` (enrollmentId, studentId, classId, method=CLASS_CODE)

---

### UC-ENROLLMENT-002: Admin Enroll Student

**Người thực hiện:** ADMIN/OWNER

**Mục đích:** Admin enroll student vào class (không cần code)

**Điều kiện trước:**
- Admin đã login
- Student và Class tồn tại

**Luồng chính:**

1. Admin truy cập Class Detail → Students tab
2. Admin click "Thêm học sinh"
3. Frontend hiển thị form:
   - Select Student (dropdown hoặc search)
   - Note field (optional)
4. Admin select student và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/enrollments`
   ```json
   {
     "studentId": 25,
     "note": "Offline registration"
   }
   ```
6. Hệ thống validate:
   - Student và Class tồn tại
   - **BR-ENROLLMENT-002:** Class chưa full
   - **BR-ENROLLMENT-001:** Student chưa enrolled
   - **BR-ENROLLMENT-005:** Student ACTIVE
7. Hệ thống tạo Enrollment:
   - enrollment_method = ADMIN
   - enrolled_by = adminId
   - status = ACTIVE
8. Hệ thống update Class current_enrolled
9. Hệ thống trả về HTTP 201 Created
10. Frontend update student roster
11. Student nhận email notification

**Kết quả:**
- Student enrolled vào class
- Student nhận welcome email
- Xuất hiện trong roster

**Events:**
- Event: `STUDENT_ENROLLED` (enrollmentId, studentId, classId, method=ADMIN, enrolledBy=adminId)

---

### UC-ENROLLMENT-003: Batch Enroll Students

**Người thực hiện:** ADMIN/OWNER

**Mục đích:** Enroll nhiều students cùng lúc

**Điều kiện trước:**
- Admin đã login
- Có list student IDs

**Luồng chính:**

1. Admin truy cập Class Detail → Students tab
2. Admin click "Batch enroll"
3. Frontend hiển thị form:
   - Upload CSV file (student IDs hoặc emails)
   - Hoặc paste student IDs
4. Admin upload/paste và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/enrollments/batch`
   ```json
   {
     "studentIds": [10, 11, 12, 13, 14],
     "note": "Fall 2026 batch"
   }
   ```
6. Hệ thống process từng student:
   - Validate student exists và ACTIVE
   - Check chưa enrolled
   - Check class capacity
7. Hệ thống create enrollments (trong transaction)
8. Hệ thống update current_enrolled
9. Hệ thống trả về HTTP 200 OK với result summary:
   ```json
   {
     "success": true,
     "data": {
       "totalRequested": 5,
       "successfulEnrollments": 4,
       "failedEnrollments": 1,
       "errors": [
         {
           "studentId": 13,
           "reason": "Student đã enrolled"
         }
       ]
     }
   }
   ```
10. Frontend hiển thị summary
11. Students nhận emails

**Kết quả:**
- Multiple enrollments created
- Summary report với successes/failures
- Students notified

**Events:**
- Event: `BATCH_ENROLLMENT_COMPLETED` (classId, successCount, failCount)

---

### UC-ENROLLMENT-007: Drop from Class (Student-Initiated)

**Người thực hiện:** STUDENT

**Mục đích:** Student tự rút khỏi class

**Điều kiện trước:**
- Student đã enrolled (status ACTIVE)
- Student có quyền drop (chưa quá deadline nếu có)

**Luồng chính:**

1. Student truy cập My Classes → Enrolled class
2. Student click "Rút khỏi lớp"
3. Frontend hiển thị confirmation dialog:
   - "Bạn có chắc muốn rút khỏi lớp này?"
   - "Sau khi rút, bạn sẽ không thể attend classes"
   - Refund info (nếu applicable)
   - Required: Reason for dropping
4. Student nhập reason và confirm
5. Frontend gửi POST `/api/v1/enrollments/{enrollmentId}/drop`
   ```json
   {
     "reason": "Schedule conflict"
   }
   ```
6. Hệ thống validate:
   - Enrollment tồn tại và status ACTIVE
   - Student owns enrollment
   - Chưa quá drop deadline (nếu có policy)
7. Hệ thống update Enrollment:
   - status = ACTIVE → DROPPED
   - dropped_at = NOW()
   - drop_reason = reason
8. Hệ thống update Class:
   - current_enrolled -= 1
9. Hệ thống check refund eligibility:
   - **BR-ENROLLMENT-007:** Check refund deadline
   - If eligible: Trigger refund workflow
10. Hệ thống trả về HTTP 200 OK
11. Frontend hiển thị: "Bạn đã rút khỏi lớp"
12. Student không còn access class materials

**Luồng thay thế:**

**AF1 - Quá drop deadline:**
- Tại bước 6, quá refund deadline
- Warning: "Bạn vẫn có thể drop nhưng không được refund"
- Student có thể proceed hoặc cancel

**AF2 - Already DROPPED:**
- Tại bước 6, enrollment đã DROPPED
- Trả về HTTP 409 Conflict
- Message: "Bạn đã rút khỏi lớp này rồi"

**Kết quả:**
- Enrollment status = DROPPED
- current_enrolled decreased
- Refund processed (nếu eligible)
- Student không còn trong roster

**Events:**
- Event: `STUDENT_DROPPED` (enrollmentId, studentId, classId, reason, refundEligible)

---

### UC-ENROLLMENT-009: Complete Enrollment (Auto)

**Người thực hiện:** System (Auto trigger)

**Mục đích:** Auto complete enrollments khi class COMPLETED

**Điều kiện trước:**
- Class status changed to COMPLETED

**Luồng chính:**

1. System detect Class.status → COMPLETED (event listener)
2. System query all ACTIVE enrollments cho class
3. For each enrollment:
   - Update status = ACTIVE → COMPLETED
   - Set completed_at = NOW()
4. System calculate final grades (integration với Grade Module)
5. System trigger certificate generation (future)
6. Students nhận completion notifications

**Kết quả:**
- All enrollments marked COMPLETED
- Enrollment history preserved
- Students receive completion notifications

**Events:**
- Event: `ENROLLMENTS_COMPLETED` (classId, studentCount)

---

### UC-ENROLLMENT-010: Cancel Enrollment (Auto)

**Người thực hiện:** System (Auto trigger)

**Mục đích:** Auto cancel enrollments khi class CANCELLED

**Điều kiện trước:**
- Class status changed to CANCELLED

**Luồng chính:**

1. System detect Class.status → CANCELLED (event listener)
2. System query all ACTIVE enrollments cho class
3. For each enrollment:
   - Update status = ACTIVE → CANCELLED
   - Set cancelled_at = NOW()
   - current_enrolled -= 1 (cho class)
4. System trigger refund workflow cho all students
5. Students nhận cancellation + refund notifications

**Kết quả:**
- All enrollments marked CANCELLED
- Refunds initiated
- Students notified

**Events:**
- Event: `ENROLLMENTS_CANCELLED` (classId, studentCount, refundAmount)

---

### UC-ENROLLMENT-011: Validate Class Code

**Người thực hiện:** System (Internal validation)

**Mục đích:** Validate class code trước khi enroll

**Luồng chính:**

1. System nhận class code
2. System query classes table:
   ```sql
   SELECT * FROM classes WHERE class_code = :code
   ```
3. System validate:
   - Code exists
   - Code chưa expired
   - Class status valid (UPCOMING/ONGOING)
   - Class chưa full
4. System trả về validation result

**Response:**
```json
{
  "valid": true,
  "classId": 15,
  "className": "English B1 - Evening",
  "currentEnrolled": 18,
  "maxStudents": 20,
  "spotsLeft": 2,
  "codeExpiresAt": "2026-03-01T23:59:59Z"
}
```

**Use case:** Frontend preview class info before enrollment

---

## 🔐 5. Permission Model

### Enrollment Permissions

**Roles:**
- **OWNER/ADMIN:** Full access all enrollments
- **TEACHER (assigned to class):** View enrollments in their classes
- **STUDENT:** View/manage own enrollments only

**Permission Matrix:**

| Operation | OWNER/ADMIN | TEACHER | STUDENT |
|-----------|-------------|---------|---------|
| Enroll by code | ✅ | ✅ | ✅ |
| Admin enroll | ✅ | ❌ | ❌ |
| Batch enroll | ✅ | ❌ | ❌ |
| View roster | ✅ | Assigned classes | Own enrollments |
| Drop student | ✅ | ❌ | Own only |
| Complete enrollment | ✅ (manual) | ❌ | ❌ |
| Cancel enrollment | ✅ | ❌ | ❌ |

---

## 🔗 6. Integration với Other Modules

### 6.1. Class Module Integration

**Dependency:** Enrollment depends on Class Module

**Integration points:**
- Enrollment.class_id FK to classes.id
- Update class.current_enrolled
- Validate class.max_students

**APIs used:**
```java
// Get class info
GET /internal/classes/{classId}

// Update current_enrolled
PATCH /internal/classes/{classId}/enrolled-count
```

### 6.2. Student Module Integration

**Dependency:** Enrollment depends on Student Module

**Integration points:**
- Enrollment.student_id FK to students.id
- Validate student.status = ACTIVE

**APIs used:**
```java
// Get student info
GET /internal/students/{studentId}

// Validate student active
GET /internal/students/{studentId}/status
```

### 6.3. Gateway Integration (UC-GAT-008)

**Integration point:** Join Class by Code

**Flow:**
```
Gateway:
1. Student login
2. POST /api/v1/enrollments/join-by-code {code}

Gateway → Core:
3. Validate user type = STUDENT
4. Get student.referenceId
5. Call Core: POST /internal/enrollments
   {
     studentId: referenceId,
     classCode: code
   }

Core:
6. Validate code
7. Create enrollment
8. Return success

Gateway:
9. Return to student
```

### 6.4. Payment Module Integration (Future)

**Integration points:**
- payment_status field
- payment_amount field
- Refund workflows

---

## 📊 7. Summary

### Entities
- ✅ **Enrollment:** Main entity
- ✅ **Student:** Reference (Student Module)
- ✅ **Class:** Reference (Class Module)

### Business Rules
- ✅ BR-ENROLLMENT-001: Không enroll 2 lần
- ✅ BR-ENROLLMENT-002: Class không vượt max students
- ✅ BR-ENROLLMENT-003: Chỉ enroll UPCOMING/ONGOING
- ✅ BR-ENROLLMENT-004: Class code valid và chưa expired
- ✅ BR-ENROLLMENT-005: Student ACTIVE
- ✅ BR-ENROLLMENT-006: Auto update current_enrolled
- ✅ BR-ENROLLMENT-007: Refund deadline

### Use Cases

**Enrollment Creation:**
- ✅ UC-ENROLLMENT-001: Enroll by Class Code
- ✅ UC-ENROLLMENT-002: Admin Enroll Student
- ✅ UC-ENROLLMENT-003: Batch Enroll

**Enrollment Management:**
- ✅ UC-ENROLLMENT-004: Get Enrollment Details
- ✅ UC-ENROLLMENT-005: List Student Enrollments
- ✅ UC-ENROLLMENT-006: List Class Enrollments

**Enrollment Lifecycle:**
- ✅ UC-ENROLLMENT-007: Drop from Class
- ✅ UC-ENROLLMENT-008: Withdraw Student (Admin)
- ✅ UC-ENROLLMENT-009: Complete Enrollment (Auto)
- ✅ UC-ENROLLMENT-010: Cancel Enrollment (Auto)

**Validation:**
- ✅ UC-ENROLLMENT-011: Validate Class Code
- ✅ UC-ENROLLMENT-012: Check Eligibility

**Total:** 12 use cases

### Lifecycle
```
PENDING → ACTIVE → COMPLETED
            ↓
         DROPPED
            ↓
        CANCELLED

PENDING:
- Chờ payment (future feature)

ACTIVE:
- Student đang học
- Attend classes, submit assignments

COMPLETED:
- Class finished
- Final grade calculated

DROPPED:
- Student tự rút
- Có thể refund (nếu đúng deadline)

CANCELLED:
- Class cancelled
- Auto refund
```

### Integration
- ✅ Class Module: current_enrolled tracking
- ✅ Student Module: Student validation
- ✅ Gateway: UC-GAT-008 (Join by code)
- ✅ Payment Module: Refund workflow (future)
- ✅ Attendance Module: Enrollment validation
- ✅ Grade Module: Final grades

---

## 🚀 Next Steps

**Sau khi document này được approve:**

1. **Create PR 2.6: Enrollment Module**
   - Implement Enrollment entity
   - Implement repositories
   - Implement services (enrollment logic)
   - Implement REST APIs
   - Implement class code validation
   - Implement auto cancel/complete listeners
   - Write tests (unit + integration)

2. **Update Gateway UC-GAT-008**
   - Implement join-by-code endpoint
   - Call Core enrollment API
   - Test integration

3. **Update Class Module**
   - Ensure current_enrolled updates correctly
   - Test capacity enforcement

---

**Author:** VictorAurelius + Claude Sonnet 4.5
**Created:** 2026-01-28
**Status:** Ready for Review
**Next:** Update implementation plan với new PRs
