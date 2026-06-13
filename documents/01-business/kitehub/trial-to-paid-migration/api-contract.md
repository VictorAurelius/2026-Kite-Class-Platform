# Trial → Paid Migration — API Contract

All endpoints assume Bearer JWT (Owner or Admin role) unless noted. Error envelope follows project standard (`{error: {code, message, details}}`).

**Atomicity guarantees (BE-2 hardening — [ADR-042](../../../02-architecture/adr/ADR-042-trial-to-paid-migration-atomicity.md)):** HTTP contract shape KHÔNG đổi, nhưng internal migration đã hardened atomicity (wave kitehub-biz-100): (1) pessimistic write lock `findByIdForUpdate` (T2P-08, GAP-1253) serialize concurrent worker → zero double-convert; (2) `MigrationRetryRunner` là Spring `@Component` + self-reference qua `ObjectProvider` → `@Transactional` per-attempt thật-sự áp dụng (GAP-1254 — trước đây inert do self-invocation); (3) `markMigrationFailed` chạy `REQUIRES_NEW` → DLQ event + phase `MIGRATION_FAILED` survive khi attempt txn rollback (T2P-10); (4) tier requested carry qua flip → `instances.tier` sync (GAP-1095, SUB-21); (5) `idempotencyKey` persist trong cùng txn `initiateUpgrade` (catch `DataIntegrityViolation` concurrent-create race → cached replay), TTL `kitehub.trial-to-paid.idempotency.ttl-minutes: 10`.

## POST /api/platform/instances/{id}/upgrade
**Use case:** UC-T2P-01
**Auth:** Bearer token (Owner of instance)
**Request:**
```json
{
  "tier": "PRO",
  "billingCycle": "MONTHLY",
  "paymentMethodId": "pm_xxx",
  "idempotencyKey": "uuid-v4"
}
```
**Response 202 (Accepted, async):**
```json
{
  "instanceId": "uuid",
  "migrationPhase": "INITIATED",
  "startedAt": "2026-04-07T12:00:00Z",
  "estimatedCompletionSeconds": 5,
  "pollUrl": "/api/platform/instances/{id}/trial-status"
}
```
**Errors:**
- 400: invalid tier / billingCycle / paymentMethodId
- 402: payment method pre-validation failed
- 409: another migration already in flight (T2P-08) — `{error.code: "MIGRATION_IN_FLIGHT"}`
- 410: trial expired past rescue window (T2P-05)
- 423: instance in MIGRATION_FAILED (locked, requires manual resolution)

**Idempotency:** `idempotencyKey` persisted; duplicate request within 10 minutes returns original 202 response.

---

## GET /api/platform/instances/{id}/trial-status (extended)
**Use case:** UC-T2P-06 + UC-TR-05 (existing — extended with migration fields)
**Auth:** Bearer token (Owner)
**Response 200:**
```json
{
  "instanceId": "uuid",
  "subdomain": "thptabc",
  "status": "TRIAL",
  "isOnTrial": true,
  "trialStartedAt": "2026-03-24T00:00:00Z",
  "trialExpiresAt": "2026-04-07T00:00:00Z",
  "daysLeft": 3,
  "needsWarning": true,
  "warningLevel": "MEDIUM",

  "migrationPhase": "MIGRATING",
  "migrationStartedAt": "2026-04-07T11:59:58Z",
  "migrationCompletedAt": null,
  "migrationFailureReason": null
}
```

**Phase enum:** `NONE | INITIATED | PAYMENT_PENDING | PAYMENT_CAPTURED | MIGRATING | COMPLETED | REVERSED | MIGRATION_FAILED`

---

## POST /api/platform/webhooks/trial-migration
**Use case:** UC-T2P-02 (reversal), UC-T2P-01 step 6 (capture)
**Auth:** HMAC-SHA256 signature (gateway shared secret) sent via header `X-Signature: <hex>`; signed body = raw request bytes
**Note:** the path `/webhooks/payment` is already owned by the VietQR
`PaymentWebhookController` (pre-GAP-192); the migration-specific webhook therefore lives
at `/webhooks/trial-migration`. Both use HMAC-SHA256 but different body-signing schemes
(VietQR uses key=value& ordering; trial-migration uses raw body per modern gateway
convention — Stripe/Adyen style).
**Request (capture):**
```json
{
  "eventType": "payment.captured",
  "paymentIntentId": "pi_xxx",
  "metadata": { "instanceId": "uuid", "migrationPhase": "PAYMENT_PENDING" },
  "amount": 49900,
  "currency": "VND"
}
```
**Request (reversal):**
```json
{
  "eventType": "payment.reversed",
  "paymentIntentId": "pi_xxx",
  "metadata": { "instanceId": "uuid" },
  "reason": "chargeback"
}
```
**Response 200:**
```json
{ "ack": true, "migrationPhase": "PAYMENT_CAPTURED" }
```
**Errors:**
- 401: invalid HMAC signature
- 404: instance not found
- 409: event arrives for unexpected phase (e.g., capture for phase=NONE) — logged + 409

**Idempotency:** `paymentIntentId` tracked; duplicates ignored.

---

## POST /api/platform/admin/instances/{id}/force-convert
**Use case:** UC-T2P-05
**Auth:** Bearer token (Admin role)
**Request:**
```json
{
  "tier": "ENTERPRISE",
  "billingCycle": "ANNUAL",
  "invoiceRef": "INV-2026-0042",
  "reason": "Bank transfer verified — case #T-12345"
}
```
**Response 202:** same envelope as POST /upgrade
**Errors:**
- 403: not admin
- 409: migration already in flight

---

## POST /api/platform/admin/instances/{id}/rollback-migration
**Use case:** UC-T2P-02 (manual trigger, ops tool)
**Auth:** Bearer token (Admin role)
**Request:**
```json
{
  "reason": "Gateway reversal confirmed out-of-band",
  "referenceId": "TICKET-456"
}
```
**Response 200:**
```json
{
  "instanceId": "uuid",
  "migrationPhase": "REVERSED",
  "rolledBackAt": "2026-04-08T10:00:00Z",
  "newStatus": "TRIAL",
  "trialExpiresAt": "2026-04-14T00:00:00Z"
}
```
**Errors:**
- 410: outside reversal window (T2P-04 24h) — `{error.code: "REVERSAL_WINDOW_EXPIRED"}`
- 409: instance status not ACTIVE (cannot rollback a non-ACTIVE instance)

---

## Outbox Events (consumed, not exposed as HTTP)

Published to `kitehub.migration` topic. See `rules.md` §5 for full schema. Subscribers:
- BillingService — creates/cancels subscription rows
- KiteClass core — invalidates tenant cache on `instance.migrated` + `migration.rolled_back`
- BrandingService — consumes `branding.refresh.required`
- EmailService — sends upgrade-success + rollback-notice emails
- Alertmanager receiver (via DLQ topic `kitehub.migration.dlq`) — pages ops on `migration.failed`

---

## Error Codes Reference

| Code | HTTP | Meaning |
|------|------|---------|
| `MIGRATION_IN_FLIGHT` | 409 | Another migration currently running for this instance (T2P-08) |
| `REVERSAL_WINDOW_EXPIRED` | 410 | Rollback requested beyond 24h window (T2P-04) |
| `RESCUE_WINDOW_EXPIRED` | 410 | Upgrade attempted beyond 24h rescue window post trial expiry (T2P-05) |
| `MIGRATION_FAILED_LOCKED` | 423 | Instance in MIGRATION_FAILED; requires manual ops resolution |
| `PAYMENT_DECLINED` | 402 | Gateway rejected payment method pre-validation |
| `INVALID_PHASE_TRANSITION` | 409 | Webhook/admin action attempted against unexpected phase |
