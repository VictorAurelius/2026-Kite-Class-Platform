# GAP-631: QR account verification quarterly refresh + lifecycle notification

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Backend + Frontend + Notification service)
**Detected:** 2026-05-18
**Affects:** P1 Solo Teacher + P2 Center Owner (QR account lifecycle universal); kitehub-subscription tenant payment account domain; kitehub-email + SMS notification service; PH-facing UI changes when QR rotation

---

## Current State (verified 2026-05-18)

Phase 1.5 PAID scope = greenfield. QR account lifecycle management chưa tồn tại:

- `grep -rl "qr_account\|payment_account.*expire\|account.*verification.*refresh" kiteclass/ kitehub/ --include="*.java" --include="*.ts" --include="*.tsx" 2>/dev/null` → 0 hit
- `find kiteclass kitehub -path '*/payment_account*' -o -path '*/qr_lifecycle*' 2>/dev/null` → 0 hit
- Notification scan `find kitehub/kitehub-email -path '*/qr*' -o -path '*/account.*refresh*' 2>/dev/null` → 0 hit
- Migration scan `find */src/main/resources/db/migration -name "*qr*account*" -o -name "*payment*account*" 2>/dev/null` → 0 hit

Existing kitehub-email service infrastructure (per Resend/SES integration foundation gap GAP-370) provides notification backbone — extensible cho quarterly refresh notification trigger.

Verdict: build-from-scratch. AC framing = greenfield account lifecycle workflow + notification cron + grace period logic + PH-facing UI rotation.

---

## Problem

QR account stale scenarios surface trong realistic Phase 1.5 BETA lifecycle:

1. **Owner đổi ngân hàng** — chị Hằng switch primary banking từ Vietcombank sang Techcombank → STK cũ không còn active → PH chuyển vào account cũ → tiền mất hoặc kẹt
2. **Nhân viên offboarding** — Trợ giảng cấp 2 có QR cá nhân làm proxy collection point → nhân viên nghỉ → bank account cá nhân disconnected → PH chuyển tiếp vào account đã rời
3. **Chồng resign account** — hộ kinh doanh dạy thêm thường dùng vợ/chồng account cho QR display → ly hôn / chuyển sở hữu → account ownership ambiguity
4. **Bank account closure** — Vietcombank đóng account inactive sau 12 tháng không transaction → QR STK cũ rỗng

3 outside-in agents converge xác nhận lifecycle risk:

- **Failure-mode matrix agent** (P0 cluster): "Multi-tenant QR binding leak — QR có thể share across tenants; PH copy nhầm" + Owner lifecycle "Anti-fraud owner verification — QR không validate recipient bank account ownership" + edge case "Mid-cycle QR change conflict — Owner đổi ngân hàng → PH cũ chuyển vào account cũ"
- **Persona walkthrough agent** (P2 chị Hằng scenario 5): GV nghỉ → tiền stuck + Owner mid-cycle bank switch → PH confusion
- **External benchmark agent** (industry pitfall): "Chọn payment processor sớm = compliance hell" → QR-based system requires lifecycle management substitute cho merchant onboarding KYC refresh

**Cost of miss** without lifecycle management:
- PH chuyển nhầm account → "stray payment recovery nightmare" (Owner phải đi đòi PH chuyển lại + return chi phí ngân hàng)
- Owner reputation damage — PH cảm thấy không reliable
- Audit trail incomplete — không có record khi nào QR rotation xảy ra → compliance gap PDPL Art 11

---

## Root Cause

QR is a **static artifact** từ perspective của KiteHub system: chỉ là image URL hoặc base64 string Owner upload. Không có inherent lifecycle (không expire automatically, không validate bank-side). Đây là tradeoff fundamental của non-broker model: KiteHub không có API integration với VietQR / NAPAS / banking gateway để validate STK ownership real-time.

Substitute solution: **periodic Owner self-attestation** ("STK này còn active không?") + **grace period dual-acceptance** (old + new QR cùng accepted 30 ngày) + **PH-facing notification** ("Trung tâm đã đổi STK, vui lòng dùng QR mới"). Pattern này industry-norm cho self-service KYC trong non-broker platforms (e.g., Airbnb host bank account confirmation flow, Etsy seller payout account verification reminder).

---

## Proposed Fix

### Component 1: Quarterly notification cron (Backend scheduled job)

- Scheduled job `qr-account-quarterly-verification` runs ngày 1 mỗi quarter (Jan 1 / Apr 1 / Jul 1 / Oct 1)
- Query: `SELECT tenant_id, owner_user_id, payment_account_id FROM payment_accounts WHERE last_verified_at < NOW() - INTERVAL '90 days' AND status = 'ACTIVE'`
- For each: trigger email + SMS notification cho Owner via kitehub-email + Resend/SES integration
- Notification template (Vietnamese narrative):
  ```
  Subject: [KiteHub] Xác nhận STK nhận học phí hiện tại
  Body: Chào [Owner name], đã 3 tháng từ lần xác nhận STK học phí gần nhất.
  Vui lòng xác nhận STK [redacted XXXX1234] tại ngân hàng [bank name] vẫn còn active...
  [Yes button → confirm active] [No button → upload QR mới]
  ```
- Track notification delivery + 14-day response SLA → escalation email lần 2 nếu không respond → 30-day → flag account `VERIFICATION_OVERDUE` status

### Component 2: Owner confirmation flow (Frontend + Backend)

- Dedicated route `/account/payment-verification` cho Owner role
- UI 2 options:
  - **"STK của tôi vẫn còn active"** → backend updates `last_verified_at = NOW()` + audit log `qr_account_verified`
  - **"Tôi cần đổi STK / upload QR mới"** → trigger rotation flow (Component 3)
- Form validation: confirmation timestamp recorded, reason note optional cho "no change" path
- Audit log mandatory per GAP-625 immutable log: `qr_account_verified_active` OR `qr_account_rotation_initiated`

### Component 3: QR rotation flow với 30-day grace period (Backend)

Migration `V{N}__create_payment_account_rotations_table.sql`:

```sql
CREATE TABLE payment_account_rotations (
  rotation_id BIGSERIAL PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
  payment_account_id BIGINT NOT NULL REFERENCES payment_accounts(payment_account_id),
  old_qr_evidence_id BIGINT REFERENCES payment_evidence(evidence_id), -- paired GAP-630
  new_qr_evidence_id BIGINT NOT NULL REFERENCES payment_evidence(evidence_id),
  rotation_reason VARCHAR(100) NOT NULL,
  old_account_grace_period_until DATE NOT NULL, -- rotation_initiated_at + 30 days
  notification_to_parents_sent_at TIMESTAMPTZ,
  rotation_initiated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  rotation_completed_at TIMESTAMPTZ, -- when grace period ends + old QR deactivated
  initiated_by UUID NOT NULL REFERENCES users(user_id),
  status VARCHAR(20) NOT NULL DEFAULT 'GRACE_PERIOD' -- GRACE_PERIOD | COMPLETED | CANCELLED
);

CREATE INDEX idx_rotations_tenant ON payment_account_rotations(tenant_id, status);
CREATE INDEX idx_rotations_grace ON payment_account_rotations(old_account_grace_period_until) WHERE status = 'GRACE_PERIOD';
```

Rotation flow:
1. Owner upload new QR + bank info → new evidence row inserted (paired GAP-630)
2. Backend inserts `payment_account_rotations` row với `status = 'GRACE_PERIOD'`, `old_account_grace_period_until = today + 30 days`
3. KiteHub flags **BOTH** old + new QR as acceptable mark-paid sources for 30 ngày
4. Trigger Component 4 notification flow cho PH
5. Scheduled job `qr-rotation-grace-period-close` runs daily → query expired grace periods → flip `status = 'COMPLETED'` + deactivate old QR (status = 'ROTATED_OUT')

### Component 4: PH-facing notification (Backend + Frontend)

- When rotation initiated → kitehub-email sends batch notification cho all PH có active invoices trong tenant
- Notification template (Vietnamese):
  ```
  Subject: [Trung tâm Sky Education] Cập nhật STK nhận học phí
  Body: Chào quý phụ huynh, trung tâm đã cập nhật STK nhận học phí.
  Từ tháng tới, vui lòng dùng QR mới khi chuyển khoản học phí.
  QR cũ vẫn được chấp nhận đến hết ngày [DD/MM/YYYY] để tránh gián đoạn.
  [Image QR mới embedded] [Image QR cũ với "expires DD/MM" overlay]
  ```
- SMS short version cho PH (160 ký tự): "[Sky Edu] STK học phí đã đổi. QR mới: kitehub.me/qr/[tenant-slug]. QR cũ hết hạn DD/MM."
- Track delivery + click-through rates (analytics for benchmark)
- PH-facing invoice detail page (public route) shows NEW QR by default + collapsed "Show old QR" toggle suốt grace period

### Component 5: Audit log mandatory cho mọi QR change (Backend, paired GAP-625)

Every state transition logs đến `admin_audit_logs` với:

```
action = 'qr_account_verification_reminder_sent' (Component 1)
       | 'qr_account_verified_active' (Component 2 no change)
       | 'qr_account_rotation_initiated' (Component 2 → 3)
       | 'qr_account_rotation_pending_completion' (Component 3 grace period)
       | 'qr_account_rotation_completed' (Component 3 grace period end)
       | 'qr_account_parent_notification_sent' (Component 4 batch sent)
       | 'qr_account_verification_overdue' (Component 1 missed SLA)

target_type = 'payment_account' target_id = <payment_account_id>
actor_id = <user_id or 'SYSTEM'> tenant_id = <tenant_id>
metadata = {rotation_reason, old_qr_hash, new_qr_hash, grace_period_until, ...}
```

PDPL Art 11 audit compliance — paired GAP-626 PII consent scope.

---

## Acceptance Criteria

- [ ] **AC-631.1:** Quarterly cron job runs ngày 1 mỗi quarter; test on staging với clock-skew (set clock to next quarter start → job fires)
- [ ] **AC-631.2:** Email + SMS delivery integration via kitehub-email + Resend/SES; success rate ≥98% trên test cohort 50 notifications
- [ ] **AC-631.3:** Owner confirmation UI 2 paths tested: "still active" path updates `last_verified_at`; "needs rotation" path triggers Component 3 flow
- [ ] **AC-631.4:** 14-day + 30-day escalation logic tested via clock-skew simulation
- [ ] **AC-631.5:** Account `VERIFICATION_OVERDUE` status flag enforced UI-side — Owner sees banner "STK xác nhận quá hạn — vui lòng cập nhật ngay" upon login
- [ ] **AC-631.6:** Migration `V{N}__create_payment_account_rotations_table.sql` apply success
- [ ] **AC-631.7:** Rotation flow integration test: Owner upload new QR → 30-day grace period active → both old + new QR accepted for mark-paid → day 31 → old QR deactivated automatically
- [ ] **AC-631.8:** PH batch notification trigger when rotation initiated — test: tenant với 30 active invoices → 30 PH receive email + SMS within 5 phút
- [ ] **AC-631.9:** PH-facing invoice page shows NEW QR by default + collapsed "Show old QR" toggle suốt grace period; after grace end → only new QR shown
- [ ] **AC-631.10:** Audit log integration — 7 distinct event types log to `admin_audit_logs` per GAP-625 immutable log schema; test full rotation cycle → ≥6 audit log rows
- [ ] **AC-631.11:** Role-guard tests — Owner ✅ own tenant verification flow; PH ❌ verification page 403; Platform Admin ✅ all tenants
- [ ] **AC-631.12:** Business doc 3-layer sync — `documents/01-business/kitehub/payment-account/rules.md` + `use-cases.md` + `api-contract.md` updated trong same PR

---

## Related

### Sibling P1 gaps (Wave 33-34 Phase 1.5b cluster, paired same audit)

- [GAP-628: QR batch reconcile API](GAP-628-qr-batch-reconcile-api.md)
- [GAP-629: QR refund workflow SOP](GAP-629-qr-refund-workflow-sop.md)
- [GAP-630: QR evidence receipt storage](GAP-630-qr-evidence-receipt-storage.md) — direct dependency cho new QR evidence_id storage
- [GAP-632: Manual mark-paid audit trail + override](GAP-632-qr-manual-mark-paid-audit-trail.md) — direct consumer (grace period both QR acceptable)

### P0 foundation (MUST close trước Phase 1.5 launch — Wave 31-32 Phase 1.5a)

- GAP-625: QR payment foundation — KYC + multi-tenant binding + immutable audit (direct dependency cho audit log schema)
- GAP-626: QR payment PDPL transaction PII handling + consent collection (direct dependency cho PII redaction trong PH notifications)
- GAP-627: Payment-amount mismatch detection (consumer dual-QR phase với amount tracking)

### Infrastructure + service references

- kitehub-email service (Resend/SES integration foundation per GAP-370)
- SMS gateway (TBD per Phase 1.5 vendor decision — VnPay SMS / SpeedSMS / Vietguys)
- `documents/05-guides/operations/cloudflare-account-setup-runbook.md` — pattern cho lifecycle SLA monitoring
- `.claude/rules/pre-handoff-self-test-completeness.md` §2.10 time-sensitive flow checklist (TTL / refresh rotation / clock skew)

### Audit + planning references

- [Outside-in audit Phase 1.5 QR payment](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md) §2.1 P2 scenario 5 GV nghỉ + §2.3 failure-mode owner lifecycle + §3 cross-cutting "QR ownership ambiguity catastrophic" + §5.1 P1 gaps section
- Wave plan: `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md`
- `documents/03-planning/roadmap/release-1-plan-2026.md` §4 Phase 1.5 PAID
- ROADMAP.md §🚀 Next Action — Wave 93+ queue

---

## Log

- **2026-05-18:** Gap filed. Triggered by 3-agent outside-in audit của Phase 1.5 QR payment proposal — failure-mode matrix surface "QR ownership ambiguity catastrophic" + persona walkthrough P2 scenario GV nghỉ + benchmark agent industry-pattern self-service KYC reminder cho non-broker platforms (Airbnb host / Etsy seller analog). Scope: 5 components (quarterly cron + Owner confirmation UI + rotation 30-day grace period + PH batch notification + audit log integration). Wave 33-34 Phase 1.5b target window; P0 foundation (GAP-625/626) + GAP-630 evidence storage MUST close concurrent precedent. Author: @nguyenvankiet (solo-dev). Verified greenfield via state-check §Current State (4 grep/find commands, 0 hits per `audit-to-gap-pipeline.md` §2.5 mandate).
