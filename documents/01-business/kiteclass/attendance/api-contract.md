# Attendance — API Contract

> Extracted from: `AttendanceController`, `AttendanceResponse`, `AttendanceStatsResponse`
> Base path: `/api/v1/attendance`

## Endpoints

### POST `/api/v1/attendance`
Mark attendance for a single student.
- **Request:** `CreateAttendanceRequest`
  - `enrollmentId` (Long, required), `sessionId` (Long, required), `status` (enum, required), `notes` (String), `markedBy` (Long, required)
- **Response:** `AttendanceResponse` (200)
- **Errors:** `404` enrollment/session not found, `409` already marked

### POST `/api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance`
Mark attendance for multiple students in a session.
- **Request:** `BulkAttendanceRequest`
  - `sessionId` (Long, required), `records[]` — each: `enrollmentId` (Long), `status` (enum), `notes` (String)
- **Response:** `List<AttendanceResponse>` (200)

### GET `/api/v1/attendance/{id}`
Get single attendance record.
- **Response:** `AttendanceResponse` (200)
- **Errors:** `404` not found

### GET `/api/v1/attendance/enrollment/{enrollmentId}`
Get attendance history for an enrollment. Paginated.
- **Params:** `page`, `size`, `sort`
- **Response:** `Page<AttendanceResponse>` (200)

### GET `/api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance`
Get all attendance records for a session. Paginated.
- **Response:** `Page<AttendanceResponse>` (200)

### GET `/api/v1/attendance/stats/student/{studentId}`
Get attendance statistics for a student across all classes.
- **Response:** `AttendanceStatsResponse` (200)

### GET `/api/v1/attendance/stats/class/{classId}`
Get attendance statistics for a class across all students.
- **Response:** `AttendanceStatsResponse` (200)

### PATCH `/api/v1/attendance/{id}`
Update attendance status (e.g., correct a mistake).
- **Request:** `UpdateAttendanceStatusRequest` — `status` (enum, required), `notes` (String)
- **Response:** `AttendanceResponse` (200)
- **Errors:** `404` not found

### DELETE `/api/v1/attendance/{id}`
Delete an attendance record.
- **Response:** `204 No Content`
- **Errors:** `404` not found

## DTOs

### AttendanceResponse
| Field | Type | Description |
|-------|------|-------------|
| id | Long | Attendance record ID |
| enrollmentId | Long | Enrollment reference |
| studentName | String | Denormalized student name |
| sessionId | Long | Class session reference |
| sessionNumber | Integer | Session sequence number |
| status | String | PRESENT, ABSENT, LATE, EXCUSED, MAKEUP |
| markedDate | LocalDate | Date attendance was marked |
| markedBy | Long | User ID who marked |
| markedByName | String | Denormalized marker name |
| notes | String | Optional notes |
| pointsAwarded | Integer | Gamification points |
| createdAt | Instant | Record creation time |
| updatedAt | Instant | Last update time |

### AttendanceStatsResponse
| Field | Type | Description |
|-------|------|-------------|
| targetId | Long | Student ID or Class ID |
| targetType | String | STUDENT or CLASS |
| totalSessions | Integer | Total sessions |
| presentCount | Integer | Present count |
| absentCount | Integer | Absent count |
| lateCount | Integer | Late count |
| excusedCount | Integer | Excused count |
| makeupCount | Integer | Makeup count |
| attendanceRate | Double | Percentage (0-100) |

## Cross-references
- **Use Cases:** UC-ATT-01 → UC-ATT-07
- **Business Rules:** BR-ATT-xxx (see `rules.md`)
