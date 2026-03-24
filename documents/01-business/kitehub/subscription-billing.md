# Subscription & Billing

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| SUB-01 | FREE tier cannot have paid subscription | N/A | (hardcoded in SubscriptionService) |
| SUB-02 | Billing cycles | MONTHLY (30 days), ANNUALLY (365 days) | BillingCycle enum |
| SUB-03 | Auto-renew default | true | request.getAutoRenew() default |
| SUB-04 | Grace period after expiration | 3 days | `kitehub.subscription.grace-period-days` |
| SUB-05 | Expiration warning days | 7, 3, 1 days before | `kitehub.subscription.warning-days` |
| SUB-06 | Upgrade direction | Only to higher tier (ordinal comparison) | (hardcoded) |
| SUB-07 | Upgrade timing | Immediate with prorated charge | (hardcoded) |
| SUB-08 | Downgrade direction | Only to lower tier | (hardcoded) |
| SUB-09 | Downgrade timing | Applied at end of current billing cycle | pendingTier field |
| SUB-10 | Prorated calculation | (newPrice - oldPrice) / cycleDays * daysLeft | (hardcoded formula) |
| SUB-11 | Default payment method | VietQR | PaymentMethod.VIETQR |
| SUB-12 | Cancel immediate | Sets expiresAt to now, autoRenew=false | (hardcoded) |
| SUB-13 | Cancel at end of cycle | Keeps expiresAt, sets autoRenew=false | (hardcoded) |
| SUB-14 | One active subscription per instance | Validated on create | (hardcoded) |
| SUB-15 | Currency | VND | payment.setCurrency("VND") |
| SUB-16 | Expiring subscriptions query window | 30 days ahead | (hardcoded) |

## Flow

### Subscription Creation
1. Validate instance exists
2. Check no active subscription already exists for instance
3. Reject FREE tier (cannot create paid subscription)
4. Calculate price from tier + billing cycle
5. Create subscription (status=ACTIVE, autoRenew=true by default)
6. Update instance status to ACTIVE, link subscriptionId
7. Send subscription-created email

### Upgrade Flow
1. Validate new tier ordinal > current tier ordinal
2. Validate subscription status = ACTIVE
3. Calculate prorated charge: `(newPrice - oldPrice) / cycleDays * daysLeft`
4. Update subscription tier and price immediately
5. Create PENDING payment record (VietQR, VND)
6. Link pendingPaymentId to subscription

### Downgrade Flow
1. Validate new tier ordinal < current tier ordinal
2. Validate subscription status = ACTIVE
3. Set pendingTier on subscription (no immediate change)
4. Tier change applies when current cycle expires

### Cancellation Flow
1. If already CANCELLED, skip (idempotent)
2. Immediate: set expiresAt=now, autoRenew=false, status=CANCELLED
3. End-of-cycle: keep expiresAt, set autoRenew=false, status=CANCELLED

### Expiration Processing (Schedulers)
1. **9:00 AM** - SubscriptionExpirationChecker.checkExpiringSubscriptions()
   - Find ACTIVE subscriptions expiring within 7 days
   - Send renewal-reminder at warning days [7, 3, 1]
2. **10:00 AM** - SubscriptionExpirationChecker.processExpiredSubscriptions()
   - Mark past-due ACTIVE subscriptions as EXPIRED
   - Suspend instances where grace period (3 days) has ended

## Emails

| Trigger | Template | Method |
|---------|----------|--------|
| Subscription created | subscription-created | sendSubscriptionCreatedEmail() |
| 7/3/1 days before expiry | subscription-renewal-reminder | sendRenewalReminder() |
| Instance suspended after grace period | subscription-suspended | sendSuspensionNotification() |
| Subscription expired | subscription-expired | sendSubscriptionExpiredEmail() |

## Config

```yaml
kitehub:
  subscription:
    grace-period-days: 3        # Days after expiry before suspension
    warning-days: 7,3,1         # Days before expiry to send reminders

# Payment
payment:
  vietqr:
    api-url: ${VIETQR_API_URL:https://api.vietqr.io/v2/generate}
    api-key: ${VIETQR_API_KEY:}
    mock-mode: ${PAYMENT_MOCK_MODE:true}
    bank-code: ${VIETQR_BANK_CODE:VCB}
    account-number: ${VIETQR_ACCOUNT_NUMBER:}
    account-name: ${VIETQR_ACCOUNT_NAME:}
    template: ${VIETQR_TEMPLATE:compact}

# Scheduler cron expressions
# checkExpiringSubscriptions:    0 0 9 * * *   (daily 9:00 AM)
# processExpiredSubscriptions:   0 0 10 * * *  (daily 10:00 AM)
```
