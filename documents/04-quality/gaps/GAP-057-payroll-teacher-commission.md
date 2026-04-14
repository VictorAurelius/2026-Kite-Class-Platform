# GAP-057: Teacher Payroll + Commission Calculation

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Finance
**Detected:** 2026-04-14 (persona review)
**Persona blocked:** P3 Medium Center, P4 Chain, P5 School

## Problem

Giáo viên được trả theo model đa dạng:
- **Full-time salary:** monthly fixed (trường)
- **Hourly rate:** theo tiết dạy (trung tâm)
- **Commission:** % of tuition (trung tâm)
- **Mix:** base + bonus
- **Special:** GVCN allowance, overtime

Không có payroll module → center/school tính thủ công Excel → error-prone.

## Proposed Fix

### Entities

```java
@Entity
public class PayrollConfig {
  Teacher teacher;
  PayrollType type;  // SALARY, HOURLY, COMMISSION, HYBRID
  BigDecimal baseSalary;
  BigDecimal hourlyRate;
  BigDecimal commissionPercent;
  BigDecimal gvcnAllowance;
  Map<String, BigDecimal> bonuses;
}

@Entity
public class PayrollPeriod {
  Teacher teacher;
  LocalDate startDate, endDate;
  Integer hoursWorked;
  Integer classesTaught;
  BigDecimal totalStudents;
  BigDecimal grossAmount;
  BigDecimal deductions;  // tax, insurance
  BigDecimal netAmount;
  PayrollStatus status;  // DRAFT, APPROVED, PAID
}
```

### Calculation engine

Per pay period (monthly):
```
For each teacher:
  1. Get PayrollConfig
  2. Gather data:
     - Hours taught (from class schedules)
     - Tuition collected for teacher's classes (commission)
     - Overtime hours
     - Bonuses earned (GVCN, perfect attendance, etc.)
  3. Calculate gross
  4. Apply deductions (tax, BHXH, etc.)
  5. Generate PayrollPeriod record
```

### UI

Admin:
- PayrollConfig per teacher
- Run payroll (monthly)
- Preview + approve
- Generate payslips (PDF — reuse GAP-047)
- Export to bank (batch transfer)

Teacher:
- View payslips history
- Download PDF

### VN Tax Integration

- Personal income tax (thuế TNCN) progressive rates
- BHXH (social insurance) mandatory percentage
- Region-based minimum wage check

## Acceptance Criteria

- [ ] PayrollConfig + PayrollPeriod entities
- [ ] Calculation engine support 4 types
- [ ] Monthly payroll run UI
- [ ] Payslip PDF generation
- [ ] VN tax/BHXH calculation
- [ ] Bank export format
- [ ] Audit log for payroll changes

## Dependencies

- GAP-047 (PDF generation)
- GAP-049 (business correctness — VN tax compliance)

## Log
- 2026-04-14 — Persona review
