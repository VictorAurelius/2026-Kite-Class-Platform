# GAP-334: Multi-fee Structure (HP + Bán trú + Đồng phục + BHYT + BHTN + Quỹ PH) + Discount Rules

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-FIN-001

## Current State (verified 2026-05-04)

```bash
grep -rl "MultiFee\|fee.structure" kitehub/kitehub-billing/ --include="*.java"
```
Result: kitehub-billing has invoice scaffold but no multi-fee/discount/policy-discount.

## Problem

Real K-12 school has 6+ fee types per HS. Single-fee model infeasible. Discount rules theo HS chính sách (con thương binh, hộ nghèo) required by law.

## Proposed Fix

1. **FeeType enum:** TUITION, BOARDING, UNIFORM, HEALTH_INSURANCE, ACCIDENT_INSURANCE, PARENT_FUND, OTHER
2. **FeeSchedule:** monthly / semester / annual / one-time / per-actual-day (bán trú)
3. **DiscountRule:** policy-based (war-orphan 50%, low-income waive parent fund, sibling 10%)
4. **Per-student fee profile** computed at billing time

## Acceptance Criteria

- [ ] FeeType + FeeSchedule + DiscountRule entities
- [ ] Per-student fee profile generation
- [ ] Bán trú actual-day calculation from period attendance (GAP-323)
- [ ] Discount audit log (who applied, why, amount)
- [ ] Test: 7A 42 HS sinh 42 invoice với 6 fee types + 3 discount profiles
- [ ] Documentation 3-layer
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** GAP-323 (period attendance for bán trú)
- **Cross-cuts:** GAP-335 (public vs private compliance)
- **Wave plan:** Bucket D Stage 4

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
