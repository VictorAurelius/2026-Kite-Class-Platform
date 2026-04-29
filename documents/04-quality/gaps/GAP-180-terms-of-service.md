# GAP-180: Terms of Service (TOS) — Customer Legal Contract

**Status:** 🟡 PARTIAL — Phase 1 skeleton SHIPPED 2026-04-29 (Wave Legal-BRD Phase 1, PR #689). Phase 2 (legal counsel content + sign-off + click-wrap UI implementation) blocked-on stakeholder engagement → tracked GAP-154 umbrella per `gap-done-discipline.md` §3 PARTIAL exit-ramp.
**Priority:** 🔴 P0 (business-logic tier — GA blocker, legal)
**Domain:** Legal / BRD / Compliance
**Found:** 2026-04-20 (BRD simulation gap-finder — GAP-154 Phase 1)
**Wave:** Wave 8 Business Governance
**Affects:** Payment processing, enterprise sales, legal dispute defense

## Problem

No Terms of Service document. Tenant signing up today has no legal contract binding use of platform. Blocks:
- Payment processor onboarding (VNPay/Stripe require TOS link)
- Enterprise RFP responses (legal review requirement)
- Dispute defense (no agreed terms = liability exposure)

## Scope

Create `documents/00-brd/terms-of-service.md` with sections:

1. **Parties + Definitions** — KiteClass/KiteHub provider vs Customer (tenant) vs End Users (teacher, student, parent)
2. **Service Description** — scope, tiers, exclusions
3. **Customer Obligations** — content, security, lawful use, data accuracy
4. **Provider Obligations** — uptime commitment (links to customer SLA GAP-189), support, data security
5. **Acceptable Use** — link to AUP (GAP-181)
6. **Intellectual Property** — customer data ownership, provider IP, feedback license
7. **Payment Terms** — link to Billing Terms (GAP-185)
8. **Confidentiality + Data Protection** — link to Privacy Policy (GAP-182)
9. **Term + Termination** — cancellation, suspension, data handling on termination (link to GAP-184)
10. **Warranties + Disclaimers** — as-is clauses, limitations
11. **Limitation of Liability** — cap on damages, excluded damages
12. **Indemnification** — customer indemnifies for content, misuse
13. **Dispute Resolution** — link to Refund Policy (GAP-183), jurisdiction (VN), ADR
14. **Modifications** — notice period, acceptance mechanism
15. **Entire Agreement + Severability + Governing Law** — VN law, TAND jurisdiction

## Acceptance Criteria

### Phase 1 (skeleton — this gap)

- [ ] `documents/00-brd/terms-of-service.md` skeleton created with 15 sections + TODO markers
- [ ] Frontmatter: `status: skeleton`, owner: Legal, reviewer: PM + CEO
- [ ] Glossary of defined terms (tenant, end user, service, etc.)
- [ ] Link slots for GAP-181 (AUP), GAP-182 (Privacy), GAP-183 (Refund), GAP-184 (Retention), GAP-185 (Billing), GAP-189 (SLA)
- [ ] Acceptance mechanism documented (click-wrap at signup, re-accept on modification)
- [ ] Jurisdiction clause — VN law, specific TAND court
- [ ] Cross-reference from `00-brd/README.md` directory map

### Phase 2 (content — separate PR, requires legal counsel)

- [ ] Legal counsel engagement (requires GAP-049 counsel engagement process)
- [ ] Section content drafted
- [ ] Legal review complete with sign-off
- [ ] Implementation: signup flow updates to show TOS acceptance
- [ ] Status: `skeleton` → `approved`

## Out of Scope

- **Click-wrap UI implementation** — separate feature gap (track when scheduled)
- **TOS versioning/history storage** — feature gap, not BRD doc
- **Translated versions** (English) — defer post-launch

## Dependencies

- GAP-049 — legal counsel engagement process
- GAP-154 — umbrella (this is Phase 1 sub-gap)
- GAP-181/182/183/184/185/189 — siblings (referenced from TOS)

## Related

- Report: `documents/04-quality/audits/business/brd-simulation-gap-finder-2026-04-20.md` §1.1 item B
- Rule: `.claude/rules/meta-gap-priority.md` §3 — business-logic P0 justification
- VN Law: Civil Code 2015 (contract formation), Electronic Transactions Law 2023
- Wave 8 Business Governance plan (`documents/03-planning/roadmap/master-plan-all-gaps-2026-04-20.md`)

## Log

- **2026-04-29** — Phase 1 skeleton SHIPPED (Wave Legal-BRD Phase 1, PR #689 squash-merged commit `25ef4730`). 401-line markdown file `documents/00-brd/terms-of-service.md` với 15 mandated sections + Glossary + Cross-references + Phase 2 closure checklist. Markdown-header frontmatter (8 fields: Trạng thái 🔵 SKELETON / Owner Legal+PM/CEO / Reviewer / Last-Updated 2026-04-29 / Tracking GAP-180→GAP-154 / Legal basis VN Civil Code 2015 + Luật Giao dịch điện tử 2023 + Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 / Acceptance mechanism click-wrap / Jurisdiction TAND VN). Cross-links: 11 markdown links — 3 same-wave siblings (GAP-181/182/184), 3 existing 00-brd refs, 2 rules refs, 3 deferred-sibling references (GAP-183/185/189 written as planned-no-hyperlink). Phase 2 TODO markers inline với `<!-- Phase 2: legal counsel to fill — informed gut value, GAP-154 -->` comments. Status flipped 🔵 OPEN → 🟡 PARTIAL by coordinator per `gap-done-discipline.md` §3 (Phase 2 not in scope this wave; AC items 1-7 of Phase 1 fully met, AC items 8-12 of Phase 2 explicitly tracked under GAP-154 umbrella).
- 2026-04-20 — Created as GAP-154 Phase 1 sub-gap per BRD simulation findings.
