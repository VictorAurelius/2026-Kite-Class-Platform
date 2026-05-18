# GAP-633: VietQR EduPay (NAPAS) partnership investigation — Phase 2 webhook auto-reconcile pivot

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed (Backend + Business + Compliance + Partnership)
**Found:** 2026-05-18 (outside-in audit `2026-05-18-phase-1-5-qr-payment-outside-in.md` §5.1 + 3-agent benchmark convergence)
**Affects:** Phase 2 payment scale path (KiteHub > 100 PH); KiteHub stay non-PSP positioning; competitive cost-edge vs MISA EMIS (~0% take-rate baseline)
**Phase:** phase-2

## Problem

Phase 1.5 sẽ ship pure **QR upload + manual mark** payment flow (per audit §4.2 + P0 trio GAP-625/626/627 + P1 quintet GAP-628..632). Industry benchmark surveyed 7 VN edu SaaS competitors (audit §2.2) — **VietQR EduPay (NAPAS product)** đã solve VN-specific compliance + bank network + webhook auto-reconcile cho exact use case này:

- Multi-bank QR routing qua NAPAS network (43+ banks VN)
- Webhook delivery cho transaction notification (auto-reconcile invoice ↔ payment)
- Cost-plus pricing ~0% (vs MISA EMIS revenue-share với MSB bank)
- KiteHub **stay non-broker** — NAPAS webhook chỉ notify "transaction occurred"; money flows direct PH bank → Owner bank, KHÔNG qua NAPAS hay KiteHub holding account

Khi KiteHub scale **PH count > 100** (Phase 2 trigger per audit §5.3 + §6 phase progression table), **manual mark workflow KHÔNG còn viable**:

| Scale signal | Phase 1.5 (QR + manual) | Phase 2 (NAPAS partnership pivot) |
|---|---|---|
| Monthly transaction count P2 medium-center 100-200 HS | ~150-300 marks/tháng = 5-10h admin/tháng | Auto-reconcile via webhook = ~30 min admin/tháng |
| Reconciliation error rate manual | ~5-10% mismatch (audit Persona TOP 3 friction #2) | ~0.1% (NAPAS transaction_reference deterministic) |
| Owner edit "mark paid" fraud surface | Real (audit Failure-mode #4 anti-fraud) | Eliminated — NAPAS notify = source of truth |
| Audit trail | Manual + screenshot evidence (GAP-630) | NAPAS-side immutable + KiteHub mirror |

KHÔNG có investigation gap = nguy cơ surprise migration cost cao + lock-in vendor decision khi Phase 2 trigger hit (không có alternative đã evaluated).

## Root Cause

Phase 1.5 scope **đúng đắn** ship QR-only cho greenfield launch (audit verdict ✅ PROCEED conditional). Phase 2 path = compound cost: webhook receiver build self vs partnership integration. Industry already proven partnership wins — investigate NOW (Wave 35+) để Phase 2 trigger không bị surprise.

Per `outside-in-coverage-trigger.md` §3 — Phase 2 vendor decision = user-facing scope critical, outside-in audit MANDATORY trước lock. Audit §2.2 đã surface VietQR EduPay benchmark; investigation gap formalize follow-up.

## Proposed Fix

### Investigation tasks (Wave 35+ hoặc Phase 2 pre-trigger window)

1. **Contact NAPAS BD team** — request partnership terms cho KiteHub use case (B2B SaaS edu, non-broker positioning)
2. **Verify non-broker compliance** — confirm NAPAS webhook = notification only; money flows direct PH bank → Owner bank, KHÔNG qua NAPAS holding account, KHÔNG qua KiteHub holding account
3. **Integration scope evaluation** — webhook receiver endpoint design (Spring Boot `@PostMapping("/api/v1/payments/napas-webhook")`) + auto-reconcile mapping (transaction_reference → invoice_id resolver)
4. **Cost model comparison** — NAPAS cost-plus baseline ~0% vs MISA EMIS revenue-share với MSB (~1-2% transaction); document trong ADR phase-2-payment-partnership.md
5. **Pilot scope** — 5 tenants Phase 2 launch (1 P1 solo teacher với MST + 4 P2 small-center có scale > 100 HS)
6. **Risk analysis** — vendor lock-in mitigation (NAPAS API contract review + fallback path back to manual QR if partnership terminate)
7. **Onboarding flow** — Owner provides bank account info + NAPAS verifies bank account ownership (replace KiteHub-side KYC from GAP-625)
8. **Data residency** — NAPAS webhook payload contains PH PII (name + bank_account_masked + amount); verify NAPAS PDPL compliance + cross-border transfer not triggered (NAPAS = VN entity)

### Phase 2 trigger conditions (re-evaluate when)

- PH count > 100 tenants total HOẶC
- Any single tenant scales > 200 HS (P2 cliff per audit §3 convergence) HOẶC
- Manual reconciliation cost > 5h/tháng/tenant sustained 3 tháng

## Acceptance Criteria

- [ ] NAPAS BD contact established + partnership terms document obtained
- [ ] Non-broker compliance VERIFIED — NAPAS notification-only confirmed; KiteHub stays non-PSP per Phase 1 BETA positioning
- [ ] ADR `documents/02-architecture/adr/ADR-NNN-phase-2-payment-partnership.md` filed với decision rationale
- [ ] Integration scope estimate (LOC + tuần) cho webhook receiver + auto-reconcile module
- [ ] Cost model comparison table (NAPAS vs MISA EMIS vs other VN PSPs) ship trong ADR
- [ ] Pilot tenant list pre-identified (5 tenants criteria match Phase 2 trigger)
- [ ] Risk analysis section trong ADR (vendor lock-in + fallback path)
- [ ] Data residency PDPL compliance verified (NAPAS = VN entity, no cross-border transfer)
- [ ] Status flip DONE only sau ADR ACCEPTED + pilot scope signed-off OR pivot cancel (alternative chosen)

## Related

- **Audit report** — `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md` §5.1 row "GAP-633" + §2.2 benchmark + §6 Phase 2 trigger
- **Wave plan** — `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md` (paired same-PR Wave 93)
- **Foundation gaps Phase 1.5** — GAP-625/626/627 (P0 trio) + GAP-628/629/630/631/632 (P1 quintet)
- **Sibling P2 gaps** — GAP-634 (MISA MeInvoice partnership VAT), GAP-635 (QR installment Phase 2)
- **Phase context** — `documents/03-planning/roadmap/release-1-plan-2026.md` §5 Phase 2 medium-center scope
- **Industry source** — https://vietqr.com/edu/ (NAPAS product page) + PayOS Top 15 cổng thanh toán VN https://payos.vn/top-15-cong-thanh-toan-tot-nhat-hien-nay/
- **Re-scope gaps** — GAP-185 originally self-build VAT engine → re-scope MISA MeInvoice (paired GAP-634); GAP-108 12 config keys → re-scope QR display config

## Log

- **2026-05-18:** Filed by Wave 93 audit team per outside-in audit §5.1 P2 trio. Triggered Phase 2 partnership investigation gap để eliminate surprise migration cost khi PH > 100 trigger hit. Investigation tasks defer Phase 2 execution (post-Wave 35); rule ships gap NOW để track follow-up. Per `audit-to-gap-pipeline.md` §3 gap template + `gap-done-discipline.md` §1 — status OPEN per phase-2 phase classification. Per `meta-gap-priority.md` §3 Business-Logic tier 2nd after Meta (payment partnership decision = business-logic correctness, không phải feature execution).
