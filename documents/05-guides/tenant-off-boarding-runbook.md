# Tenant Off-boarding Runbook

> Last updated: 2026-04-20 (GAP-201 Phase 1) | Owner: Customer Success / DevOps / Legal
> Complements: [`tenant-onboarding-checklist.md`](tenant-onboarding-checklist.md)

End-to-end procedure for churn — from "Cancel" click to final purge and tombstone — for every tenant tier. Target: zero data loss for the tenant, zero compliance violations for the platform.

**Governing docs:**
- Business rules: [`../01-business/kitehub/off-boarding/rules.md`](../01-business/kitehub/off-boarding/rules.md)
- Use cases: [`../01-business/kitehub/off-boarding/use-cases.md`](../01-business/kitehub/off-boarding/use-cases.md)
- API contract: [`../01-business/kitehub/off-boarding/api-contract.md`](../01-business/kitehub/off-boarding/api-contract.md)
- Retention classification: [`../02-architecture/adr/ADR-013-data-retention-classification.md`](../02-architecture/adr/ADR-013-data-retention-classification.md)

---

## 1. State Machine (at a glance)

```
PAID_ACTIVE (NONE)
    │ (user cancel)
    ▼
CANCEL_REQUESTED ──(bundle ready)──► EXPORT_READY
    │                                    │
    │ ◄──── (undo within 30d) ────       │
    │                                    ▼
    │                          CANCEL_GRACE_ACTIVE (read+write, 30d)
    │                                    │
    │                                    ▼ (day 30)
    │                          CANCEL_GRACE_READONLY (read-only, days 31-90)
    │                                    │
    │                                    ▼ (day 90)
    │                                ARCHIVED (subdomain released)
    │                                    │
    │                                    ▼
    │                                 PURGED (tombstone stored)
    │
    └─(RTBF)─► RTBF_FAST_TRACK ──► ARCHIVED ──► PURGED (≤48h)
```

Grace periods: **30 days active / 60 days read-only / final purge at 90 days** — rationale in §5.

---

## 2. User Flow — Self-Service Cancel (90% of churn)

### 2.1 Entry

Settings → Billing → **"Cancel subscription"** (requires 2FA).

### 2.2 Wizard (required by OFF-01)

1. **Reason selection** (dropdown — seeds analytics):
   - Switched provider
   - Cost
   - Unused / low adoption
   - Temporary pause
   - Other (free text)
2. **Export bundle opt-in** (checkbox; default ON) — GDPR Art. 20 portability
3. **Grace-period explanation** (one paragraph — 30d keep-alive, 60d read-only, purge day 90)
4. **Financial-retention disclosure** (OFF-08 — invoices retained 7y, pseudonymized at purge)
5. **Confirm** button → `POST /api/platform/instances/{id}/off-boarding/cancel`

### 2.3 Post-confirm UX

- Banner on all pages: "Cancellation scheduled. Read/write active until `{graceEndsAt}`. [Undo]."
- Email confirmation with undo link (signed token, 30d TTL)
- Email with bundle link when `offboarding.export.ready` fires (SLA ≤24h per OFF-05)

### 2.4 Grace windows visible to user

| Day | Phase | What the user sees | What they can do |
|:---:|-------|-------------------|------------------|
| 0 | CANCEL_REQUESTED → EXPORT_READY | Yellow banner + export link | Use normally, undo |
| 1-30 | CANCEL_GRACE_ACTIVE | Yellow banner with countdown | Use normally, undo |
| 31-90 | CANCEL_GRACE_READONLY | Red banner — read-only | Export data only (no new writes, AI off) |
| 91 | ARCHIVED | Cannot log in | Contact support to request data (only if legal hold) |
| 91+ | PURGED | N/A | N/A |

---

## 3. Staff Flow — Enterprise / Manual Cancel

### 3.1 Ticket intake
- [ ] Verify identity (video call + corporate email)
- [ ] Confirm contract end date + no outstanding balance
- [ ] Confirm no legal hold / active dispute
- [ ] Offer export bundle delivery via secure channel
- [ ] Agree on scheduledPurgeDate per Enterprise contract

### 3.2 Execute
```bash
# Admin API — requires admin role + audit trail
./kitehub/scripts/exec.sh kitehub-admin \
  curl -X POST http://localhost:8085/api/platform/admin/instances/{id}/off-boarding/cancel \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{
      "reason": "CONTRACT_END",
      "reasonDetail": "...",
      "ticketRef": "TICKET-12345",
      "scheduledPurgeDate": "2026-08-01T00:00:00Z"
    }'
```

### 3.3 Hand-off
- [ ] Bundle link delivered via encrypted email OR SFTP drop (Enterprise preference)
- [ ] Confirm receipt signature before bundle TTL (OFF-06 7d)
- [ ] Schedule 30-day follow-up to confirm no data recovery needed
- [ ] Record in CRM + audit log

### 3.4 Staff SLAs (OFF-12)
- **Self-service ticket response:** 1 business day
- **Enterprise cancel response:** 4 hours
- **Bundle re-request:** same business day

---

## 4. Right-to-be-Forgotten (RTBF) — GDPR Art. 17

### 4.1 When

- Tenant owner opts for RTBF fast-track (skip 90d grace)
- Individual data subject (staff, parent) exercises right via DPO
- Regulator-ordered erasure

### 4.2 Flow (UC-OFF-03)

1. Subject submits `POST /off-boarding/rtbf` with legal basis + email
2. System emails 6-digit token (15-min TTL, OFF-10)
3. Subject confirms: `POST /off-boarding/rtbf/confirm`
4. `off_boarding_phase=RTBF_FAST_TRACK` — bypasses grace
5. Export bundle built ≤24h; delivered
6. Purge executes ≤24h after bundle delivery (total ≤48h)
7. **Financial records pseudonymized** (OFF-08 override — tax law 7y retention)

### 4.3 Legal-hold interaction

Check `LegalComplianceService.hasActiveHold(instanceId)` before any RTBF transition. If hold active:
- Queue request; notify subject of delay with reason + estimated lift date
- Resume automatically when hold lifts

---

## 5. Grace Period Rationale

| Window | Duration | Why |
|--------|:--------:|-----|
| Active (read/write) | 30d | Matches OFF-07 undo window; most regret decisions happen in first 2 weeks |
| Read-only cold | 60d | Aligned with VN PDPL "reasonable period" + staff/parent data-export follow-up |
| Purge trigger | Day 90 | GDPR "without undue delay" interpretation; matches industry norm (Stripe 90d, GitHub 90d) |
| Backup retention post-purge | 30d | OFF-11 — allows disaster-recovery restore if purge was erroneous |

Rationale source: GAP-184 retention policy + ADR-013. Grace windows are config-driven (§6); adjust via `application.yml` not code.

---

## 6. Data Export Bundle Specification

See [`api-contract.md`](../01-business/kitehub/off-boarding/api-contract.md) §"Export Bundle Specification" for full layout.

**Summary:**
- **Format:** ZIP (deflate), signed MinIO presigned URL
- **TTL:** 7 days (OFF-06) — re-request rebuilds fresh bundle
- **Scope options:** `FULL`, `BRANDING_ONLY`, `FINANCIAL_ONLY`, `ACADEMIC_ONLY`
- **Integrity:** SHA256 per file + bundle-level checksum in `X-Bundle-Checksum` header
- **Content types:**
  - Tabular → XLSX + CSV mirror
  - Documents → PDF with stylesheet
  - Structured → JSON
  - Audit → JSONL
- **SLA:** ≤24h from request to `EXPORT_READY` (OFF-05)
- **Size cap:** none enforced; bundles >2GB use MinIO multipart download
- **Encryption at rest:** AES-256 (MinIO default); encryption in transit via HTTPS

---

## 7. Right-to-be-Forgotten API Endpoint Specification

See [`api-contract.md`](../01-business/kitehub/off-boarding/api-contract.md) §"POST /off-boarding/rtbf" + `/rtbf/confirm`.

**Security requirements (MANDATORY for production):**
- Token delivery via subject-verified email ONLY (no SMS; prevents SIM-swap attack)
- Token entropy ≥ 20 bits (6 decimal digits = 20 bits — acceptable for 15-min TTL)
- Rate limit: 3 confirm attempts per request, then lock 1h
- Audit record: IP + user-agent + legal basis + DPO reviewer
- Legal-hold precheck blocks even token issuance (403 instead of 202)

---

## 8. Retention Conflict Resolution

See [`rules.md`](../01-business/kitehub/off-boarding/rules.md) §6 "Retention Conflict Matrix".

**Key decisions:**
- **Invoices + payment logs:** retained 7 years (VN Tax Law — GAP-185); pseudonymized at purge (OFF-08)
- **Student grades + attendance:** retained 5 years (MOET Education Law); pseudonymized per ADR-013 bucket `RETAIN_WITH_PSEUDO`
- **Audit logs:** retained 7 years (Cybersecurity Law); subject references pseudonymized
- **Uploaded logos + AI-generated assets:** hard-deleted at PURGED (no legal retention basis)
- **Session + cache:** TTL'd, no action needed

RTBF does NOT override legal retention — only reassigns to `RETAIN_WITH_PSEUDO` bucket per ADR-013.

---

## 9. Metrics + Observability

### 9.1 Funnel dashboard (Grafana)

| Metric | Definition | Alert threshold |
|--------|-----------|-----------------|
| `offboarding.cancel.intent.rate` | Cancel clicks / active tenants (monthly) | >2% → growth review |
| `offboarding.cancel.confirm.rate` | Confirms / intents | <60% → UX friction investigation |
| `offboarding.export.sla.breach` | Exports >24h | >0 per day → SRE page |
| `offboarding.undo.rate` | Undos / confirms | >15% → messaging clarity review |
| `offboarding.purge.blocked.legal.count` | Held-purge queue depth | >10 → legal-team review |
| `offboarding.rtbf.e2e.p95` | Request → PURGED p95 (hours) | >48h → SLA breach |

### 9.2 Per-phase dwell-time

Track p50/p95 dwell per phase — flags stuck jobs. Example:
- `CANCEL_REQUESTED → EXPORT_READY` p95 should be ≤24h
- `ARCHIVED → PURGED` p95 should be ≤1h under normal load

### 9.3 Logging

Structured JSON logs per phase transition with: `instanceId`, `phase`, `actor (user/admin/system)`, `correlationId`. Retention 7y per OFF-16.

---

## 10. Dependencies + Phase 2 Roadmap

### Phase 1 (this runbook — design)
- [x] 3-layer business docs (rules / use-cases / api-contract)
- [x] Runbook (user-flow + staff-flow + RTBF + grace rationale + export spec + metrics)
- [x] State machine drafted

### Phase 2 (implementation — future gaps)
- [ ] **Controller + service:** `OffBoardingController`, `OffBoardingService`, `PurgeScheduler` — scope for a new wave
- [ ] **MinIO streaming export:** GAP-073 deferred item — ArchiveOutputStream streaming to MinIO
- [ ] **`@Scheduled` expiry job:** GAP-073 deferred item — triggers grace-active → read-only → archive → purge
- [ ] **Pseudonymization executor:** GAP-073 deferred item — HMAC-based consistent pseudonymization
- [ ] **Migration:** new table `offboarding_request` + `off_boarding_phase` column on `instance`
- [ ] **Contract tests:** add to `kitehub-subscription/src/test/java/.../contract/OffBoardingApiContractTest.java`
- [ ] **DomainRegistryService:** subdomain quarantine 180d per OFF-13
- [ ] **Email templates:** cancel confirm, bundle ready, undo reminder, RTBF token, archive notice, purge receipt

### Retention policy (external dependency)
- GAP-184 ROADMAP approval of retention periods (OFF-08 financial-retention-years authoritative value)
- GAP-185 legal sign-off on VN tax 7y vs 10y (invoice retention)
- GAP-174 legal review hook for RTBF endpoint security

---

## 11. Common Issues + Fixes

### 11.1 Export bundle not delivered within SLA
Check MinIO disk + DataExportService logs. Re-queue:
```bash
./kitehub/scripts/exec.sh kitehub-admin \
  curl -X POST http://localhost:8085/api/platform/exports/{jobId}/retry
```

### 11.2 User lost undo email
Staff can manually undo via admin API (UC-OFF-04 adjacent) if within OFF-07 window.

### 11.3 Purge blocked but no legal hold visible
Check `LegalComplianceService.holds` table directly. GAP-174 legal review hook may hold purge pending sign-off.

### 11.4 Subdomain squatting attempt during quarantine
OFF-13 enforces 180d — rejections logged in `domain_registry_audit`. Extend quarantine if repeated probes from same IP.

### 11.5 User asks "why can't you delete my invoices?"
OFF-08 + GAP-185. Financial records retained 7y under VN tax law; pseudonymization removes PII while preserving audit row. Point user at Privacy page.

---

## 12. Related

- [`../00-brd/data-retention-deletion-policy.md`](../00-brd/data-retention-deletion-policy.md) (GAP-184, drafted)
- [`../04-quality/gaps/GAP-073-gdpr-deletion-ai-assets.md`](../04-quality/gaps/GAP-073-gdpr-deletion-ai-assets.md) (DONE — deferred items become Phase 2 sub-tasks)
- [`../04-quality/gaps/GAP-184-data-retention-deletion-policy.md`](../04-quality/gaps/GAP-184-data-retention-deletion-policy.md)
- [`../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md`](../04-quality/gaps/GAP-185-billing-terms-vat-tct-compliance.md)
- [`../04-quality/gaps/GAP-192-trial-to-paid-zero-downtime-migration.md`](../04-quality/gaps/GAP-192-trial-to-paid-zero-downtime-migration.md) (sibling lifecycle doc)
- [`../04-quality/gaps/GAP-201-tenant-off-boarding-runbook.md`](../04-quality/gaps/GAP-201-tenant-off-boarding-runbook.md)
- [`tenant-onboarding-checklist.md`](tenant-onboarding-checklist.md) — §8 placeholder "Offboarding (Tenant Churn)" now realized by this runbook

---

## 13. Log

- **2026-04-20:** Created (GAP-201 Phase 1). Phase 2 implementation deferred to a dedicated wave; all Phase 1 Acceptance Criteria met.
