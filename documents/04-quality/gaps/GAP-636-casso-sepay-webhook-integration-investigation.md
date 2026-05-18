# GAP-636: Casso/SePay webhook integration investigation Phase 1.5b — auto-reconcile pivot replacing OCR proposal

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Detected:** 2026-05-18
**Related PRs:** []
**Related Docs:** [`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-ocr-auto-confirm-outside-in.md`, `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md`, `documents/03-planning/roadmap/release-1-plan-2026.md` §4 Phase 1.5 PAID]

## Current State (verified 2026-05-18)

> Per `.claude/rules/audit-to-gap-pipeline.md` §2.5 — state-check confirms greenfield Phase 1.5b scope.

| Piece | File / Path | Status |
|-------|-------------|--------|
| Casso/SePay vendor evaluation doc | Not yet built | ❌ missing |
| Webhook receiver endpoint | Not yet built | ❌ missing |
| `addInfo` unique ID generator | Not yet built | ❌ missing |
| Tenant bank-connect onboarding UI | Not yet built | ❌ missing |
| Webhook signature verify middleware | Not yet built | ❌ missing |
| Idempotency layer for webhook replay | Not yet built | ❌ missing |
| Casso vs SePay decision ADR | Not yet built | ❌ missing |

**Grep commands run:**
```bash
grep -rl "casso\|sepay\|Casso\|SePay" kitehub/ kiteclass/ documents/ 2>/dev/null | head
# Result: 0 hits — completely greenfield scope
grep -rl "webhook" kitehub/kitehub-subscription/src 2>/dev/null
# Result: existing webhook patterns trong subscription billing scope, NOT applicable for payment webhook
```

## Problem

Wave 93 outside-in audit (3-agent OCR investigation) reveals industry pivot signal: 0/7 VN edu SaaS competitors dùng OCR receipt; pattern dominant 2026 là **VietQR + Casso/SePay webhook integration**. Đây là path forward thay vì OCR auto-confirm:

- **0 fraud risk** — tiền thực sự vào tài khoản; webhook = notification only
- **0% false-accept** — bank verify trực tiếp, không reliance trên user upload screenshot
- **Eliminate manual reconcile entirely** — P2 BLOCKER (90 manual marks/tháng) hoàn toàn solve
- **KiteHub stay non-PSP** — webhook KHÔNG broker tiền (tenant connect bank account direct với Casso/SePay)
- **Match industry pattern** — Casso/SePay là dominant VN e-commerce 2026 (Lightspark instant payment rails report)

User confirm 2026-05-18 chấp nhận benchmark recommendation: file GAP-636 P1 cho Casso/SePay webhook investigation Phase 1.5b thay vì OCR proposal.

3 outside-in agents convergence:
- **Persona walkthrough:** OCR Hybrid path conditional acceptable nhưng webhook eliminates Owner cognitive load entirely
- **Failure-mode matrix:** OCR 5 P0 fraud surfaces (Photoshop + AI fakes + multi-bank drift + PDPL + cross-tenant); webhook ~0% all 5
- **External benchmark:** **OVERRIDE finding** — VN edu SaaS skip OCR; Casso/SePay là proven pattern

## Context

Phase 1.5 base QR manual mark approach (Wave 93 GAP-625..632 chốt) là Phase 1.5a foundation. **Phase 1.5b extends với webhook auto-reconcile** cho P2 tenants scale ≥30 HS — eliminate 78% manual reconcile pain point identified by persona walkthrough (P2 chị Hằng 90 manual marks/tháng → 0 với webhook).

Phase 2 path Wave 93 originally đề xuất VietQR EduPay (NAPAS) partnership (GAP-633). Casso/SePay là **commercial aggregator path** với coverage rộng hơn NAPAS direct partnership (Casso/SePay đã subscribe sẵn nhiều VN banks). Phase 1.5b với Casso/SePay → Phase 2 evaluate VietQR EduPay direct partnership cost-benefit (lower per-txn cost potentially).

## Evidence

- Outside-in audit OCR: `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-ocr-auto-confirm-outside-in.md`
- Benchmark agent finding §2.2 — 7 VN edu SaaS competitor matrix: 0/7 OCR primary, 1/7 webhook primary (VietQR EduPay NAPAS)
- Industry sources: Casso (https://casso.vn), SePay (https://sepay.vn), Lightspark VN payment rails report 2026
- Persona walkthrough: P2 90-click/tháng pain point persona acceptance verdict

## Proposed Fix

**Phase 1.5b sub-bucket scope (Wave 33-34 estimate):**

### Phase A — Vendor evaluation (1-2 tuần)

- (a) **Casso vs SePay comparison matrix:**
  - Cost model: SePay free tier vs Casso $10-30/month
  - Bank coverage (% VN banks supported)
  - Webhook reliability + retry policy
  - API quality + OpenAPI docs
  - Support response time
  - Compliance (PDPL + NHNN trung gian thanh toán license status)
- (b) **ADR-XXX:** Casso vs SePay decision document (per `.claude/rules/output-review-mandate.md` §3 ADR row)
- (c) **Pilot 3-5 P2 tenants** sign-up flow + monitor 4 tuần

### Phase B — Webhook receiver implementation (2-3 tuần)

- (d) **Webhook endpoint** `POST /api/v1/payment-webhook/{provider}` với:
  - Signature verify middleware (HMAC-SHA256 hoặc provider-specific scheme)
  - Idempotency key check (provider transaction_id unique)
  - Async processing queue (RabbitMQ) cho non-blocking response
- (e) **`addInfo` unique ID generator:** format `KH-INV-{tenantId}-{invoiceId}-{nonce}` (collision-free across tenants, 32 char max)
- (f) **Match algorithm:**
  - Primary: exact match `addInfo` parse → invoice_id
  - Fallback fuzzy: amount + sender STK + date window → confidence score
  - Manual review queue cho confidence < 80% (rare edge cases)
- (g) **Audit log integration** — every webhook event logged immutable per GAP-625 audit infra

### Phase C — Tenant onboarding (1-2 tuần)

- (h) **Owner UI flow:** `/settings/payment-integration`:
  - Step 1: Select Casso vs SePay (default = SePay nếu free tier match volume)
  - Step 2: Redirect tenant đăng ký Casso/SePay account (out-of-platform — KiteHub không broker)
  - Step 3: Tenant nhập API key/webhook secret từ Casso/SePay dashboard → KiteHub
  - Step 4: Test transaction → verify webhook flow end-to-end
- (i) **Bank account verification** — Owner xác nhận bank account ownership via Casso/SePay (delegate KYC to vendor)

### Phase D — Manual reconcile fallback retained

- (j) **Cash payment manual mark** — flow Wave 93 GAP-632 giữ nguyên (Owner click "Mark Paid — Cash")
- (k) **Webhook failure fallback** — Owner có thể manual mark khi webhook miss (rare; cần audit log indicator "manual after webhook miss")

## Acceptance Criteria

- [ ] ADR-XXX Casso vs SePay decision shipped với 5 evaluation criteria + verdict
- [ ] Webhook receiver endpoint live với signature verify + idempotency + audit log
- [ ] `addInfo` generator generates collision-free IDs across all tenants (verified via test suite ≥10k IDs)
- [ ] Match algorithm exact match ≥99% accuracy (test fixtures ≥100 webhook payloads từ 5+ VN banks)
- [ ] Tenant onboarding flow E2E test: Owner sign up → connect bank → first webhook delivery → invoice auto-mark paid (≤5 min flow)
- [ ] Pilot 3-5 P2 tenants 4-tuần run với <1% webhook miss rate (manual fallback retains coverage)
- [ ] Cash payment manual mark flow unchanged + clearly distinguished in UI ("Cash" badge)
- [ ] Manual fallback when webhook fail (rare) — Owner can override mark; audit log records "manual after webhook miss" indicator
- [ ] Cost analysis report: actual Casso/SePay cost per tenant per month vs SaaS subscription revenue (financial viability check)
- [ ] Pre-handoff self-test per `.claude/rules/pre-handoff-self-test-completeness.md` §2.6 Payment flow + §2.9 Background job flow (webhook receiver = async job)

## Related

- **Audit origin:** `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-ocr-auto-confirm-outside-in.md`
- **Parent audit:** `documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md` (Wave 93 base QR audit)
- **Paired wave plan:** `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md`
- **Phase 1.5a dependencies (must close first):** GAP-625 (KYC + multi-tenant binding + audit log), GAP-626 (PDPL PII consent), GAP-627 (amount mismatch detection), GAP-628 (batch reconcile — webhook replaces for P2), GAP-632 (manual mark audit trail — webhook coexists with manual cash)
- **Phase 2 evaluation candidate:** GAP-633 (VietQR EduPay NAPAS partnership — alternative direct partnership; cost-compare post Phase 1.5b)
- **Re-scope existing:** GAP-108 (config keys cho webhook config), GAP-183/594/629 (refund SOP unchanged — manual out-of-band)
- **Phase 1.5 plan:** `documents/03-planning/roadmap/release-1-plan-2026.md` §4
- **Rules applied:** `outside-in-coverage-trigger.md`, `audit-to-gap-pipeline.md` §2.5, `pre-handoff-self-test-completeness.md` §2.6 + §2.9, `meta-gap-priority.md` §3 Business-Logic tier

## External references

- Casso developer docs: https://developer.casso.vn/webhook/thiet-lap-webhook-thu-cong
- Casso bank API: https://casso.vn/api-ngan-hang/
- SePay pricing: https://sepay.vn/bang-gia.html
- SePay virtual account: https://sepay.vn/tai-khoan-ao-theo-don-hang.html
- VietQR API: https://api.vietqr.vn/en
- Lightspark VN instant payments 2026: https://www.lightspark.com/knowledge/instant-payments-vietnam

## Log

- **2026-05-18** — Initial write-up. Filed Wave 93 outside-in OCR audit pivot decision (`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-ocr-auto-confirm-outside-in.md` §4). Replaces user inside-out OCR proposal — 3-agent benchmark surfaces VN edu SaaS skip OCR + Casso/SePay là dominant pattern. State-check confirms 0 hits cho "casso/sepay" trong codebase = greenfield. Priority P1 — không P0 vì Phase 1.5a base QR manual mark (Wave 93 GAP-625..632) đã cover beta launch path; webhook là sub-bucket Phase 1.5b enhancement cho P2 scale + reduce manual reconcile entirely. Dependencies: must close GAP-625/626/627 P0 foundation first (Phase 1.5a).
