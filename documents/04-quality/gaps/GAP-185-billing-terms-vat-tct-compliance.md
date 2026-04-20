# GAP-185: Billing Terms + VAT/TCT Invoice Compliance

**Status:** 🔵 OPEN
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

## Log

- 2026-04-20 — Created as GAP-154 Phase 1 sub-gap. TCT e-invoice mandate.
