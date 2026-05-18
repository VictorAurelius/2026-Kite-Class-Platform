# GAP-628: QR batch reconcile API — P2 monthly closing bulk mark-paid workflow

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Backend + Frontend)
**Detected:** 2026-05-18
**Affects:** P2 Center Owner (~30 HS × 3 GV = 90+ invoices/tháng); kitehub-subscription / kiteclass-core invoice domain; kiteclass-frontend invoice management UI

---

## Current State (verified 2026-05-18)

Phase 1.5 PAID scope là **greenfield** — chưa có code QR payment foundation nào tồn tại tại thời điểm audit:

- `grep -rl "batch.*paid\|markPaidBatch\|batch-mark-paid" kiteclass/ kitehub/ --include="*.java" --include="*.ts" --include="*.tsx" 2>/dev/null` → 0 hit
- `find kiteclass kitehub -path '*/invoice*' -name "*.java" 2>/dev/null` → 0 hit (chưa có invoice domain module)
- `find kiteclass kitehub -path '*/invoices*' -name "*.tsx" 2>/dev/null` → 0 hit (chưa có FE invoice management page)
- Migration scan `find */src/main/resources/db/migration -name "*invoice*" 2>/dev/null` → 0 hit

Verdict: build-from-scratch sau khi P0 foundation (GAP-625/626/627) close. AC framing = greenfield, không phải delta. Gap status 🔵 OPEN, không phải 🟡 PARTIAL.

---

## Problem

P2 Center Owner (chị Hằng — Trung tâm Anh ngữ Sky Education, ~30 HS × 3 GV = 90+ invoices/tháng) **KHÔNG THỂ** manually click "đã thu" 90 lần mỗi tháng. Persona walkthrough agent verdict cho P2 scope: 🔴 BLOCKER without batch reconcile workflow — 2-4h admin time/tháng chỉ cho mark-paid action, không viable cho center growth path.

3 outside-in agents converge xác nhận pain point này:

1. **Persona walkthrough agent** (id `a22e8469ba8bceef5`): "90+ manual marks/tháng = 2-4h admin/tháng — NOT viable for growth"; conflict trực tiếp với "Enrollment + payment collection" key need của P2 persona
2. **External benchmark agent** (id `a1ee5d6e141e07b42`): industry pitfall #2 — "QR-only KHÔNG có webhook reconcile = manual hell scale lớn — competitors mất khách scale 50→200 HS vì thiếu auto-reconcile"; VnResource + DotB EMS cung cấp bank statement export + reconcile như standard feature cho center size 30+ HS
3. **Failure-mode matrix agent** (id `a2615874804195b90`): P1 failure scenario "Reconciliation at scale — 100 PH × 1200 txn/year không real-time" — Owner manual mark drift dẫn đến missing invoices / wrong-status entries

P1 Solo Teacher scope (~15-20 HS) borderline tolerable với 20-30 marks/tháng (xem GAP-627 cho amount mismatch detection); P2 scope CẦN batch workflow.

---

## Root Cause

Phase 1.5 KiteHub stay **non-PSP** (không broker tiền) → không có webhook auto-reconcile từ payment gateway. Manual mark là essential pattern. Tuy nhiên, manual mark-per-invoice tại scale P2 (90+ tháng) = friction unsustainable. Cần batch workflow + CSV import fuzzy-match để giảm marks/tháng từ 90+ xuống ≤5 batch operations.

Industry benchmark cho thấy hybrid solution = manual + CSV import bank statement với fuzzy-match `amount + transfer_memo → invoice_id` là pattern phổ biến của VN edu SaaS competitors (Easy Edu, DotB, VnResource) cho center size 30-100 HS.

---

## Proposed Fix

### Component 1: Batch mark API (Backend)

- Endpoint `POST /api/v1/invoices/batch-mark-paid` (kiteclass-core invoice domain)
- Request body: list of `{invoiceId, actualAmountReceived (optional)}` rows
- Idempotency-Key header mandatory (per GAP-632 idempotency check)
- Transaction-bounded (all-or-nothing per batch) hoặc per-row PARTIAL response code (200 với `successCount` + `failedRows[]` cho audit clarity)
- Response shape: `{successCount, failedCount, results: [{invoiceId, status, amountMatchVerdict}]}`
- Audit log append per row (per GAP-632 audit trail mandate)
- Role-guard: P2 Center Owner OR Platform Admin only (P3 Manager CHƯA approve cấp này — escalation per GAP-632)

### Component 2: Invoices table UI với bulk-select (Frontend)

- Route `/invoices` cho P2 Owner role
- Table component với column: invoice number / student name / due_date / amount / status / actions
- Bulk-select checkboxes (header "select all current page" + per-row)
- "Mark X selected as paid" button → modal confirmation (show count + total amount)
- Sau confirm → call `POST /invoices/batch-mark-paid`
- Real-time UI update + toast feedback success/partial-fail
- Filter sidebar: by status (PENDING / PAID / OVERDUE), by due_date range, by class

### Component 3: CSV import bank statement (Backend + Frontend)

- FE: Drag-drop upload widget accepting `.csv` từ Vietcombank / Techcombank / MB / VPBank / ACB / TPBank common export formats
- BE: Parse CSV → extract rows `{date, amount, memo, sender_name_redacted}`
- Fuzzy-match algorithm:
  - Match by `amount` (exact OR ±1% tolerance per GAP-627)
  - Match by `transfer_memo` containing invoice number prefix HOẶC student name fuzzy (Levenshtein ≤2)
  - Confidence score 0-1.0 per row
- Response: list of matched candidates per CSV row với confidence + recommend "high confidence auto-mark" vs "low confidence manual review"
- UI: 2-column review table — left side CSV row, right side matched invoice candidate(s) — Owner click "approve match" hoặc "manual select"

### Component 4: Manual override per row

- Khi fuzzy-match unconfident (confidence < 0.7) OR multiple candidates → flag for manual review
- UI dropdown: "Select invoice this payment maps to" + free-text "memo note" cho audit trail
- Audit log entry mandatory ghi rõ "manual override per fuzzy-match low confidence; matcher_confidence_was=0.65; owner_decision=manual"

---

## Acceptance Criteria

- [ ] **AC-628.1:** Backend endpoint `POST /api/v1/invoices/batch-mark-paid` accept list ≤500 invoice IDs per request; success rate ≥99% trên 100-invoice batch
- [ ] **AC-628.2:** UI invoices table bulk-select hoạt động ≥30 invoices simultaneously (P2 size scope); FE round-trip ≤3s cho 30-invoice batch
- [ ] **AC-628.3:** CSV import support ≥6 VN bank statement formats (Vietcombank / Techcombank / MB / VPBank / ACB / TPBank) — at least 1 sample CSV per bank validated bằng E2E test
- [ ] **AC-628.4:** Fuzzy-match accuracy ≥80% high-confidence matches (confidence ≥0.7) trên test dataset 100 CSV rows × 100 invoices (P2 monthly scale)
- [ ] **AC-628.5:** Manual override flow tracked đầy đủ trong audit log (test: query `admin_audit_logs` WHERE action='manual_mark_paid_override' AND invoice_id=X returns row với matcher_confidence + owner_decision fields)
- [ ] **AC-628.6:** Idempotency-Key header enforce (test: replay same key 3 lần → exactly 1 mark-paid action committed)
- [ ] **AC-628.7:** Role-guard test: P2 Owner ✅ allowed, P3 Manager ❌ 403 forbidden (per GAP-632 escalation path), P5 PH ❌ 403 forbidden
- [ ] **AC-628.8:** Time savings measurement: P2 Owner test session 30-invoice batch via UI + 100-row CSV import ≤10 phút total (baseline manual 90 clicks ~30-45 phút)
- [ ] **AC-628.9:** Business doc 3-layer sync — `documents/01-business/kitehub/invoice/rules.md` + `use-cases.md` + `api-contract.md` updated với batch + CSV import use cases trong same PR (per CLAUDE.md Living Docs rule)

---

## Related

### Sibling P1 gaps (Wave 33-34 Phase 1.5b cluster, paired same audit)

- [GAP-629: QR refund workflow SOP](GAP-629-qr-refund-workflow-sop.md) — manual out-of-band transfer tracked
- [GAP-630: QR evidence receipt storage](GAP-630-qr-evidence-receipt-storage.md) — screenshot hash + metadata
- [GAP-631: QR account verification quarterly refresh](GAP-631-qr-account-verification-refresh.md) — lifecycle notification
- [GAP-632: Manual mark-paid audit trail + override](GAP-632-qr-manual-mark-paid-audit-trail.md) — idempotency + Manager approval flow

### P0 foundation (MUST close trước Phase 1.5 launch — Wave 31-32 Phase 1.5a)

- GAP-625: QR payment foundation — KYC + multi-tenant binding + immutable audit
- GAP-626: QR payment PDPL transaction PII handling + consent collection
- GAP-627: Payment-amount mismatch detection + UI alert workflow

### Re-scoped existing gaps

- GAP-108: Payment-Invoice Rules → re-scope cho QR display + reconcile metadata
- GAP-183: Refund + Dispute Resolution Policy → re-scope manual SOP (paired GAP-629)
- GAP-185: VAT/TCT Invoice engine → re-scope MISA MeInvoice partnership (paired GAP-634)

### Audit + planning references

- [Outside-in audit Phase 1.5 QR payment](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md) §2.1 persona walkthrough + §2.2 benchmark + §2.3 failure-mode + §5.1 P1 gaps section
- Wave plan: `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md`
- `documents/03-planning/roadmap/release-1-plan-2026.md` §4 Phase 1.5 PAID
- ROADMAP.md §🚀 Next Action — Wave 93+ queue

---

## Log

- **2026-05-18:** Gap filed. Triggered by 3-agent outside-in audit của Phase 1.5 QR payment proposal — P2 Center Owner persona walkthrough verdict 🔴 BLOCKER without batch reconcile + benchmark verdict industry-norm hybrid manual+CSV + failure-mode P1 reconciliation scale. Scope: 4 components (batch API + bulk-select UI + CSV import fuzzy-match + manual override). Wave 33-34 Phase 1.5b target window; P0 foundation (GAP-625/626/627) MUST close trước. Author: @nguyenvankiet (solo-dev). Verified Phase 1.5 PAID scope = greenfield via state-check §Current State (4 grep/find commands, 0 hits per `audit-to-gap-pipeline.md` §2.5 mandate).
