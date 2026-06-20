# Payroll — Business Rules

**Domain:** KiteClass Core / Finance
**Version:** 0.1 (Phase 1)
**Updated:** 2026-05-04
**Source:** GAP-057 Phase 1 (Wave 18a Bucket C); persona evidence — P3 Medium Center 9.6/100 commission/payroll keystone

---

## 1. Scope of Phase 1

> Phase 1 ships **HOURLY type only**. Other payroll types (SALARY / COMMISSION / HYBRID), VN tax (TNCN) progressive deductions, BHXH/BHYT mandatory percentages, payslip PDF, bank export, and admin run/approve UI workflow are deferred to **GAP-057b (Phase 2)**.

Phase 1 service `PayrollService.calculate(...)` throws `UnsupportedOperationException` for SALARY / COMMISSION / HYBRID with a message naming GAP-057b. Phase 1 is a foundation: it creates the entity tables, calculation engine for the most common center-based pay model (per-hour), and admin read-only views. Phase 2 builds approve/pay workflow + tax compliance on top.

## 2. Rules

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-PAYROLL-001 | One config per teacher per tenant | DB unique index `uk_payroll_configs_teacher_tenant` on (teacher_id, instance_id) where deleted=false | 1 |
| BR-PAYROLL-002 | HOURLY requires hourlyRate > 0 | Service rejects null/zero/negative `hourly_rate` with `ValidationException("PAYROLL_HOURLY_RATE_REQUIRED")`; DB check `chk_payroll_config_hourly_rate_positive` | 1 |
| BR-PAYROLL-003 | Multi-tenant isolation | `instance_id` populated by BaseEntity tenant filter; queries restricted by Hibernate `tenantFilter` | 1 |
| BR-PAYROLL-004 | endDate >= startDate | Service rejects with `ValidationException("PAYROLL_PERIOD_END_BEFORE_START")`; DB check `chk_payroll_period_dates` | 1 |
| BR-PAYROLL-005 | HOURLY gross formula | `grossAmount = sum(ClassSession duration) × hourlyRate` over all teacher's class assignments where `sessionDate ∈ [startDate, endDate]` | 1 |
| BR-PAYROLL-006 | Phase 1 deductions = 0 | Phase 1 sets `deductions = 0` → `netAmount = grossAmount`. Phase 2 (GAP-057b) ships TNCN progressive + BHXH (8%) + BHYT (1.5%) per GAP-049 VN tax compliance | 1 (Phase 2 ships full) |
| BR-PAYROLL-007 | HALF_EVEN rounding scale=2 | All VND amounts use `RoundingMode.HALF_EVEN` (banker's rounding) at scale 2; hours stored at scale 2 | 1 |
| BR-PAYROLL-008 | Phase 1 status = DRAFT only | Calculate creates DRAFT records; APPROVED + PAID transitions are Phase 2 (GAP-057b) | 1 |
| BR-PAYROLL-009 | Soft delete only | `deleted` flag inherited from BaseEntity; never hard delete | 1 |
| BR-PAYROLL-010 | Read-only API in Phase 1 | Only GET endpoints. POST/PUT/DELETE for run/approve/pay deferred to GAP-057b | 1 |
| BR-PAYROLL-011 | Audit trail via JPA auditing | `created_at` / `created_by` / `updated_at` / `updated_by` populated by BaseEntity. Detailed approval audit log (who approved when) deferred to GAP-057b | 1 (Phase 2 expands) |

## 3. Type definitions

### PayrollType (enum)

| Value | Phase 1 | Phase 2 calc engine |
|-------|:------:|---------------------|
| `HOURLY` | ✅ Supported | — |
| `SALARY` | ❌ Throws `UnsupportedOperationException` | Fixed monthly salary, prorated by period coverage |
| `COMMISSION` | ❌ Throws `UnsupportedOperationException` | `commissionPercent × tuition_collected_for_teacher_classes` |
| `HYBRID` | ❌ Throws `UnsupportedOperationException` | `baseSalary + commission + gvcn_allowance + bonuses` |

### PayrollStatus (enum)

| Value | Phase 1 | Phase 2 |
|-------|:------:|---------|
| `DRAFT` | ✅ Default on calc | — |
| `APPROVED` | ⏸ Reserved | GAP-057b admin approve UI |
| `PAID` | ⏸ Reserved | GAP-057b bank export integration |

Phase 1 transitions: only `(none) → DRAFT`. Phase 2 transitions: `DRAFT → APPROVED → PAID` (forward only).

## 4. Calculation algorithm (Phase 1, HOURLY)

Pseudocode — see `PayrollServiceImpl.calculate(...)`:

```
calculate(teacherId, startDate, endDate):
  if startDate == null or endDate == null:
    throw ValidationException(PAYROLL_PERIOD_DATES_REQUIRED)
  if endDate < startDate:
    throw ValidationException(PAYROLL_PERIOD_END_BEFORE_START)

  config = PayrollConfigRepository.findByTeacherIdAndDeletedFalse(teacherId)
  if config not found:
    throw EntityNotFoundException(PAYROLL_CONFIG_NOT_FOUND)

  if config.type != HOURLY:
    throw UnsupportedOperationException("Phase 2 GAP-057b will support {type}")

  if config.hourlyRate == null or <= 0:
    throw ValidationException(PAYROLL_HOURLY_RATE_REQUIRED)

  hoursWorked = 0
  for each TeacherClass assignment of teacher:
    for each ClassSession of that class where deleted=false:
      if session.sessionDate not in [startDate, endDate]:
        continue
      hoursWorked += duration(session.startTime, session.endTime)

  hoursWorked = HALF_EVEN(hoursWorked, scale=2)
  grossAmount = HALF_EVEN(hoursWorked × hourlyRate, scale=2)
  deductions  = 0  // BR-PAYROLL-006 Phase 1
  netAmount   = grossAmount

  persist PayrollPeriod with status=DRAFT
  return persisted period
```

## 5. Existing state references

- **ClassSession** (`kiteclass-core/module/clazz/entity/ClassSession.java` — Bucket A's domain) is the source of truth for session start/end times. Phase 1 reads it read-only — no schema change.
- **TeacherClass** (`kiteclass-core/module/teacher/entity/TeacherClass.java`) provides teacher↔class assignments. Phase 1 sums hours across all assignments for the teacher.
- **GAP-099 Phase 1** (`class_schedule_slots` table) provides K-12 weekly schedule slots; not consumed by Phase 1 payroll engine because hourly compensation flows from actual sessions taught (ClassSession), not theoretical schedule slots.

## 6. Config keys

| Key | Default | Description | Phase |
|-----|---------|-------------|:-----:|
| `payroll.money.scale` | `2` | Decimal scale for VND amounts (HALF_EVEN rounding) | 1 |
| `payroll.hours.scale` | `2` | Decimal scale for hours (fractional sessions) | 1 |
| `payroll.cache.name` | `payroll-configs` | Reserved for Phase 2 admin list cache | 2 |

> Phase 1 does **not** hardcode rates; `hourly_rate` is per-config and admin-configurable (UI: GAP-057b — Phase 1 ships read-only display).

## 7. Database indexes

`payroll_configs`:
- `uk_payroll_configs_teacher_tenant` — unique (teacher_id, instance_id) WHERE deleted=false
- `idx_payroll_configs_teacher_id` — teacher lookup
- `idx_payroll_configs_instance_id` — tenant filter
- `idx_payroll_configs_type` — admin filter by type

`payroll_periods`:
- `idx_payroll_periods_teacher_id` — teacher list
- `idx_payroll_periods_instance_id` — tenant filter
- `idx_payroll_periods_dates` — date range query (start_date, end_date)
- `idx_payroll_periods_status` — admin filter by status

## 8. Compliance considerations (Phase 1 status: deferred)

Per `business-logic-review.md` 5-attribute frontmatter:

- **Source:** GAP-057 persona review (P3 Medium Center 9.6/100, P5 School commission keystone) + competitor mapping (Hotmart per-hour mode, KiteHub equivalent). Internal data: 0% (no production payroll data yet).
- **Rationale:** HOURLY first because (a) most centers use it, (b) simplest calc, (c) decouples from VN tax compliance until Phase 2. SALARY/COMMISSION/HYBRID inherently couple with TNCN progressive rates → batch with Phase 2 to avoid shipping half-tax-compliant calc.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead, solo-dev, 2026-05-04). Legal review for VN tax (TNCN) + BHXH/BHYT compliance queued — see GAP-057b acceptance criteria. Must complete before Phase 2 ships to production.
- **Compliance check:** **N/A for Phase 1** (self-assessed) — Phase 1 calculates GROSS only with deductions=0; no tax / social-insurance amount is computed, so no withholding obligation is triggered yet. Phase 2 (GAP-057b) **MUST become Compliant** before computing real pay, against (per `documents/00-brd/compliance-checklist.md` L2/L4): **Bộ luật Lao động 2019** (Luật 45/2019/QH14 — teacher/contractor remuneration, overtime ×1.5/×2 Đ.98); **Luật Thuế Thu nhập Cá nhân (TNCN)** — progressive PIT withholding (5/10/15/20/25/30/35% brackets); **Luật BHXH 2014** — BHXH 8% + BHYT 1.5% mandatory deductions (current rates, may shift before Phase 2 ships); **Luật Quản lý Thuế 2019** (payroll record-keeping). Phase 2 posture: **Considered (self-assessed, counsel pending GAP-156 AC-D)** — no legal/tax counsel has reviewed.
- **Review cadence:** Phase 1: re-review when Phase 2 (GAP-057b) starts (gates on Phase 2 design). Phase 2: Quarterly + event-driven on TNCN/BHXH rate changes. **Next review:** at GAP-057b kickoff (no fixed date — depends on Wave 18a closure + persona round 2 priority signal).

## 9. Future scope (Phase 2 — GAP-057b)

- SALARY / COMMISSION / HYBRID calculation engines
- VN tax (TNCN) progressive deduction (5/10/15/20/25/30/35% brackets)
- BHXH (8%) + BHYT (1.5%) mandatory deductions
- Payslip PDF generation (depends on GAP-047 PDF infrastructure)
- Bank export format (BIDV / VCB / Vietcombank batch transfer file format)
- Admin run/approve UI workflow (POST /run, POST /periods/{id}/approve, POST /periods/{id}/pay)
- Audit log expansion: who approved when + reason field
- Outbox event on approve / pay transitions (per `design-patterns.md` §3.5.1)
- Region-based minimum wage check
- Overtime hours (×1.5 / ×2 per Luật Lao động Art 98)

## 10. Log

- **2026-05-04 (v0.1)** — Initial Phase 1 ship (Wave 18a Bucket C, GAP-057 Phase 1). HOURLY only; Phase 2 deferred to GAP-057b.
