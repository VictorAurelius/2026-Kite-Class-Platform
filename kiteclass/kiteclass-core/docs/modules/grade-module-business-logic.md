# Grade Module - Business Logic

**Service:** kiteclass-core
**Module:** Grade Management (Part of Learning Module)
**Priority:** P0 (Core academic feature)
**Status:** Design Phase
**Version:** 1.0.0
**Date:** 2026-01-28

---

## 📋 1. Tổng Quan Module

### Mục đích

Module Grade quản lý điểm số, calculate final grades và generate transcripts trong hệ thống KiteClass.

**Vai trò trong hệ thống:**
- Calculate final grades từ multiple components (Attendance, Assignments, Midterm, Final)
- Manage grade components và weights
- Generate transcripts và grade reports
- Track GPA (Grade Point Average)
- Support different grading scales (0-100, letter grades, 4.0 scale)
- Integration với Attendance và Assignment modules
- Generate academic reports

### Phạm vi (Scope)

**Trong phạm vi:**
- ✅ Grade calculation từ multiple components
- ✅ Weighted average calculation
- ✅ Grade component management (Attendance, Assignment, Midterm, Final)
- ✅ Grading scale configuration (0-100, A-F, 4.0 scale)
- ✅ Transcript generation
- ✅ GPA calculation
- ✅ Grade history tracking
- ✅ Pass/Fail determination
- ✅ Grade reports và exports

**Ngoài phạm vi:**
- ❌ Grade curving (Future feature)
- ❌ Extra credit management (Future)
- ❌ Grade dispute workflow (Future)
- ❌ Predictive grade analytics (Future AI feature)

### Business Context

**Real-World Scenario: English Course Grading**

```
Course: English Intermediate B1 (12 weeks)
Student: Nguyen Van A
Class: B1 - Evening Mon-Wed-Fri

Grade Components & Weights:
┌──────────────────────────────────────────────────┐
│ 1. Attendance: 10%                               │
│    - Present: 34/36 sessions (94.4%)            │
│    - Score: 94.4/100                            │
│    - Weighted: 9.44 points                      │
│                                                  │
│ 2. Assignments: 30%                              │
│    - Assignment 1: 85/100 (10% weight)          │
│    - Assignment 2: 90/100 (10% weight)          │
│    - Assignment 3: 88/100 (10% weight)          │
│    - Average: 87.67/100                         │
│    - Weighted: 26.30 points                     │
│                                                  │
│ 3. Midterm Exam: 25%                            │
│    - Score: 82/100                              │
│    - Weighted: 20.50 points                     │
│                                                  │
│ 4. Final Exam: 35%                              │
│    - Score: 88/100                              │
│    - Weighted: 30.80 points                     │
│                                                  │
│ ─────────────────────────────────────────────── │
│ FINAL GRADE: 87.04/100 (B+)                     │
│ Status: PASSED (Pass threshold: 50)             │
│ GPA: 3.3/4.0                                    │
└──────────────────────────────────────────────────┘

Transcript:
- Course: English Intermediate B1
- Final Grade: 87.04 (B+)
- GPA: 3.3
- Credits: 4.0
- Semester: Spring 2026
- Instructor: Teacher A
- Status: PASSED
```

**Grading Scale (Configurable):**

```
Letter Grade System:
A+: 95-100 (4.0 GPA)
A : 90-94  (4.0 GPA)
B+: 85-89  (3.3 GPA)
B : 80-84  (3.0 GPA)
C+: 75-79  (2.3 GPA)
C : 70-74  (2.0 GPA)
D+: 65-69  (1.3 GPA)
D : 60-64  (1.0 GPA)
F : 0-59   (0.0 GPA)

Pass/Fail:
Pass: >= 50
Fail: < 50
```

### Priority

- **Priority:** P0 (Critical)
- **Reason:**
  - Core academic feature
  - Required for transcripts
  - Essential for student assessment
  - Integration với multiple modules

---

## 🏗️ 2. Thực Thể Nghiệp Vụ

### 2.1. Grade Entity

**Table:** `grades`

**Mô tả:** Final grade của student trong class.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| student_id | BIGINT | NO | FK to students.id | Must exist |
| class_id | BIGINT | NO | FK to classes.id | Must exist |
| final_score | DECIMAL(5,2) | YES | Điểm tổng kết (0-100) | 0-100 |
| letter_grade | VARCHAR(5) | YES | A+, A, B+, B, C+, C, D+, D, F | - |
| gpa | DECIMAL(3,2) | YES | GPA (0.0-4.0) | 0-4 |
| status | VARCHAR(20) | NO | IN_PROGRESS, FINALIZED, PASSED, FAILED | Enum |
| pass_threshold | DECIMAL(5,2) | NO | Điểm đậu (default: 50) | 0-100 |
| comments | TEXT | YES | Nhận xét của giáo viên | Max 2000 chars |
| calculated_at | TIMESTAMP | YES | Thời gian tính điểm | - |
| finalized_at | TIMESTAMP | YES | Thời gian finalize | - |
| finalized_by | BIGINT | YES | Teacher ID finalize grade | FK to teachers.id |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian update | Auto-update |

**Indexes:**
```sql
CREATE INDEX idx_grades_student_id ON grades(student_id);
CREATE INDEX idx_grades_class_id ON grades(class_id);
CREATE INDEX idx_grades_status ON grades(status);
CREATE UNIQUE INDEX idx_grades_unique ON grades(student_id, class_id);
```

**Constraints:**
```sql
UNIQUE (student_id, class_id) -- 1 student 1 grade per class
```

**Status Values:**
- `IN_PROGRESS`: Đang học, chưa final
- `FINALIZED`: Đã finalize, không thể thay đổi
- `PASSED`: Đã đậu (final_score >= pass_threshold)
- `FAILED`: Đã trượt (final_score < pass_threshold)

### 2.2. GradeComponent Entity

**Table:** `grade_components`

**Mô tả:** Các thành phần điểm (Attendance, Assignment, Midterm, Final).

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| grade_id | BIGINT | NO | FK to grades.id | Must exist |
| component_type | VARCHAR(20) | NO | ATTENDANCE, ASSIGNMENT, MIDTERM, FINAL, QUIZ, PROJECT | Enum |
| component_name | VARCHAR(100) | NO | Component display name | - |
| component_ref_id | BIGINT | YES | Reference ID (assignment_id, etc.) | - |
| score | DECIMAL(5,2) | YES | Điểm thành phần (0-100) | 0-100 |
| max_score | DECIMAL(5,2) | NO | Điểm tối đa | > 0 |
| weight_percent | DECIMAL(5,2) | NO | % đóng góp vào final grade | 0-100 |
| weighted_score | DECIMAL(5,2) | YES | Score * weight / 100 | - |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian update | Auto-update |

**Indexes:**
```sql
CREATE INDEX idx_grade_components_grade_id ON grade_components(grade_id);
CREATE INDEX idx_grade_components_type ON grade_components(component_type);
CREATE INDEX idx_grade_components_ref_id ON grade_components(component_ref_id);
```

**Component Type Values:**
- `ATTENDANCE`: Điểm chuyên cần
- `ASSIGNMENT`: Bài tập (tổng hợp tất cả assignments hoặc từng assignment riêng)
- `MIDTERM`: Thi giữa kỳ
- `FINAL`: Thi cuối kỳ
- `QUIZ`: Kiểm tra nhỏ
- `PROJECT`: Dự án lớn

**Relationship:**
```
Grade 1 ──── * GradeComponent

Logic:
- 1 Grade có nhiều Components
- Components calculate → Final score
- Weights phải tổng = 100%
```

### 2.3. GradingScale Entity (Configuration)

**Table:** `grading_scales`

**Mô tả:** Cấu hình thang điểm (A-F, GPA mapping).

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| scale_name | VARCHAR(50) | NO | Tên thang điểm | - |
| letter_grade | VARCHAR(5) | NO | A+, A, B+, ... | - |
| min_score | DECIMAL(5,2) | NO | Điểm tối thiểu | 0-100 |
| max_score | DECIMAL(5,2) | NO | Điểm tối đa | 0-100 |
| gpa_value | DECIMAL(3,2) | NO | GPA tương ứng | 0-4 |
| description | VARCHAR(100) | YES | Mô tả | - |
| is_default | BOOLEAN | NO | Thang điểm mặc định | Default false |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |

**Example data:**
```sql
INSERT INTO grading_scales VALUES
(1, 'Standard', 'A+', 95.00, 100.00, 4.00, 'Excellent', true),
(2, 'Standard', 'A',  90.00, 94.99,  4.00, 'Excellent', true),
(3, 'Standard', 'B+', 85.00, 89.99,  3.30, 'Very Good', true),
(4, 'Standard', 'B',  80.00, 84.99,  3.00, 'Good', true),
(5, 'Standard', 'C+', 75.00, 79.99,  2.30, 'Satisfactory', true),
(6, 'Standard', 'C',  70.00, 74.99,  2.00, 'Fair', true),
(7, 'Standard', 'D+', 65.00, 69.99,  1.30, 'Poor', true),
(8, 'Standard', 'D',  60.00, 64.99,  1.00, 'Very Poor', true),
(9, 'Standard', 'F',  0.00,  59.99,  0.00, 'Fail', true);
```

### 2.4. Transcript Entity (View/Report)

**Table:** `transcripts`

**Mô tả:** Bảng điểm tổng hợp của student.

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| student_id | BIGINT | FK to students.id |
| semester | VARCHAR(20) | Spring 2026, Fall 2026, etc. |
| academic_year | VARCHAR(10) | 2025-2026, 2026-2027 |
| total_credits | DECIMAL(5,2) | Tổng số tín chỉ |
| semester_gpa | DECIMAL(3,2) | GPA học kỳ |
| cumulative_gpa | DECIMAL(3,2) | GPA tích lũy |
| total_courses | INT | Số khóa học |
| passed_courses | INT | Số khóa đậu |
| failed_courses | INT | Số khóa trượt |
| generated_at | TIMESTAMP | Thời gian tạo |

**Relationship:**
- 1 Student có nhiều Transcripts (per semester)
- Transcript aggregate data từ Grades

---

## 📐 3. Quy Tắc Kinh Doanh

### BR-GRADE-001: Grade Phải Thuộc 1 Student và 1 Class

**Mô tả:** Mỗi grade record link to valid student và class.

**Validation:**
```java
Student student = studentRepository.findById(studentId)
    .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

Class clazz = classRepository.findById(classId)
    .orElseThrow(() -> new ResourceNotFoundException("Class", classId));

// Check enrollment
boolean enrolled = enrollmentRepository
    .existsByStudentIdAndClassIdAndStatus(
        studentId, classId, EnrollmentStatus.ACTIVE
    );

if (!enrolled) {
    throw new BusinessException("Student không enrolled vào class này");
}
```

---

### BR-GRADE-002: Component Weights Phải Tổng = 100%

**Mô tả:** Tổng weight_percent của tất cả components phải = 100%.

**Validation:**
```java
BigDecimal totalWeight = components.stream()
    .map(GradeComponent::getWeightPercent)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

if (totalWeight.compareTo(BigDecimal.valueOf(100)) != 0) {
    throw new ValidationException(
        "Tổng weights phải = 100%. Hiện tại: " + totalWeight + "%"
    );
}
```

---

### BR-GRADE-003: Final Score Calculation

**Mô tả:** Final score = Tổng weighted scores của components.

**Formula:**
```java
BigDecimal finalScore = components.stream()
    .filter(c -> c.getScore() != null)
    .map(c -> {
        // Normalize score to 0-100 scale
        BigDecimal normalizedScore = c.getScore()
            .divide(c.getMaxScore(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        // Apply weight
        return normalizedScore
            .multiply(c.getWeightPercent())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    })
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

**Example:**
```
Attendance: 94.4/100 * 10% = 9.44
Assignments: 87.67/100 * 30% = 26.30
Midterm: 82/100 * 25% = 20.50
Final: 88/100 * 35% = 30.80
──────────────────────────────────
Final Score: 87.04/100
```

---

### BR-GRADE-004: Letter Grade Mapping

**Mô tả:** Final score → Letter grade theo grading_scales table.

**Logic:**
```java
GradingScale scale = gradingScaleRepository
    .findByScoreRange(finalScore)
    .orElseThrow(() -> new BusinessException("No matching grade scale"));

letterGrade = scale.getLetterGrade();
gpa = scale.getGpaValue();
```

---

### BR-GRADE-005: Pass/Fail Determination

**Mô tả:** Student PASSED nếu final_score >= pass_threshold.

**Logic:**
```java
if (finalScore.compareTo(passThreshold) >= 0) {
    status = GradeStatus.PASSED;
} else {
    status = GradeStatus.FAILED;
}
```

**Default:** pass_threshold = 50.0

---

### BR-GRADE-006: Không Thể Finalize Grade Khi Thiếu Components

**Mô tả:** Chỉ finalize khi tất cả required components đã có score.

**Validation:**
```java
List<GradeComponent> missingScores = components.stream()
    .filter(c -> c.getScore() == null)
    .collect(Collectors.toList());

if (!missingScores.isEmpty()) {
    throw new BusinessException(
        "Không thể finalize. Thiếu điểm: " +
        missingScores.stream()
            .map(GradeComponent::getComponentName)
            .collect(Collectors.joining(", "))
    );
}
```

---

### BR-GRADE-007: FINALIZED Grades Không Thể Thay Đổi

**Mô tả:** Grades đã FINALIZED chỉ có ADMIN mới update được (special cases).

**Validation:**
```java
if (grade.getStatus() == GradeStatus.FINALIZED &&
    !user.hasRole("ADMIN")) {
    throw new BusinessException("Grade đã finalized. Không thể thay đổi.");
}
```

---

### BR-GRADE-008: GPA Calculation

**Mô tả:** Cumulative GPA = Average của tất cả course GPAs weighted by credits.

**Formula:**
```java
BigDecimal cumulativeGPA = grades.stream()
    .map(g -> g.getGpa().multiply(g.getCredits()))
    .reduce(BigDecimal.ZERO, BigDecimal::add)
    .divide(totalCredits, 2, RoundingMode.HALF_UP);
```

**Example:**
```
Course 1: GPA 3.3, Credits 4.0 → 3.3 * 4 = 13.2
Course 2: GPA 3.7, Credits 3.0 → 3.7 * 3 = 11.1
Course 3: GPA 3.0, Credits 4.0 → 3.0 * 4 = 12.0
──────────────────────────────────────────────
Total: 36.3 / 11 credits = 3.30 GPA
```

---

## 🎯 4. Use Cases

### Overview

Module Grade hỗ trợ full grade management workflow:

**Grade Management:**
- UC-GRADE-001: Initialize Grade Record
- UC-GRADE-002: Update Grade Component
- UC-GRADE-003: Calculate Final Score
- UC-GRADE-004: Finalize Grade
- UC-GRADE-005: Update Finalized Grade (Admin only)

**Grade Viewing:**
- UC-GRADE-006: View Student Grade (Detail)
- UC-GRADE-007: View Class Grades (Roster)
- UC-GRADE-008: View Grade Statistics

**Transcript:**
- UC-GRADE-009: Generate Transcript
- UC-GRADE-010: Export Transcript (PDF)
- UC-GRADE-011: View GPA History

**Configuration:**
- UC-GRADE-012: Configure Grading Scale
- UC-GRADE-013: Set Component Weights

---

### UC-GRADE-001: Initialize Grade Record

**Người thực hiện:** System (Auto trigger)

**Mục đích:** Tạo grade record khi student enroll vào class

**Điều kiện trước:**
- Student enrolled vào class
- Enrollment status = ACTIVE

**Luồng chính:**

1. System detect ENROLLMENT_CREATED event
2. System create Grade record:
   - student_id = enrollment.student_id
   - class_id = enrollment.class_id
   - status = IN_PROGRESS
   - pass_threshold = class.pass_threshold (default 50)
3. System get class grade template (component weights)
4. System create GradeComponent records:
   - Attendance (10%)
   - Assignments (30%)
   - Midterm (25%)
   - Final (35%)
   - All scores = NULL initially
5. System save records

**Kết quả:**
- Grade record initialized
- Components ready to receive scores
- Student has grade placeholder

**Events:**
- Event: `GRADE_INITIALIZED` (gradeId, studentId, classId)

---

### UC-GRADE-002: Update Grade Component

**Người thực hiện:** System (Auto trigger from other modules)

**Mục đích:** Update component score khi data changes

**Scenarios:**

**Scenario 1: Attendance Updated**
```
Event: ATTENDANCE_MARKED
1. Calculate attendance rate for student in class
2. Find Attendance component in grade
3. Update score = attendance_rate (e.g., 94.4)
4. Recalculate final score
```

**Scenario 2: Assignment Graded**
```
Event: ASSIGNMENT_GRADED
1. Get assignment weight
2. Find Assignment component in grade
3. If component_ref_id = assignment_id:
   - Update score = adjusted_score
4. Else if aggregated assignments:
   - Calculate average of all graded assignments
   - Update score = average
5. Recalculate final score
```

**Scenario 3: Midterm/Final Entered**
```
Manual input by teacher:
1. Teacher enters midterm score: 82/100
2. Find Midterm component in grade
3. Update score = 82
4. Recalculate final score
```

**Luồng chính:**

1. System nhận event (ATTENDANCE_MARKED, ASSIGNMENT_GRADED, etc.)
2. System find relevant Grade record
3. System find relevant GradeComponent
4. System calculate new score
5. System update component:
   ```java
   component.setScore(newScore);
   component.setMaxScore(maxScore);
   component.setUpdatedAt(NOW());
   ```
6. System trigger UC-GRADE-003 (Calculate Final Score)
7. System save changes

**Kết quả:**
- Component score updated
- Final score recalculated
- Grade record updated

**Events:**
- Event: `GRADE_COMPONENT_UPDATED` (gradeId, componentType, newScore)

---

### UC-GRADE-003: Calculate Final Score

**Người thực hiện:** System (Auto calculation)

**Mục đích:** Tính final score từ components

**Điều kiện trước:**
- Grade record tồn tại
- Components có scores (có thể partial)

**Luồng chính:**

1. System get all components của grade
2. System validate weights:
   - **BR-GRADE-002:** Total weights = 100%
3. System calculate weighted scores:
   ```java
   for (GradeComponent component : components) {
       if (component.getScore() != null) {
           // Normalize to 0-100
           BigDecimal normalized = component.getScore()
               .divide(component.getMaxScore(), 4, RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));

           // Apply weight
           BigDecimal weighted = normalized
               .multiply(component.getWeightPercent())
               .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

           component.setWeightedScore(weighted);
       }
   }
   ```
4. System sum weighted scores:
   ```java
   BigDecimal finalScore = components.stream()
       .filter(c -> c.getWeightedScore() != null)
       .map(GradeComponent::getWeightedScore)
       .reduce(BigDecimal.ZERO, BigDecimal::add);
   ```
5. System update Grade:
   - final_score = calculated
   - letter_grade = lookup from grading_scales
   - gpa = lookup from grading_scales
   - calculated_at = NOW()
6. System determine pass/fail:
   - **BR-GRADE-005:** final_score >= pass_threshold → PASSED
7. System save grade

**Example Calculation:**
```
Components:
- Attendance: 94.4/100 * 10% = 9.44
- Assignments: 87.67/100 * 30% = 26.30
- Midterm: 82/100 * 25% = 20.50
- Final: NULL (not taken yet)

Current Final Score: 9.44 + 26.30 + 20.50 = 56.24/65%
(Only 65% of grade entered, missing 35% from Final)

After Final entered (88/100):
- Final: 88/100 * 35% = 30.80

Final Score: 9.44 + 26.30 + 20.50 + 30.80 = 87.04/100
Letter Grade: B+ (85-89.99)
GPA: 3.3
Status: PASSED (>= 50)
```

**Kết quả:**
- Final score calculated
- Letter grade assigned
- GPA assigned
- Pass/Fail determined

**Events:**
- Event: `GRADE_CALCULATED` (gradeId, finalScore, letterGrade, gpa)

---

### UC-GRADE-004: Finalize Grade

**Người thực hiện:** MAIN_TEACHER, CREATOR, ADMIN

**Mục đích:** Finalize grade khi class completed

**Điều kiện trước:**
- Class status = COMPLETED
- All required components có scores
- User có quyền finalize

**Luồng chính:**

1. Teacher truy cập Class Detail → Grades tab
2. Teacher click "Finalize Grades"
3. Frontend hiển thị confirmation:
   - "Finalize tất cả grades cho class này?"
   - "Sau khi finalize, không thể thay đổi (chỉ ADMIN)"
   - Summary: Total students, Average score, Pass/Fail count
4. Teacher confirm
5. Frontend gửi POST `/api/v1/classes/{classId}/grades/finalize`
6. Hệ thống validate for each grade:
   - **BR-GRADE-006:** All required components có scores
   - Final score calculated
7. Hệ thống update tất cả grades:
   - status = IN_PROGRESS → FINALIZED
   - finalized_at = NOW()
   - finalized_by = teacherId
8. Hệ thống generate transcripts
9. Hệ thống trả về HTTP 200 OK với summary
10. Frontend hiển thị: "Đã finalize {count} grades"
11. Students nhận notifications về final grades

**Luồng thay thế:**

**AF1 - Missing component scores:**
- Tại bước 6, có students thiếu scores
- Trả về HTTP 400 Bad Request
- Message: "Không thể finalize. Thiếu điểm cho {count} students"
- List students và missing components

**AF2 - Class not completed:**
- Tại bước 6, class status != COMPLETED
- Trả về HTTP 400 Bad Request
- Message: "Class chưa completed. Không thể finalize grades."

**Kết quả:**
- All grades FINALIZED
- Transcripts generated
- Students notified
- Grades locked (read-only)

**Events:**
- Event: `GRADES_FINALIZED` (classId, studentCount, avgScore)

---

### UC-GRADE-006: View Student Grade (Detail)

**Người thực hiện:** STUDENT (own grade), TEACHER (class students), ADMIN

**Mục đích:** Xem chi tiết điểm

**Điều kiện trước:**
- User có quyền view grade

**Luồng chính:**

1. User truy cập Student Profile → Grades tab
2. Frontend gửi GET `/api/v1/students/{studentId}/classes/{classId}/grade`
3. Hệ thống validate permission:
   - ADMIN: All grades
   - TEACHER: Students in their classes
   - STUDENT: Own grade only
4. Hệ thống query grade + components
5. Hệ thống calculate percentages và progress
6. Hệ thống trả về detailed grade data
7. Frontend hiển thị:
   - Final score với letter grade
   - GPA
   - Status (PASSED/FAILED/IN_PROGRESS)
   - Component breakdown table
   - Progress chart
   - Teacher comments

**Response Example:**
```json
{
  "success": true,
  "data": {
    "gradeId": 123,
    "studentId": 10,
    "studentName": "Nguyen Van A",
    "classId": 5,
    "className": "English Intermediate B1",
    "finalScore": 87.04,
    "letterGrade": "B+",
    "gpa": 3.3,
    "status": "PASSED",
    "passThreshold": 50.0,
    "comments": "Excellent progress. Keep up the good work!",
    "components": [
      {
        "type": "ATTENDANCE",
        "name": "Attendance",
        "score": 94.4,
        "maxScore": 100,
        "weightPercent": 10.0,
        "weightedScore": 9.44,
        "progress": "94.4%"
      },
      {
        "type": "ASSIGNMENT",
        "name": "Assignments (Average)",
        "score": 87.67,
        "maxScore": 100,
        "weightPercent": 30.0,
        "weightedScore": 26.30,
        "details": [
          {
            "name": "Assignment 1",
            "score": 85,
            "weight": 10
          },
          {
            "name": "Assignment 2",
            "score": 90,
            "weight": 10
          },
          {
            "name": "Assignment 3",
            "score": 88,
            "weight": 10
          }
        ]
      },
      {
        "type": "MIDTERM",
        "name": "Midterm Exam",
        "score": 82.0,
        "maxScore": 100,
        "weightPercent": 25.0,
        "weightedScore": 20.50
      },
      {
        "type": "FINAL",
        "name": "Final Exam",
        "score": 88.0,
        "maxScore": 100,
        "weightPercent": 35.0,
        "weightedScore": 30.80
      }
    ],
    "calculatedAt": "2026-05-10T15:30:00Z",
    "finalizedAt": "2026-05-12T10:00:00Z"
  }
}
```

**Kết quả:**
- Student/Teacher xem detailed breakdown
- Understand contribution của từng component
- Track progress

---

### UC-GRADE-007: View Class Grades (Roster)

**Người thực hiện:** MAIN_TEACHER, CREATOR, ADMIN

**Mục đích:** Teacher xem tất cả grades trong class

**Luồng chính:**

1. Teacher truy cập Class Detail → Grades tab
2. Frontend gửi GET `/api/v1/classes/{classId}/grades`
3. Hệ thống query all grades cho class
4. Hệ thống calculate class statistics:
   - Average final score
   - Average GPA
   - Pass rate
   - Grade distribution
5. Hệ thống trả về list + stats
6. Frontend hiển thị table:
   - Student name
   - Final score
   - Letter grade
   - GPA
   - Status (PASSED/FAILED)
   - Actions (View detail, Edit components)
7. Frontend hiển thị statistics:
   - Class average: 82.5/100
   - Average GPA: 3.1/4.0
   - Pass rate: 95% (19/20)
   - Grade distribution chart (bar chart)

**Kết quả:**
- Teacher có overview class performance
- Easy to identify struggling students
- Class-level analytics

---

### UC-GRADE-009: Generate Transcript

**Người thực hiện:** STUDENT (request), ADMIN (generate)

**Mục đích:** Generate official transcript

**Luồng chính:**

1. Student/Admin request transcript
2. Frontend gửi POST `/api/v1/students/{studentId}/transcript/generate`
3. Hệ thống query all FINALIZED grades cho student
4. Hệ thống group by semester/academic year
5. Hệ thống calculate:
   - Semester GPA
   - Cumulative GPA
   - Total credits
   - Passed/Failed courses
6. Hệ thống generate Transcript record
7. Hệ thống trả về transcript data
8. Frontend hiển thị transcript (table format)

**Transcript Example:**
```
========================================
OFFICIAL ACADEMIC TRANSCRIPT
========================================

Student: Nguyen Van A
Student ID: 2026001
Date of Birth: 01/01/2000
Program: English Language

SPRING 2026
─────────────────────────────────────────────────────────────
Course Code | Course Name              | Credits | Grade | GPA
─────────────────────────────────────────────────────────────
ENG-B1      | English Intermediate B1  | 4.0     | B+    | 3.3
ENG-CONV    | English Conversation     | 3.0     | A     | 4.0
ENG-GRAM    | Advanced Grammar         | 3.0     | B     | 3.0
─────────────────────────────────────────────────────────────
Semester Credits: 10.0
Semester GPA: 3.40
─────────────────────────────────────────────────────────────

CUMULATIVE SUMMARY
─────────────────────────────────────────────────────────────
Total Credits Earned: 10.0
Cumulative GPA: 3.40
Courses Passed: 3
Courses Failed: 0
─────────────────────────────────────────────────────────────

Generated: 2026-05-15
Official Seal: [Digital Signature]
```

**Kết quả:**
- Official transcript generated
- Ready for export/print
- Digitally signed

---

### UC-GRADE-010: Export Transcript (PDF)

**Người thực hiện:** STUDENT, ADMIN

**Mục đích:** Export transcript as PDF

**Luồng chính:**

1. User click "Export PDF" on transcript page
2. Frontend gửi GET `/api/v1/students/{studentId}/transcript/pdf`
3. Hệ thống generate PDF:
   - Use template (company logo, format)
   - Include all transcript data
   - Add digital signature/QR code
4. Hệ thống trả về PDF file
5. Frontend trigger download
6. User save PDF

**Kết quả:**
- PDF transcript downloaded
- Official format
- Ready for submission

---

## 🔐 5. Permission Model

### Grade Permissions

**Roles:**
- **OWNER/ADMIN:** Full access all grades
- **CREATOR/MAIN_TEACHER:** Manage grades in their classes
- **INSTRUCTOR:** View grades, limited edit
- **ASSISTANT:** View only
- **STUDENT:** View own grades only

**Permission Matrix:**

| Operation | OWNER/ADMIN | CREATOR/TEACHER | INSTRUCTOR | ASSISTANT | STUDENT |
|-----------|-------------|-----------------|------------|-----------|---------|
| View All Grades | ✅ | Assigned classes | Assigned classes | Assigned classes | Own only |
| View Grade Detail | ✅ | ✅ | ✅ | ✅ | Own only |
| Update Component | ✅ | ✅ | ✅ (some) | ❌ | ❌ |
| Calculate Final | ✅ (auto) | ✅ (auto) | ✅ (auto) | - | - |
| Finalize Grade | ✅ | ✅ | ❌ | ❌ | ❌ |
| Update Finalized | ✅ | ❌ | ❌ | ❌ | ❌ |
| Generate Transcript | ✅ | ✅ | ✅ | ✅ | Own only |
| Export Transcript | ✅ | ✅ | ✅ | ✅ | Own only |
| Configure Grading Scale | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## 🔗 6. Integration với Other Modules

### 6.1. Attendance Module Integration

**Event-driven integration:**
```
Event: ATTENDANCE_MARKED
→ Grade Module listener updates Attendance component score
→ Recalculate final score
```

### 6.2. Assignment Module Integration

**Event-driven integration:**
```
Event: ASSIGNMENT_GRADED
→ Grade Module listener updates Assignment component score
→ Recalculate final score
```

### 6.3. Class Module Integration

**Integration points:**
- Grade.class_id FK to classes.id
- Initialize grades when class starts
- Finalize grades when class COMPLETED

### 6.4. Student Module Integration

**Integration points:**
- Grade.student_id FK to students.id
- Transcript generation uses student data

---

## 📊 7. Summary

### Entities
- ✅ **Grade:** Final grade record
- ✅ **GradeComponent:** Component scores (Attendance, Assignment, etc.)
- ✅ **GradingScale:** Letter grade configuration
- ✅ **Transcript:** Academic transcript

### Business Rules
- ✅ BR-GRADE-001: Grade thuộc student + class
- ✅ BR-GRADE-002: Component weights = 100%
- ✅ BR-GRADE-003: Final score calculation
- ✅ BR-GRADE-004: Letter grade mapping
- ✅ BR-GRADE-005: Pass/Fail determination
- ✅ BR-GRADE-006: Không finalize khi thiếu components
- ✅ BR-GRADE-007: FINALIZED grades read-only
- ✅ BR-GRADE-008: GPA calculation

### Use Cases

**Grade Management:**
- ✅ UC-GRADE-001: Initialize Grade Record
- ✅ UC-GRADE-002: Update Grade Component
- ✅ UC-GRADE-003: Calculate Final Score
- ✅ UC-GRADE-004: Finalize Grade
- ✅ UC-GRADE-005: Update Finalized Grade (Admin)

**Grade Viewing:**
- ✅ UC-GRADE-006: View Student Grade (Detail)
- ✅ UC-GRADE-007: View Class Grades (Roster)
- ✅ UC-GRADE-008: View Grade Statistics

**Transcript:**
- ✅ UC-GRADE-009: Generate Transcript
- ✅ UC-GRADE-010: Export Transcript (PDF)
- ✅ UC-GRADE-011: View GPA History

**Configuration:**
- ✅ UC-GRADE-012: Configure Grading Scale
- ✅ UC-GRADE-013: Set Component Weights

**Total:** 13 use cases

### Calculation Logic
```
1. Component Scores → Weighted Scores
   score/max_score * 100 * weight% / 100

2. Weighted Scores → Final Score
   Sum of all weighted scores

3. Final Score → Letter Grade
   Lookup in grading_scales table

4. Letter Grade → GPA
   From grading_scales mapping

5. Course GPAs → Cumulative GPA
   Weighted average by credits
```

### Integration
- ✅ Attendance Module: Auto-update attendance component
- ✅ Assignment Module: Auto-update assignment component
- ✅ Class Module: Grade lifecycle tied to class
- ✅ Student Module: Transcript generation

---

## 🚀 Next Steps

**Sau khi document này được approve:**

1. **Create PR: Grade Module**
   - Implement Grade entity
   - Implement GradeComponent entity
   - Implement GradingScale configuration
   - Implement grade calculation logic
   - Implement transcript generation
   - Implement REST APIs
   - Write tests (unit + integration)

2. **Integration Testing**
   - Test with Attendance Module
   - Test with Assignment Module
   - Test grade calculation accuracy

3. **Transcript Template Design**
   - PDF template
   - Digital signature

---

**Author:** VictorAurelius + Claude Sonnet 4.5
**Created:** 2026-01-28
**Status:** Ready for Review
**Next:** Attendance Module business logic (if needed detailed version)
