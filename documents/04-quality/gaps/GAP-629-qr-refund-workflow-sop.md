# GAP-629: QR refund workflow SOP — manual out-of-band transfer tracked in KiteHub

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Backend + Frontend + Ops runbook)
**Detected:** 2026-05-18
**Affects:** P1 Solo Teacher + P2 Center Owner (refund path universal); kitehub-subscription / kiteclass-core invoice + refund domain; SOP runbook ownership Owner-side execution

---

## Current State (verified 2026-05-18)

Phase 1.5 PAID scope = greenfield. Refund engine chưa tồn tại tại thời điểm audit:

- `grep -rl "refund\|Refund\|REFUND" kiteclass/ kitehub/ --include="*.java" --include="*.ts" --include="*.tsx" 2>/dev/null` → 0 hit (no refund domain code)
- `find kiteclass kitehub -path '*/refund*' 2>/dev/null` → 0 hit
- Migration scan `find */src/main/resources/db/migration -name "*refund*" 2>/dev/null` → 0 hit
- Runbook search `find documents/05-guides -iname "*refund*" 2>/dev/null` → 0 hit
- Existing gap GAP-183 (Refund + Dispute Resolution Policy) đã được re-scoped trong audit §5.2 từ self-build refund engine → manual SOP — gap này (GAP-629) là implementation gap of re-scoped GAP-183 policy

Verdict: build-from-scratch. AC framing = greenfield manual SOP workflow + tracking UI + audit log + PH confirmation flow. GAP-183 trở thành parent policy gap; GAP-629 là execution gap.

---

## Problem

PH (phụ huynh) yêu cầu refund (hoàn tiền). Bối cảnh điển hình:
- Học sinh nghỉ giữa khoá (chuyển trường, bệnh kéo dài, gia đình chuyển nhà)
- Trung tâm cancel khoá không đủ học sinh đăng ký
- 30-day money-back guarantee promotion (per GAP-594)
- Dispute resolution outcome favorable for PH

Vì KiteHub stay **non-PSP** (không broker tiền per audit §4.3 revenue model implication + benchmark pitfall #1 "PSP license risk"), Owner **PHẢI tự chuyển hoàn manually** qua banking app cá nhân / Momo / VietQR. KiteHub không hold funds → không thể trigger automated refund từ payment gateway như Stripe/MoMo merchant-flow.

Tuy nhiên, **manual refund outside KiteHub** = problem:
1. **No audit trail** — Owner mark "đã hoàn" có thể fake; no banking API verify (failure-mode agent verdict)
2. **PH dispute 6 tháng sau** — "tôi chưa nhận" — không có evidence trail (failure-mode agent #6 dispute window)
3. **Compliance gap PDPL Art 11** — financial transaction PII không được proper consent + retention (paired GAP-626 scope)
4. **No reconciliation với invoices** — refund event không link với original invoice → reporting confused

3 outside-in agents converge khẳng định cần workflow này:

- **Persona walkthrough agent**: P1 thầy Tâm scenario 4 "refund khi học sinh nghỉ giữa khoá" — currently zero KiteHub support → Owner tự handle messy
- **External benchmark agent**: DotB EMS + VnResource đều có refund tracking workflow (không broker tiền nhưng track ledger entries); P2 size centers expect this as standard
- **Failure-mode matrix agent**: 2 P1 failures convergent — "Audit trail weak vs processor" + "Refund evidence dispute window 6 tháng"

---

## Root Cause

Phase 1.5 stay non-broker model (mandatory do PSP license risk + KYC merchant onboarding barrier per audit §4.3) → no automated refund path. Tuy nhiên, KiteHub có thể (và phải) act as **ledger of record** cho refund events Owner thực hiện out-of-band, tương tự cách marketplace platforms như Etsy/Shopify track seller-side cash refunds: platform không broker nhưng require seller log refund event với evidence cho compliance + dispute resolution.

Pattern này tách rõ:
- **Money flow**: Owner-side manual banking app transfer (outside KiteHub scope)
- **Record flow**: KiteHub track refund event với evidence + PH confirmation (within KiteHub scope)

---

## Proposed Fix

### Component 1: Refund request UI (Frontend)

- Trigger điểm: Invoice detail page → action button "Yêu cầu hoàn tiền"
- Form fields:
  - `refund_amount` (default = invoice paid amount; editable cho partial refund)
  - `reason` (dropdown: học sinh nghỉ / cancel khoá / 30-day guarantee / dispute resolution / other với free-text)
  - `bank_account_recipient` (PH cung cấp STK + tên chủ thẻ — Owner nhập từ liên lạc với PH)
  - `evidence_screenshot` (upload — Owner sau khi chuyển khoản screenshot banking confirmation, hash stored per GAP-630)
- Form validation: refund_amount ≤ original paid amount; bank_account format VN (9-14 chữ số)

### Component 2: Refund record schema (Backend)

Migration `V{N}__create_refunds_table.sql`:

```sql
CREATE TABLE refunds (
  refund_id BIGSERIAL PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
  original_invoice_id BIGINT NOT NULL REFERENCES invoices(invoice_id),
  refund_amount NUMERIC(12,2) NOT NULL CHECK (refund_amount > 0),
  refund_method VARCHAR(50) NOT NULL DEFAULT 'MANUAL_BANK_TRANSFER',
  reason VARCHAR(100) NOT NULL,
  reason_detail TEXT,
  recipient_bank_account VARCHAR(20),
  recipient_name VARCHAR(200),
  evidence_screenshot_id BIGINT REFERENCES refund_evidence(evidence_id), -- paired GAP-630
  evidence_screenshot_hash VARCHAR(64), -- SHA-256
  refund_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_OWNER_TRANSFER',
  refunded_at TIMESTAMPTZ,
  confirmed_by_parent_at TIMESTAMPTZ,
  confirmation_token UUID,
  created_by UUID NOT NULL REFERENCES users(user_id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refunds_tenant ON refunds(tenant_id, refund_status);
CREATE INDEX idx_refunds_invoice ON refunds(original_invoice_id);
```

State machine: `PENDING_OWNER_TRANSFER → OWNER_TRANSFERRED (Owner mark + upload evidence) → CONFIRMED_BY_PARENT (PH click confirm link) → CLOSED`. Path alternative: `DISPUTED` (PH không confirm 7 ngày + Owner cần escalation per GAP-183 dispute policy).

### Component 3: PH confirmation flow (Backend + Frontend)

- Sau Owner mark "đã chuyển hoàn" + upload evidence screenshot → backend generate `confirmation_token` UUID
- Trigger email + SMS cho PH (per kitehub-email service + Resend/SES integration):
  - Email body: "Trung tâm [tên TT] đã hoàn tiền học phí [số tiền]đ vào STK [redacted XXXX1234] tên [redacted N***N V***A] của bạn. Vui lòng kiểm tra banking app + click link xác nhận: https://kitehub.me/refund-confirm?token=<UUID>"
- Confirmation page (public route `/refund-confirm`): tokenized, không cần PH login (PH có thể chưa có KiteHub account); show refund detail + buttons "Tôi đã nhận tiền" / "Tôi chưa nhận tiền"
- "Tôi đã nhận tiền" → `confirmed_by_parent_at = NOW()`, status → CONFIRMED_BY_PARENT
- "Tôi chưa nhận tiền" → flag dispute, notify Owner + escalation per GAP-183
- 7-day timeout không response → auto-flag dispute, notify Owner; Owner có thể manual resend confirmation email

### Component 4: Audit log immutable append (Backend, paired GAP-625)

Per GAP-625 immutable `admin_audit_logs` schema:

```
action='refund_initiated' OR 'refund_owner_marked_transferred' OR 'refund_parent_confirmed' OR 'refund_disputed' OR 'refund_closed'
target_type='refund' target_id=<refund_id>
actor_id=<user_id> tenant_id=<tenant_id>
metadata={refund_amount, original_invoice_id, evidence_hash, ...}
```

PDPL Art 11 audit trail compliance — paired GAP-626 PII consent + GAP-630 evidence retention.

### Component 5: SOP runbook (Documentation)

`documents/05-guides/operations/refund-workflow-runbook.md` — Owner-side step-by-step:

1. **Initiate**: PH liên hệ → Owner tạo refund request trong KiteHub UI với reason + amount + PH bank info
2. **Transfer**: Owner mở banking app (Vietcombank / Techcombank / MB / Momo / VietQR) → chuyển tiền theo amount + STK đã nhập
3. **Evidence**: Screenshot confirmation banking app → upload qua KiteHub UI → SHA-256 hash auto-compute + store
4. **Mark**: Click "Tôi đã chuyển hoàn" → status flip → KiteHub email PH confirmation link
5. **Confirm**: PH click confirm → status CONFIRMED; OR 7-day timeout → dispute escalation per GAP-183
6. **Close**: Owner verify confirmation + close refund record

Edge cases trong runbook: partial refund cho per-month proration (per GAP-108 re-scoped), dispute escalation when PH không respond, evidence hash tamper detection, refund cho multi-invoice bundles.

---

## Acceptance Criteria

- [ ] **AC-629.1:** Refund request UI accept all 5 fields + form validation passes for valid input + rejects invalid (test: invalid bank account format → inline error)
- [ ] **AC-629.2:** Migration `V{N}__create_refunds_table.sql` ship + Flyway apply success trên staging RDS
- [ ] **AC-629.3:** State machine transitions: PENDING_OWNER_TRANSFER → OWNER_TRANSFERRED → CONFIRMED_BY_PARENT → CLOSED → all 4 paths covered by integration test
- [ ] **AC-629.4:** PH confirmation email trigger via kitehub-email service → Resend/SES delivery success rate ≥98% trên test cohort 50 emails
- [ ] **AC-629.5:** Public confirmation route `/refund-confirm?token=<UUID>` works without login (test: incognito browser → page renders + buttons functional)
- [ ] **AC-629.6:** 7-day timeout dispute escalation tested via clock-skew (set system clock +8 days → auto-flag dispute job runs → notification fires)
- [ ] **AC-629.7:** Audit log `admin_audit_logs` rows append per state transition (test: 1 refund full cycle → 4 audit log rows: initiated / owner_marked / parent_confirmed / closed)
- [ ] **AC-629.8:** PDPL Art 11 compliance — PH PII (bank account + name) redacted trong email body + logged with consent flag (paired GAP-626 verify)
- [ ] **AC-629.9:** SOP runbook reviewed by Owner persona walkthrough — 1 P2 Owner (chị Hằng simulated) complete full refund cycle ≤15 phút theo runbook
- [ ] **AC-629.10:** Business doc 3-layer sync — `documents/01-business/kitehub/refund/rules.md` + `use-cases.md` + `api-contract.md` updated trong same PR
- [ ] **AC-629.11:** GAP-183 status reconcile — GAP-183 flip to 🟢 DONE (policy shipped) khi GAP-629 (implementation) close

---

## Related

### Sibling P1 gaps (Wave 33-34 Phase 1.5b cluster, paired same audit)

- [GAP-628: QR batch reconcile API](GAP-628-qr-batch-reconcile-api.md)
- [GAP-630: QR evidence receipt storage](GAP-630-qr-evidence-receipt-storage.md) — direct dependency cho evidence screenshot hash + metadata
- [GAP-631: QR account verification quarterly refresh](GAP-631-qr-account-verification-refresh.md)
- [GAP-632: Manual mark-paid audit trail + override](GAP-632-qr-manual-mark-paid-audit-trail.md)

### P0 foundation (MUST close trước Phase 1.5 launch — Wave 31-32 Phase 1.5a)

- GAP-625: QR payment foundation — KYC + multi-tenant binding + immutable audit (direct dependency cho audit log schema)
- GAP-626: QR payment PDPL transaction PII handling + consent collection (direct dependency cho PH PII redaction)
- GAP-627: Payment-amount mismatch detection (direct dependency cho refund amount validation)

### Re-scoped existing gaps

- **GAP-183: Refund + Dispute Resolution Policy** — re-scoped trong audit §5.2 từ self-build refund engine → manual SOP + dispute escalation. GAP-183 = parent policy gap; GAP-629 = implementation gap. Close GAP-183 khi GAP-629 ship.
- **GAP-594: 30-day money-back doc** — align với manual refund SOP (this gap provides implementation backbone cho 30-day guarantee fulfillment)
- **GAP-108: Payment-Invoice Rules** — refund amount validation references invoice config keys

### Audit + planning references

- [Outside-in audit Phase 1.5 QR payment](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md) §2.1 P1 thầy Tâm scenario 4 + §2.2 benchmark DotB/VnResource + §2.3 failure-mode #4 audit trail + §5.2 GAP-183 re-scope decision
- Wave plan: `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md`
- `documents/03-planning/roadmap/release-1-plan-2026.md` §4 Phase 1.5 PAID
- ROADMAP.md §🚀 Next Action — Wave 93+ queue

---

## Log

- **2026-05-18:** Gap filed. Triggered by 3-agent outside-in audit của Phase 1.5 QR payment proposal — failure-mode agent surface "audit trail weak vs processor" + persona walkthrough surface "refund khi học sinh nghỉ giữa khoá" + benchmark agent confirm industry norm refund tracking workflow for non-broker SaaS platforms. Scope: 5 components (UI request + DB schema + PH confirm flow + audit log integration + SOP runbook). Replaces GAP-183 self-build refund engine scope — GAP-183 trở thành parent policy gap, GAP-629 implementation gap. Wave 33-34 Phase 1.5b target window; P0 foundation (GAP-625/626/627) + sibling GAP-630 (evidence storage) MUST close concurrent/precedent. Author: @nguyenvankiet (solo-dev). Verified greenfield via state-check §Current State (5 grep/find commands, 0 hits per `audit-to-gap-pipeline.md` §2.5 mandate).
