# GAP-644: BetaRequestAbortCleanupScheduler CloudWatch drift metric (silent failure detection)

**Status:** 🟢 DONE 2026-05-18 (Wave 97 Bucket D salvage — Scheduler edited MeterRegistry drift counter 55 ins + Test updated 93 ins + `scheduler-drift-runbook.md` 146 lines; mvn verify PASS)
**Priority:** 🟡 P2
**Domain:** Backend / Observability
**Detected:** 2026-05-18 (Wave 92 post-wave Security audit v2 P2-2 NEW finding per GAP-619)
**Related Audits:** `documents/04-quality/audits/security/2026-05-18-wave-92-security-audit-v2.md`

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Scheduler implementation | `kitehub/kitehub-subscription/.../BetaRequestAbortCleanupScheduler.java` (Wave 92 Bucket C) | ✅ shipped với @Scheduled + @Transactional + idempotency drift detection |
| Unit tests | `BetaRequestAbortCleanupSchedulerTest.java` 6 @Test | ✅ 11 IT total |
| CloudWatch metric on drift | None | ❌ silent failure mode — production drift undetected |

## Problem

`BetaRequestAbortCleanupScheduler` Wave 92 Bucket C shipped với in-code drift detection (compare `staleCount` returned by query vs `aborted` count from update statement). Tuy nhiên KHÔNG emit Micrometer/CloudWatch metric khi drift detected. Silent failure mode — production drift undetected unless reviewer manually grep logs.

Per Wave 92 Security audit v2 P2-2 NEW finding — observability gap. Drift indicates race condition OR data inconsistency between query + update phases of scheduler.

## Context

Wave 92 post-wave audit suite (GAP-619) shipped 2026-05-18; this gap surfaced từ Security audit v2 P2-2 NEW finding. Paired với Wave 84 ops-readiness baseline + CloudWatch alarm cluster.

## Proposed Fix

1. **Add Micrometer Counter** `kitehub.scheduler.beta_request.abort.drift_count` emit when `staleCount != aborted`
2. **Tag dimensions:** `scheduler_run_id` + `expected_count` + `actual_count` + `delta`
3. **CloudWatch alarm:** threshold `drift_count > 0` for 3 consecutive scheduler runs → SNS notify ops
4. **Runbook entry:** `documents/05-guides/operations/scheduler-drift-runbook.md` — investigation steps (query state at drift moment, race condition vs data inconsistency)
5. **Test coverage:** add `@Test` simulate drift scenario (mock staleCount > aborted via concurrent insert) → verify metric emit

## Acceptance Criteria

- [ ] Counter `kitehub.scheduler.beta_request.abort.drift_count` emitted on drift detection (test fixture verifies)
- [ ] CloudWatch alarm config wired (deferred Phase 1.5+ when AWS observability stack mature)
- [ ] Runbook `scheduler-drift-runbook.md` shipped với investigation flowchart
- [ ] Unit test simulates drift scenario + asserts metric emit
- [ ] Pre-handoff self-test per `pre-handoff-self-test-completeness.md` §2.9 Background job flow

## Related

- **Audit origin:** `documents/04-quality/audits/security/2026-05-18-wave-92-security-audit-v2.md` P2-2 NEW finding
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-94c-gap-619-wave-92-audit-suite.md`
- **Parent gap:** GAP-619 (this gap surfaces from Wave 92 post-wave audit suite)
- **Related observability gaps:** GAP-115 (Grafana dashboards), GAP-122 (alert RateLimitBreachSpike), GAP-144 (alertmanager production receivers)
- **Code:** `BetaRequestAbortCleanupScheduler.java` (Wave 92 Bucket C)

## Log

- **2026-05-18** — Initial write-up. Filed from Wave 92 post-wave audit suite (GAP-619) Security audit v2 P2-2 NEW finding. State-check confirms scheduler shipped với in-code drift detection but no metric emission. Priority P2 — silent failure detection nice-to-have; Phase 1 BETA cohort small enough manual log grep OK; Phase 1.5+ paid scale needs proactive alarm.
