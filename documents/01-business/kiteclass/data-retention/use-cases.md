# Data Retention — Use Cases

**Domain:** data-retention
**Source:** GAP-073, Wave 4 Sub-PR 4.4, ADR-013

## UC-RET-001 — User requests account deletion (GDPR Art. 17)

**Actor:** Authenticated tenant user (owner of their own account)

**Preconditions:**
- User is logged in
- No non-terminal `DeletionRequest` already exists for the user (BR-RET-004)

**Steps:**
1. User clicks "Delete my account" in Settings → DangerZone (KiteHub FE — wiring deferred)
2. FE shows confirmation dialog explaining 7-day grace period + data export option
3. User confirms
4. Backend calls `DeletionService.requestDeletion(userId, tenantId)`
5. Row created with `status=PENDING`, `graceEndsAt = now + 7 days`
6. `AuditLog` row `deletion.requested` written in same txn (BR-RET-003)
7. FE shows success with grace-period countdown + "Cancel deletion" CTA

**Errors:**
- `IllegalStateException` if a non-terminal request already exists — FE shows existing request status
- Any DB error — rollback ensures no partial state

**FE behavior:**
- Show grace countdown ("Your account will be deleted in 6 days 23 hours")
- Prominent "Cancel deletion" button while PENDING / GRACE_PERIOD
- Disable the delete button for users who already have a request

---

## UC-RET-002 — User cancels deletion during grace

**Actor:** Same user who initiated the deletion

**Preconditions:**
- A `DeletionRequest` in `PENDING` or `GRACE_PERIOD` exists for the user

**Steps:**
1. User clicks "Cancel deletion" in DangerZone
2. FE prompts for optional cancellation reason
3. Backend calls `DeletionService.cancelDeletion(deletionId, reason)`
4. Row transitions to `CANCELLED`, `cancelledAt` stamped
5. `AuditLog` row `deletion.cancelled` written in same txn
6. FE returns user to normal settings view

**Errors:**
- `IllegalStateException` if request is already terminal (`PROCESSING` / `COMPLETED` / `CANCELLED`) — grace window has passed

---

## UC-RET-003 — Grace period expires (scheduler promotes to PROCESSING)

**Actor:** System scheduler (Spring `@Scheduled` job — wiring deferred to 4.5/4.6)

**Preconditions:**
- One or more `PENDING` rows with `graceEndsAt <= now`

**Steps:**
1. Scheduler invokes `DeletionService.expirePastGrace()` periodically (e.g. hourly)
2. For each due row: transition `PENDING → PROCESSING`, stamp `processingStartedAt`
3. `AuditLog` row `deletion.processing_started` (reason: "grace period expired") per row
4. Method returns count for metrics

**Errors:**
- Rows whose state machine rejects the transition are skipped and logged (should not happen under normal operation)

---

## UC-RET-004 — Admin marks deletion completed

**Actor:** Deletion pipeline worker (future Sub-PR) OR admin tool

**Preconditions:**
- `DeletionRequest` in `PROCESSING`
- All purge / pseudonymize actions have been applied

**Steps:**
1. Pipeline calls `DeletionService.markCompleted(deletionId)`
2. Row transitions to `COMPLETED`, `completedAt` stamped
3. `AuditLog` row `deletion.completed` written in same txn
4. (Future) tombstone record created to block future signup with same email

---

## UC-RET-005 — User exports their data (GDPR Art. 20)

**Actor:** Authenticated tenant user

**Preconditions:**
- User is logged in

**Steps:**
1. User clicks "Download my data" in Settings (before or after deletion request)
2. Backend calls `DataExportService.exportForUser(userId, tenantId)`
3. Service assembles ZIP with `profile.json`, `audit-trail.csv`, `README.txt`
4. `AuditLog` row `deletion.export_generated` written
5. ZIP bytes streamed back as download (future: upload to MinIO + signed URL)

**Errors:**
- `UncheckedIOException` wrapping `IOException` on ZIP assembly failure — caller returns 500

---

## UC-RET-006 — Developer classifies a new entity

**Actor:** Developer adding a new entity

**Steps:**
1. Determine retention bucket per ADR-013 classification matrix
2. Add class-level `@Retention(RetentionBucket.XXX)` annotation (import from
   `com.kiteclass.core.module.retention`)
3. For `RETAIN_WITH_PSEUDO`, also specify `pseudonymizeFields = {"email", "phone"}`
4. Verify via `RetentionClassifier.classify(EntityClass.class)` in a unit test

**Gotchas:**
- Our `@Retention` shadows `java.lang.annotation.Retention`; if the entity declares other
  meta-annotations, use fully-qualified `@java.lang.annotation.Retention` to avoid collision.
- If no annotation is present, classifier returns `PURGE_ON_REQUEST` as safe default —
  do NOT rely on this for entities that must be retained.

## Log
- 2026-04-15 — Initial use-cases (Wave 4 Sub-PR 4.4)
