# GAP-883: Money type inconsistency — BIGINT vs NUMERIC vs DECIMAL + minor-unit confusion

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB / Business
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC finance + KH subscription)
**Affects:** Mọi bảng có cột tiền cross-cluster KiteClass + KiteHub

## Problem

Kiểu tiền không nhất quán cross-cluster:

- KC `invoices`/`invoice_items`/`payments`: DECIMAL(12,2)
- KC `payment_records`: NUMERIC(19,2)
- KC `payroll_*`: DECIMAL(15,2)/(7,2)/(5,2)
- KC `payment_idempotency_keys` V61 comment "BIGINT minor-unit" nhưng bảng KHÔNG có cột amount
- KH `subscriptions.price_vnd`/`payments.amount_vnd`: BIGINT đồng (scale 0)

Cross-DB analytics (KH MRR vs KC revenue per-tenant) cần normalize. Tài liệu/quy ước minor-unit BIGINT mâu thuẫn với thực tế DECIMAL scale 2.

## Proposed Fix

Document money type convention per service: KH BIGINT đồng (control-plane), KC DECIMAL scale 2 (per-tenant per session). Cross-DB report skill normalize. Cân nhắc converge sang DECIMAL(19,2) cho cả 2 service trong future refactor wave.

## Acceptance Criteria

- [ ] Architecture doc money-convention.md
- [ ] Reference cluster doc 04-finance §A5 + KH 02-subscription-billing §A1
- [ ] Future wave: decide converge or document divergence permanently

## Discovered in

KC `04-finance.md` §A5 + KH `02-subscription-billing.md` §A1
