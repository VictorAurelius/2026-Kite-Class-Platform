# Trial → Paid Migration — Use Cases

### UC-T2P-01: Successful Upgrade (Happy Path)
- **Actor:** Owner (instance on TRIAL, valid payment method)
- **Precondition:** Instance `status=TRIAL`, `migration_phase=NONE`, trial not yet expired (or within T2P-05 rescue window)
- **Steps:**
  1. User: click "Upgrade" → select plan → submit payment details
  2. FE: `POST /api/platform/instances/{id}/upgrade` with `{tier, paymentMethodId}`
  3. System: validate — status=TRIAL, no active migration in flight (T2P-08), payment method valid
  4. System: set `migration_phase=INITIATED`; emit `trial.upgrade.initiated` via outbox
  5. System: submit payment to gateway → `migration_phase=PAYMENT_PENDING`
  6. Gateway: confirms capture (webhook or sync response) → `migration_phase=PAYMENT_CAPTURED`; emit `payment.captured`
  7. Async worker: picks up PAYMENT_CAPTURED → `migration_phase=MIGRATING`
  8. Worker: verify DB reachable, verify no pending writes blocking, create subscription row, flip `status=ACTIVE` atomically with `migration_phase=COMPLETED`
  9. Worker: emit `instance.migrated` + `branding.refresh.required`
  10. FE: poll `GET /trial-status` → `{status: ACTIVE, migrationPhase: COMPLETED, completedAt}`; redirect to dashboard with welcome banner
- **Postcondition:** Instance ACTIVE, subscription created, AI budget refreshed per tier, branding templates tier-refreshed. **Zero user-visible downtime** (SLA T2P-02).
- **Errors:**
  - 409: already has active subscription → rejected
  - 402: payment declined → `migration_phase=REVERSED`; status stays TRIAL; emit `payment.reversed`
  - 503: migration worker unavailable → `migration_phase` stays PAYMENT_CAPTURED; retry picks up (T2P-09)
- **FE Behavior:** Modal spinner during steps 2-8 (typically <5s p95); fallback to polling if SSE unavailable

### UC-T2P-02: Payment Reversal Within Window (Rollback)
- **Actor:** System (gateway webhook) OR Admin
- **Precondition:** Instance `status=ACTIVE`, `migration_phase=COMPLETED`, completed within last 24h (T2P-04)
- **Steps:**
  1. Gateway: webhook `payment.reversed` arrives (or Admin triggers manual rollback)
  2. System: validate reversal window → within T2P-04? Yes → proceed; No → open dispute case (out-of-scope)
  3. System: lock instance (T2P-08); set `migration_phase=REVERSED`
  4. System: flip `status=ACTIVE → TRIAL` atomically; restore original `trialExpiresAt`
  5. System: cancel subscription row (soft-delete, retain for audit T2P-13)
  6. System: restore AI budget to trial level; emit `migration.rolled_back` + `branding.refresh.required`
  7. System: send rollback email to owner with reason
  8. Post-tick: reset `migration_phase=NONE`
- **Postcondition:** Instance back on TRIAL with original expiry; user retains data; AI budget trial-level. **Zero user-visible downtime**.
- **Errors:**
  - Outside reversal window (>24h): return 410 Gone; do not auto-rollback

### UC-T2P-03: Migration Worker Failure (Retry + DLQ)
- **Actor:** System (async worker)
- **Precondition:** Instance `migration_phase=MIGRATING`
- **Steps:**
  1. Worker: attempts migration step (DB commit, subscription row, status flip)
  2. On exception: increment retry counter; sleep per backoff (T2P-09: 1s, 3s, 9s)
  3. After 3 failed attempts: `migration_phase=MIGRATION_FAILED`; emit `migration.failed` to DLQ topic; alert ops
  4. Instance remains TRIAL (status unchanged); payment remains captured (no auto-refund — manual resolution)
- **Postcondition:** Ops alerted via Alertmanager; instance in intermediate state requiring manual intervention
- **FE Behavior:** `GET /trial-status` returns `migrationPhase=MIGRATION_FAILED`; UI displays support contact banner

### UC-T2P-04: Trial Expired + Rescue Window Upgrade
- **Actor:** Owner (instance on TRIAL, trial just expired but within T2P-05 rescue window)
- **Precondition:** `status=TRIAL`, `trialExpiresAt < now`, `trialExpiresAt > now - 24h` (T2P-05)
- **Steps:**
  1. User: visits post-expire upgrade page; clicks "Upgrade + Rescue"
  2. System: validate → within rescue window; proceed with UC-T2P-01 flow
  3. System: upon COMPLETED, do not suspend or archive — data remains fully accessible (no re-provisioning)
- **Postcondition:** Instance ACTIVE; trial gap handled without data loss
- **Errors:**
  - Outside rescue window (>24h past expiry): return 410 Gone; direct user to re-registration (but GAP-TR-07 re-trial prevention still applies)

### UC-T2P-05: Admin Force-Convert (Ops Tool)
- **Actor:** Admin (support ticket — tenant paid out-of-band)
- **Precondition:** Instance `status=TRIAL`, admin role
- **Steps:**
  1. Admin: `POST /api/platform/admin/instances/{id}/force-convert` with `{tier, invoiceRef, reason}`
  2. System: validate admin role + audit trail; skip payment gateway; set `migration_phase=PAYMENT_CAPTURED` with manual flag
  3. System: continue normal migration flow (UC-T2P-01 from step 7)
- **Postcondition:** Instance ACTIVE; audit log captures admin action + invoiceRef
- **FE Behavior:** Admin console shows success banner; owner gets welcome email

### UC-T2P-06: View Migration Status (Observability)
- **Actor:** Owner or Admin
- **Precondition:** Any status
- **Steps:**
  1. FE: `GET /api/platform/instances/{id}/trial-status` (existing endpoint, extended)
  2. System: returns existing trial fields + `migrationPhase`, `migrationStartedAt`, `migrationCompletedAt` (nullable)
- **FE Behavior:** Dashboard badge shows migration phase; spinner during MIGRATING; error banner for MIGRATION_FAILED
