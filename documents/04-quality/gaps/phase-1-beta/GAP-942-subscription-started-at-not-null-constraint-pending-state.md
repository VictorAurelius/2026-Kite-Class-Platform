# GAP-942: Subscription `started_at` + `expires_at` NOT NULL constraint blocks PENDING state (PR #2151 SUB-20 contract drift)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 G1 walk — first POST /api/platform/subscriptions BASIC)
**Affects:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/SubscriptionService.java` createSubscription path + V2 subscriptions schema

## Problem

PR #2151 SUB-20 fix (merged main `1ce04fc0` 2026-06-04) introduces PENDING subscription state:
- `subscription.setStatus(SubscriptionStatus.PENDING)`
- `// startedAt + expiresAt remain null until activation; computed in applyPendingUpgrade.`

Pre-existing schema V2 (`V2__create_subscriptions_table.sql`) defines:
- `started_at TIMESTAMP NOT NULL`
- `expires_at TIMESTAMP NOT NULL`

→ INSERT fails với `SQLState 23502 — null value in column "started_at" violates not-null constraint`. `GlobalExceptionHandler` maps DataIntegrityViolationException → HTTP 409 `RESOURCE_CONFLICT` (misleading error code; actual cause = schema drift).

G1 walk evidence (POST /api/platform/subscriptions BASIC từ Owner FREE/TRIAL):
```
[HTTP 409]
{"type":"about:blank","title":"Conflict","status":409,"detail":"Tài nguyên đã tồn tại hoặc xung đột với tài nguyên khác. Vui lòng thử lại với giá trị khác.","errorCode":"RESOURCE_CONFLICT"}
```

Container log:
```
ERROR: null value in column "started_at" of relation "subscriptions" violates not-null constraint
Detail: Failing row contains (..., FREE, MONTHLY, 500000, PENDING, null, null, t, ..., null, null, f, BASIC, null, 0)
```

## Root Cause

Contract drift: BE code change shipped without paired DB migration. PR #2151 added PENDING state semantics (`started_at=null, expires_at=null until applyPendingUpgrade`) but did NOT relax NOT NULL constraint on those columns.

Per `audit-to-gap-pipeline.md` §2.6 wave-plan state-check + Entity ↔ Migration ↔ Mapper triad drift CI check: triad check passes because Entity field type không changed (still LocalDateTime); only nullability semantic shifted. Triad drift CI detector heuristic blind to nullability-only contract changes.

## Proposed Fix

V62 migration drop NOT NULL on `started_at` + `expires_at`:

```sql
ALTER TABLE subscriptions
  ALTER COLUMN started_at DROP NOT NULL,
  ALTER COLUMN expires_at DROP NOT NULL;
```

Activation path (`applyPendingUpgrade`) still sets both to non-null when flipping PENDING → ACTIVE — no read-side code change needed (Java field LocalDateTime nullable; existing nullsafe checks adequate).

## Acceptance Criteria

- [ ] V62 migration created + applied on local stack
- [ ] POST /api/platform/subscriptions với BASIC trên Owner FREE/TRIAL trả HTTP 201 (không 409)
- [ ] Response shape: `status=PENDING, tier=FREE, pendingTier=BASIC, pendingPaymentId=<uuid>, startedAt=null, expiresAt=null`
- [ ] Payment row created với `bank_code='VCB', account_number='1234567890', account_name='CONG TY KITECLASS'` (GAP-939 fix verified inline)
- [ ] Instance KHÔNG flip ACTIVE (status stays TRIAL until admin confirm)
- [ ] PaymentService.confirmPayment → applyPendingUpgrade sets startedAt + expiresAt + flip ACTIVE
- [ ] Existing UpgradeSubscription test pass với new nullable columns

## Related

- Triggered by: Wave flow-kh3 G1 walk 2026-06-04 (first walk step)
- Caused by: PR #2151 SUB-20 fix (merged main 1ce04fc0)
- Sister concerns: GAP-939 Payment account_number snapshot (already fixed PR #2153) — would chain-test in same re-walk
- Rule cite: `audit-to-gap-pipeline.md` §2.6 wave-plan state-check + Entity ↔ Migration triad drift
- Rule cite: `release-fix-retry-budget.md` §3.5 investigation phase mandate (apply trong fix PR body)
- Meta gap candidate: triad drift CI detector blind to nullability-only contract changes — file follow-up if recurrence ≥1
