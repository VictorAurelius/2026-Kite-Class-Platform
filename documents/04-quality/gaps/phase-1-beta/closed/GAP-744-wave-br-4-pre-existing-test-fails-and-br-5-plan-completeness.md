# GAP-744: Wave br-4 6 pre-existing test fails + Wave br-5 plan completeness CI fail

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (Testing) + Meta
**Found:** 2026-05-25 (Wave beta-readiness-8 closure — admin override pattern surfaced across PRs #1797/#1800/#1805)
**Affects:** CI signal noise across all PRs touching kiteclass-core; admin merge override usage frequency

## Problem

### Subgap A — Wave br-4 6 pre-existing test fails

Wave beta-readiness-4 (Bucket C + D shipped 2026-05-22) introduced 6 pre-existing test failures vào main:
- `EnrollmentIntegrationTest.enrollStudent_shouldIsolate_multiTenantData:471 Status expected:<404> but was:<500>` (1 failure)
- `CourseSecurityTest.*` (4 failures — specific test methods TBD via debug)
- `InvoiceFlowIntegrationTest.*` (1 failure — TBD)

Total: **1484 tests run, 6 Failures, 0 Errors, 54 Skipped**.

Pre-existing nature confirmed via PR #1797 (Bucket E docs-only javadoc) + #1800 (Bucket D+F bundle) + #1805 (Bucket C PaymentMethod) — ALL 3 PRs surfaced same 6 fails despite scope không touch failing test classes.

Per CLAUDE.md "CI Trigger Policy — Solo-dev Mode 2026-04-24" — `core-ci.yml` removed `push: main` trigger; pre-existing fails only surface on PR (first PR after main merge inherits silent failure state from previous main merge).

### Subgap B — Wave br-5 plan completeness CI fail

`scripts/check-wave-plan-completeness.sh` returns FAIL on `documents/03-planning/waves/wave-2026-05-25-beta-readiness-5-beta-signup-unblock.md` post-merge. Local execution PASS (37/0/23) — CI vs local discrepancy investigation needed.

Possible causes:
- Wave plan file missing required `## State-Check Evidence` table
- Wave plan file references absent symbols (per `audit-to-gap-pipeline.md` §2.6 mandate)
- CI script logic differs from local (env var, path resolution, gh CLI vs git diff)

## Root Cause

### Subgap A
Wave br-4 Bucket C #1783 + hotfix #1784 (Course entity field add) shipped without complete IT test fix. Hotfix iteration rate 60% Wave br-4 was symptom — `entity-migration-mapper triad CI gate` (GAP-743) addresses prevention but underlying broken tests need fix.

### Subgap B
TBD — needs investigation. Wave br-5 plan was last reviewed pre-Wave-8 work; rebase post-merge might have introduced incongruence.

## Proposed Fix

### Subgap A — fix 6 pre-existing tests
1. Debug each test individually trên local Docker stack + Testcontainers
2. Likely causes: multi-tenant test isolation broken (instanceId binding) + RLS NULL force-fail interaction + course/invoice fixture data drift
3. Either fix bug OR mark `@Disabled` với cross-link follow-up gap nếu test is documenting actual bug
4. Wave beta-readiness-9 candidate

### Subgap B — investigate br-5 plan
1. Read `wave-2026-05-25-beta-readiness-5-beta-signup-unblock.md` § State-Check Evidence
2. Compare local `check-wave-plan-completeness.sh` vs CI execution env
3. Either fix wave plan content OR fix script logic (whichever incorrect)

## Acceptance Criteria

### Subgap A
- [ ] All 6 failing tests identified với failure mode classification (test bug vs production bug)
- [ ] Production bugs fixed; test bugs corrected; documentation bugs marked `@Disabled` với follow-up gap
- [ ] `cd kiteclass && ./mvnw -pl kiteclass-core verify -P strict-warnings` BUILD SUCCESS clean (no Failures/Errors)
- [ ] Tests run = 1484, Failures = 0, Errors = 0 confirmed via CI

### Subgap B
- [ ] Wave br-5 plan completeness CI fail root cause identified
- [ ] Fix landed (plan content OR script logic)
- [ ] `bash scripts/check-wave-plan-completeness.sh` PASS:38/FAIL:0/EXEMPT:23

## Related

- Wave beta-readiness-4 Bucket C: PR #1783 + hotfix #1784 + #1787 (origin of pre-existing fails)
- Wave beta-readiness-8 PR #1797 Bucket E (first inheritance admin override)
- Wave beta-readiness-8 PR #1800 Bucket D+F bundle (2nd admin override)
- Wave beta-readiness-8 PR #1805 Bucket C (3rd admin override — confirmed pattern)
- GAP-743 entity-migration-mapper triad CI gate (META prevention scope)
- Rule `admin-merge-discipline.md` §4 override mechanism (used by 3 PRs in Wave 8 — exceeds 5%/quarter threshold trigger meta-review)
- Wave br-5 plan: `documents/03-planning/waves/wave-2026-05-25-beta-readiness-5-beta-signup-unblock.md`
- Wave: planned `wave-beta-readiness-9` (P1 scope)

## Log

- **2026-05-25 (created):** Filed per Wave beta-readiness-8 closure — admin override pattern across 3 PRs (#1797/#1800/#1805) confirms Wave br-4 6 pre-existing test fails class. Per ADMIN_MERGE_FOLLOWUP trailers committed trong PR bodies. Wave br-5 plan completeness CI fail noted concurrently (CI vs local discrepancy). Wave beta-readiness-9 candidate scope.
