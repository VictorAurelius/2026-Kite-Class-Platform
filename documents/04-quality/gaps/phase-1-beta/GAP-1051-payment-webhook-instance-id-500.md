# GAP-1051: PaymentWebhookController 500 — payment_webhook_logs.instance_id NOT NULL on public webhook

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend (kiteclass-core)
**Found:** 2026-06-07 (P3 G3 carve-out runtime walk C4 — discovery per `discovery-to-gap-inline-filing.md`)
**Affects:** `kiteclass-core` `module/payment/controller/PaymentWebhookController` + `payment_webhook_logs` table

## Problem

Khi walk carve-out route C4 (`/api/v1/payments/webhook/{vnpay|momo|zalopay}`) qua gateway :9000, route đến đúng kiteclass-core (TenantResolver-skip OK) NHƯNG controller trả **HTTP 500** thay vì 400/202 graceful.

Root cause (từ kiteclass-core logs):
```
org.postgresql.util.PSQLException: ERROR: null value in column "instance_id"
  of relation "payment_webhook_logs" violates not-null constraint
  Detail: Failing row contains (2, null, null, MOMO, {}, null, f, f,
  MoMo integration not implemented yet, ...).
```

Webhook là public (payment provider callback, no JWT / no subdomain / no tenant context). Controller cố INSERT `payment_webhook_logs` để audit attempt NHƯNG `instance_id` chưa được resolve server-side từ transaction reference → NOT NULL constraint violation → 500. momo/zalopay hiện là stub ("not implemented yet"); vnpay GET cùng root cause.

## Root Cause

`payment_webhook_logs.instance_id` declared NOT NULL (kiteclass_shared schema) nhưng webhook log insert path xảy ra TRƯỚC khi tenant được resolve từ txn-ref (hoặc không resolve được vì payload rỗng/invalid). Phase 1.5 payment integration chưa wire tenant-resolution-from-txn.

## Proposed Fix

Một trong các hướng (defer Phase 1.5 payment wave):
1. Resolve `instance_id` từ payment transaction reference TRƯỚC khi log (đúng cho real webhook có txn-ref hợp lệ), OR
2. Cho phép `payment_webhook_logs.instance_id` nullable cho unresolved/invalid webhook + return graceful 400/202 thay vì 500, OR
3. Validate payload + return 400 trước khi attempt INSERT (reject malformed webhook sớm).

## Acceptance Criteria

- [ ] Public webhook với payload invalid/empty → 400/202 graceful (NOT 500)
- [ ] Real webhook (valid txn-ref) → resolve instance_id + log thành công
- [ ] momo/zalopay stub trả response rõ ràng ("not implemented") thay vì 500

## Related

- Discovered in: P3 G3 carve-out runtime walk C4 — `documents/04-quality/audits/architecture/2026-06-07-gateway-carveout-runtime-walk.md`
- Routing parent: GAP-1049 (C4 routing fixed; this is downstream controller robustness, không phải routing)
- Phase 1.5 payment scope — webhooks (vnpay/momo/zalopay) chưa trong Phase 1 BETA scope; P3 defer
