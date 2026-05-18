# GAP-060: Period-Based Attendance (Nhiều tiết/ngày)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / Product
**Persona blocked:** P5 K-12 School
**Detected:** 2026-04-14

## Problem

Trường K-12 VN có **5-8 tiết/ngày** (periods). Hiện tại attendance chỉ có 1 lần/class session. Trường cần:
- Đi học buổi sáng (homeroom attendance)
- Có mặt từng tiết (per-period attendance)
- Phát hiện bỏ tiết (skip specific period)

## Proposed Fix

### Schedule

```java
@Entity
public class Period {
  Integer number;  // 1-10
  LocalTime startTime, endTime;  // 7:00-7:45, 7:50-8:35, ...
}

@Entity
public class DailyAttendance {
  Student student;
  LocalDate date;
  AttendanceStatus overallStatus;  // có mặt trường?
  Map<Period, AttendanceStatus> perPeriodStatus;
}
```

Bell schedule standard:
- Tiết 1: 7:00-7:45
- Tiết 2: 7:50-8:35
- Tiết 3 + giải lao
- Tiết 4
- ... 5 tiết sáng
- Nghỉ trưa
- Tiết 6-8 chiều (nếu học 2 buổi)

## Acceptance Criteria

- [ ] Period entity
- [ ] Daily attendance với per-period status
- [ ] Teacher records attendance từng tiết
- [ ] Parent notification nếu bỏ tiết
- [ ] Report: "Số tiết vắng trong tháng"

## Dependencies

- GAP-053 (academic year)
- GAP-054 (subject sections mapping to periods)

## Log
- 2026-04-14 — Persona review
