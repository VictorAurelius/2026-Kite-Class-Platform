# Attendance Module - Business Logic

## Mục lục
1. [Tổng quan](#tổng-quan)
2. [Business Rules](#business-rules)
3. [Use Cases](#use-cases)
4. [Entity Design](#entity-design)
5. [Integration Points](#integration-points)
6. [Events & Notifications](#events--notifications)

---

## Tổng quan

### Mục đích
Attendance Module quản lý điểm danh học viên trong các buổi học (class sessions). Module này:
- Ghi nhận trạng thái tham gia của học viên (có mặt, muộn, vắng, nghỉ phép)
- Tính toán tỷ lệ chuyên cần (attendance rate)
- Tích hợp với Grade Module để tính điểm chuyên cần
- Hỗ trợ nhiều phương thức điểm danh (thủ công, QR code, location-based)
- Cung cấp báo cáo và thống kê điểm danh

### Scope
**Bao gồm:**
- ✅ Điểm danh cho class sessions (theo lịch Class Module)
- ✅ Các trạng thái: PRESENT, LATE, ABSENT, EXCUSED_ABSENCE
- ✅ Tính toán attendance rate tự động
- ✅ Check-in qua QR code (teacher tạo QR cho session)
- ✅ Ghi chú lý do vắng mặt/muộn
- ✅ Báo cáo điểm danh cho student, teacher, admin
- ✅ Integration với Grade Module (attendance component)
- ✅ Export attendance reports (Excel, PDF)

**Không bao gồm:**
- ❌ Biometric check-in (fingerprint, face recognition) - Future
- ❌ Automatic location tracking - Privacy concerns
- ❌ Penalty/reward system - Handled by Gamification Module (P1)

### Key Concepts
- **Class Session**: Một buổi học cụ thể trong class schedule (from Class Module)
- **Attendance Record**: Bản ghi điểm danh của 1 student trong 1 session
- **Attendance Rate**: Tỷ lệ chuyên cần = (PRESENT + LATE) / Total Sessions × 100%
- **Grace Period**: Thời gian cho phép check-in muộn mà vẫn được tính PRESENT (ví dụ: 5 phút)
- **Late Threshold**: Thời gian tối đa để được tính LATE (sau grace period, ví dụ: 15 phút)
- **QR Code Check-in**: Teacher tạo QR code cho session, students scan để check-in

---

## Business Rules

### BR-ATT-001: Attendance Record Uniqueness
**Quy tắc**: Mỗi student chỉ có 1 attendance record cho 1 class session.

**Validation**:
```
UNIQUE CONSTRAINT (student_id, class_session_id)
```

**Lý do**: Đảm bảo tính toàn vẹn dữ liệu, tránh duplicate records.

---

### BR-ATT-002: Attendance Status Rules
**Quy tắc**: Attendance status được xác định dựa trên check-in time so với session start time.

**Logic**:
```
Let:
  session_start = class_session.start_time
  grace_period = 5 minutes (configurable)
  late_threshold = 15 minutes (configurable)
  check_in_time = attendance.check_in_time

Status =
  if check_in_time is null:
    ABSENT
  else if check_in_time <= session_start + grace_period:
    PRESENT
  else if check_in_time <= session_start + late_threshold:
    LATE
  else if check_in_time > session_start + late_threshold AND has_excuse_note:
    EXCUSED_ABSENCE
  else:
    ABSENT (quá muộn, không được chấp nhận)
```

**Exceptions**:
- Teacher có thể manual override status (ví dụ: student có lý do chính đáng)
- EXCUSED_ABSENCE chỉ được set khi có ghi chú lý do hợp lệ

---

### BR-ATT-003: Attendance Permission Rules
**Quy tắc**: Quyền điểm danh dựa trên role và resource ownership.

**Permissions**:
```
Mark Attendance (CREATE/UPDATE):
- Teacher với role MAIN_TEACHER hoặc ASSISTANT của class
- Admin (ADMIN role)

View Attendance:
- Student: chỉ xem attendance của chính mình
- Teacher: xem attendance của class được assigned
- Admin: xem tất cả

Delete Attendance:
- Chỉ Admin (trong trường hợp sửa lỗi)
```

---

### BR-ATT-004: Attendance Rate Calculation
**Quy tắc**: Attendance rate được tính dựa trên số sessions đã diễn ra (status = COMPLETED).

**Formula**:
```java
int totalSessions = count(ClassSession where status = COMPLETED AND class_id = X);
int presentCount = count(Attendance where student_id = Y AND class_id = X AND status IN (PRESENT, LATE));
int excusedCount = count(Attendance where student_id = Y AND class_id = X AND status = EXCUSED_ABSENCE);

// Option 1: Strict (chỉ tính PRESENT + LATE)
BigDecimal attendanceRate = presentCount / totalSessions * 100;

// Option 2: Lenient (tính cả EXCUSED_ABSENCE)
BigDecimal attendanceRate = (presentCount + excusedCount) / totalSessions * 100;

// Default: Option 1 (Strict)
```

**Lưu ý**:
- Chỉ tính sessions đã hoàn thành (không tính future sessions)
- EXCUSED_ABSENCE không được tính vào attendance rate (theo option 1)
- Kết quả làm tròn 2 chữ số thập phân

---

### BR-ATT-005: QR Code Check-in Validation
**Quy tắc**: QR code check-in chỉ hợp lệ trong thời gian session diễn ra.

**Validation**:
```
Let:
  session_start = class_session.start_time
  session_end = class_session.end_time
  current_time = now()
  qr_expiry = session_end + 5 minutes (grace period)

QR code is valid if:
  current_time >= session_start - 10 minutes (cho phép check-in sớm 10 phút)
  AND
  current_time <= qr_expiry

Otherwise: throw "QR code đã hết hạn hoặc chưa đến giờ check-in"
```

---

### BR-ATT-006: Late Penalty Integration with Grade
**Quy tắc**: Late attendance có thể bị trừ điểm trong Grade Module (nếu teacher cấu hình).

**Integration**:
```
GradeComponent (type = ATTENDANCE):
  - score calculation:
    presentScore = 100
    lateScore = 80 (configurable, default 80%)
    absentScore = 0
    excusedScore = 50 (configurable, default 50%)

  - finalScore = (presentCount * presentScore + lateCount * lateScore + excusedCount * excusedScore) / totalSessions
```

**Lưu ý**: Teacher có thể customize điểm số cho mỗi status trong class settings.

---

### BR-ATT-007: Retroactive Attendance Update
**Quy tắc**: Attendance record có thể được update sau session kết thúc, nhưng phải có audit trail.

**Constraints**:
```
Update allowed if:
  - Session status = COMPLETED
  - Update within 7 days after session_end
  - OR performer has ADMIN role (no time limit)

Audit trail:
  - Ghi lại: who, when, old_value, new_value, reason
  - Stored in attendance_audit_logs table
```

---

### BR-ATT-008: Bulk Attendance Import Validation
**Quy tắc**: Khi import điểm danh hàng loạt (Excel), validate tất cả records trước khi save.

**Validation Steps**:
```
1. File format: .xlsx, max 10MB
2. Required columns: student_id, session_id, status, check_in_time (optional), notes (optional)
3. Student must be enrolled in class
4. Session must exist and belong to class
5. Status must be valid enum
6. No duplicate records trong import file

Nếu có lỗi:
  - Rollback toàn bộ import
  - Return error report với row numbers
```

---

## Use Cases

### UC-ATT-001: Mark Attendance (Teacher)

**Actor**: Teacher (MAIN_TEACHER, ASSISTANT)

**Preconditions**:
- Teacher được assign vào class (TeacherClass record exists)
- Class session đã bắt đầu hoặc đã kết thúc (status = ONGOING or COMPLETED)
- Students đã enrolled vào class (Enrollment status = ACTIVE)

**Trigger**: Teacher mở trang điểm danh cho session

**Luồng chính**:
1. Teacher chọn class và session cần điểm danh
2. System hiển thị danh sách students enrolled (from Enrollment Module)
3. Với mỗi student, teacher chọn status:
   - PRESENT: Có mặt đúng giờ
   - LATE: Đến muộn
   - ABSENT: Vắng mặt không phép
   - EXCUSED_ABSENCE: Vắng có phép (requires notes)
4. Nếu chọn LATE hoặc EXCUSED_ABSENCE: Teacher nhập notes (optional cho LATE, required cho EXCUSED_ABSENCE)
5. Teacher nhập check_in_time (optional, default = current time)
6. Teacher nhấn "Lưu điểm danh"
7. System validates:
   - BR-ATT-001: No duplicate records
   - BR-ATT-003: Teacher có quyền mark attendance
   - BR-ATT-002: Status phù hợp với check_in_time
8. System creates/updates Attendance records
9. System tính toán lại attendance_rate cho mỗi student (BR-ATT-004)
10. System publishes `ATTENDANCE_MARKED` event
11. System shows success message

**Postconditions**:
- Attendance records được tạo/cập nhật
- Attendance rate được update
- Grade Module nhận event để recalculate attendance component (nếu có)

**Business Rules**: BR-ATT-001, BR-ATT-002, BR-ATT-003, BR-ATT-004

**Alternative Flows**:

**A1: Bulk mark all as PRESENT**
- Tại bước 3, teacher chọn "Mark All Present"
- System tự động set status = PRESENT cho tất cả students
- Teacher có thể adjust individual students sau đó

**A2: EXCUSED_ABSENCE without notes**
- Tại bước 4, nếu chọn EXCUSED_ABSENCE nhưng không nhập notes
- System shows validation error: "Vui lòng nhập lý do nghỉ phép"
- Return to step 4

---

### UC-ATT-002: Check-in via QR Code (Student)

**Actor**: Student (enrolled in class)

**Preconditions**:
- Student đã enrolled vào class (Enrollment status = ACTIVE)
- Teacher đã generate QR code cho session (UC-ATT-003)
- Current time trong khoảng check-in window (BR-ATT-005)

**Trigger**: Student scan QR code hiển thị tại lớp

**Luồng chính**:
1. Student mở camera trên mobile app
2. Student scan QR code được teacher hiển thị
3. System validates QR code:
   - QR code format hợp lệ
   - Session ID tồn tại
   - Student enrolled vào class của session
   - Current time trong check-in window (BR-ATT-005)
4. System tạo Attendance record:
   - student_id = current student
   - class_session_id = from QR code
   - check_in_time = current time
   - status = calculated theo BR-ATT-002
5. System tính toán attendance_rate cho student
6. System publishes `ATTENDANCE_MARKED` event
7. System shows success message với status: "Điểm danh thành công - PRESENT" hoặc "LATE"

**Postconditions**:
- Attendance record được tạo với status PRESENT hoặc LATE
- Student nhìn thấy confirmation trên app
- Teacher thấy attendance record được update real-time

**Business Rules**: BR-ATT-001, BR-ATT-002, BR-ATT-005

**Alternative Flows**:

**A1: QR code hết hạn**
- Tại bước 3, nếu current time > qr_expiry
- System shows error: "QR code đã hết hạn. Vui lòng liên hệ giáo viên."
- End use case

**A2: Student đã check-in rồi**
- Tại bước 4, nếu attendance record already exists (BR-ATT-001 violation)
- System shows message: "Bạn đã điểm danh cho buổi học này lúc {check_in_time}. Status: {status}"
- End use case (không update record)

**A3: Quá muộn (sau late_threshold)**
- Tại bước 4, nếu check_in_time > session_start + late_threshold
- System sets status = ABSENT (quá muộn không chấp nhận)
- System shows warning: "Bạn đến quá muộn. Vui lòng liên hệ giáo viên để xin phép."

---

### UC-ATT-003: Generate QR Code for Session (Teacher)

**Actor**: Teacher (MAIN_TEACHER, ASSISTANT)

**Preconditions**:
- Teacher được assign vào class
- Class session tồn tại
- Session status = UPCOMING hoặc ONGOING

**Trigger**: Teacher chuẩn bị điểm danh cho buổi học

**Luồng chính**:
1. Teacher chọn class và session
2. Teacher nhấn "Tạo QR Code Điểm Danh"
3. System generates QR code payload:
   ```json
   {
     "type": "attendance_checkin",
     "session_id": 123,
     "class_id": 45,
     "generated_at": "2026-01-28T09:00:00Z",
     "expires_at": "2026-01-28T11:05:00Z"
   }
   ```
4. System encodes payload thành QR code image
5. System hiển thị QR code fullscreen trên teacher device
6. Teacher projects QR code lên màn hình lớp học
7. Students scan QR code để check-in (UC-ATT-002)

**Postconditions**:
- QR code được hiển thị và có thể scan
- Teacher có thể refresh QR code bất cứ lúc nào
- QR code tự động invalid sau expires_at

**Business Rules**: BR-ATT-005

**Alternative Flows**:

**A1: Refresh QR Code**
- Tại bước 5, teacher nhấn "Refresh QR Code"
- System generates new QR code với new generated_at và expires_at
- Students đang scan old QR code vẫn hợp lệ (do expires_at chưa qua)

---

### UC-ATT-004: View Student Attendance Report

**Actor**: Student

**Preconditions**:
- Student đã enrolled vào ít nhất 1 class

**Trigger**: Student muốn xem attendance của mình

**Luồng chính**:
1. Student truy cập "My Attendance" page
2. System hiển thị danh sách classes student đã enroll
3. Student chọn 1 class
4. System truy vấn:
   - Tất cả class sessions của class (from Class Module)
   - Attendance records của student cho class đó
5. System tính toán attendance statistics:
   ```java
   int totalSessions = count(sessions với status = COMPLETED);
   int presentCount = count(attendance với status = PRESENT);
   int lateCount = count(attendance với status = LATE);
   int absentCount = count(attendance với status = ABSENT);
   int excusedCount = count(attendance với status = EXCUSED_ABSENCE);
   BigDecimal attendanceRate = (presentCount + lateCount) / totalSessions * 100; // BR-ATT-004
   ```
6. System hiển thị:
   - Bảng attendance records (session date, status, check-in time, notes)
   - Attendance statistics (present/late/absent/excused counts, rate)
   - Progress bar cho attendance rate
   - Warning nếu attendance rate < 80% (configurable threshold)

**Postconditions**:
- Student nhìn thấy chi tiết attendance của mình
- Student aware of attendance rate và có thể improve

**Business Rules**: BR-ATT-004

---

### UC-ATT-005: View Class Attendance Report (Teacher)

**Actor**: Teacher (MAIN_TEACHER, ASSISTANT)

**Preconditions**:
- Teacher được assign vào class

**Trigger**: Teacher muốn xem attendance của cả class

**Luồng chính**:
1. Teacher chọn class
2. Teacher nhấn "Attendance Report"
3. Teacher chọn date range (default: all sessions)
4. System truy vấn:
   - All students enrolled trong class
   - All class sessions trong date range
   - All attendance records
5. System renders report dạng matrix:
   ```
   Student Name  | Session 1 | Session 2 | ... | Attendance Rate
   ------------- | --------- | --------- | --- | ---------------
   Nguyễn Văn A  | P         | L         | ... | 92.5%
   Trần Thị B    | P         | A         | ... | 75.0%
   ...
   ```
   Legend: P = PRESENT, L = LATE, A = ABSENT, E = EXCUSED
6. System tính summary statistics:
   - Average attendance rate của class
   - Students with low attendance (< threshold)
   - Most absent session (session nào có nhiều absent nhất)
7. System hiển thị report với export options (Excel, PDF)

**Postconditions**:
- Teacher có overview về attendance của cả class
- Teacher identify students cần attention

**Business Rules**: BR-ATT-004

---

### UC-ATT-006: Update Attendance Record (Teacher)

**Actor**: Teacher (MAIN_TEACHER, ASSISTANT) hoặc Admin

**Preconditions**:
- Attendance record đã tồn tại
- Session đã completed
- Update trong time window (BR-ATT-007) hoặc actor = Admin

**Trigger**: Teacher phát hiện sai sót trong điểm danh

**Luồng chính**:
1. Teacher mở attendance report cho session
2. Teacher chọn student có attendance record cần sửa
3. Teacher nhấn "Edit Attendance"
4. System hiển thị form:
   - Current status: ABSENT
   - New status: [Dropdown: PRESENT, LATE, ABSENT, EXCUSED_ABSENCE]
   - Check-in time: [DateTime picker]
   - Notes: [Textbox]
   - Reason for change: [Textbox, required]
5. Teacher updates status và notes
6. Teacher nhập reason: "Student có lý do chính đáng, đã nộp giấy phép sau"
7. Teacher nhấn "Save Changes"
8. System validates:
   - BR-ATT-007: Update allowed (within time window or admin)
   - Required fields present
9. System creates audit log:
   ```java
   AttendanceAuditLog {
     attendance_id: 123,
     changed_by: teacher_id,
     changed_at: now(),
     old_status: ABSENT,
     new_status: EXCUSED_ABSENCE,
     old_notes: null,
     new_notes: "Nghỉ phép gia đình",
     reason: "Student có lý do chính đáng, đã nộp giấy phép sau"
   }
   ```
10. System updates Attendance record
11. System recalculates attendance_rate
12. System publishes `ATTENDANCE_UPDATED` event
13. System shows success message

**Postconditions**:
- Attendance record được update
- Audit log được tạo
- Grade Module recalculates attendance component (nếu có)

**Business Rules**: BR-ATT-007

**Alternative Flows**:

**A1: Update quá thời hạn (non-admin)**
- Tại bước 8, nếu current time > session_end + 7 days và actor không phải Admin
- System shows error: "Không thể sửa điểm danh sau 7 ngày. Vui lòng liên hệ Admin."
- End use case

---

### UC-ATT-007: Export Attendance Report

**Actor**: Teacher, Admin

**Preconditions**:
- User có quyền view attendance của class

**Trigger**: User muốn export attendance data

**Luồng chính**:
1. User mở Attendance Report (UC-ATT-005)
2. User nhấn "Export Report"
3. System hiển thị export options:
   - Format: Excel (.xlsx), PDF, CSV
   - Date range: [From - To] (default: all sessions)
   - Include: [☑ Summary statistics, ☑ Attendance rate, ☑ Notes]
4. User chọn options và nhấn "Export"
5. System generates report file:
   - Excel: Matrix layout với colors (green/yellow/red)
   - PDF: Formatted report với class info, teacher name, export date
   - CSV: Raw data cho data analysis
6. System downloads file to user device

**Postconditions**:
- Report file được tạo và download
- User có thể share hoặc archive report

---

### UC-ATT-008: Bulk Import Attendance (Admin)

**Actor**: Admin

**Preconditions**:
- Admin có permission ADMIN
- Excel template đã được download

**Trigger**: Admin cần import điểm danh hàng loạt (ví dụ: từ legacy system)

**Luồng chính**:
1. Admin tải Excel template từ System
2. Admin điền dữ liệu vào template:
   ```
   Class ID | Session ID | Student ID | Status  | Check-in Time       | Notes
   45       | 101        | 1001       | PRESENT | 2026-01-28 09:00:00 |
   45       | 101        | 1002       | LATE    | 2026-01-28 09:12:00 | Stuck in traffic
   45       | 101        | 1003       | ABSENT  |                     |
   ```
3. Admin upload file qua UI
4. System validates file (BR-ATT-008):
   - File format .xlsx, size < 10MB
   - Required columns present
   - All student_ids are enrolled trong class
   - All session_ids exist
   - Status values valid
   - No duplicates trong file
5. System shows preview:
   - Total records: 35
   - Valid records: 33
   - Invalid records: 2 (with error details)
6. Admin reviews và nhấn "Confirm Import"
7. System imports valid records trong transaction
8. System tính lại attendance_rate cho all affected students
9. System publishes `ATTENDANCE_BULK_IMPORTED` event
10. System shows summary: "Đã import 33/35 records thành công"

**Postconditions**:
- Valid attendance records được import
- Invalid records được report cho admin xử lý

**Business Rules**: BR-ATT-008

**Alternative Flows**:

**A1: File có lỗi validation**
- Tại bước 4, nếu có critical errors (format sai, columns thiếu)
- System rejects entire file
- System shows error report
- Admin fix errors và re-upload

---

### UC-ATT-009: Get Attendance Statistics for Admin Dashboard

**Actor**: Admin

**Preconditions**: None

**Trigger**: Admin truy cập Dashboard

**Luồng chính**:
1. Admin mở Admin Dashboard
2. System tính toán global attendance statistics:
   ```java
   // Platform-wide statistics
   BigDecimal avgAttendanceRate = average(attendance_rate của tất cả students);
   int totalPresentToday = count(attendance với status = PRESENT AND session.date = today);
   int totalAbsentToday = count(attendance với status = ABSENT AND session.date = today);

   // Class statistics
   List<Class> lowAttendanceClasses = classes với avg_attendance_rate < 80%;

   // Student statistics
   List<Student> atRiskStudents = students với attendance_rate < 70%;
   ```
3. System hiển thị dashboard widgets:
   - Overall attendance rate (platform-wide)
   - Today's attendance summary
   - Low attendance classes (alert)
   - At-risk students list
   - Attendance trend chart (last 30 days)

**Postconditions**:
- Admin có overview về attendance situation
- Admin có thể drill down vào specific classes/students

---

### UC-ATT-010: Calculate Attendance Component for Grade

**Actor**: Grade Module (automated)

**Preconditions**:
- Class has grading configured với attendance component
- Attendance records exist

**Trigger**:
- `ATTENDANCE_MARKED` event received
- Teacher manually triggers grade recalculation
- End of term grade calculation

**Luồng chính**:
1. Grade Module requests attendance data cho student trong class
2. Attendance Module returns:
   ```json
   {
     "student_id": 1001,
     "class_id": 45,
     "total_sessions": 36,
     "present_count": 32,
     "late_count": 3,
     "absent_count": 1,
     "excused_count": 0,
     "attendance_rate": 97.22
   }
   ```
3. Grade Module calculates attendance score theo BR-ATT-006:
   ```java
   // Configurable weights per status
   int presentScore = 100;  // 100% score
   int lateScore = 80;      // 80% score
   int absentScore = 0;     // 0% score
   int excusedScore = 50;   // 50% score

   BigDecimal totalScore =
     (presentCount * presentScore +
      lateCount * lateScore +
      absentCount * absentScore +
      excusedCount * excusedScore);

   BigDecimal attendanceComponentScore = totalScore / totalSessions;
   // Result: (32*100 + 3*80 + 1*0 + 0*50) / 36 = 95.56/100
   ```
4. Grade Module saves GradeComponent:
   ```java
   GradeComponent {
     grade_id: ...,
     component_type: ATTENDANCE,
     name: "Chuyên cần",
     max_score: 100,
     score: 95.56,
     weight: 10  // 10% of final grade
   }
   ```

**Postconditions**:
- Attendance component được add vào Grade
- Final grade được recalculate với attendance weight

**Business Rules**: BR-ATT-006

---

### UC-ATT-011: Request Excused Absence (Student)

**Actor**: Student

**Preconditions**:
- Student enrolled trong class
- Session đã hoặc sắp diễn ra
- Student có lý do chính đáng (sick, family emergency, etc.)

**Trigger**: Student biết sẽ vắng mặt hoặc đã vắng mặt

**Luồng chính**:
1. Student truy cập "My Attendance" page
2. Student chọn session cần xin phép
3. Student nhấn "Request Excused Absence"
4. System hiển thị form:
   - Session: [Readonly: Session 12 - 2026-01-28 09:00]
   - Reason: [Dropdown: Sick, Family Emergency, Other]
   - Details: [Textbox, required]
   - Attachment: [File upload, optional - medical certificate, etc.]
5. Student điền form:
   - Reason: Sick
   - Details: "Bị sốt cao, đã đi bác sĩ"
   - Attachment: medical_certificate.pdf
6. Student nhấn "Submit Request"
7. System tạo ExcusedAbsenceRequest:
   ```java
   ExcusedAbsenceRequest {
     student_id: 1001,
     class_session_id: 101,
     reason: SICK,
     details: "Bị sốt cao, đã đi bác sĩ",
     attachment_url: "s3://...",
     status: PENDING,
     created_at: now()
   }
   ```
8. System gửi notification cho teacher (assigned to session)
9. System shows success: "Đơn xin phép đã được gửi, chờ giáo viên duyệt"

**Postconditions**:
- Request được tạo với status PENDING
- Teacher nhận notification để review (UC-ATT-012)

---

### UC-ATT-012: Approve/Reject Excused Absence Request (Teacher)

**Actor**: Teacher (MAIN_TEACHER, ASSISTANT)

**Preconditions**:
- ExcusedAbsenceRequest exists với status = PENDING
- Teacher assigned to class

**Trigger**: Teacher nhận notification về request mới

**Luồng chính**:
1. Teacher mở "Absence Requests" page
2. System hiển thị danh sách pending requests:
   ```
   Student      | Session       | Reason | Details                     | Attachment | Action
   Nguyễn Văn A | 2026-01-28... | Sick   | Bị sốt cao, đã đi bác sĩ   | [View PDF] | [Approve/Reject]
   ```
3. Teacher review request details
4. Teacher nhấn "Approve" hoặc "Reject"
5. Nếu Reject, teacher nhập rejection reason: "Lý do không hợp lệ, vui lòng nộp giấy xác nhận"
6. System updates request:
   ```java
   request.status = APPROVED; // or REJECTED
   request.reviewed_by = teacher_id;
   request.reviewed_at = now();
   request.rejection_reason = "..." (if rejected);
   ```
7. Nếu APPROVED:
   - System tạo/updates Attendance record:
     ```java
     Attendance {
       status: EXCUSED_ABSENCE,
       notes: request.details,
       check_in_time: null
     }
     ```
   - System recalculates attendance_rate
   - System publishes `ATTENDANCE_UPDATED` event
8. System gửi notification cho student về decision
9. System shows success message

**Postconditions**:
- Request được approve/reject
- Nếu approved: Attendance record updated to EXCUSED_ABSENCE
- Student nhận notification

---

## Entity Design

### Attendance Entity

**Purpose**: Ghi nhận trạng thái tham gia của student trong class session

**Database Schema**:
```sql
CREATE TABLE attendances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    class_session_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PRESENT, LATE, ABSENT, EXCUSED_ABSENCE
    check_in_time TIMESTAMP NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,

    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_session FOREIGN KEY (class_session_id)
        REFERENCES class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uk_attendance_student_session UNIQUE (student_id, class_session_id),
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED_ABSENCE'))
);

CREATE INDEX idx_attendance_student ON attendances(student_id);
CREATE INDEX idx_attendance_class ON attendances(class_id);
CREATE INDEX idx_attendance_session ON attendances(class_session_id);
CREATE INDEX idx_attendance_status ON attendances(status);
```

**Java Entity**:
```java
@Entity
@Table(name = "attendances")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "class_session_id", nullable = false)
    private Long classSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

public enum AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    EXCUSED_ABSENCE
}
```

---

### AttendanceAuditLog Entity

**Purpose**: Audit trail cho attendance updates

**Database Schema**:
```sql
CREATE TABLE attendance_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attendance_id BIGINT NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NOT NULL,
    old_check_in_time TIMESTAMP NULL,
    new_check_in_time TIMESTAMP NULL,
    old_notes TEXT NULL,
    new_notes TEXT NULL,
    reason TEXT NOT NULL,

    CONSTRAINT fk_audit_attendance FOREIGN KEY (attendance_id)
        REFERENCES attendances(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_attendance ON attendance_audit_logs(attendance_id);
CREATE INDEX idx_audit_changed_by ON attendance_audit_logs(changed_by);
```

---

### ExcusedAbsenceRequest Entity

**Purpose**: Đơn xin phép nghỉ học của student

**Database Schema**:
```sql
CREATE TABLE excused_absence_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    class_session_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL, -- SICK, FAMILY_EMERGENCY, OTHER
    details TEXT NOT NULL,
    attachment_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    rejection_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_absence_student FOREIGN KEY (student_id)
        REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_absence_session FOREIGN KEY (class_session_id)
        REFERENCES class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT chk_absence_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_absence_student ON excused_absence_requests(student_id);
CREATE INDEX idx_absence_session ON excused_absence_requests(class_session_id);
CREATE INDEX idx_absence_status ON excused_absence_requests(status);
```

---

## Integration Points

### 1. Class Module Integration
**Purpose**: Lấy class sessions để điểm danh

**Integration**:
```java
// Attendance Module calls Class Module API
GET /api/v1/classes/{classId}/sessions

Response:
List<ClassSession> {
    id, class_id, session_number, start_time, end_time,
    status, room, description
}
```

**Usage**:
- UC-ATT-001: Load sessions for attendance marking
- UC-ATT-002: Validate session exists for QR check-in

---

### 2. Enrollment Module Integration
**Purpose**: Lấy danh sách students enrolled trong class

**Integration**:
```java
// Attendance Module calls Enrollment Module API
GET /api/v1/classes/{classId}/enrollments?status=ACTIVE

Response:
List<Enrollment> {
    student_id, class_id, status, enrolled_at
}
```

**Usage**:
- UC-ATT-001: Load student list for attendance marking
- UC-ATT-002: Validate student enrolled trước khi accept check-in

---

### 3. Grade Module Integration
**Purpose**: Provide attendance data cho grade calculation

**Event-Driven**:
```java
// Attendance Module publishes events
Event: ATTENDANCE_MARKED
Payload: {
    student_id, class_id, session_id, status, attendance_rate
}

Event: ATTENDANCE_UPDATED
Payload: {
    student_id, class_id, old_status, new_status, new_attendance_rate
}

// Grade Module subscribes
@EventListener
void onAttendanceMarked(AttendanceMarkedEvent event) {
    recalculateAttendanceComponent(event.getStudentId(), event.getClassId());
}
```

**API Integration**:
```java
// Grade Module calls Attendance Module
GET /api/v1/attendance/students/{studentId}/classes/{classId}/statistics

Response:
AttendanceStatistics {
    total_sessions: 36,
    present_count: 32,
    late_count: 3,
    absent_count: 1,
    excused_count: 0,
    attendance_rate: 97.22
}
```

**Usage**: UC-ATT-010 (Calculate Attendance Component for Grade)

---

### 4. Teacher Module Integration
**Purpose**: Validate teacher permissions

**Integration**:
```java
// Attendance Module calls Teacher Module API
GET /api/v1/teachers/{teacherId}/classes/{classId}/permissions

Response:
TeacherPermissions {
    can_mark_attendance: true,
    can_view_attendance: true,
    role: MAIN_TEACHER
}
```

**Usage**:
- BR-ATT-003 enforcement
- UC-ATT-001, UC-ATT-005, UC-ATT-006

---

## Events & Notifications

### Published Events

**1. ATTENDANCE_MARKED**
```json
{
  "event_type": "ATTENDANCE_MARKED",
  "timestamp": "2026-01-28T09:15:00Z",
  "data": {
    "student_id": 1001,
    "class_id": 45,
    "class_session_id": 101,
    "status": "PRESENT",
    "check_in_time": "2026-01-28T09:03:00Z",
    "attendance_rate": 97.22,
    "marked_by": "teacher_123"
  }
}
```
**Subscribers**: Grade Module (recalculate attendance component)

---

**2. ATTENDANCE_UPDATED**
```json
{
  "event_type": "ATTENDANCE_UPDATED",
  "timestamp": "2026-01-28T15:30:00Z",
  "data": {
    "attendance_id": 5678,
    "student_id": 1001,
    "class_id": 45,
    "old_status": "ABSENT",
    "new_status": "EXCUSED_ABSENCE",
    "new_attendance_rate": 97.22,
    "updated_by": "teacher_123",
    "reason": "Student nộp giấy phép sau"
  }
}
```
**Subscribers**: Grade Module, Notification Service

---

**3. ATTENDANCE_BULK_IMPORTED**
```json
{
  "event_type": "ATTENDANCE_BULK_IMPORTED",
  "timestamp": "2026-01-28T16:00:00Z",
  "data": {
    "class_id": 45,
    "total_records": 35,
    "success_count": 33,
    "error_count": 2,
    "imported_by": "admin_456"
  }
}
```
**Subscribers**: Audit Service

---

**4. LOW_ATTENDANCE_ALERT**
```json
{
  "event_type": "LOW_ATTENDANCE_ALERT",
  "timestamp": "2026-01-28T18:00:00Z",
  "data": {
    "student_id": 1002,
    "class_id": 45,
    "attendance_rate": 65.0,
    "threshold": 70.0,
    "alert_level": "WARNING"
  }
}
```
**Subscribers**: Notification Service (gửi email/SMS cho student và teacher)

---

### Subscribed Events

**1. CLASS_SESSION_COMPLETED** (from Class Module)
```json
{
  "event_type": "CLASS_SESSION_COMPLETED",
  "data": {
    "class_session_id": 101,
    "class_id": 45,
    "completed_at": "2026-01-28T11:00:00Z"
  }
}
```
**Action**:
- Automatically mark ABSENT cho students chưa có attendance record
- Trigger attendance rate recalculation

---

**2. ENROLLMENT_CANCELLED** (from Enrollment Module)
```json
{
  "event_type": "ENROLLMENT_CANCELLED",
  "data": {
    "student_id": 1003,
    "class_id": 45
  }
}
```
**Action**: Soft delete attendance records (hoặc keep for audit)

---

### Notification Rules

**1. Low Attendance Alert**
- Trigger: Attendance rate < 70% (configurable threshold)
- Recipients: Student, Teacher (MAIN_TEACHER), Admin
- Channels: Email, In-app notification
- Frequency: Weekly digest

**2. Excused Absence Request**
- Trigger: Student submits request (UC-ATT-011)
- Recipients: Teacher (MAIN_TEACHER, ASSISTANT)
- Channels: Email, In-app notification
- Priority: Normal

**3. Absence Request Decision**
- Trigger: Teacher approve/reject request (UC-ATT-012)
- Recipients: Student
- Channels: Email, In-app notification, SMS (if rejected)
- Priority: High

---

## Implementation Notes

### Performance Considerations
1. **Attendance Rate Calculation**: Cache attendance_rate trong Enrollment table, update incrementally
2. **Bulk Operations**: Use batch insert/update (max 1000 records per batch)
3. **Report Generation**: Async job for large classes (> 100 students)
4. **QR Code**: Cache QR image for session duration (Redis TTL = session end time)

### Security Considerations
1. **QR Code**: Include HMAC signature để prevent tampering
2. **Attachment Upload**: Validate file type (only PDF, JPG, PNG), max 5MB
3. **Permission Checks**: Always validate teacher assigned to class before mark/update
4. **Audit Trail**: Log all attendance changes for compliance

### Data Retention
- Attendance records: Keep forever (academic records)
- Audit logs: Keep for 7 years (compliance)
- QR codes: Delete after session ends + 1 day
- Excused absence requests: Keep for 1 year after session

---

## Summary

Attendance Module quản lý điểm danh với các highlights:
- ✅ 4 trạng thái: PRESENT, LATE, ABSENT, EXCUSED_ABSENCE
- ✅ QR code check-in cho students
- ✅ Manual điểm danh cho teachers
- ✅ Attendance rate calculation với customizable weights
- ✅ Integration với Grade Module cho attendance component
- ✅ Excused absence request workflow
- ✅ Comprehensive reports và exports
- ✅ Audit trail cho tất cả attendance changes
- ✅ Bulk import cho admin
- ✅ Low attendance alerts

**Business Rules**: 8 rules covering uniqueness, status logic, permissions, rate calculation, QR validation, grade integration, retroactive updates, bulk import

**Use Cases**: 12 use cases covering full attendance lifecycle from check-in to reporting and grading integration
