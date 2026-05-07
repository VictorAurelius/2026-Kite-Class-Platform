# GAP-420: KC critical-journeys E2E specs scaffold-as-DONE — UI selector drift

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Phase 4.5 staging E2E gate cannot enforce — KC half of 22-test gate at 0/17 → 4/8 after 1 helper fix)
**Domain:** Frontend / E2E test
**Found:** 2026-05-07 (Option B' E2E full-coverage scope review session)
**Affects:** `e2e-pre-release.yml` Phase 4.5 staging gate (Wave 37 GAP-403); KiteClass frontend production launch readiness

## Problem

`kiteclass/kiteclass-frontend/e2e/critical-journeys/` (3 specs, ~17 tests, written 2026-02-24 per docstrings) shipped scaffold-only — never validated against actual UI. Specs reference English copy + selectors that don't exist on production UI (which is Vietnamese-first per CLAUDE.md communication rule).

**Initial run 2026-05-07** (KC dev server port 4700 + Playwright route mocks):
- **17/17 failed** at `helpers/auth.ts:107` looking for `getByText('Welcome back')`
- Actual UI: `<h1>Chào mừng trở lại</h1>` in `src/components/auth/login-form.tsx`

**After login helper fix** (PR #953 — accepts both VN+EN heading):
- Re-run sample `dashboard-navigation.spec.ts`: **4 passed / 4 failed**
- Failure pattern: downstream selectors still EN-only (e.g., `getByRole('link', { name: /thêm học viên|new student/i })` finds nothing — UI button text differs)

**Pattern recurrence:** Same scaffold-as-DONE issue PR #950 fixed for KH beta-funnel. Wave 37 GAP-403/404/406 shipped E2E specs without local validation; KC critical-journeys are even older (pre-Wave-37) and have larger surface debt.

## Root Cause

1. Specs written before CLAUDE.md Vietnamese-first communication lock → English UI copy assumptions baked in
2. Production UI evolved with Vietnamese labels (per persona reviews + UI audits) but tests not updated
3. No local pre-commit validation gate runs E2E specs against running dev server

## Reproduction

```bash
cd kiteclass/kiteclass-frontend
pnpm exec playwright test critical-journeys/ --project=chromium --reporter=list
# → 17 failed (or 4/8 sample after PR #953 helper fix)
```

## Proposed Fix

Wave-pack Phase 1 — 3 buckets parallel (1 spec file each), per `feedback_parallel_agent_strategy.md` rule #9:

**Bucket A:** `dashboard-navigation.spec.ts` (8 tests)
- Reconcile selectors: sidebar nav links, "Thêm học viên" / "Thêm giáo viên" / "Thêm lớp" create buttons, search input placeholder, logout button
- Verify route paths still match (`/classes`, `/students`, `/teachers`, `/courses` etc.)
- Estimated 4-6 selector fixes

**Bucket B:** `class-lifecycle.spec.ts` (6 tests)
- Reconcile class state machine UI: SCHEDULED→IN_PROGRESS→COMPLETED button labels (likely "Bắt đầu lớp" / "Hoàn thành lớp" / "Huỷ lớp")
- Class code generation copy
- Cancel reason modal labels
- Estimated 6-10 selector fixes (largest spec)

**Bucket C:** `course-to-class-flow.spec.ts` (3 tests)
- Course publish flow UI ("Xuất bản" button)
- DRAFT badge text
- Error toast/alert format
- Estimated 3-5 selector fixes

**Common helper improvements:**
- Add `data-testid` attributes to critical interactive elements in production UI (lower drift risk vs text selectors)
- Document VN-EN parallel regex pattern as helper for future specs

## Acceptance Criteria

- [ ] All 17 KC critical-journeys tests pass locally against `pnpm dev` server
- [ ] Re-run via `pnpm exec playwright test critical-journeys/` → exit 0
- [ ] Phase 4.5 gate matrix `kiteclass-frontend critical-journeys/` returns green when triggered
- [ ] Add 5+ `data-testid` attributes to highest-drift-risk components (sidebar links, primary CTAs)
- [ ] Update spec headers to note "Validated locally YYYY-MM-DD against {commit-sha}"
- [ ] Cross-link memory `feedback_post_merge_doc_sync.md` extension if needed

## Related

- **PR #950** (2026-05-07) — same scaffold pattern fixed for KH beta-funnel; reference for fix style
- **PR #953** (2026-05-07) — login helper VN+EN heading fix; standalone universal value, lands separately
- Wave 37 GAP-403/404/406 — original scaffold ship
- `feedback_post_merge_doc_sync.md` — Tier 1/2/3 enforcement extension may need to cover E2E spec freshness
- `e2e-pre-release.yml` workflow — Phase 4.5 gate consumer
- CLAUDE.md §Communication Language — VN-first principle that specs violate
- `release-1-deploy-runbook.md` §4.5 — staging E2E gate where this manifests in production launch
- `gap-done-discipline.md` §2 — Wave 37 GAP-403/404/406 closure violated criterion 5 (audit-driven gap missing verification artifact)

## Estimated effort

~30-45 min wave-pack (3 buckets parallel @ 10-15 min each + 5-10 min coordinator merge). Could pair with running KC dev backend infra to validate against real-backend (post GAP-419 gateway fix).
