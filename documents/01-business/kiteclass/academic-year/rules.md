# Academic Year — Business Rules

**Domain:** academic-year
**Last verified:** 2026-04-14
**Source:** GAP-053, ADR-002

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| BR-ACYR-001 | Academic year name unique per tenant | — | unique constraint (instance_id, name) |
| BR-ACYR-002 | endDate > startDate | enforced | DB CHECK constraint |
| BR-ACYR-003 | Only 1 CURRENT year per tenant at any time | 1 | Enforced in `AcademicYearService.setCurrent()` |
| BR-ACYR-004 | Academic year contains 1+ semesters | 1-3 | HK1, HK2, optional SUMMER |
| BR-ACYR-005 | Holidays scoped to academic year | — | FK constraint + range check |
| BR-ACYR-006 | VN national holidays auto-seeded on year creation | auto | `VnHolidayProvider.generateForAcademicYear()` |
| BR-ACYR-007 | Status transitions: UPCOMING → CURRENT → COMPLETED | unidirectional | State enforced in service |

## Config

```yaml
academic-year:
  vn-holiday-provider: vn  # Strategy: vn, en, international
  auto-seed-holidays: true
```

## Default VN Holidays (seeded per year)

| Holiday | Date | Notes |
|---------|------|-------|
| Tết Dương lịch | 1/1 | Solar |
| Tết Nguyên đán | late Jan/early Feb | Lunar, 7 days |
| Giỗ tổ Hùng Vương | 10/3 lunar (~April 18) | Lunar |
| Ngày Thống nhất | 30/4 | Solar |
| Quốc tế Lao động | 1/5 | Solar |
| Quốc khánh | 2/9 | Solar |

## State Machine

```
UPCOMING ──setCurrent()──> CURRENT ──endDate passed──> COMPLETED
                              │
                              │ demote (previous when new becomes CURRENT)
                              ▼
                          COMPLETED
```

## Log
- 2026-04-14 — Initial rules (GAP-053, ADR-002)
