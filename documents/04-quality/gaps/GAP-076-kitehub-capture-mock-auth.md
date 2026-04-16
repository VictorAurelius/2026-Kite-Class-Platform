# GAP-076: KiteHub Capture Script Mock Auth Not Working

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** DevOps / Screenshot Capture
**Found:** 2026-04-16 (UI audit)
**Affects:** KiteHub dashboard + admin pages (16/24 pages)

## Problem

KiteHub capture script injects mock auth via `localStorage.setItem('kitehub-auth', ...)` + `page.evaluate()`, nhưng 10/16 dashboard pages vẫn show:
- Login redirect (dashboard, admin)
- Error states (settings, billing, billing-upgrade)
- Loading spinners (instance-detail, branding, branding-wizard, billing-payment)

KiteClass capture script dùng MSW mock API routes (`setupMockApi(page)`) → dashboard pages render đầy đủ với mock data. KiteHub thiếu tương đương.

## Root Cause

KiteHub capture script chỉ inject auth state vào localStorage. Không có mock API responses → API calls fail → pages show error/loading. Khác với KiteClass có `mock-api-routes.ts` intercepting Playwright routes.

## Proposed Fix

1. Tạo `kitehub/kitehub-frontend/scripts/mock-api-routes.ts` tương tự KiteClass
2. Mock responses cho: instances list, billing data, branding assets, admin data
3. Import và setup trong capture script: `await setupMockApi(page)`

## Acceptance Criteria

- [ ] Dashboard page renders với mock stats + instance list
- [ ] Admin page renders với mock admin dashboard
- [ ] Billing-history shows mock transactions
- [ ] Branding-templates shows mock template cards
- [ ] All 24 pages capture ≥50KB (không có blank/error shells)

## Related

- GAP-014 (mock data for AI branding)
- GAP-032 (branded error pages)
- UI audit 2026-04-16: KiteHub dashboard avg 36-62/128
