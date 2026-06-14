# Attendance — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## Use Cases

### UC-ATT-01: Mark Single Attendance

**Actor:** Teacher (MAIN_TEACHER / ASSISTANT) / Admin
**Precondition:** Class session exists, student enrolled in class

**Steps:**
1. FE: Display session attendance form with enrolled students list
2. Teacher: Select student, choose status (PRESENT/LATE/ABSENT/EXCUSED), add optional notes
3. System: Validate no duplicate record per BR-ATT-001
4. System: If EXCUSED, require notes per BR-ATT-005
5. System: Check permission matrix per BR-ATT-006, BR-ATT-007
6. System: Save attendance record, recalculate attendance_rate per BR-ATT-008
7. FE: Toast success, update attendance list

**Postcondition:** Attendance record created, stats updated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | EXCUSED without notes | "Excused absence requires a note" |
| 404 | Enrollment not found | "Enrollment not found" |
| 409 | Duplicate student+session | "Attendance already recorded for this session" |

---

### UC-ATT-02: Mark Bulk Attendance

**Actor:** Teacher / Admin
**Precondition:** Class session exists with enrolled students

**Steps:**
1. FE: Display session with all enrolled students in a grid
2. Teacher: Set status for each student in bulk, submit BulkAttendanceRequest
3. System: Validate each record per BR-ATT-001, BR-ATT-005
4. System: Save all records in batch, recalculate rates per BR-ATT-008
5. FE: Toast success with count of records saved

**Postcondition:** All attendance records for session created

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | Empty records list | "At least one attendance record required" |
| 409 | Any duplicate in batch | "Duplicate attendance record in batch" |

---

### UC-ATT-03: QR Code Check-in

**Actor:** Student (via QR scan)
**Precondition:** Teacher generated QR code for active session

**Steps:**
1. Student: Scan QR code linked to session
2. System: Record check-in timestamp
3. System: Auto-determine status per BR-ATT-002, BR-ATT-003, BR-ATT-004:
   - Within 5 min -> PRESENT
   - Within 15 min -> LATE
   - Beyond 15 min -> ABSENT
4. System: Save attendance record per BR-ATT-001
5. FE: Display check-in result with status

**Postcondition:** Attendance recorded with time-based status

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | QR code expired | "Session QR code has expired" |
| 409 | Already checked in | "Attendance already recorded for this session" |

---

### UC-ATT-04: Update Attendance Status

**Actor:** Teacher (MAIN_TEACHER) / Admin
**Precondition:** Attendance record exists

**Steps:**
1. FE: Display current attendance record with status
2. Teacher: Select new status, add override reason in notes
3. System: Validate permission — only MAIN_TEACHER or ADMIN per BR-ATT-006
4. System: Update status, recalculate attendance_rate per BR-ATT-008
5. FE: Toast success, refresh record

**Postcondition:** Attendance status overridden

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 403 | ASSISTANT attempting override | "Only main teacher or admin can override" |
| 404 | Record not found | "Attendance record not found" |

---

### UC-ATT-05: View Student Attendance Stats

**Actor:** Teacher / Admin / Student (own records)
**Precondition:** Student has attendance records

**Steps:**
1. FE: Request stats via GET /stats/student/{studentId}
2. System: Calculate totals per status, attendance_rate per BR-ATT-008
3. System: Filter by instance_id per BR-ATT-009
4. FE: Display stats dashboard (total sessions, present, late, absent, rate %)

**Postcondition:** Stats displayed

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Student not found | "Student not found" |

---

### UC-ATT-06: View Class Attendance Stats

**Actor:** Teacher / Admin
**Precondition:** Class has sessions with attendance records

**Steps:**
1. FE: Request stats via GET /stats/class/{classId}
2. System: Aggregate attendance across all sessions per BR-ATT-008
3. FE: Display class-level report (average rate, per-student breakdown)

**Postcondition:** Class attendance report displayed

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 404 | Class not found | "Class not found" |

---

### UC-ATT-07: Delete Attendance Record

**Actor:** Admin
**Precondition:** Attendance record exists

**Steps:**
1. Admin: Select record to delete
2. System: Validate admin permission per BR-ATT-007
3. System: Delete record, recalculate stats per BR-ATT-008
4. FE: Toast success, remove from list

**Postcondition:** Record deleted, stats recalculated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 403 | Non-admin attempt | "Only admin can delete attendance records" |
| 404 | Record not found | "Attendance record not found" |

---

### UC-ATT-08: Calendar View of Attendance

**Actor:** Teacher / Admin / Student (own records)
**Precondition:** Student (or class) has attendance records spanning a time range

**Steps:**
1. FE: Render `<AttendanceCalendar>` / `<EnhancedAttendanceCalendar>` — month grid with per-day status
2. User: Pick date range (week / month / semester), optional filter by status
3. System: `GET /attendance?studentId=X&from=...&to=...` (or class variant)
4. System: Filter by `instance_id` per BR-ATT-009
5. FE: Render each day as color-coded cell:
   - Green — PRESENT
   - Yellow — LATE
   - Red — ABSENT
   - Blue — EXCUSED
   - Grey — no session / weekend / holiday
6. User: Click cell → `AttendanceDetailDialog` with session details + note + override action

**Components (already exist):**
- `kiteclass-frontend/src/components/attendance/attendance-calendar.tsx`
- `kiteclass-frontend/src/components/attendance/enhanced-attendance-calendar.tsx`
- `kiteclass-frontend/src/components/attendance/attendance-detail-dialog.tsx`

**Postcondition:** Visual overview of attendance; drill-down available

**Errors:** same as UC-ATT-05 / UC-ATT-06 (student/class not found)

**Notes:**
- Weekends + holidays respect Holiday table (Wave 2 GAP-053)
- Calendar respects academic year boundaries from AcademicYear entity

### UC-ATT-09: Class-overview batch save (GAP-268a)

**Actor:** Teacher / GVCN
**Precondition:** Teacher có quyền truy cập lớp; class có students enrolled; subject_section đã được lập lịch cho ngày đó.

**Steps:**
1. Teacher mở UI `(teacher)/teacher/attendance/[classId]` overview screen
2. UI hiển thị grid 1-10 tiết × N students với pre-loaded current state (hoặc trống nếu chưa có data)
3. Teacher chỉnh từng cell (PRESENT / ABSENT / LATE / EXCUSED / MAKEUP) + optional notes
4. Teacher click "Lưu" → FE gọi `POST /api/v1/attendance/class/{classId}/batch?date=YYYY-MM-DD` với array of `{studentId, subjectSectionId, periodNo, status, notes}` cells
5. BE service `AttendancePeriodService.upsertClassBatch` folds (classId, date) vào từng entry → forwards tới existing `upsertBatch` path
6. Per-row idempotent upsert: existing rows → update (status/notes/recordedBy/recordedAt), new tuples → insert
7. Response 201 + `List<AttendancePeriodResponse>` (entry order preserved) — FE refresh state

**Postcondition:**
- Mỗi (student × subjectSection × date × periodNo) tuple có exactly 1 row in `attendance_period`
- Resubmit cùng body → cùng state, KHÔNG duplicate rows (DB unique index V50 backstop)

**Errors:**
- `400` entries empty / batch > 200 cells / periodNo ngoài 1..10
- `403` caller role không thuộc {TEACHER, STAFF, OWNER, ADMIN} — role gate GAP-1300 (định danh giáo viên ghi lấy từ token `X-User-Reference-Id`, không còn header client `X-Teacher-Id` spoofable)
- `409 OPTIMISTIC_LOCK_CONFLICT` nếu concurrent edit beats request (rare — overview save is single-teacher path)

**Notes:**
- Wave 51 Bucket B (GAP-268a) thêm endpoint mới; per-tiết save (`POST /api/v1/attendance/periods`) vẫn được giữ nguyên cho path "mark từng tiết riêng lẻ"
- Outbox emission deferred (consistent với existing `upsertBatch` path)
- 200-cell cap = 10 tiết × 20 students leaves headroom cho combined bộ môn classes
