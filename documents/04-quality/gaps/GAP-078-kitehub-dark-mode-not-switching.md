# GAP-078: KiteHub Dark Mode Not Switching Visually

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend / Theming
**Found:** 2026-04-16 (UI audit)
**Affects:** All 24 KiteHub pages

## Problem

Capture script inject `class="dark"` + `style.colorScheme = 'dark'` lên `<html>`, nhưng dark screenshots vẫn visually identical với light mode. Hero gradients, backgrounds, text colors không thay đổi.

File sizes dark vs light chênh ~1KB (1221KB vs 1222KB) → gần như identical.

## Root Cause Hypotheses

1. `next-themes` override lại class sau hydration (remove `dark` class)
2. Tailwind dark mode chưa enabled trong `tailwind.config.ts` (`darkMode: 'class'`)
3. CSS không dùng `dark:` variants — chỉ hardcode light colors
4. `next-themes` dùng attribute khác (data-theme thay vì class)

## Proposed Fix

1. Check `tailwind.config.ts` — xác nhận `darkMode: 'class'`
2. Check `ThemeProvider` config trong layout — `attribute`, `defaultTheme`, `enableSystem`
3. Grep cho `dark:` variants trong CSS/components — nếu không có → dark mode chưa implemented
4. Nếu chưa implement → thêm `dark:` variants cho key components (nav, cards, backgrounds)

## Acceptance Criteria

- [ ] Dark screenshots visually khác light (dark backgrounds, light text)
- [ ] `tailwind.config.ts` có `darkMode: 'class'`
- [ ] Key pages (landing, login, dashboard) có `dark:` CSS variants
- [ ] File size difference giữa light/dark >10% (indicating different rendering)
