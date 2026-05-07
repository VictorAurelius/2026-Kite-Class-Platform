# GAP-404: Beta Funnel E2E Coverage (Wave 33-35 flow)

**Status:** 🔵 OPEN
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

- [ ] 3 spec files cover happy path
- [ ] `kitehub-frontend/e2e/beta-funnel/` folder created với `playwright.config.ts`
- [ ] `pnpm test:e2e --project=chromium beta-funnel/` passes locally
- [ ] Wired vào GAP-403 E2E pre-release workflow (kitehub-frontend matrix)
- [ ] Test data cleanup post-test (no DB pollution)

## Related

- GAP-403 (parent E2E gate)
- GAP-405 (visual regression baseline)
- Wave 33 PR #898 (BetaAccess BE)
- Wave 35 PR #921 (PDPL consent)
- Wave 36 PR #933 (claim code 2FA)
