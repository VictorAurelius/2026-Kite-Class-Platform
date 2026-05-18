# GAP-630: QR evidence receipt storage — screenshot hash + metadata for dispute resolution

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Backend + Storage infra + Compliance)
**Detected:** 2026-05-18
**Affects:** P1 Solo Teacher + P2 Center Owner (universal dispute path); kitehub MinIO/S3 storage layer; kiteclass-core invoice + refund evidence domain; PDPL Art 11 compliance scope

---

## Current State (verified 2026-05-18)

Phase 1.5 PAID scope = greenfield. Evidence storage infrastructure chưa tồn tại tại thời điểm audit:

- `grep -rl "evidence\|receipt.*hash\|screenshot.*hash" kiteclass/ kitehub/ --include="*.java" --include="*.ts" --include="*.tsx" 2>/dev/null` → 0 hit (no evidence domain code)
- `find kiteclass kitehub -path '*/evidence*' -o -path '*/receipt*' 2>/dev/null` → 0 hit
- MinIO bucket scan trong `infrastructure/` configs `grep -l "evidence\|receipt" infrastructure/ 2>/dev/null` → 0 hit (no bucket provisioned)
- Migration scan `find */src/main/resources/db/migration -name "*evidence*" -o -name "*receipt*" 2>/dev/null` → 0 hit

Existing MinIO infrastructure foundation exists (per `infrastructure/` configs cho kitehub-branding asset storage); MinIO bucket pattern reusable, but new `payment-evidence` bucket cần provisioned.

Verdict: build-from-scratch. AC framing = greenfield evidence storage infrastructure + hash compute + metadata schema + retention policy + export API.

---

## Problem

Dispute scenario 6 tháng sau thanh toán surfaces:

- **PH claim**: "Tôi đã chuyển 1.500.000đ ngày 15/03, không thể trung tâm bảo không nhận"
- **Owner claim**: "Tôi không thấy tiền vào account; không có receipt"
- **KiteHub state**: Owner đã mark invoice = PAID 15/03 trong system, nhưng không có evidence backing

**Vì KiteHub stay non-PSP** (không broker tiền per audit §4.3), không có `gateway_txn_id` để cross-reference với bank ledger. Industry-standard solution: **require Owner upload VietQR receipt screenshot tại mark-paid time** + store hash + metadata cho dispute window.

Mục đích evidence storage:
1. **Dispute resolution** — 6-tháng window PH có thể dispute → evidence retained
2. **Audit log immutability** — file content hash khoá entry không bị tamper (Owner không thể edit screenshot post-facto)
3. **Legal compliance** — PDPL Art 11 + VN Civil Code dispute evidence requirements (7-year retention cho tax audit)
4. **Tenant offboarding evidence** — khi tenant rời KiteHub, evidence retained for legal window

3 outside-in agents converge xác nhận pain point:

- **Failure-mode matrix agent**: P1 failure scenario "Refund evidence dispute window 6 tháng" — no evidence trail → Owner stuck với he-said-she-said
- **Persona walkthrough agent**: P2 chị Hằng edge case #2 — "PH chuyển sai số tiền, sau 2 tháng đòi refund nhưng claim đã chuyển đúng" → cần evidence cross-reference
- **External benchmark agent**: industry-norm MISA EMIS + DotB EMS đều có receipt upload mandatory cho non-broker payment workflow

---

## Root Cause

Phase 1.5 non-broker model (mandatory per audit §4.3 PSP license barrier) → không có gateway-side audit trail từ payment processor. KiteHub PHẢI act as **evidence custodian** trong dispute window, tương tự cách rental platforms (Airbnb) store guest-side payment receipts cho host-side dispute.

Architecture decision:
- **Money flow**: out-of-band (PH banking app → Owner banking app)
- **Evidence flow**: in-band (PH/Owner upload screenshot → KiteHub MinIO + hash + metadata)

Hash-based evidence (SHA-256) prevents tampering: any pixel modification → hash mismatch → audit flag. Metadata stored separately enables retention policy + redacted export cho legal evidence (raw screenshot stays encrypted, only hash exported).

---

## Proposed Fix

### Component 1: MinIO bucket provisioning (Infrastructure)

- New bucket `payment-evidence-{env}` (e.g., `payment-evidence-production`, `payment-evidence-staging`)
- Bucket policy: server-side encryption SSE-S3 (or KMS-encrypted at AWS S3 if migrate per ADR future); versioning enabled; public access BLOCKED
- Bucket lifecycle policy: retention 7 years (per VN tax audit Decree 174/2016/NĐ-CP); transition to Glacier-equivalent cold storage after 1 year
- IAM policy: only `kitehub-evidence-service` role can PUT/GET (no public URL access; signed URLs only)
- Terraform module `infrastructure/terraform-aws/modules/payment-evidence-bucket/` per `release-deploy-standard.md` §2 standards

### Component 2: Evidence schema (Backend)

Migration `V{N}__create_payment_evidence_table.sql`:

```sql
CREATE TABLE payment_evidence (
  evidence_id BIGSERIAL PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
  evidence_type VARCHAR(30) NOT NULL, -- 'INVOICE_PAYMENT' | 'REFUND_TRANSFER' | 'DISPUTE_PROOF'
  related_invoice_id BIGINT REFERENCES invoices(invoice_id),
  related_refund_id BIGINT REFERENCES refunds(refund_id), -- paired GAP-629
  storage_object_key VARCHAR(500) NOT NULL UNIQUE, -- MinIO key path
  storage_bucket VARCHAR(100) NOT NULL,
  file_hash_sha256 CHAR(64) NOT NULL,
  file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0 AND file_size_bytes <= 10485760), -- max 10MB
  file_mime_type VARCHAR(50) NOT NULL CHECK (file_mime_type IN ('image/png','image/jpeg','image/webp','application/pdf')),
  uploaded_by UUID NOT NULL REFERENCES users(user_id),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  received_at_from_screenshot TIMESTAMPTZ, -- parsed from screenshot OCR future-scope
  sender_name_redacted VARCHAR(200), -- "N*** V*** A***" PDPL redacted
  sender_account_last4 VARCHAR(4), -- last 4 digits only per PDPL
  amount_from_screenshot NUMERIC(12,2),
  ocr_extracted_data JSONB, -- future-scope OCR fields
  retention_until DATE NOT NULL, -- uploaded_at + 7 years
  legal_hold BOOLEAN NOT NULL DEFAULT FALSE, -- override auto-delete for active dispute/litigation
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evidence_tenant ON payment_evidence(tenant_id, evidence_type);
CREATE INDEX idx_evidence_invoice ON payment_evidence(related_invoice_id);
CREATE INDEX idx_evidence_refund ON payment_evidence(related_refund_id);
CREATE INDEX idx_evidence_retention ON payment_evidence(retention_until) WHERE legal_hold = FALSE;
```

PDPL Art 11 compliance: `sender_name_redacted` + `sender_account_last4` only — raw screenshot held trong MinIO encrypted; full PII chỉ accessible via signed URL với role-guard (Platform Admin + tenant Owner only).

### Component 3: Upload service (Backend)

- Endpoint `POST /api/v1/evidence/upload` (kiteclass-core evidence domain)
- Multipart upload accepting image/PNG, image/JPEG, image/WebP, application/PDF up to 10MB
- Pipeline:
  1. Validate MIME type + size (per AC §2.5 file-upload flow checklist `pre-handoff-self-test-completeness.md` §2.5)
  2. Compute SHA-256 hash streaming (no temp file)
  3. Generate storage_object_key: `{tenant_id}/{evidence_type}/{YYYY}/{MM}/{evidence_id}-{hash_prefix}.{ext}`
  4. PUT to MinIO bucket với SSE
  5. Insert row vào `payment_evidence` table với hash + metadata
  6. Audit log append `evidence_uploaded` per GAP-625 immutable log
  7. ClamAV scan (or document exemption per `pre-handoff-self-test-completeness.md` §2.5 (c)) — defer ClamAV wiring per premature-rule guard nếu scope inflation; document risk acceptance trong PR
- Response: `{evidence_id, file_hash_sha256, signed_url_for_view, retention_until}`

### Component 4: Retrieval + hash verification (Backend)

- Endpoint `GET /api/v1/evidence/{evidence_id}` — returns metadata + signed_url (15-min TTL)
- Endpoint `GET /api/v1/evidence/{evidence_id}/verify-hash` — recompute hash từ MinIO object + compare với stored hash → return match/mismatch verdict
- Role-guard: Platform Admin + tenant Owner of evidence's tenant_id only
- Audit log every access `evidence_viewed` + `evidence_hash_verified`

### Component 5: Export hash-only API (Backend, legal evidence path)

- Endpoint `GET /api/v1/evidence/{evidence_id}/legal-export`
- Returns: evidence metadata + hash + retention info — **WITHOUT raw screenshot**
- Use case: legal counsel cần evidence existence proof cho dispute without exposing PII screenshot
- Signed legal export document (PDF với KiteHub digital signature) shows: evidence_id, hash, upload timestamp, retention_until, legal_hold status
- Role-guard: Platform Admin only

### Component 6: Retention enforcement job (Backend cron)

- Scheduled job `evidence-retention-cleanup` runs daily
- Query: `SELECT evidence_id FROM payment_evidence WHERE retention_until < CURRENT_DATE AND legal_hold = FALSE`
- For each row: DELETE MinIO object + UPDATE row set `storage_object_key = NULL` (preserve metadata row for audit trail) + audit log `evidence_retention_deleted`
- Legal hold flag: Platform Admin can SET via admin UI when dispute escalates → blocks retention cleanup

---

## Acceptance Criteria

- [ ] **AC-630.1:** MinIO bucket `payment-evidence-{env}` provisioned via Terraform; bucket policy verified non-public + encryption enabled
- [ ] **AC-630.2:** Migration `V{N}__create_payment_evidence_table.sql` apply success trên staging RDS
- [ ] **AC-630.3:** Upload endpoint accept ≥5 valid file formats (PNG / JPEG / WebP / PDF); reject invalid MIME (e.g., `text/html` with `.png` ext returns 415)
- [ ] **AC-630.4:** Size limit 10MB enforced (server returns 413 when exceeded; limit cited in `api-contract.md`)
- [ ] **AC-630.5:** SHA-256 hash compute deterministic — test: upload same file twice → same hash returned
- [ ] **AC-630.6:** Hash verification API detects tampering — test: directly modify MinIO object → `/verify-hash` returns mismatch
- [ ] **AC-630.7:** Signed URL TTL enforced — test: signed_url returned, wait 16 phút, retry → 403 expired
- [ ] **AC-630.8:** PDPL redaction verified — `sender_name_redacted` shows "N*** V*** A***" pattern; `sender_account_last4` only 4 digits stored (paired GAP-626 consent)
- [ ] **AC-630.9:** Retention enforcement job runs daily without error; test scenario: insert evidence row with `retention_until = yesterday` + `legal_hold = false` → next job run → MinIO object deleted, metadata row preserved
- [ ] **AC-630.10:** Legal hold override works — set `legal_hold = true` → retention job skips deletion (test sau 7-year boundary)
- [ ] **AC-630.11:** Audit log integration — every upload + view + verify + retention-delete event logs to `admin_audit_logs` per GAP-625 immutable log mandate
- [ ] **AC-630.12:** Role-guard tests — Platform Admin ✅ all endpoints; tenant Owner ✅ own tenant's evidence only; PH ❌ 403 forbidden; cross-tenant Owner ❌ 403
- [ ] **AC-630.13:** Business doc 3-layer sync — `documents/01-business/kitehub/evidence/rules.md` + `use-cases.md` + `api-contract.md` updated trong same PR

---

## Related

### Sibling P1 gaps (Wave 33-34 Phase 1.5b cluster, paired same audit)

- [GAP-628: QR batch reconcile API](GAP-628-qr-batch-reconcile-api.md)
- [GAP-629: QR refund workflow SOP](GAP-629-qr-refund-workflow-sop.md) — direct consumer của evidence storage (refund evidence screenshot)
- [GAP-631: QR account verification quarterly refresh](GAP-631-qr-account-verification-refresh.md)
- [GAP-632: Manual mark-paid audit trail + override](GAP-632-qr-manual-mark-paid-audit-trail.md) — direct consumer (mark-paid evidence screenshot mandatory)

### P0 foundation (MUST close trước Phase 1.5 launch — Wave 31-32 Phase 1.5a)

- GAP-625: QR payment foundation — KYC + multi-tenant binding + immutable audit (direct dependency cho audit log integration)
- GAP-626: QR payment PDPL transaction PII handling + consent collection (direct dependency cho PII redaction + retention policy)
- GAP-627: Payment-amount mismatch detection (consumer của amount_from_screenshot OCR field)

### Compliance + standards references

- `.claude/rules/pre-handoff-self-test-completeness.md` §2.5 file-upload flow checklist (7 items: MIME validation / size limit / virus scan / storage location / retrieval URL / failed upload UI / audit log)
- PDPL 2023 + Decree 13/2023/NĐ-CP Art 11 — financial transaction PII redaction
- VN Civil Code Art 429 + Decree 174/2016/NĐ-CP — dispute evidence retention 7 years cho tax audit
- `documents/05-guides/operations/secrets-rotation-runbook.md` — pattern cho infrastructure secret management

### Audit + planning references

- [Outside-in audit Phase 1.5 QR payment](../audits/persona-review/2026-05-18-phase-1-5-qr-payment-outside-in.md) §2.3 failure-mode dispute window + §2.1 P2 edge case wrong-amount + §5.1 P1 gaps section
- Wave plan: `documents/03-planning/waves/wave-2026-05-18-93-phase-1-5-qr-payment-audit.md`
- `documents/03-planning/roadmap/release-1-plan-2026.md` §4 Phase 1.5 PAID
- ROADMAP.md §🚀 Next Action — Wave 93+ queue

---

## Log

- **2026-05-18:** Gap filed. Triggered by 3-agent outside-in audit của Phase 1.5 QR payment proposal — failure-mode agent dispute window scenario + persona walkthrough P2 wrong-amount edge case + benchmark agent industry-norm receipt upload mandatory cho non-broker payment workflow. Scope: 6 components (MinIO bucket provisioning + DB schema + upload service + retrieval/hash verify + legal export + retention cron). Direct dependency cho GAP-629 refund evidence + GAP-632 mark-paid evidence. PDPL Art 11 compliance scope + VN 7-year retention. Wave 33-34 Phase 1.5b target window; P0 foundation (GAP-625/626) MUST close concurrent precedent. Author: @nguyenvankiet (solo-dev). Verified greenfield via state-check §Current State (4 grep/find commands, 0 hits per `audit-to-gap-pipeline.md` §2.5 mandate).
