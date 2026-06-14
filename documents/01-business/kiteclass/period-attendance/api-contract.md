---
title: Period Attendance — API Contract (Phase 1A read + Phase 1B write/rollup v1)
status: draft
created: 2026-05-04
updated: 2026-05-04
domain: kiteclass.period-attendance
gaps: [GAP-323, GAP-323b]
---

# Period Attendance — API Contract

> Phase 1A (read-only) shipped Wave 18b1. Phase 1B v1 (Wave 18b2 first PR)
> adds idempotent batch upsert, optimistic-lock PATCH, and an on-demand daily
> roll-up endpoint. Mobile UI / offline queue / matview / load-test rig
> remain deferred per `rules.md` §4.

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

## POST `/api/v1/attendance/periods`  *(Phase 1B, GAP-323b)*

Idempotent batch upsert. The server looks up each entry by the V50 unique
tuple `(studentId, subjectSectionId, date, periodNo)` within the current
tenant and either updates or inserts. Resubmitting the same payload yields
the same final state — duplicates are impossible at the DB and service
layers (BR-PERIOD-ATT-008).

**Headers:**
| Name | Type | Required |
|------|------|:--------:|
| `X-Tenant-Id` | UUID | yes |
| `X-User-Reference-Id` | Long | yes — token-derived recording teacher; populates `recorded_by` (GAP-1300, replaces spoofable `X-Teacher-Id`) |

**Body:** [`AttendancePeriodBatchCreateRequest`](#attendanceperiodbatchcreaterequest).

**Responses:**
| Code | Body | When |
|------|------|------|
| 201 | `List<AttendancePeriodResponse>` | Upsert succeeded; same order as request |
| 400 | validation error | Invalid `periodNo` (outside 1..10), missing required field, batch empty / >60 entries |

## PATCH `/api/v1/attendance/periods/{id}`  *(Phase 1B, GAP-323b)*

Update status / notes on a single row with optimistic-lock check
(BR-PERIOD-ATT-009). Client must pass the `version` they read.

**Path parameter:** `id: Long`.

**Headers:** as POST above.

**Body:** [`AttendancePeriodUpdateRequest`](#attendanceperiodupdaterequest).

**Responses:**
| Code | Body | When |
|------|------|------|
| 200 | `AttendancePeriodResponse` | Updated |
| 404 | `{"error":"ATTENDANCE_PERIOD_NOT_FOUND",...}` | Row missing or soft-deleted |
| 409 | `{"error":"OPTIMISTIC_LOCK_CONFLICT",...}` | Stale `version` in body |

## GET `/api/v1/attendance/periods/daily-rollup`  *(Phase 1B v1, GAP-323b)*

On-demand per-(student, date) roll-up across one class for a date range.
Phase 1B v1 implements via SQL aggregation; the matview path is deferred
(BR-PERIOD-ATT-010).

**Query parameters:**
| Name | Type | Required |
|------|------|:--------:|
| `classId` | Long | yes |
| `from` | ISO-8601 date | yes |
| `to` | ISO-8601 date | yes |

**Response:** 200 + `List<DailyAttendanceRollupResponse>`.

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

### `AttendancePeriodBatchCreateRequest`

```jsonc
{
  "entries": [
    {
      "studentId": 101,
      "classId": 202,
      "subjectSectionId": 303,
      "periodNo": 2,        // 1..10 (V51 + DTO)
      "date": "2026-09-05",
      "status": "PRESENT",
      "notes": null
    }
    // ... ≤60 entries per batch
  ]
}
```

### `AttendancePeriodUpdateRequest`

```jsonc
{
  "status": "EXCUSED",
  "notes": "ốm",
  "version": 3            // current version of the row (optimistic lock)
}
```

### `DailyAttendanceRollupResponse`

```jsonc
{
  "studentId": 101,
  "classId": 202,
  "date": "2026-09-05",
  "periodCount": 7,
  "presentCount": 5,
  "absentCount": 1,
  "lateCount": 1,
  "excusedCount": 0,
  "makeupCount": 0,
  "allDayAbsent": false   // (absent + late) >= 7 per TT 22/2021
}
```

## Errors

| Code | HTTP | Description |
|------|------|-------------|
| `ATTENDANCE_PERIOD_NOT_FOUND` | 404 | i18n key resolved against `messages*.properties`. |
| `OPTIMISTIC_LOCK_CONFLICT` | 409 | Stale `version` on PATCH; refresh + retry. |
| `VALIDATION_ERROR` | 400 | Bean-Validation failure (e.g., `periodNo` outside 1..10, batch >60). |

## Out-of-scope (deferred contracts)

- `DELETE /api/v1/attendance/periods/{id}` (soft delete, BGH-only)
- Per-tenant audit-window override on backdated edits (BR-PERIOD-ATT-007)
- Materialized-view variant of `/daily-rollup` with debounced refresh
- Subject-section-bound RBAC on POST/PATCH

All tracked under GAP-323b follow-up PRs.

## Log

- **2026-05-04** (Phase 1B v1) Wave 18b2 first PR — added POST batch upsert,
  PATCH with optimistic lock, GET daily-rollup. New schemas:
  `AttendancePeriodBatchCreateRequest`, `AttendancePeriodUpdateRequest`,
  `DailyAttendanceRollupResponse`. New error code:
  `OPTIMISTIC_LOCK_CONFLICT`.
- **2026-05-04** Phase 1A api-contract.md created (Wave 18b1 Bucket F).
