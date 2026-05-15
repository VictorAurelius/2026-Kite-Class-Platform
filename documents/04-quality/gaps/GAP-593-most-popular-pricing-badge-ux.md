# GAP-593: "Most Popular" pricing badge UX enhancement

**Status:** 🔵 OPEN
**Priority:** 🟢 P3 (defer Wave 87+ Phase 1.5)
**Domain:** Frontend
**Phase:** phase-1.5-paid
**Found:** 2026-05-15 (Wave 86 Bucket A benchmark-vn-saas-edu Q3)
**Affects:** Pricing page conversion (post-rc1 public launch)

## Problem

Industry benchmark Q3:
- Tiered pricing avg 3.5 packages (low/mid/high)
- "Most Popular" badge reduces decision anxiety (social proof lever)
- Hidden pricing → bounce rate spike

Wave 85 pricing public (xác nhận từ wave plan §3 ref). Hiện thiếu "Most Popular" badge → decision anxiety không reduced → conversion rate damage post-rc1 public launch.

## Root Cause

Pricing page minimal UX, không có visual hierarchy emphasizing recommended tier.

## Proposed Fix

1. **Pricing component enhancement** `kitehub-frontend/src/components/pricing/pricing-card.tsx`:
   - Prop `isMostPopular: boolean`
   - Visual badge "🔥 Phổ biến nhất" top of card
   - Border accent color (primary brand)
   - Slight scale-up (105%) tower over siblings
2. **Apply to Gói Trial tier** (mid-tier = recommended):
   - `pricing-page.tsx` set `<PricingCard tier="trial" isMostPopular={true} />`
3. **A/B test framework** (defer Wave 88+): measure CTR delta with/without badge

## Acceptance Criteria

- [ ] PricingCard component supports isMostPopular
- [ ] Visual badge + accent border + scale shipped
- [ ] Mid-tier marked Most Popular
- [ ] Defer A/B test to Wave 88+
- [ ] Phase 1.5 scope confirmed (not blocking rc1)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q3 + §6 GAP-NEW-6
- Wave 86 NOT blocking — defer Wave 87+ Phase 1.5
