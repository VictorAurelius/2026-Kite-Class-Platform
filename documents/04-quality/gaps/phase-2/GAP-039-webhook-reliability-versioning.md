# GAP-039: Webhook Reliability (Retry, Idempotency, Event Versioning)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Integration / Backend
**Detected:** 2026-04-14 (simulation: Developer × Integration × C8)

## Problem

Webhook system cho `branding.updated` event (GAP-010, GAP-021) hiện **không có reliability guarantees**:

- ❌ Không retry nếu consumer down
- ❌ Không idempotency (duplicate events nếu retry)
- ❌ Không dead letter queue
- ❌ Không event versioning (schema evolution)
- ❌ Không signature verification (security)
- ❌ Không delivery tracking (audit)

## Proposed Fix

### 1. Retry Policy

```yaml
webhook-retry:
  max-attempts: 5
  backoff: exponential
  initial-delay: 1s
  max-delay: 15min
  jitter: true
  # Attempts: 1s, 2s, 4s, 8s, 16min (cumulative ~17 min before DLQ)
```

### 2. Idempotency

Each event has unique ID + timestamp:
```json
{
  "eventId": "evt_01HXYZ...",
  "eventType": "branding.updated",
  "version": "1.0",
  "timestamp": "2026-04-14T10:00:00Z",
  "tenantId": "...",
  "data": { ... },
  "signature": "sha256=..."
}
```

Consumer tracks processed eventIds → skip duplicates.

### 3. Signature Verification

HMAC-SHA256 với shared secret:
```
X-KiteClass-Signature: sha256=abc123...
X-KiteClass-Timestamp: 1713091200
```

Consumer verify:
```typescript
const expected = hmac('sha256', secret, timestamp + body);
if (!timingSafeEqual(expected, signature)) reject();
if (Date.now() - timestamp > 5_MINUTES) reject();  // replay protection
```

### 4. Event Schema Versioning

```json
{
  "eventType": "branding.updated",
  "version": "1.0"  // semantic versioning
}
```

- v1.0 → v1.1: add fields (backward compat)
- v1.1 → v2.0: breaking change (send both v1 + v2 during deprecation)
- 90-day deprecation notice

### 5. Dead Letter Queue

Sau 5 retry fails → move to DLQ:
- Admin alert
- Manual review/retry option
- Root cause analysis

### 6. Delivery Tracking

```java
@Entity
public class WebhookDelivery {
  String deliveryId;
  String eventId;
  String consumerUrl;
  Integer attempt;
  Integer statusCode;
  String response;
  Boolean success;
  Timestamp attemptedAt;
}
```

Admin dashboard show delivery stats per tenant.

### 7. Rate Limiting

Prevent webhook storm:
- Max 100 webhooks/minute per consumer
- If consumer slow → batching?

## Acceptance Criteria

- [ ] Retry with exponential backoff (5 attempts)
- [ ] Unique eventId + idempotency
- [ ] HMAC-SHA256 signature
- [ ] Event schema versioning
- [ ] DLQ for permanent failures
- [ ] Delivery tracking table + dashboard
- [ ] Rate limiting per consumer
- [ ] Integration test: consumer down 5 min → events delivered after recovery
- [ ] Developer docs explain signature verification

## Dependencies

- RabbitMQ (retry mechanism)
- GAP-019 (monitoring) — alert on high failure rate

## Log

- 2026-04-14 — Webhook reliability gap
