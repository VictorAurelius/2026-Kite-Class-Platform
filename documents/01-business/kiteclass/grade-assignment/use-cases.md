# Grade & Assignment — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## Use Cases — Assignment

### UC-GRD-01: Create Assignment

**Actor:** Teacher (MAIN_TEACHER / INSTRUCTOR)
**Precondition:** Teacher assigned to class per BR-ASG-005

**Steps:**
1. FE: Display assignment creation form (title, type, max_score, due_date, description)
2. Teacher: Fill form, select type per BR-ASG-001 (HOMEWORK/PROJECT/QUIZ/ESSAY)
3. System: Validate max_score > 0 per BR-ASG-003, due_date required per BR-ASG-004
4. System: Save assignment with status = DRAFT
5. FE: Redirect to assignment detail page

**Postcondition:** Assignment created in DRAFT status

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Missing due_date | "Due date is required" |
| 400 | max_score <= 0 | "Max score must be positive" |
| 403 | Not class teacher | "Only class teachers can create assignments" |

---

### UC-GRD-02: Publish / Close Assignment

**Actor:** Teacher
**Precondition:** Assignment in DRAFT (publish) or PUBLISHED (close)

**Steps:**
1. Teacher: Click Publish or Close button
2. System: Transition status DRAFT -> PUBLISHED or PUBLISHED -> CLOSED
3. System: On close, block future submissions per BR-ASG-006
4. FE: Toast success, update status badge

**Postcondition:** Assignment status updated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invalid transition | "Cannot publish a non-draft assignment" |

---

### UC-GRD-03: Submit Assignment

**Actor:** Student
**Precondition:** Assignment PUBLISHED, before close_date per BR-ASG-006

**Steps:**
1. FE: Display submission form (text, file upload)
2. Student: Submit work
3. System: Check not past close_date per BR-ASG-006
4. System: If past due_date, calculate late penalty per BR-ASG-002 (10%/day, max 50%)
5. System: Save submission with late_penalty metadata
6. FE: Toast success with late warning if applicable

**Postcondition:** Submission recorded

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Past close_date | "Assignment is closed for submissions" |
| 404 | Assignment not found | "Assignment not found" |

---

### UC-GRD-04: Grade Submission

**Actor:** Teacher
**Precondition:** Submission exists for a PUBLISHED/CLOSED assignment

**Steps:**
1. FE: Display submission with student work and grading form
2. Teacher: Enter score, feedback, click Grade
3. System: Validate score <= max_score per BR-ASG-007
4. System: Apply late penalty if applicable per BR-ASG-002
5. System: Save graded submission
6. FE: Toast success, move to next ungraded submission

**Postcondition:** Submission graded with final score

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Score exceeds max | "Score cannot exceed max score" |
| 404 | Submission not found | "Submission not found" |

---

### UC-GRD-05: Return Submission for Revision

**Actor:** Teacher
**Precondition:** Submission is graded

**Steps:**
1. Teacher: Click Return with feedback notes
2. System: Reset submission status, student can resubmit
3. FE: Toast success

**Postcondition:** Submission returned, student notified

---

## Use Cases — Grade

### UC-GRD-06: Initialize Grade Record

**Actor:** System / Admin
**Precondition:** Student enrolled in class

**Steps:**
1. System: POST /grades/initialize with studentId + classId
2. System: Create grade record with default components per BR-GRD-003
3. System: Set weights: Attendance 10%, Assignments 30%, Midterm 25%, Final 35%

**Postcondition:** Grade record initialized with component structure

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 409 | Grade already exists | "Grade record already exists for this student and class" |

---

### UC-GRD-07: Calculate Final Grade

**Actor:** Teacher / System
**Precondition:** Grade components have scores

**Steps:**
1. Teacher: Click Calculate on student grade
2. System: Collect component scores (attendance rate, assignment avg, midterm, final)
3. System: Calculate weighted average per BR-GRD-001, validate weights sum 100% per BR-GRD-002
4. System: Determine letter grade per BR-GRD-005, GPA per BR-GRD-006
5. System: Determine pass/fail per BR-GRD-004 (threshold 50/100)
6. FE: Display calculated grade with breakdown

**Postcondition:** Final grade calculated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Weights don't sum to 100% | "Grade component weights must total 100%" |
| 404 | Grade record not found | "Grade not found" |

---

### UC-GRD-08: Finalize / Unfinalize Grade

**Actor:** Teacher / Admin
**Precondition:** Grade calculated (finalize) or finalized (unfinalize)

**Steps:**
1. Teacher: Click Finalize — locks grade from further edits
2. System: Mark grade as finalized, record timestamp
3. Teacher (if needed): Click Unfinalize to reopen for corrections per BR-GRD-007

**Postcondition:** Grade locked/unlocked

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Already finalized | "Grade is already finalized" |

---

### UC-GRD-09: Generate and View Transcript

**Actor:** Student / Teacher / Admin
**Precondition:** Finalized grades exist

**Steps:**
1. System: POST /transcripts/generate — compile all finalized grades
2. FE: GET transcript by student + semester or full history
3. FE: Display transcript with courses, letter grades, GPA per BR-GRD-006

**Postcondition:** Transcript available for viewing

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | No grades found | "No transcript data available" |
