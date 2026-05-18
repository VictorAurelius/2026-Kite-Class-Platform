# GAP-625: QR payment foundation — Owner KYC + multi-tenant QR binding + immutable mark-paid audit log

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed
**Detected:** 2026-05-18
**Related PRs:** []
**Related Docs:** [`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md), [`documents/03-planning/roadmap/release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md) §4 Phase 1.5 PAID

## Current State (verified 2026-05-18)

> Per `.claude/rules/audit-to-gap-pipeline.md` §2.5 — state-check xác nhận tenant-Owner-PH (parent) QR payment scope là **greenfield**. KiteHub subscription billing scope (tenant pays KiteHub) đã có `QRCodeDisplay.tsx` + `PaymentController.java` từ Wave trước nhưng **KHÔNG cover** tenant → PH payment flow (scope khác).

| Piece | File / Path | Status |
|-------|-------------|--------|
| Owner KYC upload (CMND/CCCD) UI | Chưa build | ❌ missing |
| Owner KYC verify backend service | Chưa build | ❌ missing |
| Bank account ownership verification (micro-deposit hoặc API) | Chưa build | ❌ missing |
| QR generation với `tenant_id` embed | Chưa build (existing `QRCodeDisplay.tsx` chỉ render KiteHub billing QR, không phải Owner-PH QR) | ❌ missing |
| Tenant-bound QR storage table | Chưa build | ❌ missing |
| Mark-paid action endpoint | Chưa build (KiteHub subscription `PaymentController` scope khác) | ❌ missing |
| Immutable audit log table cho mark-paid action | Chưa build | ❌ missing |
| Refund/dispute lookup via audit log | Chưa build | ❌ missing |

**Grep commands run:**

```bash
# Verify no existing tenant-Owner-PH QR payment implementation
grep -rl "VietQR\|tenant_id.*qr\|qr.*binding\|owner_kyc\|owner_kyc_status" \
  kitehub/kitehub-subscription/src kitehub/kitehub-platform/src \
  --include="*.java" 2>/dev/null
# Result: 0 files — greenfield

grep -rl "ownerKyc\|tenantPaymentQr\|markPaid" \
  kitehub/kitehub-frontend/src --include="*.tsx" --include="*.ts" 2>/dev/null
# Result: 0 files — greenfield

# Existing QR scope confirm (KiteHub subscription billing, NOT tenant-Owner-PH):
# kitehub-subscription/PaymentController.java — tenant pays KiteHub subscription
# kitehub-frontend/billing/QRCodeDisplay.tsx — render subscription billing QR
# → Scope khác hoàn toàn với Phase 1.5 tenant Owner → PH (parent) học phí collection
```

## Problem

Phase 1.5 PAID payment scope quyết định 2026-05-18 dùng QR upload thay payment processor (per audit `5.1` row P0-1). Tenant Owner generate QR cá nhân (VietQR / Momo cá nhân / ZaloPay cá nhân) → PH (parent) quét QR chuyển khoản → Owner manual đánh dấu "đã thu". Đây là **MVP foundation block** cho Phase 1.5 launch.

**Failure mode nếu KHÔNG có gap này:**

- **Outside-in agent 1 (Persona walkthrough)** surfaced: "QR ownership ambiguity catastrophic — GV nghỉ → tiền stuck vì QR personal account của GV cũ; PH chuyển nhầm vào account đã closed; trường hợp 3 GV × 3 QR khác nhau, không rõ ai owner ultimate"
- **Outside-in agent 2 (External benchmark)** confirmed: "MISA EMIS + DotB EMS đều require KYC merchant onboarding với CMND/CCCD scan + bank ownership verify; QR-only competitors như Easy Edu, Mona vẫn track Owner identity tại tenant level để dispute resolution work"
- **Outside-in agent 3 (Failure-mode matrix)** scenario `P0-anti-fraud-owner-verification`: "QR không validate recipient bank account ownership — Owner có thể paste QR của account khác để route money tới đó (rửa tiền tenant / employee fraud)"

**3 sub-items share dataflow:** Owner identity verified (KYC) → QR generation embed tenant_id + bound to verified bank account → Mark-paid action signed against Owner identity → Audit log immutable, query-able 6 tháng sau cho refund/dispute.

**Tách rời = half-shipped P0:**

- KYC without binding = QR vẫn có thể paste cross-tenant
- Binding without KYC = QR bound đúng tenant nhưng Owner identity chưa verify (fraud risk)
- Audit log without binding+KYC = log records nhưng không match Owner identity (forensics fail)

## Context

Phase 1.5 PAID payment scope chốt 2026-05-18 via outside-in audit (3-agent convergence). User original inside-out proposal: "cho phép Owner edit QR thay payment processor cho giáo viên đơn lẻ". Audit reveals QR là **mandatory** (không phải shortcut tùy chọn) vì:

- VN PSP license barrier — nếu KiteHub broker tiền giữa Owner và PH → trở thành PSP, cần giấy phép NHNN trung gian thanh toán (12-18 tháng + vốn pháp định 50 tỷ)
- Hộ kinh doanh dạy thêm phần lớn KHÔNG có MST doanh nghiệp đầy đủ → KYC merchant onboarding với cổng thanh toán (Stripe, MoMo Business, VNPay Merchant) fail tại bước đăng ký
- Industry norm — 80%+ VN edu SaaS competitors (Easy Edu, DotB, Mona, VnResource, Faceworks) dùng QR upload + manual reconcile cho phân khúc <50 HS

Gap này là foundation block đầu tiên trong cluster 3 P0 gaps (GAP-625/626/627) **MUST close trước Phase 1.5 launch**. Sibling P0 gaps cover PDPL transaction PII (GAP-626) + payment-amount mismatch detection (GAP-627).

## Evidence

- **Audit report:** [`2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md) §2.3 Failure-mode TOP 5 P0/P1 (item 1 + item 3) + §3 3-agent convergence row "QR ownership ambiguity catastrophic" (✅ all 3 agents)
- **Persona walkthrough finding (agent transcript `a22e8469ba8bceef5`):** P2 Chị Hằng scenario — trường hợp 3 GV × 3 QR khác nhau khi 1 GV nghỉ → tiền vào account cũ stuck; chị Hằng không có cách prove ownership cho refund 2 tháng sau
- **Failure-mode scenario (agent transcript `a2615874804195b90`):** Actor=Owner × Class=anti-fraud × Phase=onboarding — "QR copy-paste attack: Owner trung tâm A copy QR của trung tâm B vào KiteHub UI → PH trung tâm A chuyển tiền vào trung tâm B's account; KiteHub không có mechanism detect"
- **Benchmark pitfall (agent transcript `a1ee5d6e141e07b42`):** "MISA EMIS + DotB EMS đều require KYC merchant onboarding với CMND/CCCD scan; QR-only competitors vẫn track Owner identity tại tenant level"

## Proposed Fix

### Sub-item (a) — Owner KYC at QR setup

**Backend (kitehub-platform / kitehub-subscription):**

- New service `OwnerKycService` với 3 endpoints:
  - `POST /api/v1/owner/kyc/upload-id-card` — multipart upload CMND/CCCD front + back image (per `pre-handoff-self-test-completeness.md` §2.5 file-upload checklist: MIME `image/jpeg|png` only, ≤5MB, ClamAV scan, MinIO bucket `kite-kyc-private` non-public)
  - `POST /api/v1/owner/kyc/verify-bank-account` — input bank account number + bank code; backend issues micro-deposit (VND 1,000 → 10,000) qua tenant's chosen bank API (Phase 1.5 manual SOP nếu API integration chưa ship); Owner confirm amount received → status `KYC_VERIFIED`
  - `GET /api/v1/owner/kyc/status` — returns `PENDING | UNDER_REVIEW | VERIFIED | REJECTED`
- New table `owner_kyc` (columns: `owner_id`, `cmnd_image_url_front`, `cmnd_image_url_back`, `bank_account_number_hashed` (BCrypt), `bank_code`, `verification_method` (`MICRO_DEPOSIT` | `MANUAL_REVIEW`), `status`, `verified_at`, `verified_by` (admin actor for manual review), `kyc_audit_log_id` (FK to immutable log))
- Flyway migration `V{N}__owner_kyc_table.sql` + `V{N+1}__owner_kyc_indexes.sql`

**Frontend (kitehub-frontend):**

- New page `/owner/kyc/setup` — wizard 3-step: upload CMND/CCCD → input bank account → confirm micro-deposit
- Status badge in Owner dashboard navbar (red "KYC chưa xong" → green "KYC verified")

### Sub-item (b) — Multi-tenant QR binding

**Backend:**

- Extend QR generation logic embed `tenant_id` + `owner_kyc_id` + `bank_account_fingerprint` (SHA-256 of bank_account_number) vào QR payload metadata (theo VietQR EMVCo TLV spec — field `62` (Additional Data) + `81` (Bill Number) hoặc proprietary field theo NAPAS guideline)
- New table `tenant_payment_qr` (columns: `qr_id` (UUID), `tenant_id` (FK), `owner_id` (FK), `bank_account_fingerprint` (SHA-256), `qr_image_url` (MinIO signed URL), `created_at`, `revoked_at` (nullable), `revoke_reason`)
- Endpoint `POST /api/v1/tenant/{tenantId}/payment-qr` — generate + bind QR sau khi KYC verified
- Endpoint `GET /api/v1/payment-qr/{qrId}/verify` — public endpoint cho PH scan reveal QR ownership info (tenant name + Owner first name partial mask) trước khi chuyển khoản
- Guard rail: 1 active QR per tenant tại bất cứ thời điểm — revoke old khi generate new

**Frontend:**

- `/owner/payment/qr` page — show active QR + history of revoked QRs + audit trail
- PH-facing tenant verify page `/verify-qr/{qrId}` — public, shows "Bạn đang chuyển khoản tới: Trung tâm Anh ngữ Sky Education (Chị H***)" để PH confirm trước scan

### Sub-item (c) — Immutable mark-paid audit log

**Backend:**

- New table `payment_mark_paid_audit_log` (columns: `audit_id` (UUID), `tenant_id`, `owner_id`, `invoice_id`, `qr_id`, `amount_marked_paid`, `marked_paid_at`, `marked_paid_by` (Owner actor), `bank_transaction_reference` (optional), `screenshot_url` (optional, per GAP-630), `created_at_immutable_timestamp` (PostgreSQL trigger BEFORE INSERT/UPDATE/DELETE → REJECT UPDATE/DELETE per `documents/04-quality/audits/aws-verification/...immutable_admin_audit_logs...`))
- Endpoint `POST /api/v1/invoice/{invoiceId}/mark-paid` — Owner action; insert row only (no UPDATE allowed); returns `audit_id`
- Endpoint `GET /api/v1/invoice/{invoiceId}/audit-log` — returns chronological audit entries for refund/dispute lookup
- Flyway migration với PostgreSQL trigger pattern tương tự V60 admin_audit_logs immutable (per audit-skill rubric ops-readiness-audit baseline)

**Frontend:**

- Owner UI mark-paid button → Modal confirm với "Bạn xác nhận đã nhận VND X từ PH Y? Hành động này KHÔNG thể edit sau khi confirm."
- Refund/dispute view shows audit log timeline với immutable badge

## Acceptance Criteria

- [ ] **KYC sub-item:** `OwnerKycService` ship với 3 endpoints; `owner_kyc` table + migration applied production; UI wizard 3-step ship; KYC status surfaced trong Owner dashboard navbar; status badge color (red/yellow/green) reflects DB state. Verified qua manual test trên staging: Owner upload CMND → bank account verify → status `VERIFIED` trong ≤5 phút (manual review path)
- [ ] **Binding sub-item:** `tenant_payment_qr` table với constraint UNIQUE(`tenant_id`, `revoked_at IS NULL`) — chỉ 1 active QR per tenant; QR generation endpoint embed `tenant_id` trong payload (verify qua decode TLV); public PH-facing verify page render tenant name + masked Owner name correctly; revoke flow archives old QR + creates new
- [ ] **Audit log sub-item:** `payment_mark_paid_audit_log` table với PostgreSQL trigger BLOCK UPDATE/DELETE attempts (verify qua direct SQL: `UPDATE payment_mark_paid_audit_log SET amount = 0 WHERE audit_id = '...'` → ERROR raised); mark-paid endpoint inserts row + returns `audit_id`; refund/dispute query 6-month-old row returns same data unchanged
- [ ] **End-to-end test scenario PASS** per `.claude/rules/pre-handoff-self-test-completeness.md` §2.6 Payment flow + §2.5 File upload + §2.4 Admin/privileged action:
  - (a) Tenant Owner đăng ký mới → KYC wizard → CMND upload (test invalid MIME rejected 415) → bank verify (test micro-deposit flow) → status `VERIFIED`
  - (b) Owner generate QR → verify tenant_id embed in QR payload TLV → revoke old QR → generate new (verify constraint enforce)
  - (c) PH scan QR → public verify page render correct tenant + masked Owner name
  - (d) Owner mark-paid → audit log row inserted (verify `audit_id` returned + DB row exists)
  - (e) Attempt SQL UPDATE on audit log → ERROR (immutability verified)
  - (f) Refund query 6 months later → audit row returns identical
- [ ] **Reviewer manual:** verify via DB query for any payment recorded — audit trail returns `tenant_id` + `owner_id` + `qr_id` + `bank_transaction_reference` (when provided) + immutable timestamp; no UPDATE traces in DB log
- [ ] **Documentation:** 3-layer docs created per CLAUDE.md mandate — `documents/01-business/payment/owner-kyc/{rules,use-cases,api-contract}.md`; rules.md cites BR-PAYMENT-KYC-001 through BR-PAYMENT-KYC-005

## Related

- **Audit origin:** [`2026-05-18-phase-1-5-qr-payment-outside-in.md`](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md)
- **Paired wave plan:** `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md` (file paired same-PR by coordinator)
- **Sibling P0 gaps (ship together as foundation block):** GAP-626 (PDPL transaction PII), GAP-627 (payment-amount mismatch detection)
- **P1 follow-up gaps:** GAP-628 (batch reconcile P2), GAP-629 (refund SOP), GAP-630 (evidence storage), GAP-631 (KYC quarterly refresh), GAP-632 (mark-paid override approval)
- **P2 Phase 2 gaps:** GAP-633 (VietQR EduPay partnership), GAP-634 (MISA MeInvoice partnership), GAP-635 (installment payment)
- **Re-scoped existing:** [`GAP-108`](GAP-108-payment-invoice-config-hardcoded.md) (payment-invoice config), [`GAP-183`](GAP-183-refund-dispute-resolution-policy.md) (refund), GAP-185 (VAT — re-scope to MISA partnership), [`GAP-594`](GAP-594-refund-policy-30-day-money-back.md) (30-day money-back align với manual refund SOP)
- **Phase 1.5 plan:** [`release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md) §4
- **Rules:** [`outside-in-coverage-trigger.md`](../../../.claude/rules/outside-in-coverage-trigger.md), [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §2.5, [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.4 §2.5 §2.6, [`release-deploy-standard.md`](../../../.claude/rules/release-deploy-standard.md) §3.1 Pre-release Security row
- **Compliance refs:** Luật Phòng chống rửa tiền 2022 + Nghị định 19/2023/NĐ-CP (KYC mandatory cho merchant); NAPAS VietQR TLV spec; PostgreSQL immutable trigger pattern V60 (admin_audit_logs)

## Log

- **2026-05-18** — Initial write-up. Filed via Wave 93 outside-in audit (3-agent convergence per `outside-in-coverage-trigger.md`). State-check (per `audit-to-gap-pipeline.md` §2.5) confirms tenant-Owner-PH payment scope greenfield (existing `QRCodeDisplay.tsx` + subscription `PaymentController` cover KiteHub-pays-tenant scope, NOT tenant-Owner-PH scope). Priority P0 — blocking Phase 1.5 PAID launch trigger. Combo scope (KYC + binding + audit log) justified bằng shared dataflow + atomic foundation block; tách rời = half-shipped P0.
