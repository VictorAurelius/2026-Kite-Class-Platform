# GAP-634: MISA MeInvoice partnership — VAT eInvoice integration cho P2 tenant có MST

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed (Backend + Compliance + Business + Partnership)
**Found:** 2026-05-18 (outside-in audit `2026-05-18-phase-1-5-qr-payment-outside-in.md` §5.1 + benchmark agent pitfall #3)
**Affects:** P2 Center Owner có MST doanh nghiệp; tenant compliance VAT 10% học phí; KiteHub differentiation vs self-build VAT engine
**Phase:** phase-2 (potential Phase 1.5b nếu MeInvoice API ready + legal counsel engaged)

## Problem

P2 Center Owner có **MST doanh nghiệp** (mã số thuế) → bắt buộc xuất hóa đơn 10% VAT cho học phí thu được (per Thông tư 78/2021/TT-BTC + Nghị định 123/2020/NĐ-CP):

- **Phải xuất eInvoice** khi tenant có doanh thu năm > 100M VND (mức bắt buộc đăng ký VAT)
- **Một số trường hợp miễn VAT** — học phí ngoại ngữ, kỹ năng nhất định (theo Điều 5 TT 78/2021); cần legal counsel verify per-tenant
- **eInvoice phải sync** với cơ quan thuế (TCT portal) — invoice number management, chữ ký số (digital signature)

Self-build VAT engine = **compliance hell** (audit benchmark §2.2 pitfall #3):

| Self-build burden | Partnership với MISA MeInvoice |
|---|---|
| TCT portal integration (đăng ký + upload + reconcile) | MISA đã có MOU với TCT — API call duy nhất từ KiteHub |
| Invoice number management (sequential, no skip, audit trail) | MeInvoice manage số hóa đơn auto |
| Chữ ký số (digital signature) — VD HSM, USB Token | MeInvoice tích hợp sẵn |
| eInvoice template compliance (data fields TT 78/2021) | MeInvoice template ready, customize per tenant |
| Late-submit penalty risk khi miss deadline | MeInvoice SLA + escalation |
| Per-tenant MST verification | MeInvoice provides MST lookup API |

GAP-185 originally scoped **self-build VAT engine** (filed Wave 23 pre-audit). Industry benchmark surveyed (audit §2.2) — **MISA MeInvoice** (https://www.meinvoice.vn) là norm tuyệt đối cho VN edu sector. Re-scope GAP-185 → MISA partnership integration thay vì self-build.

## Root Cause

GAP-185 inside-out scope (Wave 23) = self-build VAT engine; outside-in benchmark (Wave 93 audit) surface industry norm = partnership. Self-build = compliance hell + slow time-to-market + ongoing maintenance burden. Partnership = MeInvoice TCT-certified + faster integration + lower compliance risk.

Per `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync — when industry benchmark surfaces partnership norm, re-scope sibling gap (GAP-185) trong cùng PR. Per `outside-in-coverage-trigger.md` §3 — partnership vendor decision = compliance-critical + cross-cutting (legal + tech + UX) → outside-in audit MANDATORY trước lock.

## Proposed Fix

### Phase A: Investigation (Wave 33-34 Phase 1.5b nếu MeInvoice API ready)

1. **MISA MeInvoice API contract evaluation** — request developer docs + sandbox access
2. **Pricing model** — per-tenant flat fee vs per-invoice cost-plus; compare vs internal time-savings
3. **TCT certification verification** — confirm MeInvoice still TCT-certified (renewal annual)
4. **Tenant onboarding flow design** — Owner provides MST + connect MeInvoice account (OAuth-like flow OR manual API token)
5. **API integration scope** — `kitehub-subscription` module new submodule `vat-einvoice` (~3-5 days est)

### Phase B: Integration (Wave 34+ hoặc Phase 2)

1. **Auto-issue eInvoice trigger** — KiteHub mark-paid event (per GAP-625 manual mark workflow) → fire async job → MeInvoice API call → store invoice_id + PDF URL
2. **eInvoice PDF storage** — MinIO bucket `kitehub-einvoices/{tenant_id}/{invoice_number}.pdf` với 10-year retention (PDPL Art 11 + TT 78/2021 retention requirement)
3. **Retrieval UI** — P2 Owner dashboard tab "Hóa đơn VAT" hiển thị list + download PDF
4. **PH-side delivery** — auto-attach eInvoice PDF vào email "đã thu học phí" (already on Resend per Phase 1 BETA)
5. **Tax authority sync trigger** — high-volume tenants (>1000 invoices/quý) require quarterly sync; MeInvoice handle background
6. **Cancel/correction flow** — refund triggers cancel-invoice API call (paired GAP-629 refund workflow SOP)

### Phase relevance + dependency

- **Wave 33-34 Phase 1.5b** trigger nếu: MeInvoice API ready + legal counsel engaged (per Phase 2 trigger condition `release-1-plan-2026.md` §3 Phase progression "counsel engaged")
- **Phase 2 (post-Wave 35)** default if Phase 1.5b conditions not met
- **Dependency** — GAP-625 (P0 KYC) must close first (Owner MST verified); GAP-185 re-scope same PR

## Acceptance Criteria

- [ ] MISA MeInvoice developer docs reviewed + sandbox access obtained
- [ ] Pricing model comparison (MeInvoice vs self-build cost analysis) ship trong ADR `documents/02-architecture/adr/ADR-NNN-vat-einvoice-partnership.md`
- [ ] TCT certification verified current (annual renewal check)
- [ ] Tenant onboarding flow designed + UI mockup (Owner dashboard "Setup VAT")
- [ ] API integration scope estimate (LOC + tuần) ship trong wave plan kế tiếp
- [ ] GAP-185 re-scoped trong same PR (status updated: self-build → partnership integration)
- [ ] Phase trigger conditions documented (Phase 1.5b vs Phase 2 decision criteria)
- [ ] Data residency PDPL compliance verified (MeInvoice = VN entity, no cross-border transfer)
- [ ] Legal counsel review (when engaged per Phase 3 condition) cho miễn VAT cases (ngoại ngữ/kỹ năng)
- [ ] Status flip DONE only sau ADR ACCEPTED + integration scope locked OR pivot cancel

## Related

- **Audit report** — `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md` §5.1 row "GAP-634" + §5.2 re-scope GAP-185 + §2.2 benchmark pitfall #3
- **Wave plan** — `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md` (paired same-PR Wave 93)
- **Re-scope** — GAP-185 (originally self-build VAT/TCT Invoice engine; re-scope partnership integration)
- **Foundation gaps Phase 1.5** — GAP-625 (KYC), GAP-629 (refund SOP — paired cancel-invoice flow)
- **Sibling P2 gaps** — GAP-633 (VietQR EduPay NAPAS), GAP-635 (QR installment Phase 2)
- **Phase context** — `documents/03-planning/roadmap/release-1-plan-2026.md` §5 Phase 2 medium-center scope + §3 Phase 1.5b counsel-engaged trigger
- **Industry source** — https://www.meinvoice.vn (MISA MeInvoice) + https://www.meinvoice.vn/tin-tuc/4543/hoa-don-hoc-phi-nganh-giao-duc/ (eInvoice giáo dục) + https://einvoice.vn/tin-tuc/nam-2025-ho-kinh-doanh-day-them-phai-nop-nhung-loai-thue-nao (hộ kinh doanh dạy thêm thuế 2025)
- **Legal framework** — Thông tư 78/2021/TT-BTC + Nghị định 123/2020/NĐ-CP (VAT eInvoice mandate)

## Log

- **2026-05-18:** Filed by Wave 93 audit team per outside-in audit §5.1 P2 trio. Triggered MISA MeInvoice partnership investigation gap để re-scope GAP-185 self-build VAT engine sang partnership integration. Industry benchmark surfaced MeInvoice = norm tuyệt đối cho VN edu sector — self-build compliance hell eliminated. Investigation tasks defer Phase 2 execution (or Phase 1.5b nếu MeInvoice API ready + legal counsel engaged); rule ships gap NOW để track follow-up. Per `audit-to-gap-pipeline.md` §3 gap template + §2.7 Decision-Doc Code-Sync — re-scope GAP-185 trong cùng PR Wave 93 closure. Per `meta-gap-priority.md` §3 Business-Logic tier 2nd after Meta (VAT compliance partnership decision = business-logic-P2 — compliance-critical nhưng defer Phase 2 do P2 medium-center scope chưa đến trigger).
