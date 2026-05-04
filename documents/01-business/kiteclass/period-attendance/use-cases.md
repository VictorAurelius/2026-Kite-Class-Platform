---
title: Period Attendance — Use Cases (Phase 1A read + Phase 1B write/rollup v1)
status: draft
created: 2026-05-04
updated: 2026-05-04
domain: kiteclass.period-attendance
gaps: [GAP-323, GAP-323b]
---

# Period Attendance — Use Cases

> Phase 1A read paths shipped Wave 18b1. Phase 1B v1 (Wave 18b2 first PR) adds
> idempotent batch write, optimistic-lock update, and on-demand daily roll-up.
> Mobile UI / offline queue (UC-PERIOD-ATT-UI*) and grade-publishing
> (UC-PERIOD-GRADE-*) remain deferred per `rules.md` §4.

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

## UC-PERIOD-ATT-W-001 — GVCN bulk record a tiết roster (Phase 1B v1)

**Actor:** GV bộ môn (per-tiết) or GVCN (tiết 1 of the day).

**Pre-conditions:**
- Tenant `vertical_type = 'K12_SCHOOL'`.
- Caller has resolved `X-Teacher-Id` (gateway/auth maps to user ID).
- Each entry's `(studentId, classId, subjectSectionId)` is consistent with the
  tenant's K-12 structure (FK validation deferred to GAP-323b follow-up;
  service trusts the body in v1).

**Steps:**
1. Caller issues `POST /api/v1/attendance/periods` with up to 60 entries
   (one period × ≤42 students or any subset).
2. Service iterates entries. For each:
   - Look up by V50 unique tuple (studentId, subjectSectionId, date,
     periodNo) within the tenant.
   - Found → update `status`, `notes`, `recordedBy`, `recordedAt`,
     `classId`.
   - Not found → insert new row with the tenant's instanceId.
3. Returns `201` + `List<AttendancePeriodResponse>` in request order.

**Idempotency:** resubmitting the same payload yields the same final state
(BR-PERIOD-ATT-008). The DB unique index is the backstop.

**Post-conditions:** ≥0 rows inserted, ≥0 rows updated. No duplicates.

**FE behaviour (v1, pre mobile UI):** any client posting JSON to this
endpoint receives a deterministic outcome. The dedicated GVCN tap-grid mobile
UI ships in a follow-up PR (UC-PERIOD-ATT-UI-001 stub below).

## UC-PERIOD-ATT-W-002 — Edit a single recorded period (Phase 1B v1)

**Actor:** GV bộ môn / GVCN correcting a recorded entry.

**Pre-conditions:**
- Caller has read the row first and holds its `version`.
- The row is not soft-deleted.

**Steps:**
1. Caller issues `PATCH /api/v1/attendance/periods/{id}` with body
   `{status, notes?, version}`.
2. Service fetches the row, asserts `version` matches, applies new
   `status`/`notes`, refreshes `recordedBy`/`recordedAt`.
3. Stale `version` → 409 + `OPTIMISTIC_LOCK_CONFLICT`. Missing row → 404.

**FE behaviour:** detail dialog re-fetches on 409, shows merge dialog so the
GVCN can resolve concurrent-edit collision.

## UC-PERIOD-ATT-R-005 — Daily roll-up across one class (Phase 1B v1)

**Actor:** GVCN dashboard, BGH report screen.

**Pre-conditions:** Same as R-001; caller has class-level read permission.

**Steps:**
1. Caller issues
   `GET /api/v1/attendance/periods/daily-rollup?classId=X&from=Y&to=Z`.
2. Service runs the SQL aggregation and returns one row per (student, date)
   with counts + `allDayAbsent` flag.

**Post-conditions:** No state change.

**FE behaviour:** GVCN dashboard renders a per-day summary; rows where
`allDayAbsent=true` are highlighted (vắng cả ngày per TT 22/2021).

## UC-PERIOD-ATT-UI-001..NNN — Mobile tap-grid UI (deferred GAP-323b §1B.2)

Placeholder for: 42-student × 4-button tap-grid, ≤2 min Playwright perf
target, "Mark all present" + "Reset" bulk actions, "Inherit from previous
period" delta entry.

## UC-PERIOD-ATT-UI-002..NNN — Offline queue (deferred GAP-323b §1B.3)

Placeholder for: queue submissions when offline, retry on reconnect, surface
"pending submissions" badge.

## UC-PERIOD-GRADE-001..NNN — Grade publishing (deferred GAP-323c)

Placeholder for: ĐTBmHK formula, DRAFT → REVIEWED → PUBLISHED state machine,
Tổ trưởng approval, học bạ generation.

## Errors

| Code | HTTP | When |
|------|------|------|
| `ATTENDANCE_PERIOD_NOT_FOUND` | 404 | UC-PERIOD-ATT-R-001 / UC-PERIOD-ATT-W-002 cannot resolve `id` for the current tenant. |
| `OPTIMISTIC_LOCK_CONFLICT` | 409 | UC-PERIOD-ATT-W-002 client passed a stale `version`. |
| `VALIDATION_ERROR` | 400 | UC-PERIOD-ATT-W-001 entry violates DTO constraints (e.g., `periodNo` outside 1..10, batch >60, missing required field). |
| (Generic 400) | 400 | `from > to` date range; invalid pagination parameters. |
| (Generic 401/403) | 401/403 | Auth or RBAC failure (handled upstream by gateway + tenant filter). |

## Log

- **2026-05-04** (Phase 1B v1) Wave 18b2 first PR — added UC-PERIOD-ATT-W-001
  (idempotent batch upsert), UC-PERIOD-ATT-W-002 (single-row PATCH with
  optimistic lock), UC-PERIOD-ATT-R-005 (on-demand daily roll-up). Mobile UI
  + offline queue placeholders parked under UC-PERIOD-ATT-UI-* per GAP-323b
  §1B.2/§1B.3.
- **2026-05-04** Phase 1A use-cases.md created (Wave 18b1 Bucket F).
