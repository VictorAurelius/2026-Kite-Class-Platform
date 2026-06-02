---
audience: dev
---

# GAP-872 — E2E class-lifecycle: toast/alert strict-mode selector collision (3 sites)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-02 (surfaced on PR #2100 CI — unrelated to that PR's scope)
**Closed:** 2026-06-02
**Affects:** E2E gate `e2e/critical-journeys/class-lifecycle.spec.ts` (route-mocked Playwright)
**Phase:** phase-1-beta

## Problem

E2E gate `class-lifecycle.spec.ts` FAIL trên CI route-mocked Playwright. Surfaced ban đầu chỉ ở test "should show error for invalid class ID" (line 460), nhưng sau khi sửa GAP-830 e2e-helper regression (login giờ chạy được full suite) lộ thêm 2 test cùng bug class:

```
strict mode violation: getByText(/không tìm thấy lớp học|class not found/i) resolved to 2 elements   (invalid-ID)
strict mode violation: getByText(/vui lòng nhập lý do hủy|.../i) resolved to 2 elements                (cancel-reason)
strict mode violation: getByText(/đã sao chép|copied/i) resolved to 2 elements                          (copied)
```

Mỗi assertion `getByText(...)` khớp **2 elements** → Playwright strict mode fail (kỳ vọng đúng 1).

## Root Cause

**Toast/alert dual-surface collision.** Cùng một message hiển thị qua ≥2 element:

1. **invalid-ID:** page-level `ErrorAlert` (Shadcn `Alert`, `role="alert"`) + transient api-client toast → broad `getByText` khớp cả 2.
2. **cancel-reason + copied:** validation/success chỉ hiện qua Shadcn **toast**, mà toast render đồng thời:
   - 1 `<div class="text-sm opacity-90">` (description **visible**)
   - 1 `<span role="status" aria-live="assertive">Notification Lỗi/Thành công ...</span>` (sr-only live-region announcer)
   
   `getByText` khớp cả 2 → strict mode fail. (Lưu ý `role="status"` span là sr-only nên `.first()` rủi ro — `toBeVisible()` có thể fail nếu trúng span ẩn.)

Đây là test-selector specificity issue, KHÔNG phải UI bug — message hiển thị đúng, toast (role=status) là behavior cố ý của api-client (GAP-777). 2 test cancel-reason + copied flaky-pass trên CI trước đây (toast auto-dismiss kịp trước assertion) nhưng deterministic-fail khi chạy local nhanh hơn.

## Fix

3 assertion scoped để khớp đúng 1 element visible:
- **invalid-ID** (line ~466): `page.getByRole('alert').getByText(...)` — scope vào ErrorAlert, loại toast role=status.
- **cancel-reason** (line ~350) + **copied** (line ~405): `page.getByText(...).and(page.locator(':not([role="status"])'))` — loại sr-only announcer, giữ regex + EN fallback gốc.

KHÔNG xóa toast (GAP-777 deliberate api-client behavior). Chỉ narrow test selector.

## Acceptance Criteria

- [x] Cả 3 test (invalid-ID + cancel-reason + copied) PASS — strict-mode selector khớp đúng 1 element
- [x] E2E gate (route-mocked class-lifecycle) green local: `6 passed (36.7s)` — toàn bộ 6 test
- [x] Root cause documented (toast/alert dual-surface, KHÔNG phải UI duplicate)
- [x] api-client toast (role=status) giữ nguyên (GAP-777 behavior không regress)

## Related

- Surfaced PR #2100 (GAP-865 reports FE) — unrelated scope; check non-required nên không block merge
- Combined fix PR cùng GAP-830 e2e-helper sessionStorage regression (sweep miss — xem GAP-830 Log)
- Cùng bug class: `cross-flow-bug-class-sweep.md` — fix 1 site (invalid-ID) → sweep lộ 2 sister sites cùng spec
- `e2e-rst-test-layer-boundary.md` §3 — RST/flake finding → deterministic E2E spec same PR
- api-client toast deliberate: GAP-777
