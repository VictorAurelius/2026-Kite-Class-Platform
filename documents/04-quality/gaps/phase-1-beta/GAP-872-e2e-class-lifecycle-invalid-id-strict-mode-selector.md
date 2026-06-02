---
audience: dev
---

# GAP-872 — E2E class-lifecycle "invalid class ID" test: strict-mode selector matches 2 elements

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-02 (surfaced on PR #2100 CI — unrelated to that PR's scope)
**Affects:** E2E gate `e2e/critical-journeys/class-lifecycle.spec.ts:460` (route-mocked Playwright)
**Phase:** phase-1-beta

## Problem

E2E test `Critical Journey: Class Lifecycle › should show error for invalid class ID` (`class-lifecycle.spec.ts:460`, assertion line ~467-469) FAIL trên CI route-mocked Playwright gate:

```
Error: strict mode violation: getByText(/không tìm thấy lớp học|class not found/i) resolved to 2 elements
Expect "toBeVisible" with timeout 5000ms
```

Selector `getByText(/không tìm thấy lớp học|class not found/i)` khớp **2 elements** trên trang invalid-class-ID → Playwright strict mode fail (expects exactly 1). Test retry 3x đều fail cùng lý do.

Phụ: log có `[WebServer] Failed to fetch landing page data: getaddrinfo EAI_AGAIN kite-gateway` — DNS flake của route-mocked harness (gateway không resolve trong CI), nhưng KHÔNG phải nguyên nhân chính của assertion fail.

Pre-existing trên main — surfaced khi PR #2100 (GAP-865 reports FE) chạy CI; PR #2100 KHÔNG đụng `class-lifecycle.spec.ts` hoặc class-not-found component (chỉ thêm `(dashboard)/reports` page + types/hook/api/chart). Check non-required (mergeable UNSTABLE) → PR #2100 merged bình thường.

## Root Cause

Cần investigate — 2 ứng viên:
1. UI page invalid-class-ID render 2 elements cùng text "không tìm thấy lớp học / class not found" (vd heading + toast, hoặc duplicate sau refactor) → selector cần narrow (`.first()` / role-scoped / test-id).
2. Test selector quá rộng (regex OR match cả 2 locale strings nếu page render cả vi + en) → cần scope theo `getByRole` hoặc `data-testid`.

## Proposed Fix

1. Reproduce local: `pnpm --filter kiteclass-frontend exec playwright test e2e/critical-journeys/class-lifecycle.spec.ts:460 --project=chromium`.
2. Inspect invalid-class-ID page → đếm elements khớp text "không tìm thấy lớp học".
3. Nếu UI duplicate → fix page (1 error element). Nếu test selector → narrow bằng `getByRole('heading', ...)` / `getByTestId('class-not-found')` / `.first()`.
4. Verify E2E gate green sau fix.

## Acceptance Criteria

- [ ] `class-lifecycle.spec.ts:460` PASS (strict-mode selector khớp đúng 1 element)
- [ ] E2E gate (route-mocked class-lifecycle) green trên CI
- [ ] Root cause documented (UI duplicate vs test selector) trong Log

## Related

- Surfaced PR #2100 (GAP-865 reports FE) — unrelated scope; check non-required nên không block merge
- `e2e/critical-journeys/class-lifecycle.spec.ts` line ~460-469
- Sister concern: `kite-gateway` DNS flake trong route-mocked harness (secondary log noise — separate if recurs)
