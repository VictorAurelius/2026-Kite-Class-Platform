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
- Caller is authenticated with a role in {TEACHER, STAFF, OWNER, ADMIN} (role gate, GAP-1300). The recording teacher (`recordedBy`) is derived from the gateway-injected `X-User-Reference-Id` (token), NOT a client header — the spoofable `X-Teacher-Id` was dropped (GAP-1300; gateway does not control it per GAP-814).
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

**FE behaviour (Phase 1B v1):** the GVCN mobile route at
`/attendance/period/{classId}/{periodNo}/{date}` (Wave 18b2 Bucket A,
`(teacher)` route group) renders a tap-grid + bulk-actions toolbar:

1. On load it seeds local state from the daily-roster fetch
   (`useDailyRoster`) for the current `(classId, date)`, filtering to rows
   whose `periodNo` matches the URL param.
2. The teacher taps one of the four status buttons per student
   (`PRESENT` / `EXCUSED` / `ABSENT` / `LATE`); local state updates
   optimistically without a network round-trip.
3. "Đánh dấu tất cả có mặt" sets every roster student to `PRESENT` in one
   click; "Xoá lựa chọn" clears local state back to "no entries".
4. "Lưu" calls `useUpsertAttendancePeriod`, which POSTs the batch; the
   recording teacher is taken from the authenticated principal
   (`X-User-Reference-Id`), not a client header (GAP-1300). On success, the
   matching daily-roster query is invalidated so the canonical server state
   is re-read.
5. `Save` is disabled until at least one student has a status set, AND a
   `subjectSectionId` is known (read from the existing roster rows for the
   class+date — Phase 1B follow-up will let the teacher pick the section
   explicitly when the day's first tiết has no prior rows).

`MAKEUP` (học bù) is intentionally NOT in the v1 tap-grid — it is a
correction action that lives in the future detail dialog
(UC-PERIOD-ATT-W-002). Offline queue + Playwright ≤2-min perf assertion +
multi-period quick-switch + "inherit from previous period" all stay
deferred per UC-PERIOD-ATT-UI-002 below + GAP-323b follow-up.

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
GVCN can resolve concurrent-edit collision. Phase 1B v1 ships the API
client wrapper (`attendancePeriodApi.updateOne`) but NOT the dedicated
hook + dialog — the tap-grid currently treats every change as a batch
upsert (UC-PERIOD-ATT-W-001), which the backend handles idempotently. The
single-row PATCH path activates when the merge-dialog UX lands as a
Phase 1B follow-up.

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

## UC-PERIOD-ATT-UI-001 — Mobile tap-grid UI happy path (Phase 1B v1)

**Actor:** GVCN / GV bộ môn on a mobile device.

**Pre-conditions:**
- Caller is authenticated; `useAuthStore.user.id` resolves to a teacher ID.
- URL is `/attendance/period/{classId}/{periodNo}/{date}` with
  `periodNo ∈ 1..10` and `date` matching `YYYY-MM-DD`.
- Existing roster rows for `(classId, date)` exist OR have at least one
  `subjectSectionId` reachable (Phase 1B v1 reads it from the roster — a
  truly empty class+date will show an empty-state until Phase 1B follow-up
  adds an explicit section picker).

**Steps:**
1. Page loads → `useDailyRoster(classId, date)` fetches the daily roster.
2. Page seeds local optimistic state from the rows whose `periodNo` matches
   the URL param (one tap-grid row per unique `studentId`).
3. Teacher taps `Có mặt | Có phép | Vắng | Trễ` per student; the active
   button shows `aria-pressed="true"`.
4. Optionally: teacher hits "Đánh dấu tất cả có mặt" → every roster student
   → `PRESENT`; or "Xoá lựa chọn" → reset.
5. Teacher hits "Lưu" → page issues POST batch via
   `useUpsertAttendancePeriod` → toast "Đã lưu điểm danh".

**Post-conditions:** Server state matches local state for the
`(classId, date, periodNo)` slice; daily-roster cache invalidated.

**Errors:**
- Invalid URL params → static error page ("Tham số URL không hợp lệ").
- Auth missing → static error page ("Cần đăng nhập"). Layout-level
  redirect to `/login` already covers token expiry.
- POST batch fails → toast with backend message; local state preserved so
  the teacher can retry without re-entering.

## UC-PERIOD-ATT-UI-002..NNN — Offline queue (deferred GAP-323b follow-up)

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

- **2026-05-04** (Phase 1B v1 mobile UI) Wave 18b2 Bucket A —
  promoted UC-PERIOD-ATT-UI-001 from placeholder to a full UC describing
  the GVCN tap-grid happy path against the route
  `/attendance/period/{classId}/{periodNo}/{date}` ((teacher) route
  group). Extended UC-PERIOD-ATT-W-001 + UC-PERIOD-ATT-W-002 with
  Phase 1B v1 FE behaviour subsections pointing at the route shell +
  hooks + components. Offline queue stays under UC-PERIOD-ATT-UI-002 as
  a deferred placeholder.
- **2026-05-04** (Phase 1B v1) Wave 18b2 first PR — added UC-PERIOD-ATT-W-001
  (idempotent batch upsert), UC-PERIOD-ATT-W-002 (single-row PATCH with
  optimistic lock), UC-PERIOD-ATT-R-005 (on-demand daily roll-up). Mobile UI
  + offline queue placeholders parked under UC-PERIOD-ATT-UI-* per GAP-323b
  §1B.2/§1B.3.
- **2026-05-04** Phase 1A use-cases.md created (Wave 18b1 Bucket F).
