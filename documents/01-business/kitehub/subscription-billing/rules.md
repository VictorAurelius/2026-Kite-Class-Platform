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

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — Luật Quản lý Thuế 2019; Nghị định 123/2020/NĐ-CP (e-invoice); Consumer Protection Law (refund + dispute window 24mo); Luật Giao dịch điện tử 2023.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Tax law amendment, e-invoice regulation, payment-gateway swap, tier pricing change.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
