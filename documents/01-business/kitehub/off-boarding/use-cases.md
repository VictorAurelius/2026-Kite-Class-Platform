# Tenant Off-boarding — Use Cases

### UC-OFF-01: Self-Service Cancellation (Happy Path)
- **Actor:** Owner (instance on `PAID_ACTIVE`, `off_boarding_phase=NONE`)
- **Precondition:** No billing dispute on file; user authenticated with 2FA
- **Steps:**
  1. User: Settings → Billing → "Cancel subscription"
  2. FE: Show cancellation reasons dropdown + export-bundle opt-in checkbox
  3. User: confirm → `POST /api/platform/instances/{id}/off-boarding/cancel` with `{reason, exportRequested: true}`
  4. System: validate status = `PAID_ACTIVE`, no `off_boarding_phase` in flight → set `off_boarding_phase=CANCEL_REQUESTED`
  5. System: enqueue data-export job (SLA OFF-05 24h); emit `offboarding.cancel.requested`
  6. System: send confirmation email with undo link (OFF-07 30-day window)
  7. Async: DataExportService builds ZIP bundle → uploads to MinIO → signs URL (TTL OFF-06 7d)
  8. System: `off_boarding_phase=EXPORT_READY`; emit `offboarding.export.ready`; email bundle link
  9. System: transitions to `CANCEL_GRACE_ACTIVE` (same tick as step 8 unless user requested no export)
  10. Day 30: transitions to `CANCEL_GRACE_READONLY`; `InstanceStatus=SUSPENDED`; AI features disabled (emit `offboarding.grace.readonly`)
  11. Day 90: transitions to `ARCHIVED`; `InstanceStatus=DELETED`; subdomain released with quarantine (OFF-13); backup snapshot taken (OFF-11); emit `offboarding.archived`
  12. Day 90 + purge window: `PurgeScheduler` runs → `PURGED`; emit `offboarding.purged`; tombstone record persisted (OFF-15)
- **Postcondition:** Instance fully off-boarded; user received export; invoices pseudonymized but retained (OFF-08)
- **Errors:**
  - 403: 2FA not enabled → require enrollment first
  - 409: already in off-boarding flow → return current phase + undo link
  - 422: active billing dispute → cannot cancel until resolved

### UC-OFF-02: Undo Cancellation Within Grace
- **Actor:** Owner
- **Precondition:** `off_boarding_phase ∈ {CANCEL_REQUESTED, EXPORT_READY, CANCEL_GRACE_ACTIVE}`
- **Steps:**
  1. User: click undo link from email OR `POST /api/platform/instances/{id}/off-boarding/undo`
  2. System: validate phase allows undo (OFF-07)
  3. System: `off_boarding_phase=NONE`; `InstanceStatus=PAID_ACTIVE`
  4. System: cancel any pending purge jobs; re-enable AI features; emit `offboarding.undo`
  5. System: notify user of reversal
- **Postcondition:** Instance fully active; billing continues per original cycle
- **Errors:**
  - 410: phase already `CANCEL_GRACE_READONLY` or beyond — undo window expired; user must re-signup
- **FE Behavior:** Settings banner shows "Cancellation scheduled — Undo" until `CANCEL_GRACE_READONLY`

### UC-OFF-03: Right-to-be-Forgotten (Fast-track)
- **Actor:** Data subject (tenant owner OR individual user under GDPR Art. 17)
- **Precondition:** Verified identity; legal basis documented
- **Steps:**
  1. User: Settings → Privacy → "Request data deletion" OR email DPO
  2. FE: `POST /api/platform/instances/{id}/off-boarding/rtbf` with `{legalBasis, subjectEmail}`
  3. System: send 6-digit confirmation token to subject email (OFF-10 15-min TTL)
  4. User: submit token → `POST /api/platform/instances/{id}/off-boarding/rtbf/confirm`
  5. System: validate token; set `off_boarding_phase=RTBF_FAST_TRACK`
  6. System: emit `offboarding.rtbf.requested`; LegalComplianceService creates audit record
  7. System: skip grace; export bundle built within 24h (OFF-05); notify subject
  8. System: transitions to `ARCHIVED` immediately after export delivery
  9. System: `PurgeScheduler` priority queue → `PURGED` same-day
  10. System: financial records pseudonymized per OFF-08 (not deleted — tax law override)
- **Postcondition:** PII purged; financial rows retained but de-identified; tombstone record for fraud prevention
- **Errors:**
  - 404: instance not found OR subject has no standing
  - 409: legal hold in place (DMCA, active litigation) — queue request post-hold
  - 423: confirmation token expired — restart flow
- **FE Behavior:** Privacy page explains Art. 17 + Art. 20 rights, retention conflicts (tax 7y), and expected timeline (24h export + 24h purge = ≤48h)

### UC-OFF-04: Staff-Managed Enterprise Cancellation
- **Actor:** Support staff (Admin role) — Enterprise tier manual off-boarding
- **Precondition:** Support ticket opened; Enterprise contract terms allow cancel
- **Steps:**
  1. Support: verify identity via video call + corporate email confirmation
  2. Support: `POST /api/platform/admin/instances/{id}/off-boarding/cancel` with `{reason, ticketRef, scheduledPurgeDate}`
  3. System: validate admin role + audit trail; set `off_boarding_phase=CANCEL_REQUESTED`
  4. System: continue normal UC-OFF-01 flow from step 5; `scheduledPurgeDate` may override OFF-04 per contract
  5. Support: delivers export bundle link via secure channel (OFF-05 SLA applies)
- **Postcondition:** Enterprise instance off-boarded with custom timeline; audit log captures admin + ticketRef
- **Errors:**
  - 403: not admin
  - 409: instance in billing dispute — resolve first
- **FE Behavior:** Admin console shows cancellation timeline + bundle delivery status

### UC-OFF-05: Export-Bundle-Only (No Cancellation)
- **Actor:** Owner (wants export for portability without cancel)
- **Precondition:** `off_boarding_phase=NONE`; `InstanceStatus=PAID_ACTIVE`
- **Steps:**
  1. User: Settings → Privacy → "Download my data" (GDPR Art. 20)
  2. FE: `POST /api/platform/instances/{id}/export` with `{scope: FULL | BRANDING_ONLY | FINANCIAL_ONLY}`
  3. System: validate status; enqueue export job (does NOT set off_boarding_phase)
  4. System: DataExportService builds bundle; signs URL (TTL OFF-06 7d)
  5. System: email bundle link; NO state transition
- **Postcondition:** User has export; instance remains ACTIVE; no churn signal
- **Errors:**
  - 429: rate limit — max 1 full export per 24h

### UC-OFF-06: Purge Blocked by Legal Hold
- **Actor:** System (scheduled purge)
- **Precondition:** `off_boarding_phase=ARCHIVED`; legal hold active (e.g. DMCA dispute)
- **Steps:**
  1. PurgeScheduler: picks up archived instance
  2. System: check LegalComplianceService.hasActiveHold(instanceId) → true
  3. System: skip purge; log `PURGE_BLOCKED_LEGAL_HOLD`; requeue for retry after hold lift
  4. System: hold lifted → scheduler resumes; continues to `PURGED`
- **Postcondition:** Data retained as long as hold active; deletion honored immediately after
- **FE Behavior:** Admin audit view shows pending-purge queue with hold reasons

### UC-OFF-07: View Off-boarding Status (Observability)
- **Actor:** Owner or Admin
- **Precondition:** Any state
- **Steps:**
  1. FE: `GET /api/platform/instances/{id}/off-boarding/status`
  2. System: returns `{phase, requestedAt, graceEndsAt, archiveScheduledAt, purgeScheduledAt, undoAvailable, bundleUrl, bundleExpiresAt}`
- **FE Behavior:** Billing page banner reflects phase; countdown timer to grace-readonly transition
