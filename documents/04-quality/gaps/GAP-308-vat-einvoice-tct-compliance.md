# GAP-308: VAT E-Invoice + Chữ Ký Số HSM + XML TCT (NĐ 123/2020/NĐ-CP)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — VN tax compliance + B2B parent enablement)
**Domain:** Backend / Compliance / Financial
**Found:** 2026-05-04 (Wave 17 Bucket C P3 persona review — round 1)
**Affects:** P3 Medium Center (B2B parents), P5 K-12 School (BHXH/B2B/sponsorships), all tenants needing tax-deductible invoices

## Problem

P3 cần serve B2B parents (corporate paying học phí cho con). Theo NĐ 123/2020/NĐ-CP, e-invoice phải:
- Có MST + tên công ty + địa chỉ corporate fields
- Ký số bằng HSM (Hardware Security Module)
- Format XML chuẩn Tổng cục Thuế (TCT)
- Push tự động lên hệ thống TCT qua API

Batch generation 250 monthly invoices ≤5 phút (per AC-FIN-001) cũng nằm trong scope (cùng module).

State-check 2026-05-04:
- `Invoice` entity exists với `InvoiceNumberGenerator` (auto-numbering)
- `InvoiceServiceImpl` (13.5K) does single-invoice CRUD only
- `InvoiceController` endpoints: GET single, POST adjustments, POST late-fees, GET overdue. **NO** `POST /generate-batch` endpoint
- `grep -rln "VAT\|MST\|HSM" kiteclass/kiteclass-core/src/main/java` → 0 functional results (only AccessLevel/StorageStatus matches)
- No sibling discount logic (`grep Sibling` returns parent-link only, không phải discount)

Affects ACs: AC-FIN-001, AC-FIN-006 (tenant), AC-FIN-002 (admin).

## Root Cause

Wave 1-16 shipped invoice CRUD foundation (existing GAP-185 Phase 1). VAT compliance + batch generation explicitly deferred. NĐ 123/2020/NĐ-CP có hạn áp dụng từ 1/7/2022 — đã áp dụng trên thị trường VN.

## Proposed Fix

3-phase delivery:

**Phase 1 — Batch invoice generation** (Wave 18)
- `BatchInvoiceGenerator` service + `POST /api/v1/invoices/generate-monthly`
- Tuition fee rules engine: `TuitionFeeRule` entity per (class × month × pricing tier)
- Sibling discount: `SiblingDiscountRule` entity per tenant
- Late fee carryover from previous month
- Async generation with progress events (250 invoices in <5 min)

**Phase 2 — VAT e-invoice + TCT compliance** (Wave 19)
- Extend `Invoice` entity: `corporate_mst`, `corporate_name`, `corporate_address`, `vat_rate`, `vat_amount`, `xml_payload`, `tct_pushed_at`
- HSM signing service (3rd-party HSM provider integration)
- TCT XML schema mapper (per Thông tư 78/2021/TT-BTC)
- `TctApiClient` adapter for push API

**Phase 3 — Reconciliation + bank statement import** (Wave 19-20)
- MT940 / Vietcombank CSV / BIDV CSV importer
- Auto-match payment to invoice by reference + amount
- Cash entry workflow with receipt # + audit log

## Acceptance Criteria

- [ ] Phase 1: `POST /api/v1/invoices/generate-monthly` endpoint generates 250 invoices in <5 minutes
- [ ] Phase 1: TuitionFeeRule + SiblingDiscountRule entities + REST endpoints
- [ ] Phase 1: Late fee carryover applied automatically
- [ ] Phase 2: VAT invoice fields added to Invoice entity (migration)
- [ ] Phase 2: HSM signing integrated (mock for dev, real HSM for prod)
- [ ] Phase 2: XML TCT format matches Thông tư 78/2021/TT-BTC schema
- [ ] Phase 2: TCT push API integration (sandbox first, prod after compliance review)
- [ ] Phase 3: MT940 + bank CSV importers + auto-match service
- [ ] Phase 3: Cash entry workflow with audit log
- [ ] Each phase: `documents/01-business/kiteclass/billing-vat/{rules,use-cases,api-contract}.md` ships with code

## Related

- Audit report: `documents/00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md` §Critical Findings #3
- Existing gap (re-scope): GAP-185 (billing/VAT/TCT compliance)
- Compliance: NĐ 123/2020/NĐ-CP, Thông tư 78/2021/TT-BTC, Luật Quản lý Thuế 2019
- Persona AC: P3-medium-center.md AC-FIN-001/006, admin-in-P3.md AC-FIN-002
