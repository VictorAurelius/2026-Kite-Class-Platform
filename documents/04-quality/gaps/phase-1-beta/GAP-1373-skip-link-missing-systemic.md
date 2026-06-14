# GAP-1373: Skip-to-content link thiếu systemic (KH toàn bộ + KC dashboard/auth/teacher) — WCAG 2.4.1

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-14 (UI review full audit, AUDIT-2026-06-14-ui-review-full)
**Affects:** `kitehub/kitehub-frontend/src/app/**/layout.tsx` + `kiteclass/kiteclass-frontend/src/app/(dashboard|auth|teacher)/layout.tsx`

## Problem

Skip-to-content link ("Bỏ qua, tới nội dung chính") CHỈ tồn tại ở `kiteclass-frontend/src/app/(public)/layout.tsx` (pattern đúng: `sr-only focus:not-sr-only` → `<main id="main-content" role="main">`). KiteHub frontend (toàn bộ route group: public/auth/customer/admin/school-admin) có **0 skip-link**; KiteClass dashboard/auth/teacher layout cũng **0 skip-link**.

Hệ quả WCAG 2.4.1 Bypass Blocks (Level A): keyboard-only / screen-reader user phải Tab qua toàn bộ nav/sidebar mỗi lần đổi page trước khi tới nội dung — đặc biệt nặng ở dashboard có sidebar dài.

## Root Cause

Skip-link được implement riêng lẻ ở KC public layout (Wave landing) nhưng chưa propagate thành shared pattern qua các layout còn lại. Không có lint/CI check cho skip-link presence per layout.

## Proposed Fix

Trích pattern từ `(public)/layout.tsx` thành shared component (vd `SkipToContent`) và wire vào MỌI top-level layout có nav: KH `(public|auth|customer|admin|school-admin)/layout.tsx` + KC `(dashboard|auth|teacher)/layout.tsx`. Mỗi layout đảm bảo có `<main id="main-content">` target tương ứng.

## Acceptance Criteria

- [ ] Mỗi top-level layout (KH + KC) render skip-link `sr-only focus:not-sr-only` trỏ `#main-content`
- [ ] `<main id="main-content" role="main">` tồn tại trong mỗi layout đó
- [ ] Keyboard Tab đầu tiên trên mỗi page surface skip-link
- [ ] (optional) CI grep check skip-link presence per layout

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, WCAG 2.4.1)
- Pattern source: `kiteclass/kiteclass-frontend/src/app/(public)/layout.tsx:144-219`
- WCAG 2.4.1 Bypass Blocks (Level A)
