# Subscription & Billing — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `kitehub.subscription`

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| SUB-01 | FREE tier không được tạo subscription | N/A | hardcoded SubscriptionService |
| SUB-02 | Billing cycles | MONTHLY (30 ngày), ANNUALLY (365 ngày) | BillingCycle enum |
| SUB-03 | Auto-renew mặc định | true | request.getAutoRenew() |
| SUB-04 | Grace period sau hết hạn | 3 ngày | `kitehub.subscription.grace-period-days` |
| SUB-05 | Warning days | 7, 3, 1 ngày trước hết hạn | `kitehub.subscription.warning-days` |
| SUB-06 | Upgrade: chỉ lên tier cao hơn | ordinal comparison | hardcoded |
| SUB-07 | Upgrade timing | Immediate + prorated charge | hardcoded |
| SUB-08 | Downgrade: chỉ xuống tier thấp hơn | ordinal comparison | hardcoded |
| SUB-09 | Downgrade timing | Cuối chu kỳ hiện tại | pendingTier field |
| SUB-10 | Prorated formula | (newPrice - oldPrice) / cycleDays * daysLeft | hardcoded |
| SUB-11 | Default payment method | VietQR | PaymentMethod.VIETQR |
| SUB-12 | Cancel immediate | expiresAt = now, autoRenew=false | hardcoded |
| SUB-13 | Cancel end-of-cycle | giữ expiresAt, autoRenew=false | hardcoded |
| SUB-14 | 1 subscription active per instance | validate on create | hardcoded |
| SUB-15 | Currency | VND | payment.setCurrency("VND") |
| SUB-16 | Expiring query window | 30 ngày tới | hardcoded |

## Config

```yaml
kitehub:
  subscription:
    grace-period-days: 3
    warning-days: 7,3,1

payment:
  vietqr:
    api-url: ${VIETQR_API_URL:https://api.vietqr.io/v2/generate}
    api-key: ${VIETQR_API_KEY:}
    mock-mode: ${PAYMENT_MOCK_MODE:true}
    bank-code: ${VIETQR_BANK_CODE:VCB}
    account-number: ${VIETQR_ACCOUNT_NUMBER:}
    account-name: ${VIETQR_ACCOUNT_NAME:}
    template: ${VIETQR_TEMPLATE:compact}
```
