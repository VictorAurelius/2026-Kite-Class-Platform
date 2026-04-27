# GAP-241: kitehub-ci.yml does not test kitehub-admin module

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (CI blind spot — admin module tests fail locally but CI is green; latent regression hazard)
**Domain:** DevOps / CI/CD
**Detected:** 2026-04-27 (GAP-238 fix investigation)

## Current State (verified 2026-04-27)

`grep -E "Test KiteHub" .github/workflows/kitehub-ci.yml` returns 3 jobs:
- ✅ `Test KiteHub Platform Module`
- ✅ `Test KiteHub Subscription Service`
- ✅ `Test KiteHub Branding Service`
- ❌ `Test KiteHub Admin Service` — DOES NOT EXIST
- ❌ `Test KiteHub Email Service` — DOES NOT EXIST
- ❌ `Test KiteHub Gateway` — DOES NOT EXIST

## Problem

GAP-238 investigation revealed kitehub-admin tests have been failing locally (pre-existing) without ever blocking CI. Without explicit CI job:
1. Bean collision (GAP-238) shipped to main without ever failing CI
2. JPA repository scan issue (GAP-240) similar
3. Future kitehub-admin regressions ship invisibly until production

Same hazard applies to email + gateway modules.

## Proposed Fix

Add 3 jobs to `.github/workflows/kitehub-ci.yml`:

```yaml
test-admin:
  name: Test KiteHub Admin Service
  runs-on: ubuntu-latest
  steps:
    # ... same pattern as test-subscription, test-branding
    - name: Build and Test Admin Service
      run: cd kitehub && ./mvnw -pl kitehub-admin -am test

test-email:
  name: Test KiteHub Email Service
  # ... same pattern

test-gateway:
  name: Test KiteHub Gateway
  # ... same pattern (note: gateway uses Spring Cloud Gateway — may need different test runner)
```

Or batch as matrix:

```yaml
strategy:
  matrix:
    service: [admin, email, gateway, subscription, branding, platform]
```

(Subscription + branding already have jobs → migrate to matrix to avoid duplication.)

## Acceptance Criteria

- [ ] CI runs tests for all 6 kitehub modules (admin, branding, email, gateway, platform, subscription)
- [ ] Failing test in any module blocks PR merge
- [ ] Existing 3 jobs (platform, subscription, branding) preserved or migrated cleanly
- [ ] Total CI runtime acceptable (parallel matrix preferred)

## Out-of-scope

- kiteclass-core, kiteclass-frontend CI (separate workflows already)
- E2E / integration tests beyond what each module's `mvnw test` runs

## Related

- Parent: GAP-238 (DONE — exposed this gap)
- Sibling: GAP-240 (admin JPA scan — would have been caught earlier with this CI job)
- Solo-dev mode CI policy: `CLAUDE.md` §"CI Trigger Policy"

## Log

- **2026-04-27** — Filed during GAP-238 fix investigation. Pre-existing CI blind spot dating back to original kitehub-ci.yml authorship. P1 because gates future regressions.
