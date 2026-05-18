# GAP-632: Manual mark-paid audit trail + override approval flow

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Backend + Frontend + Compliance)
**Detected:** 2026-05-18
**Affects:** P1 Solo Teacher + P2 Center Owner + P3 Center Manager (role-guard scope); kiteclass-core invoice domain mark-paid action layer; admin_audit_logs (paired GAP-625); kitehub-email notification trigger

---

## Current State (verified 2026-05-18)

Phase 1.5 PAID scope = greenfield. Mark-paid action layer + idempotency safeguards chưa tồn tại:

- `grep -rl "markAsPaid\|mark_paid\|MARK_PAID\|markPaid" kiteclass/ kitehub/ --include="*.java" --include="*.ts" --include="*.tsx" 2>/dev/null` → 0 hit (no mark-paid implementation)
- `grep -rl "idempoten\|Idempoten" kiteclass/kiteclass-core/src/main/java --include="*.java" 2>/dev/null` → 0 hit (no idempotency middleware)
- `grep -rl "reverse.*paid\|untag.*paid\|unmark" kiteclass/ kitehub/ --include="*.java" --include="*.tsx" 2>/dev/null` → 0 hit (no reversal flow)
- Migration scan `find */src/main/resources/db/migration -name "*mark_paid*" -o -name "*invoice*idempot*" 2>/dev/null` → 0 hit
- Existing audit log foundation `grep -rl "admin_audit_logs\|AdminAuditLog" kiteclass/ kitehub/ --include="*.java" 2>/dev/null` → 0 hit (GAP-625 P0 foundation chưa close → audit log table chưa exist)

Verdict: build-from-scratch entirely. AC framing = greenfield idempotency layer + audit trail + reversal flow + Manager approval workflow + UI safeguards. Direct dependency lên GAP-625 (admin_audit_logs schema) + paired GAP-630 (evidence screenshot mandatory).

---

## Problem

Manual mark-paid action surface 2 cluster của failure modes:

### Cluster A: Double-payment risk (PH-side)

- PH chuyển 2 lần vì không thấy confirmation timely
- KiteHub không broker tiền → không có gateway txn_id để idempotency check natural
- Without idempotency safeguard: Owner mark invoice = PAID 2 lần khi PH thực sự chuyển 2 lần → audit confused → refund nightmare

### Cluster B: Anti-fraud Owner-side

- Owner mark "đã thu" có thể **fake** without actual money transfer evidence
- Trợ giảng cấp 2 (Manager scope) có thể bypass Owner approval → unauthorized mark-paid
- Reverse mark-paid (Owner click "untag paid") không có audit log → trail breaks → dispute resolution impossible
- Multi-Owner scenarios (co-owners / partnership centers) — không có cross-approval safeguard

3 outside-in agents converge xác nhận:

- **Failure-mode matrix agent** (P1 idempotency): "No idempotency = double-payment risk" + "Transaction audit immutability — Owner edit 'mark paid' log sau-the-fact"
- **Persona walkthrough agent** (P1 thầy Tâm edge case): "PH chuyển 2 lần vì không thấy confirm" — idempotency safeguard critical
- **External benchmark agent**: MISA EMIS + DotB EMS đều có Manager-approval workflow cho large amount thresholds + audit log mandatory cho all mark-paid actions

**Cost of miss** without these safeguards:
- Phase 1.5 BETA P0 incident risk: double-payment events surface trong first month → tenant trust loss → bounce
- Compliance risk: PDPL Art 11 audit trail incomplete → fine risk + ISO27001 readiness blocked
- Dispute resolution gap: 6-month window claims không có evidence → "he-said-she-said" stalemate

---

## Root Cause

Manual mark-paid trong non-broker model lacks 2 properties processor-broker model có sẵn:

1. **Gateway txn_id natural idempotency** — processor returns unique txn_id; replay = same txn_id detected. Without processor: KiteHub PHẢI generate idempotency key + check itself.
2. **Multi-party verification** — processor verifies bank-side balance change; without processor: KiteHub PHẢI require Owner upload evidence (paired GAP-630) + Manager-tier approval cho large amounts.

Substitute architectural pattern: **explicit idempotency token + mandatory evidence link + role-tiered approval workflow + immutable audit log**. Industry pattern mirrors Stripe Connect platform model where platform doesn't broker funds but provides ledger of record với explicit double-entry validation.

---

## Proposed Fix

### Component 1: Idempotency middleware (Backend)

- Add HTTP middleware `IdempotencyKeyFilter` checking `Idempotency-Key` header on mark-paid endpoints
- Storage: Redis với 24-hour TTL per key (key namespace `idempotency:{endpoint}:{tenant_id}:{user_id}:{client_key}`)
- Flow:
  1. Request arrives với `Idempotency-Key: <uuid>` header
  2. Check Redis: key exists? → return cached response (status + body)
  3. Key not exists → process request → store response in Redis với TTL
  4. Idempotency window: 24 hours (any replay within 24h → same response)
- Apply to endpoints: `POST /api/v1/invoices/{id}/mark-paid` (single) + `POST /api/v1/invoices/batch-mark-paid` (batch, per-row idempotency per GAP-628)

### Component 2: Mark-paid action schema (Backend)

Migration `V{N}__create_invoice_payment_actions_table.sql`:

```sql
CREATE TABLE invoice_payment_actions (
  action_id BIGSERIAL PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
  invoice_id BIGINT NOT NULL REFERENCES invoices(invoice_id),
  action_type VARCHAR(30) NOT NULL, -- 'MARK_PAID' | 'REVERSE_MARK_PAID' | 'OVERRIDE_DOUBLE_MARK'
  amount_marked NUMERIC(12,2) NOT NULL,
  evidence_id BIGINT REFERENCES payment_evidence(evidence_id), -- paired GAP-630, mandatory cho MARK_PAID
  idempotency_key UUID NOT NULL,
  performed_by UUID NOT NULL REFERENCES users(user_id),
  performed_by_role VARCHAR(30) NOT NULL,
  reason VARCHAR(500),
  requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
  approval_status VARCHAR(20), -- 'PENDING' | 'APPROVED' | 'REJECTED' (null if not required)
  approved_by UUID REFERENCES users(user_id),
  approved_at TIMESTAMPTZ,
  rejection_reason TEXT,
  performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (tenant_id, invoice_id, idempotency_key)
);

CREATE INDEX idx_payment_actions_invoice ON invoice_payment_actions(invoice_id, performed_at DESC);
CREATE INDEX idx_payment_actions_approval ON invoice_payment_actions(approval_status) WHERE approval_status = 'PENDING';
```

UNIQUE constraint `(tenant_id, invoice_id, idempotency_key)` = double-mark prevention at DB layer (defense-in-depth on top of Redis layer).

### Component 3: Mark-paid endpoint với evidence mandatory (Backend)

- Endpoint `POST /api/v1/invoices/{id}/mark-paid`
- Request body:
  ```json
  {
    "amount_received": 1500000,
    "evidence_id": <evidence_id from GAP-630 upload>,
    "payment_received_at": "2026-05-15T09:30:00+07:00",
    "memo": "PH chuyển khoản thành công via VietQR"
  }
  ```
- Required header: `Idempotency-Key: <uuid v4>`
- Validation:
  - `evidence_id` mandatory (cannot be null) — enforces GAP-630 evidence upload precedent
  - `amount_received` vs `invoice.expected_amount` mismatch detection (paired GAP-627) → if mismatch → require additional `mismatch_acknowledgment_reason` field
  - Role-guard P2 Owner only (P3 Manager → triggers Component 5 approval workflow)
- Response: action_id + idempotency_key echo + audit_log_id

### Component 4: Reverse mark-paid (untag paid) endpoint (Backend)

- Endpoint `POST /api/v1/invoices/{id}/reverse-mark-paid`
- Request body:
  ```json
  {
    "reverse_action_id": <original mark-paid action_id>,
    "reason": "PH yêu cầu hủy, học sinh không học nữa", // required, min 20 chars
    "reason_category": "STUDENT_WITHDRAWAL" // enum: STUDENT_WITHDRAWAL | DUPLICATE_PAYMENT | AMOUNT_DISPUTE | OWNER_ERROR
  }
  ```
- Reverse action creates new row in `invoice_payment_actions` với `action_type = 'REVERSE_MARK_PAID'`
- Invoice status reverts to PENDING; UI display shows "Marked PAID on YYYY-MM-DD by [name], reversed on YYYY-MM-DD by [name] — reason: [...]"
- **Reverse PHẢI require Manager approval** for P3 Manager role; Owner-tier auto-approve
- Audit log immutable: original mark-paid event KHÔNG bị delete (append-only) → reverse event linked via `reverse_action_id`

### Component 5: Manager approval workflow (Backend + Frontend)

Tiered approval matrix:

| Actor role | Action | Approval required? |
|---|---|---|
| P2 Owner | MARK_PAID any amount | Auto-approved |
| P2 Owner | REVERSE_MARK_PAID any amount | Auto-approved (with reason mandatory) |
| P3 Manager | MARK_PAID amount < 500,000đ | Auto-approved |
| P3 Manager | MARK_PAID amount ≥ 500,000đ | Owner approval required |
| P3 Manager | REVERSE_MARK_PAID any amount | Owner approval required |
| P3 Manager | OVERRIDE_DOUBLE_MARK | Owner + Platform Admin approval required |
| Platform Admin | All actions | Auto-approved (system-tier) |

When approval required:
1. Manager creates action → status `PENDING` → `requires_approval = true`
2. Owner receives notification (email + in-app) "Manager yêu cầu approve mark-paid [invoice] [amount]đ — reason: [...]"
3. Owner clicks approval link → review screen → approve hoặc reject với reason
4. Approval recorded → action status → APPROVED hoặc REJECTED
5. If APPROVED → invoice status flip → PAID; audit log appends approval event
6. If REJECTED → audit log appends rejection event; Manager notified với rejection reason

### Component 6: Override double-mark flow (rare, P0 incident path)

- Khi UNIQUE constraint violation surfaces (same `idempotency_key` retry → blocked by DB)
- Owner inspect action history → see existing mark-paid → decision: "this is correct duplicate (actual second payment from PH)" OR "this is system error (replay)"
- For "actual second payment" scenario: create new action với `action_type = 'OVERRIDE_DOUBLE_MARK'` + fresh idempotency_key + Owner reason + new evidence_id (second receipt screenshot)
- Override requires both Owner + Platform Admin approval (highest tier — prevents fraud)
- Audit log per state transition

### Component 7: UI safeguards (Frontend)

- Invoice detail page shows previous mark-paid status prominently:
  ```
  Previously marked PAID on 15/05/2026 by Trần Thị Hồng (Owner)
  Evidence: receipt-evidence-12345.png (hash verified)
  Amount: 1.500.000đ
  ```
- "Mark as paid" button DISABLED if already PAID; shows "Already paid — click to view evidence" instead
- Reverse mark-paid button visible only sau confirmation modal "Bạn có chắc muốn hủy đánh dấu đã thanh toán? Hành động sẽ được audit log."
- Bulk mark-paid (GAP-628) shows per-row preview với existing PAID rows greyed out

### Component 8: Audit log integration (Backend, paired GAP-625)

Every action transition logs đến `admin_audit_logs` (per GAP-625 immutable log schema) với:

```
action ∈ {
  'invoice_mark_paid',
  'invoice_reverse_mark_paid',
  'invoice_override_double_mark',
  'invoice_mark_paid_approval_requested',
  'invoice_mark_paid_approval_granted',
  'invoice_mark_paid_approval_rejected'
}
target_type = 'invoice' target_id = <invoice_id>
metadata = {action_id, amount_marked, evidence_id, idempotency_key, performed_by_role, reason, ...}
```

PDPL Art 11 + ISO27001 compliance — non-repudiation enforced via immutable append + cryptographic chain (per GAP-625 V60 hash-chain audit log).

---

## Acceptance Criteria

- [ ] **AC-632.1:** Idempotency middleware integrated; test: same `Idempotency-Key` replayed 3 lần → exactly 1 mark-paid action committed, 2 cached responses returned
- [ ] **AC-632.2:** Migration `V{N}__create_invoice_payment_actions_table.sql` apply success on staging
- [ ] **AC-632.3:** UNIQUE constraint `(tenant_id, invoice_id, idempotency_key)` enforces DB-layer double-mark prevention; test: bypass Redis (e.g., flush cache) → DB blocks 2nd insert với 23505 unique violation
- [ ] **AC-632.4:** Mark-paid endpoint rejects request without `evidence_id` (400 error citing GAP-630 evidence mandate)
- [ ] **AC-632.5:** Amount mismatch detection integrated với GAP-627 — `amount_received` differs from `invoice.expected_amount` → requires `mismatch_acknowledgment_reason` field (test: same amount → no extra field; mismatch → 400 without reason field)
- [ ] **AC-632.6:** Reverse mark-paid flow tested end-to-end: Owner reverses 1 paid invoice → invoice status reverts PENDING → audit log shows both events linked
- [ ] **AC-632.7:** Manager approval workflow tested 6 matrix rows (per §5 matrix); each role × amount × action_type combination produces correct approval requirement verdict
- [ ] **AC-632.8:** Owner approval notification trigger via kitehub-email; delivery success rate ≥98% trên test cohort 30 approval requests
- [ ] **AC-632.9:** Override double-mark flow requires Owner + Platform Admin dual-approval; test: Manager initiates → Owner approves but Admin rejects → action status REJECTED
- [ ] **AC-632.10:** UI safeguards visible: "Previously marked PAID on YYYY-MM-DD by [name]" badge renders correctly trên invoice detail page; "Mark as paid" button DISABLED state styled consistently with design system
- [ ] **AC-632.11:** Audit log integration — 6 distinct action types log to `admin_audit_logs`; test full mark → reverse → re-mark cycle → ≥4 audit log rows linked via `reverse_action_id`
- [ ] **AC-632.12:** Role-guard tests all 4 personas — P2 Owner ✅ all actions; P3 Manager ✅ actions <500k auto + ≥500k pending Owner approval; PH ❌ 403 forbidden; Platform Admin ✅ all + override capabilities
- [ ] **AC-632.13:** Business doc 3-layer sync — `documents/01-business/kitehub/invoice-payment-actions/rules.md` + `use-cases.md` + `api-contract.md` updated trong same PR; config keys for approval thresholds (default 500,000đ) cited per rules.md hardcode-free convention

---

## Related

### Sibling P1 gaps (Wave 33-34 Phase 1.5b cluster, paired same audit)

- [GAP-628: QR batch reconcile API](GAP-628-qr-batch-reconcile-api.md) — direct consumer (per-row idempotency in batch)
- [GAP-629: QR refund workflow SOP](GAP-629-qr-refund-workflow-sop.md) — sibling action layer (refund vs reverse-mark-paid distinct paths)
- [GAP-630: QR evidence receipt storage](GAP-630-qr-evidence-receipt-storage.md) — direct dependency (`evidence_id` mandatory for mark-paid)
- [GAP-631: QR account verification quarterly refresh](GAP-631-qr-account-verification-refresh.md) — interplay during 30-day grace period (both QR acceptable for mark-paid)

### P0 foundation (MUST close trước Phase 1.5 launch — Wave 31-32 Phase 1.5a)

- **GAP-625: QR payment foundation — KYC + multi-tenant binding + immutable audit** — direct dependency cho audit log schema + V60 hash-chain immutability
- GAP-626: QR payment PDPL transaction PII handling + consent collection — PII redaction in audit log metadata
- **GAP-627: Payment-amount mismatch detection + UI alert workflow** — direct integration `mismatch_acknowledgment_reason` field per AC-632.5

### Compliance + standards references

- `.claude/rules/pre-handoff-self-test-completeness.md` §2.6 payment flow checklist (gateway redirect / webhook signature / idempotency key / reconciliation / audit log) — adapted cho non-broker scope
- `.claude/rules/design-patterns.md` §3 anti-patterns — non-repudiation enforcement
- PDPL 2023 + Decree 13/2023/NĐ-CP Art 11 — financial transaction audit trail
- ISO27001:2022 A.5.32 — privileged access management for approval workflows
- VN Civil Code Art 429 — dispute evidence requirements

### Audit + planning references

- [Outside-in audit Phase 1.5 QR payment](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md) §2.1 P1 thầy Tâm edge case PH chuyển 2 lần + §2.3 failure-mode anti-fraud Owner + §3 cross-cutting "No idempotency = double-payment risk" + §5.1 P1 gaps section
- Wave plan: `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md`
- `documents/03-planning/roadmap/release-1-plan-2026.md` §4 Phase 1.5 PAID
- ROADMAP.md §🚀 Next Action — Wave 93+ queue

---

## Log

- **2026-05-18:** Gap filed. Triggered by 3-agent outside-in audit của Phase 1.5 QR payment proposal — failure-mode matrix surface "No idempotency = double-payment risk" + "Anti-fraud Owner verification" + persona walkthrough P1 edge case PH chuyển 2 lần + benchmark agent industry-norm Manager approval workflow + audit log mandatory cho all mark-paid actions. Scope: 8 components (idempotency middleware + DB schema with UNIQUE constraint + mark-paid endpoint with evidence mandatory + reverse mark-paid + Manager approval workflow + override double-mark + UI safeguards + audit log integration). Direct dependency lên GAP-625 (audit log schema) + GAP-627 (amount mismatch) + GAP-630 (evidence mandatory). Wave 33-34 Phase 1.5b target window; P0 foundation MUST close concurrent precedent. Author: @nguyenvankiet (solo-dev). Verified greenfield via state-check §Current State (5 grep/find commands, 0 hits per `audit-to-gap-pipeline.md` §2.5 mandate).
