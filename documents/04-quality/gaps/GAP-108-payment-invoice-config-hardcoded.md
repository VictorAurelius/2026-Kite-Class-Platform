# GAP-108: Payment-Invoice Rules Document 12 Config Keys But Code Hardcoded (Persistent Drift)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** KiteClass / Payment & Invoice / Config Externalization
**Found:** 2026-04-19 (business-logic audit; originally flagged 2026-03-23 business-gap-check, not yet fixed)
**Affects:** kiteclass-core invoice + payment modules, tenant customization, Wave 5 K-12 payment flows

## Problem

`documents/01-business/kiteclass/payment-invoice/rules.md:89-100` documents **12 config keys**:

```
invoice.payment-term-days: 7
invoice.late-fee-percent-per-day: 0.1
invoice.late-fee-max-percent: 10
invoice.installment.min-amount: 500000
invoice.installment.min-invoice: 5000000
invoice.installment.max-periods: 12
invoice.installment.overdue-cancel-days: 15
payment.gateway-timeout-minutes: 15
payment.minimum-amount: 100000
payment.daily-limit.cash: 50000000
payment.daily-limit.bank: 500000000
payment.daily-limit.gateway: 200000000
```

Verification:
```
$ grep "invoice\.\|payment\.gateway-timeout\|payment\.minimum-amount\|payment\.daily-limit" \
    kiteclass/kiteclass-core/src/main/resources/application.yml
# 0 hits

$ grep -n "LATE_FEE_RATE\|0.001\|SOFT_DELETE_GRACE" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/
InvoiceServiceImpl.java:64:    private static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.001");
```

Note: `LATE_FEE_RATE = 0.001` trong code = 0.1%/day = correct per rules.md BR-INV-004 "Late fee 0.1%/day". Giá trị ĐÚNG nhưng không externalized per docs.

Similar batch items (rolled into this gap):
- **Marketing config keys** (`marketing.contact.message.max-length`, `marketing.lead.sources`, `marketing.landing.color.pattern`) cũng documented nhưng không exist trong application.yml
- **Storage cleanup grace** (`StorageCleanupScheduler.java:40` = `SOFT_DELETE_GRACE_PERIOD_DAYS = 30` hardcoded, per rules.md should be configurable)

## Root Cause

Carry-forward từ business-gap-check 2026-03-23 (#12 trong KiteClass report: "Late fee rate configurable | hardcoded"). 27 ngày trôi qua, không fix. Root cause:
- GAP-049 (business-correctness review) flagged nhưng không spawn actionable gap với priority
- Payment module chưa có tenant customization yêu cầu → hardcoded "works" cho 1-tenant-size-fits-all
- Tests không verify config injection, chỉ check end-to-end behavior

Risk khi landing Wave 5 (K-12 + parent portal full): tenants khác nhau có late-fee policy khác nhau (school vs center vs language-center), nhưng hardcoded không cho phép.

## Proposed Fix

Externalize 12 invoice/payment + 3 marketing config keys vào `kiteclass-core/application.yml`, tạo 2 `@ConfigurationProperties`:

### InvoiceProperties (prefix: `invoice`)
```java
@ConfigurationProperties(prefix = "invoice")
public record InvoiceProperties(
    int paymentTermDays,
    BigDecimal lateFeePercentPerDay,
    BigDecimal lateFeeMaxPercent,
    Installment installment
) {
    public record Installment(long minAmount, long minInvoice, int maxPeriods, int overdueCancelDays) {}
}
```

### PaymentProperties (prefix: `payment`)
```java
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
    int gatewayTimeoutMinutes,
    long minimumAmount,
    DailyLimit dailyLimit
) {
    public record DailyLimit(long cash, long bank, long gateway) {}
}
```

Wire vào `InvoiceServiceImpl`, `PaymentServiceImpl` via constructor injection. Remove `LATE_FEE_RATE` constant.

Tests: verify config override works (`@TestPropertySource(properties = "invoice.late-fee-percent-per-day=0.5")` → assert applied).

## Acceptance Criteria
- [ ] 12 config keys existe trong `kiteclass-core/application.yml`
- [ ] `InvoiceProperties` + `PaymentProperties` records exist với `@ConfigurationProperties`
- [ ] `InvoiceServiceImpl` không có hardcoded `LATE_FEE_RATE` constant
- [ ] `StorageCleanupScheduler` dùng `storage.cleanup.grace-period-days` config key
- [ ] Unit test cho config override cho late-fee
- [ ] Marketing 3 config keys + wiring cho `ContactMessage.maxLength`, `Lead.source` enum, `LandingPage.color` regex validator
- [ ] Rules.md "Log" section reference 2026-04-19 audit + GAP-108 fix PR

## Related
- Audit report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
- Previous audit (2026-03-23): `business-gap-check-2026-03-23-kiteclass.md` §Configuration #12, #13 (carry-forward)
- Related gap: GAP-049 (business-correctness review — broader stakeholder validation)
- Marketing rules.md drift included as batch (no separate gap)

## Scope Refinement (2026-05-18 audit)

Per outside-in audit Wave 93 — 3-agent convergence:
- **Kept in scope (P1):** Config keys cho QR display metadata, reconcile workflow tier-multiplier, manual mark-paid behaviors
- **Moved to GAP-634:** VAT eInvoice config (MISA MeInvoice partnership integration)
- **Moved to GAP-633:** Payment processor config (cancelled Phase 1.5; defer Phase 2 VietQR EduPay partnership)
- **Out of scope:** Self-build payment broker engine (KiteHub stays non-PSP per VN compliance constraint surfaced by benchmark agent)

## Log

- **2026-05-18** — Scope refined per outside-in audit Wave 93 (`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md`). Original scope (12 hardcoded keys for payment-invoice engine self-build) re-scoped narrower: 12 keys cover QR display metadata + reconcile workflow only, NOT processor integration. VAT eInvoice config moved to GAP-634 (MISA MeInvoice partnership). Related: GAP-625/626/627 P0 foundation + GAP-628 batch reconcile.
