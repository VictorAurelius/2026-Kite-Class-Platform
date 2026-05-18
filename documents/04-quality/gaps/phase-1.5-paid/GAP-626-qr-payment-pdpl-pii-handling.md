# GAP-626: QR payment PDPL Art 11 — PII handling + consent collection for transaction metadata

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed
**Detected:** 2026-05-18
**Related PRs:** []
**Related Docs:** [`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md), [`documents/03-planning/roadmap/release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md) §4 Phase 1.5 PAID

## Current State (verified 2026-05-18)

> Per `.claude/rules/audit-to-gap-pipeline.md` §2.5 — state-check xác nhận PDPL consent UI + transaction metadata retention scope là **greenfield** cho PH (parent) payment metadata. KiteHub đã có general PDPL consent flow cho tenant Owner registration, nhưng KHÔNG cover PH-specific consent tại payment moment.

| Piece | File / Path | Status |
|-------|-------------|--------|
| DPA addendum cho tenant covering PH payment metadata | Chưa build | ❌ missing |
| PH consent UI tại invoice payment step | Chưa build | ❌ missing |
| Transaction metadata storage schema (lawful basis: consent + legitimate interest reconcile) | Chưa build | ❌ missing |
| Data retention policy (90-day txn metadata, 6-year audit log) | Chưa build | ❌ missing |
| DSAR endpoint cho PH-initiated deletion | Chưa build | ❌ missing |
| PDPL Art 11 audit log cho PII access events | Existing V60 admin_audit_logs scaffold, nhưng KHÔNG extend cho PH PII scope | 🟡 partial |

**Grep commands run:**

```bash
# Verify PH PDPL consent + DSAR not yet implemented
grep -rl "phPaymentConsent\|ph_consent\|payment_metadata_retention\|dsar.*ph" \
  kitehub/kitehub-subscription/src kitehub/kitehub-platform/src \
  --include="*.java" 2>/dev/null
# Result: 0 files — greenfield

grep -rl "PaymentConsentModal\|PHConsentForm" \
  kitehub/kitehub-frontend/src --include="*.tsx" 2>/dev/null
# Result: 0 files — greenfield

# Existing PDPL scaffold confirms general scope only:
grep -rl "PDPL\|pdpl\|GDPR" documents/01-business --include="*.md" 2>/dev/null \
  | head -3
# Result: tenant Owner PDPL flow exists; PH-specific PDPL gap remains
```

## Problem

Khi PH (parent) quét QR + chuyển khoản qua banking app cá nhân tới account Owner, ngân hàng record metadata `[PH_name, PH_bank_account_number, transfer_amount, transfer_memo, timestamp] → Owner_bank_account_number`. Nếu KiteHub:

- **Hiện cho Owner xem metadata này** trong reconciliation UI (Owner mark-paid workflow cần Owner đối chiếu tên PH với danh sách HS) → KiteHub **đang lưu PH PII** mà PH chưa explicit consent
- **Lưu metadata này** trong DB cho 6-month dispute window → triggers PDPL Nghị định 13/2023/NĐ-CP Art 11 (xử lý dữ liệu cá nhân) yêu cầu **lawful basis** (consent | legitimate interest | contract) + **purpose limitation** + **retention policy**

**Failure mode nếu KHÔNG có gap này:**

- **Outside-in agent 3 (Failure-mode matrix)** scenario `P0-PII-PDPL-Art-11`: "KiteHub ngầm lưu PH_name + Owner_STK + chi tiết chuyển khoản không có explicit consent từ PH; PH có quyền khiếu nại Cục An toàn thông tin (BCATTT MIC) → phạt VND 100 triệu / vi phạm + bị buộc xóa data"
- **Persona walkthrough (agent 1)** flagged: "P2 Chị Hằng review reconciliation UI thấy tên 30+ PH per tháng cùng STK của họ — chị Hằng không biết phải hỏi consent từ PH, KiteHub không có UI prompt"
- **Benchmark (agent 2)** confirmed: "MISA EMIS + DotB EMS cả 2 đều có DPA addendum riêng cover payment metadata + consent checkbox tại invoice issue moment; competitor analysis 7/7 SaaS có consent UI"

**Why P0:** PDPL 2023 effective 2026-07-01 (~7 tuần countdown từ 2026-05-18) — Phase 1.5 PAID launch nếu ship sau 2026-07-01 phải compliant; nếu ship trước, vẫn cần ready vì PDPL Art 17 hồi tố cho data collected before effective date.

## Context

Phase 1.5 PAID payment scope chốt 2026-05-18 via outside-in audit (3-agent convergence). Lawful basis analysis per outside-in agent:

| Scope | Lawful basis | Required mechanism |
|---|---|---|
| Owner KYC data (CMND/CCCD + bank account) | Consent (Owner registers tenant) + Legal obligation (Luật Phòng chống rửa tiền 2022) | Existing tenant Owner PDPL consent flow đủ |
| Owner-side transaction record (Owner marks paid + amount + invoice ref) | Contract (tenant subscription) | Tenant Terms of Service covers |
| **PH-side transaction metadata (PH name + bank account + memo)** | **Consent (explicit per-payment) + Legitimate interest (reconciliation)** | **NEW — gap này addresses** |
| Audit log retention | Legal obligation (Luật Kế toán + PDPL Art 11) | 6-year retention table-level (audit_log immutable) |

Gap này là **2nd P0 trong cluster 3 foundation gaps** (GAP-625/626/627) **MUST close trước Phase 1.5 launch**. Sibling: GAP-625 (KYC + binding + audit log foundation) + GAP-627 (payment-amount mismatch).

## Evidence

- **Audit report:** [`2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md) §2.3 Failure-mode TOP 5 P0/P1 (item 4) + §3 3-agent convergence row "PII/PDPL Art 11" (Failure-mode + Benchmark cùng surface)
- **Persona walkthrough finding (agent transcript `a22e8469ba8bceef5`):** P2 Chị Hằng scenario reconciliation UI — chị Hằng confused về việc xử lý 30 PH names per tháng, không có UI guide về consent
- **Failure-mode scenario (agent transcript `a2615874804195b90`):** Actor=Regulator × Class=compliance × Phase=runtime — "BCATTT MIC inspect KiteHub DB → phát hiện 1,000+ rows PH PII không có consent trail → phạt VND 100 triệu × số tenant vi phạm + buộc xóa toàn bộ data + reissue invoice"
- **Benchmark pitfall (agent transcript `a1ee5d6e141e07b42`):** "eInvoice MeInvoice tại https://www.meinvoice.vn/tin-tuc/4543/hoa-don-hoc-phi-nganh-giao-duc/ — MISA partnership cho phép tenant self-issue invoice với PH consent integrated; KiteHub Phase 2 GAP-634 partnership; Phase 1.5 vẫn cần PH consent UI dù dùng QR upload"
- **Compliance ref:** Nghị định 13/2023/NĐ-CP Art 11 "Xử lý dữ liệu cá nhân" + Art 17 "Quyền của chủ thể dữ liệu cá nhân (DSAR — quyền truy cập, sửa, xóa, hạn chế xử lý)"

## Proposed Fix

### Sub-item (a) — DPA addendum cho tenant covering PH payment metadata

**Legal docs (documents/01-business/legal/dpa-addendum-ph-payment.md):**

- New legal doc DPA addendum specific cho PH payment metadata scope
- Định nghĩa rõ tenant role là Data Controller (Owner thu thập + xử lý PH PII), KiteHub là Data Processor (lưu trữ + cung cấp UI access cho Owner)
- Lists: (1) categories of PII processed (name, bank account, memo, timestamp), (2) purpose (reconciliation, dispute resolution), (3) retention (90-day metadata, 6-year audit log), (4) third-party sharing (none), (5) cross-border transfer (none — AWS Singapore ap-southeast-1)
- Required acceptance: tenant Owner accept khi enable Phase 1.5 PAID feature (gate behind feature flag `phase_1_5_paid_enabled`)

### Sub-item (b) — PH consent UI tại payment moment

**Frontend (kitehub-frontend):**

- New component `PaymentConsentModal.tsx` rendered khi PH access invoice payment page `/pay/{invoiceId}`
- Modal content (Vietnamese narrative):

```text
Khi bạn chuyển khoản qua QR code, ngân hàng sẽ chia sẻ thông tin sau với trung tâm:
- Tên chủ tài khoản (tên của bạn)
- Số tài khoản ngân hàng (4 số cuối)
- Số tiền + nội dung chuyển khoản + thời gian

KiteHub sẽ lưu thông tin này trong 90 ngày để giúp trung tâm đối soát học phí.
Bạn có quyền yêu cầu xóa thông tin này bất cứ lúc nào tại trang Hồ sơ.

☐ Tôi đồng ý chia sẻ thông tin chuyển khoản với trung tâm
   (theo Nghị định 13/2023/NĐ-CP Điều 11)

[Đồng ý + Mở QR] [Không đồng ý + Đóng]
```

- Consent captured trong table `ph_payment_consent` (columns: `consent_id`, `ph_email_or_phone_hashed`, `tenant_id`, `invoice_id`, `consented_at`, `consent_version`, `ip_address_anonymized`, `user_agent_hashed`)
- Revoke flow: PH portal `/ph-portal/consents` lists active consents + revoke button

### Sub-item (c) — Data retention policy + automated cleanup

**Backend:**

- New scheduled job `PhPaymentMetadataRetentionJob` chạy daily 02:00 UTC:
  - Query rows trong `ph_payment_metadata` với `created_at < NOW() - INTERVAL '90 days'`
  - Hard-delete rows (NOT soft-delete) — comply Art 17 right-to-erasure default state
  - Insert audit log entry `payment_metadata_retention_audit` với count deleted + timestamp
- Audit log retention separate: `payment_mark_paid_audit_log` (per GAP-625) retain 6 năm theo Luật Kế toán + PDPL Art 11 legal-obligation basis (NOT consent-based, immune to PH revoke)
- Document distinction trong `documents/01-business/payment/data-retention/rules.md`:
  - PH transaction metadata = consent-based → 90 days, deletable on DSAR
  - Audit log entries = legal-obligation-based → 6 năm, NOT deletable (immutable)

### Sub-item (d) — DSAR endpoint cho PH-initiated deletion

**Backend:**

- New endpoints under `/api/v1/ph-portal/dsar`:
  - `POST /access-request` — PH input email/phone → email link verify → returns paginated list of all data KiteHub holds about them
  - `POST /erasure-request` — PH input email/phone → email verify → trigger async deletion (90-day retention rows + active consents revoked + future payment require new consent)
  - `GET /erasure-status/{requestId}` — poll status (`PENDING | VERIFIED | DELETING | COMPLETED | FAILED`)
- Erasure flow: respect "exception" per PDPL Art 17 §4 — audit_log rows NOT deleted (legal obligation override), but metadata rows deleted; response notifies PH which categories deletable vs preserved-by-law
- SLA per PDPL Art 17 §2: 30 days response time → DSAR endpoint SLA dashboard alert if pending >25 days

**Frontend:**

- New page `/ph-portal/privacy` (public, no login required) với 2 buttons:
  - "Xem dữ liệu của tôi" → access request flow
  - "Yêu cầu xóa dữ liệu" → erasure request flow
- Status tracking page `/ph-portal/dsar-status/{requestId}` với progress indicator

## Acceptance Criteria

- [ ] **DPA addendum sub-item:** Legal doc `documents/01-business/legal/dpa-addendum-ph-payment.md` ship với 5 sections (PII categories, purpose, retention, third-party, cross-border); tenant Owner phải explicit accept khi enable Phase 1.5 PAID feature; acceptance recorded trong `tenant_dpa_acceptance` table với version + timestamp
- [ ] **Consent UI sub-item:** `PaymentConsentModal.tsx` ship + rendered at invoice payment page; consent captured DB row when checked + redirect to QR; "Không đồng ý" path closes modal + shows alternative payment method instructions (out-of-band manual transfer); consent table indexed by `(ph_email_or_phone_hashed, tenant_id)` for fast lookup
- [ ] **Retention policy sub-item:** `PhPaymentMetadataRetentionJob` scheduled + verified runs daily; manual trigger test deletes rows >90 days correctly; retention audit log row inserted per run; `documents/01-business/payment/data-retention/rules.md` ship với 3-layer (rules.md + use-cases.md + api-contract.md) covering distinction metadata vs audit log
- [ ] **DSAR sub-item:** 3 endpoints ship + tested; PH access request returns paginated PII list correctly; PH erasure request triggers async deletion + audit log preservation (verify SQL `SELECT COUNT(*) FROM ph_payment_metadata WHERE ph_id = '...'` returns 0 after erasure complete, while `SELECT COUNT(*) FROM payment_mark_paid_audit_log WHERE ph_id = '...'` unchanged); SLA dashboard alert wired if pending >25 days
- [ ] **End-to-end test scenario PASS** per `.claude/rules/pre-handoff-self-test-completeness.md` §2.6 Payment flow:
  - (a) PH access `/pay/{invoiceId}` → consent modal renders
  - (b) PH check consent + click "Đồng ý" → row inserted `ph_payment_consent` table → QR revealed
  - (c) PH unchecked + click "Không đồng ý" → alternative payment instructions shown
  - (d) After payment + Owner mark-paid, PH access `/ph-portal/privacy` → access request reveals all PII held
  - (e) PH submit erasure request → email verify link → after 30 days SLA, verify metadata deleted + audit log preserved
- [ ] **Reviewer manual:** verify DB query post-PH-erasure — `ph_payment_metadata` rows for that PH = 0; `payment_mark_paid_audit_log` rows for that PH unchanged; `ph_payment_consent` rows marked `revoked_at`
- [ ] **Compliance review:** all 4 sub-items map to PDPL Nghị định 13/2023/NĐ-CP articles cited (Art 11 lawful basis, Art 17 DSAR rights, Art 20 retention) — cross-checked với CLAUDE.md note "v1 pending counsel review" disclaimer OK for non-K-12 Phase 1.5 scope
- [ ] **Documentation:** 3-layer docs created — `documents/01-business/payment/ph-pdpl-consent/{rules,use-cases,api-contract}.md`; rules.md cites BR-PDPL-PH-001 through BR-PDPL-PH-008

## Related

- **Audit origin:** [`2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md)
- **Paired wave plan:** `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md` (paired same-PR by coordinator)
- **Sibling P0 gaps (ship together as foundation block):** GAP-625 (KYC + binding + audit log), GAP-627 (payment-amount mismatch detection)
- **P1 follow-up gaps:** GAP-628 (batch reconcile P2), GAP-629 (refund SOP), GAP-630 (evidence storage), GAP-631 (KYC quarterly refresh), GAP-632 (mark-paid override approval)
- **Re-scoped existing:** [`GAP-108`](GAP-108-payment-invoice-config-hardcoded.md), [`GAP-183`](GAP-183-refund-dispute-resolution-policy.md), GAP-185 (VAT — re-scope MISA partnership), [`GAP-594`](GAP-594-refund-policy-30-day-money-back.md)
- **Phase 1.5 plan:** [`release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md) §4
- **Rules:** [`outside-in-coverage-trigger.md`](../../../.claude/rules/outside-in-coverage-trigger.md), [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §2.5, [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.6 §2.11 i18n, [`release-deploy-standard.md`](../../../.claude/rules/release-deploy-standard.md) §3.4 K-12 LEGAL Phase 3 trigger (PDPL counsel-reviewed legal docs)
- **Compliance refs:** Nghị định 13/2023/NĐ-CP Art 11 (lawful basis) + Art 17 (DSAR) + Art 20 (retention); CLAUDE.md PDPL hard deadline 2026-07-01

## Log

- **2026-05-18** — Initial write-up. Filed via Wave 93 outside-in audit (3-agent convergence per `outside-in-coverage-trigger.md`). State-check confirms PH-specific consent UI + DSAR endpoints greenfield (existing tenant Owner PDPL scaffold cover general flow only, NOT PH transaction metadata scope). Priority P0 — blocking Phase 1.5 PAID launch trigger + PDPL hard deadline 2026-07-01 (~7 tuần countdown). META P0 force-multiplier per `meta-gap-priority.md` — fix consent foundation 1 lần → mọi PH payment future flow auto-comply.
