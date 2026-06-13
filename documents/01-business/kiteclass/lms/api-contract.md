# LMS (Learning Management System) — API Contract

> Base path: `/api/v1/lms`
> Controllers: `LmsController`, `LessonProgressController`

---

## Public / Student Endpoints

### GET `/courses/{courseId}/modules` — UC-LMS-01

- **Headers:** `X-User-Id` (optional — omit for guest mode)
- **Response:** `ApiResponse<List<CourseModuleDetailResponse>>`

```json
{ "id": 1, "courseId": 10, "title": "...", "description": "...",
  "orderNumber": 1, "lessonCount": 3,
  "lessons": [{ "id": 1, "moduleId": 1, "title": "...", "content": "...",
    "videoUrl": "...", "isTrial": true, "orderNumber": 1,
    "estimatedDuration": 15, "createdAt": "...", "updatedAt": "..." }],
  "createdAt": "...", "updatedAt": "..." }
```

### GET `/lessons/{lessonId}` — UC-LMS-02

- **Headers:** `X-User-Id` (optional)
- **Response:** `ApiResponse<LessonDetailResponse>`

```json
{ "id": 1, "moduleId": 1, "title": "...", "content": "...",
  "videoUrl": "...", "isTrial": false, "orderNumber": 1,
  "estimatedDuration": 30, "resourceCount": 2,
  "resources": [{ "id": 1, "lessonId": 1, "type": "VIDEO",
    "url": "...", "title": "...", "fileSize": 1048576,
    "createdAt": "...", "updatedAt": "..." }],
  "createdAt": "...", "updatedAt": "..." }
```

---

## Teacher Endpoints — Module CRUD (UC-LMS-03)

### POST `/courses/{courseId}/modules`

- **Headers:** `X-Teacher-Id` (required)
- **Request:** `CreateCourseModuleRequest`
  ```json
  { "title": "string (required, max 200)",
    "description": "string (optional, max 5000)",
    "orderNumber": "int (required, min 1)" }
  ```
- **Response:** `201` `ApiResponse<CourseModuleResponse>`

### PUT `/modules/{moduleId}`

- **Headers:** `X-Teacher-Id` (required)
- **Request:** `UpdateCourseModuleRequest` (all fields optional)
  ```json
  { "title": "string (max 200)",
    "description": "string (max 5000)",
    "orderNumber": "int (min 1)" }
  ```
- **Response:** `200` `ApiResponse<CourseModuleResponse>`

### DELETE `/modules/{moduleId}`

- **Headers:** `X-Teacher-Id` (required)
- **Response:** `204` `ApiResponse<Void>`
- **Error:** `400` if module has lessons (BR-LMS-007)

### GET `/modules/{moduleId}`

- **Headers:** `X-Teacher-Id` (required)
- **Response:** `200` `ApiResponse<CourseModuleDetailResponse>`

---

## Teacher Endpoints — Lesson CRUD (UC-LMS-04)

### POST `/modules/{moduleId}/lessons`

- **Headers:** `X-Teacher-Id` (required)
- **Request:** `CreateLessonRequest`
  ```json
  { "title": "string (required, max 200)",
    "content": "string (optional, max 10000)",
    "videoUrl": "string (optional, max 500)",
    "isTrial": "boolean (default false)",
    "orderNumber": "int (required, min 1)",
    "estimatedDuration": "int (optional, minutes, min 1)" }
  ```
- **Response:** `201` `ApiResponse<LessonResponse>`

### PUT `/lessons/{lessonId}/manage`

- **Headers:** `X-Teacher-Id` (required)
- **Request:** `UpdateLessonRequest` (all fields optional)
- **Response:** `200` `ApiResponse<LessonResponse>`

### DELETE `/lessons/{lessonId}/manage`

- **Headers:** `X-Teacher-Id` (required)
- **Response:** `204` `ApiResponse<Void>`

### GET `/lessons/{lessonId}/manage`

- **Headers:** `X-Teacher-Id` (required)
- **Response:** `200` `ApiResponse<LessonDetailResponse>`

---

## Teacher Endpoints — Resource (UC-LMS-05)

### POST `/lessons/{lessonId}/resources`

- **Headers:** `X-Teacher-Id` (required)
- **Request:** `CreateLearningResourceRequest`
  ```json
  { "type": "VIDEO|PDF|SLIDE|AUDIO|LINK|CODE|OTHER (required)",
    "url": "string (required, max 500)",
    "title": "string (required, max 200)",
    "fileSize": "long (optional, bytes, min 1)" }
  ```
- **Response:** `201` `ApiResponse<LearningResourceResponse>`

### DELETE `/resources/{resourceId}`

- **Headers:** `X-Teacher-Id` (required)
- **Response:** `204` `ApiResponse<Void>`

---

## Progress Endpoints — `/progress` (UC-LMS-06/07/08)

### POST `/progress/lessons/{lessonId}/complete` — UC-LMS-06

- **Headers:** `X-User-Id` (required)
- **Response:** `200` `ApiResponse<LessonProgressResponse>`

```json
{ "id": 1, "userId": 100, "lessonId": 5,
  "completed": true, "completedAt": "2026-03-24T10:00:00",
  "progressPercent": 100, "createdAt": "...", "updatedAt": "..." }
```

### GET `/progress/courses/{courseId}` — UC-LMS-07

- **Headers:** `X-User-Id` (required)
- **Response:** `200` `ApiResponse<CourseProgressResponse>`

```json
{ "courseId": 10, "userId": 100,
  "totalLessons": 20, "completedLessons": 8, "progressPercent": 40.0 }
```

### GET `/progress/lessons/{lessonId}` — UC-LMS-08

- **Headers:** `X-User-Id` (required)
- **Response:** `200` `ApiResponse<LessonProgressResponse>` (or `null` body if no record)

---

## Teacher Endpoints — Reorder (UC-LMS-09, Phase0-BE)

> Drag-drop reorder. The FE sends the **FULL ordered set** of siblings; the server
> applies order numbers atomically (two-phase negative-park swap so the
> `(course_id|module_id, order_number)` unique constraint is never transiently
> violated). Partial sets / duplicate order numbers are rejected.

### PUT `/courses/{courseId}/modules/reorder`

- **Headers:** `X-Teacher-Id` (required, must be course owner)
- **Request:** `ReorderRequest`
  ```json
  { "items": [ { "id": 10, "orderNumber": 2 }, { "id": 11, "orderNumber": 1 } ] }
  ```
- **Response:** `200` `ApiResponse<List<CourseModuleResponse>>` (ascending `orderNumber`)
- **Errors:** `400 VALIDATION_ERROR` (`REORDER_INCOMPLETE_SET` / `REORDER_DUPLICATE_ID` / `REORDER_DUPLICATE_ORDER`), `403 PERMISSION_DENIED` (not owner), `404 NOT_FOUND`

### PUT `/modules/{moduleId}/lessons/reorder`

- **Headers:** `X-Teacher-Id` (required, must be course owner)
- **Request:** `ReorderRequest` (same shape — `items[].id` = lesson IDs)
- **Response:** `200` `ApiResponse<List<LessonResponse>>` (ascending `orderNumber`)
- **Errors:** as above

---

## Teacher Endpoints — Resource Upload (UC-LMS-10, Phase0-BE)

> Presigned MinIO/S3 upload reusing the central storage pipeline (`StorageService` —
> MIME whitelist + tenant quota + presigned PUT, 30-min TTL). `learning_resources`
> stays a metadata table: after uploading + confirming, the teacher persists the row
> via `POST /lessons/{lessonId}/resources` with the resulting file URL.

### POST `/lessons/{lessonId}/resources/upload-url`

- **Headers:** `X-Teacher-Id` (required, must be course owner)
- **Request:** `PresignedUploadRequest`
  ```json
  { "fileName": "slides.pdf", "fileSize": 1048576,
    "mimeType": "application/pdf", "fileType": "DOCUMENT", "accessLevel": "TENANT" }
  ```
- **Response:** `201` `ApiResponse<PresignedUploadResponse>`
  ```json
  { "fileId": 7, "uploadUrl": "https://<minio>/...signed...", "expiresAt": "2026-06-14T10:30:00Z" }
  ```
- **Client workflow:** (1) call this → (2) HTTP PUT file to `uploadUrl` → (3) `POST /api/v1/storage/{fileId}/confirm` → (4) `POST /lessons/{lessonId}/resources` with the file URL + title.
- **Errors:** `400 VALIDATION_ERROR` (file too large / type not allowed / quota exceeded), `403 PERMISSION_DENIED` (not owner), `404 NOT_FOUND`

---

## Teacher Endpoints — Completion Roster (UC-LMS-11, Phase0-BE)

### GET `/courses/{courseId}/completion-roster`

- **Headers:** `X-Teacher-Id` (required, must be course owner)
- **Response:** `200` `ApiResponse<CompletionRosterResponse>`
  ```json
  { "courseId": 10, "totalLessons": 20,
    "students": [
      { "userId": 100, "completedLessons": 8, "progressPercent": 40.0,
        "completedLessonIds": [1, 2, 3, 5, 8, 11, 13, 18] }
    ] }
  ```
- Only students with ≥1 completed lesson appear in `students`. `progressPercent` = `completedLessons / totalLessons * 100` (2 dp).
- **Errors:** `403 PERMISSION_DENIED` (not owner), `404 NOT_FOUND`

---

## Related — Course catalog + lifecycle (Course domain, NOT `/api/v1/lms`)

Course **list/search** and the **publish/unpublish/archive** lifecycle live in the
Course domain (`/api/v1/courses`, `CourseController`) — the FE catalog consumes these,
not the LMS controller:

- `GET /api/v1/courses?status=PUBLISHED&search=&teacherId=&page=&size=&sort=` — paginated, tenant-scoped catalog.
- `POST /api/v1/courses/{id}/publish` — DRAFT → PUBLISHED.
- `POST /api/v1/courses/{id}/unpublish` — **PUBLISHED → DRAFT (NEW, Phase0-BE)** — revert for full re-editing, then re-publish.
- `POST /api/v1/courses/{id}/archive` — PUBLISHED → ARCHIVED (take off catalog; ARCHIVED is terminal).

---

## Common Error Responses

| HTTP | Code | Description |
|------|------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid input (duplicate orderNumber, module has lessons, etc.) |
| 403 | `PERMISSION_DENIED` | Not course owner, not enrolled, guest accessing paid lesson |
| 404 | `NOT_FOUND` | Course, module, lesson, or resource not found |
