# K-12 Multi-Subject Model — API Contract

**Base path:** `/api/v1/homeroom-classes`, `/api/v1/subject-sections`, `/api/v1/subject-grades`, `/api/v1/curricula`

## POST /api/v1/homeroom-classes
**Request:**
```json
{
  "academicYearId": 1,
  "grade": "10",
  "section": "A1",
  "capacity": 40,
  "homeroomTeacherId": 5
}
```
**Response 201:** HomeroomClass + fullName "10A1"

## GET /api/v1/homeroom-classes?academicYearId=1
List homeroom classes in year.

## PATCH /api/v1/homeroom-classes/{id}/homeroom-teacher
**Request:** `{ "teacherId": 10 }`

## POST /api/v1/homeroom-classes/{id}/enroll
Increments currentEnrolled (called by enrollment service).
Error 409 if full.

## POST /api/v1/subject-sections
**Request:**
```json
{
  "homeroomClassId": 1,
  "courseId": 12,
  "teacherId": 5,
  "schedule": "T2,T4,T6 07:00-07:45",
  "weeklyHours": 4
}
```

## POST /api/v1/subject-grades
**Request:**
```json
{
  "studentId": 100,
  "subjectSectionId": 5,
  "semesterId": 2,
  "regularScore": 8.0,
  "midtermScore": 7.5,
  "finalScore": 9.0
}
```
**Response:** với auto-computed average + letterGrade.

## GET /api/v1/subject-grades?studentId=100&semesterId=2
Returns list of grades for transcript view.

## Log
- 2026-04-14 — Initial API
