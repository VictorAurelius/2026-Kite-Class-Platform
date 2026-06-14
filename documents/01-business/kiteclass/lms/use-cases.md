# LMS (Learning Management System) — Use Cases

> Domain: `kiteclass/lms`
> Controllers: `LmsController`, `LessonProgressController`

---

## UC-LMS-01: View Course Structure (Guest/Student)

- **Actor:** Guest (unauthenticated) or Student (enrolled)
- **Endpoint:** `GET /api/v1/lms/courses/{courseId}/modules`
- **Steps:**
  1. System checks `X-User-Id` header presence
  2. **Guest mode** (no header): Return modules with trial lessons only (BR-LMS-001)
  3. **Student mode** (header present): Verify enrollment (BR-LMS-002), return modules with all lessons
  4. Modules and lessons sorted by `orderNumber`
- **Errors:**
  - `404` — Course not found
  - `403` — Student not enrolled (student mode, paid course)
  - `400` — Course not published (guest mode)

## UC-LMS-02: View Lesson Detail (Guest/Student)

- **Actor:** Guest or Student
- **Endpoint:** `GET /api/v1/lms/lessons/{lessonId}`
- **Steps:**
  1. System checks `X-User-Id` header presence
  2. **Guest mode**: Only allow if `isTrial=true` (BR-LMS-001)
  3. **Student mode**: Allow trial lessons freely; for paid lessons, verify enrollment (BR-LMS-002)
  4. Return lesson detail with attached resources
- **Errors:**
  - `404` — Lesson not found
  - `403` — Guest accessing paid lesson, or student not enrolled

## UC-LMS-03: Module CRUD (Teacher)

- **Actor:** Teacher (course owner)
- **Endpoints:**
  - `POST /api/v1/lms/courses/{courseId}/modules` — Create
  - `PUT /api/v1/lms/modules/{moduleId}` — Update
  - `DELETE /api/v1/lms/modules/{moduleId}` — Delete
  - `GET /api/v1/lms/modules/{moduleId}` — Get detail
- **Steps (Create):**
  1. Role gate `TEACHER`/`OWNER`/`ADMIN`, then verify the acting teacher (gateway `X-User-Reference-Id`, NOT client `X-Teacher-Id` — GAP-1299) is the course owner; OWNER/ADMIN bypass ownership (BR-LMS-006)
  2. Validate `title` required, `orderNumber` unique within course (BR-LMS-004, BR-LMS-005)
  3. Create module under course — module belongs to exactly one course (BR-LMS-003), return `CourseModuleResponse`
- **Steps (Update):**
  1. Verify ownership (BR-LMS-006)
  2. Partial update — only non-null fields applied (BR-LMS-011)
  3. Validate `orderNumber` uniqueness if changed (BR-LMS-004)
- **Steps (Delete):**
  1. Verify ownership (BR-LMS-006)
  2. Reject if module has lessons (BR-LMS-007)
  3. Delete module
- **Errors:**
  - `404` — Course or module not found
  - `403` — Teacher is not course owner
  - `400` — Duplicate `orderNumber`, or module has lessons (delete)

## UC-LMS-04: Lesson CRUD (Teacher)

- **Actor:** Teacher (course owner)
- **Endpoints:**
  - `POST /api/v1/lms/modules/{moduleId}/lessons` — Create
  - `PUT /api/v1/lms/lessons/{lessonId}/manage` — Update
  - `DELETE /api/v1/lms/lessons/{lessonId}/manage` — Delete
  - `GET /api/v1/lms/lessons/{lessonId}/manage` — Get detail
- **Steps (Create):**
  1. Verify teacher owns the course that contains the module (BR-LMS-010)
  2. Validate fields (BR-LMS-008, BR-LMS-009): title required, orderNumber unique within module
  3. `isTrial` defaults to `false` if not provided
  4. Create lesson, return `LessonResponse`
- **Steps (Update):**
  1. Verify ownership (BR-LMS-010)
  2. Partial update (BR-LMS-011)
  3. Validate orderNumber uniqueness if changed (BR-LMS-008)
- **Steps (Delete):**
  1. Verify ownership (BR-LMS-010)
  2. Delete lesson and associated resources
- **Errors:**
  - `404` — Module or lesson not found
  - `403` — Not course owner
  - `400` — Duplicate orderNumber

## UC-LMS-05: Manage Learning Resources (Teacher)

- **Actor:** Teacher (course owner)
- **Endpoints:**
  - `POST /api/v1/lms/lessons/{lessonId}/resources` — Add
  - `DELETE /api/v1/lms/resources/{resourceId}` — Delete
- **Steps (Add):**
  1. Verify teacher owns the course (BR-LMS-015)
  2. Validate `type` (enum), `url`, `title` required (BR-LMS-012, BR-LMS-013, BR-LMS-014)
  3. Create resource, return `LearningResourceResponse`
- **Steps (Delete):**
  1. Verify ownership (BR-LMS-015)
  2. Delete resource
- **Errors:**
  - `404` — Lesson or resource not found
  - `403` — Not course owner
  - `400` — Invalid resource type

## UC-LMS-06: Complete Lesson (Student)

- **Actor:** Student (enrolled)
- **Endpoint:** `POST /api/v1/lms/progress/lessons/{lessonId}/complete`
- **Steps:**
  1. Verify student enrollment for paid lessons (BR-LMS-019)
  2. Create or update progress record — idempotent (BR-LMS-016)
  3. Set `completed=true`, `completedAt=now()`
  4. Publish `LessonCompletedEvent` (BR-LMS-017)
  5. Return `LessonProgressResponse`
- **Errors:**
  - `404` — Lesson not found
  - `403` — Not enrolled (paid lesson)

## UC-LMS-07: View Course Progress (Student)

- **Actor:** Student (enrolled)
- **Endpoint:** `GET /api/v1/lms/progress/courses/{courseId}`
- **Steps:**
  1. Verify enrollment (BR-LMS-002)
  2. Count total lessons across all modules
  3. Count completed lessons for this student
  4. Calculate `progressPercent` (BR-LMS-018)
  5. Return `CourseProgressResponse`
- **Errors:**
  - `404` — Course not found
  - `403` — Not enrolled

## UC-LMS-08: View Lesson Progress (Student)

- **Actor:** Student
- **Endpoint:** `GET /api/v1/lms/progress/lessons/{lessonId}`
- **Steps:**
  1. Look up progress record for student + lesson
  2. Return `LessonProgressResponse` or `null` if no record (BR-LMS-020)
- **Errors:**
  - `404` — Lesson not found
