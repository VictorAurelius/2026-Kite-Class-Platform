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

## Common Error Responses

| HTTP | Code | Description |
|------|------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid input (duplicate orderNumber, module has lessons, etc.) |
| 403 | `PERMISSION_DENIED` | Not course owner, not enrolled, guest accessing paid lesson |
| 404 | `NOT_FOUND` | Course, module, lesson, or resource not found |
