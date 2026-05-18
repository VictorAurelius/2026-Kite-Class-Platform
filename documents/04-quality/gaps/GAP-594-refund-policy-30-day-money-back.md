# GAP-594: Refund policy 30-day money-back doc (post-launch)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (defer Wave 88+ launch readiness)
**Domain:** Legal / Ops / Content
**Phase:** phase-1.5-paid
**Found:** 2026-05-15 (Wave 86 Bucket A benchmark-vn-saas-edu Q10)
**Affects:** Post-rc1 v1.0.0 GA refund handling

## Problem

Industry benchmark Q10:
- 30-day money-back → +21% sales lift
- Refund request rate ~12%
- Microsoft Azure 30d cancel; common SaaS pattern
- Complex products (B2B SaaS) → 30-day appropriate (vs 7d for simple)

Pre-rc1 beta cohort = FREE invite (no payment) → refund N/A immediately. **Post-rc1 (v1.0.0 GA) MUST lock refund policy** trước khi accept first paid customer.

## Root Cause

Wave 86 scope = rc1 tag + 5 beta cohort invite. Refund policy không scope vào Wave 86 vì beta free.

## Proposed Fix

1. **Refund policy doc** `documents/01-business/legal/refund-policy.md`:
   - 30-day money-back guarantee từ payment date
   - Eligible cases: dissatisfaction, missing feature claimed, technical inability to use
   - Excluded cases: > 30 days, partial-month, after explicit "no refund" acknowledgment
   - Process: support@kitehub.me → review 3-5 ngày → refund via original payment method
2. **TOS reference**: TOS link to refund policy doc
3. **Pricing page**: visible "30-ngày hoàn tiền không lý do" badge
4. **Ops process** `documents/05-guides/operations/refund-handling-runbook.md`:
   - Receive request → validate eligibility → mark account → process Stripe refund
   - Audit log every refund với reason

## Acceptance Criteria

- [ ] Refund policy doc shipped
- [ ] TOS references policy
- [ ] Pricing page badge live
- [ ] Ops runbook shipped
- [ ] Defer to Wave 88+ launch readiness scope

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q10 + §6 GAP-NEW-7
- Wave 88+ launch readiness scope

## Scope Refinement (2026-05-18 audit)

- **Kept in scope unchanged:** 30-day money-back specific policy text (Wave 86 Bucket A benchmark Q10 +21% sales lift confirmation)
- **Newly paired same-phase:** GAP-629 (manual refund SOP), GAP-630 (evidence storage), GAP-183 (high-level policy)
- **Implementation note:** since KiteHub non-PSP, "money-back" = Owner manual bank transfer; KiteHub UI shows refund record + PH confirmation receipt only

## Log

- **2026-05-18** — Scope confirmed (no major change) per outside-in audit Wave 93. This gap remains: 30-day money-back specific policy document. Now explicitly paired with GAP-183 (high-level refund policy) + GAP-629 (manual refund workflow SOP) + GAP-630 (evidence storage). All flow off-platform (Owner-side bank transfer); KiteHub tracks audit + policy enforcement only.
