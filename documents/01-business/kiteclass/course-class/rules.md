# Course & Class — Business Rules

**Domain:** KiteClass Core
**Version:** 1.1.0
**Updated:** 2026-05-04 (GAP-290 Wave 18a — recurrence_rule)

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
| BR-CLASS-009 | Structured recurrence rule (GAP-290 / Wave 18a) | Class MAY have a `recurrence_rule` JSONB (RFC 5545 RRULE subset). When set, sessions are generated via `RecurrenceService`. Phase 1: WEEKLY only. See §5 below. |

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
- `idx_classes_recurrence_rule_gin` — Partial GIN index on `recurrence_rule` (NULL excluded)
- `idx_class_sessions_class_id` — Sessions per class
- `idx_class_sessions_unique` — Unique (class_id, session_number)

---

## 5. BR-CLASS-009 — Structured Recurrence Rule (GAP-290 Wave 18a)

### 5.1 JSONB schema

`classes.recurrence_rule` (nullable JSONB; `NULL` = no recurrence, sessions created manually or via legacy `/schedule` endpoint):

```json
{
  "freq": "WEEKLY",
  "by_day": ["TU", "TH"],
  "start_time": "19:00",
  "end_time": "20:30",
  "until": "2026-08-01",
  "exclude_dates": ["2026-06-15"]
}
```

### 5.2 Constraints (BR-CLASS-009)

| # | Constraint | Error code |
|---|------------|-----------|
| 1 | `freq=WEEKLY` only (Phase 1) | `RECURRENCE_REQUIRED` if missing |
| 2 | `by_day` non-empty; values from `MO/TU/WE/TH/FR/SA/SU` | `RECURRENCE_NO_DAYS` |
| 3 | `end_time` strictly after `start_time` | `RECURRENCE_INVALID_TIME` |
| 4 | `until` required; must be on/after class.startDate (or today) | `RECURRENCE_INVALID_RANGE` |
| 5 | `exclude_dates` optional; deduped by service | — |
| 6 | Range cap: `until - start <= 3700 days` (~10 years) | `RECURRENCE_RANGE_TOO_LARGE` |
| 7 | Class status must be `SCHEDULED` or `IN_PROGRESS` | `CLASS_RECURRENCE_LOCKED` |

### 5.3 Edit state machine

Re-running `POST /api/v1/classes/{id}/sessions/generate-from-recurrence` is idempotent:

```
existing ClassSession decision:
  - sessionDate < today  OR  attendanceTaken == true   → preserved
  - sessionDate >= today AND attendanceTaken == false  → soft-deleted, regenerated
```

The newly-planned occurrences are filtered to skip dates that match a preserved session
(prevents duplicate session_date when the same day is both preserved and in the new rule's range).

### 5.4 Configuration

| Key | Default | Description |
|-----|---------|-------------|
| `recurrence.range.max-days` | `3700` | Hard cap on total `until - start` in days |
| `recurrence.tz.default` | `Asia/Ho_Chi_Minh` | FE-side rendering timezone (RRULE itself is timezone-agnostic) |

### 5.5 Source / Rationale / Reviewer / Compliance / Cadence (per `business-logic-review.md`)

- **Source:** AC-OPS-002 from P1 Solo Teacher persona review (Wave 17 round 1, 2026-05-04). Cross-cutting: P1 + P2 + P3 + P5 personas all need recurring class scheduling.
- **Rationale:** RFC 5545 is the industry standard; `WEEKLY + by_day + until + exclude_dates` covers ~95% of education center scheduling needs (M-W-F evening, T-Th evening, weekend block). 3700-day cap prevents runaway loops without restricting any realistic class span.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead, solo-dev, 2026-05-04). Phase 1 only — no compliance review needed (no PII/payment/regulated content).
- **Compliance check:** N/A — scheduling data, no regulated area touched.
- **Review cadence:** Quarterly. **Next review:** 2026-08-04. Event triggers: persona review round 2 (Wave 19) feedback, MONTHLY/YEARLY frequencies requested by ≥3 tenants, `recurrence_rule` corruption incident.
