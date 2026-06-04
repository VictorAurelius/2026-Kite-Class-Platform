# GAP-943: VietQR account_number + account_name application.yml empty default overrides Java @Value fallback (GAP-939 fix incomplete)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 G1 walk — Step 2 Payment row inspection post-create)
**Affects:** `kitehub/kitehub-subscription/src/main/resources/application.yml` payment.vietqr config + `VietQRService.@Value` defaults

## Problem

PR #2153 (GAP-939 fix, merged main `250a90c9`) added 3 explicit getter calls in `PaymentService.java` + `SubscriptionService.createPendingPayment` + `SubscriptionRenewalService`:
- `vietQRService.getBankCode()`
- `vietQRService.getAccountNumber()`
- `vietQRService.getAccountName()`

`VietQRService.java` has `@Value` defaults:
```java
@Value("${payment.vietqr.bank-code:VCB}") private String bankCode;
@Value("${payment.vietqr.account-number:1234567890}") private String accountNumber;
@Value("${payment.vietqr.account-name:CONG TY KITECLASS}") private String accountName;
```

BUT `application.yml` overrides those defaults với EMPTY string fallback:
```yaml
payment:
  vietqr:
    account-number: ${VIETQR_ACCOUNT_NUMBER:}    # ← env var OR EMPTY STRING
    account-name: ${VIETQR_ACCOUNT_NAME:}        # ← env var OR EMPTY STRING
```

Spring `@Value` precedence: YAML wins over Java `:default`. Result: `getAccountNumber()` returns `""`, `getAccountName()` returns `""`.

G1 walk evidence:
```
POST /api/platform/subscriptions BASIC → HTTP 201 (post V62 GAP-942 fix)
Payment row: bank_code=VCB ✅, account_number='' ❌, account_name='' ❌
```

Owner sees QR code without account info — cannot complete VietQR transfer in real flow.

## Root Cause

PR #2153 cross-flow sweep stopped at Java code level. Did not sweep `application.yml` for `${VIETQR_*}` empty-default overrides. Sister precedent: PR #2150 (admin payment controller) added `PAYMENT_MOCK_MODE=true` default to YAML — same pattern of YAML-side default population was needed for VIETQR_ACCOUNT_NUMBER/NAME but missed.

Per `cross-flow-bug-class-sweep.md` §1: when fixing a config-shape bug class, sweep should include all config layers (Java `@Value`, application.yml, application-{profile}.yml, env vars, docker-compose overrides).

## Proposed Fix

Update `kitehub/kitehub-subscription/src/main/resources/application.yml`:

```yaml
payment:
  vietqr:
    account-number: ${VIETQR_ACCOUNT_NUMBER:1234567890}
    account-name: ${VIETQR_ACCOUNT_NAME:CONG TY KITECLASS}
```

Match Java `@Value` defaults so dev/local stack has working account info without env var setup. Production overrides via env vars when real KiteHub account number is provisioned.

## Acceptance Criteria

- [ ] application.yml `account-number` default = `1234567890` (matches VietQRService.java line 14)
- [ ] application.yml `account-name` default = `CONG TY KITECLASS` (matches VietQRService.java line 17)
- [ ] Rebuild kitehub-subscription + verify new Payment row has populated bank_code + account_number + account_name without manual UPDATE
- [ ] Cross-flow sweep cùng PR: `grep -rE "VIETQR_(ACCOUNT|BANK)" kitehub/ kiteclass/ docker-compose*.yml infrastructure/` → no other empty-default overrides
- [ ] G1 walk Step 2 re-verify: POST /api/platform/subscriptions BASIC → Payment row populated without workaround

## Related

- Triggered by: Wave flow-kh3 G1 walk Step 2 Payment inspection 2026-06-04
- Caused by: PR #2153 GAP-939 fix incomplete (Java getter added but YAML default not updated)
- Sister sweep precedent: PR #2150 added PAYMENT_MOCK_MODE=true YAML default (same pattern)
- Rule cite: `cross-flow-bug-class-sweep.md` §1 — sweep all config layers when fixing config-shape bug class
