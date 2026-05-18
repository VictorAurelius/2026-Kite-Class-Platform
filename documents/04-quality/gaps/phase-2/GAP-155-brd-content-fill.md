# GAP-155: BRD Content Fill (Phase 2 of GAP-150)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (business-logic tier per `meta-gap-priority.md` §3 — extends Phase 1 BRD scaffold)
**Domain:** Business / BRD / Stakeholder engagement
**Found:** 2026-04-29 (filed by Agent A of Wave Business Correctness during GAP-150 closure)
**Affects:** All per-domain `documents/01-business/*/rules.md` (consume BRD values), pricing decisions, compliance posture, GTM execution, NFR commitments
**Parent:** GAP-150 (BRD skeletons — Phase 1, DONE 2026-04-29 PR #651)

## Problem

GAP-150 shipped 5 BRD skeleton docs with section structure + frontmatter + TODO markers. Each skeleton has placeholders that require **stakeholder-driven content** to become operationally useful:

| File | Content needed | Stakeholder |
|------|---------------|-------------|
| `business-objectives.md` | Final OKR numbers Q2/Q3 2026, KPI targets, north-star metric value | PM + Business Lead |
| `compliance-scope.md` | Legal text per VN framework (PDPL 2023, MoET, Cybersecurity, Labor, Consumer Protection, Tax, K-12) | External counsel + Legal-aware PM |
| `pricing-model.md` | Tier prices VND, AI quota numbers, discount % policy, upgrade flow | Finance + PM + market analysis |
| `nfr-catalog.md` | SLA targets per tier, RTO/RPO, perf budget P50/P95/P99 numbers, scalability targets | Architect + SRE + PM |
| `go-to-market.md` | Pilot customer list, marketing budget, launch timeline dates, funnel stage targets | Sales + Marketing + PM |

Without these values, per-domain `01-business/*/rules.md` cannot trace business decisions to BRD source — they remain assumption-driven.

## Root Cause

GAP-150 was Phase-1-scoped intentionally to unblock skeleton structure without blocking on stakeholder availability. Phase 2 requires:
- Stakeholder availability (Wave 0 stakeholder sync still pending per `00-brd/README.md`)
- Paying customer #1 acquisition (informs realistic pricing + GTM)
- External counsel engagement (informs compliance text)

These are operational gates, not in-PR-deliverable.

## Proposed Fix

5 sub-tasks, can ship as separate PRs or batch into themed clusters:

### Sub-task A — Business objectives (PM workshop)
- [ ] Run OKR workshop with PM + Business Lead
- [ ] Replace TODO markers in `business-objectives.md` §OKRs Q2/Q3 with concrete numbers
- [ ] Define north-star metric (single number that matters most)
- [ ] Update `status: skeleton` → `status: draft` in frontmatter
- [ ] Stakeholder sign-off recorded in §Log

### Sub-task B — Compliance scope (legal counsel)
- [ ] Engage external VN counsel (legal scout)
- [ ] Per framework (PDPL 2023, MoET, Cybersecurity, Labor, Consumer Protection, Tax, K-12), counsel produces requirement summary
- [ ] Replace TODO markers in `compliance-scope.md` with legal text
- [ ] Cross-link to `business-logic-review.md` §3 compliance check requirements (per GAP-049)
- [ ] Update `status: skeleton` → `status: draft`

### Sub-task C — Pricing model (Finance + market)
- [ ] Competitor pricing analysis (VN K-12 SaaS landscape)
- [ ] Finance models tier pricing (Free/Pro/Premium/Enterprise) with break-even projection
- [ ] AI quota allocation per tier — finalize from current placeholders (FREE 3/day, PRO 10/day, PREMIUM 30/day, ENTERPRISE unlimited per `ai-branding-guidelines.md` §4.3)
- [ ] **Reconcile pricing tier naming inconsistency** flagged by Agent A in `pricing-model.md` §9: FREE/PRO/PREMIUM/ENTERPRISE (tier doc) vs FREE/BASIC/PREMIUM/ENTERPRISE (`ai-branding-guidelines.md` §2.5) — single source of truth required
- [ ] Discount policy (annual / volume / education non-profit)
- [ ] Update `status: skeleton` → `status: draft`

### Sub-task D — NFR targets (Architect + SRE)
- [ ] Architect + SRE workshop on SLA, RTO/RPO, perf budgets
- [ ] Replace TODO markers in `nfr-catalog.md` with concrete numbers per tier
- [ ] Cross-link to `output-review-mandate.md` §3 NFR row + `documents/04-quality/audits/performance/` baselines
- [ ] WCAG AA target confirmed per `ai-branding-guidelines.md` §5
- [ ] Update `status: skeleton` → `status: draft`

### Sub-task E — Go-to-market (Sales + Marketing)
- [ ] Pilot customer list: target 3-5 schools for paid customer #1 acquisition
- [ ] Marketing budget allocation
- [ ] Launch timeline (sprint-aligned with current ROADMAP)
- [ ] Funnel stage targets: discovery → trial signup → wizard completion → conversion → renewal
- [ ] Update `status: skeleton` → `status: draft`

## Acceptance Criteria

- [ ] All 5 BRD files transition `status: skeleton` → `status: draft` (no `approved` until full stakeholder sign-off cycle, separate gap)
- [ ] All TODO markers in 5 BRD files replaced with concrete content
- [ ] Pricing tier naming reconciliation closed (one canonical scheme across `pricing-model.md`, `ai-branding-guidelines.md`, all per-domain rules.md)
- [ ] Each BRD file has §Log entry citing stakeholder + date + sub-task PR
- [ ] `00-brd/README.md` Current Gaps table reflects each file's `status: draft` post-fill
- [ ] Per-domain `01-business/*/rules.md` files reviewed for traceability to filled BRD (GAP-156 audit consumes this)

## Dependencies

- **Blocks:** GAP-156 Sub-task D (5 representative business rules sign-offs — needs filled BRD as compliance reference)
- **Blocked by:** Wave 0 stakeholder sync; paying customer #1 acquisition; legal counsel engagement
- **Sister:** GAP-154 (umbrella for 22 additional BRD docs not in scope here — TOS, Privacy, AUP, Refund, Data Retention, Child Protection)

## Out of Scope

- Filling 22 additional BRD docs (GAP-154 covers — separate scope)
- `status: draft` → `status: approved` flip (requires stakeholder sign-off PR per BRD, separate gap per file once draft is complete)
- Per-domain rules.md updates to cite filled BRD values (GAP-156 covers as part of audit)

## Related

- **Parent:** GAP-150 (BRD skeletons — Phase 1)
- **Siblings:** GAP-156 (business rules compliance audit — consumes Phase 2 output)
- **Cross-cluster:** GAP-152 (persona review execution — consumes BRD pricing/objectives for validation)
- **Umbrella:** GAP-154 (22 additional BRD docs)
- **Rules:** `meta-gap-priority.md` §3 — BL tier; `output-review-mandate.md` §3 — review standards per BRD type
- **Wave:** `documents/03-planning/waves/wave-2026-04-29-business-correctness.md` (parent wave that filed this gap)

## Log

- 2026-04-29 — Filed by Agent A during GAP-150 PR #651 closure. Phase 2 of GAP-150 BRD content (skeleton → draft transition with stakeholder-driven values). Tracked separately because each sub-task requires operational stakeholder engagement (workshops, legal counsel, market analysis, customer acquisition) that's not in-PR-deliverable.
