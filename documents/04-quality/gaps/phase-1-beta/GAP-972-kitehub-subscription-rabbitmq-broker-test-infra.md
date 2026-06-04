# GAP-972: kitehub-subscription preexisting RabbitMQ broker test infra failures

**Status:** 🔵 OPEN
**Priority:** P1
**Domain:** Backend (test infrastructure)
**Found:** 2026-06-04 (Wave flow-kh3 PR #2160 + #2162 CI cluster diagnosis)
**Affects:** Every PR touching `kitehub-subscription/**` — Test KiteHub Subscription Service strict-warnings job FAILs

## Problem

On `main` HEAD post PR #2162 merge (commit `665ce680`), `kitehub-subscription verify` strict-warnings reveals 12 failures + 2 errors in tests interacting với RabbitMQ broker mock. CI log surfaces:

```
WARN com.kitehub.subscription.service.migration.SubscriptionEventEmitter
  -- Fast-path publish failed (eventType=email.queued topic=email.send)
  — dispatcher will retry: broker offline
```

Test classes affected (per PR #2162 CI fix agent diagnosis verified via `git stash + retest` on clean main):
- `EmailServiceClient` — broker connection assertions
- `SubscriptionOutboxDispatcher` — fast-path publish + retry path
- `SubscriptionEventEmitter` — Outbox event emission expects broker handshake

Pattern matches prior gaps:
- GAP-735 (kiteclass-core preexisting flaky tests) — closed Wave meta-3 2026-05-25
- GAP-937 (kitehub-subscription Mockito UnnecessaryStubbing) — closed PR #2155 2026-06-04

This is the 3rd recurrence of "preexisting test infrastructure prevents PR merges on shared module" class.

## Root Cause (Investigation needed)

Likely candidates (need empirical state-check per `release-fix-retry-budget.md` §3.5):
1. **RabbitMQ Testcontainer not starting in CI environment** — broker offline = container failed to bind or pull failed
2. **Mock RabbitTemplate stub drift** — tests stub old method signature; production code post-GAP-925 wave switched to `send(exchange, routingKey, Message)` raw UTF-8 path (sister of GAP-937 fix pattern)
3. **Async timing** — tests assume synchronous broker handshake but production code switched to async outbox

Investigation MUST empirically read failing test names + stack traces + cross-reference với recent `kitehub-subscription/SubscriptionEventEmitter` + `EmailServiceClient` commits.

## Proposed Fix

Phase 1: Investigation (1 session)
- Read full failure log (not grep — Read tool with offset/limit)
- Cross-reference `kitehub-subscription/src/test/.../EmailServiceClient*Test*.java` + `SubscriptionOutboxDispatcher*Test*.java` mock stubs against current production code
- Decide: stub swap (à la GAP-937) OR Testcontainer config fix OR async-timing wait

Phase 2: Fix shipped via dedicated PR per `cross-flow-bug-class-sweep.md` §3 (sweep all sister sites once class identified).

## Acceptance Criteria

- [ ] `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` exits 0 on main HEAD
- [ ] No `ADMIN_MERGE_OVERRIDE: GAP-972` trailer needed for kitehub-subscription-touching PRs prospectively
- [ ] Root cause documented in this gap Log + paired fix PR cites root cause

## Related

- Triggered by: PR #2160 + #2162 cluster diagnosis 2026-06-04
- Tiền lệ: GAP-735 (kiteclass-core), GAP-937 (kitehub-subscription Mockito)
- Sister rule: `cross-flow-bug-class-sweep.md` §1 + `release-fix-retry-budget.md` §3.5 investigation mandate
- Likely paired post-incident: trace back to GAP-925 raw UTF-8 path migration era

## Log

- **2026-06-04** Gap filed during Wave flow-kh3 PR #2160 rebase post-#2162 merge. Empirical evidence: PR #2162 CI fix agent confirmed preexisting (12F/2E) on main HEAD before its own fix. Investigation deferred per `release-fix-retry-budget.md` §3.5 — pre-fix STOP signal until per-test failure log Read tool inspection.
