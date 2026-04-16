# GAP-077: KiteClass Dev Error Overlay on All Pages

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend / Developer Experience
**Found:** 2026-04-16 (UI audit)
**Affects:** All 37 KiteClass pages

## Problem

"2 errors" hoặc "5 errors" badge hiển thị bottom-left trên MỌI page trong dev mode. Đây là Next.js/Vite error overlay báo console errors. Nếu visible trong production build → ảnh hưởng user trust nghiêm trọng.

## Root Cause

Cần investigate:
1. Console errors từ failed API calls (expected khi không có backend)?
2. React hydration mismatches?
3. Missing environment variables?
4. Third-party script errors?

## Proposed Fix

1. Investigate console errors: `page.on('console', msg => ...)` trong capture script
2. Fix root cause errors (likely API call failures cần try-catch)
3. Verify production build (`next build && next start`) không show overlay
4. Add error boundary wrappers nếu cần

## Acceptance Criteria

- [ ] Dev mode: ≤0 errors hiển thị (hoặc chỉ expected API 401s)
- [ ] Production build: 0 error overlays
- [ ] Screenshots: không có error badge trên bất kỳ page nào
