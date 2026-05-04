# GAP-315: VAT E-Invoice (NĐ 123/2020) with HSM Signature + TCT API Integration

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — legal compliance (TCT mandatory)
**Domain:** Backend (kiteclass-core invoice module + new VAT submodule)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 2 ACs — tenant + admin (kế toán)

---

## Problem

P3 thường có B2B parents (công ty thanh toán học phí cho nhân viên / con employee). Họ yêu cầu hóa đơn VAT theo NĐ 123/2020/NĐ-CP (e-invoice mandatory từ 1/7/2022 cho tất cả doanh nghiệp). Workflow:

1. Kế toán convert invoice → VAT invoice (input MST + tên cty + địa chỉ)
2. Generate e-invoice với template chuẩn TCT
3. Ký số HSM (chữ ký số token)
4. Email PDF + XML cho công ty
5. Push lên Tổng cục Thuế qua API

Without this, kế toán phải dùng MISA/Viettel-Invoice ngoài hệ thống → VAT compliance break, tax audit risk.

## Root Cause

`module/invoice` có scaffold standard invoice nhưng:
- Không có VAT-specific fields (MST, ký hiệu hóa đơn, mẫu số)
- Không có HSM signing service
- Không có TCT API client
- Không có XML format generator chuẩn TCT

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Invoice entity | `kiteclass-core/.../module/invoice/` | ✅ exists (no VAT fields) |
| VAT-specific fields | — | ❌ missing |
| HSM signing service | — | ❌ missing |
| TCT API client | — | ❌ missing |
| XML schema generator (chuẩn TCT) | — | ❌ missing |
| Frontend "Convert to VAT" wizard | — | ❌ missing |

## Proposed Fix

**Phase 1 — VAT entity + XML generator:**
1. `VatInvoice` entity extends Invoice with MST + ký hiệu + mẫu số + buyer info
2. `VatXmlGenerator` produces XML matching TCT schema
3. Frontend "Convert to VAT" form

**Phase 2 — HSM signing + TCT API:**
1. HSM client (USB token / cloud HSM via FPT-CA / Viettel-CA)
2. `TctApiClient.submit(vatInvoice)` → returns receipt
3. Retry + circuit breaker for TCT outage

**Phase 3 — Bulk batch + reconciliation:**
1. Monthly batch convert eligible invoices to VAT
2. Reconcile with TCT receipts

## Acceptance Criteria

- [ ] VatInvoice entity with all NĐ 123/2020 mandatory fields
- [ ] XML schema validates against TCT XSD
- [ ] HSM signing test with FPT-CA sandbox
- [ ] TCT API submission returns valid receipt code
- [ ] Frontend "Convert to VAT" wizard with MST validation (10 or 13 digits)
- [ ] Email + XML delivered to buyer company within ≤2 phút
- [ ] Failed submissions queued + retry with exponential backoff
- [ ] Business rules in `documents/01-business/kiteclass/vat-einvoice/rules.md` per `business-logic-review.md` §2 5-attribute (Source = NĐ 123/2020/NĐ-CP, Compliance = "Compliant")

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-FIN-006 | Tenant Director | `P3-medium-center.md` |
| AC-FIN-002 | Admin (kế toán) | `secondary/admin-in-P3.md` |

## Related

- Existing: GAP-185 (billing/VAT — this gap is the concrete TCT integration)
- Compliance: NĐ 123/2020/NĐ-CP, Luật Quản lý Thuế 2019, Thông tư 78/2021/TT-BTC
- Persona review: §2 (Tenant AC-FIN-006), §4 (Admin AC-FIN-002)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C. NĐ 123/2020 mandates e-invoice cho mọi doanh nghiệp; without this, P3 cannot ship VAT-compliant feature.
