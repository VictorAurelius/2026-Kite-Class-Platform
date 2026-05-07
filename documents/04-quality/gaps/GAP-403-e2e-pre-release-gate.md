# GAP-403: E2E Pre-release Gate trên `tags: v*.*.*`

**Status:** 🟢 DONE 2026-05-07 (Wave 37 Bucket C, PR pending)
**Priority:** 🔴 P0 cho v1.0.0 PRODUCTION (P1 cho v0.9.0-beta)
**Domain:** CI/CD / Testing
**Found:** 2026-05-07 (Wave 37 — Layer 3 Deploy+verify)
**Affects:** Production release quality gate — catch regression trước khi promote tag → production

## Problem

`kiteclass-frontend` đã có 10+ Playwright specs (`e2e/critical-journeys/*`) nhưng KHÔNG có CI job chạy pre-release. `frontend-ci.yml` chỉ unit + build. Tag `v*.*.*` push → image build push ECR → production deploy với KHÔNG có E2E verification.

## Proposed Fix

NEW `.github/workflows/e2e-pre-release.yml`:
- Trigger: `push` tags `v*.*.*-rc*` HOẶC manual `workflow_dispatch`
- Steps: install Playwright → run specs against staging URL (configurable env var) → upload trace HTML report on failure
- Gate: tag không promoted production nếu E2E fail

Phase 2 future: `pnpm test:e2e --reporter=github` direct annotations on PR.

## Acceptance Criteria

- [x] `e2e-pre-release.yml` workflow exists, YAML validated (`python3 yaml.safe_load` PASS)
- [x] Targets `vars.STAGING_URL` (default `https://staging.kite.vn`)
- [x] Playwright install cached via `actions/cache@v4` keyed on Playwright version (~3min saved per run)
- [x] Matrix runs `pnpm -F kiteclass-frontend exec playwright test --project=chromium critical-journeys/` AND `pnpm -F kitehub-frontend exec playwright test --project=chromium beta-funnel/`
- [x] Trace + screenshots uploaded artifact on failure (`if: failure()` upload step, 14d retention)
- [x] Gate summary job fails workflow if any matrix leg fails (blocking promotion)

## Log

- **2026-05-07** Wave 37 Bucket C shipped: `.github/workflows/e2e-pre-release.yml` triggers on `tags: v*.*.*-rc*` + `workflow_dispatch`. Matrix [kiteclass-frontend, kitehub-frontend] × chromium. Verification: YAML safe_load PASS; Playwright `--list` shows 5 beta-funnel specs + 8 visual-regression specs ready (123 total tests in kitehub-frontend). Wired with GAP-404/405. README update for "production tag requires E2E green" tracked under GAP-374 (tag-based release CI orchestration); reflected in `release-deploy-standard.md` §3.4 already.

## Related

- GAP-380 staging environment (sister)
- GAP-404 (beta funnel E2E coverage)
- `release-deploy-standard.md` §3.4 — automated smoke required
- Existing specs: `kiteclass-frontend/e2e/critical-journeys/*.spec.ts`
