# GAP-748: kiteclass-frontend E2E test env flake — class-lifecycle.spec.ts ECONNREFUSED backend

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Test infrastructure (Playwright + Next.js dev server + BE mock setup)
**Found:** 2026-05-25 (Wave meta-4 PR #1830 + Wave br-5 closure session)
**Affects:** Mọi PR touching `kiteclass/kiteclass-frontend/**` → E2E gate fail → admin-merge override cycle

## Problem

E2E test `class-lifecycle.spec.ts` fail liên tục trên 3+ PR consecutive khác nhau:

| PR | Branch | E2E gate result |
|---|---|---|
| #1830 | wave/meta-4-vercel-residue-cleanup | FAILURE (re-run 2x) |
| #1805 | wave/beta-readiness-8-bucket-C | FAILURE |
| dependabot | dependabot/npm_and_yarn/kiteclass/kiteclass-frontend | FAILURE |

Failure pattern:
```
[WebServer] Failed to fetch landing page data: Error [AggregateError]
[WebServer]   isAxiosError: true
[WebServer]   [cause]: [AggregateError: ] { code: 'ECONNREFUSED' }
TimeoutError: page.waitForURL: Timeout 15000ms exceeded.
  at kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts:93:5
```

## Investigation

Per `release-fix-retry-budget.md` §3.5 investigation phase:
- Hypothesis "PR #1830 Vercel CSP cleanup gây regression" REJECTED — empirical baseline cho thấy E2E fail trên 2 PRs khác (br-8 Bucket C + Dependabot) trước PR #1830
- Per `e2e-rst-test-layer-boundary.md` §1: E2E owns regression. Pre-existing fail = test env infrastructure issue
- Hypothesis primary: Next.js dev server SSR fetch backend trong test setup; BE service not started trong E2E CI job; ECONNREFUSED → page load fail → waitForURL timeout
- Hypothesis secondary: route-mocked E2E mock không cover landing page initial data fetch path

## Root cause (cần xác định)

Đọc cụ thể:
1. `kiteclass/kiteclass-frontend/playwright.config.ts` — webServer config
2. `kiteclass/kiteclass-frontend/e2e/critical-journeys/class-lifecycle.spec.ts:91-93` — test setup
3. `.github/workflows/frontend-ci.yml` — E2E gate job, có start BE service hay không?
4. Next.js `getStaticProps` / `getServerSideProps` / RSC fetch path trên landing page

## Proposed Fix

### Path A — Mock backend trong E2E webserver setup
Add `route.fulfill()` cho landing page initial fetch trong test setup (per Playwright mock pattern).

### Path B — Skip landing page fetch trong test env
Add env var `NEXT_PUBLIC_E2E_MODE=true` → FE skip BE fetch khi env=test.

### Path C — Start mock BE container trong E2E job
docker-compose service `mock-backend` start trước Playwright run.

Preference Path A (least invasive).

## Acceptance Criteria

- [ ] `class-lifecycle.spec.ts` PASS trên CI 3 consecutive runs
- [ ] No regression khác trong kiteclass-frontend E2E suite
- [ ] Root cause documented + fix path chosen documented trong gap Log

## Out-of-scope

- Broader Playwright test env redesign — track separate gap if scope grows
- BE integration (real BE start trong E2E) — Phase 2+ scope

## Related

- PR #1830 Wave meta-4 — first surfacing (admin-merge với override)
- `release-fix-retry-budget.md` §3.5 — investigation phase mandate (used here)
- `e2e-rst-test-layer-boundary.md` — E2E owns regression guard; pre-existing fail = real infra bug
- `pre-handoff-self-test-completeness.md` — verify discipline
