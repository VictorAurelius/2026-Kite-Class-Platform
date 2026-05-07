# Gamification Points Business Logic

> Last verified: 2026-03-24 | Source: `kiteclass-core/module/gamification/`

## 1. Rules

| ID | Rule | Detail | Config Key |
|----|------|--------|-----------|
| GP-01 | Points per PRESENT | 0 (no deduction) | `AttendanceStatus.PRESENT` |
| GP-02 | Points per LATE | -5 points | `AttendanceStatus.LATE` |
| GP-03 | Points per ABSENT | -10 points | `AttendanceStatus.ABSENT` |
| GP-04 | Points per EXCUSED | 0 (no deduction) | `AttendanceStatus.EXCUSED` |
| GP-05 | Points per MAKEUP | 0 (no deduction) | `AttendanceStatus.MAKEUP` |
| GP-06 | Points can be positive or negative | `points` column allows both | — |
| GP-07 | Multi-tenant isolation | Each point record has `instance_id` | — |
| GP-08 | Reference type tracking | Each point links to source: ATTENDANCE, GRADE, ASSIGNMENT | `reference_type` |
| GP-09 | Reference ID tracking | Links to specific source record ID | `reference_id` |
| GP-10 | One point per attendance | One StudentPoint per attendance record (delete+recreate on update) | — |
| GP-11 | Total points = SUM | `SELECT COALESCE(SUM(points), 0) WHERE student_id = ?` | — |
| GP-12 | Rule ID optional | `rule_id` column exists for future point rules (currently unused) | — |
| GP-13 | Points awarded on attendance mark | Automatic via AttendanceServiceImpl after save | — |
| GP-14 | Points updated on status change | Delete old + create new point record | — |

## 2. Flow

### Award Points (on attendance mark)
```
Teacher marks attendance (status: PRESENT/LATE/ABSENT/EXCUSED/MAKEUP)
  → Attendance saved with pointsAwarded (from AttendanceStatus.getPointsDeduction())
  → pointService.awardAttendancePoints(studentId, attendanceId, points, description)
    → Create StudentPoint record:
        instanceId = current tenant
        studentId = enrollment.studentId
        points = status.getPointsDeduction()
        referenceType = "ATTENDANCE"
        referenceId = attendanceId
        earnedAt = now
    → Save to student_points table
```

### Update Points (on attendance status change)
```
Teacher updates attendance status
  → pointService.updateAttendancePoints(studentId, attendanceId, newPoints, description)
    → Find existing StudentPoint by (referenceType=ATTENDANCE, referenceId=attendanceId)
    → Delete old record (if exists)
    → Create new StudentPoint with updated points
    → Save
```

### Get Total Points
```
Request student total points
  → pointService.getTotalPoints(studentId)
  → SQL: SELECT COALESCE(SUM(points), 0) FROM student_points WHERE student_id = ?
  → Return integer total
```

## 3. Emails

Khong co email trigger trong module gamification.

## 4. Config

```yaml
# Points values (hardcoded in AttendanceStatus enum)
# Currently not externalized to config
gamification:
  attendance:
    present: 0       # GP-01
    late: -5         # GP-02
    absent: -10      # GP-03
    excused: 0       # GP-04
    makeup: 0        # GP-05
```

### StudentPoint Entity (student_points table)
| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | BIGINT | PK | Auto-increment |
| instance_id | UUID | NOT NULL | Multi-tenant isolation |
| student_id | BIGINT | NOT NULL | FK to students |
| rule_id | BIGINT | NULL | Future: FK to point rules |
| points | INTEGER | NOT NULL | Positive=award, Negative=deduct |
| reference_type | VARCHAR(50) | NULL | "ATTENDANCE", "GRADE", "ASSIGNMENT" |
| reference_id | BIGINT | NULL | FK to source record |
| description | TEXT | NULL | Human-readable reason |
| earned_at | TIMESTAMP | NOT NULL | When points were earned |
| created_at | TIMESTAMP | NOT NULL | Record creation time |

### Future Extensions
- `rule_id` column ready for configurable point rules
- `reference_type` supports GRADE and ASSIGNMENT (not yet implemented)
- Leaderboard queries possible via `getTotalPointsByStudentId()`

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **N/A** — internal points system; no monetary value, no Consumer Protection trigger (not advertised as redeemable benefit).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Points-to-reward exchange added (would trigger Consumer Protection review).

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
