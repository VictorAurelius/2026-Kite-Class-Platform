# GAP-850: Transient-State Production Suspense/Skeleton Implementation (Phase 2 of GAP-429)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-02 (GAP-429 Phase 1 closure — autonomous local-doable gap campaign)
**Affects:** Production `kiteclass-frontend` + `kitehub-frontend` transient-state rendering (loading/empty/error)

---

## Problem

GAP-429 Phase 1 raised 7 HTML kit prototype screens (loading/empty/error) to ≥108/128 by improving Motion/Interaction + Content/Copy dimensions: async-job step indicators, cache-preserved summary cards during fetch, purpose-built inline SVG empty illustrations, and error-TYPE-distinguished recovery actions.

These improvements live only in the **design-system HTML kit prototypes**. The production Next.js apps still render the weaker transient-state patterns (or none). Phase 2 ports the validated kit patterns into production React components.

## Root Cause

Phase 1 scope was intentionally limited to HTML kit prototypes (per GAP-429 §Proposed Fix Phase 1). Production implementation is a separate code-shaped concern requiring Suspense boundaries + skeleton components + error-boundary recovery logic — cannot ship alongside HTML-prototype edits in the same PR.

## Proposed Fix

Port the GAP-429 Phase 1 kit patterns into production:

- `kiteclass-frontend` `attendance/reports/page.tsx` — Suspense boundary with skeleton component matching `reports-loading.html` (header context preserved + section-aware progress hint)
- `kiteclass-frontend` `attendance/page.tsx` — empty state component matching `attendance-empty.html` (purpose-built SVG + mobile full-width CTA)
- `kitehub-frontend` `app/(dashboard)/branding/page.tsx` — loading state for AI generation jobs matching `branding-hub-loading.html` (step indicator `Bước N/5` + progress bar)
- Error boundaries distinguishing network/service/auth error types per the dashboard-error kit pattern (NETWORK → retry primary; SERVICE 503 → status-page primary + auto-retry; auth → re-login)

## Acceptance Criteria

- [ ] `attendance/reports/page.tsx` Suspense + skeleton matches kit `reports-loading.html` pattern
- [ ] `attendance/page.tsx` empty state matches kit `attendance-empty.html` (SVG + prominent CTA)
- [ ] `kitehub-frontend` branding page AI-job loading state matches `branding-hub-loading.html` step indicator
- [ ] Error boundary distinguishes error type + recovery action per `dashboard-error.html` pattern
- [ ] `pnpm --filter <pkg> build` passes (no prerender bailout per `fe-build-local-verify.md`)
- [ ] Browser walk verifies each transient-state renders correctly

## Related

- Parent: GAP-429 (Phase 1 HTML kit prototypes — PARTIAL, Phase 1 DONE 2026-06-02)
- Source patterns: `documents/02-architecture/design-system/ui_kits/{kiteclass-teacher,kitehub-pro-v2,kiteclass-pro-v2}/screens/` (7 screens at 108/128)

## Log

- **2026-06-02:** Filed as Phase 2 follow-up at GAP-429 Phase 1 closure per `gap-done-discipline.md` §3 PARTIAL exit ramp. Phase 1 (kit HTML) shipped; Phase 2 (production Suspense/skeleton) tracked here separately as code-shaped work.
