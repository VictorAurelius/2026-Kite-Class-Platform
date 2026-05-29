---
audience: dev
---

# GAP-807 — Theme đã lưu của tenant KHÔNG apply lên dashboard (2 hệ theme tách rời)

**Status:** 🟡 PARTIAL (fix shipped — BrandingThemeApplier wire dashboard; live-walk deferred stack down)
**Priority:** 🟠 P1 (demo-blocker — tính năng tùy biến UI cốt lõi)
**Domain:** Frontend (KiteClass)
**Found:** 2026-05-29 (user-flagged: "dashboard đã có đổi theme nhưng chưa apply đúng theme chọn" → theme-apply investigation agent)
**Phase:** phase-1-beta
**Affects:** Owner đổi branding màu/theme → reload dashboard → KHÔNG thấy theme custom (vẫn default). Phá chứng minh tùy biến UI (GAP-805 demo).

## Problem

KiteClass FE có **2 hệ theme tách rời, không nối nhau**:

| Hệ | Cơ chế | Wrap đâu |
|---|---|---|
| **A — `BrandingProvider`** | fetch tenant branding (`usePublicBranding` → `GET public-branding`) → `applyCssVars()` set `--primary`/`--accent`/`--brand-*` lên `documentElement` (THẬT) | CHỈ `app/(auth)/layout.tsx:21` (login/register) — KHÔNG wrap dashboard |
| **B — `ThemeContext`** | load từ `localStorage` key `kiteclass_theme` (`loadThemeFromStorage`), KHÔNG fetch API branding | provider DUY NHẤT wrap dashboard (`app/layout.tsx:44`) |

**Điểm đứt:**
- `app/(dashboard)/layout.tsx` — KHÔNG mount `BrandingProvider`, KHÔNG fetch `GET /api/v1/settings/branding`, KHÔNG gọi `applyCssVars/applyThemeVariables`. Chỉ check auth + CommandPalette.
- `components/settings/branding-settings.tsx:67-69` — lưu màu qua `useUpdateBranding` → `PUT /settings/branding`. Sau lưu KHÔNG ai fetch lại + apply CSS vars lên dashboard live.
- `contexts/ThemeContext.tsx:54-79,148` — nguồn theme dashboard = localStorage, KHÔNG sync branding DB tenant.

→ Theme chọn LƯU vào DB nhưng dashboard render bằng default CSS vars (`globals.css:14` `--primary: 221.2 83.2%`) hoặc localStorage cũ. Đúng triệu chứng user.

## Root Cause

Dashboard route group không wire persisted-branding-apply. `BrandingProvider` (cơ chế fetch+apply duy nhất) chỉ phục vụ auth pages; dashboard dùng `ThemeContext` localStorage-only độc lập với branding DB. Hai hệ không nối.

## Test gap

Test hiện chỉ cover **preview** (`ThemePreviewPanel.test`, `themeReceiver.test` postMessage) + **localStorage** (`ThemeContext.test`, `utils.test`). **KHÔNG test nào** verify "branding đã lưu → fetch `GET settings/branding` → apply CSS vars lên dashboard khi load". `grep useBranding/brandingApi *.test.*` = rỗng. Persisted-apply path zero coverage.

## Proposed Fix

- Mount BrandingProvider-equivalent ở `(dashboard)/layout.tsx`: dùng `useBranding` → `GET /settings/branding` → apply CSS vars on mount.
- Sau `useUpdateBranding` success → re-apply CSS vars (invalidate query đã có, thiếu re-apply step).
- Reconcile 2 hệ: ThemeContext nên seed từ branding DB (không chỉ localStorage), HOẶC dashboard dùng BrandingProvider thay ThemeContext cho tenant theme.
- Test: persisted-theme-apply (fetch branding mock → assert `documentElement` CSS vars set đúng màu đã lưu).

## Acceptance Criteria

- [x] `(dashboard)/layout.tsx` fetch + apply persisted tenant branding CSS vars on mount — `BrandingThemeApplier` (useBranding → applyBrandColorVars on mount)
- [x] Sau update branding → dashboard re-apply không cần hard reload — branding-settings.tsx onSuccess re-apply
- [x] Test cover persisted-apply path — `BrandingThemeApplier.test.tsx` (mock useBranding cam #F97316 → assert CSS vars ≠ default blue); vitest 2/2 PASS + build PASS
- [ ] Live-walk: owner đổi màu → save → reload dashboard → thấy màu custom (per `feature-ship-runtime-walk-mandate.md`) — **DEFERRED stack down**

## Related

- **GAP-805** — demo tenant tùy biến UI; GAP-807 là **prerequisite** (demo theme không hiện nếu chưa fix)
- **GAP-804** — branding logo upload (cùng branding surface, khác bug)
- GAP-078 (closed) — kitehub dark-mode not switching (cùng class theme-not-applying)
- `BrandingProvider.tsx` / `ThemeContext.tsx` / `(dashboard)/layout.tsx` — files liên quan

## Log

- **2026-05-29 (PARTIAL — fix shipped):** Opus agent fix (worktree). New `components/theme/BrandingThemeApplier.tsx` (render null, side-effect) mount trong `(dashboard)/layout.tsx` sau auth gate → `useBranding` (authenticated `GET /settings/branding`) → `applyBrandColorVars` on mount + on branding change. Extract `applyBrandColorVars` + `hexToHslString` từ `BrandingProvider.applyCssVars` (reuse, no behavior change auth pages). Set `--primary`/`--accent` (Shadcn HSL) + `--brand-*` (hex). Tách rõ: brand-color (DB) ≠ light/dark (`NextThemesProvider` `.dark`) ≠ `--theme-*` RGB (ThemeContext localStorage) ≠ postMessage preview — chỉ đụng brand-color vars, không phá 3 hệ kia. branding-settings.tsx onSuccess re-apply (đổi màu thấy ngay). Test `BrandingThemeApplier.test.tsx` (cam #F97316 → assert vars ≠ default blue + no-data no-op), vitest 2/2 PASS, `pnpm build` PASS, eslint 0. Live-walk DEFER stack down. Bộ 3 demo tùy biến UI: GAP-805 (seed branding) + GAP-804 (logo upload) + GAP-807 (theme apply) đủ.
- **2026-05-29:** Filed từ user-flagged observation + theme-apply investigation agent (read-only trace). Verdict bug thật: 2 hệ theme (BrandingProvider fetch+apply chỉ wrap auth; ThemeContext localStorage-only wrap dashboard) không nối → persisted theme không apply lên dashboard. Zero test cho persisted-apply path. Demo-blocker P1 — ưu tiên trước GAP-805 live-walk.
