# UI Review — KiteClass Frontend

**Ngày:** 2026-04-04
**Phiên bản:** main @ after PR #256–#259 + globals.css dark mode compile fix
**Phương pháp:** Screenshot thực tế (Playwright) — 144 PNGs, 36 trang × 2 themes × 2 viewports
**Screenshots:** `documents/screenshots/after-pr-259-darkfix/`
**Previous review:** `audit-2026-04-03` (baseline)
**Next review:** Sau khi có backend data thực

---

## Fix Verification (so với audit 2026-04-03)

| Issue | Status | Notes |
|-------|--------|-------|
| P0 — Dark mode broken | FIXED | `.dark {}` outside `@layer base` — compile bug Tailwind 3.4.17 + Next.js 15 |
| P1 — i18n auth pages (100% English) | FIXED | login, forgot-password, reset-password, auth sidebar — 100% tiếng Việt |
| P2 — Catalog spinner vô hạn | FIXED | `retry: 1` — max ~22s trước khi error state |
| P2 — Date input mm/dd/yyyy | FIXED | Hint "Định dạng: ngày/tháng/năm" |
| P3 — Landing sections rỗng | FIXED | Teachers, Certificates, Enrollment, Pricing sections |
| P4 — ARIA live regions | FIXED | `aria-live="polite"` trên FormInput/Select/Textarea errors |

**Root cause phát hiện mới:** `.dark { --background: ... }` bên trong `@layer base` bị Tailwind compiler drop hoàn toàn (confirmed Playwright CSS scan). Fix: move ra ngoài `@layer`.

---

## Before/After Comparison

| Screen | Before | After | Delta |
|--------|--------|-------|-------|
| Landing | 76/128 | 89/128 | +13 |
| Login | 82/128 | 93/128 | +11 |
| Register | 84/128 | 93/128 | +9 |
| Register-student | 84/128 | 89/128 | +5 |
| Forgot-password | ~80/128 | 88/128 | +8 |
| Reset-password | ~78/128 | 87/128 | +9 |
| About | ~75/128 | 87/128 | +12 |
| Catalog | ~70/128 | 78/128 | +8 |
| Contact | ~76/128 | 85/128 | +9 |

---

## Scores per Screen (/128)

### Landing `/`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 17/20 | Dark mode confirmed (navy bg). Responsive + SSR OK |
| Design Heuristics | 23/40 | Sections filled: teachers, certs, enrollment, pricing. Placeholder contact data |
| Visual Aesthetics | 21/28 | Dark mode polished. New sections with real content |
| User Friendliness | 15/20 | Complete product picture. New sections build trust |
| WCAG | 13/20 | Contrast OK light + dark |
| **Total** | **89/128** | |

### Login `/login`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 16/20 | Dark mode confirmed (dark navy form area) |
| Design Heuristics | 27/40 | Split layout, error states, forgot pwd. 100% Vietnamese |
| Visual Aesthetics | 20/28 | Light: blue/white. Dark: blue/navy. Both polished |
| User Friendliness | 16/20 | "Chao mung tro lai", "Dang nhap" — all Vietnamese |
| WCAG | 14/20 | `aria-live="polite"` on field errors. Contrast both themes |
| **Total** | **93/128** | |

### Register `/register`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 16/20 | Dark mode confirmed |
| Design Heuristics | 27/40 | Account type selection. "Trung tam" disabled = coming soon |
| Visual Aesthetics | 21/28 | Card selection clean both themes |
| User Friendliness | 15/20 | Vietnamese. Auth sidebar Vietnamese (fixed PR #256) |
| WCAG | 14/20 | |
| **Total** | **93/128** | |

### Register-student `/register/student`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 16/20 | Dark mode confirmed |
| Design Heuristics | 26/40 | Full form, password hint |
| Visual Aesthetics | 19/28 | Long form, dark mode clean |
| User Friendliness | 15/20 | Date hint "Dinh dang: ngay/thang/nam". All Vietnamese |
| WCAG | 13/20 | Required markers, labels visible |
| **Total** | **89/128** | |

### Forgot-password `/forgot-password`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 16/20 | Dark mode confirmed |
| Design Heuristics | 24/40 | "Quen mat khau?" focused form |
| Visual Aesthetics | 19/28 | Clean dark/light |
| User Friendliness | 15/20 | 100% Vietnamese |
| WCAG | 14/20 | |
| **Total** | **88/128** | |

### Reset-password `/reset-password`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 16/20 | Dark mode confirmed. Invalid token state renders correctly |
| Design Heuristics | 22/40 | "Lien ket khong hop le" error clear |
| Visual Aesthetics | 19/28 | Error state clean in dark mode |
| User Friendliness | 15/20 | 100% Vietnamese |
| WCAG | 15/20 | `role="alert"` + `aria-live="polite"` both present |
| **Total** | **87/128** | |

### About `/about`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 17/20 | Dark mode clearly visible (dark navy). Responsive |
| Design Heuristics | 24/40 | Stats section, feature cards, timeline |
| Visual Aesthetics | 22/28 | Dark mode polished. Timeline clean |
| User Friendliness | 13/20 | Good content density |
| WCAG | 11/20 | |
| **Total** | **87/128** | |

### Catalog `/catalog`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 15/20 | Dark mode confirmed. retry:1 applied |
| Design Heuristics | 18/40 | Loading spinner visible. CTA always shows |
| Visual Aesthetics | 18/28 | Filters clean. Dark mode spinner visible |
| User Friendliness | 14/20 | Error after ~22s (improved from ~47s) |
| WCAG | 13/20 | |
| **Total** | **78/128** | |

### Contact `/contact`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 17/20 | Dark mode confirmed |
| Design Heuristics | 23/40 | Form, contact info |
| Visual Aesthetics | 20/28 | Clean both themes |
| User Friendliness | 14/20 | |
| WCAG | 11/20 | |
| **Total** | **85/128** | |

---

## Overall Summary

| Metric | Before | After | Delta |
|--------|--------|-------|-------|
| Lowest screen | 70/128 (catalog) | 78/128 (catalog) | +8 |
| Average public pages (9) | ~78/128 | 88/128 | +10 |
| Auth pages avg | ~82/128 | 90/128 | +8 |

---

## Remaining Issues

| Priority | Issue |
|----------|-------|
| Content | Placeholder: "1900 xxxx", "support@kiteclass.com" |
| Backend | Catalog spinner to error (expected without backend) |
| Nice-to-have | ARIA landmarks (`<main>`, `<nav>`, `<header>`) |
| Nice-to-have | Keyboard navigation audit |

---

## Dark Mode Compile Bug Details

**Symptom:** `.dark { --background: ... }` absent from compiled CSS.
**Root cause:** Tailwind 3.4.17 + Next.js 15.1.6 drop `.dark {}` block inside `@layer base` containing only CSS custom properties (no Tailwind utilities).
**Fix:** Move `.dark {}` outside `@layer base` in `globals.css`.
**Verified:** `getComputedStyle(body).backgroundColor` = `rgb(2, 8, 23)` in dark mode.
