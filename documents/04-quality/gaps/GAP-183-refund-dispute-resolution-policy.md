# GAP-183: Refund + Dispute Resolution Policy

**Status:** 🟡 PARTIAL — Phase 1 skeleton SHIPPED 2026-04-29 (Wave Legal-BRD Phase 1.5, PR #694). Phase 2 (legal counsel content + payment processor alignment + contract templates + support team SOP) blocked-on stakeholder engagement → tracked GAP-154 umbrella per `gap-done-discipline.md` §3 PARTIAL exit-ramp.
**Priority:** 🔴 P0 (business-logic tier — **VN Consumer Protection Law mandate**)
**Domain:** Legal / BRD / Commercial
**Found:** 2026-04-20 (BRD simulation — GAP-154 Phase 1)
**Wave:** Wave 8 Business Governance
**Affects:** Payment processor onboarding, chargeback defense, customer churn handling, enterprise contracts

## Problem

No refund or dispute resolution policy. Blocks:
- **VN Consumer Protection Law 2023** (Law No. 19/2023/QH15) compliance — Article 14 requires clear refund terms
- Payment processor chargeback defense (without written policy, chargebacks auto-win for customer)
- Enterprise contract negotiations (procurement requires refund terms)
- Support team decisions (currently ad-hoc)

## Scope

Create `documents/00-brd/refund-dispute-resolution-policy.md`:

1. **Refund Eligibility Matrix**
   | Tier | Trial refund | Mid-cycle refund | Feature failure refund | Goodwill refund |
   |------|:-----------:|:---------------:|:---------------------:|:--------------:|
   | Free | N/A | N/A | N/A | N/A |
   | Pro | Pro-rated | Case-by-case | Full | Support discretion |
   | Premium | Pro-rated | Pro-rated | Full | Up to 100% |
   | Enterprise | Per contract | Per contract | Per contract | Per contract |
2. **Refund Process**
   - Request channel (email, in-app)
   - Information required
   - Response SLA (5 business days per VN law)
   - Refund method + timing (same payment method, 7-14 days)
3. **Non-Refundable Items**
   - Used services (classes held, certificates issued)
   - AI generation already delivered
   - Custom branding already approved
4. **Service Credits** (alternative to refund)
   - Eligibility
   - Calculation (linked to GAP-189 customer SLA)
   - Validity period
5. **Dispute Resolution Process**
   - Informal: support escalation (L1 → L2 → Support lead)
   - Formal: written complaint, 30-day response
   - Mediation (commercial mediation center)
   - Arbitration (VIAC — Vietnam International Arbitration Centre) for commercial contracts
   - Court (TAND HCMC or tenant's jurisdiction per contract)
6. **Consumer vs Commercial Customers**
   - Consumer (solo teacher, small center owner) — stronger protections per Consumer Law
   - Commercial (incorporated schools, chains) — contract-based
7. **Chargeback Handling** — response procedure, evidence collection
8. **Force Majeure** — how service interruptions handled (Force Majeure events vs provider fault)

## Acceptance Criteria

### Phase 1 (skeleton)

- [ ] `documents/00-brd/refund-dispute-resolution-policy.md` skeleton with 8 sections
- [ ] Refund eligibility matrix (tier × scenario)
- [ ] Process flow diagram description (request → review → decision → execution)
- [ ] Dispute escalation ladder
- [ ] Cross-references to GAP-185 (billing terms), GAP-189 (SLA credits), TOS GAP-180
- [ ] README link updated

### Phase 2 (content — legal counsel)

- [ ] Legal counsel review (VN Consumer Protection Law expertise)
- [ ] Support team SOP derived
- [ ] Payment processor alignment (VNPay/MoMo chargeback SLA)
- [ ] Contract templates for enterprise updated
- [ ] Status: `skeleton` → `approved`

## Out of Scope

- **Refund workflow implementation** — separate feature gap
- **Chargeback response automation** — operational tooling
- **Contract template redesign** — legal deliverable

## Dependencies

- GAP-154 umbrella
- GAP-180 TOS (dispute resolution clause references this)
- GAP-185 Billing Terms (refund calculation basis)
- GAP-189 Customer SLA (service credit eligibility)
- GAP-108 (payment/invoice config — hardcoded values need externalization first)
- Legal counsel + payment processor coordination

## Related

- Report: `brd-simulation-gap-finder-2026-04-20.md` §1.1 item K
- VN Law: **Law No. 19/2023/QH15** (Consumer Protection), Commercial Law 2005
- Rule: `.claude/rules/meta-gap-priority.md` §3

## Scope Refinement (2026-05-18 audit)

This gap remains P0 (legal exposure for paid market) but scope narrows:
- **Kept in scope:** Refund policy document (eligibility windows, dispute escalation, evidence requirements per VN consumer protection law)
- **Moved to GAP-629:** Detailed Owner-side workflow (UI + DB record + PH confirmation flow)
- **Moved to GAP-630:** Evidence storage (screenshot hash + metadata)
- **Out of scope:** Automated refund engine (KiteHub non-PSP — refund flow happens off-platform)
- **Paired:** GAP-594 (30-day money-back specific policy)

## Log

- **2026-05-18** — Scope refined per outside-in audit Wave 93. Original "build refund engine" rejected — KiteHub stays non-broker per `release-1-plan-2026.md` §4 + audit benchmark finding (PSP license risk). Refund = manual Owner-side bank transfer; KiteHub tracks SOP + audit trail only. Detailed workflow moved to GAP-629 (QR refund workflow SOP). This gap remains scope: high-level policy document (refund eligibility windows, dispute escalation paths, evidence requirements). Related: GAP-594 30-day money-back doc, GAP-629 SOP, GAP-630 evidence storage.
- **2026-04-29** — Phase 1 skeleton SHIPPED (Wave Legal-BRD Phase 1.5, PR #694 squash-merged commit `a491f517`). 384-line markdown file `documents/00-brd/refund-dispute-resolution-policy.md` với 11 sections (8 mandated + 3 governance). 4 tables (refund eligibility matrix 4×4 + dispute escalation ladder L1-L7 + service credits SLA breach tier + Phase 2 ownership matrix). 12 markdown cross-links: 5 sibling skeletons (TOS/AUP/Privacy/Retention/Billing) + 3 ai-branding-guidelines.md refs + GAP-189/108 planned. Frontmatter cites Law No. 19/2023/QH15 + Commercial Law 2005 + VIAC arbitration rules. Status flipped 🔵 OPEN → 🟡 PARTIAL by coordinator per `gap-done-discipline.md` §3 (Phase 1 AC items 1-6 fully met; Phase 2 AC items 7-11 tracked under GAP-154 umbrella).
- 2026-04-20 — Created as GAP-154 Phase 1 sub-gap. VN Consumer Protection Law 2023 mandate.
