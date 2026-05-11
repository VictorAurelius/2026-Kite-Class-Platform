# GAP-404: Beta Funnel E2E Coverage (Wave 33-35 flow)

**Status:** 🟢 DONE 2026-05-07 (Wave 37 Bucket C, PR pending)
**Priority:** 🟠 P1
**Domain:** Testing / E2E
**Found:** 2026-05-07 (Wave 37 — gap UI audit identified critical journey uncovered)
**Affects:** Phase 1 BETA invite-only conversion funnel — happy path verification

## Problem

Wave 33-35 ship beta funnel: request-beta-access → admin approve → beta-signup (claim code post Wave 36 388-B) → consent → dashboard. KHÔNG có Playwright spec covering full journey end-to-end.

UI audit 2026-05-07 (Wave 36) flag P1: admin/beta-requests dùng `window.prompt()` — không E2E test thì regression UX dễ slip.

## Proposed Fix

3 spec files trong `kitehub-frontend/e2e/beta-funnel/` (kitehub-frontend chưa có e2e folder — tạo new):
1. `request-flow.spec.ts` — visit `/request-beta-access` → fill form + consent → submit → success page
2. `admin-approve.spec.ts` — login admin → `/admin/beta-requests` → click approve → email mock receives claim code
3. `signup-with-claim-code.spec.ts` — visit `/beta-signup?token=<claim>` → exchange claim code → consent → dashboard

Setup: `playwright.config.ts` với `webServer` start `pnpm dev` + database seed fixtures.

## Acceptance Criteria

- [x] 3 spec files cover happy path: `request-flow.spec.ts`, `admin-approve.spec.ts`, `signup-with-claim-code.spec.ts`
- [x] `kitehub-frontend/e2e/beta-funnel/` folder created (existing `playwright.config.ts` reused — already configured baseURL 4701)
- [x] `pnpm -F kitehub-frontend exec playwright test --list beta-funnel/` shows 5 tests in 3 files (parse PASS)
- [x] Wired vào GAP-403 E2E pre-release workflow (kitehub-frontend matrix leg runs `beta-funnel/`)
- [x] Test data cleanup: specs use `page.route()` to mock all backend endpoints — no DB writes occur, zero pollution risk

## Log

- **2026-05-07** Wave 37 Bucket C shipped: 3 specs in `kitehub/kitehub-frontend/e2e/beta-funnel/` covering request flow + admin approve + claim code signup. Each spec mocks endpoints via `page.route()` per `feedback_kitehub_frontend_msw_missing.md` — route-level mocks until MSW infra lands (separate concern, GAP-272h). Verification: `pnpm exec playwright test --list beta-funnel/` returned `Total: 5 tests in 3 files` cleanly.

## Related

- GAP-403 (parent E2E gate)
- GAP-405 (visual regression baseline)
- Wave 33 PR #898 (BetaAccess BE)
- Wave 35 PR #921 (PDPL consent)
- Wave 36 PR #933 (claim code 2FA)
