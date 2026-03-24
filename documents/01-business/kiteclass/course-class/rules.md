# Course & Class — Business Rules

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## 1. Rules

### Course Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-CRS-001 | Name unique per teacher | Same teacher cannot have two courses with same name (case-insensitive) |
| BR-CRS-002 | Must have CREATOR | Auto-create TeacherCourse(CREATOR) on course creation |
| BR-CRS-003 | DRAFT editable freely | PUBLISHED courses: only description, syllabus editable |
| BR-CRS-004 | Cannot delete with classes | DRAFT course with existing classes cannot be deleted |
| BR-CRS-005 | Publish requires fields | Name, level, duration_weeks required to publish |
| BR-CRS-006 | Archive stops enrollment | ARCHIVED courses reject new enrollments |
| BR-CRS-007 | Multi-tenant isolation | All queries filtered by `instance_id` |

**Course statuses:** DRAFT, PUBLISHED, ARCHIVED

**Course levels:** BEGINNER, ELEMENTARY, INTERMEDIATE, UPPER_INTERMEDIATE, ADVANCED, PROFICIENCY

**TeacherCourse roles:** CREATOR (full control), INSTRUCTOR (teach + manage), ASSISTANT (view only)

### Class Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-CLS-001 | Must belong to course | `course_id` is required FK |
| BR-CLS-002 | Class code unique | 6-20 chars uppercase, nullable, unique when set |
| BR-CLS-003 | Capacity enforced | `current_enrolled <= max_students` |
| BR-CLS-004 | Status lifecycle | UPCOMING -> ONGOING -> COMPLETED; any -> CANCELLED |
| BR-CLS-005 | End after start | `end_date > start_date` |
| BR-CLS-006 | Location type required | IN_PERSON or ONLINE |
| BR-CLS-007 | Session auto-generate | Sessions created from schedule when class created |

**Class statuses:** UPCOMING, ONGOING, COMPLETED, CANCELLED

**TeacherClass roles:** MAIN_TEACHER (full control), ASSISTANT (limited)

---

## 2. Flow

### Course Lifecycle
1. Teacher creates course -> status = DRAFT, auto TeacherCourse(CREATOR)
2. Teacher edits syllabus, objectives, pricing
3. Teacher publishes -> status = PUBLISHED (requires name, level, duration)
4. Students enroll via classes (not directly to course)
5. Course completed -> teacher archives -> status = ARCHIVED

### Class Lifecycle
1. Admin/Teacher creates class under course -> status = UPCOMING
2. Generate class code for self-enrollment
3. Assign teacher(s) via TeacherClass
4. Class starts -> status = ONGOING
5. Teacher takes attendance, creates assignments per session
6. Class ends -> status = COMPLETED

### Session Flow
1. Sessions auto-generated from class schedule (e.g., 12 weeks x 3 days = 36 sessions)
2. Each session has: session_number, date, start_time, end_time, topic
3. Session statuses: SCHEDULED, COMPLETED, CANCELLED

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| (Planned) Course published | course-published | Enrolled students |
| (Planned) Class starting soon | class-reminder | Enrolled students |
| (Planned) Class cancelled | class-cancelled | Enrolled students + teachers |

> Email templates not yet implemented.

---

## 4. Config

| Key | Default | Description |
|-----|---------|-------------|
| `course.name.max-length` | `200` | Max course name characters |
| `course.description.max-length` | `5000` | Max description characters |
| `course.syllabus.max-length` | `10000` | Max syllabus characters |
| `class.code.length` | `6-20` | Class code character range |
| `class.code.expiry` | configurable | Code expiration timestamp |
| `class.max-students.default` | `20` | Default max students per class |

### Database Indexes
- `idx_courses_status` — Course status filter
- `idx_courses_subject` — Subject filter
- `idx_courses_level` — Level filter
- `idx_courses_created_by` — Creator lookup
- `idx_classes_course_id` — Classes per course
- `idx_classes_status` — Class status filter
- `idx_classes_class_code` — Class code lookup (unique)
- `idx_class_sessions_class_id` — Sessions per class
- `idx_class_sessions_unique` — Unique (class_id, session_number)
