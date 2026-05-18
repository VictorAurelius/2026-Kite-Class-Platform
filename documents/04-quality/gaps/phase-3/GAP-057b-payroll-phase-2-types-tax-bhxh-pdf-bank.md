# GAP-057b: Payroll Phase 2 — SALARY/COMMISSION/HYBRID + VN tax + BHXH/BHYT + PDF + bank export + run/approve UI

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (sister of GAP-057 Phase 1 SHIPPED Wave 18a)
**Domain:** Backend / Finance
**Detected:** 2026-05-04 (Wave 18a Bucket C closure — sister gap filed by closure coordinator)
**Affects:** P2 + P3 + P5 personas (commission centers + K-12 schools using payroll)

---

## Context

Phase 1 of GAP-057 shipped Wave 18a Bucket C (PR #758 merged 2026-05-04):
- ✅ `PayrollConfig` + `PayrollPeriod` entities (greenfield `module/payroll/`)
- ✅ `PayrollType` enum (SALARY, HOURLY, COMMISSION, HYBRID — only HOURLY supported by Phase 1 service)
- ✅ `PayrollStatus` enum (DRAFT, APPROVED, PAID — Phase 1 creates DRAFT only)
- ✅ `PayrollService` HOURLY calc engine + 15 unit tests (HALF_EVEN rounding, VND scale=2)
- ✅ `PayrollController` 3 read-only endpoints + admin-only RBAC
- ✅ V48 migration with CHECK constraints + unique teacher+tenant index
- ✅ FE read-only list page
- ✅ Business docs 3-layer + business-logic-review.md 5-attribute frontmatter

Phase 1 service throws `UnsupportedOperationException("Phase 2 GAP-057b will support {type}")` for SALARY/COMMISSION/HYBRID.

This gap (057b) covers Phase 2 deferred items.

## Problem

Phase 1 supports HOURLY only. Real-world VN centers + schools use mixed:
- **SALARY** (full-time school teacher, monthly fixed)
- **COMMISSION** (private center, % of tuition collected)
- **HYBRID** (base + bonus + GVCN allowance + overtime)

Plus VN compliance (TNCN tax + BHXH + BHYT) and operational needs (PDF payslip + bank export + run/approve workflow) are blocked.

P3 Medium Center scored 9.6/100 in Wave 17 review — commission/payroll the top blocker.

## Current State (verified 2026-05-04)

| Piece | Phase 1 baseline | Status |
|-------|------------------|--------|
| Entities + migration + read-only UI | ✅ DONE | — |
| HOURLY calc engine | ✅ DONE | — |
| 4 PayrollType enum values | ✅ DONE in enum | Service rejects 3 of 4 |
| SALARY calc | ❌ Phase 2 | service throws UnsupportedOperationException |
| COMMISSION calc (% of tuition) | ❌ Phase 2 | depends on tuition collection data |
| HYBRID calc (base + bonus + allowance + overtime) | ❌ Phase 2 | most complex |
| VN TNCN progressive tax | ❌ Phase 2 | depends GAP-049 (business correctness) |
| BHXH 8% + BHYT 1.5% mandatory deductions | ❌ Phase 2 | |
| Region-based minimum wage check | ❌ Phase 2 | |
| Payslip PDF generation | ❌ Phase 2 | depends GAP-047 (document generation skills) |
| Bank export format (batch transfer) | ❌ Phase 2 | |
| Admin run/approve/pay workflow UI | ❌ Phase 2 | needs state machine wiring |
| Audit log expansion (who approved when) | ❌ Phase 2 | Phase 1 has basic logging |

## Proposed Fix

### 2.1 Calculation engines (3 remaining types)

1. **SALARY**: `gross = baseSalary + sum(monthly_bonuses) + gvcnAllowance` (constant)
2. **COMMISSION**: `gross = sum(tuition_collected_for_teacher_classes_in_period) × commissionPercent / 100`
   - Read tuition data from `module/payment` (cross-module read-only access)
   - Handle refund/dispute deductions
3. **HYBRID**: `gross = baseSalary + (hoursWorked × overtimeRate × overtimeMultiplier) + commissionPart + gvcnAllowance + bonuses`
   - Most complex; per-component config in PayrollConfig.bonuses Map

Strategy Pattern: refactor `PayrollService` to dispatch via type-specific calculator (`SalaryCalculator`, `CommissionCalculator`, `HybridCalculator`). HOURLY engine stays as-is.

### 2.2 VN tax + insurance deductions

1. **TNCN progressive (Personal Income Tax)** rates per Luật Thuế TNCN 2007 (current rates):
   - 0-5M VND/month → 5%
   - 5-10M → 10%
   - 10-18M → 15%
   - 18-32M → 20%
   - 32-52M → 25%
   - 52-80M → 30%
   - >80M → 35%
   - Plus deductions: personal allowance 11M + dependent 4.4M each
2. **BHXH** 8% (employee portion) + **BHYT** 1.5% mandatory
3. **Region-based minimum wage check**: compare gross vs region min, alert if below
4. New service: `VnTaxCalculatorService.computeDeductions(grossAmount, dependents, region)`
5. Tax law constants in `application.yml` per `business-logic-review.md` §2.5 (5-attribute rule entry)

### 2.3 Payslip PDF generation

1. Depends on GAP-047 (document generation skills with Word/PDF/Excel/PPT engines)
2. Template: company branding, period dates, gross/deductions/net breakdown, VN signature block
3. Multi-language: Vietnamese primary, English optional for international schools
4. Audit: PDF generation event logged

### 2.4 Bank export format

1. Vietnamese banks support various batch formats — primary candidates: VCB / VietinBank / BIDV / TCB
2. ADR for format selection
3. Excel/CSV file with: account number, account holder, amount, narration
4. Encrypted at rest (file contains PII)
5. Admin downloads from approval workflow UI

### 2.5 Run/Approve/Pay workflow UI

1. Admin triggers monthly payroll run for a date range
2. Preview screen shows aggregated totals before commit
3. Approve transitions DRAFT → APPROVED (state machine — `design-patterns.md` §3.3)
4. Pay action generates PDFs + bank export + transitions to PAID
5. Audit log: who triggered, who approved, who paid

### 2.6 Audit expansion

1. New `payroll_audit_log` table: payroll_period_id, action, actor_id, before_state, after_state, timestamp
2. Hash-chained for non-repudiation (precedent: GAP-322 child protection workflow audit pattern)

## Acceptance Criteria

- [ ] `SalaryCalculator` + `CommissionCalculator` + `HybridCalculator` implemented + unit tests for each (15-20 tests/calculator)
- [ ] `VnTaxCalculatorService` with progressive TNCN + BHXH/BHYT, configurable via YAML
- [ ] Region minimum wage check + alert mechanism
- [ ] PayrollService dispatches via Strategy Pattern (no switch statement)
- [ ] Payslip PDF generation working (depends GAP-047)
- [ ] Bank export to chosen format (1 bank Phase 2.4; multi-bank Phase 2.5)
- [ ] Admin run/preview/approve/pay UI workflow
- [ ] State machine DRAFT → APPROVED → PAID enforced (no direct status-set)
- [ ] Hash-chained payroll audit log
- [ ] Tests: scenario for each PayrollType + tax bracket + edge case (overtime > 200% normal hours, commission carryover, hybrid all-components)
- [ ] Business docs updated: BR-PAYROLL-012..030 + use-cases for run/approve/pay
- [ ] business-logic-review.md 5-attribute on rules.md (Source: Luật Thuế TNCN + Luật BHXH + competitor analysis on commission %; Reviewer: solo-dev + queue formal Tax advisor review; Compliance: Compliant per VN tax codes)

## Estimated Effort

~3-4 weeks. Suggested split:
- 057b.1: SALARY + COMMISSION + HYBRID calculators (~7 days)
- 057b.2: VN tax + BHXH/BHYT (~5 days, depends on GAP-049 + tax data sourcing)
- 057b.3: Run/Approve/Pay UI workflow + state machine (~5 days)
- 057b.4: Payslip PDF (~3 days, depends GAP-047)
- 057b.5: Bank export + admin download (~3 days)
- 057b.6: Audit log hash-chain (~2 days)

## Related

- **Sister of:** GAP-057 Phase 1 (PR #758 merged 2026-05-04)
- **Depends on:** GAP-047 (document generation skills — for PDF), GAP-049 (business correctness — VN tax compliance)
- **Cross-cuts:** GAP-017 (billing), GAP-321 (parent portal — payslip viewable for teacher employees), GAP-322 (child protection audit hash-chain pattern reuse)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-18a-keystones.md`

## Log

- **2026-05-04** — Filed by Wave 18a closure coordinator. Phase 1 SHIPPED HOURLY only; Phase 2 scope explicitly listed in PR #758 description + service throws UnsupportedOperationException for unsupported types. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
