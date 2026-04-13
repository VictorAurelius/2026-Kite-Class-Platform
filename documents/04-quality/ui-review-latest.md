# UI Review — KiteClass Frontend

**Ngày:** 2026-04-13
**Phiên bản:** main @ `0350c0ce`
**Phương pháp:** Screenshot thực tế (Playwright) — 144 PNGs, 36 trang × 2 themes × 2 viewports
**Screenshots:** `documents/screenshots/status-2026-04-13/`
**Previous review:** `ui-review` 2026-04-04 (baseline after PR #259)
**Dev server:** Port 3100 (port 3000 occupied by other app)

---

## Fix Verification (vs ui-audit-issues 2026-04-11)

| Issue | Status | Evidence |
|-------|--------|----------|
| P0-1 — ReactQueryDevtools in prod | ✅ FIXED | DevTools panel hidden. "2 errors" badge = Next.js dev overlay (dev-only) |
| P0-2 — Mobile dashboard broken | ✅ FIXED | Sidebar hidden, hamburger nav works (classes/light-mobile.png) |
| P0-3 — billing-pay blank | ✅ FIXED | Error state + "Quay lại danh sách" link (billing-pay/light-desktop.png) |
| P0-4 — settings blank | ⚠️ PARTIAL | Tabs render, but "Đang tải cài đặt..." still stuck loading |
| P1-1 — list pages spinner | ⚠️ PARTIAL | Classes fixed (empty state). Billing/Students still spinner |
| P1-2 — teacher stats skeleton | ⚠️ OPEN | 4 skeleton cards visible, no labels (dashboard-teacher) |
| P1-4 — catalog spinner | ✅ FIXED | "Đang tải khóa học..." then "Không tìm thấy khóa học phù hợp?" fallback |
| P2-1 — contact placeholder | ✅ FIXED | Env vars working (support@kiteclass.com, 1900 xxxx) |
| P2-3 — attendance skeleton labels | ⚠️ OPEN | Skeleton cards still have no labels |

---

## Scores per Screen (/128)

### Public Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Landing | 17 | 24 | 22 | 16 | 13 | **92** |
| About | 17 | 24 | 22 | 14 | 12 | **89** |
| Catalog | 16 | 22 | 19 | 14 | 12 | **83** |
| Contact | 17 | 23 | 20 | 14 | 12 | **86** |
| Catalog-detail | 14 | 16 | 14 | 12 | 10 | **66** |

### Auth Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Login | 17 | 27 | 22 | 16 | 14 | **96** |
| Register | 17 | 27 | 22 | 15 | 14 | **95** |
| Register-student | 16 | 26 | 20 | 15 | 13 | **90** |
| Forgot-password | 16 | 24 | 20 | 15 | 14 | **89** |
| Reset-password | 16 | 22 | 19 | 15 | 15 | **87** |

### Dashboard Pages

| Screen | Tech /20 | Heuristics /40 | Aesthetics /28 | UX /20 | WCAG /20 | Total |
|--------|----------|----------------|----------------|--------|----------|-------|
| Dashboard-teacher | 15 | 20 | 17 | 13 | 11 | **76** |
| Classes | 16 | 24 | 19 | 15 | 12 | **86** |
| Courses | 16 | 22 | 18 | 14 | 12 | **82** |
| Students | 15 | 20 | 17 | 13 | 11 | **76** |
| Teachers | 15 | 20 | 17 | 13 | 11 | **76** |
| Attendance | 16 | 24 | 20 | 15 | 12 | **87** |
| Attendance-stats | 15 | 20 | 17 | 13 | 11 | **76** |
| Billing | 14 | 18 | 15 | 12 | 10 | **69** |
| Billing-pay | 12 | 14 | 10 | 10 | 10 | **56** |
| Settings | 14 | 16 | 12 | 10 | 10 | **62** |

---

## Summary

| Metric | Previous (04-04) | Current (04-13) | Delta |
|--------|------------------|-----------------|-------|
| Lowest screen | 70/128 (catalog) | **56/128 (billing-pay)** | -14 |
| Auth pages avg | 90/128 | **91/128** | +1 |
| Public pages avg | 88/128 | **83/128** | -5* |
| Dashboard pages avg | N/A | **75/128** | new |

*catalog-detail drags down public avg (404 page).

---

## New Issues Found

### Still Open from Previous Audit

| ID | Severity | Screen | Issue | Notes |
|----|----------|--------|-------|-------|
| S-1 | 🟡 P2 | settings | "Đang tải cài đặt..." stuck loading | No error/timeout fallback |
| S-2 | 🟡 P2 | billing | Spinner without timeout | No empty state after API fail |
| S-3 | 🟡 P2 | billing-pay | Error renders outside dashboard layout | No sidebar/header |
| S-4 | 🟡 P2 | dashboard-teacher, attendance-stats | Skeleton cards without labels | Can't tell what's loading |
| S-5 | 🟡 P2 | students, teachers | Spinner on mobile | Expected without backend |
| S-6 | 🟢 P3 | landing | Nav: no active state on current page | Minor |
| S-7 | 🟢 P3 | login mobile | Left branding panel hidden | Acceptable UX |

### Dev-Only (NOT production issues)

| Issue | Screens | Notes |
|-------|---------|-------|
| "2 errors" badge | ALL 36 pages | Next.js dev error overlay. Hidden in production build. |

---

## Comparison with Previous Audit (2026-04-04)

| Screen | Before | After | Delta |
|--------|--------|-------|-------|
| Landing | 89/128 | 92/128 | +3 |
| Login | 93/128 | 96/128 | +3 |
| Register | 93/128 | 95/128 | +2 |
| About | 87/128 | 89/128 | +2 |
| Catalog | 78/128 | 83/128 | +5 |
| Contact | 85/128 | 86/128 | +1 |

---

## Remaining Issues Priority

| Priority | Count | Action |
|----------|-------|--------|
| 🟡 P2 | 5 | Fix in next PR wave |
| 🟢 P3 | 2 | Nice-to-have |

**Lowest screen: billing-pay (56/128)** — needs dashboard layout wrapper around error state.

---

## Targeted Re-audit — PR #264 (2026-04-13)

**Method:** 32 screenshots (8 pages × 4 variants) — affected screens only.
**Screenshots:** `documents/screenshots/after-pr-264/`

### Fix Verification

| Issue | Before | After | Status |
|-------|--------|-------|--------|
| S-1 settings stuck loading | "Đang tải cài đặt..." forever | Error message shown immediately | ✅ FIXED |
| S-2 billing spinner | Spinner no timeout | DashboardLayout + error alert | ✅ FIXED |
| S-3 billing-pay outside layout | No sidebar/header | Full dashboard chrome + icon + back button | ✅ FIXED |
| S-4 dashboard skeleton labels | "..." text only | "--" with labels when API fails | ✅ FIXED |
| S-4 attendance-stats skeleton | Blank rectangles | Empty state with icons + messages | ✅ FIXED |
| S-5 students/teachers spinner | Spinner only | Error alert "Lỗi tải dữ liệu" | ✅ FIXED |

### Updated Scores (affected screens only)

| Screen | Before (04-13) | After PR #264 | Delta |
|--------|----------------|---------------|-------|
| Billing-pay | 56/128 | **76/128** | +20 |
| Billing | 69/128 | **82/128** | +13 |
| Settings | 62/128 | **72/128** | +10 |
| Dashboard-teacher | 76/128 | **86/128** | +10 |
| Students | 76/128 | **82/128** | +6 |
| Teachers | 76/128 | **82/128** | +6 |
| Attendance-stats | 76/128 | **80/128** | +4 |

### Remaining Issues

| Priority | Count | Notes |
|----------|-------|-------|
| 🟢 P3 | 2 | Landing nav active state, auth mobile branding |
| 🟡 Observation | 2 | Settings + attendance-stats missing DashboardLayout (pre-existing, not in scope) |

---

## Next Steps

1. ~~Wrap billing-pay + billing-detail error states in dashboard layout~~ ✅ Done PR #264
2. ~~Add timeout/error fallback to settings loading~~ ✅ Done PR #264
3. ~~Add labels to skeleton cards (dashboard, attendance-stats)~~ ✅ Done PR #264
4. ~~Add empty state after spinner timeout for billing, students, teachers~~ ✅ Done PR #264
5. (P3) Add DashboardLayout to settings + attendance-stats pages
6. (P3) Landing nav active state
