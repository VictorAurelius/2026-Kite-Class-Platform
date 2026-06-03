# GAP-912: KH money type harmonize — `price_vnd`/`amount_vnd` BIGINT→NUMERIC + Long→BigDecimal

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-03 (Wave 14 D-KH defer — safe-max scope decision)
**Affects:** `kitehub-subscription` + `kitehub-platform` payment/subscription flow

## Problem

Wave 14 "hoàn thiện DB 100%" harmonized KC money columns to `NUMERIC(19,2)` (V86) but **deferred the KiteHub side** (D-KH) under the safe-max scope decision. KH `subscriptions.price_vnd` + `payments.amount_vnd` remain `BIGINT`; entities/DTOs remain `Long`.

Deferred because the Long→BigDecimal change ripples through the payment flow with **semantic gotchas** that need a full payment-flow test pass, not a tail-of-session edit:
- `payment.getAmountVnd().equals(amountVnd)` (PaymentService:189) — `BigDecimal.equals` is scale-sensitive → must become `.compareTo(...) != 0`
- `String.format("...%d+VND", amountVnd)` (VietQRService:63-64) — `%d` rejects `BigDecimal`
- `VietQRRequest.amount` field type ripple
- `PaymentWebhookController:65` payload `.longValue()` parse → `BigDecimal`
- `EmailServiceClient:172,187` `long amountVnd` params
- Mockito stubs in `PaymentServiceTest` / `SubscriptionServiceTest` returning `Long`

The deferred migration content already exists on branch `wave-14-bucket-d-deferred` as `V60__type_harmonize_kh.sql` (must be **renumbered** to next-free KH version — V59-V61 now consumed by Wave 14 C-KH; next free = V62 at time of writing).

## Proposed Fix

Dedicated wave (own PR) with full payment-flow verification per `api-contract-change-caller-sweep.md`:
1. Port `V60__type_harmonize_kh.sql` → next-free KH version (money→NUMERIC(19,2) + timestamp→TIMESTAMPTZ; `email_sent_log.sent_at` stays TIMESTAMP per immutable-index constraint).
2. Entities `Payment.amountVnd` + `Subscription.priceVnd` Long→BigDecimal (`@Column precision=19, scale=2`).
3. DTOs `CreatePaymentRequest`/`PaymentResponse`/`SubscriptionResponse` Long→BigDecimal.
4. Callers: PaymentService / VietQRService / PaymentWebhookController / EmailServiceClient + `VietQRRequest.amount` — fix arithmetic (`compareTo`), format specifiers, parse.
5. Mockito stubs swap to BigDecimal matchers.
6. `./mvnw test -f kitehub-subscription/pom.xml` (not just compile) PASS + `scripts/check-type-consistency.sh` exit 0 (KH columns NUMERIC).

## Acceptance Criteria

- [ ] KH `price_vnd`/`amount_vnd` columns NUMERIC(19,2); `scripts/check-type-consistency.sh` exit 0 (no `kitehub|...|money` rows)
- [ ] KH timestamp columns TIMESTAMPTZ (note: KH BaseEntity uses LocalDateTime — verify validate or migrate entity types)
- [ ] Entities + DTOs + callers BigDecimal; `BigDecimal.compareTo` replaces `.equals` for amount checks
- [ ] `./mvnw test -f kitehub-subscription/pom.xml` PASS (mocks swapped)
- [ ] `scripts/check-schema-drift.sh` PASS

## Related

- Deferred from: Wave 14 D-KH (this session 2026-06-03); sister GAP-883 (money cross-cluster, KC done)
- Migration source: branch `wave-14-bucket-d-deferred` V60 (renumber needed)
- Per `local-fix-production-parity-check.md`: AWS apply GAP-612-gated; ship code + verify post-restore
