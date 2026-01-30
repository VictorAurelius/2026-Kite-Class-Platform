# Assignment Module - Business Logic

**Service:** kiteclass-core
**Module:** Assignment Management (Part of Learning Module)
**Priority:** P0 (Core teaching feature)
**Status:** Design Phase
**Version:** 1.0.0
**Date:** 2026-01-28

---

## 📋 1. Tổng Quan Module

### Mục đích

Module Assignment quản lý bài tập, submissions và grading trong hệ thống KiteClass.

**Vai trò trong hệ thống:**
- Teachers tạo assignments cho classes
- Students submit assignments (files, text, links)
- Teachers grade submissions và provide feedback
- Track submission status, deadlines, late submissions
- Calculate assignment grades contribution to final grade
- Integration với Grade Module cho grade calculation
- Support different assignment types (homework, quiz, project, essay)

### Phạm vi (Scope)

**Trong phạm vi:**
- ✅ CRUD operations cho Assignment entity
- ✅ Assignment submission workflow
- ✅ Grading và feedback system
- ✅ Deadline management và late submission handling
- ✅ File attachments (upload/download)
- ✅ Assignment types và categories
- ✅ Auto-calculate grades from submissions
- ✅ Submission history tracking
- ✅ Notification integration (assignment created, deadline approaching, graded)

**Ngoài phạm vi:**
- ❌ Auto-grading cho multiple choice (Future AI feature)
- ❌ Plagiarism detection (Future integration)
- ❌ Peer review system (Future feature)
- ❌ Group assignments (V2 feature)

### Business Context

**Real-World Scenario: English Class**

```
Teacher: Cô Mai
Class: English Intermediate B1 (20 students)

Week 1: Assignment 1
┌─────────────────────────────────────────────────┐
│ Assignment: Unit 1 - Grammar Exercise           │
│ Type: HOMEWORK                                  │
│ Due: 2026-02-05 23:59                          │
│ Max Score: 100 points                          │
│ Weight: 10% of final grade                    │
│ Instructions: Complete exercises page 45-48    │
│ Attachments: exercise-template.pdf             │
└─────────────────────────────────────────────────┘

Student submissions:
├── Student A: Submitted 2026-02-04 20:30 (On time) → Graded: 85/100
├── Student B: Submitted 2026-02-06 10:00 (Late 10h) → Graded: 75/100 (-10% penalty)
├── Student C: Submitted 2026-02-07 23:00 (Late 2 days) → Graded: 60/100 (-20% penalty)
├── Student D: Not submitted → Score: 0/100
└── ... 16 other students

Teacher workflow:
1. Create assignment → Students notified
2. Students submit work before deadline
3. Teacher grades submissions → Students notified
4. Grades auto-calculated into final grade
5. Teacher provides feedback for improvement
```

**Different Assignment Types:**

```
HOMEWORK: Regular exercises, practice problems
├── Due: Short deadline (1-3 days)
├── Weight: 5-10% each
└── Grading: Quick, standardized rubric

PROJECT: Long-term assignments
├── Due: Long deadline (2-4 weeks)
├── Weight: 20-30% of final grade
└── Grading: Detailed rubric, multiple criteria

QUIZ: Short assessments
├── Due: In-class or 24h window
├── Weight: 10-15% each
└── Grading: Auto-grading (future) or manual

ESSAY: Writing assignments
├── Due: 1-2 weeks
├── Weight: 15-20%
└── Grading: Detailed feedback on content, grammar, structure
```

### Priority

- **Priority:** P0 (Critical)
- **Reason:**
  - Core teaching feature
  - Essential for learning assessment
  - Required by all courses
  - Integration với Grade Module

---

## 🏗️ 2. Thực Thể Nghiệp Vụ

### 2.1. Assignment Entity

**Table:** `assignments`

**Mô tả:** Bài tập được tạo bởi teachers cho classes.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| class_id | BIGINT | NO | FK to classes.id | Must exist |
| title | VARCHAR(200) | NO | Tiêu đề bài tập | 5-200 chars |
| description | TEXT | YES | Mô tả chi tiết | Max 5000 chars |
| type | VARCHAR(20) | NO | HOMEWORK, PROJECT, QUIZ, ESSAY, PRESENTATION | Enum |
| instructions | TEXT | YES | Hướng dẫn làm bài | Max 10000 chars |
| max_score | INT | NO | Điểm tối đa | >= 1 |
| weight_percent | DECIMAL(5,2) | YES | % contribution to final grade | 0-100 |
| due_date | TIMESTAMP | YES | Deadline nộp bài | Future date |
| allow_late_submission | BOOLEAN | NO | Cho phép nộp muộn | Default true |
| late_penalty_percent | DECIMAL(5,2) | YES | % penalty per day late | 0-100 |
| max_late_days | INT | YES | Số ngày tối đa được nộp muộn | >= 0 |
| attachment_urls | TEXT | YES | URLs file đính kèm (JSON array) | Valid URLs |
| status | VARCHAR(20) | NO | DRAFT, PUBLISHED, CLOSED, GRADED | Enum |
| created_by | BIGINT | YES | Teacher ID người tạo | FK to teachers.id |
| created_at | TIMESTAMP | NO | Thời gian tạo | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian cập nhật | Auto-update |
| published_at | TIMESTAMP | YES | Thời gian publish | Set when published |
| closed_at | TIMESTAMP | YES | Thời gian close | Set when closed |

**Indexes:**
```sql
CREATE INDEX idx_assignments_class_id ON assignments(class_id);
CREATE INDEX idx_assignments_status ON assignments(status);
CREATE INDEX idx_assignments_due_date ON assignments(due_date);
CREATE INDEX idx_assignments_created_by ON assignments(created_by);
CREATE INDEX idx_assignments_type ON assignments(type);
```

**Status Values:**
- `DRAFT`: Đang soạn, chưa public cho students
- `PUBLISHED`: Đã publish, students có thể submit
- `CLOSED`: Đã đóng, không nhận submissions nữa
- `GRADED`: Đã chấm điểm xong tất cả submissions

**Type Values:**
- `HOMEWORK`: Bài tập về nhà
- `PROJECT`: Dự án lớn
- `QUIZ`: Bài kiểm tra ngắn
- `ESSAY`: Bài viết luận
- `PRESENTATION`: Bài thuyết trình

### 2.2. AssignmentSubmission Entity

**Table:** `assignment_submissions`

**Mô tả:** Bài làm của students.

| Field | Type | Nullable | Description | Validation |
|-------|------|----------|-------------|------------|
| id | BIGINT | NO | Primary key, auto-increment | - |
| assignment_id | BIGINT | NO | FK to assignments.id | Must exist |
| student_id | BIGINT | NO | FK to students.id | Must exist |
| submission_text | TEXT | YES | Nội dung text bài làm | Max 50000 chars |
| attachment_urls | TEXT | YES | URLs files nộp (JSON array) | Valid URLs |
| link_url | VARCHAR(500) | YES | Link external (Google Docs, etc.) | Valid URL |
| submitted_at | TIMESTAMP | YES | Thời gian nộp | - |
| is_late | BOOLEAN | NO | Nộp muộn hay không | Auto-calculated |
| late_days | INT | YES | Số ngày nộp muộn | >= 0 |
| status | VARCHAR(20) | NO | NOT_SUBMITTED, SUBMITTED, GRADED, RETURNED | Enum |
| score | DECIMAL(5,2) | YES | Điểm số (0-max_score) | 0 <= score <= max_score |
| adjusted_score | DECIMAL(5,2) | YES | Điểm sau khi trừ late penalty | - |
| feedback | TEXT | YES | Feedback từ teacher | Max 5000 chars |
| graded_at | TIMESTAMP | YES | Thời gian chấm điểm | - |
| graded_by | BIGINT | YES | Teacher ID người chấm | FK to teachers.id |
| returned_at | TIMESTAMP | YES | Thời gian trả bài | - |
| created_at | TIMESTAMP | NO | Thời gian tạo record | Auto-set |
| updated_at | TIMESTAMP | NO | Thời gian update | Auto-update |

**Indexes:**
```sql
CREATE INDEX idx_submissions_assignment_id ON assignment_submissions(assignment_id);
CREATE INDEX idx_submissions_student_id ON assignment_submissions(student_id);
CREATE INDEX idx_submissions_status ON assignment_submissions(status);
CREATE INDEX idx_submissions_graded_by ON assignment_submissions(graded_by);
CREATE UNIQUE INDEX idx_submissions_unique ON assignment_submissions(assignment_id, student_id);
```

**Constraints:**
```sql
UNIQUE (assignment_id, student_id) -- Mỗi student chỉ nộp 1 lần per assignment
```

**Status Values:**
- `NOT_SUBMITTED`: Chưa nộp (placeholder record)
- `SUBMITTED`: Đã nộp, chờ chấm
- `GRADED`: Đã chấm xong
- `RETURNED`: Đã trả bài cho student

**Relationship:**
```
Assignment 1 ──── * AssignmentSubmission * ──── 1 Student

Logic:
- 1 Assignment có nhiều Submissions (1 per student)
- Each student có 1 submission per assignment
- Submissions created when assignment published hoặc khi student submit
```

### 2.3. AssignmentGradingCriteria Entity (Optional - Future)

**Table:** `assignment_grading_criteria`

**Mô tả:** Rubric chi tiết cho grading (optional, for complex assignments).

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT | Primary key |
| assignment_id | BIGINT | FK to assignments.id |
| criteria_name | VARCHAR(100) | Content, Grammar, Structure, etc. |
| max_points | INT | Max points cho criteria này |
| description | TEXT | Mô tả yêu cầu |
| weight_percent | DECIMAL(5,2) | % of total score |

**Use case:** PROJECT assignments với multiple grading criteria

---

## 📐 3. Quy Tắc Kinh Doanh

### BR-ASSIGNMENT-001: Assignment Phải Thuộc 1 Class

**Mô tả:** Mỗi assignment phải có class_id valid.

**Lý do:** Assignments không thể tồn tại độc lập.

**Validation:**
```java
Class clazz = classRepository.findById(classId)
    .orElseThrow(() -> new ResourceNotFoundException("Class", classId));

if (clazz.getStatus() == ClassStatus.COMPLETED ||
    clazz.getStatus() == ClassStatus.CANCELLED) {
    throw new BusinessException("Không thể tạo assignment cho class này");
}
```

---

### BR-ASSIGNMENT-002: Max Score Phải > 0

**Mô tả:** Điểm tối đa phải lớn hơn 0.

**Validation:**
```java
if (maxScore <= 0) {
    throw new ValidationException("Max score phải > 0");
}
```

---

### BR-ASSIGNMENT-003: Due Date Phải Trong Tương Lai (Khi Publish)

**Mô tả:** Khi publish assignment, due_date phải >= NOW nếu có.

**Lý do:** Không thể publish assignment với deadline đã qua.

**Validation:**
```java
if (dueDate != null && dueDate.isBefore(Instant.now())) {
    throw new BusinessException("Due date phải trong tương lai");
}
```

**Note:** DRAFT assignments có thể có due_date trong quá khứ (for editing).

---

### BR-ASSIGNMENT-004: Chỉ PUBLISHED Assignments Mới Nhận Submissions

**Mô tả:** Students chỉ có thể submit khi assignment status = PUBLISHED.

**Validation:**
```java
if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
    throw new BusinessException("Assignment chưa được publish hoặc đã đóng");
}
```

---

### BR-ASSIGNMENT-005: Late Submission Rules

**Mô tả:** Nếu allow_late_submission = false, không nhận submissions sau due_date.

**Logic:**
```java
boolean isLate = submittedAt.isAfter(assignment.getDueDate());

if (isLate && !assignment.isAllowLateSubmission()) {
    throw new BusinessException("Assignment không cho phép nộp muộn");
}

if (isLate && assignment.getMaxLateDays() != null) {
    long lateDays = ChronoUnit.DAYS.between(
        assignment.getDueDate(), submittedAt
    );

    if (lateDays > assignment.getMaxLateDays()) {
        throw new BusinessException(
            "Quá " + assignment.getMaxLateDays() + " ngày deadline"
        );
    }
}
```

---

### BR-ASSIGNMENT-006: Late Penalty Calculation

**Mô tả:** Nếu nộp muộn, tự động trừ điểm theo late_penalty_percent.

**Formula:**
```java
if (isLate && latePenaltyPercent != null) {
    BigDecimal penalty = score.multiply(
        latePenaltyPercent.divide(BigDecimal.valueOf(100))
    ).multiply(BigDecimal.valueOf(lateDays));

    adjustedScore = score.subtract(penalty).max(BigDecimal.ZERO);
} else {
    adjustedScore = score;
}
```

**Example:**
- Score: 80/100
- Late: 2 days
- Penalty: 10% per day
- Adjusted Score: 80 - (80 * 0.1 * 2) = 80 - 16 = 64

---

### BR-ASSIGNMENT-007: Score Phải <= Max Score

**Mô tả:** Điểm chấm không thể vượt quá max_score.

**Validation:**
```java
if (score.compareTo(assignment.getMaxScore()) > 0) {
    throw new ValidationException(
        "Score không thể vượt quá max score: " + assignment.getMaxScore()
    );
}
```

---

### BR-ASSIGNMENT-008: Một Student Chỉ Nộp 1 Lần Per Assignment

**Mô tả:** Mỗi student chỉ có 1 submission record per assignment (UNIQUE constraint).

**Lý do:** Prevent duplicates, maintain data integrity.

**Note:** Students có thể UPDATE submission trước deadline, nhưng không thể create multiple submissions.

---

### BR-ASSIGNMENT-009: Không Thể Edit GRADED Assignments

**Mô tả:** Assignments đã GRADED (tất cả submissions đã chấm) không thể edit title, max_score, due_date.

**Lý do:** Breaking change - affect graded submissions.

**Allowed edits:**
- Description, Instructions (minor clarifications)
- Status changes (GRADED → CLOSED)

---

## 🎯 4. Use Cases

### Overview

Module Assignment hỗ trợ full assignment lifecycle:

**Assignment Management:**
- UC-ASSIGNMENT-001: Create Assignment (Draft)
- UC-ASSIGNMENT-002: Update Assignment
- UC-ASSIGNMENT-003: Publish Assignment
- UC-ASSIGNMENT-004: Close Assignment
- UC-ASSIGNMENT-005: Delete Assignment (Draft only)

**Student Submission:**
- UC-ASSIGNMENT-006: Submit Assignment
- UC-ASSIGNMENT-007: Update Submission (Before deadline)
- UC-ASSIGNMENT-008: View Submission Status
- UC-ASSIGNMENT-009: View Graded Assignment

**Teacher Grading:**
- UC-ASSIGNMENT-010: Grade Submission
- UC-ASSIGNMENT-011: Bulk Grade Submissions
- UC-ASSIGNMENT-012: Return Graded Work
- UC-ASSIGNMENT-013: View Submission List
- UC-ASSIGNMENT-014: View Assignment Statistics

---

### UC-ASSIGNMENT-001: Create Assignment (Draft)

**Người thực hiện:** MAIN_TEACHER, CREATOR (of course), ADMIN/OWNER

**Mục đích:** Tạo assignment mới ở status DRAFT

**Điều kiện trước:**
- User có quyền create assignments trong class
- Class tồn tại và status UPCOMING hoặc ONGOING

**Luồng chính:**

1. Teacher truy cập Class Detail → Assignments tab
2. Teacher click "Tạo bài tập mới"
3. Frontend hiển thị form:
   - Title (required)
   - Type (HOMEWORK, PROJECT, QUIZ, ESSAY, PRESENTATION)
   - Description
   - Instructions
   - Max score (default: 100)
   - Weight % (contribution to final grade)
   - Due date & time
   - Allow late submission (checkbox)
   - Late penalty % per day
   - Max late days
   - Upload attachments (templates, references)
4. Teacher điền thông tin và submit
5. Frontend gửi POST `/api/v1/classes/{classId}/assignments`
   ```json
   {
     "title": "Unit 1 - Grammar Exercise",
     "type": "HOMEWORK",
     "description": "Complete grammar exercises",
     "instructions": "Answer all questions on page 45-48. Submit as PDF.",
     "maxScore": 100,
     "weightPercent": 10.0,
     "dueDate": "2026-02-05T23:59:59Z",
     "allowLateSubmission": true,
     "latePenaltyPercent": 10.0,
     "maxLateDays": 3,
     "attachmentUrls": ["https://storage.../exercise-template.pdf"]
   }
   ```
6. Hệ thống validate:
   - **BR-ASSIGNMENT-001:** Class tồn tại và valid
   - **BR-ASSIGNMENT-002:** Max score > 0
   - Title không rỗng
7. Hệ thống tạo Assignment:
   - status = DRAFT
   - created_by = teacherId
8. Hệ thống lưu database
9. Hệ thống trả về HTTP 201 Created
10. Frontend redirect đến Assignment Detail (edit mode)
11. Teacher thấy: "Bài tập đã được tạo (DRAFT). Publish để students có thể làm."

**Luồng thay thế:**

**AF1 - Class không valid:**
- Tại bước 6, class COMPLETED hoặc CANCELLED
- Trả về HTTP 400 Bad Request
- Message: "Không thể tạo assignment cho class này"

**Kết quả:**
- Assignment created với status DRAFT
- Chưa visible cho students
- Teacher có thể tiếp tục edit

**Events:**
- Event: `ASSIGNMENT_CREATED` (assignmentId, classId, title, status=DRAFT)

---

### UC-ASSIGNMENT-003: Publish Assignment

**Người thực hiện:** MAIN_TEACHER, CREATOR, ADMIN/OWNER

**Mục đích:** Publish assignment để students có thể submit

**Điều kiện trước:**
- Assignment status = DRAFT
- User có quyền publish

**Luồng chính:**

1. Teacher truy cập Assignment Detail (DRAFT)
2. Teacher click "Publish bài tập"
3. Frontend hiển thị confirmation:
   - "Publish bài tập này?"
   - "Students sẽ được thông báo và có thể bắt đầu làm bài"
   - Preview: Due date, max score, instructions
4. Teacher confirm
5. Frontend gửi POST `/api/v1/assignments/{assignmentId}/publish`
6. Hệ thống validate:
   - Status = DRAFT
   - **BR-ASSIGNMENT-003:** Due date valid (nếu có)
   - User có quyền publish
7. Hệ thống update Assignment:
   - status = DRAFT → PUBLISHED
   - published_at = NOW()
8. Hệ thống create placeholder submissions:
   - Query enrolled students trong class
   - Create AssignmentSubmission records với status = NOT_SUBMITTED
   - Cho mỗi student
9. Hệ thống send notifications:
   - Students nhận notification về assignment mới
   - Email/Push: "Bài tập mới: {title}, Due: {dueDate}"
10. Hệ thống trả về HTTP 200 OK
11. Frontend hiển thị: "Bài tập đã được publish"
12. Students thấy assignment trong class assignments list

**Luồng thay thế:**

**AF1 - Already PUBLISHED:**
- Tại bước 6, assignment đã PUBLISHED
- Trả về HTTP 409 Conflict
- Message: "Assignment đã được publish"

**AF2 - Due date trong quá khứ:**
- Tại bước 6, due_date < NOW
- Trả về HTTP 400 Bad Request
- Message: "Due date phải trong tương lai"

**Kết quả:**
- Assignment status = PUBLISHED
- Students có thể submit
- Placeholder submissions created
- Notifications sent

**Events:**
- Event: `ASSIGNMENT_PUBLISHED` (assignmentId, classId, studentCount, dueDate)

---

### UC-ASSIGNMENT-006: Submit Assignment

**Người thực hiện:** STUDENT (enrolled in class)

**Mục đích:** Student nộp bài tập

**Điều kiện trước:**
- Assignment status = PUBLISHED
- Student enrolled trong class
- Student chưa nộp hoặc đang update submission

**Luồng chính:**

1. Student truy cập Class → Assignments tab
2. Student thấy assignment với "Nộp bài" button
3. Student click "Nộp bài"
4. Frontend hiển thị submission form:
   - Text editor (cho essay/written work)
   - File upload (PDF, DOCX, images)
   - Link field (Google Docs, external links)
   - Preview của files uploaded
   - Deadline countdown
5. Student điền/upload và submit
6. Frontend gửi POST `/api/v1/assignments/{assignmentId}/submit`
   ```json
   {
     "submissionText": "My answer is...",
     "attachmentUrls": [
       "https://storage.../student-work.pdf"
     ],
     "linkUrl": "https://docs.google.com/document/d/..."
   }
   ```
7. Hệ thống validate:
   - **BR-ASSIGNMENT-004:** Assignment PUBLISHED
   - Student enrolled trong class
   - **BR-ASSIGNMENT-005:** Check late submission rules
   - **BR-ASSIGNMENT-008:** Chưa có submission hoặc updating existing
8. Hệ thống calculate late status:
   - submitted_at = NOW()
   - is_late = (NOW > due_date)
   - late_days = calculate days past deadline
9. Hệ thống update AssignmentSubmission:
   - submission_text = text
   - attachment_urls = urls
   - link_url = url
   - submitted_at = NOW()
   - status = NOT_SUBMITTED → SUBMITTED
   - is_late = calculated
   - late_days = calculated
10. Hệ thống trả về HTTP 200 OK
11. Frontend hiển thị: "Nộp bài thành công!"
12. Teacher nhận notification: "Student X đã nộp bài {assignmentTitle}"

**Luồng thay thế:**

**AF1 - Late submission not allowed:**
- Tại bước 7, NOW > due_date và allow_late_submission = false
- Trả về HTTP 400 Bad Request
- Message: "Đã quá deadline. Assignment không cho phép nộp muộn."

**AF2 - Exceed max late days:**
- Tại bước 7, late_days > max_late_days
- Trả về HTTP 400 Bad Request
- Message: "Quá {max_late_days} ngày deadline. Không thể nộp bài."

**AF3 - Assignment closed:**
- Tại bước 7, assignment status = CLOSED
- Trả về HTTP 400 Bad Request
- Message: "Assignment đã đóng. Không nhận submissions nữa."

**Kết quả:**
- Submission created/updated
- Status = SUBMITTED
- Late status calculated
- Teacher notified

**Events:**
- Event: `ASSIGNMENT_SUBMITTED` (submissionId, assignmentId, studentId, isLate)

---

### UC-ASSIGNMENT-010: Grade Submission

**Người thực hiện:** MAIN_TEACHER, CREATOR, graded_by teacher

**Mục đích:** Teacher chấm điểm submission

**Điều kiện trước:**
- Submission status = SUBMITTED
- User có quyền grade

**Luồng chính:**

1. Teacher truy cập Assignment Detail → Submissions tab
2. Teacher thấy list submissions với status
3. Teacher click vào SUBMITTED submission
4. Frontend hiển thị grading form:
   - Student work (text, files, links)
   - Score input (0 - max_score)
   - Feedback editor (text)
   - Late info (if late)
   - Auto-calculated adjusted score (sau penalty)
5. Teacher xem bài, nhập điểm và feedback
6. Teacher click "Lưu điểm"
7. Frontend gửi POST `/api/v1/submissions/{submissionId}/grade`
   ```json
   {
     "score": 85.0,
     "feedback": "Good work! Cần cải thiện grammar ở phần 2."
   }
   ```
8. Hệ thống validate:
   - Submission tồn tại và SUBMITTED
   - **BR-ASSIGNMENT-007:** score <= max_score
   - User có quyền grade
9. Hệ thống calculate adjusted_score:
   - **BR-ASSIGNMENT-006:** Apply late penalty nếu is_late
   - adjusted_score = score - penalty
10. Hệ thống update AssignmentSubmission:
    - score = input score
    - adjusted_score = calculated
    - feedback = text
    - status = SUBMITTED → GRADED
    - graded_at = NOW()
    - graded_by = teacherId
11. Hệ thống update grade record:
    - Integration với Grade Module
    - Update student's assignment grade component
12. Hệ thống trả về HTTP 200 OK
13. Frontend hiển thị: "Đã chấm điểm"
14. Student nhận notification: "Bài tập {title} đã được chấm điểm: {score}"

**Example với Late Penalty:**
```
Score: 80/100
Late: 2 days
Penalty: 10% per day
Calculation: 80 - (80 * 0.1 * 2) = 80 - 16 = 64
Adjusted Score: 64/100
```

**Luồng thay thế:**

**AF1 - Score invalid:**
- Tại bước 8, score > max_score
- Trả về HTTP 400 Bad Request
- Message: "Điểm không thể vượt quá {max_score}"

**AF2 - Already GRADED:**
- Tại bước 8, submission đã GRADED
- Warning: "Submission đã được chấm. Có muốn update điểm?"
- Teacher có thể proceed để update

**Kết quả:**
- Submission GRADED
- Score và adjusted_score set
- Feedback provided
- Student notified
- Grade record updated

**Events:**
- Event: `SUBMISSION_GRADED` (submissionId, assignmentId, studentId, score, adjustedScore)

---

### UC-ASSIGNMENT-011: Bulk Grade Submissions

**Người thực hiện:** MAIN_TEACHER, CREATOR

**Mục đích:** Chấm nhiều submissions cùng lúc (cho simple assignments)

**Điều kiện trước:**
- Assignment có multiple SUBMITTED submissions
- User có quyền grade

**Luồng chính:**

1. Teacher truy cập Assignment Detail → Submissions tab
2. Teacher click "Chấm điểm hàng loạt"
3. Frontend hiển thị bulk grading table:
   - Columns: Student name, Submission status, Late?, Score input, Quick feedback
   - Pre-filled với NOT_SUBMITTED (score = 0)
4. Teacher nhập điểm cho từng student
5. Teacher có thể add quick feedback (Good, Needs improvement, etc.)
6. Teacher click "Lưu tất cả"
7. Frontend gửi POST `/api/v1/assignments/{assignmentId}/bulk-grade`
   ```json
   {
     "grades": [
       {
         "submissionId": 1,
         "score": 85.0,
         "feedback": "Good work!"
       },
       {
         "submissionId": 2,
         "score": 75.0,
         "feedback": "Needs improvement on grammar"
       },
       {
         "submissionId": 3,
         "score": 0,
         "feedback": "Not submitted"
       }
     ]
   }
   ```
8. Hệ thống validate từng grade:
   - Scores valid (<= max_score)
   - Submissions tồn tại
9. Hệ thống update tất cả submissions (trong transaction)
10. Hệ thống update grade records
11. Hệ thống trả về HTTP 200 OK với summary
12. Frontend hiển thị: "Đã chấm {count} bài"
13. Students nhận notifications

**Response Example:**
```json
{
  "success": true,
  "data": {
    "totalProcessed": 20,
    "successfulGrades": 18,
    "failedGrades": 2,
    "errors": [
      {
        "submissionId": 5,
        "reason": "Score vượt quá max score"
      }
    ]
  }
}
```

**Kết quả:**
- Multiple submissions graded
- Summary report
- Notifications sent
- Grade records updated

**Events:**
- Event: `BULK_GRADING_COMPLETED` (assignmentId, successCount, failCount)

---

### UC-ASSIGNMENT-013: View Submission List

**Người thực hiện:** MAIN_TEACHER, CREATOR

**Mục đích:** Teacher xem overview submissions

**Luồng chính:**

1. Teacher truy cập Assignment Detail → Submissions tab
2. Frontend gửi GET `/api/v1/assignments/{assignmentId}/submissions`
3. Hệ thống query all submissions cho assignment
4. Hệ thống calculate statistics:
   - Total students enrolled
   - Submitted count
   - Not submitted count
   - Graded count
   - Pending grading count
   - Average score
   - On-time vs Late submissions
5. Hệ thống trả về list + stats
6. Frontend hiển thị table:
   - Student name
   - Status (NOT_SUBMITTED, SUBMITTED, GRADED)
   - Submitted time (with late badge)
   - Score (if graded)
   - Actions (View, Grade)
7. Teacher có thể:
   - Sort by status, score, submit time
   - Filter by status (All, Submitted, Graded, Not submitted)
   - Click vào submission → View detail

**Response Example:**
```json
{
  "success": true,
  "data": {
    "statistics": {
      "totalStudents": 20,
      "submittedCount": 15,
      "notSubmittedCount": 5,
      "gradedCount": 10,
      "pendingGradingCount": 5,
      "averageScore": 78.5,
      "onTimeCount": 12,
      "lateCount": 3
    },
    "submissions": [
      {
        "id": 1,
        "studentId": 10,
        "studentName": "Nguyen Van A",
        "status": "GRADED",
        "submittedAt": "2026-02-04T20:30:00Z",
        "isLate": false,
        "score": 85.0,
        "adjustedScore": 85.0
      },
      {
        "id": 2,
        "studentId": 11,
        "studentName": "Tran Thi B",
        "status": "SUBMITTED",
        "submittedAt": "2026-02-06T10:00:00Z",
        "isLate": true,
        "lateDays": 1,
        "score": null
      },
      {
        "id": 3,
        "studentId": 12,
        "studentName": "Le Van C",
        "status": "NOT_SUBMITTED",
        "submittedAt": null,
        "isLate": false,
        "score": null
      }
    ]
  }
}
```

**Kết quả:**
- Teacher có overview đầy đủ
- Easy to track progress
- Identify students cần follow-up

---

### UC-ASSIGNMENT-014: View Assignment Statistics

**Người thực hiện:** MAIN_TEACHER, CREATOR, ADMIN

**Mục đích:** View detailed analytics

**Luồng chính:**

1. Teacher truy cập Assignment Detail → Statistics tab
2. Frontend gửi GET `/api/v1/assignments/{assignmentId}/statistics`
3. Hệ thống calculate:
   - Score distribution (histogram)
   - Average score by status
   - Late submission analysis
   - Grade ranges (A: 90-100, B: 80-89, etc.)
   - Time to submission (how early/late students submit)
4. Frontend display charts:
   - Bar chart: Score distribution
   - Pie chart: Status breakdown
   - Line chart: Submission timeline
   - Table: Grade ranges

**Kết quả:**
- Data-driven insights
- Identify trends
- Assess assignment difficulty

---

## 🔐 5. Permission Model

### Assignment Permissions

**Roles:**
- **OWNER/ADMIN:** Full access all assignments
- **CREATOR (of course):** Full control assignments trong course
- **MAIN_TEACHER (of class):** Full control assignments trong class
- **INSTRUCTOR (of course):** Can create, grade assignments
- **ASSISTANT:** View only
- **STUDENT:** View published, submit own work

**Permission Matrix:**

| Operation | OWNER/ADMIN | CREATOR | MAIN_TEACHER | INSTRUCTOR | ASSISTANT | STUDENT |
|-----------|-------------|---------|--------------|------------|-----------|---------|
| Create Assignment | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| View DRAFT | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| View PUBLISHED | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Edit Assignment | ✅ | ✅ | ✅ | ✅ (limited) | ❌ | ❌ |
| Publish Assignment | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Close Assignment | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Delete Assignment | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Submit Work | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ (own) |
| Grade Submission | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| View All Submissions | ✅ | ✅ | ✅ | ✅ | View only | Own only |
| View Statistics | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |

---

## 🔗 6. Integration với Other Modules

### 6.1. Class Module Integration

**Dependency:** Assignment depends on Class Module

**Integration points:**
- Assignment.class_id FK to classes.id
- Only create assignments for UPCOMING/ONGOING classes
- List assignments in class detail

### 6.2. Student Module Integration

**Dependency:** Assignment uses Student data

**Integration points:**
- AssignmentSubmission.student_id FK to students.id
- Query enrolled students để create placeholder submissions

### 6.3. Grade Module Integration

**Dependency:** Assignment contributes to Grade calculation

**Integration points:**
- Assignment scores → Grade components
- weight_percent used for final grade calculation
- Auto-update grades when submissions graded

**APIs called:**
```java
// Update grade component
POST /internal/grades/update-component
{
  "studentId": 10,
  "classId": 5,
  "componentType": "ASSIGNMENT",
  "componentId": assignmentId,
  "score": adjustedScore,
  "maxScore": assignment.maxScore,
  "weight": assignment.weightPercent
}
```

### 6.4. Teacher Module Integration

**Integration points:**
- Assignment.created_by FK to teachers.id
- AssignmentSubmission.graded_by FK to teachers.id
- Permission checks query teacher_classes/teacher_courses

### 6.5. Notification Module Integration

**Events published:**
- `ASSIGNMENT_PUBLISHED` → Notify students
- `SUBMISSION_GRADED` → Notify student
- `DEADLINE_APPROACHING` → Remind students (24h before)
- `ASSIGNMENT_NOT_SUBMITTED` → Remind after deadline

---

## 📊 7. Summary

### Entities
- ✅ **Assignment:** Main entity
- ✅ **AssignmentSubmission:** Student work
- ⏳ **AssignmentGradingCriteria:** Rubric (optional, future)

### Business Rules
- ✅ BR-ASSIGNMENT-001: Assignment thuộc 1 Class
- ✅ BR-ASSIGNMENT-002: Max score > 0
- ✅ BR-ASSIGNMENT-003: Due date trong tương lai (when publish)
- ✅ BR-ASSIGNMENT-004: Chỉ PUBLISHED nhận submissions
- ✅ BR-ASSIGNMENT-005: Late submission rules
- ✅ BR-ASSIGNMENT-006: Late penalty calculation
- ✅ BR-ASSIGNMENT-007: Score <= Max score
- ✅ BR-ASSIGNMENT-008: 1 student 1 submission per assignment
- ✅ BR-ASSIGNMENT-009: Không edit GRADED assignments

### Use Cases

**Assignment Management:**
- ✅ UC-ASSIGNMENT-001: Create Assignment (Draft)
- ✅ UC-ASSIGNMENT-002: Update Assignment
- ✅ UC-ASSIGNMENT-003: Publish Assignment
- ✅ UC-ASSIGNMENT-004: Close Assignment
- ✅ UC-ASSIGNMENT-005: Delete Assignment

**Student Submission:**
- ✅ UC-ASSIGNMENT-006: Submit Assignment
- ✅ UC-ASSIGNMENT-007: Update Submission
- ✅ UC-ASSIGNMENT-008: View Submission Status
- ✅ UC-ASSIGNMENT-009: View Graded Assignment

**Teacher Grading:**
- ✅ UC-ASSIGNMENT-010: Grade Submission
- ✅ UC-ASSIGNMENT-011: Bulk Grade Submissions
- ✅ UC-ASSIGNMENT-012: Return Graded Work
- ✅ UC-ASSIGNMENT-013: View Submission List
- ✅ UC-ASSIGNMENT-014: View Assignment Statistics

**Total:** 14 use cases

### Lifecycle
```
DRAFT → PUBLISHED → GRADED → CLOSED

DRAFT:
- Created, being edited
- Not visible to students
- Can edit freely
- Can delete

PUBLISHED:
- Students can submit
- Limited edits
- Cannot delete
- Can close

GRADED:
- All submissions graded
- Read-only (mostly)
- Can view statistics

CLOSED:
- No new submissions
- Teacher can still grade pending
- Read-only for students
```

### Integration
- ✅ Class Module: Assignment thuộc class
- ✅ Student Module: Submissions từ students
- ✅ Grade Module: Contribute to final grades
- ✅ Teacher Module: Permissions và grading
- ✅ Notification Module: Events và notifications

---

## 🚀 Next Steps

**Sau khi document này được approve:**

1. **Create PR: Assignment Module**
   - Implement Assignment entity
   - Implement AssignmentSubmission entity
   - Implement repositories
   - Implement services (lifecycle, grading)
   - Implement REST APIs
   - Implement late penalty calculation
   - Write tests (unit + integration)

2. **Integration với Grade Module**
   - Ensure grade calculation works
   - Test weight_percent contribution

3. **Notification Integration**
   - Publish events
   - Test notifications

---

**Author:** VictorAurelius + Claude Sonnet 4.5
**Created:** 2026-01-28
**Status:** Ready for Review
**Next:** Grade Module business logic
