# GAP-061: Promotion / Retention Logic (Lên lớp / Ở lại lớp)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / Product
**Persona blocked:** P5 K-12 School
**Detected:** 2026-04-14

## Problem

Cuối năm học, học sinh:
- **Đủ điểm:** lên lớp (promote)
- **Thiếu điểm:** ở lại lớp (retain) hoặc thi lại (reexam)
- **Đặc biệt:** chuyển trường, bỏ học

Không có logic tự động → admin phải xử lý thủ công từng học sinh.

## Proposed Fix

### Promotion Rules (configurable)

```yaml
promotion-rules:
  TOT: # Hạnh kiểm
    min-avg: 8.0
  KHA:
    min-avg: 6.5
    max-failing-subjects: 1
  TB:
    min-avg: 5.0
    max-failing-subjects: 2
  YEU:
    auto-retain: true
```

### Year-End Batch Process

```java
@Scheduled
public void endOfYearPromotion() {
  for (student in currentYear) {
    var decision = calculatePromotion(student);
    switch (decision) {
      case PROMOTE: enrollInNextGrade(student);
      case RETAIN: reenrollSameGrade(student);
      case REEXAM: scheduleReexam(student);
      case EXIT: markInactive(student);
    }
  }
}
```

### Manual Override

Admin/GVCN có thể override với reason (e.g., medical, special circumstances).

## Acceptance Criteria

- [ ] Configurable promotion rules
- [ ] Auto-calculate promotion status
- [ ] Year-end batch runner
- [ ] Manual override UI
- [ ] Audit log decisions
- [ ] Parent notification của decision

## Dependencies

- GAP-053 (academic year rollover)
- GAP-054 (subject grades)
- GAP-059 (conduct)

## Log
- 2026-04-14 — Persona review
