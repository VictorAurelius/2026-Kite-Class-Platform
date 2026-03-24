# Course & Class — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## Use Cases

### UC-CRS-01: Create Course

**Actor:** Teacher
**Precondition:** Teacher authenticated, has ACTIVE status

**Steps:**
1. FE: Display course creation form (name, level, category, description, syllabus, durationWeeks, tuitionFee)
2. User: Fill fields, name required and unique per teacher (BR-CRS-001)
3. System: Auto-create TeacherCourse with role CREATOR (BR-CRS-002)
4. System: Set instance_id for tenant isolation (BR-CRS-007)
5. System: Save course with status DRAFT
6. FE: Redirect to course detail page

**Postcondition:** Course created as DRAFT with teacher as CREATOR

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Name blank or > 200 chars | "Course name is required" |
| 409 | Duplicate name for same teacher | "Course name already exists" |

---

### UC-CRS-02: Update Course

**Actor:** Teacher (CREATOR or INSTRUCTOR)
**Precondition:** Course exists

**Steps:**
1. FE: Display edit form with current values
2. User: Modify fields
3. System: If PUBLISHED, only allow description and syllabus changes (BR-CRS-003)
4. System: Save updates
5. FE: Show success toast

**Postcondition:** Course updated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Editing restricted field on PUBLISHED course | "Cannot modify this field after publishing" |
| 404 | Course not found | "Course not found" |

---

### UC-CRS-03: Publish Course

**Actor:** Teacher (CREATOR)
**Precondition:** Course is in DRAFT status

**Steps:**
1. User: Click "Publish" button
2. System: Validate required fields — name, level, durationWeeks (BR-CRS-005)
3. System: Transition status DRAFT → PUBLISHED
4. FE: Update status badge, show success toast

**Postcondition:** Course status is PUBLISHED, visible for enrollment

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Missing required fields | "Name, level, and duration are required to publish" |
| 400 | Not in DRAFT status | "Only DRAFT courses can be published" |

---

### UC-CRS-04: Archive Course

**Actor:** Teacher (CREATOR)
**Precondition:** Course is PUBLISHED

**Steps:**
1. User: Click "Archive" button, confirm dialog
2. System: Transition status PUBLISHED → ARCHIVED (BR-CRS-006)
3. System: New enrollments blocked for all classes under this course
4. FE: Update status badge

**Postcondition:** Course ARCHIVED, no new enrollments accepted

---

### UC-CRS-05: Delete Course

**Actor:** Teacher (CREATOR)
**Precondition:** Course is DRAFT with no existing classes

**Steps:**
1. User: Click delete, confirm dialog
2. System: Check no classes exist under course (BR-CRS-004)
3. System: Soft-delete course
4. FE: Redirect to course list

**Postcondition:** Course deleted

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Course has existing classes | "Cannot delete course with existing classes" |
| 400 | Course not in DRAFT | "Only DRAFT courses can be deleted" |

---

### UC-CRS-06: Manage Prerequisites

**Actor:** Teacher (CREATOR)
**Precondition:** Both courses exist

**Steps:**
1. User: Select prerequisite course from dropdown
2. System: POST /{id}/prerequisites/{prereqId} — link courses
3. FE: Show prerequisite in course detail
4. User: Optionally remove via DELETE /{id}/prerequisites/{prereqId}

**Postcondition:** Prerequisite relationship created or removed

---

### UC-CRS-07: Create Class

**Actor:** Teacher / Admin
**Precondition:** Course exists (BR-CLS-001)

**Steps:**
1. FE: Display class form (name, startDate, endDate, maxStudents, locationType, location, schedule)
2. User: Fill fields, endDate must be after startDate (BR-CLS-005)
3. System: Select locationType IN_PERSON or ONLINE (BR-CLS-006)
4. System: Save class with status UPCOMING under course; maxStudents enforced as capacity limit (BR-CLS-003)
5. System: Auto-generate sessions from schedule if provided (BR-CLS-007)
6. FE: Redirect to class detail

**Postcondition:** Class created as UPCOMING, sessions generated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | End date before start date | "End date must be after start date" |
| 404 | Course not found | "Course not found" |

---

### UC-CRS-08: Start / Complete / Cancel Class

**Actor:** Teacher (MAIN_TEACHER) / Admin
**Precondition:** Class exists in valid status

**Steps:**
1. User: Click Start (UPCOMING→ONGOING), Complete (ONGOING→COMPLETED), or Cancel (any→CANCELLED)
2. System: Validate status transition per BR-CLS-004
3. System: Update class status
4. FE: Update status badge

**Postcondition:** Class status transitioned

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Invalid transition | "Cannot transition from {current} to {target}" |

---

### UC-CRS-09: Generate Class Code

**Actor:** Teacher / Admin
**Precondition:** Class exists

**Steps:**
1. User: Click "Generate Code"
2. System: Generate unique code, 6-20 chars uppercase (BR-CLS-002)
3. System: Save code on class record
4. FE: Display code for sharing with students

**Postcondition:** Unique class code generated and stored

---

### UC-CRS-10: Create Schedule & List Sessions

**Actor:** Teacher / Admin
**Precondition:** Class exists

**Steps:**
1. User: Define schedule (days of week, start/end time, number of weeks)
2. System: POST /{classId}/schedule — auto-generate sessions (BR-CLS-007)
3. System: Each session gets session_number, date, start_time, end_time
4. User: GET /{classId}/sessions to view generated sessions
5. FE: Display session calendar/list

**Postcondition:** Sessions generated per schedule pattern
