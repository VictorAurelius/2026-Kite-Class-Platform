# GAP-1376: Không có user-facing light/dark theme toggle dù darkMode:['class'] + dark: variants

**Status:** 🟡 PARTIAL
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

- [x] Quyết định A hoặc B được record → **Decision B (defer toggle to Phase 2)**
- [ ] Nếu A: toggle control tồn tại + `setTheme` wired + dark mode verify được trên ≥1 screen — **DEFER Phase 2** (feature-scale, ngoài scope a11y batch này)
- [x] Nếu B: provider `defaultTheme` documented + dead-variant policy ghi rõ

## Resolution (PARTIAL — Decision B)

**Decision recorded:** 2026-06-15 (branch `fix/audit-fixH-ui-2026-06-14`)

Đây là **feature-scale** (toggle component + wire vào header/settings của CẢ 2 app + verify cross-app + mounted-guard tránh hydration mismatch). Trong batch a11y này (P3, "don't over-build") → chọn **Decision B: defer build sang Phase 2**, document hiện trạng provider:

- **KH** `providers/ThemeProvider.tsx`: `next-themes` với `attribute="class"`, `defaultTheme="light"`, `enableSystem` → dark mode reachable qua OS preference (system). localStorage persistence + `class` trên `<html>` đã do `next-themes` quản lý sẵn.
- **KC** `providers/NextThemesProvider.tsx`: `next-themes` tương tự; tenant brand color qua `ThemeReceiver`/`BrandingThemeApplier`.
- **Dead-variant policy:** giữ `dark:` variants (KHÔNG strip) — provider đã hỗ trợ `enableSystem` nên không phải dead code hoàn toàn (OS-dark users vẫn thấy), + sẵn sàng cho Phase 2 toggle. Build `ThemeToggle` (`useTheme().setTheme` + sun/moon + mounted-guard) khi Phase 2 mở scope theme UX.

**Còn lại (PARTIAL):** user-facing toggle control (Decision A) defer Phase 2.

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, P3)
- `kitehub/kitehub-frontend/tailwind.config.ts` + `kiteclass/kiteclass-frontend/tailwind.config.ts` (darkMode:['class'])
- Providers: `kitehub/.../providers/ThemeProvider.tsx`, `kiteclass/.../providers/NextThemesProvider.tsx`
