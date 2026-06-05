# GAP-976: SePay webhook — Apikey auth + payload adapter + idempotency

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3-2 SePay integration plan)
**Affects:** `kitehub-subscription` PaymentWebhookController + PaymentService + V64 migration

## Problem

The existing `PaymentWebhookController` used a generic HMAC-SHA256 body-signature
scheme with a `sorted-key=value&` payload format. SePay (https://sepay.vn)
authenticates via an `Authorization: Apikey <key>` header and sends a SePay-specific
payload shape. The webhook also had no idempotency guard — a replayed notification
could double-process a payment.

## Proposed Fix

Rewrite the controller (keep class + path `/api/platform/webhooks/payment`) to verify
the Apikey header (constant-time), adapt the SePay payload, extract `txnRef` from the
description, locate the payment by exact match, and add idempotency (early-return on a
replayed SePay transaction id, backed by a partial UNIQUE index).

## Acceptance Criteria

- [x] `Authorization: Apikey <key>` verified against `kitehub.payment.sepay.api-key` (constant-time `MessageDigest.isEqual`)
- [x] SePay payload adapter (`id`/`transferType`/`transferAmount`/`description`)
- [x] `txnRef` extracted via regex + `findByTxnRef` exact-match (cross-tenant collision guard)
- [x] Idempotency: replayed SePay `id` → HTTP 200 early-return, no double-process
- [x] V64 partial UNIQUE index on `payments.transaction_id`
- [x] 401 on missing/wrong Apikey; 400 on orphan txnRef; 200 on success/ignored/replay
- [x] Webhook controller test rewritten for SePay + service idempotency unit tests

## Related

- Implemented in: Wave flow-kh3-2 Bucket B (inline)
- Cross-flow sweep: `findByContent` legacy `processPaymentWebhook` kept (unused by new controller)
- Sister: [[GAP-975]] (provides txnRef), [[GAP-974]] (activation email)
