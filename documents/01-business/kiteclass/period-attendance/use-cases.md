---
title: Period Attendance — Use Cases (Phase 1A read-only)
status: draft
created: 2026-05-04
updated: 2026-05-04
domain: kiteclass.period-attendance
gaps: [GAP-323]
---

# Period Attendance — Use Cases (Phase 1A)

> Phase 1A is read-only. Write-path use cases (UC-PERIOD-ATT-W*) are placeholders
> tracked in GAP-323b. GVCN mobile UI use cases (UC-PERIOD-ATT-UI*) are tracked
> in GAP-323b. Grade-publishing use cases (UC-PERIOD-GRADE-*) are tracked in
> GAP-323c.

## UC-PERIOD-ATT-R-001 — Lookup a single period attendance record

**Actor:** Authenticated KiteClass user (GVCN, bộ môn, BGH, parent via portal
on-behalf flow GAP-321).

**Pre-conditions:**
- Tenant `vertical_type = 'K12_SCHOOL'`.
- Caller is authorized to view the record (RBAC enforced at gateway + service
  layer; Phase 1A relies on existing tenant-filter; finer RBAC ships GAP-323b).

**Steps:**
1. Caller issues `GET /api/v1/attendance/periods/{id}`.
2. Service resolves `id` against `attendance_period` filtered by tenant.
3. If not found → 404 + `ATTENDANCE_PERIOD_NOT_FOUND`.
4. Otherwise → 200 + `AttendancePeriodResponse`.

**Post-conditions:** No state change.

**FE behaviour:** Detail dialog shows status, recorded_by, recorded_at, notes.

## UC-PERIOD-ATT-R-002 — Student attendance history for parent portal

**Actor:** Parent via portal feed (GAP-321) OR student.

**Pre-conditions:** Same as R-001.

**Steps:**
1. Caller issues
   `GET /api/v1/attendance/periods/students/{studentId}?from=YYYY-MM-DD&to=YYYY-MM-DD`.
2. Service pages records, default sort by `(date desc, periodNo desc)`.
3. Returns `Page<AttendancePeriodResponse>`.

**Post-conditions:** No state change.

**FE behaviour:** Parent portal "Điểm danh" tab renders calendar view with
per-tiết drill-down (Phase 1A returns raw rows; daily roll-up in GAP-323b).

## UC-PERIOD-ATT-R-003 — Class daily roster (BGH / GVCN view)

**Actor:** BGH (Ban Giám hiệu), GVCN, MIS staff.

**Pre-conditions:** Same as R-001; caller has class-level read permission.

**Steps:**
1. Caller issues
   `GET /api/v1/attendance/periods/classes/{classId}?date=YYYY-MM-DD`.
2. Service returns the full list of period records for the class on that date.

**Post-conditions:** No state change.

**FE behaviour:** Daily roster table with rows = students, columns = period_no,
cells = status code (P/V/T/CP/HB). Daily roll-up (vắng cả ngày = vắng ≥7
tiết) is computed client-side as a Phase 1A interim; server-side view ships
GAP-323b.

## UC-PERIOD-ATT-R-004 — Bộ môn (subject teacher) review

**Actor:** GV bộ môn reviewing their own SubjectSection across a date range.

**Pre-conditions:** Caller is the SubjectSection's `teacherId` OR Tổ trưởng of
the subject area (GAP-323c).

**Steps:**
1. Caller issues
   `GET /api/v1/attendance/periods/subject-sections/{subjectSectionId}?from=…&to=…`.
2. Service pages records for that SubjectSection in the date window.

**Post-conditions:** No state change.

**FE behaviour:** Subject teacher dashboard shows attendance trend and
pinpoints tiết with anomaly status counts. Phase 1A returns raw rows.

## UC-PERIOD-ATT-W-001..NNN — Write paths (deferred GAP-323b)

Placeholder for: record one period, bulk-record a roster, edit recorded
period within audit window, soft-delete (only Tổ trưởng / BGH).

## UC-PERIOD-GRADE-001..NNN — Grade publishing (deferred GAP-323c)

Placeholder for: ĐTBmHK formula, DRAFT → REVIEWED → PUBLISHED state machine,
Tổ trưởng approval, học bạ generation.

## Errors

| Code | HTTP | When |
|------|------|------|
| `ATTENDANCE_PERIOD_NOT_FOUND` | 404 | UC-PERIOD-ATT-R-001 cannot resolve `id` for the current tenant. |
| (Generic 400) | 400 | `from > to` date range; invalid pagination parameters. |
| (Generic 401/403) | 401/403 | Auth or RBAC failure (handled upstream by gateway + tenant filter). |

## Log

- **2026-05-04** Phase 1A use-cases.md created (Wave 18b1 Bucket F).
