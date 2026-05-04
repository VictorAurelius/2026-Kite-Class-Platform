# GAP-285: AdminControllerTest.testGetRevenue failing on PR CI

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 — single test, blocks `Test KiteHub Admin Service` job + rollup `Test Results`
**Domain:** Backend (kitehub-admin)
**Found:** 2026-05-04 (during GAP-284 PR #737 CI triage)
**Affects:** `kitehub-ci.yml` Test Results aggregate; pre-existing failure surfaced on every PR (admin-touching or not)

## Problem

`Test KiteHub Admin Service (strict-warnings — GAP-245)` job in `kitehub-ci.yml` reports:

```
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 17.54 s <<< FAILURE! -- in com.kitehub.admin.controller.AdminControllerTest
[ERROR] com.kitehub.admin.controller.AdminControllerTest.testGetRevenue -- Time elapsed: 0.049 s <<< FAILURE!
```

Surfaced during PR #737 (Docker workspace fix — unrelated diff). Confirmed the failure is pre-existing and not caused by Docker workspace work — diff for #737 touches only Docker + next.config + .dockerignore + GAP file + workflow yaml.

`kitehub-ci.yml` no longer has `push: main` trigger (per CLAUDE.md solo-dev policy), so main-branch baseline runs aren't available — failure history must be reconstructed from PR runs.

## Root Cause

Unknown — needs investigation. Likely candidates:
- Mock data drift after a recent admin migration (admin enums in log: `migration_phase`, `response_phase`, `domain_status`)
- Hibernate/JPA query change post-Wave 5/6
- Strict-warnings flag enabled by GAP-245 surfacing a previously-tolerated condition

## Proposed Fix

1. Pull failing test stack trace + assertion message from `kitehub/kitehub-admin/target/surefire-reports`
2. Identify whether the regression is real (admin revenue endpoint) or mock-test-only
3. Fix in dedicated PR (NOT bundled with #737 Docker fix per `audit-to-gap-pipeline.md` § scope discipline)

## Acceptance Criteria

- [ ] Root cause documented in this gap file
- [ ] `Test KiteHub Admin Service` job green on a PR touching admin
- [ ] No regression in admin revenue calculation if the test was catching a real bug

## Related

- Surfaced by: PR #737 (GAP-284 Docker workspace fix)
- Strict-warnings: GAP-245
- Failing run: 25300192838 / job 74165717775
- ci policy: CLAUDE.md "CI Trigger Policy — Solo-dev Mode"

## Log

- **2026-05-04** — Filed during PR #737 CI triage. Pre-existing — out of scope for #737.
