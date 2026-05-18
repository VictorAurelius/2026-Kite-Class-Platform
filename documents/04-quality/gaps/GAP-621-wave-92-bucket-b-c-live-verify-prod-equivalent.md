# GAP-621: Wave 92 Bucket B+C — live verify GAP-432 boundary tests + GAP-600 IT trên prod-equivalent env

**Status:** 🔵 OPEN (gated GAP-612 AWS restoration)
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-05-18 (Wave 92 closure scope-completeness audit per `wave-closure-scope-completeness.md` §3)
**Affects:** Wave 92 Bucket B + Bucket C closure integrity — CI Testcontainers PASS nhưng prod-equivalent env verification (real Postgres + RabbitMQ + scheduler context) defer

## Problem

Wave 92 Bucket B (PR #1515) + Bucket C (PR #1512) shipped:
- Bucket B: 5 boundary tests `PaymentControllerPaginationBoundaryTest` + 17 unit tests `jwt-storage.test.ts` + 3 two-tab simulation tests
- Bucket C: 6 unit tests `BetaRequestAbortCleanupSchedulerTest` + 5 Testcontainers IT `BetaAccessRequestRepositoryPostgresIT`

CI verifies code-level (all 12-18 checks SUCCESS). NHƯNG **production-equivalent verify defer** per `gap-done-discipline.md` §2 row 6:
> For schema/migration/infra/CI gaps, the closing PR shows verification on a fresh equivalent environment — not just "passed locally on my machine after I worked around X."

Specific verify items defer:
- GAP-432 boundary tests trên Postgres production-equivalent (CI dùng H2 OR Testcontainers ephemeral; prod Postgres có constraints + connection pool config khác)
- GAP-600 scheduler @Scheduled execution trên prod cron context (CI test mock @Scheduled trigger; live prod scheduler có timezone + jvm flag interplay)
- GAP-600 V53 composite index query plan verify trên prod data shape (CI synthetic data; prod có real distribution)

GAP-612 AWS suspension chặn deploy → prod-equivalent verify defer.

## Root Cause

Wave 92 plan §6 Agent Spawn Pattern offline-safe design + Wave 91 Coordinator F deploy chain gated GAP-612 → prod-equivalent verify naturally defer. Wave 92 closure flipped status: complete dù verify orphan.

Per `pre-handoff-self-test-completeness.md` §2.5 file-upload flow + §2.9 background job/async flow, prod-equiv verify mandatory cho:
- Scheduler retry on failure → DLQ
- Status query endpoint returns correct state
- Completion notification fires within SLA

CI test cover (a)-(b) [enqueue + worker pick]; (c)-(f) [retry + DLQ + status + completion] defer prod-equiv.

## Proposed Fix

### Phase 1: Wait GAP-612 AWS restoration (same precondition as GAP-620)

### Phase 2: Prod-equivalent verify (~30-60min)

**Bucket B verify:**
```bash
# Boundary test prod Postgres
curl -X GET -H "Authorization: Bearer $JWT" \
  "https://api.kitehub.me/api/v1/payments?page=0&size=250"
# Expect: HTTP 200 OR 400 (size > 200 sanitiser); verify behavior matches CI test
```

**Bucket C verify:**
```bash
# Scheduler execution prod context
# SSM to subscription EC2 → tail logs
docker logs kitehub-subscription --since 6h | grep -E "BetaRequestAbortCleanupScheduler|cleanup"
# Expect: scheduled invocations cron 0 0 */6 * * * (every 6h)

# V53 composite index query plan
docker exec kite-postgres psql -U kite -d kitehub -c \
  "EXPLAIN ANALYZE SELECT * FROM beta_access_request WHERE status='PENDING' AND created_at < NOW() - INTERVAL '24 hours';"
# Expect: Index Scan using idx_beta_access_request_status_created_at
```

### Phase 3: Document verify results

Log to `documents/04-quality/audits/aws-verification/2026-05-{XX}-wave-92-bucket-b-c-live-verify.md` per `agent-aws-access.md` §5.

## Acceptance Criteria

- [ ] GAP-612 AWS restoration confirmed (precondition)
- [ ] Bucket B boundary tests verified trên prod Postgres (5 cases match CI behavior)
- [ ] Bucket C scheduler execution verified prod cron context (≥1 scheduled invocation logged)
- [ ] Bucket C V53 composite index query plan verified (Index Scan, not Seq Scan)
- [ ] Audit artifact saved per `agent-aws-access.md` §5
- [ ] Status flip DONE only sau Phase 2+3 complete
- [ ] Cross-reference GAP-432 + GAP-600 status notes (these gaps already DONE code-level Wave 92; this gap captures prod-equiv verify)

## Related

- Wave 92 Bucket B PR: [#1515](https://github.com/VictorAurelius/2026-Kite-Class-Platform/pull/1515) — GAP-432 + GAP-599
- Wave 92 Bucket C PR: [#1512](https://github.com/VictorAurelius/2026-Kite-Class-Platform/pull/1512) — GAP-600
- GAP-612 — AWS account suspension (blocker precondition)
- Rule: `gap-done-discipline.md` §2 row 6 production-equivalent env verify mandate
- Rule: `pre-handoff-self-test-completeness.md` §2.9 background job/async flow checklist
- Rule: `wave-closure-scope-completeness.md` §3 (sister gap GAP-619 + GAP-620 same wave closure)
- Rule: `postgres-specific-type-testcontainers.md` §6 prod-equiv test mandate (Bucket C IT đã satisfy code-level)

## Log

- **2026-05-18 (filed):** Filed by Wave 92 closure scope-completeness audit per `wave-closure-scope-completeness.md` v1.0.0 §3 reconciliation. Orphan item surfaced khi user-flagged 2nd recurrence — code-level tests PASS CI (Bucket B 27/27 BE + 22/22 FE; Bucket C 6 unit + 5 IT) nhưng prod-equivalent verify defer vì GAP-612 chặn deploy. Status OPEN until GAP-612 restore + Phase 2-3 execution.
