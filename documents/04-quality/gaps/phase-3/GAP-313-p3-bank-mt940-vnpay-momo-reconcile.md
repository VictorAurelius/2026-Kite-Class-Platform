# GAP-313: Bank MT940 Import + VNPay/MoMo CSV Payment Reconciliation

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core payment module)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 1 AC tenant-level + 1 AC admin (kế toán)

---

## Problem

P3 kế toán handle 250 invoices/tháng × 3-4 payment methods (bank transfer, VNPay, MoMo, cash). Auto-reconcile yêu cầu:
1. Import bank statement file format MT940 (Vietcombank, BIDV)
2. Import VNPay CSV monthly settlement file
3. Import MoMo CSV
4. Match by invoice reference / amount / date → auto-reconcile 80%+ invoices
5. Cash entries manual với receipt # + audit log

## Root Cause

`module/payment` exists với VNPay integration tại runtime (callback) nhưng:
- Không có MT940 parser
- Không có VNPay/MoMo settlement file CSV importer
- Không có reconciliation matcher service
- Không có manual cash entry UI với receipt # + audit log

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Payment entity | `kiteclass-core/.../module/payment/` | ✅ exists |
| VNPay runtime callback | likely exists | ⚠️ scaffold only |
| MT940 parser | — | ❌ missing |
| VNPay settlement CSV import | — | ❌ missing |
| MoMo CSV import | — | ❌ missing |
| ReconciliationMatcherService | — | ❌ missing |
| Manual cash entry UI | — | ❌ missing |

## Proposed Fix

1. `BankFileParser` interface with MT940 + CSV implementations
2. `ReconciliationService.match(payments, invoices)` returns matched + unmatched
3. Frontend kế toán: "Import bank file" wizard → preview matches → confirm
4. Manual cash entry form with receipt # + audit log entry
5. Reconciliation report exportable

## Acceptance Criteria

- [ ] MT940 parser tested with Vietcombank + BIDV sample files
- [ ] VNPay/MoMo CSV importers handle their respective formats
- [ ] Auto-match rate ≥80% on test dataset of 250 invoices
- [ ] Manual cash entry creates audit log row with receipt # + entered_by
- [ ] Reconciliation report exportable as Excel/PDF

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-FIN-002 | Tenant Director | `P3-medium-center.md` |
| AC-OPS-003 | Admin (kế toán) | `secondary/admin-in-P3.md` |

## Related

- Existing: GAP-185 (billing/VAT — partial overlap)
- Persona review: §2 (Tenant AC-FIN-002), §4 (Admin AC-OPS-003)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
