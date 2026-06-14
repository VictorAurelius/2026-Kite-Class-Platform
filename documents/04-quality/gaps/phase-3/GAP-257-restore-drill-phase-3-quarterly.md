# GAP-257: Restore Drill Phase 3 — Quarterly DR Exercise + Measured RTO Baseline

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps / Data Safety
**Found:** 2026-04-28 (split from GAP-117 closure as Phase 3 follow-up per `gap-done-discipline.md` §3)
**Affects:** Validated RTO/RPO baselines, end-to-end DR confidence

## Problem

GAP-117 Phase 1 (runbook) + Phase 2 (monthly automated drill) SHIPPED 2026-04-28 (PR #632). Phase 3 — quarterly full DR exercise + measured RTO baseline — explicitly deferred because it requires:

1. Real S3 backups accumulated (depends on GAP-093 production deploy generating backup history)
2. Staging-environment coordination + manual sign-off windows
3. CI gate flip (`vars.BACKUP_DRILL_ENABLED=true`) after AWS OIDC role + IAM policies exist
4. Stakeholder availability for full destruction + restore window

Until this lands, RTO/RPO numbers in `documents/05-guides/operations/dr-rto-rpo-matrix.md` (GAP-119) are **target** values, not **measured** values.

## Root Cause

Restore-drill development decoupled from production deploy lifecycle. Phase 1+2 are testable locally with fixtures; Phase 3 demands real prod-equivalent infra.

## Proposed Fix

### Scope (single quarterly exercise)
1. Schedule first exercise window (current proposal: Q3 2026; reconfirm with stakeholders)
2. Pre-exercise checklist:
   - Latest backup ≤24h old in S3
   - Fresh staging Postgres + MinIO instance provisioned
   - DR coordinator available + comms channel staffed
3. Execute per `documents/05-guides/deploy/restore-procedure.md` Scenario A (full destruction → restore)
4. Measure + record:
   - Backup retrieval time
   - Restore-to-usable time (RTO actual)
   - Data lag at restore (RPO actual)
   - Validation script `scripts/verify-restore.sh` exit code
5. Post-exercise:
   - Update `dr-rto-rpo-matrix.md` rows with **measured** numbers (replacing target)
   - File any procedural gaps surfaced as new gaps
   - Lessons-learned entry in `disaster-recovery-plan.md` §11

## Acceptance Criteria

- [ ] First quarterly exercise completed (date + duration recorded)
- [ ] Measured RTO baseline recorded for: subscription DB, tenant DBs, MinIO assets
- [ ] Measured RPO baseline recorded for same components
- [ ] `dr-rto-rpo-matrix.md` updated with measured columns (target vs actual)
- [ ] Recurring quarterly schedule confirmed with stakeholders + added to ops calendar
- [ ] Lessons-learned section in `disaster-recovery-plan.md` §11 populated
- [ ] CI gate `vars.BACKUP_DRILL_ENABLED=true` flipped (after first successful exercise)

## Related

- Parent: GAP-117 (Phase 1+2 DONE, Phase 3 split here)
- Depends on: GAP-093 (backup implementation, DONE) + production AWS deploy (separate roadmap)
- Cross-link: GAP-118 (S3 versioning — restore source), GAP-119 (DR runbook — exercise context)

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-3 (title says Phase 3 quarterly DR).
- 2026-04-28 — Filed as Phase 3 follow-up after GAP-117 PR #632 shipped Phase 1+2. Honest deferral per `gap-done-discipline.md` §3 PARTIAL exit-ramp; not buried in DONE-flip Log entry.
