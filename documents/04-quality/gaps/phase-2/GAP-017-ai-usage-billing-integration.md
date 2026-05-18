# GAP-017: AI Usage → Billing Integration

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Billing / AI / Product
**Detected:** 2026-04-14 (simulation)

## Problem

Rate limit hiện tại chỉ là **daily quota per tier**, không link với billing/subscription system. Vấn đề:

- Tenant hit quota → show error message, **không upsell flow**
- Không cost attribution per tenant → hard to forecast infrastructure costs
- Không có "pay-per-generation" cho Enterprise cần burst capacity
- Không tracking revenue impact của AI feature

## Evidence

- `AIRateLimitService` chỉ check daily quota, không trigger billing events
- Không có entity `AIUsageBilling`, `AICostEntry`
- Không có integration với Stripe/payment gateway cho AI usage
- Dashboard tenant không show "AI usage this month + cost"

## Proposed Fix

### 1. Usage Tracking → Billing Events

```java
@Entity
public class AIUsageBillingRecord {
  Long id;
  String tenantId;
  String jobId;
  AIFeatureType feature;  // LOGO_ANALYSIS, IMAGE_GEN, TEXT_GEN
  Integer computeUnits;   // normalized cost unit
  BigDecimal estimatedCost;
  Timestamp createdAt;
  String billingPeriod;   // YYYY-MM
}
```

### 2. Upsell Flow

Khi tenant hit quota:
```tsx
<AIQuotaExceededModal>
  Bạn đã dùng hết 3/3 AI generations hôm nay
  [Upgrade to PRO] — 10 gens/day — 299k/tháng
  [Upgrade to PREMIUM] — 50 gens/day — 799k/tháng
  [Chờ 24h (reset)]
</AIQuotaExceededModal>
```

### 3. Enterprise Pay-per-Use

- Enterprise có base quota
- Vượt quota → pay-per-generation billing
- Invoice cuối tháng tổng AI usage

### 4. Admin Cost Dashboard

- Per-tenant AI cost monthly
- Per-tier aggregate
- Cost forecast based on growth rate
- Outlier detection (tenant tiêu bất thường)

## Acceptance Criteria

- [ ] `AIUsageBillingRecord` entity + DB table
- [ ] Quota exceeded → upsell modal với CTAs
- [ ] Enterprise pay-per-use billing flow
- [ ] Admin cost dashboard
- [ ] Monthly invoice generation cho AI overage
- [ ] Integration test: hit quota → upsell → upgrade → continue using

## Dependencies

- GAP-005 (rate limit infrastructure)
- kitehub-subscription service (payment flows)

## Log

- 2026-04-14 — Phát hiện qua simulation tenant journey
