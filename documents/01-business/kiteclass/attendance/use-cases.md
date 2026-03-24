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
2. Teacher: Select student, choose status (PRESENT/LATE/ABSENT/EXCUSED_ABSENCE), add optional notes
3. System: Validate no duplicate record per BR-ATT-001
4. System: If EXCUSED_ABSENCE, require notes per BR-ATT-005
5. System: Check permission matrix per BR-ATT-006, BR-ATT-007
6. System: Save attendance record, recalculate attendance_rate per BR-ATT-008
7. FE: Toast success, update attendance list

**Postcondition:** Attendance record created, stats updated

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| 400 | EXCUSED_ABSENCE without notes | "Excused absence requires a note" |
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
