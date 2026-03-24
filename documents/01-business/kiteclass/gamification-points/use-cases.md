# Gamification Points — Use Cases

> Last verified: 2026-03-24 | Source: `kiteclass-core/module/gamification/`

## Use Cases

### UC-GAM-01: Award Points on Attendance Mark

**Actor:** System (triggered by AttendanceService)
**Precondition:** Teacher marks attendance for a student in a class session.

**Steps:**
1. Teacher marks attendance with status (PRESENT/LATE/ABSENT/EXCUSED/MAKEUP)
2. AttendanceService saves attendance record with `pointsAwarded` from `AttendanceStatus.getPointsDeduction()`
3. System calls `pointService.awardAttendancePoints(studentId, attendanceId, points, description)`
4. System validates per GP-01..GP-05: PRESENT=0, LATE=-5, ABSENT=-10, EXCUSED=0, MAKEUP=0
5. System creates StudentPoint record with:
   - `instance_id` = current tenant (per GP-07)
   - `reference_type` = "ATTENDANCE" (per GP-08)
   - `reference_id` = attendanceId (per GP-09)
   - `earned_at` = now
6. System saves record to `student_points` table

**Postcondition:** One StudentPoint record exists linking to the attendance record (per GP-10).

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| — | No direct errors | Points are always created; values come from enum |

---

### UC-GAM-02: Update Points on Attendance Status Change

**Actor:** System (triggered by AttendanceService)
**Precondition:** Attendance record already exists with a linked StudentPoint.

**Steps:**
1. Teacher updates an existing attendance status (e.g., ABSENT -> EXCUSED)
2. AttendanceService calls `pointService.updateAttendancePoints(studentId, attendanceId, newPoints, description)`
3. System finds existing StudentPoint by `reference_type=ATTENDANCE` and `reference_id=attendanceId`
4. System deletes old StudentPoint record (per GP-14)
5. System creates new StudentPoint with updated points value (per GP-14)
6. System saves new record

**Postcondition:** Old point record deleted, new record with updated points exists. One point per attendance maintained (per GP-10).

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| — | No existing point found | System creates new record (no error thrown) |

---

### UC-GAM-03: Get Student Total Points

**Actor:** Teacher / Admin / System
**Precondition:** Student exists in the system.

**Steps:**
1. Caller requests total points for a student
2. System calls `pointService.getTotalPoints(studentId)`
3. System executes: `SELECT COALESCE(SUM(points), 0) FROM student_points WHERE student_id = ?` (per GP-11)
4. System returns integer total (can be negative)

**Postcondition:** Total points returned. Returns 0 if no point records exist.

**Errors:**
| Code | Condition | Message |
|------|-----------|---------|
| — | No points found | Returns 0 (COALESCE handles null) |

---

## Notes

- **No direct API endpoint** — Points are exclusively triggered by AttendanceService internally (per GP-13)
- **Reference types** GRADE and ASSIGNMENT defined in enum but not yet implemented (per GP-08)
- **Multi-tenant isolation** enforced via `instance_id` on every query (per GP-07)
- **Point values hardcoded** in `AttendanceStatus` enum, not externally configurable
