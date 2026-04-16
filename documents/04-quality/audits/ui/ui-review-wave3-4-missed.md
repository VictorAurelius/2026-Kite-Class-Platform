# UI Review — Missed FE Pages (Wave 3 Sub-PR 3.7 + Wave 4 Sub-PR 4.3)

**Date:** 2026-04-16
**Version:** main @ `49802f5d` (post-Wave 4)
**Method:** Targeted Playwright capture — 12 PNGs, 3 pages × 2 themes × 2 viewports
**Screenshots:** `documents/screenshots/kitehub-wave3-4-missed/`
**Previous review:** `ui-review-latest.md` 2026-04-13 (KiteClass full audit)
**Reason:** These 2 FE PRs shipped without UI audit — gap detected by user, remediated now.

---

## Pages Audited

| # | Page | App | PR | Route | Status |
|---|------|-----|----|-------|--------|
| 1 | DMCA Takedown Notice | KiteHub | #295 (Sub-PR 4.3) | `(public)/legal/dmca` | Scored |
| 2 | Branding Wizard | KiteHub | #290 (Sub-PR 3.7) | `(customer)/branding/wizard` | Scored (scaffold — no backend) |
| 3 | Branding Wizard | KiteClass | #290 (Sub-PR 3.7) | `(dashboard)/branding/wizard` | NOT SCORABLE (auth injection failed) |

---

## 1. KiteHub DMCA Page — `/legal/dmca` — **76/128**

**Screenshots:** `kitehub-wave3-4-missed/legal-dmca/`

### Technical (10/20)

| Criterion | Score | Notes |
|-----------|:-----:|-------|
| Responsive | 3/4 | Mobile stacks correctly, form usable at 375px |
| Dark mode | 0/4 | **BROKEN** — page body, nav, card all stay light-bg; only `<input>` fields get dark background |
| Theming | 3/4 | Inherits KiteHub public page style correctly (nav, footer, CTA button) |
| Anti-patterns | 2/4 | Clean semantic `<form>`, labels + inputs. No JS framework oddities |
| Loading/states | 2/4 | Form ready on load (good). No loading spinner needed |

### Design Heuristics (23/40)

| # | Heuristic | Score | Notes |
|---|-----------|:-----:|-------|
| 1 | System status | 2/4 | Required markers (*) present, no real-time validation |
| 2 | Real-world match | 3/4 | Legal language ("§512") appropriate, mixed Vi/En OK for legal context |
| 3 | User control | 2/4 | No reset/clear/cancel, submit only |
| 4 | Consistency | 3/4 | Matches other KiteHub public pages (nav, footer, palette) |
| 5 | Error prevention | 2/4 | Required fields marked, placeholder for URL. No client-side validation visible |
| 6 | Recognition | 3/4 | Placeholder "https://tenant.kiteclass.vn/..." helpful, "Before you submit" box excellent |
| 7 | Flexibility | 1/4 | Basic HTML form — no drag-drop, no auto-save, no file attachment |
| 8 | Aesthetic minimal | 3/4 | Focused, not overloaded, appropriate density |
| 9 | Error recovery | 1/4 | No error states visible (requires backend) |
| 10 | Help | 3/4 | "Before you submit" info box with 4 guidance points — well done |

### Visual Aesthetics (16/28)

| Criterion | Score | Notes |
|-----------|:-----:|-------|
| Color harmony | 3/4 | Blue primary consistent with KiteHub palette |
| Typography | 3/4 | Clear hierarchy: h1 bold → body → labels → placeholders |
| Spacing | 2/4 | Form section vertically compact; could use more padding between fields |
| Visual hierarchy | 3/4 | Heading → intro → info box → form → CTA — clear flow |
| Polish | 2/4 | Info box has nice border. Form area lacks card/shadow wrapper — basic Tailwind |
| Animation | 1/4 | No transitions visible |
| Detail | 2/4 | Clean but standard |

### User Friendliness (14/20)

| Criterion | Score | Notes |
|-----------|:-----:|-------|
| First impression | 3/4 | Clear purpose immediately |
| Navigation | 3/4 | Consistent nav + footer |
| Action clarity | 3/4 | Single blue CTA "Submit DMCA notice" |
| Onboarding | 3/4 | Info box guides first-time user well |
| Efficiency | 2/4 | No auto-fill, no file attachment for evidence |

### WCAG (13/20)

| Criterion | Score | Notes |
|-----------|:-----:|-------|
| Contrast | 3/4 | Light mode passes. Dark mode fails (light body vs dark inputs) |
| Touch targets | 3/4 | Inputs standard size, button adequate |
| Labels | 3/4 | All fields labeled with required markers |
| Screen reader | 2/4 | Semantic form. Missing aria-describedby for potential errors |
| Keyboard | 2/4 | Standard tab order expected |

### Issues Found

| Priority | Issue | Impact |
|----------|-------|--------|
| **P0** | Dark mode broken — page body stays white, only `<input>` get dark bg | Readability on dark theme |
| P1 | No client-side form validation | User submits bad data, waits for server error |
| P2 | No file/evidence upload capability | User can't attach screenshots proving infringement |
| P2 | "Before you submit" card could use an icon or color accent | Visual weight vs plain text |
| P2 | Missing breadcrumbs (Legal > DMCA) | Navigation path unclear |

---

## 2. KiteHub Branding Wizard — `/branding/wizard` — **48/128**

**Screenshots:** `kitehub-wave3-4-missed/branding-wizard/`

**Note:** Wizard content area shows only loading spinner because it depends on backend API. Score reflects **scaffold state**, not bugs. Fair evaluation requires backend running.

### Technical (8/20)

| Criterion | Score | Notes |
|-----------|:-----:|-------|
| Responsive | 2/4 | Sidebar renders on mobile but header text overlaps ("Quản lý trung tâm" + user email + "Đăng xuất" cramped) |
| Dark mode | 0/4 | **BROKEN** — sidebar stays light in dark mode, identical to light mode |
| Theming | 2/4 | Matches customer dashboard sidebar style |
| Anti-patterns | 2/4 | No error boundary for failed API load |
| Loading state | 2/4 | Spinner present (correct for no-backend) |

### Design Heuristics (14/40)

| # | Heuristic | Score | Notes |
|---|-----------|:-----:|-------|
| 1 | System status | 2/4 | Spinner indicates loading |
| 2 | Real-world | 2/4 | Menu items (Tổng quan, AI Branding) clear |
| 3 | User control | 1/4 | Can navigate sidebar, but stuck on spinner |
| 4 | Consistency | 3/4 | Sidebar style matches other customer pages |
| 5 | Error prevention | 1/4 | N/A |
| 6 | Recognition | 2/4 | Active menu item highlighted in blue |
| 7 | Flexibility | 1/4 | N/A |
| 8 | Aesthetic | 2/4 | Sidebar clean, content empty |
| 9 | Error recovery | 0/4 | No error boundary — infinite spinner on failed load |
| 10 | Help | 0/4 | No help text or empty-state guidance |

### Visual Aesthetics (12/28)

| Criterion | Score | Notes |
|-----------|:-----:|-------|
| Color | 3/4 | Blue active-state highlight consistent |
| Typography | 2/4 | Sidebar text readable |
| Spacing | 2/4 | Sidebar padding adequate |
| Hierarchy | 2/4 | Limited — only sidebar visible |
| Polish | 1/4 | Empty content area |
| Animation | 1/4 | Spinner animation present |
| Detail | 1/4 | Minimal |

### User Friendliness (6/20)

| Criterion | Score | Notes |
|-----------|:-----:|-------|
| First impression | 1/4 | Empty page with spinner — discouraging |
| Navigation | 3/4 | Sidebar works, items clickable |
| Action clarity | 1/4 | Nothing actionable visible |
| Onboarding | 0/4 | No guidance text while loading |
| Efficiency | 1/4 | N/A |

### WCAG (8/20)

| Criterion | Score | Notes |
|-----------|:-----:|-------|
| Contrast | 2/4 | Sidebar OK |
| Touch targets | 2/4 | Sidebar links adequate |
| Labels | 2/4 | Menu items labeled |
| Screen reader | 1/4 | Nav structure exists |
| Keyboard | 1/4 | Sidebar should be focusable |

### Issues Found

| Priority | Issue | Impact |
|----------|-------|--------|
| **P0** | Dark mode broken — sidebar stays light | Visual consistency |
| **P0** | No error boundary — infinite spinner if API fails | User stuck forever |
| P1 | Mobile header text overlap (email + nav cramped) | Unusable on small screens |
| P1 | No empty-state/skeleton while loading | No context for what's loading |
| P2 | Sidebar should collapse on mobile (hamburger) | Screen real estate wasted |

---

## 3. KiteClass Branding Wizard — NOT SCORABLE

**Screenshots:** `kitehub-wave3-4-missed/kiteclass-branding-wizard/`

Mock Zustand auth injection uses `kitehub-auth` key but KiteClass uses a different auth store (`kiteclass-auth`). Result:
- **Light mode:** Full-page loading spinner — no dashboard layout renders
- **Dark mode:** Redirected to login page — wizard never reached

**Action required:** Update `capture-targeted.ts` (and main capture script) to inject KiteClass-specific auth token. Until then, KiteClass dashboard pages including wizard cannot be audited via Playwright.

---

## Summary

| Page | Score | Grade | Key Issue |
|------|:-----:|:-----:|-----------|
| KiteHub DMCA | **76/128** | C+ | Dark mode broken, no client validation |
| KiteHub Wizard | **48/128** | D | Scaffold only (needs backend), dark mode broken |
| KiteClass Wizard | **N/A** | — | Auth injection failed |

### Cross-cutting Issues

1. **Dark mode broken across ALL new pages** — both DMCA and Wizard stay light-themed regardless of `colorScheme: 'dark'`. Likely missing `dark:` Tailwind classes or not inheriting from the theme provider correctly.
2. **No error boundary** — wizard shows infinite spinner when API unavailable instead of a helpful empty state.
3. **KiteClass auth injection** — capture scripts use KiteHub auth store key; KiteClass uses different store structure.

### Recommendations

| # | Action | Score Impact | Effort |
|---|--------|:-----------:|--------|
| 1 | Fix dark mode on DMCA page (add `dark:bg-gray-900`, `dark:text-white` etc.) | DMCA +8 → ~84 | 1h |
| 2 | Fix dark mode on Wizard sidebar + content area | Wizard +8 → ~56 | 1h |
| 3 | Add error boundary + empty state for Wizard loading | Wizard +10 → ~58 | 2h |
| 4 | Add client-side validation to DMCA form | DMCA +4 → ~80 | 1h |
| 5 | Fix KiteClass auth injection in capture script | Enables KC audit | 30m |
| 6 | Add mobile hamburger for sidebar on wizard | Wizard +4 | 2h |

---

## Process Notes

- **First targeted UI audit** on this repo (previous audits were full-sweep)
- **MSYS_NO_PATHCONV=1** required when passing URL paths as CLI args on Windows Git Bash (Git Bash auto-converts `/legal/dmca` → `C:/Program Files/Git/legal/dmca`)
- **Port 3000 trap** — KiteClass backend Tomcat occupies 3000, so FE dev must use 3002
- **Playwright chromium** needed manual install after fresh pnpm
- Screenshot folders added to capture scripts: `legal-dmca` (kitehub) + `branding-wizard` (both apps)

---

## Log

- 2026-04-16 — Targeted audit for 2 missed FE PRs (#290 wizard, #295 DMCA). Hook added to prevent future misses.
