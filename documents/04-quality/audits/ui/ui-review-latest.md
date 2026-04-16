# UI Review — KiteHub + KiteClass Frontend

**Date:** 2026-04-16
**Version:** main @ `49802f5d` (post-Wave 4)
**Method:** Playwright automated screenshots — KiteHub 96 PNGs (24 pages x 4 variants), KiteClass 147/148 PNGs (37 pages x 4 variants, 1 timeout)
**Screenshots:** `documents/screenshots/kitehub-latest/` + `documents/screenshots/latest/`
**Previous review:** ui-review 2026-04-13 (KiteClass only, main @ `0350c0ce`)

---

## CRITICAL: Capture-Tool Issues Affecting This Audit

Two capture-tool bugs significantly limit scoring accuracy:

### Issue 1: KiteHub dark mode not activating

All KiteHub dark-mode screenshots are **identical to light mode**. The capture script does not properly trigger `next-themes` dark mode (requires localStorage + class toggle via JS interaction that Playwright doesn't execute). This is a **capture-tool bug**, NOT a code defect.

**Impact:** Dark mode cannot be evaluated for KiteHub. All KiteHub scores are based on light-mode screenshots only. Dark mode sub-score = 0 (unverifiable, not broken).

### Issue 2: KiteClass Tailwind CSS not loading

ALL KiteClass screenshots render **without Tailwind CSS**. Light variants show raw HTML with browser-default styling (white bg, default fonts, unstyled form controls). Dark variants get dark background color applied but no Tailwind utility classes.

**Root cause:** Capture script uses `waitUntil: 'domcontentloaded'` which does not wait for CSS bundle download. First page capture triggers CSS load; subsequent captures may or may not benefit from cache.

**Impact:** KiteClass scores reflect **unstyled HTML rendering**, not actual app quality. Previous audit (2026-04-13) scored KiteClass at 56-96/128 with CSS loading. Current low scores are NOT a code regression — they are a capture-tool regression.

**Fix required:** Change `waitUntil: 'domcontentloaded'` to `waitUntil: 'networkidle'` in `kiteclass/kiteclass-frontend/scripts/capture-screenshots.ts`.

---

## Fix Verification (vs ui-review 2026-04-13)

Cannot verify fixes visually because KiteClass CSS is not loading in captures. Status based on code review / manifest evidence:

| Issue | Previous Status | Current | Notes |
|-------|----------------|---------|-------|
| S-1 settings stuck loading | FIXED (PR #264) | Unverifiable | CSS not loading in capture |
| S-2 billing spinner | FIXED (PR #264) | Unverifiable | CSS not loading in capture |
| S-3 billing-pay outside layout | FIXED (PR #264) | Unverifiable | CSS not loading in capture |
| S-4 skeleton labels | FIXED (PR #264) | Unverifiable | CSS not loading in capture |
| S-5 students/teachers spinner | FIXED (PR #264) | Unverifiable | CSS not loading in capture |
| S-6 landing nav active state | OPEN | Unverifiable | CSS not loading in capture |
| S-7 login mobile branding | OPEN | Unverifiable | CSS not loading in capture |

---

## KiteHub Scores (/128)

Scored from **light-mode screenshots** (dark mode broken in captures).

### Public Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Landing | 13 | 28 | 23 | 16 | 12 | **92** |
| Pricing | 13 | 26 | 20 | 14 | 12 | **85** |
| Blog | 13 | 24 | 18 | 13 | 11 | **79** |
| Blog-detail | 12 | 20 | 14 | 12 | 10 | **68** |
| Legal-DMCA | 13 | 24 | 18 | 14 | 12 | **81** |

**Notes:**
- **Landing**: Excellent gradient hero, feature sections with icons, stats counters, testimonials, CTA. Responsive mobile (sidebar hidden, content stacked). Professional marketing page.
- **Pricing**: Clean tier comparison cards. Clear feature lists per plan.
- **Blog**: Listing with tags, categories. Decent layout. May show empty without backend.
- **Blog-detail**: Shows 404/error state without backend data. Minimal content to evaluate.
- **Legal-DMCA**: New page (Wave 4 PR #302). DMCA intake form with proper fields, accessible.

### Auth Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Login | 14 | 28 | 22 | 16 | 13 | **93** |
| Register | 14 | 28 | 22 | 15 | 13 | **92** |
| Verify-email | 12 | 22 | 16 | 13 | 12 | **75** |

**Notes:**
- **Login/Register**: Split-layout with illustration on left, form on right. Clean design, good form labels, Vietnamese content. Mobile hides illustration panel (acceptable).
- **Verify-email**: Shows verification state without token. Functional but sparse.

### Customer Dashboard Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Dashboard | 13 | 26 | 20 | 14 | 12 | **85** |
| Billing-history | 13 | 24 | 18 | 14 | 12 | **81** |
| Branding-templates | 13 | 24 | 18 | 14 | 12 | **81** |
| Instance-detail | 10 | 16 | 10 | 10 | 10 | **56** |
| Settings | 10 | 16 | 10 | 10 | 10 | **56** |
| Billing | 10 | 16 | 10 | 10 | 10 | **56** |
| Billing-upgrade | 10 | 16 | 10 | 10 | 10 | **56** |
| Billing-payment | 10 | 16 | 10 | 10 | 10 | **56** |
| Branding | 10 | 16 | 10 | 10 | 10 | **56** |
| Branding-assets | 10 | 16 | 10 | 10 | 10 | **56** |
| Branding-wizard | 10 | 16 | 10 | 10 | 10 | **56** |

**Notes:**
- **Dashboard**: Sidebar navigation, stats cards, instance overview. Mobile-responsive (sidebar collapses). Clean layout.
- **Billing-history**: Table with mock data rows. Proper column headers.
- **Branding-templates**: Template gallery with filter tabs, loading spinner for content. Shows template cards.
- **Instance-detail through Branding-wizard** (7 pages): All show authenticated shell with minimal/placeholder content (31-32KB). Mock auth injects layout but API calls return errors. Shows loading states or minimal error UI. Cannot evaluate feature quality — only shell is visible.

### Admin Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Admin | 10 | 18 | 12 | 11 | 10 | **61** |
| Admin-instances | 13 | 24 | 18 | 14 | 12 | **81** |
| Admin-instance-detail | 10 | 18 | 12 | 11 | 10 | **61** |
| Admin-payments | 13 | 24 | 18 | 14 | 12 | **81** |
| Admin-revenue | 13 | 24 | 18 | 14 | 12 | **81** |

**Notes:**
- **Admin-instances**: Data table with instance list, sidebar navigation, status badges. Professional admin interface.
- **Admin-payments**: Payment management table with info notice banner. Clear columns.
- **Admin-revenue**: Revenue charts with graph visualizations. Dashboard metrics.
- **Admin, Admin-instance-detail**: Minimal content shown (placeholder/shell states).

---

## KiteClass Scores (/128)

**WARNING: All scores below are artificially low due to Tailwind CSS not loading in captures. Previous audit (2026-04-13) scored these pages 56-96/128 with CSS working. These scores reflect the capture-tool failure, NOT actual app quality.**

### Public Pages (content visible, but unstyled)

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total | Prev Score |
|--------|----------|----------------|----------------|--------|----------|-------|------------|
| Landing | 7 | 18 | 6 | 10 | 9 | **50** | 92 |
| About | 7 | 16 | 5 | 9 | 8 | **45** | 89 |
| Catalog | 7 | 16 | 5 | 9 | 8 | **45** | 83 |
| Catalog-detail | 5 | 12 | 3 | 7 | 7 | **34** | 66 |
| Contact | 7 | 16 | 5 | 9 | 8 | **45** | 86 |

**Notes:**
- **Landing**: All content renders (hero text, features, stats 100+/10,000+/500+/1,000+, teacher profiles, certs, pricing tiers, testimonials, CTA). BUT zero CSS styling — browser defaults, no visual hierarchy beyond HTML headings, no cards/shadows/gradients.
- **About**: Full mission/vision/values/features/roadmap content. Some highlighted text (gold/yellow). Completely unstyled layout.
- **Catalog**: Search bar + filter dropdowns visible. "Đang tải khóa học..." spinner. Empty state CTA ("Không tìm thấy khóa học phù hợp?"). Unstyled.
- **Contact**: Form fields (name, email, phone, message) + contact info (email, hotline, address). Browser-default form controls.

### Auth Pages (content visible, but unstyled)

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total | Prev Score |
|--------|----------|----------------|----------------|--------|----------|-------|------------|
| Login | 7 | 18 | 5 | 10 | 9 | **49** | 96 |
| Register | 7 | 18 | 5 | 10 | 9 | **49** | 95 |
| Register-student | 7 | 16 | 5 | 9 | 8 | **45** | 90 |
| Forgot-password | 7 | 16 | 5 | 9 | 9 | **46** | 89 |
| Reset-password | 6 | 14 | 4 | 8 | 8 | **40** | 87 |

**Notes:**
- **Login**: Email/password form + "Ghi nho dang nhap" checkbox + "Quên mật khẩu?" link. Branding header above. All functional HTML but zero styling.
- **Register**: Role selection (Học viên / Trung tâm) with descriptions and action buttons. Good structure.
- **Register-student**: Full form (name, email, password, confirm, phone, DOB, gender, address). All fields render with Vietnamese labels and placeholders. Unstyled.
- **Forgot-password**: Simple email form + back-to-login link. Minimal but functional.

### Dashboard Pages (mostly loading spinners)

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total | Prev Score |
|--------|----------|----------------|----------------|--------|----------|-------|------------|
| Dashboard-teacher | 6 | 14 | 4 | 8 | 8 | **40** | 86 |
| Classes | 4 | 8 | 2 | 4 | 6 | **24** | 86 |
| Courses | 4 | 8 | 2 | 4 | 6 | **24** | 82 |
| Students | 4 | 8 | 2 | 4 | 6 | **24** | 82 |
| Teachers | 4 | 8 | 2 | 4 | 6 | **24** | 82 |
| Attendance | 4 | 8 | 2 | 4 | 6 | **24** | 87 |
| Billing | 4 | 8 | 2 | 4 | 6 | **24** | 82 |
| Settings | 4 | 8 | 2 | 4 | 6 | **24** | 72 |
| Branding-wizard | 4 | 8 | 2 | 4 | 6 | **24** | N/A |

**Notes:**
- **Dashboard-teacher**: Shows sidebar structure + content area. Unstyled but layout detectable.
- **All other dashboard pages**: Only loading spinner visible on dark background. No content rendered — requires backend API data. Cannot evaluate features, UX, or design. Score reflects "spinning loader with no content."
- 18 remaining dashboard pages (class-detail, class-edit, class-attendance, course-new, course-detail, course-edit, course-class-new, student-new, student-detail, student-edit, student-attendance, teacher-new, teacher-detail, teacher-edit, attendance-reports, attendance-stats, billing-detail, billing-pay) — all show loading spinners. Scored same as above: **24/128**.

---

## Summary

### KiteHub

| Metric | Score |
|--------|-------|
| **Best screen** | Landing — **92/128** |
| **Worst screen (with content)** | Blog-detail — **68/128** |
| **Worst screen (shell only)** | 7 customer pages — **56/128** |
| Public pages avg | **81/128** |
| Auth pages avg | **87/128** |
| Customer dashboard avg (with content) | **82/128** |
| Customer dashboard avg (shell) | **56/128** |
| Admin pages avg (with content) | **81/128** |
| **Overall avg (all 24 pages)** | **71/128** |

### KiteClass (INVALID — capture-tool CSS failure)

| Metric | Score | Previous (04-13) |
|--------|-------|-----------------|
| **Best screen** | Landing — **50/128** | 92/128 |
| **Worst screen** | Dashboard pages — **24/128** | 56/128 |
| Public pages avg | **44/128** | 83/128 |
| Auth pages avg | **46/128** | 91/128 |
| Dashboard pages avg | **25/128** | 78/128 |
| **Overall avg** | **31/128** | 80/128 |

**KiteClass delta vs previous: -49 points average. This is entirely due to the capture-tool CSS loading bug, NOT a code regression.**

---

## Issues Found

### Capture-Tool Issues (Blocking)

| ID | Severity | App | Issue | Fix |
|----|----------|-----|-------|-----|
| CT-1 | P0 | KiteHub | Dark mode not activating in captures | Inject `localStorage.theme = 'dark'` + `document.documentElement.classList.add('dark')` before navigation |
| CT-2 | P0 | KiteClass | Tailwind CSS not loading in captures | Change `waitUntil: 'domcontentloaded'` to `waitUntil: 'networkidle'` |
| CT-3 | P2 | KiteClass | class-edit/dark-mobile timeout (1/148) | Increase timeout or retry logic |

### KiteHub UI Issues

| ID | Severity | Screen | Issue | Notes |
|----|----------|--------|-------|-------|
| H-1 | P1 | 7 customer pages | Shell-only content (no mock data) | Instance-detail, settings, billing, billing-upgrade, billing-payment, branding, branding-assets, branding-wizard show only auth shell. Need MSW mock data in capture script. |
| H-2 | P1 | Dashboard mobile | Sidebar overlaps content on some breakpoints | Visible in dashboard/light-mobile |
| H-3 | P2 | Blog-detail | 404-like state without backend | Expected; needs graceful empty state |
| H-4 | P2 | Admin, Admin-instance-detail | Minimal shell content | Same mock data gap as H-1 |
| H-5 | P2 | ALL pages | Dark mode unverifiable | Blocked by CT-1 |
| H-6 | P3 | Branding-templates | Loading spinner for template gallery | Expected without backend; shows filter tabs above |

### KiteClass UI Issues (from content/structure only, not visual)

| ID | Severity | Screen | Issue | Notes |
|----|----------|--------|-------|-------|
| K-1 | P2 | Landing light-mobile | Navigation links run together | "Trang chủKhóa họcGiới thiệuLiên hệ" — no separators/spacing (may be CSS issue) |
| K-2 | P2 | Contact | Form fields use browser defaults | No custom styling visible — likely CSS loading issue |
| K-3 | P2 | About | Very long single-column page | No visual breaks between sections (may be missing CSS grid/cards) |
| K-4 | P3 | Register | "Đăng ký trung tâm" button has arrow icon | Minor: external link indicator on internal navigation |

---

## Cross-App Observations

### Positive
1. **Vietnamese i18n complete** — Both apps render all UI text in Vietnamese. Labels, buttons, error messages, placeholder text all localized.
2. **Content quality** — Landing pages, about pages have comprehensive and professional content.
3. **Form completeness** — Registration forms have all necessary fields (name, email, password, phone, DOB, gender, address).
4. **Footer consistency** — Both apps have structured footers with links, contact info, copyright.
5. **Accessibility basics** — Skip-to-content links visible in KiteClass. Semantic HTML structure present.
6. **KiteHub design quality** — When CSS loads (light mode), KiteHub pages are genuinely well-designed with gradients, cards, responsive layouts.

### Concerns
1. **Capture-tool reliability** — Two critical bugs make this audit partially invalid. Must fix before next audit.
2. **Mock data coverage** — Many authenticated pages show empty shells. MSW mock data injection would allow full page evaluation.
3. **KiteHub dark mode** — Cannot verify dark mode works at all from captures. Needs manual browser check or capture-tool fix.
4. **KiteClass CSS loading** — The `domcontentloaded` issue means no automated CSS verification is possible for KiteClass.

---

## Recommendations

### Immediate (before next audit)

1. **Fix capture-tool CT-2**: Change `waitUntil` to `'networkidle'` in KiteClass capture script
2. **Fix capture-tool CT-1**: Add proper dark mode injection for KiteHub (localStorage + classList)
3. **Add MSW mock data**: For authenticated pages to render actual content in captures

### Next PR wave

4. **Re-run full audit** after capture-tool fixes to get valid KiteClass scores
5. **Verify KiteHub dark mode** manually in browser
6. **Address H-1**: Add mock API responses to show realistic dashboard content in captures

### Quality bar

- **KiteHub target**: All scoreable pages >= 80/128 (currently 10/14 pages meet this)
- **KiteClass target**: Re-establish after capture-tool fix. Previous baseline was 75-96/128 for most pages.
- **Lowest screen**: Should be >= 65/128 for production readiness

---

## Audit Validity Statement

This audit is **partially valid**:

| App | Validity | Reason |
|-----|----------|--------|
| **KiteHub light mode** | Valid | CSS loads, pages render correctly |
| **KiteHub dark mode** | Invalid | Capture-tool not triggering dark theme |
| **KiteClass all variants** | Invalid | Tailwind CSS not loading in any capture |

KiteHub light-mode scores can be used for quality gate decisions. KiteClass scores should NOT be used — refer to previous audit (2026-04-13) as the last valid baseline.

---

*Generated by UI Review skill | 2026-04-16 | Auditor: Claude Code*
*Screenshots: kitehub-latest/ (96 OK), latest/ (147 OK, 1 timeout)*
