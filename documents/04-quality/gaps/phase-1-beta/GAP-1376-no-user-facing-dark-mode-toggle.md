# GAP-1376: Không có user-facing light/dark theme toggle dù darkMode:['class'] + dark: variants

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-14 (UI review full audit, AUDIT-2026-06-14-ui-review-full)
**Affects:** `kitehub/kitehub-frontend/src/**` + `kiteclass/kiteclass-frontend/src/**`

## Problem

Cả 2 frontend khai báo `darkMode: ['class']` trong `tailwind.config.ts` và dùng `dark:` variants dày đặc (vd admin dashboard `dark:bg-green-950/30`, KC dùng token `bg-card`/`text-muted-foreground`). Nhưng **0 user-facing toggle control**: grep `setTheme('dark'|'light'|'system')` / `toggleTheme()` / Sun-Moon switcher = 0 call-site (trừ test files).

Hệ quả: dark-mode CSS chỉ reachable qua (a) OS-preference nếu provider config `defaultTheme="system"`, hoặc (b) tenant-inject color (KC `ThemeReceiver`). User KHÔNG có cách chủ động bật/tắt dark mode. → công sức viết `dark:` variants effectively dead/unverified styling; trong audit này trục dark-mode rendering không verify được (không có toggle để test).

## Root Cause

ThemeProvider (KH) + NextThemesProvider (KC) tồn tại nhưng UI toggle control chưa được build/wire. Dark variants được viết "đề phòng" mà chưa có entry-point cho user.

## Proposed Fix

Quyết định 1 trong 2 hướng (cần product decision):
- **A.** Build `ThemeToggle` component (sun/moon, `setTheme`) + wire vào header/settings của cả 2 app → dark mode reachable + verifiable.
- **B.** Nếu dark mode KHÔNG trong scope Phase 1 → document quyết định + cân nhắc strip `dark:` variants để giảm dead code (hoặc giữ + ghi rõ "chờ Phase 2 toggle").

## Acceptance Criteria

- [ ] Quyết định A hoặc B được record (ADR hoặc gap note)
- [ ] Nếu A: toggle control tồn tại + `setTheme` wired + dark mode verify được trên ≥1 screen
- [ ] Nếu B: provider `defaultTheme` documented + dead-variant policy ghi rõ

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, P3)
- `kitehub/kitehub-frontend/tailwind.config.ts` + `kiteclass/kiteclass-frontend/tailwind.config.ts` (darkMode:['class'])
- Providers: `kitehub/.../providers/ThemeProvider.tsx`, `kiteclass/.../providers/NextThemesProvider.tsx`
