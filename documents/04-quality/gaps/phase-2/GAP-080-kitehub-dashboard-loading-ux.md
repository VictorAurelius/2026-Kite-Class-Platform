# GAP-080: KiteHub Dashboard Loading/Error UX Inconsistent

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / UX
**Found:** 2026-04-16 (UI audit)
**Affects:** KiteHub customer + admin dashboard pages

## Problem

Dashboard pages khi API fail có 3 patterns khác nhau:
1. **Bare spinner** — instance-detail, branding, branding-wizard, billing-payment: chỉ loading spinner, không timeout, không context
2. **Error banner** — settings, billing, billing-upgrade: red error text nhưng KHÔNG có retry button
3. **Error + retry** — admin-payments, admin-instance-detail: error text + "Thử lại" link

Inconsistent → user experience không đoán được.

## Proposed Fix

1. Tạo shared `ErrorState` component: icon + message + "Thử lại" button + "Quay lại" link
2. Tạo shared `LoadingState` component: skeleton loader + timeout (10s) → auto-switch to ErrorState
3. Apply cả 2 components trên tất cả dashboard pages
4. Thêm `aria-live="polite"` cho error states

## Acceptance Criteria

- [ ] Tất cả dashboard pages dùng cùng ErrorState component
- [ ] Loading state có timeout → tự chuyển sang error sau 10s
- [ ] Mọi error state có "Thử lại" button
- [ ] Skeleton loaders thay thế bare spinners
