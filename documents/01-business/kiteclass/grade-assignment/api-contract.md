# Grade & Assignment — API Contract

> Extracted from: `AssignmentController`, `GradeController`, related DTOs
> Base paths: `/api/v1/assignments`, `/api/v1/grades`

## Assignment Endpoints

### POST `/api/v1/assignments`
Create a new assignment for a class.
- **Request:** `CreateAssignmentRequest`
  - `classId` (Long, required), `title` (String, required), `description` (String), `instructions` (String), `dueDate` (Instant), `maxScore` (Double, required), `weightPercent` (Double), `allowLateSubmission` (Boolean), `latePenaltyPercent` (Double)
- **Response:** `ApiResponse<AssignmentResponse>` (201)

### PUT `/api/v1/assignments/{id}`
Update assignment details (only DRAFT status).
- **Response:** `ApiResponse<AssignmentResponse>` (200)

### POST `/api/v1/assignments/{id}/publish` | `/{id}/close`
Change assignment lifecycle status.
- **Response:** `ApiResponse<AssignmentResponse>` (200)

### DELETE `/api/v1/assignments/{id}`
Delete assignment (only DRAFT). **Response:** `ApiResponse<Void>` (200)

### GET `/api/v1/assignments/{id}` | `/class/{classId}` | `/class/{classId}/published`
Retrieve assignments. Class endpoints return `ApiResponse<List<AssignmentResponse>>`.

### POST `/api/v1/assignments/submit`
Student submits work for an assignment.
- **Request:** `SubmitAssignmentRequest` — `assignmentId` (Long), `contentUrl` (String), `notes` (String)
- **Response:** `ApiResponse<SubmissionResponse>` (201)

### POST `/api/v1/assignments/submissions/{id}/grade`
Teacher grades a submission.
- **Request:** `GradeSubmissionRequest` — `score` (Double, required), `feedback` (String)
- **Response:** `ApiResponse<SubmissionResponse>` (200)

### POST `/api/v1/assignments/submissions/{id}/return`
Return submission to student for revision. **Response:** `ApiResponse<SubmissionResponse>` (200)

### GET `/api/v1/assignments/submissions/{id}` | `/{assignmentId}/submissions` | `/{assignmentId}/submissions/student/{studentId}` | `/submissions/student/{studentId}` | `/class/{classId}/pending-grading`
Query submissions. Pending-grading returns ungraded submissions for a class.

## Grade Endpoints

### POST `/api/v1/grades/initialize`
Initialize grade record for a student in a class.
- **Params:** `studentId` (Long), `classId` (Long)
- **Response:** `ApiResponse<GradeResponse>` (201)

### GET `/api/v1/grades/{id}` | `/student/{studentId}/class/{classId}` | `/student/{studentId}` | `/class/{classId}`
Query grades. Student/class endpoints return lists.

### POST `/api/v1/grades/components`
Add or update a grade component.
- **Request:** `CreateGradeComponentRequest` — `gradeId` (Long), `componentType` (String), `componentName` (String), `componentRefId` (Long), `score` (Double), `maxScore` (Double), `weightPercent` (Double)
- **Response:** `ApiResponse<GradeComponentResponse>` (200)

### PUT `/api/v1/grades/components/{id}` | DELETE `/api/v1/grades/components/{id}`
Update or delete a grade component.

### POST `/api/v1/grades/{id}/calculate`
Recalculate final score from components. **Response:** `ApiResponse<GradeResponse>` (200)

### POST `/api/v1/grades/{id}/finalize`
Lock grade as final.
- **Request:** `FinalizeGradeRequest` — `teacherId` (Long, required), `comments` (String)
- **Response:** `ApiResponse<GradeResponse>` (200)

### POST `/api/v1/grades/{id}/unfinalize`
Reopen a finalized grade. **Response:** `ApiResponse<GradeResponse>` (200)

### POST `/api/v1/grades/transcripts/generate`
Generate transcript for a student/semester.
- **Params:** `studentId` (Long), `semester` (String)
- **Response:** `ApiResponse<TranscriptResponse>` (200)

### GET `/api/v1/grades/transcripts/student/{studentId}/semester/{semester}` | `/transcripts/student/{studentId}`
Query transcripts. All-semesters endpoint returns list.

### GET `/api/v1/grades/class/{classId}/statistics`
Class-level grade statistics summary.

## Key DTOs

### AssignmentResponse
`id`, `classId`, `title`, `description`, `instructions`, `dueDate`, `maxScore`, `weightPercent`, `allowLateSubmission`, `latePenaltyPercent`, `status` (DRAFT/PUBLISHED/CLOSED), `createdBy`, `createdAt`, `updatedAt`, `isOverdue` (computed), `isAcceptingSubmissions` (computed)

### SubmissionResponse
`id`, `assignmentId`, `studentId`, `submissionDate`, `contentUrl`, `notes`, `score`, `adjustedScore`, `status` (SUBMITTED/GRADED/RETURNED), `gradedBy`, `gradedAt`, `feedback`, `createdAt`, `isLate` (computed), `penaltyApplied` (computed)

### GradeResponse
`id`, `studentId`, `classId`, `finalScore`, `letterGrade`, `gpa`, `status` (IN_PROGRESS/FINALIZED), `passThreshold`, `comments`, `calculatedAt`, `finalizedAt`, `finalizedBy`, `createdAt`, `updatedAt`, `components[]`, `isFinalized`, `isPassed`, `isFailed`, `totalWeight`, `isWeightValid` (computed)

### TranscriptResponse
`id`, `studentId`, `semester`, `academicYear`, `totalCredits`, `semesterGpa`, `cumulativeGpa`, `totalCourses`, `passedCourses`, `failedCourses`, `grades[]`, `studentName`, `studentEmail`

## Cross-references
- **Use Cases:** UC-GRD-01 → UC-GRD-09
- **Business Rules:** BR-GRD-xxx (see `rules.md`)
