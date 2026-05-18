# GAP-026: Trial / Freemium AI Mechanics

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Product / Billing
**Detected:** 2026-04-14 (simulation)

## Problem

Không rõ cơ chế AI cho trial/free tenants:

- ❌ Trial tenant (14 days) được bao nhiêu AI calls total?
- ❌ Trial expires → branding stays hay reset?
- ❌ Trial → paid conversion: preserve branding?
- ❌ FREE tier sau trial: 3/day có đủ không?
- ❌ Không có "demo data" để free users experience AI branding
- ❌ Không có grace period khi trial gần hết

**Impact:** Trial UX confusing → conversion rate thấp.

## Proposed Fix

### 1. Trial Period AI Budget

```java
public class TrialAIBudget {
  // Generous for trial để tenant experience feature
  TRIAL:
    - 50 AI generations total (not daily, total for 14 days)
    - All features unlocked (image gen, analysis, text)
    - Quality: same as PREMIUM

  FREE (post-trial):
    - 3/day (as current)
    - Text only (no image gen on FREE)
    - Quality: standard template

  PRO: 10/day
  PREMIUM: 50/day
  ENTERPRISE: unlimited
}
```

### 2. Trial Expiry Handling

Khi trial hết:
```
Branding hiện tại: keep visible (đã DEPLOYED)
AI regeneration: disabled (hit quota)
Tenant action:
  [Upgrade to PRO] — 299k/month
  [Keep on FREE] — 3 regen/day, no image gen
  [Export branding + delete account]
```

**Quan trọng:** KHÔNG delete branding khi trial expire → giữ UX tốt, tenant dễ convert.

### 3. Trial → Paid Conversion

```java
public void onTrialToPaid(String tenantId, Tier newTier) {
  // 1. Reset daily quota
  // 2. Preserve all branding assets
  // 3. Unlock premium templates
  // 4. Send congrats email
}
```

### 4. Demo Sandbox cho FREE Users

```
/demo/ai-branding (public, no auth)
  ↓
Interactive demo với sample tenant "demo-school"
  ↓
User click around, try wizard steps
  ↓
CTA: "Sign up to try with YOUR logo"
```

Lets potential users experience trước khi sign up.

### 5. Grace Period

- Trial day 12 → email "Còn 2 ngày + last chance regenerate"
- Trial day 14 → email "Hết trial, giảm 20% nếu upgrade hôm nay"
- Post-expire: 3-day grace period với FREE limits trước khi enforce

### 6. Trial Activation Tips

Email sequence:
- Day 0: Welcome + wizard link
- Day 1: "Hoàn thành branding?" nhắc nhở
- Day 3: "Xem template gallery"
- Day 7: "Còn 7 days — try advanced features"
- Day 12: "Còn 2 ngày — upgrade now"

## Acceptance Criteria

- [ ] Trial AI budget configurable (50 total, 14 days)
- [ ] Trial expiry không delete branding
- [ ] Upgrade flow preserves all data
- [ ] Demo sandbox page cho public
- [ ] Email sequence cho trial activation
- [ ] Grace period sau trial expire
- [ ] Metric: trial → paid conversion rate tracked
- [ ] A/B test: 50 AI calls vs 30 vs 100 → find optimal

## Dependencies

- GAP-017 (billing integration) — upgrade flow
- kitehub-email service — email sequence
- kitehub-subscription — trial lifecycle

## Log

- 2026-04-14 — Conversion funnel gap identified
