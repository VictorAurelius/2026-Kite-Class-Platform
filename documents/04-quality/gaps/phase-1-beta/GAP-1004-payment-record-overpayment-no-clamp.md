# GAP-1004: record-payment thiếu over-payment clamp + idempotency không enforce DB-side

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-05 (KC-7 invoice→payment G1 walk)
**Affects:** `PaymentRecordServiceImpl` + `RecordPaymentRequest` (kiteclass-core)

## Problem

KC-7 G1 walk (live, kiteclass_shared) bắt 2 hardening gaps trên record-payment:

1. **Over-payment không clamp:** `POST /invoices/15/record-payment` amount=4,000,000 trên invoice total 3,500,000 → **HTTP 201**, `amount_paid=4,000,000`, `balance_due=-500,000` (generated column âm), status PAID. `RecordPaymentRequest` chỉ validate amount>0 (cần verify), `PaymentRecordServiceImpl:85-86` cộng dồn không check trần `total`.
2. **Idempotency không enforce DB-side:** 2 POST cùng `Idempotency-Key` → **2 payment_records** (count=2). `PaymentRecordServiceImpl:66-68` chỉ `log.debug` key, không check bảng `idempotency_keys` (V66) — code comment thừa nhận deferred "until duplicate-record incident surfaces". Walk = incident reproduction.

## Root Cause

(1) Thiếu upper-bound validation amount ≤ (total − amount_paid). (2) Idempotency-Key chỉ logged, không persisted/checked.

## Proposed Fix

(1) Validate `amount ≤ balanceDue` trong service → 400 `PAYMENT_EXCEEDS_BALANCE` (hoặc clamp + PENDING_BALANCE per GAP-627 nếu Phase 1.5). (2) Check/insert `idempotency_keys` (V66 shared) bằng (invoice_id, key) tuple trước khi tạo record.

## Acceptance Criteria

- [ ] Over-payment → 400 (hoặc clamp documented); `balance_due` không âm
- [ ] Double-submit cùng Idempotency-Key → 1 payment_record (2nd trả cached 200/409)
- [ ] IT cover cả 2 case trên Testcontainers Postgres

## Related

- Discovered in: KC-7 G1 walk artifact `documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc7-invoice-payment.md` §G1 (#4, #5)
- Related: GAP-627 (payment-amount mismatch PENDING_BALANCE, Phase 1.5), GAP-632 (idempotency Redis + UNIQUE constraint, Phase 1.5)
