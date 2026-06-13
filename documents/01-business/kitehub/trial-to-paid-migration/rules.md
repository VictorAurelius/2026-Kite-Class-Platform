# Trial → Paid Migration — Business Rules

**Last verified:** 2026-04-20 (drafted via GAP-192)
**Config prefix:** `kitehub.trial-to-paid`
**Related domains:** trial-lifecycle (TR-*), subscription-billing (SB-*), ai-branding (AI-BR-*)

## 1. Scope

Rules governing the transition of an instance from TRIAL status to ACTIVE (paid) subscription. Covers:
- The formal state machine of the migration
- Data-handoff guarantees (zero-downtime)
- Rollback behavior on payment reversal
- Outbox events for downstream consumers
- SLA for user-visible availability

**Not in scope (owned elsewhere):**
- Trial start/expiration timers → `trial-lifecycle/`
- Subscription pricing + invoicing → `subscription-billing/`
- AI-budget carryover at upgrade → GAP-026 (captured in `ai-branding/`)

## 2. Rules

| ID | Rule | Value | Config Key | Code Location |
|----|------|-------|-----------|---------------|
| T2P-01 | Migration strategy default | Flip-in-place (same instance, same DB, status flip only) | `kitehub.trial-to-paid.strategy: flip-in-place` | TrialToPaidService (new) |
| T2P-02 | User-visible downtime SLA | 0 seconds (read/write remain available) | `kitehub.trial-to-paid.sla.downtime-seconds: 0` | — (assertion, not a knob) |
| T2P-03 | Backend processing SLA (p95) | ≤ 5 seconds | `kitehub.trial-to-paid.sla.backend-p95-seconds: 5` | Migration p95 timer |
| T2P-04 | Payment-reversal rollback window | 24 hours after migration COMPLETED (measured from `migrationCompletedAt`) | `kitehub.trial-to-paid.reversal-window-hours: 24` | MigrationStateMachine.isWithinReversalWindow |
| T2P-05 | Rescue window after trial expiry | 24 hours — trial expired but grace allows upgrade without data re-provision | `kitehub.trial-to-paid.rescue-window-hours: 24` | TrialExpirationChecker guard |
| T2P-06 | Cross-tier shadow provisioning | Disabled by default (FREE→ENTERPRISE still flip-in-place) | `kitehub.trial-to-paid.shadow-cross-tier: false` | TrialToPaidService branch |
| T2P-07 | Outbox required | Yes — all state transitions publish events via outbox pattern (per `ai-branding-design-patterns.md`) | — | OutboxPublisher |
| T2P-08 | Concurrent in-flight migrations per instance | 1 (pessimistic lock on instance_id during MIGRATING) | — | @Lock(PESSIMISTIC_WRITE) |
| T2P-09 | Retry on MIGRATING failure | 3 attempts, exponential backoff 1s/3s/9s | `kitehub.trial-to-paid.retry.attempts: 3`, `kitehub.trial-to-paid.retry.backoff: [1,3,9]` | @Retry(resilience4j) |
| T2P-10 | Dead-letter after retry exhausted | Mark instance MIGRATION_FAILED; alert ops; preserve trial state | — | Fallback method |
| T2P-11 | Branding refresh on completion | Mandatory — emit `branding.refresh.required` event after COMPLETED | — | OutboxPublisher |
| T2P-12 | AI-budget carryover at upgrade | Yes — trial AI credits carry over; tier-appropriate budget added | cross-ref GAP-026 `kitehub.ai.budget.trial-carryover: true` | BudgetService |
| T2P-13 | Audit log retention | 7 years (tax law — per billing-terms) | `kitehub.trial-to-paid.audit-retention-years: 7` | MigrationAuditEntity |
| T2P-14 | Customer-facing status visible | `GET /trial-status` returns `migrationPhase` field while migrating | — | TrialStatusResponse DTO |

## 3. Migration Phase — Sub-State Machine

`InstanceStatus` (existing 6 values: PENDING, TRIAL, ACTIVE, SUSPENDED, DELETED, PURGED) is preserved as-is.

A new column `migration_phase ENUM` is added to the `instance` table to track in-flight migration detail while `status` remains TRIAL (until completion flip):

```
MigrationPhase:
  NONE               — default (no migration in flight)
  INITIATED          — user clicked Upgrade; payment request submitted
  PAYMENT_PENDING    — awaiting payment gateway confirmation
  PAYMENT_CAPTURED   — payment captured; migration queued
  MIGRATING          — backend running validations + outbox events
  COMPLETED          — status flipped TRIAL → ACTIVE; phase reset to NONE at next tick
  REVERSED           — payment reversed within window; status rolled back to TRIAL
  MIGRATION_FAILED   — retries exhausted; alert ops; phase stays FAILED until manual intervention
```

**Valid transitions:**

```
NONE ──(user upgrade)──► INITIATED ──(submit payment)──► PAYMENT_PENDING
                                                              │
                          ◄──(payment failed)──────┬──────────┘
                         REVERSED                  ▼ (payment captured)
                                               PAYMENT_CAPTURED
                                                    │
                                                    ▼ (async worker picks up)
                                                MIGRATING
                                                    │
                              ┌───(retries ok)──────┼────(retries exhausted)──┐
                              ▼                                                ▼
                          COMPLETED ──(within 24h)──► REVERSED       MIGRATION_FAILED
                                                          │
                                                          ▼
                                                         NONE (status = TRIAL)
```

**Invariants:**
- `status` transitions from TRIAL to ACTIVE **only** when `migration_phase = MIGRATING` and all validations pass, atomically with phase set to COMPLETED.
- While `migration_phase ≠ NONE` and `≠ COMPLETED`, user reads are unaffected (SLA T2P-02).
- REVERSED → NONE is only allowed within 24h (T2P-04); beyond that, refunds are handled via billing dispute flow, not automatic rollback.
- **Reversal-window anchor (GAP-1272 reconciliation):** the 24h window in T2P-04 is measured from `migrationCompletedAt` (the moment `status` flipped TRIAL → ACTIVE), NOT from PAYMENT_CAPTURED. Rationale: PAYMENT_CAPTURED has no persisted timestamp, while COMPLETED does (`migrationCompletedAt`); UC-T2P-02 already states "completed within last 24h". Since PAYMENT_CAPTURED → MIGRATING → COMPLETED happens within the backend p95 of ≤5s (T2P-03), the practical difference is negligible (<5s out of 24h). Code is the single source: `MigrationStateMachine.isWithinReversalWindow` (rule, use-case, and code now agree).

### 3.1 Atomicity & concurrency guarantees (GAP-192)

The zero-downtime flip-in-place (T2P-01/T2P-02) plus the state-machine invariants above are now concretely backed by:

- **Single in-flight migration per instance (T2P-08):** every mutating path loads the instance with a pessimistic write lock (`InstanceRepository.findByIdForUpdate`, GAP-1253) so two concurrent upgrade initiations / webhooks cannot both pass the can-start guard and double-migrate or double-convert.
- **Effective transaction boundaries (GAP-1254):** the retry worker (`MigrationRetryRunner`) is a Spring bean and invokes its per-attempt methods through the proxy, so each MIGRATING → COMPLETED attempt is a real transaction that rolls back atomically on failure; terminal `MIGRATION_FAILED` marking runs in its own `REQUIRES_NEW` transaction so the dead-letter record survives the failed attempt's rollback (T2P-10).
- **Retry idempotency (FM-5):** a retry never re-runs the trial→paid flip on an instance whose prior attempt already reached ACTIVE — `resetToPaymentCapturedForRetry` skips reset when `status = ACTIVE`.
- **Tier never drifts from status (T2P-11 readers):** the denormalized `instances.tier` is set to the requested paid tier through the single SUB-21 sync point (`InstanceTierSyncService`) at upgrade and reset to the FREE trial tier on rollback (GAP-1095 / GAP-1256), keeping connection-pool size / custom-domain eligibility / data-retention consistent with the ACTIVE/TRIAL status.
- **Idempotent upgrade requests:** duplicate `initiateUpgrade` calls with the same idempotency key return the original 202 envelope; a concurrent idempotency-row insert race is absorbed (`REQUIRES_NEW` + `DataIntegrityViolationException` catch, GAP-1271) so the loser still gets a 202, never a 500.

**Known tradeoff:** the requested paid tier is persisted on `instances.tier` at `initiateUpgrade` (so it can be carried to the async, request-less completion step). During the brief PAYMENT_PENDING → MIGRATING window (status still TRIAL, p95 ≤5s) the instance carries the paid tier; rollback resets it to FREE. A future enhancement could add a dedicated `pending_tier` column to defer the effective-tier grant until COMPLETED (would require an `Instance` schema change, out of this scope).

## 4. Config

```yaml
kitehub:
  trial-to-paid:
    strategy: flip-in-place           # alternate: shadow-provision (future)
    sla:
      downtime-seconds: 0
      backend-p95-seconds: 5
    reversal-window-hours: 24
    rescue-window-hours: 24
    shadow-cross-tier: false
    retry:
      attempts: 3
      backoff: [1, 3, 9]              # seconds
    audit-retention-years: 7
```

**Dependency — GAP-108:** trial config currently has hardcoded values in some call sites. GAP-108 closure is required before these keys can be fully honored. Until GAP-108 closes, treat the YAML above as the contract; code must eventually reference it, not hardcoded constants.

## 5. Outbox Events

All emitted via `@Transactional` outbox pattern (same txn as DB write) — no direct broker calls.

| Event | Topic | Payload | Consumers |
|-------|-------|---------|-----------|
| `trial.upgrade.initiated` | `kitehub.migration` | `instanceId, tier, timestamp` | BillingService, analytics |
| `payment.captured` | `kitehub.migration` | `instanceId, amount, method, txnId` | BillingService, outbox dispatcher |
| `instance.migrated` | `kitehub.migration` | `instanceId, fromStatus=TRIAL, toStatus=ACTIVE, completedAt` | KiteClass cache invalidator, email service, BrandingService |
| `branding.refresh.required` | `kitehub.branding` | `instanceId, tier` | BrandingService (tier-dependent template swap) |
| `payment.reversed` | `kitehub.migration` | `instanceId, reason, reversedAt` | BillingService, RollbackService |
| `migration.rolled_back` | `kitehub.migration` | `instanceId, fromStatus=ACTIVE, toStatus=TRIAL, rolledBackAt` | KiteClass cache invalidator, email, BrandingService |
| `migration.failed` | `kitehub.migration.dlq` | `instanceId, failureReason, attempts` | Ops alerting (Alertmanager receiver) |

## 6. Rollback Matrix

| Trigger | Within reversal window? | Action |
|---------|:-----------------------:|--------|
| Payment gateway reversal (e.g., card decline post-capture) | ✅ | Phase → REVERSED; `status` flip ACTIVE → TRIAL; emit `migration.rolled_back`; AI budget restored to trial level |
| Customer chargeback confirmed | ✅ | Same as above |
| Customer chargeback confirmed | ❌ (>24h) | No auto-rollback; open billing dispute case; instance remains ACTIVE (suspend via separate policy if applicable) |
| Internal retry exhausted (MIGRATING fails) | N/A | Phase → MIGRATION_FAILED; status stays TRIAL; ops alert; manual intervention required |
| User requests cancel post-upgrade | ❌ | Normal cancellation flow per off-boarding runbook (GAP-201); NOT a rollback |

## 7. Related Rules + Dependencies

- **Reads from:** `trial-lifecycle/rules.md` (TR-01 duration, TR-07 re-trial block)
- **Writes to:** `subscription-billing/rules.md` (creates subscription row on COMPLETED)
- **Dependency:** GAP-108 (trial config hardcoded) must close first for clean config
- **Cross-ref:** GAP-026 (trial/freemium AI mechanics) — AI-budget preservation layer
- **Cross-ref:** GAP-201 (tenant off-boarding runbook) — post-ACTIVE cancellation, not rollback

## 8. Log

- 2026-06-13 — Wave kitehub-biz-100 Bucket BE-2 (migration atomicity). T2P-04 reversal-window anchor reconciled to `migrationCompletedAt` (GAP-1272) — code is source-of-truth; rule/use-case/code now agree. Added §3.1 atomicity & concurrency guarantees (GAP-192) backing T2P-01/T2P-02/T2P-08 with the shipped fixes: pessimistic-lock mutating loads (GAP-1253), effective retry-worker transactions + REQUIRES_NEW dead-letter marking (GAP-1254), FM-5 retry-after-ACTIVE guard, SUB-21 tier sync at upgrade + FREE reset on rollback (GAP-1095 / GAP-1256), idempotency-row race absorption (GAP-1271). No business-value (constraint/threshold/price) changed — config keys + 24h/5s SLAs unchanged; this entry records the atomicity-implementation reconciliation only.
- 2026-04-20 — Drafted under GAP-192 (BL-P0). State-check confirmed `TrialService.convertTrialToSubscription()` exists as simple flip (UC-TR-03); this doc formalizes the full state machine + outbox + rollback around it, not replaces it.

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — Consumer Protection Law (clear pricing + trial-end disclosure); Luật Quảng cáo (trial-to-paid claims).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Consumer Protection Law revision, paid-conversion regulation update.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
