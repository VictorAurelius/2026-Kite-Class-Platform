# GAP-403: E2E Pre-release Gate trên `tags: v*.*.*`

**Status:** 🔵 OPEN
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

- [ ] `e2e-pre-release.yml` workflow exists, YAML validated
- [ ] Targets `vars.STAGING_URL` (default `https://staging.kite.vn`)
- [ ] Playwright install cached (~3min saved per run)
- [ ] Workflow runs `pnpm -F kiteclass-frontend test:e2e -- --project=chromium critical-journeys/`
- [ ] Trace + screenshots uploaded artifact on failure
- [ ] README cập nhật: "v1.0.0 production tag requires E2E green"

## Related

- GAP-380 staging environment (sister)
- GAP-404 (beta funnel E2E coverage)
- `release-deploy-standard.md` §3.4 — automated smoke required
- Existing specs: `kiteclass-frontend/e2e/critical-journeys/*.spec.ts`
