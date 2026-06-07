# GAP-942: Subscription `started_at` + `expires_at` NOT NULL constraint blocks PENDING state (PR #2151 SUB-20 contract drift)

**Status:** 🟢 DONE (100% — V62 verified live, POST /subscriptions BASIC walk LIVE PASS, regression IT green)
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

## Resolution status (Wave p0-2, 2026-06-07)

Fix-time state-check (`audit-to-gap-pipeline.md` §2.8 + §2.6.1 Bucket-Completion Check): V62 migration (`V62__subscription_nullable_started_at_expires_at.sql`) + SubscriptionService PENDING semantics were **already shipped** (PR #2157) — the gap's proposed fix was code-complete before this wave. **AC#1 verified live**: `started_at` + `expires_at` both `is_nullable=YES` on `kitehub` DB + `flyway_schema_history` V62 `success=t` (rebuilt kitehub-subscription this session). Stays PARTIAL: runtime subscription-flow ACs (#2 POST 201 / #3 response shape / #5-#6 confirmPayment activation) need a live POST `/api/platform/subscriptions` walk (not done this session); regression IT drafted (`SubscriptionPendingNullableColumnsIT`, Flyway-replay Testcontainers) but had an FK-parent-instance seed bug → **GAP-1054** tracks the fixed IT.

## Acceptance Criteria

- [x] V62 migration created + applied on local stack — **verified live 2026-06-07** (`is_nullable=YES` both cols, V62 success=t)
- [x] POST /api/platform/subscriptions với BASIC trên Owner FREE/TRIAL trả HTTP 201 (không 409) — **verified live 2026-06-07** (Owner kc1walk3 via gateway :9000 → HTTP 201)
- [x] Response shape: `status=PENDING, startedAt=null, expiresAt=null, pendingTier=BASIC, pendingPaymentId` set — **verified live 2026-06-07** (response body confirmed; DB row kc1walk3 subscription PENDING, `started_at` null)
- [x] Instance KHÔNG flip ACTIVE (subscription stays PENDING until admin confirm) — **verified live 2026-06-07** (status=PENDING, started_at null)
- [x] Regression IT `SubscriptionPendingNullableColumnsIT` green — 2/2 tests pass (FK-parent-instance seed fix tracked + closed GAP-1054); full kitehub-subscription module BUILD SUCCESS 879 tests

## Out-of-scope (tracked separately)

| Item | Where |
|---|---|
| Payment row bank snapshot (`bank_code='VCB'`, account_number, account_name) | GAP-939 (already fixed PR #2153) — chained re-walk concern, not the V62 nullable fix |
| PaymentService.confirmPayment → applyPendingUpgrade activation (PENDING → ACTIVE sets startedAt/expiresAt) | Downstream activation flow — separate from the V62 nullable-column fix that this gap scopes |

## Log

- **2026-06-07 (Wave p0-prov-1 closure):** Status PARTIAL → 🟢 DONE. V62 fix was code-complete pre-wave (PR #2157); this wave verified the runtime flow live. POST `/api/platform/subscriptions` BASIC (Owner kc1walk3, via gateway :9000) → **HTTP 201**, response `status=PENDING, startedAt=null, expiresAt=null, pendingTier=BASIC, pendingPaymentId` set. DB row: kc1walk3 subscription PENDING, `started_at` null. Previously 409 (NOT NULL violation) — V62 live-confirmed (`is_nullable=YES` both cols, `flyway_schema_history` V62 success=t). Regression IT `SubscriptionPendingNullableColumnsIT` 2/2 green (FK seed fix → GAP-1054 DONE); full kitehub-subscription module BUILD SUCCESS 879 tests. Payment bank snapshot + confirmPayment activation moved to §Out-of-scope (separate concerns).

## Related

- Triggered by: Wave flow-kh3 G1 walk 2026-06-04 (first walk step)
- Caused by: PR #2151 SUB-20 fix (merged main 1ce04fc0)
- Sister concerns: GAP-939 Payment account_number snapshot (already fixed PR #2153) — would chain-test in same re-walk
- Rule cite: `audit-to-gap-pipeline.md` §2.6 wave-plan state-check + Entity ↔ Migration triad drift
- Rule cite: `release-fix-retry-budget.md` §3.5 investigation phase mandate (apply trong fix PR body)
- Meta gap candidate: triad drift CI detector blind to nullability-only contract changes — file follow-up if recurrence ≥1
