## GAP-222c: Migrate Remaining kitehub-subscription Direct-Publish Sites to Outbox (re-scoped 2026-04-26)

**Status:** 🔵 OPEN — UNBLOCKED by GAP-222a (per-module pattern established) + GAP-230 (Exception D ruled out for these 2 cases)
**Priority:** 🟠 P1 (reliability — 2 services affecting email + tenant purge)
**Domain:** Backend (kitehub-subscription)
**Found:** 2026-04-26 (Sub-PR 6.4 scope check)
**Re-scoped:** 2026-04-26 (after GAP-222a Phase 2 + GAP-230 triage)
**Parent gap:** GAP-222
**Effort:** M (2-3h — 2 services × Exception A migration + tests; consider extract-pure-dispatcher for EmailServiceClient)

## Re-scope rationale (2026-04-26)

Original scope was "4 services" assuming GAP-222a would extract a cross-product shared lib. ADR-021 + GAP-222a Phase 2 + GAP-230 changed the picture:

| Original site | Resolved by | Status |
|---------------|-------------|:------:|
| `BrandingJobService` | GAP-222a Phase 2 (Exception A via `BrandingEventEmitter`) | ✅ DONE |
| `AIQueueDispatcher` | GAP-230 (Exception D + marker) | ✅ DONE |
| `EmailServiceClient` (kitehub-subscription) | This gap (needs Exception A migration; consider extract-pure-dispatcher) | 🔵 OPEN |
| `InstancePurgeService` (kitehub-subscription) | This gap (needs Exception A migration) | 🔵 OPEN |

Approach for the 2 remaining: copy the per-module outbox pattern into `kitehub-subscription` (it already has `MigrationOutbox` for the migration domain — add `EmailOutbox` + `PurgeOutbox` OR generalize MigrationOutbox into a per-module generic outbox). Decision deferred to this gap's scoping pass.

## Current State (verified 2026-04-26)

| Service | Module | LOC | Bypass site(s) | Has @Transactional? | Outbox infra accessible? |
|---------|--------|----:|----------------|:-------------------:|:-----------------------:|
| `BrandingJobService` | kitehub-branding | 183 | line 69 (within method ~42-90) | ✅ class-level | ❌ blocked on 222a |
| `AIQueueDispatcher` | kitehub-branding | 91 | lines 65 + 70 (`dispatch()`) | ❌ no `@Transactional` annotation | ❌ blocked on 222a |
| `InstancePurgeService` | kitehub-subscription | 228 | line 188 (within `@Transactional` method 90+) | ✅ method-level | ⚠️ only domain-specific `MigrationOutboxRepository` exists |
| `EmailServiceClient` | kitehub-subscription | 663 | line 588 (`publishEmailEvent`) | ⚠️ — verify call sites | ⚠️ same as above |

## Problem

Per `design-patterns.md` §3.5 + §3.5.1 (Sub-PR 6.4):
- These 4 services bypass Outbox without one of the documented exceptions (Exception A/B/C)
- Risk: broker outage → DB committed but event lost (cache stale, queue not dispatched, purge not scheduled, email not sent)
- All four are tenant-facing reliability paths

## Root Cause

Two compounding issues:
1. **No shared outbox lib** (GAP-222a) — kitehub modules cannot import `OutboxEventWriter` without violating module boundaries
2. **No policy** (closed by Sub-PR 6.4 §3.5.1) — without a clear policy, "just use rabbitTemplate" was the path of least resistance during initial implementation

Sub-PR 6.4 closed (2) by adding the policy. (1) remains as GAP-222a blocker for this gap.

## Proposed Fix

### Phase 1 — Wait for GAP-222a (shared outbox lib)
Cannot start until shared infra reachable from kitehub modules.

### Phase 2 — Per-service migration (after 222a)

For each of the 4 services:

1. Add `OutboxEventWriter` dep via constructor
2. Replace `rabbitTemplate.convertAndSend(...)` with `outbox.enqueue(routingKey, aggregateType, aggregateId, payload)` inside the existing `@Transactional` block (or wrap caller in one if missing — see AIQueueDispatcher special case)
3. Add unit test asserting outbox row written
4. Run integration test (TestContainers RabbitMQ) verifying outbox publisher drains → consumer receives

**Special case — `AIQueueDispatcher.dispatch()`** has no `@Transactional` annotation. Two options:
- Option A: Wrap in `@Transactional` (small txn just for outbox row)
- Option B: Caller of `dispatch()` already has a txn — verify call graph, defer wrapping to caller
Pick during implementation after reading caller chain.

**Special case — `InstancePurgeService` + `EmailServiceClient`** in kitehub-subscription:
After 222a, generic `OutboxEventWriter` becomes available. Can keep domain-specific `MigrationOutboxEvent` for migration-only events; new uses go through generic.

### Phase 3 — Verify

Re-run `design-pattern-audit` skill — Cat 5 should drop to 0-1 real bypass (only documented Exception A like `BrandingEventPublisher`).

## Acceptance Criteria

- [ ] All 4 services have no silent direct-publish (only Exception A documented sites remain in codebase)
- [ ] Each migration includes 1 unit test (outbox row asserted) + 1 integration test (consumer receives)
- [ ] `design-pattern-audit` Cat 5 re-run scores ≥ 16/20 with all remaining sites carrying §3.5.1 markers
- [ ] No behavior regression on tenant-facing flows (AI gen, email, purge)
- [ ] PR splits into ≤2 sub-PRs if needed (one per module: branding + subscription) per `audit-to-gap-pipeline.md` "max 3-5 gaps per PR" guidance — but here it's 4 services × 1 gap, so split allowed for review tractability

## Dependencies

- **GAP-222a (BLOCKER)** — shared outbox lib must ship first
- TestContainers RabbitMQ (already in CI)
- Outbox publisher already wired to the same exchanges these services use

## Risk / Tradeoffs

- **Latency** — outbox adds DB row + async dispatch (~50-200ms). Acceptable for non-real-time paths (email, purge). For AI dispatch (`AIQueueDispatcher`) verify SLA assumptions before migrating.
- **Migration ordering** — recommend: BrandingJobService (single site) → InstancePurgeService → EmailServiceClient → AIQueueDispatcher (most complex due to no @Transactional)
- **Consumer compatibility** — outbox publisher publishes to same exchange/routing key, so consumers should not need changes. Verify per service.

## Related

- Parent: GAP-222
- Blocked by: GAP-222a
- Sibling: GAP-222b (ParentInvitationServiceImpl, independent path)
- Rule: `.claude/rules/design-patterns.md` §3.5 + §3.5.1 (this gap is the cleanup; rule prevents recurrence)
- Audit: `documents/04-quality/audits/design-patterns/audit-2026-04-26.md` Cat 5

## Log

- 2026-04-26 — Gap created during Sub-PR 6.4 scope check. State-check confirmed: 4 services, 5 bypass sites total (AIQueueDispatcher has 2), all in kitehub modules. Blocked on 222a until shared outbox infra reachable.
