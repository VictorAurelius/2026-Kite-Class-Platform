# GAP-469: RLS performance baseline measurement

**Status:** 🟡 PARTIAL — methodology + harness + runbook shipped 2026-05-11 (Wave 57 Bucket C); full staging measurement scheduled at Phase 1 BETA cutover per `release-1-deploy-plan.md` critical-path step 4
**Priority:** 🟡 P2 (pre-Phase-1-BETA-cutover; not launch-blocking but should land before tenant onboarding ramps)
**Domain:** Backend / Database / Performance
**Found:** 2026-05-11 (Wave 56 Bucket A closure — perf AC deferred from GAP-466)
**Affects:** All `kiteclass-core` repository hot paths after V58 RLS migration

## Problem

GAP-466 enabled `ENABLE ROW LEVEL SECURITY + FORCE ROW LEVEL SECURITY` on 51 tenant-scoped tables in `kiteclass-core` plus 12 in `kitehub-subscription`. The full kc-core regression suite (1398 tests) and kh-subscription suite (452 tests) pass clean, so behavioural correctness is verified — but neither suite measures latency under sustained load. The Phase 4 AC "Performance regression <5%" was deferred from GAP-466 closure pending a proper baseline measurement.

## Why deferred (not skipped)

Realistic perf measurement for RLS impact requires:
- A representative dataset (≥100k rows per tenant-scoped table, ≥10 tenants) — TestContainers fresh-DB fixture is too small
- A sustained-load harness (pgbench / k6 / Gatling) running pre-RLS vs post-RLS comparisons
- Measurement of percentiles (p50 / p95 / p99) on representative endpoints, not just average latency
- A staging Postgres instance to avoid contaminating local dev metrics

None of those are in Wave 56 Bucket A scope, which prioritised correctness + backwards-compat. The existing index `idx_students_instance ON students(instance_id) WHERE deleted = FALSE` (and per-table equivalents created in V1/V5/V6/V26) already covers the RLS policy's `WHERE instance_id = ?` predicate, so the realistic expectation is that regression falls well within the 5% budget.

## Proposed Fix

1. **Pick 3 representative endpoints** for measurement:
   - `GET /api/v1/students` — paginated list (high read fan-out, hot path)
   - `POST /api/v1/students` — single insert (write path, exercises WITH CHECK)
   - `GET /api/v1/grades?studentId=X` — filtered list (composite predicate)

2. **Provision staging dataset** (10 tenants × 10k students × 5 courses each ≈ 500k rows total).

3. **Run pgbench / Gatling** against staging:
   - Baseline run: revert V58 + V34 in a sandbox branch (or `ALTER TABLE <t> DISABLE ROW LEVEL SECURITY`)
   - RLS run: V58 + V34 applied (matches main)
   - Same warmup + 5-minute steady-state sample for each

4. **Report deltas** in `documents/04-quality/audits/performance/2026-XX-rls-baseline.md`:
   - p50 / p95 / p99 latency per endpoint
   - Throughput (req/s)
   - DB CPU + index-only-scan ratio
   - If any p95 delta >5% → file follow-up gap (likely a missing composite index)

5. **Update GAP-466** Acceptance Criteria checklist to flip the perf row to ✅; promote GAP-466 status from PARTIAL → DONE per `gap-done-discipline.md` §3.

## Acceptance Criteria

### Phase 1 — Methodology + harness (Wave 57 Bucket C, this PR)

- [x] Three representative endpoints **specified** with query shapes + RLS surface mapping (methodology §2)
- [x] Harness script `scripts/perf/rls-baseline.sh` shipped — pgbench-based, graceful degradation for missing pgbench, `--dry-run` mode passes CI without DB
- [x] Methodology doc `documents/04-quality/audits/performance/2026-05-11-rls-baseline-methodology.md` shipped — Goal/Scope/Metrics/Expected-bound/Acceptance sections
- [x] Operator runbook `documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md` shipped — pre-flight + 3-mode run + interpretation + escalation
- [x] Dry-run validates: `bash scripts/perf/rls-baseline.sh --mode local --dry-run` → exit 0

### Phase 2 — Staging measurement (scheduled at deploy task, NOT this PR)

- [ ] Three representative endpoints measured before vs after RLS on staging-equivalent dataset (≥10 tenants × ≥10k students)
- [ ] Report committed to `documents/04-quality/audits/performance/YYYY-MM-DD-rls-baseline-report.md`
- [ ] All p95 deltas ≤ 5% (else follow-up gap filed for the offending endpoint's index plan)
- [ ] GAP-466 Phase 4 acceptance criterion perf row flipped ✅; GAP-466 promoted PARTIAL → DONE per `gap-done-discipline.md` §3

## Related

- Parent: [GAP-466](GAP-466-multi-tenant-postgres-rls-defense-in-depth.md) — RLS implementation
- Wave: [Wave 56](../../03-planning/waves/wave-2026-05-11-56-rls-hardening.md)
- Skill: `.claude/skills/quality/performance-audit/SKILL.md`
- Standard: `.claude/rules/release-deploy-standard.md` §3.4 — perf bar for first PRODUCTION
- Phase 1 BETA tracker: `feedback_release_1_first_session_priority.md` (auto-loaded memory) — ties into critical-path step 4 (perf gate before cutover)

## Log

- **2026-05-11 (Wave 57 Bucket C)**: Phase 1 shipped — methodology + harness + runbook. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp. ACs split into Phase 1 (this PR, 5/5 ✅) and Phase 2 (staging execution, 0/4 — scheduled at Phase 1 BETA cutover per `release-1-deploy-plan.md` critical-path step 4 + `release-deploy-standard.md` §9 staging-load-test = post-deploy artifact). Artifacts: `scripts/perf/rls-baseline.sh` (executable, --dry-run passes), `documents/04-quality/audits/performance/2026-05-11-rls-baseline-methodology.md`, `documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md`. GAP-466 perf-AC linkage: Phase 2 staging-run report flips it ✅.
- **2026-05-11**: Filed as Wave 56 Bucket A follow-up to capture the deferred perf-measurement AC from GAP-466. Per `gap-done-discipline.md` §3, deferring an AC to a separate PR requires a paired follow-up gap; this is that gap. Status stays PARTIAL on GAP-466 until this lands.
