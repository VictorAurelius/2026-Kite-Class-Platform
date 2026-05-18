# GAP-185: Billing Terms + VAT/TCT Invoice Compliance

**Status:** 🟡 PARTIAL — Phase 1 skeleton SHIPPED 2026-04-29 (Wave Legal-BRD Phase 1.5, PR #695). Phase 2 (legal + Tax advisor review + TCT-registered e-invoice provider selection VNPT/Viettel/Misa + MST collection UI + late fee config externalization GAP-108 + finance team SOP + first production VAT invoice verification) blocked-on stakeholder engagement → tracked GAP-154 umbrella + GAP-108 per `gap-done-discipline.md` §3 PARTIAL exit-ramp.
**Priority:** 🔴 P0 (business-logic tier — **VN Tax Law mandate**)
**Domain:** Legal / BRD / Commercial / Tax
**Found:** 2026-04-20 (BRD simulation — GAP-154 Phase 1)
**Wave:** Wave 8 Business Governance
**Affects:** VAT compliance, TCT (Tổng Cục Thuế) invoice format, enterprise procurement, late fee enforcement

## Problem

No billing terms doc. Current state:
- Invoice format not TCT-compliant (e-invoice per Circular 78/2021/TT-BTC required)
- Late fee rules hardcoded (`LATE_FEE_RATE=0.001` in code per GAP-108) without policy basis
- Payment terms (due date, currency, tax treatment) undocumented
- Dunning process undefined
- Enterprise payment methods (bank transfer, PO/invoice) not specified

Block: **Every Vietnamese tenant needs VAT invoice**; without compliant e-invoice, tenant can't claim VAT deduction → non-competitive offering.

## Scope

Create `documents/00-brd/billing-terms.md`:

1. **Payment Terms**
   - Billing cycle (monthly/annual/per-enrollment)
   - Due date (upfront, end of period, net-30 enterprise)
   - Currency (VND default, USD enterprise)
   - Payment methods per tier (VNPay, MoMo, ZaloPay, bank transfer, credit card)
   - Auto-renewal vs manual
2. **Tax Treatment**
   - VAT rate (10% standard, 5% education exemption analysis)
   - VAT invoice (e-invoice per TT 78/2021)
   - Foreign tenant handling (export of services)
   - Tax holiday / startup incentives (if applicable)
3. **Invoicing**
   - e-Invoice format (XML schema per TCT)
   - Digital signature (CA provider integration)
   - Issuance timing (on payment vs on service delivery)
   - Tenant tax info required (MST — Mã Số Thuế)
   - Credit note / adjustment invoice process
4. **Late Payment**
   - Grace period
   - Late fee calculation (ties to GAP-108 externalization)
   - Service suspension trigger
   - Reactivation process
   - Write-off criteria
5. **Pricing Changes**
   - Notice period (30-60 days)
   - Grandfathering
   - Mid-term price change prohibition
6. **Promotions + Discounts**
   - Discount types (percentage, fixed, free months)
   - Referral credits
   - Educational discounts (registered schools)
   - Promotion terms
7. **Refunds** — link to GAP-183
8. **Enterprise Payment**
   - PO process
   - Annual prepay discount
   - Multi-year contracts
   - Net-30/60 terms
9. **Currency Conversion** — for international tenants

## Acceptance Criteria

### Phase 1 (skeleton)

- [ ] `documents/00-brd/billing-terms.md` with 9 sections
- [ ] Payment method matrix (tier × available methods × fees)
- [ ] Tax calculation examples (VAT-inclusive vs VAT-exclusive)
- [ ] Late fee calculation examples (linked to GAP-108 config keys)
- [ ] Cross-references to GAP-108 (config externalization), GAP-183 (refund), GAP-180 (TOS), GAP-182 (Privacy for payment data)
- [ ] TCT e-invoice integration plan referenced (implementation = separate feature gap)
- [ ] README link updated

### Phase 2 (content + implementation)

- [ ] Legal + Finance review
- [ ] TCT-registered e-invoice provider selected (VNPT, Viettel, Misa, etc.)
- [ ] Tenant MST collection flow implemented (signup enhancement — separate feature gap)
- [ ] Invoice template aligned with TCT requirements
- [ ] Late fee configs externalized (closes GAP-108 partial)
- [ ] Finance team SOP derived
- [ ] First production VAT invoice issued + verified with accountant

## Out of Scope

- **TCT e-invoice integration** — separate feature gap (requires TCT provider signup)
- **MST collection UI** — separate frontend gap
- **Accounting system integration** (Misa, Amis) — enterprise feature

## Dependencies

- GAP-154 umbrella
- GAP-108 (payment/invoice config externalization)
- GAP-180 TOS (references billing terms)
- GAP-183 Refund (complementary)
- Finance team engagement
- TCT e-invoice provider selection

## Related

- Report: `brd-simulation-gap-finder-2026-04-20.md` §1.1 item N
- VN Law: **Circular 78/2021/TT-BTC** (e-invoice), **Decree 123/2020/NĐ-CP** (invoice + tax records), VAT Law 2008 + amendments
- Rule: `.claude/rules/meta-gap-priority.md` §3
- Connects to existing GAP-108 drift (validates root cause = missing policy)

## Scope Refinement (2026-05-18 audit)

- **Kept in scope:** Billing Terms policy + VAT inclusion/exclusion logic + eInvoice trigger rules + tenant MST onboarding requirements (P0 legal/financial exposure)
- **Moved to GAP-634:** Self-build VAT engine → MISA MeInvoice partnership integration
- **Out of scope:** TCT portal direct integration (partnership delegate handles)
- **PENDING status note:** awaiting legal counsel engagement per `release-1-plan-2026.md` Phase 2 timeline

## Log

- **2026-05-18** — Scope refined per outside-in audit Wave 93 (`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md`). Self-build VAT/TCT engine rejected — too complex (TCT portal integration + chữ ký số + invoice number management). Industry norm: partnership với MISA MeInvoice (https://www.meinvoice.vn) — already solved VN compliance. This gap remains scope: high-level Billing Terms policy document (VAT inclusion/exclusion logic, eInvoice trigger rules, tenant MST requirements). Integration work moved to GAP-634 (MISA MeInvoice partnership investigation). Related: GAP-108 (config keys narrowed), GAP-634 (integration).
- **2026-04-29** — Phase 1 skeleton SHIPPED (Wave Legal-BRD Phase 1.5, PR #695 squash-merged commit `1c2264c0`). 457-line markdown file `documents/00-brd/billing-terms.md` với 14 sections (9 mandated + 5 governance: Currency Conversion / Tax Calc Examples / Phase 2 Deliverables / Out-of-scope / Log). 6 tables: Payment Method Matrix (tier × VNPay/MoMo/ZaloPay/Bank/Card/PO × processor fees), Late Fee Calculation Examples (5 rows linked GAP-108 config keys `late-fee.rate`, `late-fee.grace-days`, `late-fee.max-cap-pct`, `late-fee.compounding`), Tax Calculation Examples (3 sub-tables: VAT-exclusive B2B + VAT-inclusive B2C + foreign export 0%), Due-date matrix, Service suspension threshold matrix, Phase 2 deliverables matrix. **14+ tax citations**: Circular 78/2021/TT-BTC (×4), Decree 123/2020/NĐ-CP (×8), VAT Law 2008, Luật Quản lý Thuế 2019, Commercial Law 2005, Consumer Protection Law 2023, Luật Thuế GTGT Art 5/Art 8. Frontmatter 7 fields. Cross-links: 5 sibling skeletons + GAP-108 planned (text reference). Status flipped 🔵 OPEN → 🟡 PARTIAL by coordinator (Phase 1 AC items 1-7 fully met; Phase 2 AC items 8-14 tracked under GAP-154 + GAP-108).
- 2026-04-20 — Created as GAP-154 Phase 1 sub-gap. TCT e-invoice mandate.
