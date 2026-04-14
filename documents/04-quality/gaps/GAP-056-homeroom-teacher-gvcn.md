# GAP-056: Homeroom Teacher (GVCN) Concept

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Product
**Detected:** 2026-04-14 (persona review)
**Persona blocked:** P5 K-12 School

## Problem

Trường K-12 VN có **Giáo viên Chủ nhiệm (GVCN)** cho mỗi homeroom class:
- Responsible for class as a whole (30 students)
- Monitor conduct (hạnh kiểm), behavior
- Communicate với phụ huynh
- Chair parent meetings
- Sign report cards
- Handle disciplinary issues
- NOT same as subject teacher

Hiện tại Teacher entity không có concept này → trường không assign được GVCN.

## Proposed Fix

### Entity extension

```java
@Entity
public class HomeroomClass {
  // ... (GAP-054)
  @OneToOne
  Teacher homeroomTeacher;  // GVCN

  @ManyToMany
  Set<Teacher> subjectTeachers;  // 12 môn teachers
}

// Teacher có thể là GVCN của 1 class + teach multiple subject sections
```

### Permissions

GVCN có quyền đặc biệt:
- View all subject grades for their class
- Edit conduct grades (hạnh kiểm)
- Approve leave requests
- Communicate with all parents in class
- Sign report cards

### Dashboard GVCN

Separate view:
- Class overview (30 students with avg grades, attendance %)
- Parent communication hub
- Behavior log
- Upcoming parent meetings
- Monthly reports to principal

## Acceptance Criteria

- [ ] HomeroomClass.homeroomTeacher field
- [ ] Special permissions for GVCN
- [ ] GVCN dashboard UI
- [ ] Signature capture on reports
- [ ] Parent communication log

## Dependencies

- GAP-054 (homeroom structure)
- GAP-058 (role hierarchy)

## Log
- 2026-04-14 — Persona review
