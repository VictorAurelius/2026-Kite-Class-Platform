# GAP-078: KiteHub Dark Mode Not Switching Visually

**Status:** ✅ DONE
**Priority:** 🟠 P1
**Domain:** Frontend / Theming
**Found:** 2026-04-16 (UI audit)
**Affects:** All 24 KiteHub pages
**Fixed:** 2026-04-16

## Problem

Capture script inject `class="dark"` + `style.colorScheme = 'dark'` lên `<html>`, nhưng dark screenshots vẫn visually identical với light mode. Hero gradients, backgrounds, text colors không thay đổi.

File sizes dark vs light chênh ~1KB (1221KB vs 1222KB) → gần như identical.

## Root Cause (Confirmed)

Dark mode infrastructure was fully in place:
- `tailwind.config.ts`: `darkMode: ['class']` — OK
- `ThemeProvider`: `attribute="class"` — OK
- `globals.css`: Both `:root` and `.dark` CSS variables defined — OK

The real issue was **hardcoded colors without `dark:` variants** across 18+ files:
- `bg-white` on toggle switches without dark equivalent
- `text-gray-900`, `text-gray-600`, `bg-gray-100` without dark variants (blog pages)
- `bg-green-100`, `bg-blue-100` accent colors without dark variants
- `text-green-600`, `text-blue-600` status/icon colors without dark variants
- `border-gray-200` without dark variant

Layout components (Sidebar, DashboardLayout, PublicLayout, AdminLayout) already used CSS variables (`bg-background`, `text-foreground`, `bg-card`, etc.) which auto-switch. The problem was localized to specific UI elements using hardcoded Tailwind color classes.

## Fix Applied

Added `dark:` variants across 18 files:

1. **StatusBadge.tsx** — fallback now includes `dark:bg-gray-900 dark:text-gray-200`
2. **CurrentPlanCard.tsx** — tier icon colors + text-gray-600 → dark variants
3. **AdminInstancesTable.tsx** — tier color config + activate button
4. **admin/instances/[id]/page.tsx** — tier color config
5. **PlanComparison.tsx** — toggle switch `bg-white` → `dark:bg-foreground`, check icon
6. **pricing/page.tsx** — toggle switch, green badge
7. **blog/page.tsx** — replaced `text-gray-900/600/500`, `border-gray-200` with CSS variables (`text-foreground`, `text-muted-foreground`, `border-border`, `bg-muted`)
8. **blog/[slug]/page.tsx** — same treatment + bottom border/link
9. **admin/page.tsx** — stat cards bg colors, pending/new text colors
10. **PaymentStatusCard.tsx** — all 4 status icon colors
11. **ChangeConfirmation.tsx** — upgrade/downgrade arrow colors
12. **TierSelector.tsx** — check icon + change indicator colors
13. **PaymentInfo.tsx** — copy confirmation check icons
14. **Landing page (page.tsx)** — floating notification icons (green/purple)
15. **QRCodeDisplay.tsx** — kept `bg-white` intentionally for QR readability

## Acceptance Criteria

- [x] Dark screenshots visually khác light (dark backgrounds, light text)
- [x] `tailwind.config.ts` có `darkMode: 'class'`
- [x] Key pages (landing, login, dashboard) có `dark:` CSS variants
- [x] File size difference giữa light/dark >10% (indicating different rendering)
