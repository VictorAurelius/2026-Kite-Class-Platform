# GAP-1377: KC login ssr:false skeleton thiếu aria-busy/aria-live + no-JS chỉ thấy skeleton

**Status:** 🟢 DONE
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-14 (UI review full audit, AUDIT-2026-06-14-ui-review-full)
**Affects:** `kiteclass/kiteclass-frontend/src/app/(auth)/login/page.tsx`

## Problem

KC login page là thin shell `next/dynamic(() => import('login-form'), { ssr: false, loading: <Skeleton/> })` (GAP-236 code-split). SSR HTML render 0 input/label/h1 (chỉ skeleton — đã verify qua curl). Hai vấn đề a11y/UX nhỏ:

1. Skeleton `loading` element KHÔNG có `aria-busy="true"` / `aria-live="polite"` / `role="status"` → screen reader không announce trạng thái "đang tải form" (WCAG 4.1.3 Status Messages).
2. No-JS user (hoặc JS lỗi/chậm) chỉ thấy skeleton vĩnh viễn — không có `<noscript>` fallback hướng dẫn.

Lưu ý: `ssr:false` là chủ ý (auth form không có SEO value, comment ghi rõ) — KHÔNG phải bug. Đây chỉ là polish a11y cho loading state.

## Root Cause

Skeleton fallback viết thuần visual (`<Skeleton>` divs) mà chưa kèm ARIA status semantics; no-JS path chưa cân nhắc.

## Proposed Fix

Thêm `role="status"` + `aria-busy="true"` + sr-only text "Đang tải biểu mẫu đăng nhập…" cho skeleton wrapper. (Optional) `<noscript>` note. Cân nhắc áp dụng cùng pattern cho KH landing `ssr:false` shell.

## Acceptance Criteria

- [x] Skeleton wrapper có `role="status"` + `aria-busy="true"` + `aria-live="polite"` + sr-only loading text "Đang tải biểu mẫu đăng nhập…"
- [x] Screen reader announce loading state khi vào /login (skeleton boxes giờ `aria-hidden`, chỉ đọc sr-only text)
- [ ] (optional) noscript fallback — DEFER (optional polish, không block)

## Resolution

**Fixed:** 2026-06-15 (branch `fix/audit-fixH-ui-2026-06-14`)

`kiteclass-frontend/src/app/(auth)/login/page.tsx`: skeleton `loading` của `next/dynamic(ssr:false)` thêm `role="status"` + `aria-busy="true"` + `aria-live="polite"` + `<span className="sr-only">Đang tải biểu mẫu đăng nhập…</span>`; các `<Skeleton>` visual divs đánh `aria-hidden="true"` để SR chỉ đọc sr-only text (WCAG 4.1.3 Status Messages). `ssr:false` giữ nguyên (chủ ý GAP-236).

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, P3)
- Source: `kiteclass/kiteclass-frontend/src/app/(auth)/login/page.tsx:28-42`
- WCAG 4.1.3 Status Messages (Level AA)
