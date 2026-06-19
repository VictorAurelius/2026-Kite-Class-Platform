# GAP-975: Dynamic VietQR txnRef + beta-amount override for SePay reconciliation

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3-2 SePay integration plan)
**Affects:** `kitehub-platform` Payment entity + `kitehub-subscription` PaymentService + V63 migration

## Problem

The SePay webhook (GAP-976) needs a deterministic, unique reference embedded in the
VietQR transfer memo to locate the exact payment via exact-match lookup. Previously
`PaymentService.createPayment` only generated a free-text `paymentContent`
(substring-matched), which is collision-prone across tenants. Phase 1 BETA also needs
a symbolic-amount override so beta testers move a token 10.000đ via a real bank
transfer instead of the full tier price.

## Proposed Fix

Add a `txnRef` field (`KH3SUB<8 hex>` derived from the payment id) with a partial
UNIQUE index, generate it in `createPayment`, and wire `kitehub.payment.beta-mode.*`
config to override `amountVnd` when enabled.

## Acceptance Criteria

- [x] `Payment.txnRef` field + V63 migration (`txn_ref VARCHAR(32)` + partial UNIQUE index)
- [x] `txn_ref = "KH3SUB" + paymentId[0:8].toUpperCase()` matching api-contract regex `KH3SUB[A-F0-9]{8}`
- [x] `beta-mode.enabled=true` → `amountVnd = override-amount-vnd` (default 10000); disabled → real amount
- [x] `PaymentRepository.findByTxnRef` exact-match query (no LIKE)
- [x] Mockito unit tests: txnRef format + beta override + real-amount-when-disabled

## Related

- Implemented in: Wave flow-kh3-2 Bucket A (inline)
- api-contract: `documents/01-business/kitehub/subscription-billing/api-contract.md`
- Sister: [[GAP-976]] (webhook consumes txnRef), [[GAP-944]]
