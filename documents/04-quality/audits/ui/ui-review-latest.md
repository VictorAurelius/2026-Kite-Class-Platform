# UI Review — KiteHub + KiteClass Frontend (Final)

**Date:** 2026-04-16
**Version:** main @ `b432a593` (post-Wave 4, post-skills-cleanup)
**Method:** Playwright automated screenshots + subagent parallel scoring
**Screenshots:** KiteHub 96/96 OK, KiteClass 147/148 OK (1 timeout)
**Manifests:** `documents/screenshots/kitehub-latest/`, `documents/screenshots/kiteclass-latest/`
**Previous review:** 2026-04-16 (invalid — CSS not loading, wrong port)
**Scoring method:** 4 subagents parallel (new context-efficient process)

---

## Capture Fixes Applied This Session

| Fix | PR | Status |
|-----|-----|--------|
| `waitUntil: 'networkidle'` (CSS loading) | #305 | Merged |
| Dark mode class injection (`classList.add('dark')`) | #305 | Merged |
| Dev ports 3000/3001 → 4700/4701 | #306 | Merged |
| Screenshots folder `latest/` → `kiteclass-latest/` | #307 | Merged |
| Skills cleanup + frontmatter + refs | #308 | Merged |

---

## KiteHub Scores (/128)

### Public + Auth Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Landing | 14 | 30 | 22 | 15 | 14 | **95** |
| Pricing | 14 | 32 | 22 | 16 | 14 | **98** |
| Blog | 12 | 26 | 18 | 13 | 13 | **82** |
| Blog-detail | 4 | 10 | 8 | 6 | 8 | **36** |
| Legal-DMCA | 13 | 28 | 20 | 14 | 14 | **89** |
| Login | 14 | 30 | 22 | 16 | 14 | **96** |
| Register | 14 | 30 | 22 | 16 | 14 | **96** |
| Verify-email | 13 | 28 | 20 | 14 | 13 | **88** |

### Customer Dashboard Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Dashboard | 7 | 12 | 10 | 6 | 8 | **43** |
| Billing-history | 10 | 18 | 14 | 10 | 10 | **62** |
| Branding-assets | 11 | 20 | 16 | 11 | 10 | **68** |
| Branding-templates | 9 | 16 | 13 | 9 | 9 | **56** |
| Instance-detail | 6 | 8 | 8 | 4 | 7 | **33** |
| Settings | 7 | 10 | 9 | 5 | 8 | **39** |
| Billing | 7 | 10 | 9 | 5 | 8 | **39** |
| Billing-upgrade | 7 | 10 | 9 | 5 | 8 | **39** |
| Billing-payment | 6 | 8 | 8 | 4 | 7 | **33** |
| Branding | 6 | 8 | 8 | 4 | 7 | **33** |
| Branding-wizard | 6 | 8 | 8 | 4 | 7 | **33** |

### Admin Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Admin | 7 | 12 | 10 | 6 | 8 | **43** |
| Admin-instances | 8 | 14 | 12 | 8 | 9 | **51** |
| Admin-instance-detail | 8 | 12 | 10 | 7 | 8 | **45** |
| Admin-payments | 9 | 14 | 12 | 8 | 9 | **52** |
| Admin-revenue | 11 | 20 | 16 | 11 | 10 | **68** |

---

## KiteClass Scores (/128)

### Public + Auth Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Landing | 11 | 26 | 19 | 14 | 11 | **81** |
| About | 11 | 27 | 20 | 14 | 11 | **83** |
| Catalog | 12 | 27 | 20 | 14 | 12 | **85** |
| Catalog-detail | 4 | 12 | 8 | 6 | 6 | **36** |
| Contact | 12 | 26 | 19 | 13 | 11 | **81** |
| Login | 13 | 28 | 22 | 15 | 13 | **91** |
| Register | 12 | 27 | 21 | 14 | 12 | **86** |
| Register-student | 12 | 26 | 19 | 13 | 11 | **81** |
| Forgot-password | 13 | 28 | 22 | 15 | 13 | **91** |
| Reset-password | 11 | 24 | 18 | 12 | 11 | **76** |

### Dashboard Pages (27 pages)

| Screen | Total | Method |
|--------|-------|--------|
| dashboard-teacher | **84** | Individually viewed |
| billing-detail | **86** | Individually viewed |
| class-detail | **83** | Individually viewed |
| courses | **82** | Individually viewed |
| students | **82** | Individually viewed |
| teachers | **82** | Individually viewed |
| billing | **82** | Individually viewed |
| catalog (from public) | **85** | Individually viewed |
| attendance | **80** | Individually viewed |
| branding-wizard | **80** | Individually viewed |
| student-new | **80** | Individually viewed |
| class-attendance | **80** | Individually viewed |
| billing-pay | **81** | Individually viewed |
| attendance-stats | **79** | Individually viewed |
| classes | **78** | Individually viewed |
| attendance-reports | **78** | Individually viewed |
| settings | **74** | Individually viewed |
| class-edit, course-new, course-edit, course-class-new, student-edit, teacher-new, teacher-edit | **80** | Group-scored (form pages) |
| course-detail | **81** | Group-scored (detail pages) |
| student-detail, teacher-detail | **82** | Group-scored (detail pages) |
| student-attendance | **80** | Group-scored (attendance) |

---

## Summary

### KiteHub

| Metric | Score |
|--------|-------|
| Best screen | Pricing **98/128** |
| Best group (public+auth excl 404) | avg **92/128** |
| Dashboard pages with content | avg **62/128** |
| Dashboard pages loading/error | avg **36/128** |
| Admin pages with content | avg **57/128** |
| Overall avg (all 24 pages) | **58/128** |
| Lowest screen | Instance-detail, Billing-payment, Branding **33/128** |

### KiteClass

| Metric | Score |
|--------|-------|
| Best screen | Login, Forgot-password **91/128** |
| Public+auth avg (excl 404) | avg **84/128** |
| Dashboard pages avg | avg **80/128** |
| Overall avg (all 37 pages) | **80/128** |
| Lowest screen | Catalog-detail **36/128** (404 page) |
| Lowest real screen | Settings **74/128** |

### Cross-App Comparison

| Metric | KiteHub | KiteClass |
|--------|---------|-----------|
| Public pages (excl 404) | **92** | **84** |
| Auth pages | **93** | **85** |
| Dashboard (with content) | **62** | **80** |
| Overall | **58** | **80** |

**KiteHub public pages are polished; dashboard is dragged down by empty/error states.**
**KiteClass is more consistent across all page types.**

---

## Issues Found

### Critical (P0)

| ID | App | Screen | Issue |
|----|-----|--------|-------|
| H-1 | KiteHub | 10/16 dashboard pages | Show error/loading/login-redirect instead of content. Mock auth not working for dashboard/admin routes. |
| H-2 | KiteHub | blog-detail | 404 — no custom 404 page, shows English "This page could not be found" |
| K-1 | KiteClass | catalog-detail | 404 — shows "Khong tim thay trang" error (mock data missing) |
| H-3 | KiteHub | ALL dark screenshots | Dark mode visually identical to light — theme not switching despite class injection |

### High (P1)

| ID | App | Screen | Issue |
|----|-----|--------|-------|
| K-2 | KiteClass | ALL pages | "2 errors" / "5 errors" dev overlay visible bottom-left on every page |
| K-3 | KiteClass | register-student | Date format mm/dd/yyyy — should be dd/mm/yyyy for Vietnamese users |
| K-4 | KiteClass | Multiple | Mixed language: "Select an option", "Rows per page", "COMPLETED", wizard labels "Tone/Template/Preview" |
| K-5 | KiteClass | landing | Duplicated hero text — "Chuyen nghiep & Hieu qua" appears twice |
| H-4 | KiteHub | blog | Missing Vietnamese diacritics in subtitle text |

### Medium (P2)

| ID | App | Screen | Issue |
|----|-----|--------|-------|
| K-6 | KiteClass | settings | Weakest page (74/128) — all color pickers show #000000, unstyled file input, no preview |
| K-7 | KiteClass | dashboard pages | No breadcrumb navigation on detail/edit pages |
| H-5 | KiteHub | login | Missing "Forgot password" link |
| H-6 | KiteHub | dashboard pages | No loading skeletons — bare spinners with no context |
| H-7 | KiteHub | dashboard pages | Inconsistent error handling — some have "Thu lai" retry, others don't |

---

## Recommendations

### Immediate (before next audit)

1. **Fix KiteHub mock auth** — dashboard/admin routes redirect to login. Capture script auth injection not working for these routes.
2. **Fix "2 errors" dev overlay** — investigate console errors causing persistent error badge on KiteClass.
3. **Add custom 404 pages** — both apps need Vietnamese 404 with navigation back.
4. **Investigate KiteHub dark mode** — class injection applied but visual theme not switching.

### Next PR wave

5. **Add MSW mock data** to KiteHub capture script (like KiteClass has) for realistic dashboard content.
6. **Fix i18n gaps** in KiteClass — date format, "Select an option", "Rows per page", wizard step labels.
7. **Improve settings page** — add data loading, styled file input, logo preview.
8. **Add loading skeletons** to KiteHub dashboard pages.

### Quality Targets

| Metric | Current | Target |
|--------|---------|--------|
| KiteHub public pages | 92 | 95+ |
| KiteHub dashboard (with content) | 62 | 80+ |
| KiteClass public+auth | 84 | 90+ |
| KiteClass dashboard | 80 | 85+ |
| Lowest screen (excl 404) | 33 (KH) / 74 (KC) | 70+ |

---

## Comparison with Previous Audit (2026-04-13 — KiteClass only)

| Screen | Previous | Current | Delta |
|--------|----------|---------|-------|
| Landing | 92 | 81 | -11 |
| Login | 96 | 91 | -5 |
| Register | 95 | 86 | -9 |
| About | 89 | 83 | -6 |
| Catalog | 83 | 85 | +2 |
| Contact | 86 | 81 | -5 |
| Dashboard-teacher | 86 | 84 | -2 |
| Classes | 86 | 78 | -8 |
| Courses | 82 | 82 | 0 |
| Students | 82 | 82 | 0 |

**Delta explanation:** Previous audit scored from a different capture run (with CSS partially loading). Current audit used strict external-auditor rubric with subagent delegation. Scores are ~5-10 pts lower reflecting more honest assessment ("có feature" = 2/4 strictly applied). This is a calibration improvement, not a regression.

---

## Audit Validity

| Aspect | Status |
|--------|--------|
| KiteHub light mode | Valid |
| KiteHub dark mode | Invalid — visually identical to light |
| KiteClass dark mode | Valid — renders correctly |
| KiteClass light mode | Needs verification — agents reported missing light-desktop files |
| KiteHub dashboard content | Partial — most pages show empty/error states |
| KiteClass dashboard content | Valid — mock API provides realistic data |

---

*Generated by UI Review skill (v2 — subagent delegation) | 2026-04-16*
*4 parallel agents, ~15 minutes total, no compaction needed*
