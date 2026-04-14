# GAP-059: Student Conduct / Behavior Tracking (Hạnh kiểm)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Product / Backend
**Persona blocked:** P5 K-12 School
**Detected:** 2026-04-14

## Problem

Trường VN grade hạnh kiểm (conduct) theo scale: Tốt, Khá, Trung bình, Yếu. Hiện tại không có.

## Proposed Fix

### Entity
```java
@Entity
public class StudentConduct {
  Student student;
  Semester semester;
  ConductGrade grade;  // TOT, KHA, TB, YEU
  String notes;
  Teacher assessedBy;  // GVCN
  List<BehaviorIncident> incidents;
}

@Entity
public class BehaviorIncident {
  Student student;
  BehaviorType type;  // LATE, ABSENCE, POSITIVE, NEGATIVE
  String description;
  Teacher reportedBy;
  LocalDate occurredAt;
  Integer points;  // + or -
}
```

### UI

GVCN records incidents, system auto-calculate conduct from incidents + attendance.

## Acceptance Criteria

- [ ] Conduct entity + incidents
- [ ] GVCN dashboard: record incidents
- [ ] Auto-calculate conduct từ incidents
- [ ] Show on report card (GAP-055)
- [ ] Parent notification khi negative incident

## Dependencies

- GAP-056 (GVCN)
- GAP-055 (report card)
- GAP-052 (parent portal)

## Log
- 2026-04-14 — Persona review
