# GAP-192: Trial → Paid Zero-Downtime Migration Design

**Status:** 🟡 PARTIAL (Phase 3 design docs 2026-04-20; Phase 4a backend state machine + outbox + endpoints 2026-04-21; Phase 4b-i backend completeness — webhook HMAC + async scheduler + idempotency persistence + retry + admin force-convert — 2026-04-21; only Phase 4c frontend integration remaining)
**Priority:** 🔴 P0 (business-logic tier — conversion-critical, SaaS standard)
**Domain:** Backend / SaaS Lifecycle / KiteHub
**Found:** 2026-04-20 (action-1 §5.1 + §15.C)
**Wave:** Wave 9 (Business-Logic-P0, front of tier) — Agent 9-A
**Affects:** All trial tenants converting to paid (SaaS conversion funnel), KiteHub subscription service, KiteClass instance lifecycle

## Current State (verified 2026-04-20)

Per `.claude/rules/audit-to-gap-pipeline.md` Step 2.5 — code state before filing:

| Piece | File / Path | Status |
|-------|-------------|--------|
| `InstanceStatus` enum | `kitehub-platform/domain/enums/InstanceStatus.java` | ✅ 6 states (PENDING, TRIAL, ACTIVE, SUSPENDED, DELETED, PURGED) — no MIGRATING sub-state |
| `TrialService.convertTrialToSubscription()` | `kitehub-subscription/service/TrialService.java:175` | ✅ exists as simple flip TRIAL→ACTIVE — no state machine, no outbox, no rollback |
| `trial-lifecycle/` 3-layer docs | `documents/01-business/kitehub/trial-lifecycle/` | ✅ exists (TR-01..TR-07); UC-TR-03 mentions "zero downtime" but no formal design |
| `subscription-billing/` 3-layer docs | `documents/01-business/kitehub/subscription-billing/` | ✅ exists |
| `trial-to-paid-migration/` 3-layer docs | `documents/01-business/kitehub/trial-to-paid-migration/` | ✅ **drafted in this gap's Phase 3** (rules.md + use-cases.md + api-contract.md) |
| `BRD trial-to-paid-conversion.md` | `documents/00-brd/` | ✅ **drafted in this gap's Phase 3** |
| Migration phase column | — | ❌ not implemented — needs `migration_phase` column + Flyway migration |
| Outbox events | — | ❌ outbox pattern used elsewhere but not wired for migration events |
| Rollback service | — | ❌ no rollback path exists — payment reversal handling absent |

**Conclusion:** Simple flip exists; full zero-downtime design (state machine + outbox + rollback + SLA) is NEW work. Scope confirmed BL-P0. Phase 3 (design docs) DONE; Phase 4 (implementation) queued for Wave 9 Agent 9-A.

## Problem

No design exists for the data-handoff + lifecycle transition when a trial tenant upgrades to a paid subscription:

- Unclear if the trial instance is **reused** (same DB, same subdomain, status flip) or **re-provisioned** (fresh paid instance, data migrated).
- No documented state machine — current `InstanceStatus` does not distinguish `TRIAL_ACTIVE`, `TRIAL_ENDING`, `PAID_MIGRATING`, `PAID_ACTIVE`.
- Downtime SLA undefined — user explicitly asked (action-1 line 33): "có down time hay không?"
- No rollback procedure if payment reverses (chargeback, failed capture after provisional upgrade).
- No outbox event pattern for the transition — risk of KiteClass cache/branding stale after upgrade.
- No data integrity guarantee for in-flight writes during the cutover window.

## Context

Discovered in action-1 reorganization (§5.1 Trial mechanics). Related gaps:
- GAP-092 re-trial prevention — DONE
- GAP-093 trial backup — DONE
- GAP-108 trial config hardcoded — OPEN (blocks clean config for this gap)
- **GAP-026 Trial/Freemium AI Mechanics** — OPEN, P1, partial overlap: GAP-026 asks "Trial → paid conversion: preserve branding?" (AI-budget + branding-preserve layer). This gap (GAP-192) owns the **data-handoff + lifecycle state-machine + downtime SLA** layer. Both must align: GAP-026 answers *what AI assets / budget follows the tenant*; GAP-192 answers *how the instance migrates without downtime*.

New P0 because: (1) conversion funnel is the core SaaS revenue path, (2) any downtime at upgrade = user mistrust at the worst moment, (3) no design = ad-hoc implementation later with migration risk.

## Proposed Fix

**3-layer docs** at `documents/01-business/kitehub/trial-to-paid-migration/`:
- `rules.md` — state machine constants, SLA thresholds, config keys
- `use-cases.md` — UC-TRIAL-UPGRADE, UC-TRIAL-ROLLBACK, UC-TRIAL-EXPIRE
- `api-contract.md` — upgrade endpoint, webhook callback, status query

**State machine (candidate):**
```
TRIAL_ACTIVE ─upgrade_initiated─► TRIAL_ENDING
TRIAL_ENDING ─payment_captured─► PAID_MIGRATING
PAID_MIGRATING ─cutover_complete─► PAID_ACTIVE
PAID_MIGRATING ─payment_reversed─► TRIAL_ENDING (rollback)
TRIAL_ENDING ─grace_expired─► ARCHIVED
```

**Outbox events:** `trial.upgrade.initiated`, `payment.captured`, `instance.migrated`, `branding.refresh.required`, `payment.reversed`.

**Zero-downtime strategy options (decide in design):**
1. **Flip-in-place** — same instance, status flip only; no data move. Lowest risk; needs config key toggling.
2. **Shadow-provision** — spin up paid instance, sync data, cut DNS. Safer for tier-level config changes.
3. **Hybrid** — flip for within-tier, shadow for cross-tier (e.g., FREE → ENTERPRISE).

**BRD reference:** add `documents/00-brd/trial-to-paid-conversion.md` if not covered by GAP-150 Phase 1 skeletons.

## Acceptance Criteria

### Phase 1 — Design
- [ ] 3-layer docs drafted with state machine + outbox events
- [ ] SLA: downtime budget stated (target: 0s user-visible)
- [ ] Rollback path for payment-reversal within 24h
- [ ] BRD ref doc cross-linked
- [ ] Dependency on GAP-108 (config hardcoded) explicitly noted

### Phase 2 — Implementation
- [ ] State machine enforced by `InstanceLifecycleService`
- [ ] Outbox events wired + consumed by KiteClass cache invalidation
- [ ] Integration test: trial → paid → rollback → stable
- [ ] Synthetic monitor: conversion path probed every 15 min

## Related

- action-1 §5.1 trial mechanics + §15.C
- `simulation-action-1-2026-04-20.md` Part A (proposed)
- GAP-092 / GAP-093 / GAP-108 (upstream trial work)
- GAP-009 instance provisioning lifecycle (state machine foundation)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Business-Logic tier 2026-04-20)
- Rule: `.claude/rules/ai-branding-guidelines.md` §6 lifecycle state machine

## Log

- 2026-04-21 — **Phase 4b-i backend completeness shipped** (Wave 9.5 Agent 9.5-A). Added HMAC-SHA256 migration webhook at `POST /api/platform/webhooks/trial-migration` (`MigrationWebhookController` + `MigrationWebhookVerifier` + `MigrationWebhookEventType`) — accepts `payment.captured` + `payment.reversed`, verifies signature over raw body, dispatches into `TrialToPaidService.handlePaymentCaptured`/`handlePaymentReversed`. Path deviates from api-contract.md `/webhooks/payment` because that path is owned by the legacy VietQR `PaymentWebhookController`; deviation documented inline. Added `MigrationScheduler` with two `@Scheduled` entry points: `tick()` at `kitehub.trial-to-paid.scheduler-fixed-delay-ms` (default 5s) drains PAYMENT_CAPTURED → MIGRATING with in-JVM AtomicBoolean concurrency guard, and `purgeExpiredIdempotencyKeys()` at `0 * * * * *` cron. Idempotency persistence: `MigrationIdempotencyKey` entity + repository + service + Flyway `V20__add_idempotency_keys.sql` (10-min TTL per api-contract.md). `TrialToPaidService.initiateUpgrade` now short-circuits on cached key returning original 202 envelope; `executeMigrationWithRetry` wraps `executeMigrationInternal` with T2P-09 policy (3 attempts, backoff `[1,3,9]` seconds, `terminalOnFailure` flag prevents mid-retry MIGRATION_FAILED) and resets phase to PAYMENT_CAPTURED between attempts via `resetToPaymentCapturedForRetry`. Admin endpoints: `AdminMigrationController` under `/api/platform/admin/instances/{id}/{force-convert|rollback-migration}` (UC-T2P-05 + UC-T2P-02) — existing `AdminApiKeyInterceptor` provides X-Admin-Key enforcement automatically. `forceConvert` delegates to new `TrialToPaidService.forceConvert` which builds canonical UpgradeRequest, runs initiateUpgrade, then advances straight to PAYMENT_CAPTURED with `manual=true` tag on the capture event. Rollback endpoint moved from `TrialToPaidController` to `AdminMigrationController` (old controller now only exposes user-initiated upgrade). `TrialService.convertTrialToSubscription()` marked `@Deprecated(since="1.0.0 GAP-192 Phase 4b-i")` with javadoc pointing callers to `TrialToPaidService.initiateUpgrade` — kept in place as the terminal delegate from `executeMigrationInternal`. New DTO `ForceConvertRequest` (tier + billingCycle + invoiceRef + reason). `UpgradeResponse` changed to `@Builder(toBuilder=true)`. `InstanceRepository.findByMigrationPhase(MigrationPhase)` added for scheduler. `TrialToPaidConfig` extended with `retryAttempts`, `retryBackoffSeconds`, `schedulerFixedDelayMs`, `webhookSecret`, `idempotency.ttlMinutes`. Entity scan in `KitehubSubscriptionApplication` widened to include `com.kitehub.subscription.idempotency`. **Tests:** 45 new tests added (285 → 330 total, 100% pass). Coverage: `MigrationIdempotencyKeyServiceTest` (8), `MigrationWebhookVerifierTest` (6), `MigrationWebhookControllerTest` (7), `MigrationSchedulerTest` (5), `TrialToPaidServiceRetryTest` (7: retry/forceConvert/handlePaymentReversed Nested groups), `AdminMigrationControllerTest` (4), plus existing `TrialToPaidServiceTest` (20) still green with updated constructor. **api-contract.md** updated to document the `/webhooks/trial-migration` path adjustment. **Only Phase 4c (frontend) remaining** — FE upgrade modal, status banner, polling client for `trial-status.migrationPhase`, admin tools UI for force-convert + rollback (status remains 🟡 PARTIAL pending FE).
- 2026-04-21 — **Phase 4a backend implementation shipped** (Wave 9 Agent 9-A). Added `MigrationPhase` enum (8 values + transition validator) at `kitehub-platform/domain/enums/MigrationPhase.java`; extended `Instance` entity with `migrationPhase`/`migrationStartedAt`/`migrationCompletedAt`/`migrationFailureReason`. Flyway `V19__add_migration_phase_column.sql` adds column + check constraint + `migration_outbox` table with undispatched-index. New `TrialToPaidService` orchestrates `initiateUpgrade` / `handlePaymentCaptured` / `executeMigration` / `rollback` with all 7 outbox events (`trial.upgrade.initiated`, `payment.captured`, `instance.migrated`, `branding.refresh.required`, `payment.reversed`, `migration.rolled_back`, `migration.failed`) written in same JPA txn. `TrialToPaidController` exposes `POST /api/platform/instances/{id}/upgrade` (202 Accepted) + `POST /api/platform/admin/instances/{id}/rollback-migration`. Extended `GET /trial-status` with 4 migration fields. `MigrationException` maps to HTTP 402/409/410/423 via `GlobalExceptionHandler`. **Tests:** 20 new tests in `TrialToPaidServiceTest` (6 nested groups — initiateUpgrade/handlePaymentCaptured/executeMigration/rollback/state-machine-invariants) covering happy path, rescue-window, reversal-window, retry-exhaustion, invalid transitions. Full `kitehub-subscription` suite: 285 tests, 0 failures. **Deferred to Phase 4b:** scheduled worker for `PAYMENT_CAPTURED → MIGRATING` (MVP is sync), webhook HMAC verification + `POST /webhooks/payment` endpoint, idempotency key persistence, admin force-convert endpoint, exponential-backoff retry (`@Retry(resilience4j)` T2P-09 1s/3s/9s), FE upgrade modal + status banner, `TrialService.convertTrialToSubscription()` refactor to delegate via `TrialToPaidService` (kept simple flip for now to preserve behavior). Follow-up gaps reserved but not yet filed: GAP-202 (async worker + retry), GAP-203 (webhook HMAC + idempotency), GAP-204 (FE upgrade flow).
- 2026-04-20 — Created from action-1 §15.C as first Business-Logic-P0 entry under new tier rule.
- 2026-04-20 — **Phase 3 design drafted.** 3-layer docs published at `documents/01-business/kitehub/trial-to-paid-migration/` (rules.md 14 rules T2P-01..T2P-14, use-cases.md UC-T2P-01..06, api-contract.md 5 endpoints + 7 outbox events). BRD at `documents/00-brd/trial-to-paid-conversion.md`. State machine formalized: `MigrationPhase` sub-state (NONE → INITIATED → PAYMENT_PENDING → PAYMENT_CAPTURED → MIGRATING → COMPLETED, with REVERSED + MIGRATION_FAILED branches). Strategy: flip-in-place (default), shadow-provision flag-gated for future cross-tier. Status → 🟡 PARTIAL pending Wave 9 Agent 9-A implementation.
