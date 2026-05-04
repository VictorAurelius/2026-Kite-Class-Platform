# GAP-057: Teacher Payroll + Commission Calculation

**Status:** 🟡 PARTIAL — Phase 1 (HOURLY only) shipped 2026-05-04 (Wave 18a Bucket C); Phase 2 → GAP-057b
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

## Acceptance Criteria (Phase 1 ✅ ; Phase 2 deferred → GAP-057b)

- [x] **Phase 1 ✅** PayrollConfig + PayrollPeriod entities (Wave 18a Bucket C — V48 migration shipped 2026-05-04)
- [x] **Phase 1 ✅ partial** Calculation engine — HOURLY only; SALARY/COMMISSION/HYBRID throw `UnsupportedOperationException` with GAP-057b reference. **Deferred:** SALARY/COMMISSION/HYBRID engines → GAP-057b
- [ ] **Deferred → GAP-057b** Monthly payroll run UI (POST /runs endpoint)
- [ ] **Deferred → GAP-057b** Payslip PDF generation (depends on GAP-047)
- [ ] **Deferred → GAP-057b** VN tax (TNCN) progressive + BHXH (8%) + BHYT (1.5%) deductions
- [ ] **Deferred → GAP-057b** Bank export format (BIDV/VCB/TCB batch transfer)
- [x] **Phase 1 ✅ partial** Audit log — JPA auditing via BaseEntity (`created_by` / `updated_by`); approve/pay audit detail → GAP-057b

## Phase 1 ship summary (2026-05-04)

**Branch:** `wave/18a-bucket-c-payroll-hourly`
**PR:** (TBD when opened)

Files shipped:
- `kiteclass-core/module/payroll/entity/{PayrollConfig, PayrollPeriod}.java`
- `kiteclass-core/module/payroll/enums/{PayrollType, PayrollStatus}.java`
- `kiteclass-core/module/payroll/repository/{PayrollConfigRepository, PayrollPeriodRepository}.java`
- `kiteclass-core/module/payroll/service/{PayrollService, impl/PayrollServiceImpl}.java`
- `kiteclass-core/module/payroll/controller/PayrollController.java`
- `kiteclass-core/module/payroll/dto/{PayrollConfigResponse, PayrollPeriodResponse}.java`
- `kiteclass-core/db/migration/V48__add_payroll_tables.sql`
- `kiteclass-frontend/src/{types/payroll.ts, lib/api/payroll.ts, hooks/use-payroll.ts, app/(dashboard)/admin/payroll/page.tsx}`
- `documents/01-business/kiteclass/payroll/{rules.md, use-cases.md, api-contract.md}` (3-layer per CLAUDE.md)

Tests: 15 unit tests in `PayrollServiceTest` covering HOURLY happy path (whole/fractional/zero hours, multi-class), HALF_EVEN rounding, validation, type-deferral exceptions, read-only views.

## Dependencies

- GAP-047 (PDF generation) — blocks Phase 2 payslip PDF
- GAP-049 (business correctness — VN tax compliance) — blocks Phase 2 TNCN/BHXH

## Log
- 2026-05-04 — **Phase 1 SHIPPED** (Wave 18a Bucket C). HOURLY calc engine + entities + read-only admin UI + 3-layer business docs + 15 unit tests green. Phase 2 (SALARY/COMMISSION/HYBRID + VN tax + BHXH/BHYT + payslip PDF + bank export + run/approve UI) deferred to **GAP-057b** (closure coordinator files).
- 2026-04-14 — Persona review
