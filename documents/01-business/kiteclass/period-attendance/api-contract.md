---
title: Period Attendance — API Contract (Phase 1A read-only)
status: draft
created: 2026-05-04
updated: 2026-05-04
domain: kiteclass.period-attendance
gaps: [GAP-323]
---

# Period Attendance — API Contract (Phase 1A)

> Phase 1A surface only — read-only. Write/update/delete endpoints (POST/PATCH/
> DELETE) are reserved by GAP-323b and intentionally absent here.

Base path: `/api/v1/attendance/periods`

All endpoints require:
- `Authorization` header (resolved by gateway).
- `X-Tenant-Id` header (resolved + injected by tenant filter).

Tenant operating-model: only tenants with `vertical_type = 'K12_SCHOOL'` are
expected to populate this table; CENTER tenants will receive empty result
sets (no error). The vertical-type contract enforcement lands in GAP-323b.

## GET `/api/v1/attendance/periods/{id}`

Fetch a single period attendance row.

**Path parameters:**
| Name | Type | Required | Description |
|------|------|:--------:|-------------|
| `id` | `Long` | yes | Primary key |

**Responses:**
| Code | Body | When |
|------|------|------|
| 200 | [`AttendancePeriodResponse`](#attendanceperiodresponse) | Found |
| 404 | `{"error":"ATTENDANCE_PERIOD_NOT_FOUND","args":[id]}` | Not found / wrong tenant |

## GET `/api/v1/attendance/periods/students/{studentId}`

Page period attendance for one student in a date range.

**Path parameters:**
| Name | Type | Required |
|------|------|:--------:|
| `studentId` | `Long` | yes |

**Query parameters:**
| Name | Type | Required | Default |
|------|------|:--------:|---------|
| `from` | ISO-8601 date (`YYYY-MM-DD`) | yes | — |
| `to` | ISO-8601 date | yes | — |
| `page` | int | no | 0 |
| `size` | int | no | 50 |
| `sort` | string | no | `date,periodNo,desc` |

**Response:** 200 + `Page<AttendancePeriodResponse>`.

## GET `/api/v1/attendance/periods/classes/{classId}`

Daily roster — all (student × period) records for a class on one date.

**Path parameters:** `classId: Long`.
**Query parameters:** `date: ISO-8601 date` (required).
**Response:** 200 + `List<AttendancePeriodResponse>`.

## GET `/api/v1/attendance/periods/subject-sections/{subjectSectionId}`

Page period attendance for one SubjectSection (lớp bộ môn) across a date range.

**Path parameters:** `subjectSectionId: Long` — references
`subject_sections.id` (GAP-054 Phase 1).
**Query parameters:** `from`, `to` (required); pagination as above.
**Response:** 200 + `Page<AttendancePeriodResponse>`.

## Schemas

### `AttendancePeriodResponse`

```jsonc
{
  "id": 1234,
  "studentId": 101,
  "classId": 202,
  "subjectSectionId": 303,
  "periodNo": 1,
  "date": "2026-09-05",
  "status": "PRESENT",          // PRESENT | ABSENT | LATE | EXCUSED | MAKEUP
  "recordedBy": 404,            // user ID
  "recordedAt": "2026-09-05T07:05:00",
  "notes": null,
  "createdAt": "2026-09-05T07:05:00.123Z",
  "updatedAt": null
}
```

## Errors

| Code | HTTP | Description |
|------|------|-------------|
| `ATTENDANCE_PERIOD_NOT_FOUND` | 404 | i18n key resolved against `messages*.properties`. |

## Out-of-scope (deferred contracts)

- `POST /api/v1/attendance/periods` (single record write)
- `POST /api/v1/attendance/periods/bulk` (idempotent roster write)
- `PATCH /api/v1/attendance/periods/{id}` (edit within audit window)
- `DELETE /api/v1/attendance/periods/{id}` (soft delete, BGH-only)
- `GET /api/v1/attendance/periods/classes/{classId}/daily-rollup` (vắng cả
  ngày = vắng ≥7 tiết)

All deferred to GAP-323b.

## Log

- **2026-05-04** Phase 1A api-contract.md created (Wave 18b1 Bucket F).
